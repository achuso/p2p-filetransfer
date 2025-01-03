package gui;

import p2p.Node;

import javax.swing.*;

public class MenuBar extends JMenuBar {
    private final Node node;
    private final JFrame parentFrame;

    public MenuBar(JFrame parentFrame, Node node) {
        this.parentFrame = parentFrame;
        this.node = node;

        JMenu fileMenu = new JMenu("File");

        JMenuItem connectItem = new JMenuItem("Connect");
        connectItem.addActionListener(e -> handleConnect());

        JMenuItem disconnectItem = new JMenuItem("Disconnect");
        disconnectItem.addActionListener(e -> handleDisconnect());

        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> {
            System.out.println("[MenuBar] Exiting...");
            System.exit(0);
        });

        fileMenu.add(connectItem);
        fileMenu.add(disconnectItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);

        add(fileMenu);

        JMenu helpMenu = new JMenu("Help");
        JMenuItem aboutItem = new JMenuItem("About");
        aboutItem.addActionListener(e -> JOptionPane.showMessageDialog(
                parentFrame,
                "P2P File Sharing Application\n\n" +
                        "Enhancements:\n" +
                        " - Docker or GUI\n" +
                        " - Chunk-based multi-source\n" +
                        " - Active downloads at 100%\n" +
                        " - Unified 'found' listing\n" +
                        " - Basic search\n",
                "About",
                JOptionPane.INFORMATION_MESSAGE
        ));
        helpMenu.add(aboutItem);

        add(helpMenu);
    }

    private void handleConnect() {
        try {
            System.out.println("[MenuBar] handleConnect invoked.");
            node.startServer();
            node.startPeerDiscovery();
            node.startFileSharing();
            node.startLocalFolderMonitor(); // if Docker

            JOptionPane.showMessageDialog(parentFrame,
                    "Connected to P2P network.",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE);
        }
        catch (Exception e) {
            System.err.print(e.getMessage());
            JOptionPane.showMessageDialog(parentFrame,
                    "Failed to connect: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleDisconnect() {
        try {
            System.out.println("[MenuBar] handleDisconnect invoked.");
            node.stopPeerDiscovery();
            node.stopFileSharing();
            node.stopLocalFolderMonitor();

            JOptionPane.showMessageDialog(parentFrame,
                    "Disconnected from P2P network.",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE);
        }
        catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(parentFrame,
                    "Failed to disconnect: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}