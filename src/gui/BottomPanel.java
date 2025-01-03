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

    private JList<String> downloadingFilesList;
    private JList<String> foundFilesList;
    private JTextField searchField;

    public BottomPanel(Node node) {
        this.node = node;
        setLayout(new BorderLayout(10, 10));
        add(createCenterPanel(), BorderLayout.CENTER);
        add(createSearchPanel(), BorderLayout.SOUTH);

        // refresh every second
        Timer timer = new Timer(1000, e -> refreshLists(null));
        timer.start();
    }

    private JPanel createCenterPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 1, 10, 10));

        // Downloading files
        JPanel downloadingPanel = new JPanel(new BorderLayout(5,5));
        downloadingPanel.setBorder(BorderFactory.createTitledBorder("Downloading files"));
        downloadingModel = new DefaultListModel<>();
        downloadingFilesList = new JList<>(downloadingModel);
        downloadingPanel.add(new JScrollPane(downloadingFilesList), BorderLayout.CENTER);

        panel.add(downloadingPanel);

        // Found files
        JPanel foundPanel = new JPanel(new BorderLayout(5,5));
        foundPanel.setBorder(BorderFactory.createTitledBorder("Found files"));
        foundModel = new DefaultListModel<>();
        foundFilesList = new JList<>(foundModel);
        foundPanel.add(new JScrollPane(foundFilesList), BorderLayout.CENTER);

        // Double-click => download
        foundFilesList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int idx = foundFilesList.locationToIndex(e.getPoint());
                    if (idx >= 0) {
                        String displayName = foundModel.getElementAt(idx);
                        FoundFile ff = findFoundFileByName(displayName);
                        if (ff != null) {
                            node.multiSourceDownload(ff.fileHash, ff.fileName);
                            JOptionPane.showMessageDialog(BottomPanel.this,
                                    "Download started for: " + ff.fileName,
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
        List<FoundFile> all = node.getFoundFiles();
        for (FoundFile ff : all) {
            if (ff.fileName.equals(filename)) {
                return ff;
            }
        }
        return null;
    }

    private JPanel createSearchPanel() {
        JPanel panel = new JPanel(new BorderLayout(5,5));
        panel.setBorder(BorderFactory.createTitledBorder("Search"));

        searchField = new JTextField();
        JButton searchBtn = new JButton("Search");
        searchBtn.addActionListener(e -> {
            String kw = searchField.getText().trim();
            refreshLists(kw);
        });

        panel.add(searchField, BorderLayout.CENTER);
        panel.add(searchBtn, BorderLayout.EAST);

        return panel;
    }

    private void refreshLists(String keyword) {
        if (keyword != null) {
            keyword = keyword.toLowerCase();
        }

        // 1) downloads
        downloadingModel.clear();
        List<DownloadProgress> downloads = node.listActiveDownloads();
        for (DownloadProgress dp : downloads) {
            double pct = dp.getPercent();
            String display = String.format("%s (%.2f%%)", dp.fileName, pct);
            if (keyword == null || dp.fileName.toLowerCase().contains(keyword)) {
                downloadingModel.addElement(display);
            }
        }

        // 2) found
        foundModel.clear();
        List<FoundFile> found = node.getFoundFiles();
        for (FoundFile ff : found) {
            if (keyword == null || ff.fileName.toLowerCase().contains(keyword)) {
                foundModel.addElement(ff.fileName);
            }
        }
    }
}