package GUI;

import javax.swing.*;
import java.awt.*;

public class TopPanel extends JPanel {
    public JTextField sharedFolderField;
    public JTextField destinationFolderField;
    public JButton setSharedFolderButton;
    public JButton setDestinationButton;

    public TopPanel() {
        setLayout(new GridLayout(2, 1, 5, 5));
        add(createSharedFolderPanel());
        add(createDestinationFolderPanel());
    }

    private JPanel createSharedFolderPanel() {
        JPanel sharedFolderPanel = new JPanel(new BorderLayout(10, 0));
        sharedFolderPanel.add(new JLabel("Root of the P2P shared folder:"), BorderLayout.WEST);
        sharedFolderField = new JTextField(30);
        sharedFolderPanel.add(sharedFolderField, BorderLayout.CENTER);
        setSharedFolderButton = new JButton("Set");
        sharedFolderPanel.add(setSharedFolderButton, BorderLayout.EAST);
        return sharedFolderPanel;
    }

    private JPanel createDestinationFolderPanel() {
        JPanel destinationFolderPanel = new JPanel(new BorderLayout(10, 0));
        destinationFolderPanel.add(new JLabel("Destination folder:"), BorderLayout.WEST);
        destinationFolderField = new JTextField(30);
        destinationFolderPanel.add(destinationFolderField, BorderLayout.CENTER);
        setDestinationButton = new JButton("Set");
        destinationFolderPanel.add(setDestinationButton, BorderLayout.EAST);
        return destinationFolderPanel;
    }
}