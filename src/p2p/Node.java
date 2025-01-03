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
            return Math.min(p, 100.0);
        }
    }

    private final Peer self;
    private final PeerMgr peerMgr;
    private final FileMgr fileMgr;

    private File sharedFolder;
    private File downloadFolder;
    private final boolean isDockerMode;

    private volatile boolean keepDiscovering = true;
    private volatile boolean keepSharing = true;
    private volatile boolean keepMonitoringFolder = false;

    private final Map<String, FoundFile> foundMap;
    private final Map<String, DownloadProgress> activeDownloads;

    // referring to the server thread so we can stop it
    private FileServer serverInstance;

    public Node(String peerID, String ip, int port, boolean isDocker) {
        this.self = new Peer(peerID, ip, port);
        this.peerMgr = new PeerMgr();
        this.fileMgr = new FileMgr();
        this.isDockerMode = isDocker;

        this.foundMap = new ConcurrentHashMap<>();
        this.activeDownloads = new ConcurrentHashMap<>();

        if (isDocker) {
            sharedFolder = new File("/test/shared");
            downloadFolder = new File("/test/downloads");
            sharedFolder.mkdirs();
            downloadFolder.mkdirs();

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
        else {
            sharedFolder = null;
            downloadFolder = null;
        }
    }

    public void startServer() {
        System.out.println("[Node] startServer()");
        serverInstance = new FileServer(null, this);
        new Thread(() -> FileServer.startServer(self.getPort(), this)).start();
    }

    public void stopServer() {
        System.out.println("[Node] stopServer()");
        FileServer.stopServer(); // calls serverSocket.close()
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
        // in containers: auto-download
        // in gui: pick found files for download
        if (isDockerMode) {
            new Thread(() -> {
                while (keepSharing) {
                    try {
                        for (FoundFile ff : foundMap.values()) {
                            boolean haveLocal = (fileMgr.getFileMetaDataByHash(ff.fileHash) != null);
                            boolean isDownloading = activeDownloads.containsKey(ff.fileHash);
                            if (!haveLocal && !isDownloading) {
                                System.out.println("[Node] Docker => auto-download => " + ff.fileName);
                                multiSourceDownload(ff.fileHash, ff.fileName);
                            }
                        }
                        Thread.sleep(5000);
                    }
                    catch (InterruptedException e) { break; }
                }
            }).start();
        }
        else {
            System.out.println("[Node] startFileSharing() => GUI mode => do nothing. user triggers multiSourceDownload.");
        }
    }

    public void stopFileSharing() {
        keepSharing = false;
    }

    public void startLocalFolderMonitor() {
        System.out.println("[Node] startLocalFolderMonitor => auto-refresh for new files, Docker or GUI.");
        keepMonitoringFolder = true;
        if (sharedFolder == null) {
            System.out.println("[Node] sharedFolder is null => ignoring");
            return;
        }
        new Thread(() -> {
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
                catch (InterruptedException e) { break; }
            }
        }).start();
    }

    public void stopLocalFolderMonitor() {
        keepMonitoringFolder = false;
    }

    private void updateFoundMapFromPeers() {
        for (Peer p : peerMgr.getAllPeers()) {
            for (File pseudoFile : p.getSharedFiles()) {
                String combined = pseudoFile.getName();
                int idx = combined.indexOf('_');
                if (idx < 0) continue;

                String hash = combined.substring(0, idx);
                String name = combined.substring(idx+1);

                if (fileMgr.getFileMetaDataByHash(hash) != null) {
                    continue; // we have it
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

    public void multiSourceDownload(String fileHash, String fileName) {
        System.out.println("[Node] multiSourceDownload => " + fileName + " " + fileHash);

        // create or get
        DownloadProgress dp = activeDownloads.get(fileHash);
        if (dp == null) {
            dp = new DownloadProgress(fileHash, fileName);
            activeDownloads.put(fileHash, dp);
        }

        FoundFile ff = foundMap.get(fileHash);
        if (ff == null) {
            System.err.println("[Node] no foundFile for hash => " + fileHash);
            return;
        }
        List<String> owners = new ArrayList<>(ff.owners);
        if (owners.isEmpty()) {
            System.err.println("[Node] no owners => " + fileHash);
            return;
        }

        // get size from first responding owner
        long fileSize = 0;
        for (String ip : owners) {
            long size = FileClient.requestFileSizeByHash(ip, 4113, fileHash);
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

        long chunkSize = 256 * 1024;
        long totalChunks = (fileSize + chunkSize - 1) / chunkSize;

        File tempFolder = new File(downloadFolder, "temp_" + fileHash);
        tempFolder.mkdirs();

        for (int i=0; i<totalChunks; i++) {
            long offset = i*chunkSize;
            long csize = Math.min(chunkSize, fileSize - offset);

            String chosenIP = owners.get(i % owners.size());
            File chunkFile = new File(tempFolder, "chunk_" + i);

            boolean ok = FileClient.downloadChunk(chosenIP, 4113, fileHash, offset, csize, chunkFile);
            if (ok) {
                dp.downloadedBytes += csize;
            }
            else {
                System.err.println("[Node] chunk " + i + " fail from " + chosenIP);
            }
        }

        reassembleFile(fileHash, fileName, fileSize, tempFolder);
        dp.isComplete = true; // keep it in active
    }

    private void reassembleFile(String hash, String name, long size, File tempFolder) {
        File finalFile = new File(downloadFolder, name);
        long written = 0;
        int chunkIndex = 0;

        try (FileOutputStream fos = new FileOutputStream(finalFile)) {
            while (written < size) {
                File cf = new File(tempFolder, "chunk_" + chunkIndex);
                if (!cf.exists()) throw new IOException("Missing chunk => " + cf);

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

        // check hash
        try {
            String calc = FileTransferMgr.calculateFileHash(finalFile);
            if (!calc.equals(hash)) {
                System.err.println("[Node] hash mismatch => expected=" + hash + ", got=" + calc);
            } else {
                System.out.println("[Node] file done => " + name);
            }
        }
        catch (IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        // cleanup
        for (File f : Objects.requireNonNull(tempFolder.listFiles())) {
            f.delete();
        }
        tempFolder.delete();
    }

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
    }

    public File getSharedFolder() {
        return sharedFolder;
    }

    public File getDownloadFolder() {
        return downloadFolder;
    }

}