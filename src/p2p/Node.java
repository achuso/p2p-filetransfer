package p2p;

import network.FileClient;
import network.FileServer;

import java.io.*;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Node - Final
 *  1) Folder exclusion works properly => subdirectories are included by default,
 *     but if user excludes "/shared/subdir1", that entire subtree is skipped.
 *  2) If user sets checkRootOnly = true => we only share top-level files in sharedFolder.
 *  3) "disconnect()" forcibly stops sharing, discovery, monitoring, & clears local data.
 */
public class Node {

    // -------------------------------------------------------------------------
    // FoundFile => merges owners by fileHash
    // -------------------------------------------------------------------------
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

    // -------------------------------------------------------------------------
    // DownloadProgress => track single file’s progress
    // -------------------------------------------------------------------------
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
            return (pct > 100.0)? 100.0 : pct;
        }
    }

    // -------------------------------------------------------------------------
    // Exclusion logic
    // -------------------------------------------------------------------------
    private final Set<File> excludedFolders;  // subfolders to skip entirely
    private final Set<String> excludedMasks;  // e.g. *.mp3
    private boolean checkRootOnly;            // if true => only top-level files

    // Node fields
    private final Peer self;
    private final PeerMgr peerMgr;
    private final FileMgr fileMgr;

    private File sharedFolder;
    private File downloadFolder;
    private final boolean isDockerMode;

    // background controls
    private volatile boolean keepDiscovering = true;
    private volatile boolean keepSharing = true;
    private volatile boolean keepMonitoringFolder = false;

    // "found" => merges owners by hash
    private final Map<String, FoundFile> foundMap;
    // "active downloads" => keep them at 100%
    private final Map<String, DownloadProgress> activeDownloads;

    public Node(String peerID, String ip, int port, boolean isDocker) {
        this.self = new Peer(peerID, ip, port);
        this.peerMgr = new PeerMgr();
        this.fileMgr = new FileMgr();
        this.isDockerMode = isDocker;

        this.foundMap = new ConcurrentHashMap<>();
        this.activeDownloads = new ConcurrentHashMap<>();

        // Exclusions
        this.excludedFolders = new HashSet<>();
        this.excludedMasks   = new HashSet<>();
        this.checkRootOnly   = false; // by default we do subdirs

        if (isDocker) {
            // defaults => /test/shared, /test/downloads
            sharedFolder   = new File("/test/shared");
            downloadFolder = new File("/test/downloads");
            sharedFolder.mkdirs();
            downloadFolder.mkdirs();

            // initial scan
            refreshSharedFolderWithExclusions(sharedFolder);
            self.getSharedFiles().clear();

            if (checkRootOnly) {
                addTopLevelFiles(sharedFolder);
            } else {
                for (FileMetaData md : fileMgr.getSharedFiles()) {
                    self.addSharedFile(md.getFile());
                }
            }
        }
        else {
            sharedFolder   = null;
            downloadFolder = null;
        }
    }

    // -------------------------------------------------------------------------
    // 1) Folder scanning logic
    // -------------------------------------------------------------------------
    /**
     * Clears fileMgr, then either scans top-level only if rootOnly=true,
     * or recursively scans everything unless excluded.
     */
    private void refreshSharedFolderWithExclusions(File root) {
        fileMgr.getSharedFiles().clear();
        if (root == null || !root.isDirectory()) return;

        if (checkRootOnly) {
            // top-level only
            File[] top = root.listFiles();
            if (top == null) return;
            for (File f : top) {
                if (f.isDirectory()) {
                    // skip subdirs
                }
                else {
                    if (shouldIncludeLocalFile(f)) {
                        fileMgr.addSharedFile(f);
                    }
                }
            }
        }
        else {
            // recursive approach
            scanRecursively(root);
        }
    }

    /**
     * Recursively scan subfolders, skipping if the folder is in excludedFolders
     * or inside an excluded folder.
     */
    private void scanRecursively(File folder) {
        // if folder is exactly or inside an excluded folder => skip entire subtree
        if (isFolderExcluded(folder)) {
            // debug
            // System.out.println("Skipping => " + folder.getName() + " due to exclusion");
            return;
        }

        File[] all = folder.listFiles();
        if (all == null) return;

        for (File f : all) {
            if (f.isDirectory()) {
                scanRecursively(f);
            }
            else {
                if (shouldIncludeLocalFile(f)) {
                    fileMgr.addSharedFile(f);
                }
            }
        }
    }

    private boolean isFolderExcluded(File folder) {
        // If folder is inside any excluded folder => skip
        String folderPath = folder.getAbsolutePath();
        for (File ex : excludedFolders) {
            String exPath = ex.getAbsolutePath();
            if (folderPath.equals(exPath)) {
                return true;
            }
            // or if folder is inside ex
            if (folderPath.startsWith(exPath + File.separator)) {
                return true;
            }
        }
        return false;
    }

    private boolean shouldIncludeLocalFile(File f) {
        // if inside excluded folder => skip
        if (isFolderExcluded(f.getParentFile())) {
            return false;
        }
        // check file mask
        String lower = f.getName().toLowerCase();
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
        for (File f : top) {
            if (!f.isDirectory() && shouldIncludeLocalFile(f)) {
                self.addSharedFile(f);
            }
        }
    }

    private boolean matchesMask(String fileName, String mask) {
        String regex = mask.replace(".", "\\.").replace("*", ".*");
        return fileName.matches(regex);
    }

    // -------------------------------------------------------------------------
    // Exclusion API => used by centerPanel
    // -------------------------------------------------------------------------
    public void setCheckRootOnly(boolean val) {
        this.checkRootOnly = val;
    }
    public boolean isCheckRootOnly() {
        return checkRootOnly;
    }

    public void addExcludedFolder(File folder) {
        if (sharedFolder == null) return;
        // ensure the folder is inside shared
        if (!folder.getAbsolutePath().startsWith(sharedFolder.getAbsolutePath())) {
            System.err.println("Exclusion => " + folder
                    + " is not inside " + sharedFolder + " ignoring");
            return;
        }
        excludedFolders.add(folder);
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

    /**
     * Re-check local => skip newly excluded subdirs. Also re-check foundMap => skip newly excluded items.
     */
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
                for (FileMetaData md : fileMgr.getSharedFiles()) {
                    self.addSharedFile(md.getFile());
                }
            }
        }
        // 2) re-check found
        Iterator<Map.Entry<String, FoundFile>> it = foundMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, FoundFile> e = it.next();
            FoundFile ff = e.getValue();
            if (!shouldIncludePseudoFile(ff.fileName)) {
                it.remove();
            }
        }
    }

    // For discovered peer files => skip if checkRootOnly => the name has a slash, or if mask
    private boolean shouldIncludePseudoFile(String fileName) {
        if (checkRootOnly) {
            // if there's any slash => subdir => skip
            if (fileName.contains("/") || fileName.contains("\\")) {
                return false;
            }
        }
        // also skip if excluded mask
        String lower = fileName.toLowerCase();
        for (String mask : excludedMasks) {
            if (matchesMask(lower, mask)) {
                return false;
            }
        }
        return true;
    }

    // -------------------------------------------------------------------------
    // Start/Stop + "disconnect()" method
    // -------------------------------------------------------------------------
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

    /**
     * "disconnect()" => forcibly stops new or ongoing share/discovery,
     * clears local data, so user can't keep downloading or sharing.
     */
    public void disconnect() {
        System.out.println("[Node] disconnect() => forcibly stopping everything.");
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

    // -------------------------------------------------------------------------
    // Found logic => merges owners
    // -------------------------------------------------------------------------
    private void updateFoundMapFromPeers() {
        for (Peer p : peerMgr.getAllPeers()) {
            for (File pseudoFile : p.getSharedFiles()) {
                String combined = pseudoFile.getName();
                int idx = combined.indexOf('_');
                if (idx < 0) continue;

                String hash = combined.substring(0, idx);
                String name = combined.substring(idx + 1);

                if (!shouldIncludePseudoFile(name)) {
                    continue;
                }
                if (fileMgr.getFileMetaDataByHash(hash) != null) {
                    continue;
                }

                FoundFile ff = foundMap.get(hash);
                if (ff == null) {
                    ff = new FoundFile(hash, name);
                    foundMap.put(hash, ff);
                }
                ff.owners.add(p.getIP());
            }
        }
    }

    // -------------------------------------------------------------------------
    // Multi-source chunk logic => 256 KB
    // -------------------------------------------------------------------------
    public void multiSourceDownload(String fileHash, String fileName) {
        // If forcibly disconnected, you might skip. But let's assume user won't call after disconnect
        DownloadProgress dp = activeDownloads.get(fileHash);
        if (dp == null) {
            dp = new DownloadProgress(fileHash, fileName);
            activeDownloads.put(fileHash, dp);
        }
        FoundFile ff = foundMap.get(fileHash);
        if (ff == null) {
            return;
        }
        List<String> owners = new ArrayList<>(ff.owners);
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
        dp.totalSize = fileSize;

        long chunkSize = 256L * 1024L;
        long totalChunks = (fileSize + chunkSize - 1) / chunkSize;

        File tempFolder = new File(downloadFolder, "temp_" + fileHash);
        tempFolder.mkdirs();

        for (int i=0; i<totalChunks; i++) {
            long offset = i * chunkSize;
            long csize = Math.min(chunkSize, fileSize - offset);

            String chosenIP = owners.get(i % owners.size());
            File chunkFile = new File(tempFolder, "chunk_" + i);

            boolean ok = network.FileClient.downloadChunk(chosenIP, 4113, fileHash, offset, csize, chunkFile);
            if (ok) {
                dp.downloadedBytes += csize;
            }
        }
        reassembleFile(fileHash, fileName, fileSize, tempFolder);
        dp.isComplete = true;
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
            e.printStackTrace();
        }

        // verify final
        try {
            String calc = p2p.FileTransferMgr.calculateFileHash(finalFile);
            if (!calc.equals(hash)) {
                System.err.println("[Node] hash mismatch => expected=" + hash + ", got=" + calc);
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

    // -------------------------------------------------------------------------
    // Basic Access
    // -------------------------------------------------------------------------
    public List<FoundFile> getFoundFiles() {
        return new ArrayList<>(foundMap.values());
    }

    public List<DownloadProgress> listActiveDownloads() {
        return new ArrayList<>(activeDownloads.values());
    }

    public FileMgr getFileManager() {
        return fileMgr;
    }

    public Peer getSelf() {
        return self;
    }

    // Let user pick shared & download folders
    public void setSharedFolder(String path) {
        File sf = new File(path);
        if (!sf.exists() || !sf.isDirectory()) {
            throw new IllegalArgumentException("Invalid shared => " + path);
        }
        sharedFolder = sf;

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

    public void setDownloadFolder(String path) {
        File df = new File(path);
        df.mkdirs();
        downloadFolder = df;
    }

    public File getSharedFolder() {
        return sharedFolder;
    }

    public File getDownloadFolder() {
        return downloadFolder;
    }
}