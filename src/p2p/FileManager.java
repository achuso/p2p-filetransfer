package p2p;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class FileManager {
    private final Map<String, FileMetaData> sharedFiles;
    private final Map<String, FileMetaData> downloadingFiles;

    public FileManager() {
        this.sharedFiles = new HashMap<>();
        this.downloadingFiles = new HashMap<>();
    }

    public void addSharedFile(File file) {
        String filename = file.getName();
        FileMetaData metadata = new FileMetaData(filename, file.length());
        sharedFiles.put(filename, metadata);
    }

    public void addDownloadingFile(FileMetaData fileMetadata) {
        downloadingFiles.put(fileMetadata.getFileName(), fileMetadata);
    }

    public List<FileMetaData> getSharedFiles() {
        return new ArrayList<>(sharedFiles.values());
    }

    public Map<String, FileMetaData> getDownloadingFiles() {
        return downloadingFiles;
    }

    public FileMetaData getFileMetadata(String fileName) {
        return sharedFiles.get(fileName);
    }
}