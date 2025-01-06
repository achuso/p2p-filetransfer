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
        final CenterPanel centerPanel = new CenterPanel(node);
        final BottomPanel bottomPanel = new BottomPanel(node);

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
}