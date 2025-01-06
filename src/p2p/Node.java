package p2p;

import network.FileServer;

import java.io.*;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Node {
    public static class FoundFile {
        public final String fileHash;
        public final String fileName;
        public final Set<String> owners;

        public FoundFile(String hash, String name) {
            this.fileHash = hash;
            this.fileName = name;
            this.owners = new HashSet<>();
        }
    }

    public static class DownloadProgress {
        public final String fileHash;
        public final String fileName;

        public long totalSize;
        public long downloadedBytes;
        public boolean isComplete;

        public DownloadProgress(String fileHash, String fileName) {
            this.fileHash = fileHash;
            this.fileName = fileName;
            this.totalSize = 0L;
            this.downloadedBytes = 0L;
            this.isComplete = false;
        }

        public double getPercent() {
            if (totalSize <= 0) return 0.0;
            double pct = (downloadedBytes * 100.0) / totalSize;
            return Math.min(pct, 100.0);
        }
    }

    // Exclusion logic
    private final Set<File> excludedFolders;
    private final Set<String> excludedMasks;
    private boolean checkRootOnly;

    // Node fields
    private final Peer self;
    private final PeerMgr peerMgr;
    private final FileMgr fileMgr;

    private File sharedFolder;
    private File downloadFolder;
    private final boolean isDockerMode;

    // Background controls
    private volatile boolean keepDiscovering = true;
    private volatile boolean keepSharing = true;
    private volatile boolean keepMonitoringFolder = false;

    private final Map<String, FoundFile> foundMap;
    private final Map<String, DownloadProgress> activeDownloads;

    public Node(String peerID, String ip, int port, boolean isDocker) {
        this.self = new Peer(peerID, ip, port);
        this.peerMgr = new PeerMgr();
        this.fileMgr = new FileMgr();
        this.isDockerMode = isDocker;

        this.foundMap = new ConcurrentHashMap<>();
        this.activeDownloads = new ConcurrentHashMap<>();

        this.excludedFolders = new HashSet<>();
        this.excludedMasks   = new HashSet<>();
        this.checkRootOnly   = false;

        if (isDocker) {
            // Defaults for docker: /test/shared, /test/downloads
            sharedFolder   = new File("/test/shared");
            downloadFolder = new File("/test/downloads");
            sharedFolder.mkdirs();
            downloadFolder.mkdirs();

            refreshSharedFolderWithExclusions(sharedFolder);
            self.getSharedFiles().clear();

            if (checkRootOnly) {
                addTopLevelFiles(sharedFolder);
            }
            else {
                for (FileMetaData md : fileMgr.getSharedFiles()) {
                    self.addSharedFile(md.getFile());
                }
            }
        }
        else {
            sharedFolder = null;
            downloadFolder = null;
        }
    }

    private void refreshSharedFolderWithExclusions(File root) {
        fileMgr.getSharedFiles().clear();
        if (root == null || !root.isDirectory()) return;

        if (checkRootOnly) {
            File[] top = root.listFiles();
            if (top == null) return;
            for (File f : top) {
                if (f.isDirectory()) {}
                else {
                    if (shouldIncludeLocalFile(f)) {
                        fileMgr.addSharedFile(f);
                    }
                }
            }
        }
        else {
            recursiveScan(root);
        }
    }

    private void recursiveScan(File folder) {
        if (isFolderExcluded(folder)) {
            return;
        }

        File[] all = folder.listFiles();
        if (all == null) return;

        for (File file : all) {
            if (file.isDirectory()) {
                recursiveScan(file);
            }
            else {
                if (shouldIncludeLocalFile(file)) {
                    fileMgr.addSharedFile(file);
                }
            }
        }
    }

    private boolean isFolderExcluded(File folder) {
        String folderPath = folder.getAbsolutePath();
        for (File ex : excludedFolders) {
            String exPath = ex.getAbsolutePath();
            if (folderPath.equals(exPath) || folderPath.startsWith(exPath + File.separator)) {
                return true;
            }
        }
        return false;
    }

    private boolean shouldIncludeLocalFile(File file) {
        if (isFolderExcluded(file.getParentFile())) {
            return false;
        }
        String lower = file.getName().toLowerCase();
        for (String mask : excludedMasks) {
            if (matchesMask(lower, mask)) {
                return false;
            }
        }
        return true;
    }

    private void addTopLevelFiles(File root) {
        File[] top = root.listFiles();
        if (top == null) return;
        for (File file : top) {
            if (!file.isDirectory() && shouldIncludeLocalFile(file)) {
                self.addSharedFile(file);
            }
        }
    }

    // Experiment with the mask further
    private boolean matchesMask(String fileName, String mask) {
        String regex = mask.replace(".", "\\.").replace("*", ".*");
        return fileName.matches(regex);
    }

    public void setCheckRootOnly(boolean val) {
        this.checkRootOnly = val;
    }
    public boolean isCheckRootOnly() {
        return checkRootOnly;
    }

    public void addExcludedFolder(File folder) {
        if (sharedFolder == null
                || !folder.getAbsolutePath().startsWith(sharedFolder.getAbsolutePath())) {
            return;
        }
        else {
            excludedFolders.add(folder);
        }
    }

    public void removeExcludedFolder(File folder) {
        excludedFolders.remove(folder);
    }

    public void addExcludedMask(String mask) {
        excludedMasks.add(mask.toLowerCase());
    }

    public void removeExcludedMask(String mask) {
        excludedMasks.remove(mask.toLowerCase());
    }

    public void applyExclusionsNow() {
        // 1) re-check local
        if (sharedFolder != null && sharedFolder.isDirectory()) {
            fileMgr.getSharedFiles().clear();
            self.getSharedFiles().clear();

            refreshSharedFolderWithExclusions(sharedFolder);

            if (checkRootOnly) {
                addTopLevelFiles(sharedFolder);
            }
            else {
                for (FileMetaData metadata : fileMgr.getSharedFiles()) {
                    self.addSharedFile(metadata.getFile());
                }
            }
        }
        // 2) re-check found
        Iterator<Map.Entry<String, FoundFile>> iter = foundMap.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, FoundFile> e = iter.next();
            FoundFile foundFile = e.getValue();
            if (shouldIncludePseudoFile(foundFile.fileName)) {
                iter.remove();
            }
        }
    }

    private boolean shouldIncludePseudoFile(String fileName) {
        if (checkRootOnly) {
            if (fileName.contains("/") || fileName.contains("\\")) {
                return true;
            }
        }
        String lower = fileName.toLowerCase();
        for (String mask : excludedMasks) {
            if (matchesMask(lower, mask)) {
                return true;
            }
        }
        return false;
    }

    public void startServer() {
        new Thread(() -> FileServer.startServer(self.getPort(), this)).start();
    }

    public void stopServer() {
        FileServer.stopServer();
    }

    public void startPeerDiscovery() {
        keepDiscovering = true;
        new Thread(() -> {
            while (keepDiscovering) {
                try {
                    peerMgr.clearPeers();
                    peerMgr.discoverPeers(self.getIP(), self.getPort());
                    updateFoundMapFromPeers();
                    Thread.sleep(5000);
                }
                catch (InterruptedException e) { break; }
            }
        }).start();
    }

    public void stopPeerDiscovery() {
        keepDiscovering = false;
    }

    public void startFileSharing() {
        keepSharing = true;
        if (isDockerMode) {
            new Thread(() -> {
                while (keepSharing) {
                    try {
                        for (FoundFile ff : foundMap.values()) {
                            boolean haveLocal = (fileMgr.getFileMetaDataByHash(ff.fileHash) != null);
                            boolean isDownloading = activeDownloads.containsKey(ff.fileHash);
                            if (!haveLocal && !isDownloading) {
                                multiSourceDownload(ff.fileHash, ff.fileName);
                            }
                        }
                        Thread.sleep(5000);
                    }
                    catch (InterruptedException e) { break; }
                }
            }).start();
        }
    }

    public void stopFileSharing() {
        keepSharing = false;
    }

    public void startLocalFolderMonitor() {
        keepMonitoringFolder = true;
        if (sharedFolder == null) return;
        new Thread(() -> {
            while (keepMonitoringFolder) {
                try {
                    Thread.sleep(5000);

                    fileMgr.getSharedFiles().clear();
                    self.getSharedFiles().clear();

                    refreshSharedFolderWithExclusions(sharedFolder);

                    if (checkRootOnly) {
                        addTopLevelFiles(sharedFolder);
                    }
                    else {
                        for (FileMetaData md : fileMgr.getSharedFiles()) {
                            self.addSharedFile(md.getFile());
                        }
                    }
                }
                catch (InterruptedException e) { break; }
            }
        }).start();
    }

    public void stopLocalFolderMonitor() {
        keepMonitoringFolder = false;
    }

    public void disconnect() {
        System.out.println("Node disconnected, hold the presses!");
        stopPeerDiscovery();
        stopFileSharing();
        stopLocalFolderMonitor();
        stopServer();

        // Clear local data
        self.getSharedFiles().clear();
        foundMap.clear();
        activeDownloads.clear();
        fileMgr.getSharedFiles().clear();
    }

    private void updateFoundMapFromPeers() {
        for (Peer peer : peerMgr.getAllPeers()) {
            for (File pseudoFile : peer.getSharedFiles()) {
                String combined = pseudoFile.getName();
                int index = combined.indexOf('_');
                if (index < 0) continue;

                String hash = combined.substring(0, index);
                String name = combined.substring(index + 1);

                if (shouldIncludePseudoFile(name)) {
                    continue;
                }
                if (fileMgr.getFileMetaDataByHash(hash) != null) {
                    continue;
                }

                FoundFile foundFile = foundMap.get(hash);
                if (foundFile == null) {
                    foundFile = new FoundFile(hash, name);
                    foundMap.put(hash, foundFile);
                }
                foundFile.owners.add(peer.getIP());
            }
        }
    }

    public void multiSourceDownload(String fileHash, String fileName) {
        DownloadProgress progress = activeDownloads.get(fileHash);
        if (progress == null) {
            progress = new DownloadProgress(fileHash, fileName);
            activeDownloads.put(fileHash, progress);
        }
        FoundFile foundFile = foundMap.get(fileHash);
        if (foundFile == null) {
            return;
        }
        List<String> owners = new ArrayList<>(foundFile.owners);
        if (owners.isEmpty()) {
            return;
        }

        long fileSize = 0;
        for (String ip : owners) {
            long s = network.FileClient.requestFileSizeByHash(ip, 4113, fileHash);
            if (s > 0) {
                fileSize = s;
                break;
            }
        }
        if (fileSize <= 0) {
            return;
        }
        progress.totalSize = fileSize;

        long chunkSize = 256L * 1024L;
        long totalChunks = (fileSize + chunkSize - 1) / chunkSize;

        File tempFolder = new File(downloadFolder, "temp_" + fileHash);
        tempFolder.mkdirs();

        for (int i = 0; i < totalChunks; i++) {
            long offset = i * chunkSize;
            long csize = Math.min(chunkSize, fileSize - offset);

            String chosenIP = owners.get(i % owners.size());
            File chunkFile = new File(tempFolder, "chunk_" + i);

            boolean isOK = network.FileClient.downloadChunk(chosenIP, 4113, fileHash, offset, csize, chunkFile);
            if (isOK) {
                progress.downloadedBytes += csize;
            }
        }
        reassembleFile(fileHash, fileName, fileSize, tempFolder);
        progress.isComplete = true;
    }

    private void reassembleFile(String hash, String name, long size, File tempFolder) {
        File finalFile = new File(downloadFolder, name);
        long written = 0;
        int chunkIndex = 0;

        try (FileOutputStream fos = new FileOutputStream(finalFile)) {
            while (written < size) {
                File cf = new File(tempFolder, "chunk_" + chunkIndex);
                if (!cf.exists()) {
                    throw new IOException("Missing chunk => " + cf);
                }
                try (FileInputStream fis = new FileInputStream(cf)) {
                    byte[] buf = new byte[256*1024];
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
            System.err.println(e.getMessage());
        }

        try {
            String calc = p2p.FileTransferMgr.calculateFileHash(finalFile);
            if (!calc.equals(hash)) {
                System.err.println("HASH MISMATCH: expected " + hash + " got " + calc);
            }
        }
        catch (IOException | NoSuchAlgorithmException e) {
            System.err.println(e.getMessage());
        }

        File[] leftover = tempFolder.listFiles();
        if (leftover != null) {
            for (File f : leftover) {
                f.delete();
            }
        }
        tempFolder.delete();
    }

    public List<FoundFile> getFoundFiles() { return new ArrayList<>(foundMap.values()); }
    public List<DownloadProgress> listActiveDownloads() { return new ArrayList<>(activeDownloads.values()); }
    public FileMgr getFileManager() { return fileMgr; }
    public Peer getSelf() { return self; }

    public void setSharedFolder(String path) {
        File sharedFolder = new File(path);
        if (!sharedFolder.exists() || !sharedFolder.isDirectory()) {
            throw new IllegalArgumentException("Invalid shared => " + path);
        }
        this.sharedFolder = sharedFolder;

        fileMgr.getSharedFiles().clear();
        self.getSharedFiles().clear();

        refreshSharedFolderWithExclusions(this.sharedFolder);

        if (checkRootOnly) {
            addTopLevelFiles(this.sharedFolder);
        }
        else {
            for (FileMetaData metadata : fileMgr.getSharedFiles()) {
                self.addSharedFile(metadata.getFile());
            }
        }
    }

    public void setDownloadFolder(String path) {
        File df = new File(path);
        df.mkdirs();
        downloadFolder = df;
    }

    public File getSharedFolder() { return sharedFolder; }

    public File getDownloadFolder() { return downloadFolder; }
}