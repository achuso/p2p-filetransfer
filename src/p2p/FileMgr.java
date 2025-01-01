package p2p;

import java.io.*;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class FileMgr {
    // Keyed by fileHash
    private final Map<String, FileMetaData> sharedFiles;
    private final Map<String, FileMetaData> downloadingFiles;

    public FileMgr() {
        this.sharedFiles = new HashMap<>();
        this.downloadingFiles = new HashMap<>();
    }

    public void addSharedFile(File file) {
        try {
            if (!file.exists() || file.isDirectory()) return;

            String fileHash = FileTransferMgr.calculateFileHash(file);

            if (!sharedFiles.containsKey(fileHash)) {
                FileMetaData metadata = new FileMetaData(file.getName(), file.length(), file, fileHash);
                sharedFiles.put(fileHash, metadata);
            }
        }
        catch (IOException | NoSuchAlgorithmException e) {
            System.err.println(e.getMessage());
        }
    }

    public void addDownloadingFile(FileMetaData fileMetadata) {
        downloadingFiles.put(fileMetadata.getFileHash(), fileMetadata);
    }

    public void refreshSharedFolder(File sharedFolder) {
        sharedFiles.clear();
        File[] files = sharedFolder.listFiles();
        if (files == null) return;
        for (File file : files) {
            addSharedFile(file);
        }
    }

    public List<FileMetaData> getSharedFiles() {
        return new ArrayList<>(sharedFiles.values());
    }

    public Map<String, FileMetaData> getDownloadingFiles() {
        return downloadingFiles;
    }

    public FileMetaData getFileMetaDataByHash(String fileHash) {
        return sharedFiles.get(fileHash);
    }

    public FileMetaData findFileByName(String fileName) {
        for (FileMetaData meta : sharedFiles.values()) {
            if (meta.getFileName().equalsIgnoreCase(fileName)) {
                return meta;
            }
        }
        return null;
    }
}