package org.esa.beam.visat.toolviews.roi;

import com.bc.layer.LayerModel;
import com.bc.layer.LayerModelChangeAdapter;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.draw.Figure;
import org.esa.beam.framework.draw.ShapeFigure;
import org.esa.beam.framework.help.HelpSys;
import org.esa.beam.framework.param.ParamChangeEvent;
import org.esa.beam.framework.param.ParamChangeListener;
import org.esa.beam.framework.param.ParamException;
import org.esa.beam.framework.param.ParamExceptionHandler;
import org.esa.beam.framework.param.ParamGroup;
import org.esa.beam.framework.param.Parameter;
import org.esa.beam.framework.param.editors.BooleanExpressionEditor;
import org.esa.beam.framework.param.editors.ComboBoxEditor;
import org.esa.beam.framework.param.validators.BooleanExpressionValidator;
import org.esa.beam.framework.ui.GridBagUtils;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.application.support.AbstractToolView;
import org.esa.beam.framework.ui.command.Command;
import org.esa.beam.framework.ui.command.CommandManager;
import org.esa.beam.framework.ui.command.ToolCommand;
import org.esa.beam.framework.ui.product.BandChooser;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.framework.ui.tool.ToolButtonFactory;
import org.esa.beam.util.Debug;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.util.PropertyMap;
import org.esa.beam.util.StringUtils;
import org.esa.beam.util.SystemUtils;
import org.esa.beam.util.XmlWriter;
import org.esa.beam.util.io.BeamFileChooser;
import org.esa.beam.util.io.BeamFileFilter;
import org.esa.beam.util.io.FileUtils;
import org.esa.beam.visat.VisatApp;
import org.esa.beam.visat.dialogs.ProductChooser;
import org.jdom.Document;
import org.jdom.input.DOMBuilder;
import org.xml.sax.SAXException;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.GeneralPath;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RoiManagerToolView extends AbstractToolView implements ParamExceptionHandler {

    public static final String ID = RoiManagerToolView.class.getName();

    private static final String _FILE_EXTENSION = ".roi";

    private VisatApp visatApp;
    private PropertyMap propertyMap;
    private final ParamGroup paramGroup;
    private AbstractButton applyButton;
    private AbstractButton resetButton;
    private AbstractButton undoButton;
    private AbstractButton multiAssignToBandsButton;
    private AbstractButton multiAssignToProductsButton;
    private AbstractButton importButton;
    private AbstractButton exportButton;
    private AbstractButton zoomToButton;
    private ProductSceneView productSceneView;
    private ROIDefinition roiDefinitionUndo;

    private Parameter operatorParam;
    private Parameter invertParam;
    private Parameter valueRangeEnabledParam;
    private Parameter valueRangeMinParam;
    private Parameter valueRangeMaxParam;
    private Parameter shapesEnabledParam;
    private Parameter bitmaskEnabledParam;
    private Parameter bitmaskExprParam;
    private Parameter pinsEnabledParam;

    private final Command[] shapeCommands;
    private JPanel shapeToolsRow;
    private final String[] operatorValueSet = new String[]{"OR", "AND"};
    private BeamFileFilter roiDefinitionFileFilter;
    // TODO -  (nf) replace bandsToBeModified with a String (name) array, memory leaks occur here!!!
    private Band[] bandsToBeModified;
    private LayerModelChangeHandler _layerModelChangeHandler;
    private Figure shapeFigure;
    private final ProductNodeListener _productNodeListener;

    private ProductNodeListener roiDefinitionPNL;
    private final JPanel roiDefPane;


    public RoiManagerToolView() {
        visatApp = VisatApp.getApp();
        CommandManager commandManager = visatApp.getCommandManager();
        ToolCommand drawLineTool = commandManager.getToolCommand("drawLineTool");
        ToolCommand drawRectangleTool = commandManager.getToolCommand("drawRectangleTool");
        ToolCommand drawEllipseTool = commandManager.getToolCommand("drawEllipseTool");
        ToolCommand drawPolylineTool = commandManager.getToolCommand("drawPolylineTool");
        ToolCommand drawPolygonTool = commandManager.getToolCommand("drawPolygonTool");

        paramGroup = new ParamGroup();
        shapeCommands = new Command[]{
                drawLineTool,
                drawPolylineTool,
                drawRectangleTool,
                drawEllipseTool,
                drawPolygonTool,
        };
        propertyMap = visatApp.getPreferences();
        _layerModelChangeHandler = new LayerModelChangeHandler();
        _productNodeListener = createProductNodeListener();
        bandsToBeModified = new Band[0];
        initParams();
        roiDefPane = createRoiDefinitionPane();

        VisatApp.getApp().getProductManager().addListener(new ROIDefinitionPML());

        // Add an internal frame listener to VISAT so that we can update our
        // contrast stretch dialog with the information of the currently activated
        // product scene view.
        //
        VisatApp.getApp().addInternalFrameListener(new ROIDefinitionIFL());

        /**
         * This listener is required to repaint the product scene view if the ROI definition was changed
         * per assign ROI to other products.
         */
        roiDefinitionPNL = new ROIDefinitionPNL();
        Product[] products = VisatApp.getApp().getProductManager().getProducts();
        for (Product product : products) {
            product.addProductNodeListener(roiDefinitionPNL);
        }

        paramGroup.addParamChangeListener(new ParamChangeListener() {
            public void parameterValueChanged(ParamChangeEvent event) {
                setApplyEnabled(true);
                updateUIState();
            }
        });

    }

    public void setProductSceneView(ProductSceneView productSceneView) {
        if (this.productSceneView == productSceneView) {
            return;
        }
        if (this.productSceneView != null) {
            this.productSceneView.getImageDisplay().getLayerModel().removeLayerModelChangeListener(
                    _layerModelChangeHandler);
            this.productSceneView.getProduct().removeProductNodeListener(_productNodeListener);
        }

        this.productSceneView = productSceneView;
        roiDefinitionUndo = null;

        if (this.productSceneView != null) {
            shapeFigure = this.productSceneView.getRasterROIShapeFigure();
            if (shapeFigure == null) {
                shapeFigure = this.productSceneView.getCurrentShapeFigure();
            }
            this.productSceneView.getImageDisplay().getLayerModel().addLayerModelChangeListener(_layerModelChangeHandler);
            this.productSceneView.getProduct().addProductNodeListener(_productNodeListener);
            roiDefinitionUndo = getCurrentROIDefinition();
            setUIParameterValues(roiDefinitionUndo);
            resetBitmaskFlagNames();
            bitmaskExprParam.getProperties().setPropertyValue(BooleanExpressionEditor.PROPERTY_KEY_SELECTED_PRODUCT,
                                                              this.productSceneView.getProduct());
        } else {
            bitmaskExprParam.getProperties().setPropertyValue(BooleanExpressionEditor.PROPERTY_KEY_SELECTED_PRODUCT,
                                                              null);
        }
        updateUIState();
        updateTitle();

        setApplyEnabled(false);
        resetButton.setEnabled(false);
        VisatApp.getApp().updateROIImage(productSceneView, true);
    }

    private void updateTitle() {
        if (productSceneView != null) {
            setTitle(
                    MessageFormat.format("{0} - {1}", getDescriptor().getTitle(), getCurrentRaster().getDisplayName()));
        } else {
            setTitle(getDescriptor().getTitle());
        }
    }

    private void initParams() {

        shapesEnabledParam = new Parameter("roi.shapesEnabled", Boolean.TRUE);
        shapesEnabledParam.getProperties().setLabel("Include pixels in geometric shape"); /*I18N*/
        shapesEnabledParam.getProperties().setDescription(
                "Select pixels within boundary given by geometric shape");/*I18N*/
        paramGroup.addParameter(shapesEnabledParam);

        valueRangeEnabledParam = new Parameter("roi.valueRangeEnabled", Boolean.FALSE);
        valueRangeEnabledParam.getProperties().setLabel("Include pixels in value range"); /*I18N*/
        valueRangeEnabledParam.getProperties().setDescription("Select pixels in a given value range");/*I18N*/
        paramGroup.addParameter(valueRangeEnabledParam);

        valueRangeMinParam = new Parameter("roi.valueRangeMin", new Float(0.0));
        valueRangeMinParam.getProperties().setLabel("Min:"); /*I18N*/
        valueRangeMinParam.getProperties().setNumCols(6);
        valueRangeMinParam.getProperties().setDescription("Minimum sample value");/*I18N*/
        paramGroup.addParameter(valueRangeMinParam);

        valueRangeMaxParam = new Parameter("roi.valueRangeMax", new Float(100.0));
        valueRangeMaxParam.getProperties().setLabel("Max:"); /*I18N*/
        valueRangeMaxParam.getProperties().setNumCols(6);
        valueRangeMaxParam.getProperties().setDescription("Maximum sample value");/*I18N*/
        paramGroup.addParameter(valueRangeMaxParam);

        bitmaskEnabledParam = new Parameter("roi.bitmaskEnabled", Boolean.FALSE);
        bitmaskEnabledParam.getProperties().setLabel("Include pixels by condition"); /*I18N*/
        bitmaskEnabledParam.getProperties().setDescription(
                "Include pixels for which a given conditional expression is true"); /*I18N*/
        paramGroup.addParameter(bitmaskEnabledParam);

        bitmaskExprParam = new Parameter("roi.bitmaskExpr", "");
        bitmaskExprParam.getProperties().setNullValueAllowed(true);
        bitmaskExprParam.getProperties().setEditorClass(BooleanExpressionEditor.class);
        bitmaskExprParam.getProperties().setValidatorClass(BooleanExpressionValidator.class);
        bitmaskExprParam.getProperties().setPropertyValue(BooleanExpressionEditor.PROPERTY_KEY_PREFERENCES,
                                                          propertyMap);
        bitmaskExprParam.getProperties().setLabel("Conditional expression:"); /*I18N*/
        bitmaskExprParam.getProperties().setDescription("The condition  al expression"); /*I18N*/
        paramGroup.addParameter(bitmaskExprParam);

        pinsEnabledParam = new Parameter("roi.pinsEnabled", Boolean.FALSE);
        pinsEnabledParam.getProperties().setLabel("Include pixels under pins"); /*I18N*/
        pinsEnabledParam.getProperties().setDescription("Include pixels under pins"); /*I18N*/
        paramGroup.addParameter(pinsEnabledParam);

        operatorParam = new Parameter("roi.operator", "OR");
        operatorParam.getProperties().setEditorClass(ComboBoxEditor.class);
        operatorParam.getProperties().setLabel("Combine criteria with: "); /*I18N*/
        operatorParam.getProperties().setDescription("Specify the criteria combination operator"); /*I18N*/
        operatorParam.getProperties().setValueSet(operatorValueSet);
        operatorParam.getProperties().setValueSetBound(true);
        paramGroup.addParameter(operatorParam);

        invertParam = new Parameter("roi.invert", Boolean.FALSE);
        invertParam.getProperties().setLabel("Invert"); /*I18N*/
        invertParam.getProperties().setDescription("Select to invert the specified ROI (NOT operator)"); /*I18N*/
        paramGroup.addParameter(invertParam);
    }

    @Override
    public JComponent createControl() {

        final AbstractButton helpButton = createButton("icons/Help24.gif");
        helpButton.setName("HelpButton");

        final JPanel buttonPane = GridBagUtils.createPanel();
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridwidth = 2;
        gbc.weightx = 0.5;
        gbc.insets.bottom = 4;
        buttonPane.add(applyButton, gbc);
        gbc.insets.bottom = 0;
        gbc.gridwidth = 1;
        gbc.gridy = 2;
        buttonPane.add(undoButton, gbc);
        gbc.gridy++;
        buttonPane.add(multiAssignToBandsButton, gbc);
        buttonPane.add(multiAssignToProductsButton, gbc);
        gbc.gridy++;
        buttonPane.add(importButton, gbc);
        buttonPane.add(exportButton, gbc);
        gbc.gridy++;
        buttonPane.add(zoomToButton, gbc);
        gbc.gridy++;
        gbc.fill = GridBagConstraints.VERTICAL;
        gbc.weighty = 1.0;
        gbc.gridwidth = 2;
        buttonPane.add(new JLabel(" "), gbc); // filler
        gbc.fill = GridBagConstraints.NONE;
        gbc.weighty = 0.0;
        gbc.gridx = 1;
        gbc.gridy++;
        gbc.gridwidth = 1;
        buttonPane.add(helpButton, gbc);

        final JPanel mainPane = new JPanel(new BorderLayout(4, 4));
        mainPane.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        mainPane.add(BorderLayout.CENTER, roiDefPane);
        mainPane.add(BorderLayout.EAST, buttonPane);

        if (getDescriptor().getHelpId() != null) {
            HelpSys.enableHelpOnButton(helpButton, getDescriptor().getHelpId());
            HelpSys.enableHelpKey(mainPane, getDescriptor().getHelpId());
        }

        updateUIState();
        setProductSceneView(VisatApp.getApp().getSelectedProductSceneView());
        return mainPane;
    }

    private JPanel createRoiDefinitionPane() {
        shapeToolsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 1, 1));
        if (shapeCommands != null) {
            for (final Command command : shapeCommands) {
                if (command != null) {
                    shapeToolsRow.add(command.createToolBarButton());
                } else {
                    shapeToolsRow.add(new JLabel("  "));
                }
            }
        }

        final JPanel roiDefPane = createROIDefPane();

        applyButton = new JButton("Apply");
        applyButton.setToolTipText("Assign ROI to selected band"); /*I18N*/
        applyButton.setMnemonic('A');
        applyButton.setName("ApplyButton");
        applyButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                apply();
            }
        });

        undoButton = createButton("icons/Undo24.gif");
        undoButton.setToolTipText("Undo ROI assignment"); /*I18N*/
        undoButton.setName("UndoButton");
        undoButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                undo();
            }
        });

        multiAssignToBandsButton = createButton("icons/MultiAssignBands24.gif");
        multiAssignToBandsButton.setToolTipText("Apply to other bands"); /*I18N*/
        multiAssignToBandsButton.setName("AssignToBandsButton");
        multiAssignToBandsButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                multiAssignToBands();
            }
        });

        multiAssignToProductsButton = createButton("icons/MultiAssignProducts24.gif");
        multiAssignToProductsButton.setToolTipText("Apply to other products"); /*I18N*/
        multiAssignToProductsButton.setName("MultiAssignButton");
        multiAssignToProductsButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                multiAssignToProducts();
            }
        });

        resetButton = createButton("icons/Undo24.gif");
        resetButton.setToolTipText("Reset ROI to default values"); /*I18N*/
        resetButton.setName("ResetButton");
        resetButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                reset();
            }
        });

        importButton = createButton("icons/Import24.gif");
        importButton.setToolTipText("Import ROI from text file."); /*I18N*/
        importButton.setName("ImportButton");
        importButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                importROIDef();
            }
        });
        importButton.setEnabled(true);

        exportButton = createButton("icons/Export24.gif");
        exportButton.setToolTipText("Export ROI to text file."); /*I18N*/
        exportButton.setName("ExportButton");
        exportButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                exportROIDef();
            }
        });
        exportButton.setEnabled(true);

        zoomToButton = createButton("icons/ZoomTo24.gif");
        zoomToButton.setToolTipText("Zoom to ROI."); /*I18N*/
        zoomToButton.setName("ZoomButton");
        zoomToButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                zoomToROI();
            }
        });
        zoomToButton.setEnabled(false);
        return roiDefPane;
    }

    private void multiAssignToBands() {
        final RasterDataNode[] protectedRasters = productSceneView.getRasters();
        Band[] availableBands = productSceneView.getProduct().getBands();
        final List<Band> availableBandList = new ArrayList<Band>(availableBands.length);
        for (final Band availableBand : availableBands) {
            boolean validBand = true;
            for (RasterDataNode protectedRaster : protectedRasters) {
                if (availableBand == protectedRaster) {
                    validBand = false;
                }
            }
            if (validBand) {
                availableBandList.add(availableBand);
            }
        }
        availableBands = new Band[availableBandList.size()];
        availableBandList.toArray(availableBands);
        availableBandList.clear();

        if (availableBands.length == 0) {
            JOptionPane.showMessageDialog(getPaneControl(), "No other bands available.",
                                          getDescriptor().getTitle(), JOptionPane.ERROR_MESSAGE);
            return;
        }
        final BandChooser bandChooser = new BandChooser(getPaneWindow(),
                                                        "Apply to other Bands", getDescriptor().getHelpId(),
                                                        availableBands,
                                                        bandsToBeModified);
        if (bandChooser.show() == BandChooser.ID_OK) {
            final ROIDefinition roiDefinition = getCurrentROIDefinition();
            bandsToBeModified = bandChooser.getSelectedBands();
            for (final Band band : bandsToBeModified) {
                band.setROIDefinition(roiDefinition.createCopy());
            }
        }
    }

    private void multiAssignToProducts() {
        final ProductSceneView selectedProductSceneView = visatApp.getSelectedProductSceneView();
        final RasterDataNode currentRaster = selectedProductSceneView.getRaster();
        final Product currentProduct = currentRaster.getProduct();

        final String title = "Transfer ROI";   /*I18N*/
        final Product[] products = visatApp.getProductManager().getProducts();
        final Product[] allProducts = extractAllProducts(products, currentProduct);

        final ProductChooser productChooser = new ProductChooser(getPaneWindow(), title, null, allProducts, null,
                                                                 currentRaster.getName());
        final int result = productChooser.show();
        final Product[] selectedProducts;
        if (result == ProductChooser.ID_OK) {
            selectedProducts = productChooser.getSelectedProducts();
        } else {
            return;
        }

        final ROIDefinition currentRoiDefinition = currentRaster.getROIDefinition();
        if (currentRoiDefinition == null) {
            visatApp.showErrorDialog(
                    MessageFormat.format("No ROI defined for ''{0}''", currentRaster.getDisplayName())); /*I18N*/
            return;
        }

        GeneralPath geoPath = null;
        for (final Product selectedProduct : selectedProducts) {
            GeneralPath pixelPath = null;
            if (!currentProduct.isCompatibleProduct(selectedProduct, 1.0f / (60.0f * 60.0f))) {
                final GeoCoding selectedGeoCoding = selectedProduct.getGeoCoding();
                if (selectedGeoCoding != null) {
                    if (geoPath == null) {
                        geoPath = transformROIShape(currentProduct, currentRoiDefinition);
                    }
                    if (geoPath != null) {
                        pixelPath = ProductUtils.convertToPixelPath(geoPath, selectedGeoCoding);
                    }
                }
            }

            final Band[] bands;
            if (productChooser.isTransferToAllBands()) {
                bands = selectedProduct.getBands();
            } else {
                bands = new Band[]{selectedProduct.getBand(currentRaster.getName())};
            }

            for (final Band band : bands) {
                if (band == null) {
                    continue;
                }
                final ROIDefinition newRoiDefinition = currentRoiDefinition.createCopy();
                if (currentRoiDefinition.isPinUseEnabled() && selectedProduct.getPinGroup().getNodeCount() == 0) {
                    newRoiDefinition.setPinUseEnabled(false);
                }

                if (pixelPath != null) {
                    final Figure shapeFigure = currentRoiDefinition.getShapeFigure();
                    final boolean dimensional = shapeFigure.isOneDimensional();
                    final Map attributes = shapeFigure.getAttributes();
                    newRoiDefinition.setShapeFigure(new ShapeFigure(pixelPath, dimensional, attributes));
                }
                band.setROIDefinition(newRoiDefinition);
            }
        }
    }

    private static GeneralPath transformROIShape(Product currentProduct, ROIDefinition currentRoiDefinition) {
        GeneralPath geoPath = null;
        final GeoCoding currentGeoCoding = currentProduct.getGeoCoding();
        final Figure figure = currentRoiDefinition.getShapeFigure();
        if (currentGeoCoding != null && figure != null) {
            final Shape shape = figure.getShape();
            if (shape != null) {
                geoPath = ProductUtils.convertToGeoPath(shape, currentGeoCoding);
            }
        }
        return geoPath;
    }

    private static Product[] extractAllProducts(Product[] products, Product currentProduct) {
        final List<Product> allProducts = new ArrayList<Product>(products.length);
        for (final Product product : products) {
            if (product != currentProduct) {
                allProducts.add(product);
            }
        }
        return allProducts.toArray(new Product[allProducts.size()]);
    }

    private void importROIDef() {
        final BeamFileChooser fileChooser = new BeamFileChooser();
        fileChooser.setDialogTitle("Import ROI Definition");
        fileChooser.setFileFilter(getOrCreateRoiDefinitionFileFilter());
        fileChooser.setCurrentDirectory(getIODir());
        final int result = fileChooser.showOpenDialog(getPaneControl());
        if (result == JFileChooser.APPROVE_OPTION) {
            final File file = fileChooser.getSelectedFile();
            if (file != null) {
                setIODir(file.getAbsoluteFile().getParentFile());
                try {
                    final ROIDefinition roiDefinition = createROIFromFile(file);
                    setUIParameterValues(roiDefinition);
                    applyImpl(roiDefinition);
                } catch (IOException e) {
                    showErrorDialog("I/O Error.\nFailed to import ROI definition.");
                }
            }
        }
    }

    private void exportROIDef() {
        final ROIDefinition roiDefinition = createROIDefinition();
        if (roiDefinition != null) {
            final BeamFileChooser fileChooser = new BeamFileChooser();
            fileChooser.setDialogTitle("Export ROI Definition");
            fileChooser.setFileFilter(getOrCreateRoiDefinitionFileFilter());/*I18N*/
            fileChooser.setCurrentDirectory(getIODir());
            fileChooser.setSelectedFile(new File(getIODir(), "ROI"));
            final int result = fileChooser.showSaveDialog(getPaneControl());
            if (result == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                if (file != null) {
                    if (!visatApp.promptForOverwrite(file)) {
                        return;
                    }
                    setIODir(file.getAbsoluteFile().getParentFile());
                    file = FileUtils.ensureExtension(file, _FILE_EXTENSION);
                    try {
                        writeROIToFile(roiDefinition, file);
                    } catch (IOException e) {
                        showErrorDialog("I/O Error.\n   Failed to export ROI definition.");
                    }
                }
            }
        } else {
            showErrorDialog("No ROI defined");
        }
    }

    private void zoomToROI() {
        if (productSceneView == null) {
            return;
        }
        final RenderedImage roiImage = productSceneView.getROIImage();
        final Raster data = roiImage.getData();
        final int width = roiImage.getWidth();
        final int height = roiImage.getHeight();
        int minX = width;
        int maxX = 0;
        int minY = height;
        int maxY = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (data.getSample(x, y, 0) != 0) {
                    minX = Math.min(x, minX);
                    maxX = Math.max(x, maxX);
                    minY = Math.min(y, minY);
                    maxY = Math.max(y, maxY);
                }
            }
        }
        final Rectangle rect = new Rectangle(minX, minY, maxX - minX + 1, maxY - minY + 1);
        if (rect.isEmpty()) {
            return;
        }
        rect.grow(50, 50);
        productSceneView.getImageDisplay().zoom(rect);
    }

    private BeamFileFilter getOrCreateRoiDefinitionFileFilter() {
        if (roiDefinitionFileFilter == null) {
            final String formatName = "ROI_DEFINITION_FILE";
            final String description = MessageFormat.format("ROI definition files (*{0})", _FILE_EXTENSION);
            roiDefinitionFileFilter = new BeamFileFilter(formatName, _FILE_EXTENSION, description);
        }
        return roiDefinitionFileFilter;
    }

    private static void writeROIToFile(ROIDefinition roi, File outputFile) throws IOException {
        Guardian.assertNotNull("roi", roi);
        Guardian.assertNotNull("outputFile", outputFile);

        XmlWriter writer = new XmlWriter(outputFile);
        roi.writeXML(writer, 0);
        writer.close();
    }

    private static ROIDefinition createROIFromFile(File inputFile) throws IOException {
        Guardian.assertNotNull("inputFile", inputFile);

        if (inputFile.canRead()) {
            try {
                final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                final DocumentBuilder builder = factory.newDocumentBuilder();
                final org.w3c.dom.Document w3cDocument = builder.parse(inputFile);
                final Document document = new DOMBuilder().build(w3cDocument);
                final ROIDefinition roiDefinition = new ROIDefinition();
                roiDefinition.initFromJDOMElem(document.getRootElement());
                return roiDefinition;
            } catch (FactoryConfigurationError error) {
                throw new IOException(error.toString());
            } catch (ParserConfigurationException e) {
                throw new IOException(e.toString());
            } catch (SAXException e) {
                throw new IOException(e.toString());
            } catch (IOException e) {
                throw new IOException(e.toString());
            }
        }
        return null;
    }

    private void showErrorDialog(String message) {
        JOptionPane.showMessageDialog(getPaneControl(),
                                      message,
                                      getDescriptor().getTitle() + " - Error",
                                      JOptionPane.ERROR_MESSAGE);
    }


    private void setApplyEnabled(boolean enabled) {
        final boolean canApply = productSceneView != null;
        applyButton.setEnabled(canApply && enabled);
        multiAssignToBandsButton.setEnabled(canApply && !enabled);
    }

    private void setIODir(File dir) {
        if (propertyMap != null && dir != null) {
            propertyMap.setPropertyString("roi.io.dir", dir.getPath());
        }
    }

    private File getIODir() {
        File dir = SystemUtils.getUserHomeDir();
        if (propertyMap != null) {
            dir = new File(propertyMap.getPropertyString("roi.io.dir", dir.getPath()));
        }
        return dir;
    }

    private void apply() {
        applyImpl(createROIDefinition());
    }

    private void reset() {
        resetImpl(new ROIDefinition());
    }

    private void undo() {
        if (roiDefinitionUndo != null) {
            resetImpl(roiDefinitionUndo);
        } else {
            reset();
        }
    }

    private void applyImpl(ROIDefinition roiDefNew) {
        if (productSceneView == null) {
            return;
        }

        final ROIDefinition roiDefOld = getCurrentROIDefinition();
        if (roiDefOld == roiDefNew) {
            return;
        }

        final String warningMessage = checkApplicability(roiDefNew);
        if (warningMessage != null) {
            visatApp.showWarningDialog(warningMessage);
            return;
        }

        setApplyEnabled(false);
        resetButton.setEnabled(false);

        productSceneView.setROIOverlayEnabled(true);
        setCurrentROIDefinition(roiDefNew);
    }

    private void resetImpl(ROIDefinition roiDefNew) {
        setUIParameterValues(roiDefNew);
        applyImpl(roiDefNew);
        updateUIState();
    }

    private JPanel createROIDefPane() {
        final JPanel pane = GridBagUtils.createPanel();
        final GridBagConstraints gbc = GridBagUtils.createConstraints("anchor=NORTHWEST,fill=HORIZONTAL");

        ////////////////////////////////////////////////////////////////////////
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 0;
        gbc.insets.top = 0;
        gbc.insets.left = 0;
        pane.add(shapesEnabledParam.getEditor().getComponent(), gbc);

        gbc.gridy++;
        gbc.gridwidth = 1;
        gbc.weightx = 1;
        gbc.insets.top = 0;
        gbc.insets.left = 20;
        pane.add(createShapesPane(), gbc);

        ////////////////////////////////////////////////////////////////////////
        gbc.gridy++;
        gbc.gridwidth = 2;
        gbc.weightx = 0;
        gbc.insets.top = 8;
        gbc.insets.left = 0;
        pane.add(valueRangeEnabledParam.getEditor().getComponent(), gbc);

        gbc.gridy++;
        gbc.gridwidth = 1;
        gbc.weightx = 1;
        gbc.insets.top = 0;
        gbc.insets.left = 20;
        pane.add(createValueRangePane(), gbc);

        ////////////////////////////////////////////////////////////////////////
        gbc.gridy++;
        gbc.gridwidth = 2;
        gbc.weightx = 0;
        gbc.insets.top = 8;
        gbc.insets.left = 0;
        pane.add(bitmaskEnabledParam.getEditor().getComponent(), gbc);

        gbc.gridy++;
        gbc.gridwidth = 1;
        gbc.weightx = 1;
        gbc.insets.top = 0;
        gbc.insets.left = 20;
        pane.add(createBitmaskPane(), gbc);

        ////////////////////////////////////////////////////////////////////////

        gbc.gridy++;
        gbc.gridwidth = 2;
        gbc.weightx = 0;
        gbc.insets.top = 8;
        gbc.insets.left = 0;
        pane.add(pinsEnabledParam.getEditor().getComponent(), gbc);

        ////////////////////////////////////////////////////////////////////////

        gbc.gridy++;
        gbc.gridwidth = 1;
        gbc.weightx = 1;
        gbc.insets.top = 12;
        gbc.insets.left = 0;
        pane.add(createOperatorPane(), gbc);

        return pane;
    }

    private JComponent createShapesPane() {
        return shapeToolsRow;
    }

    private JComponent createValueRangePane() {
        final JPanel pane = GridBagUtils.createPanel();
        final GridBagConstraints gbc = GridBagUtils.createConstraints("anchor=WEST,fill=HORIZONTAL");
        GridBagUtils.addToPanel(pane, valueRangeMinParam.getEditor().getLabelComponent(), gbc,
                                "insets.left=0,weightx=0");
        GridBagUtils.addToPanel(pane, valueRangeMinParam.getEditor().getComponent(), gbc, "insets.left=3,weightx=0.1");
        GridBagUtils.addToPanel(pane, new JLabel("  "), gbc, "insets.left=0,weightx=0.8");
        GridBagUtils.addToPanel(pane, valueRangeMaxParam.getEditor().getLabelComponent(), gbc,
                                "insets.left=0,weightx=0");
        GridBagUtils.addToPanel(pane, valueRangeMaxParam.getEditor().getComponent(), gbc, "insets.left=3,weightx=0.1");
        return pane;
    }

    private JComponent createBitmaskPane() {
        return bitmaskExprParam.getEditor().getComponent();
    }

    private JPanel createOperatorPane() {
        final JPanel pane = GridBagUtils.createPanel();
        final GridBagConstraints gbc = GridBagUtils.createConstraints("anchor=WEST,fill=HORIZONTAL");
        GridBagUtils.addToPanel(pane, operatorParam.getEditor().getLabelComponent(), gbc,
                                "gridx=0,gridy=0,gridwidth=1");
        GridBagUtils.addToPanel(pane, operatorParam.getEditor().getComponent(), gbc, "gridx=1,weightx=1");
        GridBagUtils.addToPanel(pane, invertParam.getEditor().getComponent(), gbc, "gridx=2,insets.left=7");
        return pane;
    }

    void updateUIState() {

        // Get boolean switches
        //
        final boolean roiPossible = productSceneView != null; //&& ProductData.isFloatingPointType(productSceneView.getRaster().getDataType());
        final boolean valueRangeCritSet = (Boolean) valueRangeEnabledParam.getValue();
        final boolean shapesCritPossible = shapeFigure != null;
        final boolean zoomToPossible = productSceneView != null && productSceneView.getROIImage() != null;
        final boolean shapesCritSet = (Boolean) shapesEnabledParam.getValue();
        boolean bitmaskCritPossible = false;
        boolean bitmaskCritSet = (Boolean) bitmaskEnabledParam.getValue();
        final boolean pinCritPossible = getProduct() != null && getProduct().getPinGroup().getNodeCount() > 0;
        boolean pinCritSet = (Boolean) pinsEnabledParam.getValue();

        if (roiPossible) {
            final Product product = getProduct();
            if (product != null) {
                final boolean hasFlagCoding = product.getFlagCodingGroup().getNodeCount() > 0;
                final boolean hasBands = product.getNumBands() > 0;
                final int hasTiePointGrids = product.getNumTiePointGrids();
                if (hasFlagCoding || hasBands || hasTiePointGrids > 0) {
                    bitmaskCritPossible = true;
                }
            }
        }

        if (!bitmaskCritPossible) {
            bitmaskEnabledParam.setValue(Boolean.FALSE, this);
            bitmaskCritSet = false;
        }

        if (!pinCritSet) {
            pinsEnabledParam.setValue(Boolean.FALSE, this);
            pinCritSet = false;
        }

        int numCriteria = 0;
        if (valueRangeCritSet) {
            numCriteria++;
        }
        if (shapesCritSet) {
            numCriteria++;
        }
        if (bitmaskCritSet) {
            numCriteria++;
        }
        if (pinCritSet) {
            numCriteria++;
        }

        // Enable/Disable UI
        //
        operatorParam.setUIEnabled(roiPossible && numCriteria > 1);
        invertParam.setUIEnabled(roiPossible && numCriteria > 0);

        shapesEnabledParam.setUIEnabled(roiPossible && shapesCritPossible);
        valueRangeEnabledParam.setUIEnabled(roiPossible);
        valueRangeMinParam.setUIEnabled(roiPossible && valueRangeCritSet);
        valueRangeMaxParam.setUIEnabled(roiPossible && valueRangeCritSet);
        bitmaskEnabledParam.setUIEnabled(roiPossible && bitmaskCritPossible);
        bitmaskExprParam.setUIEnabled(roiPossible && bitmaskCritPossible && bitmaskCritSet);
        pinsEnabledParam.setUIEnabled(roiPossible && pinCritPossible);
        exportButton.setEnabled(roiPossible);
        importButton.setEnabled(roiPossible);
        if (roiPossible) {
            undoButton.setEnabled(roiDefinitionUndo != null);
            multiAssignToProductsButton.setEnabled(visatApp != null
                                                    && visatApp.getProductManager().getProductCount() > 1);
        } else {
            applyButton.setEnabled(false);
            multiAssignToBandsButton.setEnabled(false);
            multiAssignToProductsButton.setEnabled(false);
            resetButton.setEnabled(false);
            undoButton.setEnabled(false);
        }
        if (!shapesCritPossible) {
            shapesEnabledParam.setValue(Boolean.FALSE, null);
        }

        zoomToButton.setEnabled(zoomToPossible);
    }

    private ROIDefinition getCurrentROIDefinition() {
        return getCurrentRaster().getROIDefinition();
    }

    private void setCurrentROIDefinition(ROIDefinition roiDefinition) {
        getCurrentRaster().setROIDefinition(roiDefinition);
    }

    private RasterDataNode getCurrentRaster() {
        return productSceneView.getRaster();
    }

    public void setUIParameterValues(ROIDefinition roiDefinition) {

        if (roiDefinition != null) {
            shapeFigure = roiDefinition.getShapeFigure();
            shapesEnabledParam.setValue(roiDefinition.isShapeEnabled(), null);
            valueRangeEnabledParam.setValue(roiDefinition.isValueRangeEnabled(), null);
            valueRangeMinParam.setValue(roiDefinition.getValueRangeMin(), null);
            valueRangeMaxParam.setValue(roiDefinition.getValueRangeMax(), null);
            bitmaskEnabledParam.setValue(roiDefinition.isBitmaskEnabled(), null);
            bitmaskExprParam.setValue(roiDefinition.getBitmaskExpr(), null);
            pinsEnabledParam.setValue(roiDefinition.isPinUseEnabled(), null);
            operatorParam.setValue(roiDefinition.isOrCombined() ? operatorValueSet[0] : operatorValueSet[1], null);
            invertParam.setValue(roiDefinition.isInverted(), null);
        } else {
            shapeFigure = productSceneView != null ? productSceneView.getCurrentShapeFigure() : null;
            shapesEnabledParam.setValue(Boolean.FALSE, this);
            valueRangeEnabledParam.setValue(Boolean.FALSE, this);
            valueRangeMinParam.setValue(0.0F, this);
            valueRangeMaxParam.setValue(1.0F, this);
            bitmaskEnabledParam.setValue(Boolean.FALSE, this);
            bitmaskExprParam.setValue("", this);
            pinsEnabledParam.setValue(Boolean.FALSE, this);
            operatorParam.setValue("OR", this);
            invertParam.setValue(Boolean.FALSE, this);
        }
    }

    private ROIDefinition createROIDefinition() {
        final ROIDefinition roiDefinition = new ROIDefinition();
        // View acts as a value-model for figures in this case
        roiDefinition.setShapeFigure(shapeFigure);
        roiDefinition.setShapeEnabled((Boolean) shapesEnabledParam.getValue());
        roiDefinition.setValueRangeEnabled((Boolean) valueRangeEnabledParam.getValue());
        roiDefinition.setValueRangeMin(((Number) valueRangeMinParam.getValue()).floatValue());
        roiDefinition.setValueRangeMax(((Number) valueRangeMaxParam.getValue()).floatValue());
        roiDefinition.setBitmaskEnabled((Boolean) bitmaskEnabledParam.getValue());
        final String bitmaskExpr = bitmaskExprParam.getValueAsText();
        roiDefinition.setBitmaskExpr(bitmaskExpr == null ? "" : bitmaskExpr);
        roiDefinition.setPinUseEnabled((Boolean) pinsEnabledParam.getValue());
        roiDefinition.setOrCombined("OR".equalsIgnoreCase((String) operatorParam.getValue()));
        roiDefinition.setInverted((Boolean) invertParam.getValue());
        return roiDefinition.isUsable() ? roiDefinition : null;
    }

    private Product getProduct() {
        return productSceneView != null ? productSceneView.getProduct() : null;
    }


    /**
     * Notifies a client if an exeption occured on a <code>Parameter</code>.
     *
     * @param e the exception
     *
     * @return <code>true</code> if the exception was handled successfully, <code>false</code> otherwise
     */
    public boolean handleParamException(ParamException e) {
        Debug.trace(e);
        return true;
    }


    private void resetBitmaskFlagNames() {
        final Product product = getProduct();
        if (product != null) {
            final String[] flagNames = product.getFlagCodingGroup().getNodeNames();
            bitmaskExprParam.getProperties().setValueSet(flagNames);
        }
    }

    private AbstractButton createButton(String path) {
        return ToolButtonFactory.createButton(UIUtils.loadImageIcon(path), false);
    }

    private String checkApplicability(ROIDefinition roiDefNew) {
        if (roiDefNew != null) {
            if (roiDefNew.isValueRangeEnabled()) {
                if (roiDefNew.getValueRangeMin() >= roiDefNew.getValueRangeMax()) {
                    return "Value Range:\nMinimum value is greater than or equal to selected maximum value."; /*I18N*/
                }
            }

            final Product product = getProduct();
            if (product != null) {
                final String bitmaskExpr = roiDefNew.getBitmaskExpr();
                if (!StringUtils.isNullOrEmpty(bitmaskExpr)) {
                    if (!product.isCompatibleBandArithmeticExpression(bitmaskExpr)) {
                        return MessageFormat.format(
                                "The bitmask expression\n''{0}''\nis not applicable for product\n''{1}''",
                                bitmaskExpr, product.getName());
                    }
                }
            }
        }
        return null;
    }

    private ProductNodeListener createProductNodeListener() {
        return new ProductNodeListener() {
            public void nodeChanged(ProductNodeEvent event) {
                final ProductNode sourceNode = event.getSourceNode();
                if (getCurrentRaster() == sourceNode) {
                    if (ProductNode.PROPERTY_NAME_NAME.equalsIgnoreCase(event.getPropertyName())) {
                        updateTitle();
                    } else if (RasterDataNode.PROPERTY_NAME_ROI_DEFINITION.equalsIgnoreCase(event.getPropertyName())) {
                        roiDefinitionUndo = getCurrentROIDefinition();
                        setUIParameterValues(roiDefinitionUndo);
                    }
                    updateUIState();
                }
            }

            public void nodeDataChanged(ProductNodeEvent event) {
                updateUIState();
            }

            public void nodeAdded(ProductNodeEvent event) {
                updateUIState();
            }

            public void nodeRemoved(ProductNodeEvent event) {
                updateUIState();
            }
        };
    }

    private class LayerModelChangeHandler extends LayerModelChangeAdapter {

        @Override
        public void handleLayerModelChanged(LayerModel layerModel) {
            if (productSceneView != null) {
                final Figure currentShapeFigure = productSceneView.getCurrentShapeFigure();
                if (currentShapeFigure != null) {
                    shapeFigure = currentShapeFigure;
                }
                updateUIState();
            }
        }
    }

    private class ROIDefinitionIFL extends InternalFrameAdapter {

        @Override
        public void internalFrameActivated(InternalFrameEvent e) {
            final Container contentPane = e.getInternalFrame().getContentPane();
            if (contentPane instanceof ProductSceneView) {
                final ProductSceneView view = (ProductSceneView) contentPane;
                setProductSceneView(view);
            } else {
                setProductSceneView(null);
            }
        }

        @Override
        public void internalFrameDeactivated(InternalFrameEvent e) {
            final Container contentPane = e.getInternalFrame().getContentPane();
            if (contentPane instanceof ProductSceneView) {
                setProductSceneView(null);
            }
        }
    }

    private class ROIDefinitionPML implements ProductManager.Listener {

        public void productAdded(ProductManager.Event event) {
            event.getProduct().addProductNodeListener(roiDefinitionPNL);
            updateUIState();
        }

        public void productRemoved(ProductManager.Event event) {
            event.getProduct().removeProductNodeListener(roiDefinitionPNL);
            updateUIState();
        }
    }


    private static class ROIDefinitionPNL extends ProductNodeListenerAdapter {

        ROIDefinitionPNL() {
        }

        @Override
        public void nodeChanged(ProductNodeEvent event) {
            if (RasterDataNode.PROPERTY_NAME_ROI_DEFINITION.equals(event.getPropertyName())) {
                final ProductNode sourceNode = event.getSourceNode();
                if (sourceNode instanceof RasterDataNode) {
                    final RasterDataNode raster = (RasterDataNode) sourceNode;
                    final JInternalFrame internalFrame = VisatApp.getApp().findInternalFrame(raster);
                    if (internalFrame == null) {
                        return;
                    }
                    final Container contentPane = internalFrame.getContentPane();
                    if (contentPane instanceof ProductSceneView) {
                        final ProductSceneView view = (ProductSceneView) contentPane;
                        VisatApp.getApp().updateROIImage(view, true);
                        view.getImageDisplay().repaint();
                    }
                }
            }
        }
    }
}
