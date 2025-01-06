package gui;

import p2p.Node;

import javax.swing.*;

public class MenuBar extends JMenuBar {
    private final Node node;
    private final JFrame parentFrame;

    public MenuBar(JFrame parentFrame, Node node) {
        this.parentFrame = parentFrame;
        this.node = node;

        JMenu fileMenu = getjMenu();
        add(fileMenu);

        JMenu helpMenu = new JMenu("Help");
        JMenuItem aboutItem = new JMenuItem("About");
        aboutItem.addActionListener(e -> JOptionPane.showMessageDialog(
                parentFrame,
                "CSE471 Term Project\n" +
                        "P2P File Sharing\n\n" +
                        "Name: Onat Ribar\n" +
                        "Student ID: 20210702099\n",
                "About",
                JOptionPane.INFORMATION_MESSAGE
        ));
        helpMenu.add(aboutItem);
        add(helpMenu);
    }

    private JMenu getjMenu() {
        JMenu fileMenu = new JMenu("File");

        JMenuItem connectItem = new JMenuItem("Connect");
        connectItem.addActionListener(e -> handleConnect());

        JMenuItem disconnectItem = new JMenuItem("Disconnect");
        disconnectItem.addActionListener(e -> handleDisconnect());

        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> System.exit(0));

        fileMenu.add(connectItem);
        fileMenu.add(disconnectItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);
        return fileMenu;
    }

    private void handleConnect() {
        try {
            node.startServer();
            node.startPeerDiscovery();
            node.startFileSharing();
            node.startLocalFolderMonitor();

            JOptionPane.showMessageDialog(parentFrame,
                    "Connected to P2P network!",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE);
        }
        catch (Exception e) {
            JOptionPane.showMessageDialog(parentFrame,
                    "Failed to connect: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleDisconnect() {
        try {
            node.disconnect();

            JOptionPane.showMessageDialog(parentFrame,
                    "Disconnected from P2P network.\n" +
                            "No new downloads or shares can occur now.",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE);
        }
        catch (Exception e) {
            JOptionPane.showMessageDialog(parentFrame,
                    "Failed to disconnect: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}