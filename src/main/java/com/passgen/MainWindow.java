package com.passgen;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * Main application window for the modernised Password Generator.
 * <p>
 * Built with Swing + FlatLaf for a clean, contemporary appearance.
 * Layout is organised into logical panels: controls on the left,
 * output & strength meter on the right.
 */
public class MainWindow extends JFrame {

    // ── Core engine & entropy harvester ──────────────────────────
    private final PasswordEngine engine = new PasswordEngine();
    private final EntropyHarvester harvester = new EntropyHarvester(engine);

    // ── Character-set controls ───────────────────────────────────
    private final JCheckBox chkLowercase   = new JCheckBox("Lowercase (a-z)", true);
    private final JCheckBox chkUppercase   = new JCheckBox("Uppercase (A-Z)");
    private final JCheckBox chkDigits      = new JCheckBox("Digits (0-9)");
    private final JCheckBox chkSymbols     = new JCheckBox("Symbols (!@#…)");
    private final JCheckBox chkExcludeAmb  = new JCheckBox("Exclude ambiguous (O 0 l 1 I)");

    // ── Length & batch controls ───────────────────────────────────
    private final JSpinner  spnLength      = new JSpinner(new SpinnerNumberModel(16, 4, 256, 1));
    private final JSpinner  spnBatchCount  = new JSpinner(new SpinnerNumberModel(1, 1, 1000, 1));

    // ── Delimiter controls ───────────────────────────────────────
    private final JCheckBox chkDelimiter   = new JCheckBox("Chunk with delimiter");
    private final JSpinner  spnBlockSize   = new JSpinner(new SpinnerNumberModel(4, 2, 32, 1));
    private final JTextField txtDelimiter   = new JTextField("-", 3);

    // ── Entropy harvester toggle ─────────────────────────────────
    private final JCheckBox chkEntropy     = new JCheckBox("Mouse entropy harvester");
    private final JLabel    lblEntropyStatus = new JLabel("Entropy: inactive");

    // ── Output area ──────────────────────────────────────────────
    private final JTextArea txtOutput      = new JTextArea();
    private final StrengthMeterPanel strengthMeter = new StrengthMeterPanel();

    // ── Buttons ──────────────────────────────────────────────────
    private final JButton btnGenerate = new JButton("⚡ Generate");
    private final JButton btnCopy     = new JButton("📋 Copy All");
    private final JButton btnClear    = new JButton("🗑 Clear");

    public MainWindow() {
        super("PassGen 2.0 — Secure Password Generator");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        initComponents();
        initMenuBar();
        initListeners();
        setMinimumSize(new Dimension(780, 520));
        setSize(840, 580);
        setLocationRelativeTo(null);

        // Attach the entropy harvester to the entire content pane
        getContentPane().addMouseMotionListener(harvester);
        harvester.setStatusLabel(lblEntropyStatus);

        // Initial strength meter update
        updateStrengthMeter();
    }

    // ═══════════════════════════════════════════════════════════════
    //  UI CONSTRUCTION
    // ═══════════════════════════════════════════════════════════════

    private void initComponents() {
        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBorder(new EmptyBorder(12, 12, 12, 12));

        // ── LEFT: Control panel ──────────────────────────────────
        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.Y_AXIS));
        controlPanel.setBorder(BorderFactory.createTitledBorder("Configuration"));
        controlPanel.setPreferredSize(new Dimension(280, 0));

        // Character sets section
        JPanel charPanel = new JPanel();
        charPanel.setLayout(new BoxLayout(charPanel, BoxLayout.Y_AXIS));
        charPanel.setBorder(BorderFactory.createTitledBorder("Character Sets"));
        charPanel.add(chkLowercase);
        charPanel.add(chkUppercase);
        charPanel.add(chkDigits);
        charPanel.add(chkSymbols);
        charPanel.add(Box.createVerticalStrut(4));
        charPanel.add(chkExcludeAmb);
        controlPanel.add(charPanel);
        controlPanel.add(Box.createVerticalStrut(8));

        // Length section
        JPanel lengthPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lengthPanel.setBorder(BorderFactory.createTitledBorder("Password Length"));
        lengthPanel.add(new JLabel("Length:"));
        spnLength.setPreferredSize(new Dimension(70, 28));
        lengthPanel.add(spnLength);
        controlPanel.add(lengthPanel);
        controlPanel.add(Box.createVerticalStrut(8));

        // Delimiter section
        JPanel delimPanel = new JPanel();
        delimPanel.setLayout(new BoxLayout(delimPanel, BoxLayout.Y_AXIS));
        delimPanel.setBorder(BorderFactory.createTitledBorder("Delimiter Formatting"));
        delimPanel.add(chkDelimiter);
        JPanel delimOpts = new JPanel(new FlowLayout(FlowLayout.LEFT));
        delimOpts.add(new JLabel("Block size:"));
        spnBlockSize.setPreferredSize(new Dimension(55, 28));
        delimOpts.add(spnBlockSize);
        delimOpts.add(new JLabel("Char:"));
        txtDelimiter.setPreferredSize(new Dimension(40, 28));
        delimOpts.add(txtDelimiter);
        delimPanel.add(delimOpts);
        // Initially disable delimiter options
        spnBlockSize.setEnabled(false);
        txtDelimiter.setEnabled(false);
        controlPanel.add(delimPanel);
        controlPanel.add(Box.createVerticalStrut(8));

        // Batch section
        JPanel batchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        batchPanel.setBorder(BorderFactory.createTitledBorder("Batch Mode"));
        batchPanel.add(new JLabel("Count:"));
        spnBatchCount.setPreferredSize(new Dimension(70, 28));
        batchPanel.add(spnBatchCount);
        controlPanel.add(batchPanel);
        controlPanel.add(Box.createVerticalStrut(8));

        // Entropy harvester section
        JPanel entropyPanel = new JPanel();
        entropyPanel.setLayout(new BoxLayout(entropyPanel, BoxLayout.Y_AXIS));
        entropyPanel.setBorder(BorderFactory.createTitledBorder("Advanced Entropy"));
        entropyPanel.add(chkEntropy);
        lblEntropyStatus.setFont(lblEntropyStatus.getFont().deriveFont(Font.ITALIC, 11f));
        entropyPanel.add(lblEntropyStatus);
        controlPanel.add(entropyPanel);
        controlPanel.add(Box.createVerticalGlue());

        // ── RIGHT: Output panel ──────────────────────────────────
        JPanel outputPanel = new JPanel(new BorderLayout(8, 8));

        // Strength meter at the top
        outputPanel.add(strengthMeter, BorderLayout.NORTH);

        // Scrollable output area
        txtOutput.setEditable(false);
        txtOutput.setLineWrap(true);
        txtOutput.setWrapStyleWord(true);
        txtOutput.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        txtOutput.setMargin(new Insets(8, 8, 8, 8));
        JScrollPane scrollPane = new JScrollPane(txtOutput);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Generated Passwords"));
        outputPanel.add(scrollPane, BorderLayout.CENTER);

        // Button bar at the bottom
        JPanel buttonBar = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 4));
        btnGenerate.setFont(btnGenerate.getFont().deriveFont(Font.BOLD, 14f));
        btnGenerate.setPreferredSize(new Dimension(160, 36));
        buttonBar.add(btnGenerate);
        buttonBar.add(btnCopy);
        buttonBar.add(btnClear);
        outputPanel.add(buttonBar, BorderLayout.SOUTH);

        // ── Assemble root ────────────────────────────────────────
        root.add(controlPanel, BorderLayout.WEST);
        root.add(outputPanel, BorderLayout.CENTER);
        setContentPane(root);
    }

    private void initMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // File menu
        JMenu fileMenu = new JMenu("File");
        JMenuItem saveItem = new JMenuItem("Save to file…");
        saveItem.addActionListener(e -> saveToFile());
        fileMenu.add(saveItem);
        fileMenu.addSeparator();
        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> dispose());
        fileMenu.add(exitItem);
        menuBar.add(fileMenu);

        // Edit menu
        JMenu editMenu = new JMenu("Edit");
        JMenuItem copyItem = new JMenuItem("Copy All");
        copyItem.addActionListener(e -> copyToClipboard());
        editMenu.add(copyItem);
        JMenuItem clearItem = new JMenuItem("Clear");
        clearItem.addActionListener(e -> clearOutput());
        editMenu.add(clearItem);
        menuBar.add(editMenu);

        // Help menu
        JMenu helpMenu = new JMenu("Help");
        JMenuItem aboutItem = new JMenuItem("About");
        aboutItem.addActionListener(e -> showAbout());
        helpMenu.add(aboutItem);
        menuBar.add(helpMenu);

        setJMenuBar(menuBar);
    }

    // ═══════════════════════════════════════════════════════════════
    //  EVENT LISTENERS
    // ═══════════════════════════════════════════════════════════════

    private void initListeners() {
        btnGenerate.addActionListener(e -> generatePasswords());
        btnCopy.addActionListener(e -> copyToClipboard());
        btnClear.addActionListener(e -> clearOutput());

        // Delimiter checkbox toggles the detail fields
        chkDelimiter.addActionListener(e -> {
            boolean on = chkDelimiter.isSelected();
            spnBlockSize.setEnabled(on);
            txtDelimiter.setEnabled(on);
        });

        // Entropy harvester checkbox
        chkEntropy.addActionListener(e -> {
            boolean on = chkEntropy.isSelected();
            harvester.setActive(on);
            if (!on) {
                lblEntropyStatus.setText("Entropy: inactive");
            } else {
                lblEntropyStatus.setText("Entropy: move your mouse!");
            }
        });

        // Update strength meter whenever settings change
        Runnable updater = this::updateStrengthMeter;
        chkLowercase.addActionListener(e -> updater.run());
        chkUppercase.addActionListener(e -> updater.run());
        chkDigits.addActionListener(e -> updater.run());
        chkSymbols.addActionListener(e -> updater.run());
        chkExcludeAmb.addActionListener(e -> updater.run());
        spnLength.addChangeListener(e -> updater.run());

        // Double-click a password line in the output to copy just that line
        txtOutput.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    copySelectedLine();
                }
            }
        });
    }

    // ═══════════════════════════════════════════════════════════════
    //  ACTIONS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Reads all control values and generates password(s).
     */
    private void generatePasswords() {
        int length    = (int) spnLength.getValue();
        int count     = (int) spnBatchCount.getValue();
        boolean lower = chkLowercase.isSelected();
        boolean upper = chkUppercase.isSelected();
        boolean digits = chkDigits.isSelected();
        boolean syms  = chkSymbols.isSelected();
        boolean excl  = chkExcludeAmb.isSelected();
        boolean delim = chkDelimiter.isSelected();
        int block     = (int) spnBlockSize.getValue();
        String delimStr = txtDelimiter.getText();

        if (!lower && !upper && !digits && !syms) {
            JOptionPane.showMessageDialog(this,
                    "Please select at least one character set.",
                    "No Characters Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }

        List<String> passwords = engine.generateBatch(
                count, length, lower, upper, digits, syms, excl, delim, block, delimStr);

        // Display results
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < passwords.size(); i++) {
            if (count > 1) {
                sb.append(String.format("%3d │ %s%n", i + 1, passwords.get(i)));
            } else {
                sb.append(passwords.get(i)).append("\n");
            }
        }
        txtOutput.setText(sb.toString());
        txtOutput.setCaretPosition(0);

        // Update strength meter
        updateStrengthMeter();
    }

    /**
     * Updates the strength meter based on current configuration.
     */
    private void updateStrengthMeter() {
        int length    = (int) spnLength.getValue();
        boolean lower = chkLowercase.isSelected();
        boolean upper = chkUppercase.isSelected();
        boolean digits = chkDigits.isSelected();
        boolean syms  = chkSymbols.isSelected();
        boolean excl  = chkExcludeAmb.isSelected();

        PasswordEngine.EntropyResult result = engine.estimateEntropy(
                length, lower, upper, digits, syms, excl);
        strengthMeter.update(result);
    }

    /**
     * Copies all output text to the system clipboard.
     */
    private void copyToClipboard() {
        String text = txtOutput.getText().trim();
        if (!text.isEmpty()) {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(new StringSelection(text), null);
            // Brief visual feedback
            String originalTitle = getTitle();
            setTitle("✓ Copied to clipboard!");
            Timer timer = new Timer(1500, e -> setTitle(originalTitle));
            timer.setRepeats(false);
            timer.start();
        }
    }

    /**
     * Copies the line at the current caret position.
     */
    private void copySelectedLine() {
        try {
            int caretPos = txtOutput.getCaretPosition();
            int lineNum  = txtOutput.getLineOfOffset(caretPos);
            int start    = txtOutput.getLineStartOffset(lineNum);
            int end      = txtOutput.getLineEndOffset(lineNum);
            String line  = txtOutput.getText(start, end - start).trim();
            // Strip the line number prefix if present (e.g. "  1 │ ")
            if (line.contains("│")) {
                line = line.substring(line.indexOf("│") + 1).trim();
            }
            if (!line.isEmpty()) {
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                clipboard.setContents(new StringSelection(line), null);
            }
        } catch (Exception ignored) {
            // Swallow offset exceptions
        }
    }

    /**
     * Clears the output text area and resets the strength meter.
     */
    private void clearOutput() {
        txtOutput.setText("");
        strengthMeter.reset();
    }

    /**
     * Shows a file-save dialog and writes output to the chosen file.
     */
    private void saveToFile() {
        JFileChooser chooser = new JFileChooser();
        int result = chooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            try (FileWriter writer = new FileWriter(chooser.getSelectedFile())) {
                writer.write(txtOutput.getText());
                JOptionPane.showMessageDialog(this,
                        "Passwords saved successfully.",
                        "Saved", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this,
                        "Error saving file: " + ex.getMessage(),
                        "Save Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Shows the about dialog.
     */
    private void showAbout() {
        JOptionPane.showMessageDialog(this,
                "PassGen 2.0 — Secure Password Generator\n\n" +
                        "Author: Bill Chamalidis\n" +
                        "University of Macedonia\n" +
                        "Applied Informatics — Information Systems\n\n" +
                        "• SecureRandom + optional mouse entropy\n" +
                        "• Visual strength meter with crack-time estimates\n" +
                        "• Batch generation, delimiter chunking\n" +
                        "• Built with FlatLaf One-Dark theme\n\n" +
                        "GitHub: https://github.com/bill-chamal",
                "About PassGen", JOptionPane.INFORMATION_MESSAGE);
    }
}
