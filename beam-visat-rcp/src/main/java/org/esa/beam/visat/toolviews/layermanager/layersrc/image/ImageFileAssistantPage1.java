package org.esa.beam.visat.toolviews.layermanager.layersrc.image;


import com.bc.ceres.glayer.tools.Tools;
import org.esa.beam.framework.ui.FileHistory;
import org.esa.beam.util.PropertyMap;
import org.esa.beam.util.io.FileUtils;
import org.esa.beam.visat.toolviews.layermanager.layersrc.AbstractLayerSourceAssistantPage;
import org.esa.beam.visat.toolviews.layermanager.layersrc.FilePathListCellRenderer;
import org.esa.beam.visat.toolviews.layermanager.layersrc.HistoryComboBoxModel;
import org.esa.beam.visat.toolviews.layermanager.layersrc.LayerSourcePageContext;

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
import java.io.IOException;
import java.util.concurrent.ExecutionException;

// todo - Check, if image is GeoTIFF -> no world file is needed

class ImageFileAssistantPage1 extends AbstractLayerSourceAssistantPage {

    private static final String LAST_IMAGE_PREFIX = "ImageFileAssistantPage1.ImageFile.history";
    private static final String LAST_DIR = "ImageFileAssistantPage1.ImageFile.lastDir";
    private JComboBox imageFileBox;
    private JTextField worldFileField;
    private HistoryComboBoxModel imageHistoryModel;
    private JLabel imagePreviewLabel;


    ImageFileAssistantPage1() {
        super("Select Image File");
    }

    @Override
    public boolean validatePage() {
        String imageFilePath = (String) getContext().getPropertyValue(ImageFileLayerSource.PROPERTY_NAME_IMAGE_FILE_PATH);
        String worldFilePath = (String) getContext().getPropertyValue(ImageFileLayerSource.PROPERTY_NAME_WORLD_FILE_PATH);
        if (imageFilePath == null) {
            return false;
        }
        return new File(imageFilePath).exists() && (worldFilePath == null || new File(worldFilePath).exists());
    }

    @Override
    public boolean hasNextPage() {
        return getContext().getPropertyValue(ImageFileLayerSource.PROPERTY_NAME_IMAGE) != null;
    }

    @Override
    public AbstractLayerSourceAssistantPage getNextPage() {
        imageHistoryModel.getHistory().copyInto(getContext().getAppContext().getPreferences());
        createTransform(getContext());
        return new ImageFileAssistantPage2();
    }

    @Override
    public boolean canFinish() {
        return getContext().getPropertyValue(ImageFileLayerSource.PROPERTY_NAME_IMAGE) != null;
    }

    @Override
    public boolean performFinish() {
        imageHistoryModel.getHistory().copyInto(getContext().getAppContext().getPreferences());
        createTransform(getContext());
        return ImageFileLayerSource.insertImageLayer(getContext());
    }

    @Override
    public Component createPageComponent() {
        GridBagConstraints gbc = new GridBagConstraints();
        final JPanel panel = new JPanel(new GridBagLayout());

        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridy = 0;

        final PropertyMap preferences = getContext().getAppContext().getPreferences();
        FileHistory fileHistory = new FileHistory(5, LAST_IMAGE_PREFIX);
        fileHistory.initBy(preferences);
        imageHistoryModel = new HistoryComboBoxModel(fileHistory);
        imageFileBox = new JComboBox(imageHistoryModel);
        imageFileBox.addActionListener(new ImageFileItemListener());
        imageFileBox.setRenderer(new FilePathListCellRenderer(80));
        final JLabel imageFileLabel = new JLabel("Path to image file (.png, .jpg, .tif, .gif):");
        JButton imageFileButton = new JButton("...");
        final FileNameExtensionFilter imageFileFilter = new FileNameExtensionFilter("Image Files",
                                                                                    "png", "jpg", "tif", "gif");
        imageFileButton.addActionListener(new FileChooserActionListener(imageFileFilter) {
            @Override
            protected void onFileSelected(LayerSourcePageContext pageContext, String filePath) {
                pageContext.setPropertyValue(ImageFileLayerSource.PROPERTY_NAME_IMAGE_FILE_PATH, filePath);
            }
        });
        addRow(panel, gbc, imageFileLabel, imageFileBox, imageFileButton);

        worldFileField = new JTextField();
        worldFileField.getDocument().addDocumentListener(new WorldFilePathDocumentListener());
        final JLabel worldFileLabel = new JLabel("Path to world file (.pgw, .jgw, .tfw, .gfw):");
        JButton worldFileButton = new JButton("...");
        final FileNameExtensionFilter worldFileFilter = new FileNameExtensionFilter("World Files",
                                                                                    "pgw", "jgw", "tfw", "gfw");
        worldFileButton.addActionListener(new FileChooserActionListener(worldFileFilter) {
            @Override
            protected void onFileSelected(LayerSourcePageContext pageContext, String filePath) {
                pageContext.setPropertyValue(ImageFileLayerSource.PROPERTY_NAME_WORLD_FILE_PATH, filePath);
            }

        });
        addRow(panel, gbc, worldFileLabel, worldFileField, worldFileButton);


        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridwidth = 1;
        imagePreviewLabel = new JLabel();
        imagePreviewLabel.setPreferredSize(new Dimension(200, 200));
        panel.add(imagePreviewLabel, gbc);
        getContext().setPropertyValue(ImageFileLayerSource.PROPERTY_NAME_IMAGE_FILE_PATH, null);
        getContext().setPropertyValue(ImageFileLayerSource.PROPERTY_NAME_WORLD_FILE_PATH, null);
        getContext().setPropertyValue(ImageFileLayerSource.PROPERTY_NAME_IMAGE, null);
        getContext().setPropertyValue(ImageFileLayerSource.PROPERTY_NAME_WORLD_TRANSFORM, null);

        return panel;
    }

    private void addRow(JPanel panel, GridBagConstraints gbc, JLabel label, JComponent component, JButton button) {
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridwidth = 1;
        panel.add(label, gbc);

        gbc.insets = new Insets(0, 4, 0, 4);
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridwidth = 1;
        panel.add(component, gbc);

        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridwidth = 1;
        panel.add(button, gbc);
    }

    private static void createTransform(LayerSourcePageContext pageContext) {
        AffineTransform transform = new AffineTransform();
        String worldFilePath = (String) pageContext.getPropertyValue(ImageFileLayerSource.PROPERTY_NAME_WORLD_FILE_PATH);
        if (worldFilePath != null && !worldFilePath.isEmpty()) {
            try {
                transform = Tools.loadWorldFile(worldFilePath);
            } catch (IOException e) {
                e.printStackTrace();
                pageContext.showErrorDialog(e.getMessage());
            }
        }
        pageContext.setPropertyValue(ImageFileLayerSource.PROPERTY_NAME_WORLD_TRANSFORM, transform);
    }

    private class WorldFilePathDocumentListener implements DocumentListener {

        @Override
        public void insertUpdate(DocumentEvent e) {
            getContext().setPropertyValue(ImageFileLayerSource.PROPERTY_NAME_WORLD_FILE_PATH, worldFileField.getText());
            getContext().updateState();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            getContext().setPropertyValue(ImageFileLayerSource.PROPERTY_NAME_WORLD_FILE_PATH, worldFileField.getText());
            getContext().updateState();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            getContext().setPropertyValue(ImageFileLayerSource.PROPERTY_NAME_WORLD_FILE_PATH, worldFileField.getText());
            getContext().updateState();
        }
    }

    private abstract class FileChooserActionListener implements ActionListener {

        private final FileFilter filter;

        private FileChooserActionListener(FileFilter fileFilter) {
            filter = fileFilter;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.addChoosableFileFilter(filter);
            fileChooser.setCurrentDirectory(getLastDirectory());

            LayerSourcePageContext pageContext = getContext();
            fileChooser.showOpenDialog(pageContext.getWindow());
            if (fileChooser.getSelectedFile() != null) {
                String filePath = fileChooser.getSelectedFile().getPath();
                imageHistoryModel.setSelectedItem(filePath);
                PropertyMap preferences = pageContext.getAppContext().getPreferences();
                preferences.setPropertyString(LAST_DIR, fileChooser.getCurrentDirectory().getAbsolutePath());
                onFileSelected(pageContext, filePath);
                pageContext.updateState();
            }
        }

        protected abstract void onFileSelected(LayerSourcePageContext pageContext, String filePath);

        private File getLastDirectory() {
            PropertyMap preferences = getContext().getAppContext().getPreferences();
            String dirPath = preferences.getPropertyString(LAST_DIR, System.getProperty("user.home"));
            File lastDir = new File(dirPath);
            if (!lastDir.isDirectory()) {
                lastDir = new File(System.getProperty("user.home"));
            }
            return lastDir;
        }

    }

    private class ImageFileItemListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            String imageFilePath = (String) imageFileBox.getSelectedItem();
            if (imageFilePath == null || !new File(imageFilePath).isFile()) {
                return;
            }

            RenderedImage image = FileLoadDescriptor.create(imageFilePath, null, true, null);
            getContext().setPropertyValue(ImageFileLayerSource.PROPERTY_NAME_IMAGE, image);
            getContext().setPropertyValue(ImageFileLayerSource.PROPERTY_NAME_IMAGE_FILE_PATH, imageFilePath);
            ImagePreviewWorker worker = new ImagePreviewWorker(image, imagePreviewLabel);
            worker.execute();

            String worldFilePath = createWorldFilePath(imageFilePath);

            if (new File(worldFilePath).isFile()) {
                worldFileField.setText(worldFilePath);
            } else {
                worldFileField.setText(null);
            }

            getContext().updateState();
        }

        private String createWorldFilePath(String imageFilePath) {
            String imageFileExt = FileUtils.getExtension(imageFilePath);
            // Rule for world file extension: <name>.<a><b><c> --> <name>.<a><c>w
            // see http://support.esri.com/index.cfm?fa=knowledgebase.techarticles.articleShow&d=17489
            String worldFilePath;
            if (imageFileExt != null && imageFileExt.length() == 4) { // three chars + leading dot
                String worldFileExt = imageFileExt.substring(0, 2) +
                                      imageFileExt.charAt(imageFileExt.length() - 1) + "w";
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

                Interpolation interpolation = Interpolation.getInstance(Interpolation.INTERP_NEAREST);
                RenderedImage scaledImage = ScaleDescriptor.create(sourceImage,
                                                                   scale, scale,
                                                                   0.0f, 0.0f,
                                                                   interpolation, null);
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