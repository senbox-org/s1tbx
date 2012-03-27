/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.beam.framework.ui.product;

import com.bc.ceres.core.Assert;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.ceres.glevel.MultiLevelSource;
import com.bc.ceres.grender.Viewport;
import com.bc.ceres.grender.support.BufferedImageRendering;
import com.bc.ceres.grender.support.DefaultViewport;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import com.bc.jexp.ParseException;
import com.bc.jexp.Term;
import org.esa.beam.framework.dataio.ProductSubsetDef;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.dataop.barithm.BandArithmetic;
import org.esa.beam.framework.dataop.barithm.RasterDataSymbol;
import org.esa.beam.framework.param.ParamChangeEvent;
import org.esa.beam.framework.param.ParamChangeListener;
import org.esa.beam.framework.param.ParamGroup;
import org.esa.beam.framework.param.Parameter;
import org.esa.beam.framework.ui.GridBagUtils;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.framework.ui.SliderBoxImageDisplay;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.glevel.BandImageMultiLevelSource;
import org.esa.beam.jai.ImageManager;
import org.esa.beam.util.*;
import org.esa.beam.util.math.MathUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A modal dialog used to specify data product subsets.
 */
public class ProductSubsetDialog extends ModalDialog {

    private static final String MEM_LABEL_TEXT = "Estimated, raw storage size: "; /*I18N*/
    private static final Color MEM_LABEL_WARN_COLOR = Color.red;
    private static final Color MEM_LABEL_NORM_COLOR = Color.black;
    private static final int MAX_THUMBNAIL_WIDTH = 148;
    private static final int MIN_SUBSET_SIZE = 1;
    private static final Font SMALL_PLAIN_FONT = new Font("SansSerif", Font.PLAIN, 10);
    private static final Font SMALL_ITALIC_FONT = SMALL_PLAIN_FONT.deriveFont(Font.ITALIC);

    private Product product;
    private ProductSubsetDef productSubsetDef;
    private ProductSubsetDef givenProductSubsetDef;
    private JLabel memLabel;
    private SpatialSubsetPane spatialSubsetPane;
    private ProductNodeSubsetPane bandSubsetPane;
    private ProductNodeSubsetPane tiePointGridSubsetPane;
    private ProductNodeSubsetPane metadataSubsetPane;
    private double memWarnLimit;
    private static final double DEFAULT_MEM_WARN_LIMIT = 1000.0;
    private AtomicBoolean updatingUI;

    /**
     * Constructs a new subset dialog.
     *
     * @param window  the parent window
     * @param product the product for which the subset is to be specified, must not be <code>null</code>
     */
    public ProductSubsetDialog(Window window, Product product) {
        this(window, product, DEFAULT_MEM_WARN_LIMIT);
    }

    /**
     * Constructs a new subset dialog.
     *
     * @param window       the parent window
     * @param product      the product for which the subset is to be specified, must not be <code>null</code>
     * @param memWarnLimit the warning limit in megabytes
     */
    public ProductSubsetDialog(Window window, Product product, double memWarnLimit) {
        this(window, product, null, memWarnLimit);
    }

    /**
     * Constructs a new subset dialog.
     *
     * @param window           the parent window
     * @param product          the product for which the subset is to be specified, must not be <code>null</code>
     * @param productSubsetDef the initial product subset definition, can be <code>null</code>
     */
    public ProductSubsetDialog(Window window,
                               Product product,
                               ProductSubsetDef productSubsetDef) {
        this(window, product, productSubsetDef, DEFAULT_MEM_WARN_LIMIT);
    }

    /**
     * Constructs a new subset dialog.
     *
     * @param window           the parent window
     * @param product          the product for which the subset is to be specified, must not be <code>null</code>
     * @param productSubsetDef the initial product subset definition, can be <code>null</code>
     * @param memWarnLimit     the warning limit in megabytes
     */
    public ProductSubsetDialog(Window window,
                               Product product,
                               ProductSubsetDef productSubsetDef,
                               double memWarnLimit) {
        super(window,
              "Specify Product Subset", /*I18N*/
              ModalDialog.ID_OK
              | ModalDialog.ID_CANCEL
              | ModalDialog.ID_HELP,
              "subsetDialog");
        Guardian.assertNotNull("product", product);
        this.product = product;
        givenProductSubsetDef = productSubsetDef;
        this.productSubsetDef = new ProductSubsetDef("undefined");
        this.memWarnLimit = memWarnLimit;
        updatingUI = new AtomicBoolean(false);
        createUI();
    }

    public Product getProduct() {
        return product;
    }

    public ProductSubsetDef getProductSubsetDef() {
        return productSubsetDef;
    }

    @Override
    protected void onOK() {
        boolean ok;
        ok = checkReferencedRastersIncluded();
        if (!ok) {
            return;
        }
        ok = checkFlagDatasetIncluded();
        if (!ok) {
            return;
        }

        spatialSubsetPane.cancelThumbnailLoader();
        if (productSubsetDef != null && productSubsetDef.isEntireProductSelected()) {
            productSubsetDef = null;
        }
        super.onOK();
    }

    private boolean checkReferencedRastersIncluded() {
        final Set<String> notIncludedNames = new TreeSet<String>();
        final List<String> includedNodeNames = Arrays.asList(productSubsetDef.getNodeNames());
        for (final String nodeName : includedNodeNames) {
            final RasterDataNode rasterDataNode = product.getRasterDataNode(nodeName);
            if (rasterDataNode != null) {
                collectNotIncludedReferences(rasterDataNode, notIncludedNames);
            }
        }

        boolean ok = true;
        if (!notIncludedNames.isEmpty()) {
            StringBuilder nameListText = new StringBuilder();
            for (String notIncludedName : notIncludedNames) {
                nameListText.append("  '").append(notIncludedName).append("'\n");
            }

            final String pattern = "The following dataset(s) are referenced but not included\n" +
                                   "in your current subset definition:\n" +
                                   "{0}\n" +
                                   "If you do not include these dataset(s) into your selection,\n" +
                                   "you might get unexpected results while working with the\n" +
                                   "resulting product.\n\n" +
                                   "Do you wish to include the referenced dataset(s) into your\n" +
                                   "subset definition?\n"; /*I18N*/
            final MessageFormat format = new MessageFormat(pattern);
            int status = JOptionPane.showConfirmDialog(getJDialog(),
                                                       format.format(new Object[]{nameListText.toString()}),
                                                       "Incomplete Subset Definition", /*I18N*/
                                                       JOptionPane.YES_NO_CANCEL_OPTION);
            if (status == JOptionPane.YES_OPTION) {
                final String[] nodenames = notIncludedNames.toArray(new String[notIncludedNames.size()]);
                productSubsetDef.addNodeNames(nodenames);
                ok = true;
            } else if (status == JOptionPane.NO_OPTION) {
                ok = true;
            } else if (status == JOptionPane.CANCEL_OPTION) {
                ok = false;
            }
        }
        return ok;
    }

    private void collectNotIncludedReferences(final RasterDataNode rasterDataNode, final Set<String> notIncludedNames) {
        final RasterDataNode[] referencedNodes = getReferencedNodes(rasterDataNode);
        for (final RasterDataNode referencedNode : referencedNodes) {
            final String name = referencedNode.getName();
            if (!productSubsetDef.isNodeAccepted(name) && !notIncludedNames.contains(name)) {
                notIncludedNames.add(name);
                collectNotIncludedReferences(referencedNode, notIncludedNames);
            }
        }
    }

    private static RasterDataNode[] getReferencedNodes(final RasterDataNode node) {
        final Product product = node.getProduct();
        if (product != null) {
            final List<String> expressions = new ArrayList<String>(10);
            if (node.getValidPixelExpression() != null) {
                expressions.add(node.getValidPixelExpression());
            }
            final ProductNodeGroup<Mask> overlayMaskGroup = node.getOverlayMaskGroup();
            if (overlayMaskGroup.getNodeCount() > 0) {
                final Mask[] overlayMasks = overlayMaskGroup.toArray(new Mask[overlayMaskGroup.getNodeCount()]);
                for (final Mask overlayMask : overlayMasks) {
                    final String expression;
                    if (overlayMask.getImageType() == Mask.BandMathsType.INSTANCE) {
                        expression = Mask.BandMathsType.getExpression(overlayMask);
                    } else if (overlayMask.getImageType() == Mask.RangeType.INSTANCE) {
                        expression = Mask.RangeType.getExpression(overlayMask);
                    } else {
                        expression = null;
                    }
                    if (expression != null) {
                        expressions.add(expression);
                    }
                }
            }
            if (node instanceof VirtualBand) {
                final VirtualBand virtualBand = (VirtualBand) node;
                expressions.add(virtualBand.getExpression());
            }

            final ArrayList<Term> termList = new ArrayList<Term>(10);
            for (final String expression : expressions) {
                try {
                    final Term term = product.parseExpression(expression);
                    if (term != null) {
                        termList.add(term);
                    }
                } catch (ParseException e) {
                    // @todo se handle parse exception
                    Debug.trace(e);
                }
            }

            final Term[] terms = termList.toArray(new Term[termList.size()]);
            final RasterDataSymbol[] refRasterDataSymbols = BandArithmetic.getRefRasterDataSymbols(terms);
            return BandArithmetic.getRefRasters(refRasterDataSymbols);
        }
        return new RasterDataNode[0];
    }

    private boolean checkFlagDatasetIncluded() {
        final String[] nodeNames = productSubsetDef.getNodeNames();
        final List<String> flagDsNameList = new ArrayList<String>(10);
        boolean flagDsInSubset = false;
        for (int i = 0; i < product.getNumBands(); i++) {
            Band band = product.getBandAt(i);
            if (band.getFlagCoding() != null) {
                flagDsNameList.add(band.getName());
                if (StringUtils.contains(nodeNames, band.getName())) {
                    flagDsInSubset = true;
                }
                break;
            }
        }

        final int numFlagDs = flagDsNameList.size();
        boolean ok = true;
        if (numFlagDs > 0 && !flagDsInSubset) {
            int status = JOptionPane.showConfirmDialog(getJDialog(),
                                                       "No flag dataset selected.\n\n"
                                                       + "If you do not include a flag dataset in the subset,\n"
                                                       + "you will not be able to create bitmask overlays.\n\n"
                                                       + "Do you wish to include the available flag dataset(s)\n"
                                                       + "in the current subset?\n",
                                                       "No Flag Dataset Selected",
                                                       JOptionPane.YES_NO_CANCEL_OPTION);
            if (status == JOptionPane.YES_OPTION) {
                productSubsetDef.addNodeNames(flagDsNameList.toArray(new String[numFlagDs]));
                ok = true;
            } else if (status == JOptionPane.NO_OPTION) {
                /* OK, no flag datasets wanted */
                ok = true;
            } else if (status == JOptionPane.CANCEL_OPTION) {
                ok = false;
            }
        }

        return ok;
    }

    @Override
    protected void onCancel() {
        spatialSubsetPane.cancelThumbnailLoader();
        super.onCancel();
    }

    private void createUI() {
        memLabel = new JLabel("####", JLabel.RIGHT);

        JTabbedPane tabbedPane = new JTabbedPane();
        setComponentName(tabbedPane, "TabbedPane");

        spatialSubsetPane = createSpatialSubsetPane();
        setComponentName(spatialSubsetPane, "SpatialSubsetPane");
        if (spatialSubsetPane != null) {
            tabbedPane.addTab("Spatial Subset", spatialSubsetPane); /*I18N*/
        }

        bandSubsetPane = createBandSubsetPane();
        setComponentName(bandSubsetPane, "BandSubsetPane");
        if (bandSubsetPane != null) {
            tabbedPane.addTab("Band Subset", bandSubsetPane);
        }

        tiePointGridSubsetPane = createTiePointGridSubsetPane();
        setComponentName(tiePointGridSubsetPane, "TiePointGridSubsetPane");
        if (tiePointGridSubsetPane != null) {
            tabbedPane.addTab("Tie-Point Grid Subset", tiePointGridSubsetPane);
        }

        metadataSubsetPane = createAnnotationSubsetPane();
        setComponentName(metadataSubsetPane, "TiePointGridSubsetPane");
        if (metadataSubsetPane != null) {
            tabbedPane.addTab("Metadata Subset", metadataSubsetPane);
        }

        tabbedPane.setPreferredSize(new Dimension(512, 380));
        tabbedPane.setSelectedIndex(0);

        JPanel contentPane = new JPanel(new BorderLayout(4, 4));
        setComponentName(contentPane, "ContentPane");

        contentPane.add(tabbedPane, BorderLayout.CENTER);
        contentPane.add(memLabel, BorderLayout.SOUTH);
        setContent(contentPane);

        updateSubsetDefNodeNameList();
    }

    private SpatialSubsetPane createSpatialSubsetPane() {
        return new SpatialSubsetPane();
    }

    private ProductNodeSubsetPane createBandSubsetPane() {
        Band[] bands = product.getBands();
        if (bands.length == 0) {
            return null;
        }
        return new ProductNodeSubsetPane(product.getBands(), true);
    }

    private ProductNodeSubsetPane createTiePointGridSubsetPane() {
        TiePointGrid[] tiePointGrids = product.getTiePointGrids();
        if (tiePointGrids.length == 0) {
            return null;
        }
        return new ProductNodeSubsetPane(product.getTiePointGrids(),
                                         new String[]{BeamConstants.LAT_DS_NAME, BeamConstants.LON_DS_NAME},
                                         true);
    }

    private ProductNodeSubsetPane createAnnotationSubsetPane() {
        final MetadataElement metadataRoot = product.getMetadataRoot();
        final MetadataElement[] metadataElements = metadataRoot.getElements();
        final String[] metaNodes;
        if (metadataElements.length == 0) {
            return null;
        }
        // metadata elements must be added to includeAlways list
        // to ensure that they are selected if isIgnoreMetada is set to false
        if (givenProductSubsetDef != null && !givenProductSubsetDef.isIgnoreMetadata()) {
            metaNodes = new String[metadataElements.length];
            for (int i = 0; i < metadataElements.length; i++) {
                final MetadataElement metadataElement = metadataElements[i];
                metaNodes[i] = metadataElement.getName();
            }
        } else {
            metaNodes = new String[0];
        }
        final String[] includeNodes = StringUtils.addToArray(metaNodes, Product.HISTORY_ROOT_NAME);
        return new ProductNodeSubsetPane(metadataElements, includeNodes, true);
    }

    private static void setComponentName(JComponent component, String name) {
        if (component != null) {
            Container parent = component.getParent();
            if (parent != null) {
                component.setName(parent.getName() + "." + name);
            } else {
                component.setName(name);
            }
        }
    }

    private void updateSubsetDefRegion(int x1, int y1, int x2, int y2, int sx, int sy) {
        productSubsetDef.setRegion(x1, y1, x2 - x1 + 1, y2 - y1 + 1);
        productSubsetDef.setSubSampling(sx, sy);
        updateMemDisplay();
    }

    private void updateSubsetDefNodeNameList() {
        /* We don't use this option! */
        productSubsetDef.setIgnoreMetadata(false);
        productSubsetDef.setNodeNames(null);
        if (bandSubsetPane != null) {
            productSubsetDef.addNodeNames(bandSubsetPane.getSubsetNames());
        }
        if (tiePointGridSubsetPane != null) {
            productSubsetDef.addNodeNames(tiePointGridSubsetPane.getSubsetNames());
        }
        if (metadataSubsetPane != null) {
            productSubsetDef.addNodeNames(metadataSubsetPane.getSubsetNames());
        }
        updateMemDisplay();
    }

    private void updateMemDisplay() {
        if (product != null) {
            long storageMem = product.getRawStorageSize(productSubsetDef);
            double factor = 1.0 / (1024 * 1024);
            double megas = MathUtils.round(factor * storageMem, 10);
            if (megas > memWarnLimit) {
                memLabel.setForeground(MEM_LABEL_WARN_COLOR);
            } else {
                memLabel.setForeground(MEM_LABEL_NORM_COLOR);
            }
            memLabel.setText(MEM_LABEL_TEXT + megas + "M");
        } else {
            memLabel.setText(" ");
        }
    }

    private class SpatialSubsetPane extends JPanel
            implements ActionListener, ParamChangeListener, SliderBoxImageDisplay.SliderBoxChangeListener {

        private Parameter paramX1;
        private Parameter paramY1;
        private Parameter paramX2;
        private Parameter paramY2;
        private Parameter paramSX;
        private Parameter paramSY;
        private Parameter paramWestLon1;
        private Parameter paramEastLon2;
        private Parameter paramNorthLat1;
        private Parameter paramSouthLat2;

        private SliderBoxImageDisplay imageCanvas;
        private JCheckBox fixSceneWidthCheck;
        private JCheckBox fixSceneHeightCheck;
        private JLabel subsetWidthLabel;
        private JLabel subsetHeightLabel;
        private int thumbNailSubSampling;
        private JButton setToVisibleButton;
        private JScrollPane imageScrollPane;
        private ProgressMonitorSwingWorker<BufferedImage, Object> thumbnailLoader;

        private SpatialSubsetPane() {
            initParameters();
            createUI();
        }

        private void createUI() {

            setThumbnailSubsampling();
            final Dimension imageSize = getScaledImageSize();
            thumbnailLoader = new ProgressMonitorSwingWorker<BufferedImage, Object>(this,
                                                                                    "Loading thumbnail image...") {

                @Override
                protected BufferedImage doInBackground(ProgressMonitor pm) throws Exception {
                    return createThumbNailImage(imageSize, pm);
                }

                @Override
                protected void done() {
                    BufferedImage thumbnail = null;
                    try {
                        thumbnail = get();
                    } catch (Exception e) {
                        if (e instanceof IOException) {
                            showErrorDialog("Failed to load thumbnail image:\n" + e.getMessage());
                        }
                    }

                    if (thumbnail != null) {
                        imageCanvas.setImage(thumbnail);
                    }

                }

            };
            thumbnailLoader.execute();
            imageCanvas = new SliderBoxImageDisplay(imageSize.width, imageSize.height, this);
            imageCanvas.setSize(imageSize.width, imageSize.height);
            setComponentName(imageCanvas, "ImageCanvas");


            imageScrollPane = new JScrollPane(imageCanvas);
            imageScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            imageScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
            imageScrollPane.getViewport().setExtentSize(new Dimension(MAX_THUMBNAIL_WIDTH, 2 * MAX_THUMBNAIL_WIDTH));
            setComponentName(imageScrollPane, "ImageScrollPane");


            subsetWidthLabel = new JLabel("####", JLabel.RIGHT);
            subsetHeightLabel = new JLabel("####", JLabel.RIGHT);

            setToVisibleButton = new JButton("Use Preview");/*I18N*/
            setToVisibleButton.setMnemonic('v');
            setToVisibleButton.setToolTipText("Use coordinates of visible thumbnail area"); /*I18N*/
            setToVisibleButton.addActionListener(this);
            setComponentName(setToVisibleButton, "UsePreviewButton");


            fixSceneWidthCheck = new JCheckBox("Fix full width");
            fixSceneWidthCheck.setMnemonic('w');
            fixSceneWidthCheck.setToolTipText("Checks whether or not to fix the full scene width");
            fixSceneWidthCheck.addActionListener(this);
            setComponentName(fixSceneWidthCheck, "FixWidthCheck");

            fixSceneHeightCheck = new JCheckBox("Fix full height");
            fixSceneHeightCheck.setMnemonic('h');
            fixSceneHeightCheck.setToolTipText("Checks whether or not to fix the full scene height");
            fixSceneHeightCheck.addActionListener(this);
            setComponentName(fixSceneHeightCheck, "FixHeightCheck");

            JPanel textInputPane = GridBagUtils.createPanel();
            setComponentName(textInputPane, "TextInputPane");
            final JTabbedPane tabbedPane = new JTabbedPane();
            setComponentName(tabbedPane, "coordinatePane");
            tabbedPane.addTab("Pixel Coordinates", createPixelCoordinatesPane());
            tabbedPane.addTab("Geo Coordinates", createGeoCoordinatesPane());
            tabbedPane.setEnabledAt(1, canUseGeoCoordinates(product));

            GridBagConstraints gbc = GridBagUtils.createConstraints(
                    "insets.left=7,anchor=WEST,fill=HORIZONTAL, weightx=1.0");
            GridBagUtils.setAttributes(gbc, "gridwidth=2");
            GridBagUtils.addToPanel(textInputPane, tabbedPane, gbc, "gridx=0,gridy=0");

            GridBagUtils.setAttributes(gbc, "insets.top=7,gridwidth=1");
            GridBagUtils.addToPanel(textInputPane, new JLabel("Scene step X:"), gbc, "gridx=0,gridy=1");
            GridBagUtils.addToPanel(textInputPane, UIUtils.createSpinner(paramSX, 1, "#0"), gbc, "gridx=1,gridy=1");
            GridBagUtils.setAttributes(gbc, "insets.top=1");
            GridBagUtils.addToPanel(textInputPane, new JLabel("Scene step Y:"), gbc, "gridx=0,gridy=2");
            GridBagUtils.addToPanel(textInputPane, UIUtils.createSpinner(paramSY, 1, "#0"), gbc, "gridx=1,gridy=2");

            GridBagUtils.setAttributes(gbc, "insets.top=4");
            GridBagUtils.addToPanel(textInputPane, new JLabel("Subset scene width:"), gbc, "gridx=0,gridy=3");
            GridBagUtils.addToPanel(textInputPane, subsetWidthLabel, gbc, "gridx=1,gridy=3");

            GridBagUtils.setAttributes(gbc, "insets.top=1");
            GridBagUtils.addToPanel(textInputPane, new JLabel("Subset scene height:"), gbc, "gridx=0,gridy=4");
            GridBagUtils.addToPanel(textInputPane, subsetHeightLabel, gbc, "gridx=1,gridy=4");

            GridBagUtils.setAttributes(gbc, "insets.top=4,gridwidth=1");
            GridBagUtils.addToPanel(textInputPane, new JLabel("Source scene width:"), gbc, "gridx=0,gridy=5");
            GridBagUtils.addToPanel(textInputPane, new JLabel(String.valueOf(product.getSceneRasterWidth()),
                                                              JLabel.RIGHT), gbc, "gridx=1,gridy=5");

            GridBagUtils.setAttributes(gbc, "insets.top=1");
            GridBagUtils.addToPanel(textInputPane, new JLabel("Source scene height:"), gbc, "gridx=0,gridy=6");
            GridBagUtils.addToPanel(textInputPane, new JLabel(String.valueOf(product.getSceneRasterHeight()),
                                                              JLabel.RIGHT), gbc, "gridx=1,gridy=6");

            GridBagUtils.setAttributes(gbc, "insets.top=7,gridwidth=1, gridheight=2");
            GridBagUtils.addToPanel(textInputPane, setToVisibleButton, gbc, "gridx=0,gridy=7");

            GridBagUtils.setAttributes(gbc, "insets.top=7,gridwidth=1, gridheight=1");
            GridBagUtils.addToPanel(textInputPane, fixSceneWidthCheck, gbc, "gridx=1,gridy=7");

            GridBagUtils.setAttributes(gbc, "insets.top=1,gridwidth=1");
            GridBagUtils.addToPanel(textInputPane, fixSceneHeightCheck, gbc, "gridx=1,gridy=8");

            setLayout(new BorderLayout(4, 4));
            add(imageScrollPane, BorderLayout.WEST);
            add(textInputPane, BorderLayout.CENTER);
            setBorder(BorderFactory.createEmptyBorder(7, 7, 7, 7));

            updateUIState(null);
            imageCanvas.scrollRectToVisible(imageCanvas.getSliderBoxBounds());
        }

        private boolean canUseGeoCoordinates(Product product) {
            final GeoCoding geoCoding = product.getGeoCoding();
            return geoCoding != null && geoCoding.canGetPixelPos() && geoCoding.canGetGeoPos();
        }

        private JPanel createGeoCoordinatesPane() {
            JPanel geoCoordinatesPane = GridBagUtils.createPanel();
            setComponentName(geoCoordinatesPane, "geoCoordinatesPane");

            GridBagConstraints gbc = GridBagUtils.createConstraints(
                    "insets.left=3,anchor=WEST,fill=HORIZONTAL, weightx=1.0");
            GridBagUtils.setAttributes(gbc, "insets.top=4");
            GridBagUtils.addToPanel(geoCoordinatesPane, new JLabel("North latitude bound:"), gbc, "gridx=0,gridy=0");
            GridBagUtils.addToPanel(geoCoordinatesPane, UIUtils.createSpinner(paramNorthLat1, (double) 1, "#0.00#"),
                                    gbc, "gridx=1,gridy=0");
            GridBagUtils.setAttributes(gbc, "insets.top=1");
            GridBagUtils.addToPanel(geoCoordinatesPane, new JLabel("West longitude bound:"), gbc, "gridx=0,gridy=1");
            GridBagUtils.addToPanel(geoCoordinatesPane, UIUtils.createSpinner(paramWestLon1, (double) 1, "#0.00#"),
                                    gbc, "gridx=1,gridy=1");

            GridBagUtils.setAttributes(gbc, "insets.top=4");
            GridBagUtils.addToPanel(geoCoordinatesPane, new JLabel("South latitude bound:"), gbc, "gridx=0,gridy=2");
            GridBagUtils.addToPanel(geoCoordinatesPane, UIUtils.createSpinner(paramSouthLat2, (double) 1, "#0.00#"),
                                    gbc, "gridx=1,gridy=2");
            GridBagUtils.setAttributes(gbc, "insets.top=1");
            GridBagUtils.addToPanel(geoCoordinatesPane, new JLabel("East longitude bound:"), gbc, "gridx=0,gridy=3");
            GridBagUtils.addToPanel(geoCoordinatesPane, UIUtils.createSpinner(paramEastLon2, (double) 1, "#0.00#"),
                                    gbc, "gridx=1,gridy=3");
            return geoCoordinatesPane;
        }

        private JPanel createPixelCoordinatesPane() {
            GridBagConstraints gbc = GridBagUtils.createConstraints(
                    "insets.left=3,anchor=WEST,fill=HORIZONTAL, weightx=1.0");
            JPanel pixelCoordinatesPane = GridBagUtils.createPanel();
            setComponentName(pixelCoordinatesPane, "pixelCoordinatesPane");

            GridBagUtils.setAttributes(gbc, "insets.top=4");
            GridBagUtils.addToPanel(pixelCoordinatesPane, new JLabel("Scene start X:"), gbc, "gridx=0,gridy=0");
            GridBagUtils.addToPanel(pixelCoordinatesPane, UIUtils.createSpinner(paramX1, 25, "#0"),
                                    gbc, "gridx=1,gridy=0");
            GridBagUtils.setAttributes(gbc, "insets.top=1");
            GridBagUtils.addToPanel(pixelCoordinatesPane, new JLabel("Scene start Y:"), gbc, "gridx=0,gridy=1");
            GridBagUtils.addToPanel(pixelCoordinatesPane, UIUtils.createSpinner(paramY1, 25, "#0"),
                                    gbc, "gridx=1,gridy=1");

            GridBagUtils.setAttributes(gbc, "insets.top=4");
            GridBagUtils.addToPanel(pixelCoordinatesPane, new JLabel("Scene end X:"), gbc, "gridx=0,gridy=2");
            GridBagUtils.addToPanel(pixelCoordinatesPane, UIUtils.createSpinner(paramX2, 25, "#0"),
                                    gbc, "gridx=1,gridy=2");
            GridBagUtils.setAttributes(gbc, "insets.top=1");
            GridBagUtils.addToPanel(pixelCoordinatesPane, new JLabel("Scene end Y:"), gbc, "gridx=0,gridy=3");
            GridBagUtils.addToPanel(pixelCoordinatesPane, UIUtils.createSpinner(paramY2, 25, "#0"),
                                    gbc, "gridx=1,gridy=3");
            return pixelCoordinatesPane;
        }

        private void setThumbnailSubsampling() {
            int w = product.getSceneRasterWidth();

            thumbNailSubSampling = w / MAX_THUMBNAIL_WIDTH;
            if (thumbNailSubSampling <= 1) {
                thumbNailSubSampling = 1;
            }
        }

        public void cancelThumbnailLoader() {
            if (thumbnailLoader != null) {
                thumbnailLoader.cancel(true);
            }
        }

        public boolean isThumbnailLoaderCanceled() {
            return thumbnailLoader != null && thumbnailLoader.isCancelled();
        }

        @Override
        public void sliderBoxChanged(Rectangle sliderBoxBounds) {
            int x1 = sliderBoxBounds.x * thumbNailSubSampling;
            int y1 = sliderBoxBounds.y * thumbNailSubSampling;
            int x2 = x1 + sliderBoxBounds.width * thumbNailSubSampling;
            int y2 = y1 + sliderBoxBounds.height * thumbNailSubSampling;
            int w = product.getSceneRasterWidth();
            int h = product.getSceneRasterHeight();
            if (x1 < 0) {
                x1 = 0;
            }
            if (x1 > w - 2) {
                x1 = w - 2;
            }
            if (y1 < 0) {
                y1 = 0;
            }
            if (y1 > h - 2) {
                y1 = h - 2;
            }
            if (x2 < 1) {
                x2 = 1;
            }
            if (x2 > w - 1) {
                x2 = w - 1;
            }
            if (y2 < 1) {
                y2 = 1;
            }
            if (y2 > h - 1) {
                y2 = h - 1;
            }
            // first reset the bounds, otherwise negative regions can occur
            paramX1.setValue(0, null);
            paramY1.setValue(0, null);
            paramX2.setValue(w - 1, null);
            paramY2.setValue(h - 1, null);

            paramX1.setValue(x1, null);
            paramY1.setValue(y1, null);
            paramX2.setValue(x2, null);
            paramY2.setValue(y2, null);
        }

        /**
         * Invoked when an action occurs.
         */
        @Override
        public void actionPerformed(ActionEvent e) {
            if (e.getSource().equals(fixSceneWidthCheck)) {
                imageCanvas.setImageWidthFixed(fixSceneWidthCheck.isSelected());
                final boolean enable = !fixSceneWidthCheck.isSelected();
                paramX1.setUIEnabled(enable);
                paramX2.setUIEnabled(enable);
            }
            if (e.getSource().equals(fixSceneHeightCheck)) {
                imageCanvas.setImageHeightFixed(fixSceneHeightCheck.isSelected());
                final boolean enable = !fixSceneHeightCheck.isSelected();
                paramY1.setUIEnabled(enable);
                paramY2.setUIEnabled(enable);
            }
            if (e.getSource().equals(setToVisibleButton)) {
                imageCanvas.setSliderBoxBounds(imageScrollPane.getViewport().getViewRect(), true);
            }
        }

        /**
         * Called if the value of a parameter changed.
         *
         * @param event the parameter change event
         */
        @Override
        public void parameterValueChanged(ParamChangeEvent event) {
            updateUIState(event);
        }

        private void initParameters() {
            ParamGroup pg = new ParamGroup();
            addPixelParameter(pg);
            addGeoParameter(pg);
            pg.addParamChangeListener(this);
        }

        private void addGeoParameter(ParamGroup pg) {

            paramNorthLat1 = new Parameter("geo_lat1", 90.0f);
            paramNorthLat1.getProperties().setDescription("North bound latitude");
            paramNorthLat1.getProperties().setPhysicalUnit("°");
            paramNorthLat1.getProperties().setMinValue(-90.0f);
            paramNorthLat1.getProperties().setMaxValue(90.0f);
            pg.addParameter(paramNorthLat1);

            paramWestLon1 = new Parameter("geo_lon1", -180.0f);
            paramWestLon1.getProperties().setDescription("West bound longitude");
            paramWestLon1.getProperties().setPhysicalUnit("°");
            paramWestLon1.getProperties().setMinValue(-180.0f);
            paramWestLon1.getProperties().setMaxValue(180.0f);
            pg.addParameter(paramWestLon1);

            paramSouthLat2 = new Parameter("geo_lat2", -90.0f);
            paramSouthLat2.getProperties().setDescription("South bound latitude");
            paramSouthLat2.getProperties().setPhysicalUnit("°");
            paramSouthLat2.getProperties().setMinValue(-90.0f);
            paramSouthLat2.getProperties().setMaxValue(90.0f);
            pg.addParameter(paramSouthLat2);

            paramEastLon2 = new Parameter("geo_lon2", 180.0f);
            paramEastLon2.getProperties().setDescription("East bound longitude");
            paramEastLon2.getProperties().setPhysicalUnit("°");
            paramEastLon2.getProperties().setMinValue(-180.0f);
            paramEastLon2.getProperties().setMaxValue(180.0f);
            pg.addParameter(paramEastLon2);

            if (canUseGeoCoordinates(product)) {
                syncLatLonWithXYParams();
            }
        }

        private void addPixelParameter(ParamGroup pg) {
            int w = product.getSceneRasterWidth();
            int h = product.getSceneRasterHeight();

            int x1 = 0;
            int y1 = 0;
            int x2 = w - 1;
            int y2 = h - 1;
            int sx = 1;
            int sy = 1;

            if (givenProductSubsetDef != null) {
                Rectangle region = givenProductSubsetDef.getRegion();
                if (region != null) {
                    x1 = region.x;
                    y1 = region.y;
                    final int preX2 = x1 + region.width - 1;
                    if (preX2 < x2) {
                        x2 = preX2;
                    }
                    final int preY2 = y1 + region.height - 1;
                    if (preY2 < y2) {
                        y2 = preY2;
                    }
                }
                sx = givenProductSubsetDef.getSubSamplingX();
                sy = givenProductSubsetDef.getSubSamplingY();
            }

            final int wMin = MIN_SUBSET_SIZE;
            final int hMin = MIN_SUBSET_SIZE;

            paramX1 = new Parameter("source_x1", x1);
            paramX1.getProperties().setDescription("Start X co-ordinate given in pixels"); /*I18N*/
            paramX1.getProperties().setMinValue(0);
            paramX1.getProperties().setMaxValue((w - wMin - 1) > 0 ? w - wMin - 1 : 0);

            paramY1 = new Parameter("source_y1", y1);
            paramY1.getProperties().setDescription("Start Y co-ordinate given in pixels"); /*I18N*/
            paramY1.getProperties().setMinValue(0);
            paramY1.getProperties().setMaxValue((h - hMin - 1) > 0 ? h - hMin - 1 : 0);

            paramX2 = new Parameter("source_x2", x2);
            paramX2.getProperties().setDescription("End X co-ordinate given in pixels");/*I18N*/
            paramX2.getProperties().setMinValue(wMin - 1);
            final Integer maxValue = w - 1;
            paramX2.getProperties().setMaxValue(maxValue);

            paramY2 = new Parameter("source_y2", y2);
            paramY2.getProperties().setDescription("End Y co-ordinate given in pixels");/*I18N*/
            paramY2.getProperties().setMinValue(hMin - 1);
            paramY2.getProperties().setMaxValue(h - 1);

            paramSX = new Parameter("source_sx", sx);
            paramSX.getProperties().setDescription("Sub-sampling in X-direction given in pixels");/*I18N*/
            paramSX.getProperties().setMinValue(1);
            paramSX.getProperties().setMaxValue(w / wMin + 1);

            paramSY = new Parameter("source_sy", sy);
            paramSY.getProperties().setDescription("Sub-sampling in Y-direction given in pixels");/*I18N*/
            paramSY.getProperties().setMinValue(1);
            paramSY.getProperties().setMaxValue(h / hMin + 1);

            pg.addParameter(paramX1);
            pg.addParameter(paramY1);
            pg.addParameter(paramX2);
            pg.addParameter(paramY2);
            pg.addParameter(paramSX);
            pg.addParameter(paramSY);
        }

        private void updateUIState(ParamChangeEvent event) {
            if (updatingUI.compareAndSet(false, true)) {
                try {
                    if (event != null && canUseGeoCoordinates(product)) {
                        final String parmName = event.getParameter().getName();
                        if (parmName.startsWith("geo_")) {
                            final GeoPos geoPos1 = new GeoPos((Float) paramNorthLat1.getValue(),
                                                              (Float) paramWestLon1.getValue());
                            final GeoPos geoPos2 = new GeoPos((Float) paramSouthLat2.getValue(),
                                                              (Float) paramEastLon2.getValue());
                            updateXYParams(geoPos1, geoPos2);
                        } else if (parmName.startsWith("source_x") || parmName.startsWith("source_y")) {
                            syncLatLonWithXYParams();
                        }

                    }
                    int x1 = ((Number) paramX1.getValue()).intValue();
                    int y1 = ((Number) paramY1.getValue()).intValue();
                    int x2 = ((Number) paramX2.getValue()).intValue();
                    int y2 = ((Number) paramY2.getValue()).intValue();

                    int sx = ((Number) paramSX.getValue()).intValue();
                    int sy = ((Number) paramSY.getValue()).intValue();

                    updateSubsetDefRegion(x1, y1, x2, y2, sx, sy);

                    Dimension s = productSubsetDef.getSceneRasterSize(product.getSceneRasterWidth(),
                                                                      product.getSceneRasterHeight());
                    subsetWidthLabel.setText(String.valueOf(s.getWidth()));
                    subsetHeightLabel.setText(String.valueOf(s.getHeight()));

                    int sliderBoxX1 = x1 / thumbNailSubSampling;
                    int sliderBoxY1 = y1 / thumbNailSubSampling;
                    int sliderBoxX2 = x2 / thumbNailSubSampling;
                    int sliderBoxY2 = y2 / thumbNailSubSampling;
                    int sliderBoxW = sliderBoxX2 - sliderBoxX1 + 1;
                    int sliderBoxH = sliderBoxY2 - sliderBoxY1 + 1;
                    Rectangle box = getScaledRectangle(new Rectangle(sliderBoxX1, sliderBoxY1, sliderBoxW, sliderBoxH));
                    imageCanvas.setSliderBoxBounds(box);
                } finally {
                    updatingUI.set(false);
                }
            }
        }

        private void syncLatLonWithXYParams() {
            final PixelPos pixelPos1 = new PixelPos(((Number) paramX1.getValue()).intValue(),
                                                    ((Number) paramY1.getValue()).intValue());
            final PixelPos pixelPos2 = new PixelPos(((Number) paramX2.getValue()).intValue(),
                                                    ((Number) paramY2.getValue()).intValue());

            final GeoCoding geoCoding = product.getGeoCoding();
            final GeoPos geoPos1 = geoCoding.getGeoPos(pixelPos1, null);
            final GeoPos geoPos2 = geoCoding.getGeoPos(pixelPos2, null);
            paramNorthLat1.setValue(geoPos1.getLat(), null);
            paramWestLon1.setValue(geoPos1.getLon(), null);
            paramSouthLat2.setValue(geoPos2.getLat(), null);
            paramEastLon2.setValue(geoPos2.getLon(), null);
        }

        private void updateXYParams(GeoPos geoPos1, GeoPos geoPos2) {
            final GeoCoding geoCoding = product.getGeoCoding();
            final PixelPos pixelPos1 = geoCoding.getPixelPos(geoPos1, null);
            if (!pixelPos1.isValid()) {
                pixelPos1.setLocation(0, 0);
            }
            final PixelPos pixelPos2 = geoCoding.getPixelPos(geoPos2, null);
            if (!pixelPos2.isValid()) {
                pixelPos2.setLocation(product.getSceneRasterWidth(),
                                      product.getSceneRasterHeight());
            }
            final Rectangle.Float region = new Rectangle.Float();
            region.setFrameFromDiagonal(pixelPos1.x, pixelPos1.y, pixelPos2.x, pixelPos2.y);
            final Rectangle.Float productBounds = new Rectangle.Float(0, 0,
                                                                      product.getSceneRasterWidth(),
                                                                      product.getSceneRasterHeight());
            Rectangle2D finalRegion = productBounds.createIntersection(region);

            paramX1.setValue((int) finalRegion.getMinX(), null);
            paramY1.setValue((int) finalRegion.getMinY(), null);
            paramX2.setValue((int) finalRegion.getMaxX() - 1, null);
            paramY2.setValue((int) finalRegion.getMaxY() - 1, null);
        }

        private Dimension getScaledImageSize() {
            final int w = (product.getSceneRasterWidth() - 1) / thumbNailSubSampling + 1;
            final int h = (product.getSceneRasterHeight() - 1) / thumbNailSubSampling + 1;
            final Rectangle rectangle = new Rectangle(w, h);
            return getScaledRectangle(rectangle).getSize();
        }

        private Rectangle getScaledRectangle(Rectangle rectangle) {
            final AffineTransform i2mTransform = ImageManager.getImageToModelTransform(product.getGeoCoding());
            final double scaleX = i2mTransform.getScaleX();
            final double scaleY = i2mTransform.getScaleY();
            double scaleFactorY = Math.abs(scaleY / scaleX);
            final AffineTransform scaleTransform = AffineTransform.getScaleInstance(1.0, scaleFactorY);
            return scaleTransform.createTransformedShape(rectangle).getBounds();
        }

        private BufferedImage createThumbNailImage(Dimension imgSize, ProgressMonitor pm) {
            Assert.notNull(pm, "pm");

            String thumbNailBandName = getThumbnailBandName();
            Band thumbNailBand = product.getBand(thumbNailBandName);

            Debug.trace("ProductSubsetDialog: Reading thumbnail data for band '" + thumbNailBandName + "'...");
            pm.beginTask("Creating thumbnail image", 5);
            BufferedImage image = null;
            try {
                MultiLevelSource multiLevelSource = BandImageMultiLevelSource.create(thumbNailBand,
                                                                                     SubProgressMonitor.create(pm, 1));
                final ImageLayer imageLayer = new ImageLayer(multiLevelSource);
                final int imageWidth = imgSize.width;
                final int imageHeight = imgSize.height;
                final int imageType = BufferedImage.TYPE_3BYTE_BGR;
                image = new BufferedImage(imageWidth, imageHeight, imageType);
                Viewport snapshotVp = new DefaultViewport(isModelYAxisDown(imageLayer));
                final BufferedImageRendering imageRendering = new BufferedImageRendering(image, snapshotVp);

                final Graphics2D graphics = imageRendering.getGraphics();
                graphics.setColor(getBackground());
                graphics.fillRect(0, 0, imageWidth, imageHeight);

                snapshotVp.zoom(imageLayer.getModelBounds());
                snapshotVp.moveViewDelta(snapshotVp.getViewBounds().x, snapshotVp.getViewBounds().y);
                imageLayer.render(imageRendering);

                pm.worked(4);
            } finally {
                pm.done();
            }
            return image;
        }

        private boolean isModelYAxisDown(ImageLayer baseImageLayer) {
            return baseImageLayer.getImageToModelTransform().getDeterminant() > 0.0;
        }

        private String getThumbnailBandName() {
            return ProductUtils.findSuitableQuicklookBandName(product);
        }
    }

    private class ProductNodeSubsetPane extends JPanel {

        private Object[] productNodes;
        private String[] includeAlways;
        private List<JCheckBox> checkers;
        private JCheckBox allCheck;
        private JCheckBox noneCheck;
        private boolean selected;

        private ProductNodeSubsetPane(Object[] productNodes, boolean selected) {
            this(productNodes, null, selected);
        }

        private ProductNodeSubsetPane(Object[] productNodes, String[] includeAlways, boolean selected) {
            this.productNodes = productNodes;
            this.includeAlways = includeAlways;
            this.selected = selected;
            createUI();
        }

        private void createUI() {

            ActionListener productNodeCheckListener = new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    updateUIState();
                }
            };

            checkers = new ArrayList<JCheckBox>(10);
            JPanel checkersPane = GridBagUtils.createPanel();
            setComponentName(checkersPane, "CheckersPane");

            GridBagConstraints gbc = GridBagUtils.createConstraints("insets.left=4,anchor=WEST,fill=HORIZONTAL");
            for (int i = 0; i < productNodes.length; i++) {
                ProductNode productNode = (ProductNode) productNodes[i];

                String name = productNode.getName();
                JCheckBox productNodeCheck = new JCheckBox(name);
                productNodeCheck.setSelected(selected);
                productNodeCheck.setFont(SMALL_PLAIN_FONT);
                productNodeCheck.addActionListener(productNodeCheckListener);

                if (includeAlways != null
                    && StringUtils.containsIgnoreCase(includeAlways, name)) {
                    productNodeCheck.setSelected(true);
                    productNodeCheck.setEnabled(false);
                } else if (givenProductSubsetDef != null) {
                    productNodeCheck.setSelected(givenProductSubsetDef.containsNodeName(name));
                }
                checkers.add(productNodeCheck);

                String description = productNode.getDescription();
                JLabel productNodeLabel = new JLabel(description != null ? description : " ");
                productNodeLabel.setFont(SMALL_ITALIC_FONT);

                GridBagUtils.addToPanel(checkersPane, productNodeCheck, gbc, "weightx=0,gridx=0,gridy=" + i);
                GridBagUtils.addToPanel(checkersPane, productNodeLabel, gbc, "weightx=1,gridx=1,gridy=" + i);
            }
            // Add a last 'filler' row
            GridBagUtils.addToPanel(checkersPane, new JLabel(" "), gbc,
                                    "gridwidth=2,weightx=1,weighty=1,gridx=0,gridy=" + productNodes.length);

            ActionListener allCheckListener = new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    if (e.getSource() == allCheck) {
                        checkAllProductNodes(true);
                    } else if (e.getSource() == noneCheck) {
                        checkAllProductNodes(false);
                    }
                    updateUIState();
                }
            };

            allCheck = new JCheckBox("Select all");
            allCheck.setName("selectAll");
            allCheck.setMnemonic('a');
            allCheck.addActionListener(allCheckListener);

            noneCheck = new JCheckBox("Select none");
            noneCheck.setName("SelectNone");
            noneCheck.setMnemonic('n');
            noneCheck.addActionListener(allCheckListener);

            JScrollPane scrollPane = new JScrollPane(checkersPane);
            scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
            scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

            JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 4));
            buttonRow.add(allCheck);
            buttonRow.add(noneCheck);

            setLayout(new BorderLayout());
            add(scrollPane, BorderLayout.CENTER);
            add(buttonRow, BorderLayout.SOUTH);
            setBorder(BorderFactory.createEmptyBorder(7, 7, 7, 7));

            updateUIState();
        }

        void updateUIState() {
            allCheck.setSelected(areAllProductNodesChecked(true));
            noneCheck.setSelected(areAllProductNodesChecked(false));
            updateSubsetDefNodeNameList();
        }

        String[] getSubsetNames() {
            String[] names = new String[countChecked(true)];
            int pos = 0;
            for (int i = 0; i < checkers.size(); i++) {
                JCheckBox checker = checkers.get(i);
                if (checker.isSelected()) {
                    ProductNode productNode = (ProductNode) productNodes[i];
                    names[pos] = productNode.getName();
                    pos++;
                }
            }
            return names;
        }

        void checkAllProductNodes(boolean checked) {
            for (JCheckBox checker : checkers) {
                if (checker.isEnabled()) {
                    checker.setSelected(checked);
                }
            }
        }

        boolean areAllProductNodesChecked(boolean checked) {
            return countChecked(checked) == checkers.size();
        }

        int countChecked(boolean checked) {
            int counter = 0;
            for (JCheckBox checker : checkers) {
                if (checker.isSelected() == checked) {
                    counter++;
                }
            }
            return counter;
        }
    }
}
