package GUI;

import javax.swing.*;
import java.awt.*;

public class MainWindow extends JFrame {

    public MainWindow() {
        setTitle("P2P File Sharing - Onat Ribar");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 800);
        setLayout(new BorderLayout());
        setResizable(false);

        final TopPanel topPanel = new TopPanel();
        final CenterPanel centerPanel = new CenterPanel();
        final BottomPanel bottomPanel = new BottomPanel();

        // Container for padding purposes
        JPanel container = new JPanel(new BorderLayout(10, 10));
        container.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        container.add(topPanel, BorderLayout.NORTH);
        container.add(centerPanel, BorderLayout.CENTER);
        container.add(bottomPanel, BorderLayout.SOUTH);
        add(container, BorderLayout.CENTER);
        setJMenuBar(new MenuBar(this));

        setVisible(true);
    }
}
