package network;

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
        try (DataInputStream input = new DataInputStream(socket.getInputStream());
             DataOutputStream output = new DataOutputStream(socket.getOutputStream())) {

            System.out.println("New connection from: " + socket.getInetAddress());

            String fileName = input.readUTF();
            System.out.println("Request received for file: " + fileName);

            File file = new File("/test/shared/" + fileName);
            System.out.println("Checking file existence: " + file.getAbsolutePath() + ", Exists: " + file.exists());

            if (!file.exists()) {
                output.writeUTF("ERROR: File not found");
                System.err.println("ERROR: File not found: " + file.getAbsolutePath());
                return;
            }

            output.writeUTF("OK");
            System.out.println("File exists. Preparing to send: " + fileName);

            long fileSize = file.length();
            output.writeLong(fileSize);

            try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file))) {
                byte[] buffer = new byte[256 * 1024];
                int bytesRead;
                while ((bytesRead = bis.read(buffer)) != -1) {
                    output.write(buffer, 0, bytesRead);
                }
            }

            System.out.println("File transfer completed: " + fileName);

        }
        catch (IOException e) {
            System.err.println("FileServer error: " + e.getMessage());
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