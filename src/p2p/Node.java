package p2p;

import network.FileClient;
import network.FileServer;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Node {
    private final Peer self;
    private final PeerMgr peerMgr;
    private final FileMgr fileMgr;

    public Node(String peerID, String ip, int port) {
        this.self = new Peer(peerID, ip, port);
        this.peerMgr = new PeerMgr();
        this.fileMgr = new FileMgr();
    }

    // Main fn. for testing
    public static void main(String[] args) {
        // Init node
        Node node = new Node("peer1", "127.0.0.1", 4113);

        // Start file server
        new Thread(node::startServer).start();

        // Test sharing files
        System.out.println("Sharing files...");
        node.shareFile(new File("shared/testfile1.txt"));
        node.shareFile(new File("shared/testfile2.txt"));

        // Test peer discovery
        System.out.println("Discovering peers...");
        node.discoverPeers();

        // Manual peer for testing
        Peer peer2 = new Peer("peer2", "127.0.0.2", 4113);
        node.getPeerManager().addPeer(peer2);

        // List all peers
        System.out.println("Discovered peers:");
        for (Peer peer : node.getPeerManager().getAllPeers()) {
            System.out.println("- " + peer.getPeerID() + " (" + peer.getIP() + ")");
        }

        // Test downloading a file from a peer
        System.out.println("Downloading file...");
        FileClient.downloadFile("127.0.0.2", 4114, "testfile1.txt", new File("downloads"));

        // Completion!
        System.out.println("Node operations completed.");
    }

    public void startServer() {
        Thread serverThread = new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(self.getPort())) {
                System.out.println("Server started on port: " + self.getPort());

                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    new Thread(new FileServer(clientSocket, this)).start();
                }
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        });

        serverThread.start();
    }

    public void discoverPeers() {
        peerMgr.discoverPeers();
    }

    public void shareFile(File file) {
        fileMgr.addSharedFile(file);
    }

    public void downloadFile(String fileName) {
        // Query PeerManager for available peers
        List<Peer> peers = new ArrayList<>(peerMgr.getAllPeers());
        if (peers.isEmpty()) {
            System.out.println("No peers available for file download: " + fileName);
            return;
        }

        System.out.println("Downloading file from " + peers.size() + " sources...");
        boolean downloadSuccessful = false;

        // Instances of downloads from each peer
        for (Peer peer : peers) {
            try {
                downloadFileFromPeer(peer.getIP(), peer.getPort(), fileName, new File("downloads"));
                downloadSuccessful = true;
                break; // Stop after download finishes
            }
            catch (Exception e) {
                System.out.println("Failed to download from peer: " + peer.getPeerID() + ". Trying next...");
            }
        }
        if (!downloadSuccessful)
            System.out.println("Failed to download file: " + fileName + " from all available peers.");
    }

    public void downloadFileFromPeer(String peerIP, int peerPort, String fileName, File destinationFolder) {
        System.out.println("Attempting to download file: " + fileName + " from peer: " + peerIP);
        try {
            FileClient.downloadFile(peerIP, peerPort, fileName, destinationFolder);
            System.out.println("File downloaded successfully: " + fileName);
        } catch (Exception e) {
            throw new RuntimeException("Download failed from peer " + peerIP + ": " + e.getMessage());
        }
    }

    public FileMgr getFileManager() { return this.fileMgr; }
    public PeerMgr getPeerManager() { return this.peerMgr; }
}