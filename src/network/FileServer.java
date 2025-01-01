package network;

import p2p.FileMgr;
import p2p.FileMetaData;
import p2p.FileTransferMgr;
import p2p.Node;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FileServer implements Runnable {
    private final Socket socket;
    private final Node node;

    public FileServer(Socket socket, Node node) {
        this.socket = socket;
        this.node = node;
    }

    @Override
    public void run() {
        try (DataInputStream input = new DataInputStream(socket.getInputStream());
             DataOutputStream output = new DataOutputStream(socket.getOutputStream())) {

            System.out.println("New connection from: " + socket.getInetAddress());
            String command = input.readUTF();

            // To accommodate for my legacy code rn
            switch (command) {
            case "REQUEST_FILE_BY_NAME":
                handleRequestFileByName(input, output);
                break;
            case "REQUEST_CHUNK":
                handleRequestChunk(input, output);
                break;
            case "LIST_SHARED_FILES":
                handleListSharedFiles(output);
                break;
            default:
                output.writeUTF("ERROR: Unknown command");
            }
        }
        catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    // Old approach
    private void handleRequestFileByName(DataInputStream input, DataOutputStream output) throws IOException {
        String requestedName = input.readUTF();
        System.out.println("Peer requested file by name: " + requestedName);

        FileMgr fileMgr = node.getFileManager();
        if (fileMgr == null) {
            output.writeUTF("ERROR: Node file manager not found");
            return;
        }

        FileMetaData meta = fileMgr.findFileByName(requestedName);
        if (meta == null) {
            output.writeUTF("ERROR: File not found");
            return;
        }

        // File obtained, indicate success!
        output.writeUTF("OK");

        // Send the file hash + file size, then the file itself
        File file = meta.getFile();
        try {
            String fileHash = FileTransferMgr.calculateFileHash(file);
            long fileSize = file.length();

            output.writeUTF(fileHash);
            output.writeLong(fileSize);

            // Send the file data
            try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file))) {
                byte[] buffer = new byte[256 * 1024];
                int bytesRead;
                while ((bytesRead = bis.read(buffer)) != -1) {
                    output.write(buffer, 0, bytesRead);
                }
            }
            System.out.println("File sent successfully: " + requestedName);

        }
        catch (NoSuchAlgorithmException e) {
            output.writeUTF("ERROR: Hash error " + e.getMessage());
        }
    }

    private void handleRequestChunk(DataInputStream input, DataOutputStream output) throws IOException {
        String fileHash = input.readUTF();
        long offset = input.readLong();
        long chunkSize = input.readLong();

        FileMgr fileMgr = node.getFileManager();
        if (fileMgr == null) {
            output.writeUTF("ERROR: Node file manager not found");
            return;
        }

        // Look up the file by hash in the Node’s FileMgr
        FileMetaData meta = fileMgr.getFileMetaDataByHash(fileHash);
        if (meta == null) {
            output.writeUTF("ERROR: File not found");
            return;
        }

        File file = meta.getFile();
        if (!file.exists()) {
            output.writeUTF("ERROR: File not found on disk");
            return;
        }

        output.writeUTF("OK"); // proceed yo

        // Send that chunk (offset..offset+chunkSize-1)
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            raf.seek(offset);
            byte[] buffer = new byte[256 * 1024];
            long bytesRemaining = chunkSize;
            int bytesRead;
            while (bytesRemaining > 0 &&
                    (bytesRead = raf.read(buffer, 0, (int)Math.min(buffer.length, bytesRemaining))) != -1) {
                output.write(buffer, 0, bytesRead);
                bytesRemaining -= bytesRead;
            }
        }
    }

    private void handleListSharedFiles(DataOutputStream output) throws IOException {
        FileMgr fileMgr = node.getFileManager();
        if (fileMgr == null) {
            output.writeUTF("ERROR: Node file manager not found");
            return;
        }

        var sharedFiles = fileMgr.getSharedFiles();
        output.writeUTF("OK");
        output.writeInt(sharedFiles.size());

        for (FileMetaData meta : sharedFiles) {
            output.writeUTF(meta.getFileName());
            output.writeLong(meta.getFileSize());
        }
    }

    public static void startServer(int port, Node node) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            ExecutorService executor = Executors.newCachedThreadPool();
            System.out.println("FileServer started on port " + port);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                executor.submit(new FileServer(clientSocket, node));
            }
        }
        catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}