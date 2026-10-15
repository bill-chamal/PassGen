package com.passgen;

import com.formdev.flatlaf.intellijthemes.FlatOneDarkIJTheme;
import javax.swing.*;

/**
 * Application entry point.
 * Installs the FlatLaf One-Dark IntelliJ theme for a modern look,
 * then launches the main window on the Event Dispatch Thread.
 */
public class Main {
    public static void main(String[] args) {
        // Install FlatLaf dark theme before any Swing components are created
        FlatOneDarkIJTheme.setup();

        // Launch UI on the Event Dispatch Thread (Swing best practice)
        SwingUtilities.invokeLater(() -> {
            MainWindow window = new MainWindow();
            window.setVisible(true);
        });
    }
}
