package main;

import p2p.Node;

import javax.swing.SwingUtilities;
import gui.MainWindow;

public class Main {
    public static void main(String[] args) {
        boolean isDocker = (System.getenv("DOCKER_BOOL") != null);
        String nodeID = (System.getenv("NODE_ID") != null) ? System.getenv("NODE_ID") : "host";
        String nodeIP = isDocker ? System.getenv("NODE_IP") : "10.22.249.198";

        if (isDocker) {
            System.out.println("[Main] Docker mode => nodeID=" + nodeID
                    + ", nodeIP=" + nodeIP);

            Node node = new Node(nodeID, nodeIP, 4113, true);
            node.startServer();
            node.startPeerDiscovery();
            node.startFileSharing();
            node.startLocalFolderMonitor();

            System.out.println("[Main] Node is running in Docker. Press Ctrl+C or kill container to stop.");
            // just block
            while (true) {
                try { Thread.sleep(60000); }
                catch (InterruptedException e) { break; }
            }
        }
        else {
            System.out.println("[Main] Non-Docker => starting GUI node...");
            Node node = new Node("host", nodeIP, 4113, false);

            SwingUtilities.invokeLater(() -> new MainWindow(node));
        }
    }
}