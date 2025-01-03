package gui;

import p2p.Node;

import javax.swing.*;

/**
 * MenuBar => has "Connect", "Disconnect", "Exit", "About".
 * Now, "Disconnect" also calls node.stopServer() so no new downloads can happen after.
 */
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
                        "Features:\n" +
                        " - Double-click found files to download\n" +
                        " - Auto-refresh local folder so new files are discovered\n" +
                        " - 100% downloads remain in list\n" +
                        " - Limited-scope UDP flooding for peer discovery\n",
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
            node.startLocalFolderMonitor();

            JOptionPane.showMessageDialog(parentFrame,
                    "Connected to P2P network.",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE);
        }
        catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(parentFrame,
                    "Failed to connect: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleDisconnect() {
        try {
            System.out.println("handleDisconnect invoked.");
            node.stopPeerDiscovery();
            node.stopFileSharing();
            node.stopLocalFolderMonitor();
            node.stopServer();

            JOptionPane.showMessageDialog(parentFrame,
                    "Disconnected from P2P network.\nNo new downloads or file shares can occur now.",
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