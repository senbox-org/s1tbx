package org.esa.beam.visat.toolviews.layermanager.layersrc.image;


import org.esa.beam.framework.ui.assistant.AbstractAppAssistantPage;
import org.esa.beam.framework.ui.assistant.AppAssistantPageContext;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.geom.AffineTransform;

public class ImageFileAssistantPage2 extends AbstractAppAssistantPage {

    private JTextField[] numberFields;
    private final String imageFilePath;
    private final AffineTransform transform;

    public ImageFileAssistantPage2(String imageFilePath, AffineTransform transform) {
        super("Edit Affine Transformation");
        this.imageFilePath = imageFilePath;
        this.transform = transform;
    }

    @Override
    public boolean validatePage() {
        try {
            return createTransform().getDeterminant() != 0.0;
        } catch (Exception ignore) {
            return false;
        }
    }

    private AffineTransform createTransform() {
        double[] flatmatrix = new double[numberFields.length];
        for (int i = 0; i < flatmatrix.length; i++) {
            flatmatrix[i] = Double.parseDouble(getText(numberFields[i]));
        }
        return new AffineTransform(flatmatrix);
    }

    private String getText(JTextField field) {
        String s = field.getText();
        return s != null ? s.trim() : "";
    }

    @Override
    public boolean performFinish() {
        return ImageFileAssistantPage.insertImage(getAppPageContext(), imageFilePath, createTransform());
    }

    @Override
    protected Component createLayerPageComponent(AppAssistantPageContext context) {
        GridBagConstraints gbc = new GridBagConstraints();
        final JPanel panel = new JPanel(new GridBagLayout());

        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridy = 0;
        gbc.weighty = 0;


        double[] flatmatrix = new double[6];
        numberFields = new JTextField[flatmatrix.length];
        transform.getMatrix(flatmatrix);

        String[] labels = new String[] {
                "X scale in resulting X direction: ",
                "Y scale in resulting X direction: ",
                "X scale in resulting Y direction: ",
                "Y scale in resulting Y direction: ",
                "X coordinate of the center of rotation: ",
                "Y coordinate of the center of rotation: "
        };
        for (int i = 0; i < labels.length; i++) {
            numberFields[i] = addRow(panel, labels[i], gbc);
            numberFields[i].setText(String.valueOf(flatmatrix[i]));
        }

        return panel;
    }

    private JTextField addRow(JPanel panel, String label, GridBagConstraints gbc) {
        gbc.gridy++;

        gbc.weightx = 0.2;
        gbc.gridx = 0;
        panel.add(new JLabel(label), gbc);

        gbc.weightx = 0.8;
        gbc.gridx = 1;
        final JTextField fileField = new JTextField(12);
        fileField.setHorizontalAlignment(JTextField.RIGHT);
        panel.add(fileField, gbc);
        fileField.getDocument().addDocumentListener(new MyDocumentListener());

        return fileField;
    }


    private class MyDocumentListener implements DocumentListener {

        @Override
        public void insertUpdate(DocumentEvent e) {
            getPageContext().updateState();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            getPageContext().updateState();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            getPageContext().updateState();
        }
    }
}