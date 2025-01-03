package network;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class FileClient {

    public static List<SimpleFileInfo> requestSharedFiles(String peerIP, int peerPort) {
        List<SimpleFileInfo> results = new ArrayList<>();
        try (Socket socket = new Socket(peerIP, peerPort);
             DataOutputStream out = new DataOutputStream(socket.getOutputStream());
             DataInputStream in = new DataInputStream(socket.getInputStream())) {

            out.writeUTF("LIST_SHARED_FILES");

            String resp = in.readUTF();
            if (!"OK".equals(resp)) {
                System.err.println("Peer responded with error: " + resp);
                return results;
            }

            int count = in.readInt();
            for (int i = 0; i < count; i++) {
                String hash = in.readUTF();
                String name = in.readUTF();
                long size = in.readLong();
                results.add(new SimpleFileInfo(hash, name, size));
            }
        }
        catch (IOException e) {
            System.err.println("requestSharedFiles => " + e.getMessage());
        }
        return results;
    }

    public static long requestFileSizeByHash(String peerIP, int peerPort, String fileHash) {
        try (Socket socket = new Socket(peerIP, peerPort);
             DataOutputStream out = new DataOutputStream(socket.getOutputStream());
             DataInputStream in = new DataInputStream(socket.getInputStream())) {

            out.writeUTF("REQUEST_FILE_SIZE_BY_HASH");
            out.writeUTF(fileHash);

            String resp = in.readUTF();
            if (!"OK".equals(resp)) {
                System.err.println("Peer responded with error: " + resp);
                return 0;
            }
            return in.readLong();
        }
        catch (IOException e) {
            System.err.println("requestFileSizeByHash => " + e.getMessage());
            return 0;
        }
    }

    public static boolean downloadChunk(String peerIP, int peerPort,
                                        String fileHash, long offset, long chunkSize,
                                        File chunkOutputFile) {
        try (Socket socket = new Socket(peerIP, peerPort);
             DataOutputStream out = new DataOutputStream(socket.getOutputStream());
             DataInputStream in = new DataInputStream(socket.getInputStream())) {

            out.writeUTF("REQUEST_CHUNK");
            out.writeUTF(fileHash);
            out.writeLong(offset);
            out.writeLong(chunkSize);

            String resp = in.readUTF();
            if (!"OK".equals(resp)) {
                System.err.println("Peer responded with error: " + resp);
                return false;
            }

            try (FileOutputStream fos = new FileOutputStream(chunkOutputFile)) {
                byte[] buffer = new byte[256 * 1024];
                long bytesRemaining = chunkSize;
                int bytesRead;
                while (bytesRemaining > 0 &&
                        (bytesRead = in.read(buffer, 0, (int)Math.min(buffer.length, bytesRemaining))) != -1) {
                    fos.write(buffer, 0, bytesRead);
                    bytesRemaining -= bytesRead;
                }
            }
            return true;
        }
        catch (IOException e) {
            System.err.println("downloadChunk => " + e.getMessage());
            return false;
        }
    }

    public static class SimpleFileInfo {
        public final String fileHash;
        public final String fileName;
        public final long fileSize;

        public SimpleFileInfo(String fileHash, String fileName, long fileSize) {
            this.fileHash = fileHash;
            this.fileName = fileName;
            this.fileSize = fileSize;
        }
    }
}