package GUI;

import javax.swing.*;
import java.awt.*;

public class CenterPanel extends JPanel {
    public JCheckBox rootCheckBox;
    public JList<String> folderExclusions;
    public JList<String> fileExclusions;
    public JButton addFolderButton, delFolderButton, addFileButton, delFileButton;

    private final DefaultListModel<String> folderExclusionsModel;
    private final DefaultListModel<String> fileExclusionsModel;

    public CenterPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createTitledBorder("Settings"));

        folderExclusionsModel = new DefaultListModel<>();
        fileExclusionsModel = new DefaultListModel<>();

        add(createRootCheckBox(), BorderLayout.NORTH);
        add(createExclusionPanel(), BorderLayout.CENTER);
    }

    private JCheckBox createRootCheckBox() {
        rootCheckBox = new JCheckBox("Check new files only in the root");
        return rootCheckBox;
    }

    private JPanel createExclusionPanel() {
        JPanel exclusionPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        JPanel folderPanel = createFolderExclusionsPanel();
        JPanel filePanel = createFileExclusionsPanel();
        exclusionPanel.add(folderPanel);
        exclusionPanel.add(filePanel);
        return exclusionPanel;
    }

    private JPanel createFolderExclusionsPanel() {
        JPanel folderPanel = new JPanel(new BorderLayout(5, 5));
        folderPanel.add(new JLabel("Exclude files under these folders:"), BorderLayout.NORTH);

        folderExclusions = new JList<>(folderExclusionsModel);
        folderPanel.add(new JScrollPane(folderExclusions), BorderLayout.CENTER);

        JPanel folderButtons = new JPanel(new GridLayout(1, 2, 5, 0));
        addFolderButton = new JButton("Add");
        delFolderButton = new JButton("Del");

        addFolderButton.addActionListener(_ -> addFolderExclusion());
        delFolderButton.addActionListener(_ -> removeFolderExclusion());

        folderButtons.add(addFolderButton);
        folderButtons.add(delFolderButton);
        folderPanel.add(folderButtons, BorderLayout.SOUTH);

        return folderPanel;
    }

    private JPanel createFileExclusionsPanel() {
        JPanel filePanel = new JPanel(new BorderLayout(5, 5));
        filePanel.add(new JLabel("Exclude files matching these masks:"), BorderLayout.NORTH);

        fileExclusions = new JList<>(fileExclusionsModel);
        filePanel.add(new JScrollPane(fileExclusions), BorderLayout.CENTER);

        JPanel fileButtons = new JPanel(new GridLayout(1, 2, 5, 0));
        addFileButton = new JButton("Add");
        delFileButton = new JButton("Del");

        addFileButton.addActionListener(_ -> addFileExclusion());
        delFileButton.addActionListener(_ -> removeFileExclusion());

        fileButtons.add(addFileButton);
        fileButtons.add(delFileButton);
        filePanel.add(fileButtons, BorderLayout.SOUTH);

        return filePanel;
    }

    private void addFolderExclusion() {
        String newFolder = JOptionPane.showInputDialog(this, "Enter folder to exclude:");
        if (newFolder != null && !newFolder.trim().isEmpty() && !folderExclusionsModel.contains(newFolder))
            folderExclusionsModel.addElement(newFolder);
    }

    private void removeFolderExclusion() {
        int selectedIndex = folderExclusions.getSelectedIndex();
        if (selectedIndex != -1)
            folderExclusionsModel.remove(selectedIndex);
    }

    private void addFileExclusion() {
        String newFileMask = JOptionPane.showInputDialog(this, "Enter file mask to exclude:");
        if (newFileMask != null && !newFileMask.trim().isEmpty() && !fileExclusionsModel.contains(newFileMask))
            fileExclusionsModel.addElement(newFileMask);
    }

    private void removeFileExclusion() {
        int selectedIndex = fileExclusions.getSelectedIndex();
        if (selectedIndex != -1)
            fileExclusionsModel.remove(selectedIndex);
    }

//    public JList<String> getFolderExclusions() { return this.folderExclusions; }
//    public JList<String> getFileExclusions() { return this.fileExclusions; }
}