package p2p;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class FileTransferMgr {

    public static final int MAX_RETRIES = 3;

    public static void sendFile(File file, DataOutputStream output) throws IOException, NoSuchAlgorithmException {
        if (!file.exists()) {
            throw new FileNotFoundException("File not found: " + file.getAbsolutePath());
        }

        long fileSize = file.length();
        String fileHash = calculateFileHash(file);

        output.writeUTF(fileHash); // Send file hash
        output.writeLong(fileSize); // Send file size

        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file))) {
            byte[] buffer = new byte[256 * 1024];
            int bytesRead;
            while ((bytesRead = bis.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
            }
        }
    }

    public static void receiveFile(String fileName, long fileSize, File destinationFolder, DataInputStream input, String expectedHash) throws IOException {
        int attempts = 0;
        while (attempts < MAX_RETRIES) {
            attempts++;
            try {
                if (!destinationFolder.exists() && !destinationFolder.mkdirs()) {
                    throw new IOException("Failed to create destination folder: " + destinationFolder.getAbsolutePath());
                }

                File outputFile = new File(destinationFolder, fileName);

                try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(outputFile))) {
                    byte[] buffer = new byte[256 * 1024];
                    long bytesReceived = 0;
                    int bytesRead;

                    while (bytesReceived < fileSize && (bytesRead = input.read(buffer)) != -1) {
                        bos.write(buffer, 0, bytesRead);
                        bytesReceived += bytesRead;
                    }
                }

                // Verify file integrity
                String calculatedHash = calculateFileHash(outputFile);
                if (!calculatedHash.equals(expectedHash)) {
                    throw new IOException("File integrity check failed. Expected: " + expectedHash + ", Found: " + calculatedHash);
                }

                System.out.println("File received successfully: " + fileName);
                return;
            }
            catch (IOException | NoSuchAlgorithmException e) {
                System.err.println("Attempt " + attempts + " failed: " + e.getMessage());
                if (attempts == MAX_RETRIES) {
                    throw new IOException("Failed to receive file after " + MAX_RETRIES + " attempts.", e);
                }
            }
        }
    }

    public static String calculateFileHash(File file) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");

        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file))) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = bis.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
        }

        StringBuilder hexString = new StringBuilder();
        for (byte b : digest.digest()) {
            hexString.append(String.format("%02x", b));
        }

        return hexString.toString();
    }
}