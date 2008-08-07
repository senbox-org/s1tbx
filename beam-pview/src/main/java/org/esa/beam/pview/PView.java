package org.esa.beam.pview;

import com.bc.ceres.binio.Format;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.ceres.glayer.swing.LayerCanvas;
import com.bc.ceres.grender.swing.ViewportScrollPane;
import com.jidesoft.utils.Lm;
import org.esa.beam.dataio.smos.SmosFormats;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.draw.ShapeFigure;
import org.esa.beam.glevel.RoiMultiLevelImage;
import org.esa.beam.glevel.TiledFileLevelImage;
import org.esa.beam.glevel.BandMultiLevelImage;
import org.esa.beam.glevel.MaskMultiLevelImage;

import javax.media.jai.JAI;
import javax.media.jai.util.ImagingListener;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

public class PView {
    private static final String APPNAME = "BEAM PView 1.1";
    private static final String WORLD_IMAGE_DIR_PROPERTY_NAME = "org.esa.beam.pview.worldImageDir";
    private static final String SMOS_DGG_DIR_PROPERTY_NAME = "org.esa.beam.pview.smosDggDir";
    private static final String LAST_DATA_DIR_PREF_KEY = "lastDataDir";
    private static final String DEFAULT_DATA_DIR = "data/smos-samples";
    private static final String ERROR_LOG = "pview-error.log";
    private static final String MANUAL_HTML = "docs/pview-manual.html";
    private static final Preferences PREFERENCES = Preferences.userNodeForPackage(PView.class);

    private static final java.util.List<Image> FRAME_ICONS = Arrays.asList(new ImageIcon(PView.class.getResource("/images/pview-16x16.png")).getImage(),
                                                                           new ImageIcon(PView.class.getResource("/images/pview-32x32.png")).getImage());
    private static Logger logger;

    private TiledFileLevelImage dggridLevelImage;
    private TiledFileLevelImage worldLevelImage;
    private int frameLocation = 0;
    private ArrayList<JFrame> frames = new ArrayList<JFrame>();

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        Locale.setDefault(Locale.ENGLISH);
        Lm.verifyLicense("Brockmann Consult", "BEAM", "lCzfhklpZ9ryjomwWxfdupxIcuIoCxg2");

        new File(ERROR_LOG).delete();
        logger = Logger.getLogger(PView.class.getPackage().getName());
        try {
            logger.addHandler(new FileHandler(ERROR_LOG));
        } catch (IOException e) {
            // fuck!
        }
        JAI.getDefaultInstance().getTileCache().setMemoryCapacity(256L * (1024 * 1024));
        JAI.getDefaultInstance().setImagingListener(new ImagingListener() {
            public boolean errorOccurred(String message, Throwable thrown, Object where, boolean isRetryable) throws RuntimeException {
                System.out.println("JAI error ocurred: " + message);
                if (thrown != null) {
                    thrown.printStackTrace();
                }
                logger.log(Level.SEVERE, "JAI error ocurred: " + message, thrown);
                return false;
            }
        });

        PView main = new PView();
        main.start(args);
    }

    private void start(String[] args) {
        if (args.length == 0) {
            openProducts();
        } else {
            openViewer(new File(args[0]));
        }
    }

    private void openProducts() {
        File[] productFiles = getProductFiles();
        if (productFiles == null || productFiles.length == 0) {
            return;
        }
        for (File productFile : productFiles) {
            openViewer(productFile);
        }
    }


    private void openViewer(File productFile) {
        try {
            openViewerImpl(productFile);
        } catch (Throwable e) {
            handleError(e);
        }
    }

    private void openViewerImpl(File productFile) throws IOException {
        final Product product = ProductIO.readProduct(productFile, null);

        final double aspectRatio = (double) product.getSceneRasterWidth() / product.getSceneRasterHeight();
        boolean worldMode = aspectRatio == 2.0;

        final Layer rootLayer = new Layer();
        rootLayer.setName("Root");

        AffineTransform i2m = createImageToModelTransform(product, worldMode);

        final Format smosFormat = SmosFormats.getInstance().getFormat(product.getProductType());
        if (smosFormat != null) {
//            String dirPath = System.getProperty(SMOS_DGG_DIR_PROPERTY_NAME);
//            if (dirPath == null || !new File(dirPath).exists()) {
//                JOptionPane.showMessageDialog(null,
//                                              "SMOS products require a DGG image.\n" +
//                                                      "Please set system property '" + SMOS_DGG_DIR_PROPERTY_NAME + "'" +
//                                                      "to a valid DGG image directory.");
//                return;
//            }
//            if (dggridLevelImage == null) {
//                dggridLevelImage = TiledFileLevelImage.createMaskOpImage(new File(dirPath), false);
//            }
            worldMode = true;
        }
        if (product.getProductType().startsWith("MER_RR__1P")
                || product.getProductType().startsWith("MER_FR__1P")
                || product.getProductType().startsWith("MER_FRS_1P")) {
            setMerisL1bRois(product);
            rootLayer.getChildLayerList().add(createMerisL1bRgbLayers(product, i2m));
        }
        setSomePlacemarks(product);

        addNonEmptyLayer(rootLayer, createPlacemarksLayer(product, i2m));
        addNonEmptyLayer(rootLayer, createBitmasksLayer(product, i2m));
        addNonEmptyLayer(rootLayer, createRoiLayers(product, i2m));
        addNonEmptyLayer(rootLayer, createNoDataMasksLayer(product, i2m));
        addNonEmptyLayer(rootLayer, createBandsLayer(product, i2m));
        addNonEmptyLayer(rootLayer, createTiePointsLayer(product, i2m));

        if (worldMode) {
            String dirPath = System.getProperty(WORLD_IMAGE_DIR_PROPERTY_NAME);
            if (dirPath != null && new File(dirPath).exists()) {
                if (worldLevelImage == null) {
                    worldLevelImage = TiledFileLevelImage.create(new File(dirPath), false);
                }
                final ImageLayer layer = new ImageLayer(worldLevelImage);
                layer.setName("World");
                layer.setVisible(true);
                rootLayer.getChildLayerList().add(layer);
            }
        }
        openFrame(productFile, rootLayer);
    }

    private void addNonEmptyLayer(Layer rootLayer, Layer childLayer) {
        if (!childLayer.getChildLayerList().isEmpty()) {
            rootLayer.getChildLayerList().add(childLayer);
        }
    }

    private File[] getProductFiles() {
        String lastDirPath = PREFERENCES.get(LAST_DATA_DIR_PREF_KEY, null);
        if (lastDirPath == null || lastDirPath.length() == 0) {
            lastDirPath = DEFAULT_DATA_DIR;
        }
        JFileChooser chooser = new JFileChooser(lastDirPath) {
            @Override
            protected JDialog createDialog(Component parent) throws HeadlessException {
                final JDialog dialog = super.createDialog(parent);
                dialog.setIconImages(FRAME_ICONS);
                return dialog;
            }
        };
        chooser.setDialogTitle(APPNAME + " - Open Data Products");
        chooser.setMultiSelectionEnabled(true);
        int option = chooser.showOpenDialog(null);
        if (chooser.getCurrentDirectory() != null) {
            if (chooser.getCurrentDirectory().compareTo(new File(lastDirPath)) != 0) {
                PREFERENCES.put(LAST_DATA_DIR_PREF_KEY, chooser.getCurrentDirectory().getPath());
            }
        }
        if (option != JFileChooser.APPROVE_OPTION) {
            return null;
        }
        final File[] selectedFiles = chooser.getSelectedFiles();
        if (selectedFiles == null || selectedFiles.length == 0) {
            return null;
        }
        return selectedFiles;
    }


    private static void setSomePlacemarks(Product product) {
        int n = 100;
        for (int i = 0; i < n; i++) {
            product.getPinGroup().add(new Pin("pin_" + i, "Pin " + i, "", new PixelPos(
                    0.5f + (i == 0 ? 0 : (int) (Math.random() * product.getSceneRasterWidth())),
                    0.5f + (i == 0 ? 0 : (int) (Math.random() * product.getSceneRasterHeight()))),
                                              null,
                                              PinSymbol.createDefaultPinSymbol()));
        }
        for (int i = 0; i < n; i++) {
            product.getGcpGroup().add(new Pin("gcp_" + i, "GCP " + i, "", new PixelPos(
                    0.5f + (i == 0 ? 0 : (int) (Math.random() * product.getSceneRasterWidth())),
                    0.5f + (i == 0 ? 0 : (int) (Math.random() * product.getSceneRasterHeight()))),
                                              null,
                                              PinSymbol.createDefaultGcpSymbol()));
        }
    }

    private static void setMerisL1bRois(Product product) {
        final ROIDefinition roiDefProto = new ROIDefinition();
        roiDefProto.setShapeFigure(new ShapeFigure(new Ellipse2D.Float(500, 150, 400, 300), false, null));
        roiDefProto.setValueRangeMin(100);
        roiDefProto.setValueRangeMax(150);
        roiDefProto.setBitmaskExpr("NOT l1_flags.BRIGHT AND NOT l1_flags.SUSPECT");
        roiDefProto.setOrCombined(true);
        roiDefProto.setInverted(false);
        roiDefProto.setShapeEnabled(false);
        roiDefProto.setBitmaskEnabled(false);
        roiDefProto.setValueRangeEnabled(false);
        roiDefProto.setPinUseEnabled(false);


        ROIDefinition roiDef;
        roiDef = roiDefProto.createCopy();
        roiDef.setShapeEnabled(true);
        product.getBandAt(0).setROIDefinition(roiDef);

        roiDef = roiDefProto.createCopy();
        roiDef.setValueRangeEnabled(true);
        product.getBandAt(1).setROIDefinition(roiDef);

        roiDef = roiDefProto.createCopy();
        roiDef.setBitmaskEnabled(true);
        product.getBandAt(2).setROIDefinition(roiDef);

        roiDef = roiDefProto.createCopy();
        roiDef.setPinUseEnabled(true);
        product.getBandAt(3).setROIDefinition(roiDef);

        roiDef = roiDefProto.createCopy();
        roiDef.setShapeEnabled(true);
        roiDef.setBitmaskEnabled(true);
        roiDef.setValueRangeEnabled(true);
        roiDef.setPinUseEnabled(true);
        product.getBandAt(4).setROIDefinition(roiDef);

        roiDef = roiDefProto.createCopy();
        roiDef.setShapeEnabled(true);
        roiDef.setBitmaskEnabled(true);
        roiDef.setValueRangeEnabled(true);
        roiDef.setPinUseEnabled(true);
        roiDef.setInverted(true);
        product.getBandAt(5).setROIDefinition(roiDef);
    }

    private static Layer createMerisL1bRgbLayers(Product product, AffineTransform i2m) {
        final Layer collectionLayer = new Layer();
        collectionLayer.setName("RGB");
        Layer l2 = createRgbLayer("RGB 751", new RasterDataNode[]{
                product.getBand("radiance_7"),
                product.getBand("radiance_5"),
                product.getBand("radiance_1"),
        }, i2m);
        collectionLayer.getChildLayerList().add(l2);
        Layer l1 = createRgbLayer("RGB 742", new RasterDataNode[]{
                product.getBand("radiance_7"),
                product.getBand("radiance_4"),
                product.getBand("radiance_2"),
        }, i2m);
        collectionLayer.getChildLayerList().add(l1);
        Layer l = createRgbLayer("RGB 931", new RasterDataNode[]{
                product.getBand("radiance_9"),
                product.getBand("radiance_3"),
                product.getBand("radiance_1"),
        }, i2m);
        collectionLayer.getChildLayerList().add(l);
        return collectionLayer;
    }

    private static Layer createRoiLayers(Product product, AffineTransform i2m) {
        final Layer collectionLayer = new Layer();
        collectionLayer.setName("ROIs");
        final String[] names = product.getBandNames();
        for (final String name : names) {
            final Band band = product.getBand(name);
            if (band.getROIDefinition() != null && band.getROIDefinition().isUsable()) {
                final Color color = Color.RED;
                final ImageLayer imageLayer = new ImageLayer(new RoiMultiLevelImage(band, color, i2m));
                imageLayer.setName("ROI of " + band.getName());
                imageLayer.setVisible(false);
                imageLayer.getStyle().setOpacity(0.5);
                collectionLayer.getChildLayerList().add(imageLayer);
            }
        }
        return collectionLayer;
    }

    private static ImageLayer createRgbLayer(String name, RasterDataNode[] rasterDataNodes, AffineTransform i2m) {
        ImageLayer imageLayer = new ImageLayer(new BandMultiLevelImage(rasterDataNodes, i2m));
        imageLayer.setName(name);
        imageLayer.setVisible(false);
        return imageLayer;
    }

    private static Layer createBitmasksLayer(Product product, AffineTransform i2m) {
        final Layer collectionLayer = new Layer();
        collectionLayer.setName("Bitmasks");
        final BitmaskDef[] bitmaskDefs = product.getBitmaskDefs();
        for (BitmaskDef bitmaskDef : bitmaskDefs) {
            final Color color = bitmaskDef.getColor();
            final String expression = bitmaskDef.getExpr();
            final ImageLayer imageLayer = new ImageLayer(new MaskMultiLevelImage(product, color, expression, false, i2m));
            imageLayer.setName(bitmaskDef.getName());
            imageLayer.setVisible(false);
            imageLayer.getStyle().setOpacity(bitmaskDef.getAlpha());
            collectionLayer.getChildLayerList().add(imageLayer);
        }
        return collectionLayer;
    }

    private static Layer createBandsLayer(Product product, AffineTransform i2m) {
        final Layer collectionLayer = new Layer();
        collectionLayer.setName("Bands");
        final String[] names = product.getBandNames();
        for (int i = 0; i < names.length; i++) {
            String name = names[i];
            final Band band = product.getBand(name);
            final ImageLayer imageLayer = new ImageLayer(new BandMultiLevelImage(band, i2m));
            imageLayer.setName(band.getName());
            imageLayer.setVisible(i == 0);
            collectionLayer.getChildLayerList().add(imageLayer);
        }

        if (!collectionLayer.getChildLayerList().isEmpty()) {
            String[] bandNames = new String[]{"radiance_13", "reflec_13"};
            boolean found = false;
            for (int i = 0; i < bandNames.length && !found; i++) {
                int bandIndex = product.getBandIndex(bandNames[i]);
                if (bandIndex != -1) {
                    collectionLayer.getChildLayerList().get(bandIndex).setVisible(true);
                    found = true;
                }
            }
            collectionLayer.getChildLayerList().get(0).setVisible(!found);
        }
        return collectionLayer;
    }

    private static Layer createNoDataMasksLayer(Product product, AffineTransform i2m) {
        final Layer collectionLayer = new Layer();
        collectionLayer.setName("No-Data Masks");
        final String[] names = product.getBandNames();
        for (final String name : names) {
            final Band band = product.getBand(name);
            if (band.getValidMaskExpression() != null) {
                final Color color = Color.ORANGE;
                final String expression = band.getValidMaskExpression();
                final ImageLayer imageLayer = new ImageLayer(new MaskMultiLevelImage(product, color, expression, true, i2m));
                imageLayer.setName("No-data mask of " + band.getName());
                imageLayer.setVisible(false);
                imageLayer.getStyle().setOpacity(0.5);
                collectionLayer.getChildLayerList().add(imageLayer);
            }
        }
        return collectionLayer;
    }

    private static Layer createTiePointsLayer(Product product, AffineTransform i2m) {
        final Layer collectionLayer = new Layer();
        collectionLayer.setName("Tie-Point Grids");
        final String[] names = product.getTiePointGridNames();
        for (final String name : names) {
            final TiePointGrid tiePointGrid = product.getTiePointGrid(name);
            final ImageLayer imageLayer = new ImageLayer(new BandMultiLevelImage(tiePointGrid, i2m));
            imageLayer.setName(tiePointGrid.getName());
            imageLayer.setVisible(false);
            collectionLayer.getChildLayerList().add(imageLayer);
        }
        return collectionLayer;
    }

    private static Layer createPlacemarksLayer(Product product, AffineTransform i2m) {
        final Layer collectionLayer = new Layer();
        collectionLayer.setName("Placemarks");

        if (product.getGeoCoding() != null) {
            final org.esa.beam.glayer.PlacemarkLayer pinLayer = new org.esa.beam.glayer.PlacemarkLayer(product, PinDescriptor.INSTANCE, i2m);
            pinLayer.setName("Pins");
            pinLayer.setVisible(false);
            pinLayer.setTextEnabled(true);
            collectionLayer.getChildLayerList().add(pinLayer);

            final org.esa.beam.glayer.PlacemarkLayer gcpLayer = new org.esa.beam.glayer.PlacemarkLayer(product, GcpDescriptor.INSTANCE, i2m);
            gcpLayer.setName("GCPs");
            gcpLayer.setVisible(false);
            gcpLayer.setTextEnabled(false);
            collectionLayer.getChildLayerList().add(gcpLayer);
        }

        return collectionLayer;
    }


    private static AffineTransform createImageToModelTransform(Product product, boolean worldMode) {
        return worldMode ? AffineTransform.getScaleInstance(360.0 / product.getSceneRasterWidth(), 180.0 / product.getSceneRasterHeight()) : new AffineTransform();
    }

    private void openFrame(final File file, final Layer collectionLayer) {
        final int initialViewWidth = 800;
        final int initialViewHeight = 800;

        double modelWidth = collectionLayer.getBounds().getWidth();
        double initialZoomFactor = initialViewWidth / modelWidth;
        if (initialZoomFactor <= 1.e-3) {
            initialZoomFactor = 1;
        }

        final LayerCanvas layerCanvas = new LayerCanvas(collectionLayer);
        ViewportScrollPane viewportScrollPane = new ViewportScrollPane(layerCanvas);
                
        final JFrame frame = new JFrame("View - [" + file.getName() + "] - " + APPNAME);
        frame.setJMenuBar(createMenuBar());
        frame.getContentPane().add(viewportScrollPane, BorderLayout.CENTER);
//        frame.getContentPane().add(layerCanvas, BorderLayout.CENTER);
        frame.setSize(initialViewWidth, initialViewHeight);
        frame.setLocation(frameLocation, frameLocation);
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.setIconImages(FRAME_ICONS);
        frame.addWindowListener(new WindowAdapter() {
            public void windowOpened(WindowEvent event) {
                layerCanvas.getViewport().zoom(collectionLayer.getBounds());
            }

            @Override
            public void windowClosing(WindowEvent e) {
                frame.dispose();
                collectionLayer.dispose();
                frames.remove(frame);
                if (frames.size() == 0) {
                    System.exit(0);
                }
            }
        });

        frame.setVisible(true);
        LayerManager.showLayerManager(frame, "Layers - [" + file.getName() + "] - " + APPNAME,
                                      collectionLayer, new Point(initialViewWidth + frameLocation, frameLocation));
        frameLocation += 24;
        frames.add(frame);
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        menuBar.add(createFileMenu());
        menuBar.add(createHelpMenu());
        return menuBar;
    }

    private JMenu createHelpMenu() {
        JMenuItem openMenuItem = new JMenuItem("Manual");
        openMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                try {
                    Desktop.getDesktop().browse(new File(MANUAL_HTML).toURI());
                } catch (IOException e) {
                    handleError(e);
                }
            }
        });

        JMenuItem exitMenuItem = new JMenuItem("About");
        exitMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JOptionPane.showMessageDialog(null, APPNAME +
                        "\n(C) 2008 by Brockmann Consult GmbH" +
                        "\n\nA proof of concept for forthcoming BEAM tiled imaging,\n" +
                        "layer management and SMOS support.", "About", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        JMenu helpMenu = new JMenu("Help");
        helpMenu.add(openMenuItem);
        helpMenu.add(exitMenuItem);
        return helpMenu;
    }

    private JMenu createFileMenu() {
        JMenuItem openMenuItem = new JMenuItem("Open");
        openMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                openProducts();
            }
        });

        JMenuItem exitMenuItem = new JMenuItem("Exit");
        exitMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });

        JMenu fileMenu = new JMenu("File");
        fileMenu.add(openMenuItem);
        fileMenu.add(exitMenuItem);
        return fileMenu;
    }

    private void handleError(Throwable e) {
        JOptionPane.showMessageDialog(null, "An error occured:\n" + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        logger.log(Level.SEVERE, e.getMessage(), e);
    }

}