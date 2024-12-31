package main;

import gui.*;
import p2p.Node;
import p2p.Peer;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        boolean isDocker = System.getenv("DOCKER_BOOL") != null;

        if (isDocker) {
            System.out.println("Running in Docker. Starting Node without GUI...");
            Node node = new Node("peer1", "127.0.0.1", 4113, true);

            // Start server
            new Thread(node::startServer).start();
            node.discoverPeers();

            // Log discovered peers
            System.out.println("Discovered peers:");
            for (Peer peer : node.getPeerManager().getAllPeers())
                System.out.println("- " + peer.getPeerID() + " (" + peer.getIP() + ")");

            System.out.println("Node is running!");
        }
        else {
            System.out.println("Starting GUI...");
            Node node = new Node("peer1", "127.0.0.1", 4113, false);
            SwingUtilities.invokeLater(() -> new MainWindow(node));
        }
    }
}