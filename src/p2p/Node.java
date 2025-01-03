package p2p;

import network.FileClient;
import network.FileServer;

import java.io.*;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Node {

    public static class FoundFile {
        public final String fileHash;
        public final String fileName;
        public final Set<String> owners; // e.g., "10.22.249.202"

        public FoundFile(String hash, String name) {
            this.fileHash = hash;
            this.fileName = name;
            this.owners = new HashSet<>();
        }
    }

    // Track the progress of a file being downloaded
    public static class DownloadProgress {
        public final String fileHash;
        public final String fileName;

        public long totalSize;
        public long downloadedBytes;
        public boolean isComplete; // remain in map at 100%

        public DownloadProgress(String hash, String name) {
            this.fileHash = hash;
            this.fileName = name;
            this.totalSize = 0;
            this.downloadedBytes = 0;
            this.isComplete = false;
        }

        public double getPercent() {
            if (totalSize <= 0) return 0.0;
            double p = (downloadedBytes * 100.0) / totalSize;
            if (p > 100.0) p = 100.0;
            return p;
        }
    }

    private final Peer self;
    private final PeerMgr peerMgr;
    private final FileMgr fileMgr;

    private final boolean isDockerMode;
    private File sharedFolder;
    private File downloadFolder;

    private volatile boolean keepDiscovering = true;
    private volatile boolean keepSharing = true;
    private volatile boolean keepMonitoringFolder = false;

    // unify owners for same fileHash of a given FoundFile
    private final Map<String, FoundFile> foundMap;
    // track downloads and keep them at 100% if/when done
    private final Map<String, DownloadProgress> activeDownloads;

    public Node(String peerID, String ip, int port, boolean isDocker) {
        System.out.println("[Node] Constructor => " + peerID + " " + ip + " " + port
                + " (Docker=" + isDocker + ")");

        this.self = new Peer(peerID, ip, port);
        this.peerMgr = new PeerMgr();
        this.fileMgr = new FileMgr();
        this.isDockerMode = isDocker;

        this.foundMap = new ConcurrentHashMap<>();
        this.activeDownloads = new ConcurrentHashMap<>();

        if (isDocker) {
            // Docker => /test/shared, /test/downloads
            sharedFolder = new File("/test/shared");
            downloadFolder = new File("/test/downloads");
            sharedFolder.mkdirs();
            downloadFolder.mkdirs();

            // refresh local files
            fileMgr.refreshSharedFolder(sharedFolder);
            File[] files = sharedFolder.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.isFile()) {
                        self.addSharedFile(f);
                    }
                }
            }
            System.out.println("[Node] Docker => local shared => "
                    + fileMgr.getSharedFiles().size() + " file(s).");
        } else {
            System.out.println("[Node] GUI => user sets shared/download folder via setSharedFolder(...)");
            sharedFolder = null;
            downloadFolder = null;
        }
    }

    public void startServer() {
        System.out.println("[Node] startServer()");
        new Thread(() -> FileServer.startServer(self.getPort(), this)).start();
    }

    public void startPeerDiscovery() {
        System.out.println("[Node] startPeerDiscovery()");
        Thread discThread = new Thread(() -> {
            while (keepDiscovering) {
                try {
                    peerMgr.clearPeers();
                    peerMgr.discoverPeers(self.getIP(), self.getPort());
                    // after discovery, unify found
                    updateFoundMapFromPeers();
                    Thread.sleep(5000);
                }
                catch (InterruptedException e) {
                    break;
                }
            }
        });
        discThread.setDaemon(true);
        discThread.start();
    }

    public void stopPeerDiscovery() {
        System.out.println("[Node] stopPeerDiscovery()");
        keepDiscovering = false;
    }

    public void startFileSharing() {
        System.out.println("[Node] startFileSharing() => isDocker=" + isDockerMode);
        if (isDockerMode) {
            Thread autoThread = new Thread(() -> {
                while (keepSharing) {
                    try {
                        // check foundMap => for each found, if we don't have it, or not in activeDownloads => auto download
                        for (FoundFile ff : foundMap.values()) {
                            boolean haveLocal = (fileMgr.getFileMetaDataByHash(ff.fileHash) != null);
                            boolean isDownloading = activeDownloads.containsKey(ff.fileHash);
                            if (!haveLocal && !isDownloading) {
                                System.out.println("[Node] Docker auto-downloading => "
                                        + ff.fileName + " (hash=" + ff.fileHash + ")");
                                multiSourceDownload(ff.fileHash, ff.fileName);
                            }
                        }
                        Thread.sleep(5000);
                    }
                    catch (InterruptedException e) {
                        break;
                    }
                }
            });
            autoThread.setDaemon(true);
            autoThread.start();
        }
        // In GUI mode, do nothing. We will trigger multiSourceDownload from the UI.
    }

    public void stopFileSharing() {
        System.out.println("[Node] stopFileSharing()");
        keepSharing = false;
    }

    public void startLocalFolderMonitor() {
        if (!isDockerMode || sharedFolder == null) return;
        System.out.println("[Node] startLocalFolderMonitor() => Docker auto-refresh /test/shared");

        keepMonitoringFolder = true;
        Thread mon = new Thread(() -> {
            while (keepMonitoringFolder) {
                try {
                    Thread.sleep(5000);
                    fileMgr.refreshSharedFolder(sharedFolder);
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
                    break;
                }
            }
        });
        mon.setDaemon(true);
        mon.start();
    }

    public void stopLocalFolderMonitor() {
        System.out.println("[Node] stopLocalFolderMonitor()");
        keepMonitoringFolder = false;
    }

    private void updateFoundMapFromPeers() {
        for (Peer p : peerMgr.getAllPeers()) {
            for (File pseudoFile : p.getSharedFiles()) {
                String combined = pseudoFile.getName(); // "hash_filename"
                int idx = combined.indexOf('_');
                if (idx < 0) continue;

                String fileHash = combined.substring(0, idx);
                String fileName = combined.substring(idx + 1);

                // skip if we have it locally
                if (fileMgr.getFileMetaDataByHash(fileHash) != null) {
                    continue;
                }
                FoundFile ff = foundMap.get(fileHash);
                if (ff == null) {
                    ff = new FoundFile(fileHash, fileName);
                    foundMap.put(fileHash, ff);
                }
                ff.owners.add(p.getIP());
            }
        }
    }

    public void multiSourceDownload(String fileHash, String fileName) {
        System.out.println("[Node] multiSourceDownload => " + fileName + " " + fileHash);

        // create or get existing
        DownloadProgress dp = activeDownloads.get(fileHash);
        if (dp == null) {
            dp = new DownloadProgress(fileHash, fileName);
            activeDownloads.put(fileHash, dp);
        }

        // find owners from foundMap
        FoundFile ff = foundMap.get(fileHash);
        if (ff == null) {
            System.err.println("[Node] multiSourceDownload => No foundFile record for " + fileHash);
            return;
        }
        List<String> owners = new ArrayList<>(ff.owners);
        if (owners.isEmpty()) {
            System.err.println("[Node] No owners => " + fileHash);
            return;
        }

        // get file size from first responding owner
        long fileSize = 0;
        for (String ownerIP : owners) {
            long size = FileClient.requestFileSizeByHash(ownerIP, 4113, fileHash);
            if (size > 0) {
                fileSize = size;
                break;
            }
        }
        if (fileSize <= 0) {
            System.err.println("[Node] Could not get file size => " + fileHash);
            return;
        }
        dp.totalSize = fileSize;

        long chunkSize = 256L * 1024L;
        long totalChunks = (fileSize + chunkSize - 1) / chunkSize;

        File tempFolder = new File(downloadFolder, "temp_" + fileHash);
        tempFolder.mkdirs();

        System.out.println("[Node] Will download " + totalChunks
                + " chunk(s) => " + fileName + " from " + owners.size() + " owners.");

        for (int i = 0; i < totalChunks; i++) {
            long offset = i * chunkSize;
            long csize = Math.min(chunkSize, fileSize - offset);

            // simple round-robin
            String chosenIP = owners.get(i % owners.size());
            File chunkFile = new File(tempFolder, "chunk_" + i);

            boolean success = FileClient.downloadChunk(
                    chosenIP, 4113, fileHash, offset, csize,
                    chunkFile
            );
            if (!success) {
                System.err.println("[Node] chunk " + i + " failed from " + chosenIP);
            } else {
                dp.downloadedBytes += csize;
            }
        }

        reassembleFile(fileHash, fileName, fileSize, tempFolder);
        dp.isComplete = true; // keep in active map at 100%
    }

    private void reassembleFile(String fileHash, String fileName, long fileSize, File tempFolder) {
        File finalFile = new File(downloadFolder, fileName);
        long written = 0;
        int chunkIndex = 0;

        try (FileOutputStream fos = new FileOutputStream(finalFile)) {
            while (written < fileSize) {
                File cf = new File(tempFolder, "chunk_" + chunkIndex);
                if (!cf.exists()) {
                    throw new IOException("Missing chunk => " + cf.getAbsolutePath());
                }
                try (FileInputStream fis = new FileInputStream(cf)) {
                    byte[] buf = new byte[256 * 1024];
                    int read;
                    while ((read = fis.read(buf)) != -1) {
                        fos.write(buf, 0, read);
                        written += read;
                    }
                }
                chunkIndex++;
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        // verify final
        try {
            String calc = FileTransferMgr.calculateFileHash(finalFile);
            if (!calc.equals(fileHash)) {
                System.err.println("[Node] Hash mismatch => expected=" + fileHash + ", got=" + calc);
            } else {
                System.out.println("[Node] Finished => " + fileName);
            }
        }
        catch (IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
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

    public List<FoundFile> getFoundFiles() {
        return new ArrayList<>(foundMap.values());
    }

    public List<DownloadProgress> listActiveDownloads() {
        return new ArrayList<>(activeDownloads.values());
    }

    // Searching found
    public List<FoundFile> searchFoundFiles(String keyword) {
        keyword = keyword.toLowerCase();
        List<FoundFile> results = new ArrayList<>();
        for (FoundFile ff : foundMap.values()) {
            if (ff.fileName.toLowerCase().contains(keyword)) {
                results.add(ff);
            }
        }
        return results;
    }

    public List<DownloadProgress> searchDownloads(String keyword) {
        keyword = keyword.toLowerCase();
        List<DownloadProgress> results = new ArrayList<>();
        for (DownloadProgress dp : activeDownloads.values()) {
            if (dp.fileName.toLowerCase().contains(keyword)) {
                results.add(dp);
            }
        }
        return results;
    }

    public void setSharedFolder(String path) {
        File sf = new File(path);
        if (!sf.exists() || !sf.isDirectory()) {
            throw new IllegalArgumentException("Invalid shared => " + path);
        }
        this.sharedFolder = sf;
        fileMgr.refreshSharedFolder(sf);
        File[] files = sf.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isFile()) {
                    self.addSharedFile(f);
                }
            }
        }
    }

    public void setDownloadFolder(String path) {
        File df = new File(path);
        df.mkdirs();
        this.downloadFolder = df;
        System.out.println("[Node] Download folder => " + df.getAbsolutePath());
    }

    public File getSharedFolder() { return sharedFolder; }
    public File getDownloadFolder() { return downloadFolder; }
    public PeerMgr getPeerManager() { return peerMgr; }

    public FileMgr getFileManager() {
        return fileMgr;
    }

    public Peer getSelf() {
        return self;
    }
}