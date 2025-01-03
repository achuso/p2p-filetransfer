package gui;

import p2p.Node;

import javax.swing.*;
import java.awt.*;

public class MainWindow extends JFrame {

    private final Node node;

    public MainWindow(Node node) {
        this.node = node;
        setTitle("P2P File Sharing - Onat Ribar");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 800);
        setLayout(new BorderLayout());
        setResizable(false);

        UIManager.put("OptionPane.messageFont", new Font("SansSerif", Font.PLAIN, 12));

        final TopPanel topPanel = new TopPanel(node);
        final CenterPanel centerPanel = new CenterPanel();
        final BottomPanel bottomPanel = new BottomPanel();

        // Container for padding purposes
        JPanel container = new JPanel(new BorderLayout(10, 10));
        container.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        container.add(topPanel, BorderLayout.NORTH);
        container.add(centerPanel, BorderLayout.CENTER);
        container.add(bottomPanel, BorderLayout.SOUTH);
        add(container, BorderLayout.CENTER);

        createMenuBar();

        System.out.println("[GUI] MainWindow constructor finished. Node is " + node.getSelf().getPeerID());
        setVisible(true);
    }

    private void createMenuBar() {
        setJMenuBar(new MenuBar(this, node));
    }

    // Optional if you want direct Connect/Disconnect from here (you currently do it in MenuBar):
    private void handleConnect() {
        System.out.println("[GUI] handleConnect() invoked in MainWindow.");
        System.out.println("[GUI] Node sharedFolder: " + (node.getSharedFolder() == null ? "null" : node.getSharedFolder()));
        System.out.println("[GUI] Node downloadFolder: " + (node.getDownloadFolder() == null ? "null" : node.getDownloadFolder()));

        try {
            node.startServer();
            node.startPeerDiscovery();
            node.startFileSharing();
            JOptionPane.showMessageDialog(this, "Connected to P2P network.", "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to connect: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleDisconnect() {
        System.out.println("[GUI] handleDisconnect() invoked in MainWindow.");
        try {
            node.stopPeerDiscovery();
            node.stopFileSharing();
            JOptionPane.showMessageDialog(this, "Disconnected from P2P network.", "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to disconnect: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}