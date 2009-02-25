package org.esa.beam.visat.toolviews.layermanager.layersrc;


import org.esa.beam.framework.ui.assistant.AbstractAppAssistantPage;
import org.esa.beam.framework.ui.assistant.AppAssistantPageContext;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

public class OpenImageFileAssistantPage extends AbstractAppAssistantPage {

    private JTextField imageFileField;
    private JTextField worldFileField;

    public OpenImageFileAssistantPage() {
        super("Select Image File");
    }

    @Override
    public boolean validatePage() {
        String imageFilePath = imageFileField.getText();
        String worldFilePath = worldFileField.getText();
        // todo - Check, if not GeoTIFF, a worldFile is required, maybe lookup automatically (e.g. <name>.png, <name>.pgw)
        // Rule <name>.<a><b><c> --> <name>.<a><c>w
        // see http://en.wikipedia.org/wiki/World_file
        return imageFilePath != null && new File(imageFilePath).exists()
               && (worldFilePath == null || new File(worldFilePath).exists());
    }

    @Override
    public boolean hasNextPage() {
        return false;
    }

    @Override
    public boolean canFinish() {
        return true;
    }

    @Override
    public boolean performFinish() {
        String imageFilePath = imageFileField.getText();
        String worldFilePath = worldFileField.getText();
        System.out.println("Selected imageFilePath = " + imageFilePath);
        System.out.println("Selected worldFilePath = " + worldFilePath);
        return true;
    }

    protected Component createLayerPageComponent(AppAssistantPageContext context) {
        GridBagConstraints gbc = new GridBagConstraints();
        final JPanel panel = new JPanel(new GridBagLayout());

        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridy = 0;

        this.imageFileField = addRow(panel, "Path to image file (.png, .jpg, .tif):", gbc);
        this.worldFileField = addRow(panel, "Path to world file (.pgw, .jgw, .tfw):", gbc);

        return panel;
    }

    private JTextField addRow(JPanel panel, String label, GridBagConstraints gbc) {
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridwidth = 2;
        panel.add(new JLabel(label), gbc);

        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridwidth = 1;
        final JTextField fileField = new JTextField();
        panel.add(fileField, gbc);
        fileField.getDocument().addDocumentListener(new MyDocumentListener());

        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridwidth = 1;
        JButton button = new JButton("...");
        button.addActionListener(new MyActionListener());
        panel.add(button, gbc);

        return fileField;
    }

    private class MyDocumentListener implements DocumentListener {

        public void insertUpdate(DocumentEvent e) {
            getPageContext().updateState();
        }

        public void removeUpdate(DocumentEvent e) {
            getPageContext().updateState();
        }

        public void changedUpdate(DocumentEvent e) {
            getPageContext().updateState();
        }
    }

    private class MyActionListener implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.showOpenDialog(getPageContext().getWindow());
            if (fileChooser.getSelectedFile() != null) {
                imageFileField.setText(fileChooser.getSelectedFile().getPath());
                getPageContext().updateState();
            }
        }
    }
}