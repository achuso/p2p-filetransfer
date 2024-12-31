package p2p;

import java.io.File;
import java.util.*;

public class FileMgr {
    private final Map<String, FileMetaData> sharedFiles;
    private final Map<String, FileMetaData> downloadingFiles;

    public FileMgr() {
        this.sharedFiles = new HashMap<>();
        this.downloadingFiles = new HashMap<>();
    }

    public void addSharedFile(File file) {
        String filename = file.getName();
        FileMetaData metadata = new FileMetaData(filename, file.length(), file);
        sharedFiles.put(filename, metadata);
    }

    public void addDownloadingFile(FileMetaData fileMetadata) {
        downloadingFiles.put(fileMetadata.getFileName(), fileMetadata);
    }

    public void refreshSharedFolder(File sharedFolder) {
        sharedFiles.clear();
        for (File file : Objects.requireNonNull(sharedFolder.listFiles())) {
            if (!file.isDirectory()) {
                addSharedFile(file);
            }
        }
    }

    public void listSharedFiles() {
        if (sharedFiles.isEmpty()) {
            System.out.println("No files shared.");
        }
        else {
            System.out.println("Shared files:");
            for (String fileName : sharedFiles.keySet()) {
                System.out.println("- " + fileName);
            }
        }
    }

    public List<FileMetaData> getSharedFiles() { return new ArrayList<>(sharedFiles.values()); }
    public Map<String, FileMetaData> getDownloadingFiles() { return downloadingFiles; }
    public FileMetaData getFileMetaData(String fileName) { return sharedFiles.get(fileName); }
}