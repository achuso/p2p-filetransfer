package p2p;

import network.FileServer;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

public class Node {
    private final Peer self;
    private final PeerManager peerManager;
    private final FileManager fileManager;

    public Node(String peerID, String ip, int port) {
        this.self = new Peer(peerID, ip, port);
        this.peerManager = new PeerManager();
        this.fileManager = new FileManager();
    }

    public void startServer() {
        Thread serverThread = new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(self.getPort())) {
                System.out.println("Server started on port: " + self.getPort());

                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    new Thread(new FileServer(clientSocket)).start();
                }
            }
            catch (IOException e) {
                System.out.println(e.getMessage());
            }
        });

        serverThread.start();
    }

    public void discoverPeers() {
        peerManager.discoverPeers();
    }

    public void shareFile(File file) {
        fileManager.addSharedFile(file);
    }

    public void downloadFile(String fileName) {
        FileMetaData metadata = fileManager.getFileMetadata(fileName);
        if (metadata != null) {
            List<Peer> peers = metadata.getPeersWithFile();
            if (peers.isEmpty()) {
                System.out.println("None of the peers have the file: " + fileName);
                return;
            }

            System.out.println("Downloading file from " + peers.size() + " sources...");
            // Multi-source download here
        }
        else {
            System.out.println("File not found: " + fileName);
        }
    }
}