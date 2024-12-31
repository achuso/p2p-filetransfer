package gui;

import javax.swing.*;
import java.awt.*;

//import java.util.List;
//import java.util.stream.Collectors;
//import java.util.stream.IntStream;

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

        add(createFolderExclusionsPanel(), BorderLayout.WEST);
        add(createFileExclusionsPanel(), BorderLayout.EAST);
    }

    private JPanel createFolderExclusionsPanel() {
        JPanel folderPanel = new JPanel(new BorderLayout(5, 5));
        folderPanel.setBorder(BorderFactory.createTitledBorder("Folder exclusion"));

        rootCheckBox = new JCheckBox("Check new files only in the root");
        folderPanel.add(rootCheckBox, BorderLayout.NORTH);

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
        filePanel.setBorder(BorderFactory.createTitledBorder("Exclude files matching these masks"));

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
        // System.out.println(getFolderExclusions());
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
        // System.out.println(getFileExclusions());
    }

    private void removeFileExclusion() {
        int selectedIndex = fileExclusions.getSelectedIndex();
        if (selectedIndex != -1)
            fileExclusionsModel.remove(selectedIndex);
    }

//    public List<String> getFolderExclusions() {
//        return IntStream.range(0, folderExclusionsModel.size())
//                .mapToObj(folderExclusionsModel::getElementAt)
//                .collect(Collectors.toList());
//    }

//    public List<String> getFileExclusions() {
//        return IntStream.range(0, fileExclusionsModel.size())
//                .mapToObj(fileExclusionsModel::getElementAt)
//                .collect(Collectors.toList());
//    }
}