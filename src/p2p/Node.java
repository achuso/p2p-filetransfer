package p2p;

import network.FileClient;
import network.FileServer;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Node {
    private final Peer self;
    private final PeerMgr peerMgr;
    private final FileMgr fileMgr;
    private File sharedFolder;
    private File downloadFolder;
    private volatile boolean keepDiscovering = true;
    private volatile boolean keepSharing = true;

    public Node(String peerID, String ip, int port, boolean isDockerInstance) {
        this.self = new Peer(peerID, ip, port);
        this.peerMgr = new PeerMgr();
        this.fileMgr = new FileMgr();

        if (isDockerInstance) {
            // Preset folders for Docker containers
            this.sharedFolder = new File("/test/shared");
            this.downloadFolder = new File("/downloads");

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

    public void startServer() {
        Thread serverThread = new Thread(() -> {
            int retries = 3; // Number of retries
            int currentPort = self.getPort();

            while (retries > 0) {
                try (ServerSocket serverSocket = new ServerSocket(currentPort)) {
                    System.out.println("Server started on port: " + currentPort);

                    while (true) {
                        Socket clientSocket = serverSocket.accept();
                        new Thread(new FileServer(clientSocket, this)).start();
                    }
                }
                catch (IOException e) {
                    if (e.getMessage().contains("Address already in use")) {
                        System.err.println("Port " + currentPort + " is already in use. Trying another port...");
                        currentPort++;
                        retries--;
                    }
                    else {
                        System.err.println("Server error: " + e.getMessage());
                        break;
                    }
                }
            }

            if (retries == 0) {
                System.err.println("Failed to bind server to a port after multiple attempts.");
            }
        });

        serverThread.start();
    }

    public void startPeerDiscovery() {
        Thread discoveryThread = new Thread(() -> {
            while (keepDiscovering) {
                try {
                    peerMgr.clearPeers();
                    peerMgr.discoverPeers(self.getIP(), self.getPort());
                    Thread.sleep(10000); // Discover peers every 10 seconds
                } catch (InterruptedException e) {
                    System.out.println("Peer discovery interrupted.");
                    break;
                }
            }
        });
        discoveryThread.setDaemon(true);
        discoveryThread.start();
    }

    public void discoverPeers() {
        try {
            peerMgr.clearPeers();
            peerMgr.discoverPeers(self.getIP(), self.getPort());
        } catch (Exception e) {
            System.err.println("Error during peer discovery: " + e.getMessage());
        }
    }

    public void startFileSharing() {
        Thread sharingThread = new Thread(() -> {
            while (keepSharing) {
                try {
                    for (Peer peer : peerMgr.getAllPeers()) {
                        for (File file : peer.getSharedFiles()) {
                            FileMetaData fileMetaData = fileMgr.getFileMetaData(file.getName());
                            if (fileMetaData == null) {
                                System.out.println("Downloading new file: " + file.getName());
                                downloadFileFromPeer(peer.getIP(), peer.getPort(), file.getName());
                            }
                        }
                    }
                    Thread.sleep(5000); // Check for new files every 5 seconds
                } catch (InterruptedException e) {
                    System.out.println("File sharing interrupted.");
                    break;
                }
            }
        });
        sharingThread.setDaemon(true);
        sharingThread.start();
    }

    public void stopPeerDiscovery() {
        keepDiscovering = false;
    }

    public void stopFileSharing() {
        keepSharing = false;
    }

    public void downloadFileFromPeer(String peerIP, int peerPort, String fileName) {
        System.out.println("Attempting to download file: " + fileName + " from peer: " + peerIP);
        try {
            FileClient.downloadFile(peerIP, peerPort, fileName, downloadFolder);
            System.out.println("File downloaded successfully: " + fileName);
        } catch (Exception e) {
            System.err.println("Error during file download: " + e.getMessage());
        }
    }

    public FileMgr getFileManager() {
        return this.fileMgr;
    }

    public PeerMgr getPeerManager() {
        return this.peerMgr;
    }
}