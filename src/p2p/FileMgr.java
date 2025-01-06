package p2p;

import java.io.File;
import java.util.*;

public class FileMgr {
    private final Map<String, FileMetaData> sharedFiles;

    public FileMgr() {
        this.sharedFiles = new HashMap<>();
    }

    public void addSharedFile(File file) {
        try {
            if (!file.exists() || file.isDirectory())
                return;

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

    public FileMetaData getFileMetaDataByHash(String hash) {
        return sharedFiles.get(hash);
    }

    public List<FileMetaData> getSharedFiles() { return new ArrayList<>(sharedFiles.values()); }
}