package gui;

import javax.swing.*;
import java.awt.*;

public class BottomPanel extends JPanel {
    public JList<String> downloadingFiles, foundFiles;
    public JTextField searchField;
    public JButton searchButton;

    public BottomPanel() {
        setLayout(new BorderLayout(10, 10));
        add(createFilesPanel(), BorderLayout.CENTER);
        add(createSearchPanel(), BorderLayout.SOUTH);
    }

    // Container for "Downloading Files" and "Found Files" panels
    private JPanel createFilesPanel() {
        JPanel downloadingPanel = createDownloadingFilesPanel();
        JPanel foundPanel = createFoundFilesPanel();

        JPanel filesPanel = new JPanel(new GridLayout(2, 1, 10, 10));
        filesPanel.add(downloadingPanel);
        filesPanel.add(foundPanel);

        return filesPanel;
    }

    private JPanel createDownloadingFilesPanel() {
        JPanel downloadingPanel = new JPanel(new BorderLayout(10, 0));
        downloadingPanel.setBorder(BorderFactory.createTitledBorder("Downloading files"));
        downloadingFiles = new JList<>();
        downloadingPanel.add(new JScrollPane(downloadingFiles), BorderLayout.CENTER);
        return downloadingPanel;
    }

    private JPanel createFoundFilesPanel() {
        JPanel foundPanel = new JPanel(new BorderLayout(10, 0));
        foundPanel.setBorder(BorderFactory.createTitledBorder("Found files"));
        foundFiles = new JList<>();
        foundPanel.add(new JScrollPane(foundFiles), BorderLayout.CENTER);
        return foundPanel;
    }

    private JPanel createSearchPanel() {
        JPanel searchPanel = new JPanel(new BorderLayout(5, 0));
        searchPanel.setBorder(BorderFactory.createTitledBorder("Search"));
        searchField = new JTextField(30);
        searchButton = new JButton("Search");
        searchPanel.add(searchField, BorderLayout.CENTER);
        searchPanel.add(searchButton, BorderLayout.EAST);
        return searchPanel;
    }
}