package gui;

import p2p.Node;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.io.File;

public class TopPanel extends JPanel {
    private JTextField sharedFolderTextField;
    private JTextField destinationFolderTextField;

    private final Node node;

    public TopPanel(Node node) {
        this.node = node;
        setLayout(new GridLayout(2, 1, 5, 5));
        add(createSharedFolderPanel());
        add(createDestinationFolderPanel());
    }

    private JPanel createSharedFolderPanel() {
        JPanel sharedFolderPanel = new JPanel(new BorderLayout(10, 0));
        sharedFolderPanel.setBorder(createTitledBorder("Root of the P2P shared folder"));

        sharedFolderTextField = new JTextField(30);
        sharedFolderTextField.setEditable(false);
        sharedFolderPanel.add(sharedFolderTextField, BorderLayout.CENTER);

        JButton setSharedFolderButton = getjButton();
        sharedFolderPanel.add(setSharedFolderButton, BorderLayout.EAST);

        return sharedFolderPanel;
    }

    private JButton getjButton() {
        JButton setSharedFolderButton = new JButton("Browse");
        setSharedFolderButton.addActionListener(_ -> {
            String path = selectFolder();
            if (path != null) {
                try {
                    node.setSharedFolder(path);
                    sharedFolderTextField.setText(path);
                    JOptionPane.showMessageDialog(this,
                            "Shared folder now set to: " + path,
                            "Success",
                            JOptionPane.INFORMATION_MESSAGE);
                }
                catch (RuntimeException rte) {
                    JOptionPane.showMessageDialog(this,
                            rte.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        return setSharedFolderButton;
    }

    private JPanel createDestinationFolderPanel() {
        JPanel destinationFolderPanel = new JPanel(new BorderLayout(10, 0));
        destinationFolderPanel.setBorder(createTitledBorder("Destination folder"));

        destinationFolderTextField = new JTextField(30);
        destinationFolderTextField.setEditable(false);
        destinationFolderPanel.add(destinationFolderTextField, BorderLayout.CENTER);

        JButton setDestinationButton = getBrowseButton();
        destinationFolderPanel.add(setDestinationButton, BorderLayout.EAST);

        return destinationFolderPanel;
    }

    private JButton getBrowseButton() {
        JButton setDestinationButton = new JButton("Browse");
        setDestinationButton.addActionListener(_ -> {
            String path = selectFolder();
            if (path != null) {
                try {
                    node.setDownloadFolder(path);
                    destinationFolderTextField.setText(path);
                    JOptionPane.showMessageDialog(this,
                            "Download folder now set to: " + path,
                            "Success",
                            JOptionPane.INFORMATION_MESSAGE);
                }
                catch (RuntimeException rte) {
                    JOptionPane.showMessageDialog(this,
                            rte.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        return setDestinationButton;
    }

    private String selectFolder() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int returnValue = fileChooser.showOpenDialog(this);

        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File selectedFolder = fileChooser.getSelectedFile();
            return selectedFolder.getAbsolutePath();
        }
        return null;
    }

    private Border createTitledBorder(String title) {
        TitledBorder border = BorderFactory.createTitledBorder(title);
        border.setTitleColor(UIManager.getColor("TitledBorder.titleColor"));
        border.setTitleFont(UIManager.getFont("Label.font"));
        return border;
    }
}