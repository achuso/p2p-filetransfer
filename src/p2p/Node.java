package p2p;

import network.FileServer;

import java.io.*;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Node {
    private final Peer self;
    private final PeerMgr peerMgr;
    private final FileMgr fileMgr;

    private File sharedFolder;
    private File downloadFolder;

    private volatile boolean keepDiscovering = true;
    private volatile boolean keepSharing = true;
    private volatile boolean keepMonitoringFolder = false; // for auto-refresh

    private final boolean isDockerMode;

    public Node(String peerID, String ip, int port, boolean isDocker) {
        System.out.println("[Node] Constructor => peerID=" + peerID
                + ", ip=" + ip + ", port=" + port
                + ", isDocker=" + isDocker);

        this.self = new Peer(peerID, ip, port);
        this.peerMgr = new PeerMgr();
        this.fileMgr = new FileMgr();
        this.isDockerMode = isDocker;

        if (isDocker) {
            // container dirs: /test/shared and /test/downloads
            this.sharedFolder = new File("/test/shared");
            this.downloadFolder = new File("/test/downloads");

            System.out.println("[Node] Docker => Checking /test/shared & /test/downloads...");
            sharedFolder.mkdirs();
            downloadFolder.mkdirs();

            if (sharedFolder.isDirectory()) {
                fileMgr.refreshSharedFolder(sharedFolder);
                File[] files = sharedFolder.listFiles();
                if (files != null) {
                    for (File f : files) {
                        if (f.isFile()) {
                            self.addSharedFile(f);
                            System.out.println("[Node] Docker added => " + f.getName());
                        }
                    }
                }
                System.out.println("[Node] After initial refresh => "
                        + fileMgr.getSharedFiles().size() + " shared file(s).");
            }
        }
        else {
            // GUI mode
            System.out.println("[Node] Non-Docker => user sets folders via setSharedFolder(...) etc.");
            this.sharedFolder = null;
            this.downloadFolder = null;
        }
        System.out.println("[Node] Constructor done.\n");
    }

    public void startServer() {
        System.out.println("[Node] startServer() called.");
        new Thread(() -> FileServer.startServer(self.getPort(), this)).start();
    }

    public void startPeerDiscovery() {
        System.out.println("[Node] startPeerDiscovery() called.");
        Thread discThread = new Thread(() -> {
            while (keepDiscovering) {
                try {
                    peerMgr.clearPeers();
                    peerMgr.discoverPeers(self.getIP(), self.getPort());
                    Thread.sleep(5000);
                }
                catch (InterruptedException e) {
                    System.out.println("[Node] Peer discovery interrupted.");
                    Thread.currentThread().interrupt();
                }
                catch (Exception e) {
                    System.err.println(e.getMessage());
                }
            }
        });
        discThread.setDaemon(true);
        discThread.start();
    }

    public void stopPeerDiscovery() {
        System.out.println("[Node] stopPeerDiscovery() called.");
        keepDiscovering = false;
    }

    public void startFileSharing() {
        System.out.println("[Node] startFileSharing() called.");
        Thread sharingThread = new Thread(() -> {
            while (keepSharing) {
                try {
                    for (Peer peer : peerMgr.getAllPeers()) {
                        for (File pseudoFile : peer.getSharedFiles()) {
                            String combined = pseudoFile.getName();
                            int idx = combined.indexOf('_');
                            if (idx < 0) {
                                continue;
                            }
                            String fileHash = combined.substring(0, idx);
                            String fileName = combined.substring(idx + 1);

                            if (fileMgr.getFileMetaDataByHash(fileHash) != null) {
                                continue; // already have it
                            }
                            System.out.println("[Node] Detected new file from "
                                    + peer.getPeerID() + ": " + fileName
                                    + " (hash=" + fileHash + ")");

                            // now do chunk-based from only the real owners
                            multiSourceDownload(fileHash, fileName);
                        }
                    }
                    Thread.sleep(5000);
                }
                catch (InterruptedException e) {
                    System.out.println("[Node] File sharing thread interrupted.");
                    Thread.currentThread().interrupt();
                }
            }
        });
        sharingThread.setDaemon(true);
        sharingThread.start();
    }

    public void stopFileSharing() {
        System.out.println("[Node] stopFileSharing() called.");
        keepSharing = false;
    }

    public void startLocalFolderMonitor() {
        if (!isDockerMode) {
            System.out.println("[Node] startLocalFolderMonitor => skipping, not Docker mode.");
            return;
        }
        if (sharedFolder == null || !sharedFolder.isDirectory()) {
            System.out.println("[Node] startLocalFolderMonitor => no valid sharedFolder, skip");
            return;
        }
        System.out.println("[Node] startLocalFolderMonitor => enabling auto-refresh of " + sharedFolder);

        keepMonitoringFolder = true;
        Thread monitorThread = new Thread(() -> {
            while (keepMonitoringFolder) {
                try {
                    Thread.sleep(5000);
                    System.out.println("[Node] Auto-refreshing " + sharedFolder + " for new files...");
                    fileMgr.refreshSharedFolder(sharedFolder);

                    // Add them to 'self'
                    File[] files = sharedFolder.listFiles();
                    if (files != null) {
                        for (File f : files) {
                            if (f.isFile()) {
                                self.addSharedFile(f);
                            }
                        }
                    }
                }
                catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        monitorThread.setDaemon(true);
        monitorThread.start();
    }

    public void stopLocalFolderMonitor() {
        keepMonitoringFolder = false;
    }

    public void multiSourceDownload(String fileHash, String fileName) {
        System.out.println("[Node] multiSourceDownload(" + fileHash
                + ", " + fileName + ") called.");

        if (fileMgr.getFileMetaDataByHash(fileHash) == null) {
            FileMetaData meta = new FileMetaData(fileName, 0, null, fileHash);
            fileMgr.addDownloadingFile(meta);
        }

        // find peers that own the hash
        List<Peer> owners = getOwnersOfFile(fileHash);
        if (owners.isEmpty()) {
            System.err.println("[Node] No actual owners have " + fileHash);
            return;
        }

        // get file size from any one owner
        long fileSize = 0;
        for (Peer p : owners) {
            long size = network.FileClient.requestFileSizeByHash(p.getIP(), p.getPort(), fileHash);
            if (size > 0) {
                fileSize = size;
                break;
            }
        }
        if (fileSize <= 0) {
            System.err.println("[Node] Could not retrieve file size => " + fileHash);
            return;
        }

        // chunk
        long chunkSize = 256L * 1024L;
        long totalChunks = (fileSize + chunkSize - 1) / chunkSize;

        File tempFolder = new File(downloadFolder, "temp_" + fileHash);
        tempFolder.mkdirs();

        System.out.println("[Node] Will download " + totalChunks + " chunks => " + fileName);

        for (int i = 0; i < totalChunks; i++) {
            long offset = i * chunkSize;
            long csize = Math.min(chunkSize, fileSize - offset);

            // round-robin over actual owners
            Peer chosenPeer = owners.get(i % owners.size());
            File chunkFile = new File(tempFolder, "chunk_" + i);

            System.out.println("[Node] chunk " + i + " from " + chosenPeer.getPeerID()
                    + " offset=" + offset + " size=" + csize);

            boolean success = network.FileClient.downloadChunk(
                    chosenPeer.getIP(), chosenPeer.getPort(),
                    fileHash, offset, csize,
                    chunkFile
            );
            if (!success) {
                System.err.println("[Node] chunk " + i
                        + " failed from " + chosenPeer.getPeerID());
            }
        }

        reassembleChunks(fileHash, fileName, fileSize, tempFolder);
    }

    private List<Peer> getOwnersOfFile(String fileHash) {
        List<Peer> owners = new ArrayList<>();
        for (Peer p : peerMgr.getAllPeers()) {
            boolean hasIt = p.getSharedFiles().stream()
                    .anyMatch(ff -> ff.getName().startsWith(fileHash + "_"));
            if (hasIt) {
                owners.add(p);
            }
        }
        return owners;
    }

    private void reassembleChunks(String fileHash, String fileName,
                                  long fileSize, File tempFolder) {
        File finalFile = new File(downloadFolder, fileName);
        long bytesWritten = 0;
        int chunkIndex = 0;

        System.out.println("[Node] Reassembling => " + finalFile.getAbsolutePath());
        try (FileOutputStream fos = new FileOutputStream(finalFile)) {
            while (bytesWritten < fileSize) {
                File cf = new File(tempFolder, "chunk_" + chunkIndex);
                if (!cf.exists()) {
                    throw new IOException("Missing chunk => " + cf.getAbsolutePath());
                }
                try (FileInputStream fis = new FileInputStream(cf)) {
                    byte[] buffer = new byte[256 * 1024];
                    int read;
                    while ((read = fis.read(buffer)) != -1) {
                        fos.write(buffer, 0, read);
                        bytesWritten += read;
                    }
                }
                chunkIndex++;
            }
        }
        catch (IOException e) {
            System.err.println("[Node] reassembleChunks error => " + e.getMessage());
            e.printStackTrace();
            return;
        }

        // verify final
        try {
            String calcHash = FileTransferMgr.calculateFileHash(finalFile);
            if (!calcHash.equals(fileHash)) {
                System.err.println("[Node] Hash mismatch => expected="
                        + fileHash + ", got=" + calcHash);
            } else {
                System.out.println("[Node] File reassembled successfully => " + fileName);
            }
        }
        catch (IOException | NoSuchAlgorithmException e) {
            System.err.println(e.getMessage());
        }

        // cleanup
        File[] leftover = tempFolder.listFiles();
        if (leftover != null) {
            for (File f : leftover) {
                f.delete();
            }
        }
        tempFolder.delete();
    }

    public void setSharedFolder(String path) {
        System.out.println("[Node] setSharedFolder(" + path + ")");
        File sf = new File(path);
        if (!sf.exists() || !sf.isDirectory()) {
            throw new IllegalArgumentException("Invalid shared folder => " + path);
        }
        this.sharedFolder = sf;
        fileMgr.refreshSharedFolder(sf);

        File[] files = sf.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isFile()) {
                    self.addSharedFile(f);
                    System.out.println("[Node] GUI added => " + f.getName());
                }
            }
        }
        System.out.println("[Node] Now " + fileMgr.getSharedFiles().size()
                + " total shared file(s).");
    }

    public void setDownloadFolder(String path) {
        System.out.println("[Node] setDownloadFolder(" + path + ")");
        File df = new File(path);
        df.mkdirs();
        this.downloadFolder = df;
        System.out.println("[Node] Download folder => " + df.getAbsolutePath());
    }

    public File getSharedFolder()           { return sharedFolder; }
    public File getDownloadFolder()         { return downloadFolder; }
    public PeerMgr getPeerManager()         { return peerMgr; }
    public Collection<Peer> getAllPeers()   { return peerMgr.getAllPeers(); }
    public Peer getSelf()                   { return self; }
    public FileMgr getFileManager()         { return fileMgr; }
}