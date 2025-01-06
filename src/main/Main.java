package main;

import p2p.Node;

import javax.swing.SwingUtilities;
import gui.MainWindow;

public class Main {
    public static void main(String[] args) {
        boolean isDocker = (System.getenv("DOCKER_BOOL") != null);
        String nodeID = (System.getenv("NODE_ID") != null)
                ? System.getenv("NODE_ID")
                : "host";
        String nodeIP = isDocker
                ? System.getenv("NODE_IP")
                : "10.22.249.198";

        if (isDocker) {
            Node node = new Node(nodeID, nodeIP, 4113, true);
            node.startServer();
            node.startPeerDiscovery();
            node.startFileSharing();
            node.startLocalFolderMonitor();

            Thread debugThread = new Thread(() -> {
                while (true) {
                    try {
                        Thread.sleep(1000); // poll active downloads every second
                        var active = node.listActiveDownloads();
                        if (!active.isEmpty()) {
                            System.out.println("Active Downloads:");
                            for (Node.DownloadProgress progress : active) {
                                System.out.printf("  - %s: %5.2f%% of %d bytes\n",
                                        progress.fileName, progress.getPercent(), progress.totalSize);
                            }
                        }
                    }
                    catch (InterruptedException e) {
                        break;
                    }
                }
            });
            debugThread.setDaemon(true);
            debugThread.start();

            // block
            while (true) {
                try { Thread.sleep(60000); }
                catch (InterruptedException e) { break; }
            }
        }
        else {
            System.out.println("Starting GUI mode");
            Node node = new Node("host", nodeIP, 4113, false);
            SwingUtilities.invokeLater(() -> new MainWindow(node));
        }
    }
}