package p2p;

import java.util.ArrayList;
import java.util.List;

class FileMetaData {
    private final String fileName;
    private final long fileSize;
    private final List<Peer> peersWithFile;

    public FileMetaData(String fileName, long fileSize) {
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.peersWithFile = new ArrayList<>();
    }
    // Getters
    public String getFileName() { return fileName; }
    public long getFileSize() { return fileSize; }
    public List<Peer> getPeersWithFile() { return peersWithFile; }
    public void addOwnerPeer(Peer peer) { peersWithFile.add(peer); }
}