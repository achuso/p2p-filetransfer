package gui;

import p2p.Node;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class CenterPanel extends JPanel {
    // UI components
    private JCheckBox rootCheckBox;

    private JList<String> folderExclusions;
    private final DefaultListModel<String> folderExclusionsModel;

    private JList<String> fileExclusions;
    private final DefaultListModel<String> fileExclusionsModel;
    private JButton delFileButton;

    // reference to our Node
    private final Node node;

    public CenterPanel(Node node) {
        this.node = node;

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createTitledBorder("Settings"));

        // Initialize models
        folderExclusionsModel = new DefaultListModel<>();
        fileExclusionsModel   = new DefaultListModel<>();

        JPanel westPanel = createFolderExclusionsPanel();
        JPanel eastPanel = createFileExclusionsPanel();

        add(westPanel, BorderLayout.WEST);
        add(eastPanel, BorderLayout.EAST);
    }

    // -------------------------------------------------------------------------
    // Folder Exclusions Panel
    // -------------------------------------------------------------------------
    private JPanel createFolderExclusionsPanel() {
        JPanel folderPanel = new JPanel(new BorderLayout(5, 5));
        folderPanel.setBorder(BorderFactory.createTitledBorder("Folder exclusion"));

        // Root checkbox => check new files only in the root
        rootCheckBox = new JCheckBox("Check new files only in the root");
        rootCheckBox.setSelected(node.isCheckRootOnly());
        rootCheckBox.addActionListener(e -> {
            boolean selected = rootCheckBox.isSelected();
            node.setCheckRootOnly(selected);
            // Immediately apply
            node.applyExclusionsNow();
        });
        folderPanel.add(rootCheckBox, BorderLayout.NORTH);

        folderExclusions = new JList<>(folderExclusionsModel);
        // so it doesn’t shrink
        folderExclusions.setPreferredSize(new Dimension(180, 120));

        folderPanel.add(new JScrollPane(folderExclusions), BorderLayout.CENTER);

        // Buttons => Add / Del
        JPanel btnPanel = new JPanel(new GridLayout(1, 2, 5, 0));
        JButton addFolderButton = new JButton("Add");
        JButton delFolderButton = new JButton("Del");

        addFolderButton.addActionListener(ev -> addFolderExclusion());
        delFolderButton.addActionListener(ev -> removeFolderExclusion());

        btnPanel.add(addFolderButton);
        btnPanel.add(delFolderButton);

        folderPanel.add(btnPanel, BorderLayout.SOUTH);

        return folderPanel;
    }

    private void addFolderExclusion() {
        // Let user pick a subfolder within node.getSharedFolder()
        File sharedRoot = node.getSharedFolder();
        if (sharedRoot == null || !sharedRoot.isDirectory()) {
            JOptionPane.showMessageDialog(this,
                    "No valid shared folder is set. Please set a shared folder first.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Start the chooser from the shared root
        JFileChooser chooser = new JFileChooser(sharedRoot);
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        int res = chooser.showOpenDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            File chosen = chooser.getSelectedFile();
            // Check if chosen is indeed inside the shared folder
            if (!chosen.getAbsolutePath().startsWith(sharedRoot.getAbsolutePath())) {
                JOptionPane.showMessageDialog(this,
                        "Selected folder is not inside the shared folder:\n" + sharedRoot,
                        "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Add to node
            node.addExcludedFolder(chosen);
            // Re-apply exclusions immediately
            node.applyExclusionsNow();

            // Update the folderExclusionsModel
            folderExclusionsModel.addElement(chosen.getAbsolutePath());
        }
    }

    private void removeFolderExclusion() {
        int idx = folderExclusions.getSelectedIndex();
        if (idx >= 0) {
            String path = folderExclusionsModel.getElementAt(idx);
            node.removeExcludedFolder(new File(path));
            node.applyExclusionsNow();
            folderExclusionsModel.remove(idx);
        }
    }

    // -------------------------------------------------------------------------
    // File Exclusions Panel => e.g. *.txt
    // -------------------------------------------------------------------------
    private JPanel createFileExclusionsPanel() {
        JPanel filePanel = new JPanel(new BorderLayout(5, 5));
        filePanel.setBorder(BorderFactory.createTitledBorder("Exclude files matching these masks"));

        fileExclusions = new JList<>(fileExclusionsModel);
        fileExclusions.setPreferredSize(new Dimension(180, 120));

        filePanel.add(new JScrollPane(fileExclusions), BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new GridLayout(1, 2, 5, 0));
        JButton addFileButton = new JButton("Add");
        delFileButton = new JButton("Del");

        addFileButton.addActionListener(e -> addFileExclusion());
        delFileButton.addActionListener(e -> removeFileExclusion());

        btnPanel.add(addFileButton);
        btnPanel.add(delFileButton);

        filePanel.add(btnPanel, BorderLayout.SOUTH);

        return filePanel;
    }

    private void addFileExclusion() {
        String mask = JOptionPane.showInputDialog(this,
                "Enter file mask to exclude (e.g. *.txt):",
                "Add File Mask",
                JOptionPane.QUESTION_MESSAGE);
        if (mask != null && !mask.trim().isEmpty()) {
            mask = mask.trim().toLowerCase();
            node.addExcludedMask(mask);
            node.applyExclusionsNow();

            fileExclusionsModel.addElement(mask);
        }
    }

    private void removeFileExclusion() {
        int idx = fileExclusions.getSelectedIndex();
        if (idx >= 0) {
            String mask = fileExclusionsModel.getElementAt(idx);
            node.removeExcludedMask(mask);
            node.applyExclusionsNow();

            fileExclusionsModel.remove(idx);
        }
    }
}