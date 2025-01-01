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

        setJMenuBar(createMenuBar());

        setVisible(true);
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("File");

        JMenuItem connectItem = new JMenuItem("Connect");
        connectItem.addActionListener(event -> handleConnect());

        JMenuItem disconnectItem = new JMenuItem("Disconnect");
        disconnectItem.addActionListener(event -> handleDisconnect());

        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(event -> System.exit(0));

        fileMenu.add(connectItem);
        fileMenu.add(disconnectItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);

        menuBar.add(fileMenu);

        JMenu helpMenu = new JMenu("Help");
        JMenuItem aboutItem = new JMenuItem("About");
        aboutItem.addActionListener(event -> JOptionPane.showMessageDialog(
                this,
                "CSE471 Term Project\nP2P File Sharing Application\n\nName: Onat Ribar\nStudent ID: 20210702099",
                "About",
                JOptionPane.INFORMATION_MESSAGE
        ));
        helpMenu.add(aboutItem);

        menuBar.add(helpMenu);

        return menuBar;
    }

    private void handleConnect() {
        try {
            node.startServer();
            node.startPeerDiscovery(); // Start discovery when connecting
            node.startFileSharing();
            JOptionPane.showMessageDialog(this, "Connected to P2P network.", "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Failed to connect: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleDisconnect() {
        try {
            node.stopPeerDiscovery(); // Stop discovery when disconnecting
            node.stopFileSharing();
            JOptionPane.showMessageDialog(this, "Disconnected from P2P network.", "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Failed to disconnect: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}