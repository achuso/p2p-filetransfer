package network;

import p2p.FileMgr;
import p2p.FileMetaData;
import p2p.Node;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
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
        try (DataInputStream in = new DataInputStream(socket.getInputStream());
             DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {

            String command = in.readUTF();
            System.out.println("[FileServer] command => " + command
                    + " from " + socket.getInetAddress());

            switch (command) {
            case "LIST_SHARED_FILES":
                handleListSharedFiles(out);
                break;
            case "REQUEST_FILE_SIZE_BY_HASH":
                handleFileSizeByHash(in, out);
                break;
            case "REQUEST_CHUNK":
                handleRequestChunk(in, out);
                break;
            default:
                out.writeUTF("ERROR: Unknown command");
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleListSharedFiles(DataOutputStream out) throws IOException {
        FileMgr fileMgr = node.getFileManager();
        if (fileMgr == null) {
            out.writeUTF("ERROR: Node file manager not found");
            return;
        }

        out.writeUTF("OK");
        var sharedFiles = fileMgr.getSharedFiles();
        System.out.println("[FileServer] handleListSharedFiles => "
                + sharedFiles.size() + " file(s) to " + socket.getInetAddress());
        out.writeInt(sharedFiles.size());

        for (FileMetaData meta : sharedFiles) {
            out.writeUTF(meta.getFileHash());
            out.writeUTF(meta.getFileName());
            out.writeLong(meta.getFileSize());
        }
    }

    private void handleFileSizeByHash(DataInputStream in, DataOutputStream out) throws IOException {
        String fileHash = in.readUTF();
        FileMgr fileMgr = node.getFileManager();
        if (fileMgr == null) {
            out.writeUTF("ERROR: Node file manager not found");
            return;
        }

        FileMetaData meta = fileMgr.getFileMetaDataByHash(fileHash);
        if (meta == null || meta.getFile() == null || !meta.getFile().exists()) {
            System.out.println("[FileServer] handleFileSizeByHash => no meta for " + fileHash);
            out.writeUTF("ERROR: File not found");
            return;
        }

        out.writeUTF("OK");
        out.writeLong(meta.getFile().length());
    }

    private void handleRequestChunk(DataInputStream in, DataOutputStream out) throws IOException {
        String fileHash = in.readUTF();
        long offset = in.readLong();
        long chunkSize = in.readLong();

        FileMgr fileMgr = node.getFileManager();
        if (fileMgr == null) {
            out.writeUTF("ERROR: Node file manager not found");
            return;
        }

        FileMetaData meta = fileMgr.getFileMetaDataByHash(fileHash);
        if (meta == null || meta.getFile() == null || !meta.getFile().exists()) {
            System.out.println("[FileServer] handleRequestChunk => no meta for " + fileHash);
            out.writeUTF("ERROR: File not found");
            return;
        }

        out.writeUTF("OK");

        try (RandomAccessFile raf = new RandomAccessFile(meta.getFile(), "r")) {
            raf.seek(offset);
            byte[] buffer = new byte[256 * 1024];
            long bytesRemaining = chunkSize;
            int bytesRead;
            while (bytesRemaining > 0 &&
                    (bytesRead = raf.read(buffer, 0, (int)Math.min(buffer.length, bytesRemaining))) != -1) {
                out.write(buffer, 0, bytesRead);
                bytesRemaining -= bytesRead;
            }
        }
    }

    public static void startServer(int port, Node node) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            ExecutorService executor = Executors.newCachedThreadPool();
            System.out.println("[FileServer] started on port " + port);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                executor.submit(new FileServer(clientSocket, node));
            }
        }
        catch (IOException e) {
            System.err.println("[FileServer] Error => " + e.getMessage());
        }
    }
}