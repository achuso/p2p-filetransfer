package network;

import p2p.FileManager;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class FileServer implements Runnable {
    private final Socket socket;
    private final FileManager fileManager;

    public FileServer(Socket socket, FileManager fileManager) {
        this.socket = socket;
        this.fileManager = fileManager;
    }

    @Override
    public void run() {
        try {
            DataInputStream input = new DataInputStream(socket.getInputStream());
            DataOutputStream output = new DataOutputStream(socket.getOutputStream());

            String fileName = input.readUTF(); // Read requested file name
            File file = new File(fileName);

            if (!file.exists()) {
                output.writeUTF("ERROR: File not found");
                return;
            }

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
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }

    public static void startServer(int port, FileManager fileManager) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            ExecutorService executor = Executors.newCachedThreadPool();
            System.out.println("FileServer started on port " + port);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                executor.submit(new FileServer(clientSocket, fileManager));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}