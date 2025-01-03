package p2p;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FileMetaData {
    private final String fileName;
    private final long fileSize;
    private final File file;
    private final String fileHash;

    private final List<Peer> owners;

    public FileMetaData(String fileName, long fileSize, File file, String fileHash) {
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.file = file;
        this.fileHash = fileHash;
        this.owners = new ArrayList<>();
    }

    public String getFileName()         { return fileName; }
    public long getFileSize()           { return fileSize; }
    public File getFile()               { return file; }
    public String getFileHash()         { return fileHash; }

    public List<Peer> getOwners()       { return owners; }
    public void addOwnerPeer(Peer p)    { owners.add(p); }
}