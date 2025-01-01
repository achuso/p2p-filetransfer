package network;

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

            String fileName = input.readUTF();
            System.out.println("Request received for file: " + fileName);

            File file = new File(node.getSharedFolder(), fileName);
            if (!file.exists()) {
                output.writeUTF("ERROR: File not found");
                return;
            }

            output.writeUTF("OK");

            FileTransferMgr.sendFile(file, output);
            System.out.println("File sent successfully: " + fileName);

        }
        catch (IOException | NoSuchAlgorithmException e) {
            System.out.println("Error: " + e.getMessage());
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