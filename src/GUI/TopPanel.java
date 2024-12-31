package GUI;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;

public class TopPanel extends JPanel {
    public JTextField sharedFolderTextField;
    public JTextField destinationFolderTextField;
    public JButton setSharedFolderButton;
    public JButton setDestinationButton;

    public TopPanel() {
        setLayout(new GridLayout(2, 1, 5, 5));
        add(createSharedFolderPanel());
        add(createDestinationFolderPanel());
    }

    private JPanel createSharedFolderPanel() {
        JPanel sharedFolderPanel = new JPanel(new BorderLayout(10, 0));
        sharedFolderPanel.setBorder(createTitledBorder("Root of the P2P shared folder"));

        JPanel innerPanel = new JPanel(new BorderLayout(10, 0));
        sharedFolderTextField = new JTextField(30);
        innerPanel.add(sharedFolderTextField, BorderLayout.CENTER);
        setSharedFolderButton = new JButton("Set");
        innerPanel.add(setSharedFolderButton, BorderLayout.EAST);

        sharedFolderPanel.add(innerPanel, BorderLayout.CENTER);
        return sharedFolderPanel;
    }

    private JPanel createDestinationFolderPanel() {
        JPanel destinationFolderPanel = new JPanel(new BorderLayout(10, 0));
        destinationFolderPanel.setBorder(createTitledBorder("Destination folder"));

        JPanel interiorPanel = new JPanel(new BorderLayout(10, 0));
        destinationFolderTextField = new JTextField(30);
        interiorPanel.add(destinationFolderTextField, BorderLayout.CENTER);
        setDestinationButton = new JButton("Set");
        interiorPanel.add(setDestinationButton, BorderLayout.EAST);

        destinationFolderPanel.add(interiorPanel, BorderLayout.CENTER);
        return destinationFolderPanel;
    }

    private TitledBorder createTitledBorder(String title) {
        TitledBorder border = BorderFactory.createTitledBorder(title);
        border.setTitleColor(UIManager.getColor("TitledBorder.titleColor"));
        border.setTitleFont(UIManager.getFont("Label.font"));
        return border;
    }
}