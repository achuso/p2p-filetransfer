package p2p;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FileMetaData {
    private final String fileName;
    private final long fileSize;
    private final File file;
    private final List<Peer> peersWithFile;

    public FileMetaData(String fileName, long fileSize, File file) {
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.file = file;
        this.peersWithFile = new ArrayList<>();
    }

    public void addOwnerPeer(Peer peer) {
        peersWithFile.add(peer);
    }

    public String getFileName() { return fileName; }
    public long getFileSize() { return fileSize; }
    public File getFile() { return file; }
    public List<Peer> getPeersWithFile() { return peersWithFile; }
}