package p2p;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class FileTransferMgr {

    // public static final int MAX_RETRIES = 3;

    public static String calculateFileHash(File file)
            throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file))) {
            byte[] buffer = new byte[1024];
            int read;
            while ((read = bis.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
        }
        StringBuilder sb = new StringBuilder();
        for (byte bayt : digest.digest()) {
            sb.append(String.format("%02x", bayt));
        }
        return sb.toString();
    }
}