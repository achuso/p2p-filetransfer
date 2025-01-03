package gui;

import p2p.Node;

import javax.swing.*;

public class MenuBar extends JMenuBar {
    private final Node node;

    public MenuBar(JFrame parentFrame, Node node) {
        this.node = node;

        JMenu fileMenu = new JMenu("File");

        JMenuItem connectItem = new JMenuItem("Connect");
        connectItem.addActionListener(event -> handleConnect(parentFrame));

        JMenuItem disconnectItem = new JMenuItem("Disconnect");
        disconnectItem.addActionListener(event -> handleDisconnect(parentFrame));

        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(event -> {
            System.out.println("[GUI] Exiting application.");
            System.exit(0);
        });

        fileMenu.add(connectItem);
        fileMenu.add(disconnectItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);

        add(fileMenu);

        JMenu helpMenu = new JMenu("Help");
        JMenuItem aboutItem = new JMenuItem("About");
        aboutItem.addActionListener(event -> JOptionPane.showMessageDialog(
                parentFrame,
                "CSE471 Term Project\n" +
                        "P2P File Sharing Application\n\n" +
                        "Name: Onat Ribar\n" +
                        "Student ID: 20210702099",
                    "About",
                JOptionPane.INFORMATION_MESSAGE
        ));
        helpMenu.add(aboutItem);

        add(helpMenu);
    }

    private void handleConnect(JFrame parentFrame) {
        try {
            System.out.println("[GUI:MenuBar] handleConnect() clicked.");
            System.out.println(" - Node sharedFolder = " + (node.getSharedFolder() == null ? "null" : node.getSharedFolder().getAbsolutePath()));
            System.out.println(" - Node downloadFolder = " + (node.getDownloadFolder() == null ? "null" : node.getDownloadFolder().getAbsolutePath()));

            node.startServer();
            System.out.println("[GUI:MenuBar] startServer() called.");
            node.startPeerDiscovery();
            System.out.println("[GUI:MenuBar] startPeerDiscovery() called.");
            node.startFileSharing();
            System.out.println("[GUI:MenuBar] startFileSharing() called.");

            JOptionPane.showMessageDialog(parentFrame, "Connected to P2P network.", "Success", JOptionPane.INFORMATION_MESSAGE);
        }
        catch (Exception e) {
            System.err.println("[GUI:MenuBar] handleConnect() failed: " + e.getMessage());
            System.err.println(e.getMessage());
            JOptionPane.showMessageDialog(parentFrame, "Failed to connect: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleDisconnect(JFrame parentFrame) {
        try {
            System.out.println("[GUI:MenuBar] handleDisconnect() clicked.");

            node.stopPeerDiscovery();
            System.out.println("[GUI:MenuBar] stopPeerDiscovery() called.");
            node.stopFileSharing();
            System.out.println("[GUI:MenuBar] stopFileSharing() called.");

            JOptionPane.showMessageDialog(parentFrame, "Disconnected from P2P network.", "Success", JOptionPane.INFORMATION_MESSAGE);
        }
        catch (Exception e) {
            System.err.println("[GUI:MenuBar] handleDisconnect() failed: " + e.getMessage());
            e.printStackTrace();
            JOptionPane.showMessageDialog(parentFrame, "Failed to disconnect: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}