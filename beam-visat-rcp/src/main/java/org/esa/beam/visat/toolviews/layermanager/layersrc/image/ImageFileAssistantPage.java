package org.esa.beam.visat.toolviews.layermanager.layersrc.image;


import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.ceres.glayer.tools.Tools;
import org.esa.beam.framework.ui.assistant.AbstractAppAssistantPage;
import org.esa.beam.framework.ui.assistant.AppAssistantPageContext;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.util.PropertyMap;
import org.esa.beam.util.io.FileUtils;
import org.esa.beam.visat.toolviews.layermanager.layersrc.HistoryComboBoxModel;

import javax.media.jai.Interpolation;
import javax.media.jai.PlanarImage;
import javax.media.jai.operator.FileLoadDescriptor;
import javax.media.jai.operator.ScaleDescriptor;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.util.concurrent.ExecutionException;

// todo - Check, if image is GeoTIFF -> no world file is needed

public class ImageFileAssistantPage extends AbstractAppAssistantPage {

    private static final String PROPERTY_LAST_IMAGE_PREFIX = "ImageFileAssistantPage.ImageFile.history";
    private JComboBox imageFileBox;
    private JTextField worldFileField;
    private HistoryComboBoxModel imageHistoryModel;
    private JTextField layerNameField;
    private JLabel imagePreviewLabel;
    private RenderedImage image;


    public ImageFileAssistantPage() {
        super("Select Image File");
    }

    @Override
    public boolean validatePage() {
        String imageFilePath = getText(imageFileBox);
        String worldFilePath = getText(worldFileField);
        String layerName = getText(layerNameField);
        return new File(imageFilePath).exists() &&
               !layerName.isEmpty() &&
               (worldFilePath.isEmpty() || new File(worldFilePath).exists());
    }

    private String getText(JComboBox comboBox) {
        final String text = (String) comboBox.getSelectedItem();
        return text == null ? "" : text.trim();
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
        imageHistoryModel.saveHistory();
        return new ImageFileAssistantPage2(image, getText(layerNameField), createTransform(getText(worldFileField)));
    }

    @Override
    public boolean performFinish() {
        imageHistoryModel.saveHistory();
        String worldFilePath = getText(worldFileField);
        String layerName = layerNameField.getText().trim();
        AffineTransform transform = createTransform(worldFilePath);
        return ImageFileAssistantPage.insertImageLayer(getAppPageContext(), image, layerName, transform);
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

        final PropertyMap preferences = context.getAppContext().getPreferences();
        imageHistoryModel = new HistoryComboBoxModel(preferences, PROPERTY_LAST_IMAGE_PREFIX, 5);
        imageFileBox = new JComboBox(imageHistoryModel);
        imageFileBox.addActionListener(new ImageFileItemListener());
        final JLabel imageFileLabel = new JLabel("Path to image file (.png, .jpg, .tif, .gif):");
        JButton imageFileButton = new JButton("...");
        final FileNameExtensionFilter imageFileFilter = new FileNameExtensionFilter("Image Files", "png", "jpg", "tif",
                                                                                    "gif");
        imageFileButton.addActionListener(new FileChooserActionListener(imageFileFilter));
        addRow(panel, gbc, imageFileLabel, imageFileBox, imageFileButton);

        worldFileField = new JTextField();
        worldFileField.getDocument().addDocumentListener(new MyDocumentListener());
        final JLabel worldFileLabel = new JLabel("Path to world file (.pgw, .jgw, .tfw, .gfw):");
        JButton worldFileButton = new JButton("...");
        final FileNameExtensionFilter worldFileFilter = new FileNameExtensionFilter("World Files", "pgw", "jgw", "tfw",
                                                                                    "gfw");
        worldFileButton.addActionListener(new FileChooserActionListener(worldFileFilter));
        addRow(panel, gbc, worldFileLabel, worldFileField, worldFileButton);

        layerNameField = new JTextField();
        addRow(panel, gbc, new JLabel("Layer Name:"), layerNameField, null);


        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(new JLabel("Preview:"), gbc);

        gbc.insets = new Insets(0, 4, 0, 4);
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.gridy++;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridwidth = 1;
        imagePreviewLabel = new JLabel();
        imagePreviewLabel.setSize(new Dimension(200, 200));
        imagePreviewLabel.setText("No preview available!");
        panel.add(imagePreviewLabel, gbc);

        return panel;
    }

    private void addRow(JPanel panel, GridBagConstraints gbc, JLabel label, JComponent component, JButton button) {
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridwidth = 2;
        panel.add(label, gbc);

        gbc.insets = new Insets(0, 4, 0, 4);
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridwidth = 1;
        panel.add(component, gbc);

        if (button != null) {
            gbc.weightx = 0;
            gbc.weighty = 0;
            gbc.gridx = 1;
            gbc.fill = GridBagConstraints.NONE;
            gbc.gridwidth = 1;
            panel.add(button, gbc);
        }
    }

    static boolean insertImageLayer(AppAssistantPageContext pageContext, RenderedImage image, String layerName,
                                    AffineTransform transform) {
        try {
            ImageLayer imageLayer = new ImageLayer(image, transform);
            imageLayer.setName(layerName);
            ProductSceneView sceneView = pageContext.getAppContext().getSelectedProductSceneView();
            Layer rootLayer = sceneView.getRootLayer();
            rootLayer.getChildren().add(sceneView.getFirstImageLayerIndex(), imageLayer);
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

    private class FileChooserActionListener implements ActionListener {

        private FileFilter filter;

        private FileChooserActionListener(FileFilter fileFilter) {
            filter = fileFilter;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.addChoosableFileFilter(filter);
            fileChooser.showOpenDialog(getPageContext().getWindow());
            if (fileChooser.getSelectedFile() != null) {
                String filePath = fileChooser.getSelectedFile().getPath();
                imageHistoryModel.setSelectedItem(filePath);
                getPageContext().updateState();
            }
        }
    }

    private class ImageFileItemListener implements ActionListener {


        @Override
        public void actionPerformed(ActionEvent e) {
            String imageFilePath = (String) imageFileBox.getSelectedItem();
            if (imageFilePath == null) {
                return;
            }

            image = FileLoadDescriptor.create(imageFilePath, null, true, null);

            ImagePreviewWorker worker = new ImagePreviewWorker(image, imagePreviewLabel);
            worker.execute();


            String worldFilePath = createWorldFilePath(imageFilePath);

            if (new File(worldFilePath).isFile()) {
                worldFileField.setText(worldFilePath);
            } else {
                worldFileField.setText(null);
            }

            layerNameField.setText(FileUtils.getFileNameFromPath(imageFilePath));
            getPageContext().updateState();
        }

        private String createWorldFilePath(String imageFilePath) {
            String imageFileExt = FileUtils.getExtension(imageFilePath);
            // Rule for world file extension: <name>.<a><b><c> --> <name>.<a><c>w
            // see http://support.esri.com/index.cfm?fa=knowledgebase.techarticles.articleShow&d=17489
            String worldFilePath;
            if (imageFileExt != null && imageFileExt.length() == 4) { // three chars + leading dot
                String worldFileExt = imageFileExt.substring(0, 2) + imageFileExt.charAt(
                        imageFileExt.length() - 1) + "w";
                worldFilePath = FileUtils.exchangeExtension(imageFilePath, worldFileExt);
            } else {
                worldFilePath = imageFilePath + "w";
            }
            return worldFilePath;
        }

        private class ImagePreviewWorker extends SwingWorker<Image, Object> {

            private final RenderedImage sourceImage;
            private final Dimension targetDimension;
            private final JLabel imageLabel;

            private ImagePreviewWorker(RenderedImage sourceImage, JLabel imageLabel) {
                this.sourceImage = sourceImage;
                this.imageLabel = imageLabel;
                this.targetDimension = this.imageLabel.getSize();
            }

            @Override
            protected Image doInBackground() throws Exception {
                int width = sourceImage.getWidth();
                int height = sourceImage.getHeight();

                float scale = (float) (targetDimension.getWidth() / width);
                scale = (float) Math.min(scale, targetDimension.getHeight() / height);
                if (scale > 1) {
                    scale = 1.0f;
                }

                RenderedImage scaledImage = ScaleDescriptor.create(sourceImage, scale, scale, 0.0f, 0.0f,
                                                                   Interpolation.getInstance(
                                                                           Interpolation.INTERP_NEAREST), null);
                PlanarImage planarImage = PlanarImage.wrapRenderedImage(scaledImage);
                BufferedImage bufferedImage = planarImage.getAsBufferedImage();
                planarImage.dispose();
                return bufferedImage;
            }


            @Override
            protected void done() {
                try {
                    imageLabel.setIcon(new ImageIcon(get()));
                    imageLabel.setText(null);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    imageLabel.setText("Could not create preview");
                } catch (ExecutionException e) {
                    e.printStackTrace();
                    imageLabel.setText("Could not create preview");
                }
            }
        }
    }

}