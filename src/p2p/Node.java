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
    private final File downloadFolder;

    public Node(String peerID, String ip, int port) {
        this.self = new Peer(peerID, ip, port);
        this.peerMgr = new PeerMgr();
        this.fileMgr = new FileMgr();
        this.downloadFolder = new File("downloads");
        if (!downloadFolder.exists()) {
            boolean created = downloadFolder.mkdirs();
            if (!created) {
                throw new RuntimeException("Failed to create download folder: " + downloadFolder.getAbsolutePath());
            }
        }
    }

    public static void main(String[] args) {
        // Init node
        Node node = new Node("peer1", "127.0.0.1", 4113);

        // Start file server
        new Thread(node::startServer).start();

        // Test sharing files
        System.out.println("Sharing files...");
        node.shareFile(new File("test/shared/test1.txt"));
        node.shareFile(new File("test/shared/test2.txt"));
        System.out.println("Shared files at startup:");
        node.getFileManager().listSharedFiles();

        // Test peer discovery
        System.out.println("Discovering peers...");
        node.discoverPeers();

        // List all peers
        System.out.println("Discovered peers:");
        for (Peer peer : node.getPeerManager().getAllPeers()) {
            System.out.println("- " + peer.getPeerID() + " (" + peer.getIP() + ")");
        }

        // Test downloading a file
        System.out.println("Initiating file download...");
        node.downloadFile("test1.txt");

        // Completion
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
            }
            catch (IOException e) {
                System.err.println("Server error: " + e.getMessage());
            }
        });

        serverThread.start();
    }

    public void discoverPeers() {
        peerMgr.clearPeers();
        peerMgr.discoverPeers(self.getIP(), self.getPort());
    }

    public void shareFile(File file) {
        if (!file.exists()) {
            System.err.println("File does not exist and cannot be shared: " + file.getAbsolutePath());
            return;
        }
        fileMgr.addSharedFile(file);
    }

    public void downloadFile(String fileName) {
        List<Peer> peers = new ArrayList<>(peerMgr.getAllPeers());
        if (peers.isEmpty()) {
            System.out.println("No peers available for file download: " + fileName);
            return;
        }

        System.out.println("Downloading file from " + peers.size() + " sources...");
        boolean downloadSuccessful = false;

        for (Peer peer : peers) {
            try {
                downloadFileFromPeer(peer.getIP(), peer.getPort(), fileName);
                downloadSuccessful = true;
                break; // Stop after successful download
            }
            catch (Exception e) {
                System.err.println("Failed to download from peer: " + peer.getPeerID() + ". Trying next...");
            }
        }

        if (!downloadSuccessful) {
            System.err.println("Failed to download file: " + fileName + " from all available peers.");
        }
    }

    public void downloadFileFromPeer(String peerIP, int peerPort, String fileName) {
        System.out.println("Attempting to download file: " + fileName + " from peer: " + peerIP);
        try {
            FileClient.downloadFile(peerIP, peerPort, fileName, downloadFolder);
            System.out.println("File downloaded successfully: " + fileName);
        } catch (Exception e) {
            System.err.println("Error during file download: " + e.getMessage());
            throw new RuntimeException("Download failed from peer " + peerIP + " on port " + peerPort + ": " + e.getMessage(), e);
        }
    }

    public FileMgr getFileManager() {
        return this.fileMgr;
    }

    public PeerMgr getPeerManager() {
        return this.peerMgr;
    }
}