package network;

import java.io.*;
import java.net.*;

public class FileClient {
    public static void downloadFile(String peerIP, int peerPort, String fileName, File destinationFolder) {
        System.out.println("Connecting to peer: " + peerIP + ":" + peerPort);
        System.out.println("Requesting file: " + fileName);

        try (Socket socket = new Socket(peerIP, peerPort);
             DataInputStream input = new DataInputStream(socket.getInputStream());
             DataOutputStream output = new DataOutputStream(socket.getOutputStream())) {

            output.writeUTF(fileName); // Request the file
            System.out.println("File request sent to: " + peerIP);

            String response = input.readUTF();
            System.out.println("Server response: " + response);

            if ("ERROR: File not found".equals(response)) {
                System.err.println("File not found on peer: " + peerIP);
                return;
            }

            long fileSize = input.readLong();
            System.out.println("File size: " + fileSize + " bytes. Starting download...");

            if (!destinationFolder.exists()) {
                destinationFolder.mkdirs();
            }
            File outputFile = new File(destinationFolder, fileName);

            try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(outputFile))) {
                byte[] buffer = new byte[256 * 1024];
                long bytesReceived = 0;
                int bytesRead;

                while ((bytesRead = input.read(buffer)) != -1) {
                    bos.write(buffer, 0, bytesRead);
                    bytesReceived += bytesRead;
                    System.out.printf("Progress: %.2f%%\r", (bytesReceived / (double) fileSize) * 100);
                }

                System.out.println("\nDownload complete: " + fileName);
            }
        }
        catch (IOException e) {
            System.err.println("FileClient error: " + e.getMessage());
            throw new RuntimeException("FileClient download failed: " + e.getMessage(), e);
        }
    }
}