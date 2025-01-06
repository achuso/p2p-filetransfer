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

    private final Set<File> excludedFolders;
    private final Set<String> excludedMasks;

    public Peer(String peerID, String ip, int port) {
        this.peerID = peerID;
        this.ip = ip;
        this.port = port;
        this.sharedFiles = new ArrayList<>();
        this.excludedFolders = new HashSet<>();
        this.excludedMasks = new HashSet<>();
    }

    public void addSharedFile(File file) {
        if (isExcluded(file)) {
            System.out.println("Peer excludes file: " + file.getName());
            return;
        }
        sharedFiles.add(file);
    }

    private boolean isExcluded(File file) {
        for (File folder : excludedFolders) {
            if (file.getAbsolutePath().startsWith(folder.getAbsolutePath())) {
                return true;
            }
        }
        for (String mask : excludedMasks) {
            if (matchesMask(file.getName(), mask)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesMask(String fileName, String mask) {
        String regex = mask.replace("*", ".*").replace(".", "\\.");
        return fileName.matches(regex);
    }

    public String getPeerID()   { return peerID; }
    public String getIP()       { return ip; }
    public int getPort()        { return port; }
    public List<File> getSharedFiles()      { return sharedFiles; }
}