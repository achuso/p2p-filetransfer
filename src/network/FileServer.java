package network;

import p2p.FileMetaData;
import p2p.Node;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.RandomAccessFile;
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
        try (DataInputStream input = new DataInputStream(socket.getInputStream());
             DataOutputStream output = new DataOutputStream(socket.getOutputStream())) {

            String fileName = input.readUTF();
            FileMetaData metadata = node.getFileManager().getFileMetaData(fileName);

            if (metadata == null || !metadata.getFile().exists()) {
                output.writeUTF("ERROR: File not found");
                return;
            }

            File file = metadata.getFile();
            long fileSize = file.length();
            output.writeLong(fileSize); // Send file size to client

            try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
                byte[] buffer = new byte[256 * 1024]; // 256KB buffer
                int bytesRead;
                while ((bytesRead = raf.read(buffer)) != -1) {
                    output.write(buffer, 0, bytesRead);
                }
            }

            System.out.println("File transfer complete for: " + fileName);
        }
        catch (Exception e) {
           System.out.println("File transfer failed: " + e.getMessage());
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
        catch (Exception e) {
            System.out.println("Error starting server:" + e.getMessage());
        }
    }
}