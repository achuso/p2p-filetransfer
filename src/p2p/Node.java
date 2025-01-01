package p2p;

import network.FileClient;
import network.FileServer;

import java.io.*;
import java.security.NoSuchAlgorithmException;
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

    public Node(String peerID, String ip, int port, boolean isDockerInstance) {
        this.self = new Peer(peerID, ip, port);
        this.peerMgr = new PeerMgr();
        this.fileMgr = new FileMgr();

        if (isDockerInstance) {
            this.sharedFolder = new File("/test/shared");
            this.downloadFolder = new File("/test/downloads");
            fileMgr.refreshSharedFolder(this.sharedFolder);
            System.out.println("Number of local shared files: " + fileMgr.getSharedFiles().size());

            if (this.sharedFolder.isDirectory()) {
                fileMgr.refreshSharedFolder(this.sharedFolder);
                File[] files = this.sharedFolder.listFiles();
                if (files != null) {
                    for (File f : files) {
                        if (f.isFile()) {
                            self.addSharedFile(f);
                        }
                    }
                }
            }
        }
        else {
            // GUI mode
            this.sharedFolder = null;
            this.downloadFolder = null;
        }
    }

    // Start a TCP server to serve file requests (multithreaded)
    public void startServer() {
        new Thread(() -> FileServer.startServer(self.getPort(), this)).start();
    }

    public void startPeerDiscovery() {
        Thread discoveryThread = new Thread(() -> {
            while (keepDiscovering) {
                try {
                    peerMgr.clearPeers();
                    // Use broadcast discovery
                    peerMgr.discoverPeers(self.getIP(), self.getPort());

                    // Sleep 5 seconds, then attempt again
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    System.out.println("Peer discovery interrupted.");
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    System.err.println("Error during peer discovery: " + e.getMessage());
                }
            }
        });
        discoveryThread.setDaemon(true);
        discoveryThread.start();
    }

    public void stopPeerDiscovery() {
        keepDiscovering = false;
    }

    public void startFileSharing() {
        Thread sharingThread = new Thread(() -> {
            while (keepSharing) {
                try {
                    for (Peer peer : peerMgr.getAllPeers()) {
                        for (File file : peer.getSharedFiles()) {
                            FileMetaData fileMetaData = fileMgr.findFileByName(file.getName());
                            if (fileMetaData == null) {
                                System.out.println("Detected new file from " + peer.getPeerID() + ": " + file.getName());
                                downloadFileFromPeer(peer.getIP(), peer.getPort(), file.getName());
                            }
                        }
                    }
                    Thread.sleep(5000); // check every 5 seconds
                }
                catch (InterruptedException e) {
                    System.out.println("File sharing interrupted.");
                    break;
                }
            }
        });
        sharingThread.setDaemon(true);
        sharingThread.start();
    }

    public void stopFileSharing() {
        keepSharing = false;
    }

    // Old method for now, makes me feel safe...
    public void downloadFileFromPeer(String peerIP, int peerPort, String fileName) {
        System.out.println("Attempting single-source download: " + fileName + " from peer: " + peerIP);
        try {
            FileClient.downloadFile(peerIP, peerPort, fileName, downloadFolder);
            System.out.println("File downloaded successfully (single source): " + fileName);
        }
        catch (Exception e) {
            System.err.println("Error during file download: " + e.getMessage());
        }
    }

    public void multiSourceDownload(String fileHash) {
        FileMetaData meta = findFileMetaOnPeers(fileHash);
        if (meta == null) {
            System.err.println("No known metadata for file hash: " + fileHash);
            return;
        }

        // Ensure that there's at least 1 owner peer
        List<Peer> owners = meta.getPeersWithFile();
        if (owners.isEmpty()) {
            System.err.println("No peers own fileHash: " + fileHash);
            return;
        }

        long fileSize = meta.getFileSize();
        long chunkSize = 256L * 1024L; // 256 KB
        long totalChunks = (fileSize + chunkSize - 1) / chunkSize; // ceiling division
        System.out.println("Downloading " + totalChunks + " chunks from " + owners.size() + " peer(s).");

        // Temp folder for partial chunks
        File tempFolder = new File(downloadFolder, "temp_" + fileHash);
        if (!tempFolder.exists()) tempFolder.mkdirs();

        // Roundrobin-like assessment of chunks
        for (int i = 0; i < totalChunks; i++) {
            long offset = i * chunkSize;
            long sizeForThisChunk = Math.min(chunkSize, fileSize - offset);

            Peer chosenPeer = owners.get(i % owners.size()); // simple round-robin

            File chunkFile = new File(tempFolder, "chunk_" + i);

            System.out.printf("Downloading chunk %d/%d from %s (offset=%d, size=%d)%n",
                    i + 1, totalChunks, chosenPeer.getPeerID(), offset, sizeForThisChunk);

            boolean success = FileClient.downloadChunk(
                    chosenPeer.getIP(), chosenPeer.getPort(),
                    fileHash, offset, sizeForThisChunk,
                    chunkFile
            );
            if (!success) {
                System.err.println("Failed downloading chunk " + i + " from peer " + chosenPeer.getPeerID());
            }
        }

        // After all chunks, avengers reassemble
        reassembleFile(meta.getFileName(), fileHash, fileSize, tempFolder);
    }

    private void reassembleFile(String fileName, String fileHash, long fileSize, File tempFolder) {
        File finalFile = new File(downloadFolder, fileName);
        long bytesWritten = 0;
        int chunkIndex = 0;

        try (FileOutputStream fos = new FileOutputStream(finalFile)) {
            while (bytesWritten < fileSize) {
                File chunkFile = new File(tempFolder, "chunk_" + chunkIndex);
                if (!chunkFile.exists()) {
                    throw new IOException("Missing chunk: " + chunkFile.getAbsolutePath());
                }

                try (FileInputStream fis = new FileInputStream(chunkFile)) {
                    byte[] buffer = new byte[256 * 1024];
                    int bytesRead;
                    while ((bytesRead = fis.read(buffer)) != -1) {
                        fos.write(buffer, 0, bytesRead);
                        bytesWritten += bytesRead;
                    }
                }
                chunkIndex++;
            }
        }
        catch (IOException e) {
            System.err.println(e.getMessage());
            System.err.println("Error reassembling file: " + fileName);
            return;
        }

        // Verify final hash
        try {
            String calculatedHash = FileTransferMgr.calculateFileHash(finalFile);
            if (!calculatedHash.equals(fileHash)) {
                System.err.println("File integrity check failed. Expected: "
                        + fileHash + ", Found: " + calculatedHash);
            }
            else {
                System.out.println("File reassembled successfully: " + fileName);
            }
        }
        catch (IOException | NoSuchAlgorithmException e) {
            System.err.println("Error reassembling file: " + fileName);
        }

        // Cleanup partial chunks
        File[] chunkFiles = tempFolder.listFiles();
        if (chunkFiles != null) {
            for (File f : chunkFiles) {
                f.delete();
            }
        }
        tempFolder.delete();
    }

    private FileMetaData findFileMetaOnPeers(String fileHash) {
        // Check local FileMgr
        FileMetaData local = fileMgr.getFileMetaDataByHash(fileHash);
        if (local != null) {
            local.addOwnerPeer(self);
        }

        return local;
    }

    public void setSharedFolder(String path) {
        File newFolder = new File(path);
        if (!newFolder.exists() || !newFolder.isDirectory()) {
            throw new IllegalArgumentException("Invalid shared folder: " + path);
        }
        this.sharedFolder = newFolder;
        System.out.println("Shared folder set to: " + sharedFolder.getAbsolutePath());

        // Refresh Node's FileMgr
        fileMgr.refreshSharedFolder(sharedFolder);

        // Also add them to the object
        File[] files = newFolder.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isFile()) {
                    self.addSharedFile(f);
                }
            }
        }
    }

    public void setDownloadFolder(String path) {
        File newFolder = new File(path);
        if (!newFolder.exists()) {
            if (!newFolder.mkdirs()) {
                throw new IllegalArgumentException("Failed to create download folder: " + path);
            }
        }
        this.downloadFolder = newFolder;
        System.out.println("Download folder set to: " + downloadFolder.getAbsolutePath());
    }

    public PeerMgr getPeerManager() { return peerMgr; }
    public Collection<Peer> getDiscoveredPeers() { return peerMgr.getAllPeers(); }
    public Peer getSelf() { return self; }
    public FileMgr getFileManager() { return fileMgr; }
    public File getDownloadFolder() { return downloadFolder; }
    public File getSharedFolder() { return sharedFolder; }
}