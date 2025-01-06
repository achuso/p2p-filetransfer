package gui;

import p2p.Node;
import p2p.Node.DownloadProgress;
import p2p.Node.FoundFile;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

public class BottomPanel extends JPanel {

    private final Node node;

    private DefaultListModel<String> downloadingModel;
    private DefaultListModel<String> foundModel;

    private JList<String> foundFilesList;
    private JTextField searchField;

    public BottomPanel(Node node) {
        this.node = node;
        setLayout(new BorderLayout(10, 10));
        add(createCenterPanel(), BorderLayout.CENTER);
        add(createSearchPanel(), BorderLayout.SOUTH);

        Timer timer = new Timer(1000, e -> refreshLists(null)); // refresh every second
        timer.start();
    }

    private JPanel createCenterPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 1, 10, 10));

        JPanel downloadingPanel = new JPanel(new BorderLayout(5,5));
        downloadingPanel.setBorder(BorderFactory.createTitledBorder("Downloading files"));

        downloadingModel = new DefaultListModel<>();
        JList<String> downloadingFilesList = new JList<>(downloadingModel);

        downloadingPanel.add(new JScrollPane(downloadingFilesList), BorderLayout.CENTER);
        panel.add(downloadingPanel);

        JPanel foundPanel = new JPanel(new BorderLayout(5,5));
        foundPanel.setBorder(BorderFactory.createTitledBorder("Found files"));

        foundModel = new DefaultListModel<>();
        foundFilesList = new JList<>(foundModel);
        foundPanel.add(new JScrollPane(foundFilesList), BorderLayout.CENTER);

        // double click to download
        foundFilesList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int index = foundFilesList.locationToIndex(e.getPoint());
                    if (index >= 0) {
                        String displayName = foundModel.getElementAt(index);
                        FoundFile foundFile = findFoundFileByName(displayName);

                        if (foundFile != null) {
                            node.multiSourceDownload(foundFile.fileHash, foundFile.fileName);
                            JOptionPane.showMessageDialog(BottomPanel.this,
                                    "Download started for: " + foundFile.fileName,
                                    "Info",
                                    JOptionPane.INFORMATION_MESSAGE);
                        }
                    }
                }
            }
        });

        panel.add(foundPanel);

        return panel;
    }

    private FoundFile findFoundFileByName(String filename) {
        List<FoundFile> allFoundFiles = node.getFoundFiles();
        for (FoundFile foundFile : allFoundFiles) {
            if (foundFile.fileName.equals(filename))
                return foundFile;
        }
        return null;
    }

    private JPanel createSearchPanel() {
        JPanel panel = new JPanel(new BorderLayout(5,5));
        panel.setBorder(BorderFactory.createTitledBorder("Search"));

        searchField = new JTextField();
        JButton searchBtn = new JButton("Search");
        searchBtn.addActionListener(e -> {
            String keyword = searchField.getText().trim();
            refreshLists(keyword);
        });

        panel.add(searchField, BorderLayout.CENTER);
        panel.add(searchBtn, BorderLayout.EAST);

        return panel;
    }

    private void refreshLists(String keyword) {
        if (keyword != null)
            keyword = keyword.toLowerCase();

        downloadingModel.clear();
        List<DownloadProgress> downloads = node.listActiveDownloads();
        for (DownloadProgress progress : downloads) {
            double percentage = progress.getPercent();
            String display = String.format("%s (%.2f%%)", progress.fileName, percentage);
            if (keyword == null || progress.fileName.toLowerCase().contains(keyword)) {
                downloadingModel.addElement(display);
            }
        }

        foundModel.clear();
        List<FoundFile> found = node.getFoundFiles();
        for (FoundFile foundFile : found) {
            if (keyword == null || foundFile.fileName.toLowerCase().contains(keyword)) {
                foundModel.addElement(foundFile.fileName);
            }
        }
    }
}