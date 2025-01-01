package network;

import p2p.FileTransferMgr;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class FileClient {

    public static void downloadFile(String peerIP, int peerPort, String fileName, File destinationFolder) {
        int attempts = 0;
        while (attempts < FileTransferMgr.MAX_RETRIES) {
            attempts++;
            try (Socket socket = new Socket(peerIP, peerPort);
                 DataOutputStream output = new DataOutputStream(socket.getOutputStream());
                 DataInputStream input = new DataInputStream(socket.getInputStream())) {

                // 1) Send the command
                output.writeUTF("REQUEST_FILE_BY_NAME");
                // 2) Then the fileName
                output.writeUTF(fileName);
                // 3) Read server's response
                String response = input.readUTF();
                if (response.startsWith("ERROR")) {
                    System.err.println("Server error: " + response);
                    return;
                }
                // If response == "OK", then read fileHash + fileSize
                String fileHash = input.readUTF();
                long fileSize = input.readLong();
                // Actually receive the file
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

    public static boolean downloadChunk(String peerIP, int peerPort,
                                        String fileHash, long offset, long chunkSize,
                                        File chunkOutputFile) {

        try (Socket socket = new Socket(peerIP, peerPort);
             DataOutputStream output = new DataOutputStream(socket.getOutputStream());
             DataInputStream input = new DataInputStream(socket.getInputStream())) {

            // 1) Send a command so the server knows we're requesting a chunk
            output.writeUTF("REQUEST_CHUNK");
            // 2) send fileHash, offset, chunkSize
            output.writeUTF(fileHash);
            output.writeLong(offset);
            output.writeLong(chunkSize);
            // 3) Read server response
            String response = input.readUTF();
            if (!"OK".equals(response)) {
                System.err.println("Peer responded with error: " + response);
                return false;
            }
            // 4) Write chunk data to chunkOutputFile
            try (FileOutputStream fos = new FileOutputStream(chunkOutputFile)) {
                byte[] buffer = new byte[256 * 1024];
                long bytesRemaining = chunkSize;
                int bytesRead;
                while (bytesRemaining > 0 &&
                        (bytesRead = input.read(buffer, 0, (int)Math.min(buffer.length, bytesRemaining))) != -1) {
                    fos.write(buffer, 0, bytesRead);
                    bytesRemaining -= bytesRead;
                }
            }
            return true;

        } catch (IOException e) {
            System.err.println("Chunk download failed: " + e.getMessage());
            return false;
        }
    }

    public static List<SimpleFileInfo> requestSharedFiles(String peerIP, int peerPort) {
        List<SimpleFileInfo> results = new ArrayList<>();

        try (Socket socket = new Socket(peerIP, peerPort);
             DataOutputStream out = new DataOutputStream(socket.getOutputStream());
             DataInputStream in = new DataInputStream(socket.getInputStream())) {

            // 1) Send command
            out.writeUTF("LIST_SHARED_FILES");
            // 2) Read response
            String response = in.readUTF();
            if (!"OK".equals(response)) {
                System.err.println("Peer responded with error: " + response);
                return results; // empty
            }
            // 3) read how many files
            int count = in.readInt();

            for (int i = 0; i < count; i++) {
                String fileName = in.readUTF();
                long fileSize = in.readLong();

                results.add(new SimpleFileInfo(fileName, fileSize));
            }

        }
        catch (IOException e) {
            System.err.println("Failed to request shared files: " + e.getMessage());
        }

        return results;
    }

    public static class SimpleFileInfo {
        public final String fileName;
        public final long fileSize;

        public SimpleFileInfo(String fileName, long fileSize) {
            this.fileName = fileName;
            this.fileSize = fileSize;
        }
    }
}