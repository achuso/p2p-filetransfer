package network;

import p2p.FileTransferMgr;

import java.io.*;
import java.net.*;

public class FileClient {
    public static void downloadFile(String peerIP, int peerPort, String fileName, File destinationFolder) {
        int attempts = 0;
        while (attempts < FileTransferMgr.MAX_RETRIES) {
            attempts++;
            try (Socket socket = new Socket(peerIP, peerPort);
                 DataInputStream input = new DataInputStream(socket.getInputStream());
                 DataOutputStream output = new DataOutputStream(socket.getOutputStream())) {

                output.writeUTF(fileName);
                String response = input.readUTF();
                if ("ERROR: File not found".equals(response)) {
                    System.err.println("File not found on peer: " + peerIP);
                    return;
                }

                String fileHash = input.readUTF(); // Read file hash
                long fileSize = input.readLong(); // Read file size

                FileTransferMgr.receiveFile(fileName, fileSize, destinationFolder, input, fileHash);
                System.out.println("File downloaded successfully: " + fileName);
                return;

            }
            catch (IOException e) {
                System.err.println("Attempt " + attempts + " failed: " + e.getMessage());
                if (attempts == FileTransferMgr.MAX_RETRIES) {
                    System.err.println("Failed to download file after " + FileTransferMgr.MAX_RETRIES + " attempts.");
                }
            }
        }
    }
}