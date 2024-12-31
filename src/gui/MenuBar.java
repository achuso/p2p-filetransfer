package gui;

import javax.swing.*;

public class MenuBar extends JMenuBar {
    final String aboutText;

    public MenuBar(JFrame parentFrame) {
        this.aboutText = """
                CSE471 Term Project\s
                P2P File Sharing Application
                
                Name: Onat Ribar
                Student ID: 20210702099
                """
        ;
        this.add(createFileMenu());
        this.add(createHelpMenu(parentFrame));
    }

    private JMenu createFileMenu() {
        JMenu fileMenu = new JMenu("File");
        JMenuItem connectItem = new JMenuItem("Connect");
        JMenuItem disconnectItem = new JMenuItem("Disconnect");
        JMenuItem exitItem = new JMenuItem("Exit");

        exitItem.addActionListener(event -> System.exit(0));

        fileMenu.add(connectItem);
        fileMenu.add(disconnectItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);

        return fileMenu;
    }

    private JMenu createHelpMenu(JFrame parentFrame) {
        JMenu helpMenu = new JMenu("Help");
        JMenuItem aboutItem = new JMenuItem("About");
        aboutItem.addActionListener(event -> JOptionPane.showMessageDialog(
                parentFrame,
                this.aboutText,
                "About",
                JOptionPane.INFORMATION_MESSAGE
        ));
        helpMenu.add(aboutItem);

        return helpMenu;
    }
}