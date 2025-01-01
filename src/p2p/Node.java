package p2p;

import java.io.File;
import java.util.Collection;

import network.FileClient;
import network.FileServer;

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
            this.sharedFolder = new File("/shared");
            this.downloadFolder = new File("/downloads");
        } else {
            this.sharedFolder = null; // To be set by the user via GUI
            this.downloadFolder = null; // To be set by the user via GUI
        }
    }

    public void startServer() {
        new Thread(() -> FileServer.startServer(self.getPort(), this)).start();
    }

    public void startPeerDiscovery() {
        Thread discoveryThread = new Thread(() -> {
            while (keepDiscovering) {
                try {
                    peerMgr.clearPeers();
                    peerMgr.discoverPeers(self.getIP(), self.getPort());
                    Thread.sleep(5000); // Discover peers every 5 seconds
                } catch (InterruptedException e) {
                    System.out.println("Peer discovery interrupted.");
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    System.err.println("Error during peer discovery: " + e.getMessage());
                }
            }
        });
        discoveryThread.setDaemon(true);
        discoveryThread.start();
    }

    public void stopPeerDiscovery() {
        keepDiscovering = false;
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

    public File getSharedFolder() {
        return sharedFolder;
    }

    public void setSharedFolder(String path) {
        File newFolder = new File(path);
        if (!newFolder.exists() || !newFolder.isDirectory()) {
            throw new IllegalArgumentException("Invalid shared folder: " + path);
        }
        this.sharedFolder = newFolder;
        System.out.println("Shared folder set to: " + sharedFolder.getAbsolutePath());
    }

    public File getDownloadFolder() {
        return downloadFolder;
    }

    public void setDownloadFolder(String path) {
        File newFolder = new File(path);
        if (!newFolder.exists()) {
            if (!newFolder.mkdirs()) {
                throw new IllegalArgumentException("Failed to create download folder: " + path);
            }
        }
        this.downloadFolder = newFolder;
        System.out.println("Download folder set to: " + downloadFolder.getAbsolutePath());
    }

    public PeerMgr getPeerManager() {
        return peerMgr;
    }

    public Collection<Peer> getDiscoveredPeers() {
        return peerMgr.getAllPeers();
    }
}