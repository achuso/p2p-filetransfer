package gui;

import p2p.Node;
import p2p.Node.DownloadProgress;
import p2p.Node.FoundFile;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * Updated BottomPanel:
 * 1) "Downloading files" panel (renamed from "Downloads") on top,
 * 2) "Found files" panel on bottom,
 * 3) Double-click on "Found files" => triggers multiSourceDownload(...),
 * 4) Single "search" text => filters both lists by name,
 * 5) 100% completed downloads remain in "Downloading files."
 */
public class BottomPanel extends JPanel {
    private final Node node;

    // Models
    private DefaultListModel<String> downloadingModel;
    private DefaultListModel<String> foundModel;

    private JList<String> downloadingFilesList;
    private JList<String> foundFilesList;
    private JTextField searchField;

    public BottomPanel(Node node) {
        this.node = node;

        setLayout(new BorderLayout(10, 10));
        add(createCenterPanel(), BorderLayout.CENTER);
        add(createSearchPanel(), BorderLayout.SOUTH);

        // Refresh the lists every second
        Timer timer = new Timer(1000, e -> refreshLists(null));
        timer.start();
    }

    /**
     * The main area => Top: "Downloading files", Bottom: "Found files"
     */
    private JPanel createCenterPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 1, 10, 10));

        // 1) Downloading files
        JPanel downloadingPanel = new JPanel(new BorderLayout(5,5));
        downloadingPanel.setBorder(BorderFactory.createTitledBorder("Downloading files"));

        downloadingModel = new DefaultListModel<>();
        downloadingFilesList = new JList<>(downloadingModel);
        downloadingPanel.add(new JScrollPane(downloadingFilesList), BorderLayout.CENTER);

        panel.add(downloadingPanel);

        // 2) Found files
        JPanel foundPanel = new JPanel(new BorderLayout(5,5));
        foundPanel.setBorder(BorderFactory.createTitledBorder("Found files"));

        foundModel = new DefaultListModel<>();
        foundFilesList = new JList<>(foundModel);
        foundPanel.add(new JScrollPane(foundFilesList), BorderLayout.CENTER);

        // Double-click on found => triggers multiSourceDownload
        foundFilesList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {  // double-click
                    int index = foundFilesList.locationToIndex(e.getPoint());
                    if (index >= 0) {
                        String displayName = foundModel.getElementAt(index);
                        // We must find the FoundFile object that has that name
                        FoundFile ff = findFoundFileByName(displayName);
                        if (ff != null) {
                            // Trigger multiSourceDownload
                            node.multiSourceDownload(ff.fileHash, ff.fileName);
                            JOptionPane.showMessageDialog(
                                    BottomPanel.this,
                                    "Download started for: " + ff.fileName,
                                    "Info",
                                    JOptionPane.INFORMATION_MESSAGE
                            );
                        }
                    }
                }
            }
        });

        panel.add(foundPanel);

        return panel;
    }

    private FoundFile findFoundFileByName(String filename) {
        List<FoundFile> all = node.getFoundFiles();
        for (FoundFile ff : all) {
            if (ff.fileName.equals(filename)) {
                return ff;
            }
        }
        return null;
    }

    /**
     * "Search" panel => text field + "Search" button => filters both lists
     */
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

    /**
     * Refresh the "downloading files" and "found files" lists.
     * If keyword != null => only show matches.
     * Completed downloads remain in the "downloading" list at 100%.
     */
    private void refreshLists(String keyword) {
        if (keyword != null) {
            keyword = keyword.toLowerCase();
        }

        // 1) Downloading files
        downloadingModel.clear();
        List<DownloadProgress> downloads = node.listActiveDownloads();
        for (DownloadProgress dp : downloads) {
            String name = dp.fileName;
            double pct = dp.getPercent();
            String display = String.format("%s (%.2f%%)", name, pct);

            if (keyword == null || name.toLowerCase().contains(keyword)) {
                downloadingModel.addElement(display);
            }
        }

        // 2) Found files
        foundModel.clear();
        List<FoundFile> foundFiles = node.getFoundFiles();
        for (FoundFile ff : foundFiles) {
            String name = ff.fileName;
            if (keyword == null || name.toLowerCase().contains(keyword)) {
                // Show the name
                foundModel.addElement(name);
            }
        }
    }
}