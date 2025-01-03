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

    public void refreshSharedFolder(File folder) {
        sharedFiles.clear();
        File[] files = folder.listFiles();
        if (files == null) return;

        for (File f : files) {
            addSharedFile(f);
        }
    }

    public void addSharedFile(File file) {
        try {
            if (!file.exists() || file.isDirectory()) return;
            String hash = FileTransferMgr.calculateFileHash(file);
            if (!sharedFiles.containsKey(hash)) {
                FileMetaData meta = new FileMetaData(file.getName(), file.length(), file, hash);
                sharedFiles.put(hash, meta);
            }
        }
        catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    public void addDownloadingFile(FileMetaData meta) {
        downloadingFiles.put(meta.getFileHash(), meta);
    }

    public FileMetaData getFileMetaDataByHash(String hash) {
        return sharedFiles.get(hash);
    }

    public List<FileMetaData> getSharedFiles()              { return new ArrayList<>(sharedFiles.values()); }

    public Map<String, FileMetaData> getDownloadingFiles()  { return downloadingFiles; }
}