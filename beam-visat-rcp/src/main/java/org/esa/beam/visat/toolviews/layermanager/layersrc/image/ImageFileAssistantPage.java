package org.esa.beam.visat.toolviews.layermanager.layersrc.image;


import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.ceres.glayer.tools.Tools;
import org.esa.beam.framework.ui.assistant.AbstractAppAssistantPage;
import org.esa.beam.framework.ui.assistant.AppAssistantPageContext;

import javax.media.jai.operator.FileLoadDescriptor;
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
import java.awt.image.RenderedImage;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.io.File;

public class ImageFileAssistantPage extends AbstractAppAssistantPage {

    private JTextField imageFileField;
    private JTextField worldFileField;

    public ImageFileAssistantPage() {
        super("Select Image File");
    }

    @Override
    public boolean validatePage() {
        String imageFilePath = getText(imageFileField);
        String worldFilePath = getText(worldFileField);
        // todo - Check, if not GeoTIFF, a worldFile is required, maybe lookup automatically (e.g. <name>.png, <name>.pgw)
        // Rule <name>.<a><b><c> --> <name>.<a><c>w
        // see http://en.wikipedia.org/wiki/World_file
        return new File(imageFilePath).exists() && (worldFilePath.isEmpty() || new File(worldFilePath).exists());
    }

    private String getText(JTextField field) {
        String s = field.getText();
        return s != null ? s.trim() : "";
    }

    @Override
    public boolean hasNextPage() {
        return true;
    }

    @Override
    public AbstractAppAssistantPage getNextPage(AppAssistantPageContext pageContext) {
        return new ImageFileAssistantPage2(getText(imageFileField), createTransform(getText(worldFileField)));
    }

    @Override
    public boolean performFinish() {
        String imageFilePath = getText(imageFileField);
        String worldFilePath = getText(worldFileField);
        return ImageFileAssistantPage.insertImage(getAppPageContext(),
                                                  imageFilePath,
                                                  createTransform(worldFilePath));
    }

    private AffineTransform createTransform(String worldFilePath) {
        return !worldFilePath.isEmpty() ? Tools.loadWorldFile(worldFilePath) : new AffineTransform();
    }

    @Override
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

    static boolean insertImage(AppAssistantPageContext pageContext, String imageFilePath, AffineTransform transform) {
        try {
            RenderedImage image = FileLoadDescriptor.create(imageFilePath, null, true, null);
            ImageLayer imageLayer = new ImageLayer(image, transform);
            imageLayer.setName(new File(imageFilePath).getName());
            Layer rootLayer = pageContext.getAppContext().getSelectedProductSceneView().getRootLayer();
            rootLayer.getChildren().add(0, imageLayer);
            return true;
        } catch (Exception e) {
            pageContext.showErrorDialog(e.getMessage());
            return false;
        }
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

    private class MyActionListener implements ActionListener {

        @Override
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