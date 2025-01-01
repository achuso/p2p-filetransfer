package main;

import gui.*;
import p2p.Node;
import p2p.Peer;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        boolean isDocker = System.getenv("DOCKER_BOOL") != null;
        String nodeID = System.getenv("NODE_ID");
        String nodeIP = isDocker ? System.getenv("NODE_IP") : "10.22.249.198"; // Host's IP

        if (isDocker) {
            System.out.println("Running in Docker. Starting Node without GUI...");
            Node node = new Node(nodeID, nodeIP, 4113, true);

            // Start server
            new Thread(node::startServer).start();
            node.startPeerDiscovery();

            // Log discovered peers
            System.out.println("Discovered peers:");
            for (Peer peer : node.getPeerManager().getAllPeers())
                System.out.println("- " + peer.getPeerID() + " (" + peer.getIP() + ")");

            System.out.println("Node is running!");
        }
        else {
            System.out.println("Starting GUI...");
            Node node = new Node("host", "10.22.249.198", 4113, false); // Host IP
            SwingUtilities.invokeLater(() -> new MainWindow(node));
        }
    }
}