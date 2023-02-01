import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileWriter;
import java.io.IOException;

public class MainWindow extends JFrame {
    private JTextArea txtBoxPasswd;
    private JScrollPane scrollBar;
    private JTextField txtBoxLength;
    private JButton btnGen, btnCopy;
    private JCheckBox ckbxUpper, ckbxNum, ckbxSymbols, ckbxLower;
    private JLabel lblLength;
    private JPanel mainPanel, passgenPanel, configPanel, lblPanel, btnPanel;
    private JMenuBar menuBar;
    private JMenu menuFile, menuEdit, menuHelp;
    private JMenuItem menuItemSave,menuItemAbout, menuItemExit, menuItemCopy, menuItemClear;
    private JFileChooser fileDialog;
    public MainWindow(){
        // Initialize (Create Objects)
        txtBoxPasswd = new JTextArea("====================\n***Password Generator***\n====================");
        scrollBar = new JScrollPane(txtBoxPasswd);
        btnGen = new JButton("Generate");
        btnCopy = new JButton("Copy");
        ckbxNum = new JCheckBox("Numbers");
        ckbxSymbols = new JCheckBox("Symbols");
        ckbxUpper = new JCheckBox("Uppercase");
        ckbxLower = new JCheckBox("Lowercase");
        txtBoxLength = new JTextField("0012");
        lblLength = new JLabel("Password length:");
        menuBar = new JMenuBar();
        menuFile = new JMenu("File");
        menuEdit = new JMenu("Edit");
        menuHelp = new JMenu("Help");
        menuItemSave = new JMenuItem("Save");
        menuItemAbout = new JMenuItem("About");
        menuItemExit = new JMenuItem("Exit");
        menuItemClear = new JMenuItem("Clear All Text");
        menuItemCopy = new JMenuItem("Copy All Text");
        fileDialog = new JFileChooser();

        // -> Panels
        mainPanel = new JPanel(new BorderLayout());
        passgenPanel = new JPanel(new BorderLayout());
        configPanel = new JPanel();
        lblPanel = new JPanel();
        btnPanel = new JPanel(new GridLayout());

        // Set Menu
        menuFile.add(menuItemSave); menuFile.addSeparator(); menuFile.add(menuItemExit);
        menuEdit.add(menuItemCopy); menuEdit.add(menuItemClear);
        menuHelp.add(menuItemAbout);
        menuBar.add(menuFile); menuBar.add(menuEdit); menuBar.add(menuHelp);

        // Configure some IU
        txtBoxPasswd.setLineWrap(true);
        ckbxLower.setSelected(true);

        // Set ActionListener
        ActListener actListener = new ActListener();
        btnGen.addActionListener(actListener);
        btnCopy.addActionListener(actListener);
        menuItemSave.addActionListener(actListener);
        menuItemExit.addActionListener(actListener);
        menuItemAbout.addActionListener(actListener);
        menuItemCopy.addActionListener(actListener);
        menuItemClear.addActionListener(actListener);

        // Add UI to Panels
        passgenPanel.add(scrollBar, BorderLayout.CENTER);
        btnPanel.add(btnGen);
        btnPanel.add(btnCopy);
        passgenPanel.add(btnPanel, BorderLayout.SOUTH);
        configPanel.add(ckbxNum);
        configPanel.add(ckbxLower);
        configPanel.add(ckbxUpper);
        configPanel.add(ckbxSymbols);
        lblPanel.add(lblLength);
        lblPanel.add(txtBoxLength);
        configPanel.add(lblPanel);
        mainPanel.add(passgenPanel, BorderLayout.CENTER);
        mainPanel.add(configPanel, BorderLayout.SOUTH);

        // Configure the JFrame window
        this.setContentPane(mainPanel);
        this.setJMenuBar(menuBar);
        this.setTitle("Password Generator");
        this.setLocationRelativeTo(null);
        this.setSize(510,200);
        this.setVisible(true);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    class ActListener implements ActionListener{

        @Override
        public void actionPerformed(ActionEvent e) {
            if(e.getSource() == btnGen){
                int bounds = Integer.parseInt(txtBoxLength.getText());
                PassGenerator passGenerator = new PassGenerator(bounds, ckbxLower.isSelected(), ckbxUpper.isSelected(), ckbxNum.isSelected(),
                        ckbxSymbols.isSelected());
                txtBoxPasswd.setText(passGenerator.GetPassword());
            }
            else if(e.getSource() == btnCopy){
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                StringSelection stringSelection = new StringSelection(txtBoxPasswd.getText());
                clipboard.setContents(stringSelection, null);
            }
            else if(e.getSource() == menuItemAbout){
                JOptionPane.showMessageDialog(null,"Author: Bill Chamalidis \nUniversity of Macedonia\nApplied Informatics - Information Systems\n~Created: 1 February 2023\n~Version: 0.0.1\n\nGithub: https://github.com/bill-chamal");
            }
            else if(e.getSource() == menuItemCopy)
                btnCopy.doClick();
            else if (e.getSource() == menuItemExit)
                dispose();
            else if (e.getSource() == menuItemClear)
                txtBoxPasswd.setText("");
            else if (e.getSource() == menuItemSave) {
                int option = fileDialog.showSaveDialog(null);
                if(option == JFileChooser.OPEN_DIALOG){
                    try {
                        FileWriter writer = new FileWriter(fileDialog.getSelectedFile(), true);
                        writer.write(txtBoxPasswd.getText());
                        writer.close();
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }

        }
    }
}
