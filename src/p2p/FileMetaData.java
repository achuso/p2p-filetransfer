package p2p;

import java.io.File;

public class FileMetaData {
    private final String fileName;
    private final long fileSize;
    private final File file;
    private final String fileHash;

    public FileMetaData(String fileName, long fileSize, File file, String fileHash) {
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.file = file;
        this.fileHash = fileHash;
    }

    public String getFileName()         { return fileName; }
    public long getFileSize()           { return fileSize; }
    public File getFile()               { return file; }
    public String getFileHash()         { return fileHash; }
}