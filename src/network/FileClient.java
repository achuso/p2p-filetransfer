package network;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.RandomAccessFile;
import java.net.Socket;

public class FileClient {
    public static void downloadFile(String serverIP, int serverPort, String fileName, File destinationFolder) {
        try (Socket socket = new Socket(serverIP, serverPort);
             DataInputStream input = new DataInputStream(socket.getInputStream());
             DataOutputStream output = new DataOutputStream(socket.getOutputStream())) {

            output.writeUTF(fileName); // Request the file
            long fileSize = input.readLong(); // Read the file size

            if (fileSize <= 0) {
                System.out.println("Error: File not found on server.");
                return;
            }

            File outputFile = new File(destinationFolder, fileName);
            try (RandomAccessFile raf = new RandomAccessFile(outputFile, "rw")) {
                byte[] buffer = new byte[256 * 1024]; // 256KB buffer
                long totalRead = 0;
                int bytesRead;

                while (totalRead < fileSize && (bytesRead = input.read(buffer)) != -1) {
                    raf.write(buffer, 0, bytesRead);
                    totalRead += bytesRead;
                    System.out.println("Progress: " + (totalRead * 100 / fileSize) + "%");
                }

                System.out.println("Download complete: " + fileName);
            }
        }
        catch (Exception e) {
            System.out.println("Error with download: " + e.getMessage());
        }
    }
}