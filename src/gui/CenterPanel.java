package gui;

import p2p.Node;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class CenterPanel extends JPanel {
    private JCheckBox rootCheckBox;
    private final Dimension dimensions;

    private JList<String> folderExclusions;
    private final DefaultListModel<String> folderExclusionsModel;

    private JList<String> fileExclusions;
    private final DefaultListModel<String> fileExclusionsModel;

    private final Node node;

    public CenterPanel(Node node) {
        this.node = node;
        this.dimensions = new Dimension(180, 120);

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createTitledBorder("Settings"));

        folderExclusionsModel = new DefaultListModel<>();
        fileExclusionsModel   = new DefaultListModel<>();

        JPanel westPanel = createFolderExclusionsPanel();
        JPanel eastPanel = createFileExclusionsPanel();

        add(westPanel, BorderLayout.WEST);
        add(eastPanel, BorderLayout.EAST);
    }

    private JPanel createFolderExclusionsPanel() {
        JPanel folderPanel = new JPanel(new BorderLayout(5, 5));
        folderPanel.setBorder(BorderFactory.createTitledBorder("Folder exclusion"));

        rootCheckBox = new JCheckBox("Check new files only in the root");
        rootCheckBox.setSelected(node.isCheckRootOnly());
        rootCheckBox.addActionListener(e -> {
            boolean selected = rootCheckBox.isSelected();
            node.setCheckRootOnly(selected);
            node.applyExclusionsNow();
        });
        folderPanel.add(rootCheckBox, BorderLayout.NORTH);

        folderExclusions = new JList<>(folderExclusionsModel);
        // so it doesn’t shrink
        folderExclusions.setPreferredSize(this.dimensions);

        folderPanel.add(new JScrollPane(folderExclusions), BorderLayout.CENTER);

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
        File sharedRoot = node.getSharedFolder();
        if (sharedRoot == null || !sharedRoot.isDirectory()) {
            JOptionPane.showMessageDialog(this,
                    "Please set a valid shared folder is set.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        JFileChooser chooser = new JFileChooser(sharedRoot);
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        int res = chooser.showOpenDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            File chosen = chooser.getSelectedFile();
            if (!chosen.getAbsolutePath().startsWith(sharedRoot.getAbsolutePath())) {
                JOptionPane.showMessageDialog(this,
                        "Selected folder is not inside the shared folder:\n" + sharedRoot,
                        "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            node.addExcludedFolder(chosen);
            node.applyExclusionsNow();

            folderExclusionsModel.addElement(chosen.getAbsolutePath());
        }
    }

    private void removeFolderExclusion() {
        int i = folderExclusions.getSelectedIndex();
        if (i >= 0) {
            String path = folderExclusionsModel.getElementAt(i);
            node.removeExcludedFolder(new File(path));
            node.applyExclusionsNow();
            folderExclusionsModel.remove(i);
        }
    }

    private JPanel createFileExclusionsPanel() {
        JPanel filePanel = new JPanel(new BorderLayout(5, 5));
        filePanel.setBorder(BorderFactory.createTitledBorder("Exclude files matching these masks"));

        fileExclusions = new JList<>(fileExclusionsModel);
        fileExclusions.setPreferredSize(this.dimensions);

        filePanel.add(new JScrollPane(fileExclusions), BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new GridLayout(1, 2, 5, 0));
        JButton addFileButton = new JButton("Add");
        JButton delFileButton = new JButton("Del");

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
        int i = fileExclusions.getSelectedIndex();
        if (i >= 0) {
            String mask = fileExclusionsModel.getElementAt(i);
            node.removeExcludedMask(mask);
            node.applyExclusionsNow();

            fileExclusionsModel.remove(i);
        }
    }
}