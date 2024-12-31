package p2p;

import network.FileServer;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
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
        FileMetaData metadata = fileMgr.getFileMetaData(fileName);
        if (metadata != null) {
            List<Peer> peers = metadata.getPeersWithFile();
            if (peers.isEmpty()) {
                System.out.println("None of the peers have the file: " + fileName);
                return;
            }

            System.out.println("Downloading file from " + peers.size() + " sources...");
            // Multi-source download logic later
        }
        else {
            System.out.println("File not found: " + fileName);
        }
    }

    public FileMgr getFileManager() { return this.fileMgr; }
    public PeerMgr getPeerManager() { return this.peerMgr; }
}