package gui;

import p2p.Node;

import javax.swing.*;

public class MenuBar extends JMenuBar {
    private final String aboutText;
    private final Node node;

    public MenuBar(JFrame parentFrame, Node node) {
        this.node = node;
        this.aboutText = """
                CSE471 Term Project 
                P2P File Sharing Application

                Name: Onat Ribar
                Student ID: 20210702099
                """;

        this.add(createFileMenu(parentFrame));
        this.add(createHelpMenu(parentFrame));
    }

    private JMenu createFileMenu(JFrame parentFrame) {
        JMenu fileMenu = new JMenu("File");

        JMenuItem connectItem = new JMenuItem("Connect");
        connectItem.addActionListener(event -> {
            try {
                node.startServer();
                node.startPeerDiscovery();
                node.startFileSharing();
                JOptionPane.showMessageDialog(parentFrame, "Connected to P2P network.", "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(parentFrame, "Failed to connect: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        JMenuItem disconnectItem = new JMenuItem("Disconnect");
        disconnectItem.addActionListener(event -> {
            try {
                node.stopPeerDiscovery();
                node.stopFileSharing();
                JOptionPane.showMessageDialog(parentFrame, "Disconnected from P2P network.", "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(parentFrame, "Failed to disconnect: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(event -> System.exit(0));

        fileMenu.add(connectItem);
        fileMenu.add(disconnectItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);

        return fileMenu;
    }

    private JMenu createHelpMenu(JFrame parentFrame) {
        JMenu helpMenu = new JMenu("Help");
        JMenuItem aboutItem = new JMenuItem("About");
        aboutItem.addActionListener(event -> JOptionPane.showMessageDialog(
                parentFrame,
                this.aboutText,
                "About",
                JOptionPane.INFORMATION_MESSAGE
        ));
        helpMenu.add(aboutItem);

        return helpMenu;
    }
}