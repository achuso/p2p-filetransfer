package p2p;

import java.io.*;
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

class Peer {
    private final String peerID;
    private final String ip;
    private final int port;
    private final List<File> sharedFiles;
    private final List<File> downloadedFiles;

    // Filters
    private final Set<File> excludedFolders;
    private final Set<String> excludedFileMasks;

    public Peer(String peerID, String ip, int port) {
        this.peerID = peerID;
        this.ip = ip;
        this.port = port;
        this.sharedFiles = new ArrayList<>();
        this.downloadedFiles = new ArrayList<>();
        this.excludedFolders = new HashSet<>();
        this.excludedFileMasks = new HashSet<>();
    }

    public void addSharedFile(File file) {
        if (isExcluded(file)) {
            System.out.println("File excluded: " + file.getName());
            return;
        }
        sharedFiles.add(file);
    }

    public void addDownloadedFile(File file) {
        downloadedFiles.add(file);
    }

    // Filters
    public void excludeFolder(File folder) { excludedFolders.add(folder); }
    public void excludeFileMask(String mask) { excludedFileMasks.add(mask); }

    private boolean isExcluded(File file) {
        // Check for excluded folders
        for (File folder : excludedFolders) {
            if (file.getAbsolutePath().startsWith(folder.getAbsolutePath())) {
                return true;
            }
        }

        // Check for excluded file mask
        for (String mask : excludedFileMasks) {
            if (matchesMask(file.getName(), mask)) { // Regex based matching
                return true;
            }
        }

        return false;
    }

    private boolean matchesMask(String fileName, String mask) {
        String regex = mask.replace("*", ".*").replace(".", "\\.");
        return fileName.matches(regex);
    }

    // Getters
    public String getPeerID() { return peerID; }
    public String getIP() { return ip; }
    public int getPort() { return port; }
    public List<File> getSharedFiles() { return sharedFiles; }
    public List<File> getDownloadedFiles() { return downloadedFiles; }
}