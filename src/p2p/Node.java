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
    private File sharedFolder;
    private File downloadFolder;

    public Node(String peerID, String ip, int port, boolean isDockerInstance) {
        this.self = new Peer(peerID, ip, port);
        this.peerMgr = new PeerMgr();
        this.fileMgr = new FileMgr();

        if (isDockerInstance) {
            // Preset folders for Docker containers
            this.sharedFolder = new File("/test/shared");
            this.downloadFolder = new File("/downloads");
        }
        else {
            this.sharedFolder = null;
            this.downloadFolder = null;
        }

        if (isDockerInstance) {
            if (!sharedFolder.exists() || !sharedFolder.isDirectory()) {
                throw new RuntimeException("Invalid shared folder in Docker: " + sharedFolder.getAbsolutePath());
            }

            if (!downloadFolder.exists()) {
                boolean created = downloadFolder.mkdirs();
                if (!created) {
                    throw new RuntimeException("Failed to create download folder in Docker: " + downloadFolder.getAbsolutePath());
                }
            }
        }
    }

    public void setSharedFolder(String path) {
        File newSharedFolder = new File(path);
        if (!newSharedFolder.exists() || !newSharedFolder.isDirectory()) {
            throw new RuntimeException("Invalid shared folder: " + path);
        }
        this.sharedFolder = newSharedFolder;
        fileMgr.refreshSharedFolder(sharedFolder);
        System.out.println("Shared folder set to: " + sharedFolder.getAbsolutePath());
    }

    public void setDownloadFolder(String path) {
        File newDownloadFolder = new File(path);
        if (!newDownloadFolder.exists()) {
            boolean created = newDownloadFolder.mkdirs();
            if (!created) {
                throw new RuntimeException("Failed to create download folder: " + path);
            }
        }
        this.downloadFolder = newDownloadFolder;
        System.out.println("Download folder set to: " + downloadFolder.getAbsolutePath());
    }

    public File getSharedFolder() {
        return sharedFolder;
    }

    public File getDownloadFolder() {
        return downloadFolder;
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
                System.err.println("Server error: " + e.getMessage());
            }
        });

        serverThread.start();
    }

    public void discoverPeers() {
        peerMgr.clearPeers();
        peerMgr.discoverPeers(self.getIP(), self.getPort());
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
        }
        catch (Exception e) {
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