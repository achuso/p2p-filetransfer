package p2p;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Peer {
    private final String peerID;
    private final String ip;
    private final int port;

    private final List<File> sharedFiles;
    private final List<File> downloadedFiles;

    private final Set<File> excludedFolders;
    private final Set<String> excludedMasks;

    public Peer(String peerID, String ip, int port) {
        this.peerID = peerID;
        this.ip = ip;
        this.port = port;
        this.sharedFiles = new ArrayList<>();
        this.downloadedFiles = new ArrayList<>();
        this.excludedFolders = new HashSet<>();
        this.excludedMasks = new HashSet<>();
    }

    public void addSharedFile(File file) {
        if (isExcluded(file)) {
            System.out.println("[Peer] Excluded => " + file.getName());
            return;
        }
        sharedFiles.add(file);
    }

    private boolean isExcluded(File f) {
        for (File folder : excludedFolders) {
            if (f.getAbsolutePath().startsWith(folder.getAbsolutePath())) {
                return true;
            }
        }
        for (String mask : excludedMasks) {
            if (matchesMask(f.getName(), mask)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesMask(String fileName, String mask) {
        String regex = mask.replace("*", ".*").replace(".", "\\.");
        return fileName.matches(regex);
    }

    public void excludeFolder(File folder) {
        excludedFolders.add(folder);
    }

    public void excludeMask(String mask) {
        excludedMasks.add(mask);
    }

    public void addDownloadedFile(File file) {
        downloadedFiles.add(file);
    }

    public String getPeerID()   { return peerID; }
    public String getIP()       { return ip; }
    public int getPort()        { return port; }

    public List<File> getSharedFiles()      { return sharedFiles; }
    public List<File> getDownloadedFiles()  { return downloadedFiles; }
}