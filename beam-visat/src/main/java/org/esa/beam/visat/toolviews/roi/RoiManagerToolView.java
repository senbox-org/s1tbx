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
import org.esa.beam.framework.param.validators.BooleanExpressionValidator;
import org.esa.beam.framework.ui.GridBagUtils;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.application.support.AbstractToolView;
import org.esa.beam.framework.ui.command.Command;
import org.esa.beam.framework.ui.command.CommandManager;
import org.esa.beam.framework.ui.command.ToolCommand;
import org.esa.beam.framework.ui.product.BandChooser;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.framework.ui.product.ProductTreeListener;
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
import java.util.ArrayList;
import java.util.Map;

public class RoiManagerToolView extends AbstractToolView implements ParamExceptionHandler {

    public static final String ID = RoiManagerToolView.class.getName();

    private static final String _FILE_EXTENSION = ".roi";

    private VisatApp _visatApp;
    private PropertyMap _propertyMap;
    private final ParamGroup _paramGroup;
    private AbstractButton _applyButton;
    private AbstractButton _resetButton;
    private AbstractButton _undoButton;
    private AbstractButton _multiAssignToBandsButton;
    private AbstractButton _multiAssignToProductsButton;
    private AbstractButton _importButton;
    private AbstractButton _exportButton;
    private AbstractButton _zoomToButton;
    private ProductSceneView _productSceneView;
    private ROIDefinition _roiDefinitionUndo;

    private Parameter _operatorParam;
    private Parameter _invertParam;
    private Parameter _valueRangeEnabledParam;
    private Parameter _valueRangeMinParam;
    private Parameter _valueRangeMaxParam;
    private Parameter _shapesEnabledParam;
    private Parameter _bitmaskEnabledParam;
    private Parameter _bitmaskExprParam;
    private Parameter _pinsEnabledParam;

    private final Command[] _shapeCommands;
    private JPanel _shapeToolsRow;
    private final String[] _operatorValueSet = new String[]{"OR", "AND"};
    private BeamFileFilter _roiDefinitionFileFilter;
    // TODO -  (nf) replace _bandsToBeModified with a String (name) array, memory leaks occur here!!!
    private Band[] _bandsToBeModified;
    private RoiManagerToolView.LayerModelChangeHandler _layerModelChangeHandler;
    private Figure _shapeFigure;
    private final ProductNodeListener _productNodeListener;

    private ProductNodeListener roiDefinitionPNL;


    public RoiManagerToolView() {
        _visatApp = VisatApp.getApp();
        CommandManager commandManager = _visatApp.getCommandManager();
        ToolCommand drawLineTool = commandManager.getToolCommand("drawLineTool");
        ToolCommand drawRectangleTool = commandManager.getToolCommand("drawRectangleTool");
        ToolCommand drawEllipseTool = commandManager.getToolCommand("drawEllipseTool");
        ToolCommand drawPolylineTool = commandManager.getToolCommand("drawPolylineTool");
        ToolCommand drawPolygonTool = commandManager.getToolCommand("drawPolygonTool");

        _paramGroup = new ParamGroup();
        _shapeCommands = new Command[]{
                drawLineTool,
                drawPolylineTool,
                drawRectangleTool,
                drawEllipseTool,
                drawPolygonTool,
        };
        _propertyMap = _visatApp.getPreferences();
        _layerModelChangeHandler = new RoiManagerToolView.LayerModelChangeHandler();
        _productNodeListener = createProductNodeListener();
        initParams();
    }

    public void setProductSceneView(final ProductSceneView productSceneView) {
        if (_productSceneView == productSceneView) {
            return;
        }
        if (_productSceneView != null) {
            _productSceneView.getImageDisplay().getLayerModel().removeLayerModelChangeListener(
                    _layerModelChangeHandler);
            _productSceneView.getProduct().removeProductNodeListener(_productNodeListener);
        }

        _productSceneView = productSceneView;
        _roiDefinitionUndo = null;

        if (_productSceneView != null) {
            _shapeFigure = _productSceneView.getRasterROIShapeFigure();
            if (_shapeFigure == null) {
                _shapeFigure = _productSceneView.getCurrentShapeFigure();
            }
            _productSceneView.getImageDisplay().getLayerModel().addLayerModelChangeListener(_layerModelChangeHandler);
            _productSceneView.getProduct().addProductNodeListener(_productNodeListener);
            _roiDefinitionUndo = getCurrentROIDefinition();
            setUIParameterValues(_roiDefinitionUndo);
            resetBitmaskFlagNames();
            _bitmaskExprParam.getProperties().setPropertyValue(BooleanExpressionEditor.PROPERTY_KEY_SELECTED_PRODUCT,
                                                               _productSceneView.getProduct());
            updateUIState();
        } else {
            _bitmaskExprParam.getProperties().setPropertyValue(BooleanExpressionEditor.PROPERTY_KEY_SELECTED_PRODUCT,
                                                               null);
            updateUIState();
        }

        updateTitle();

        setApplyEnabled(false);
        _resetButton.setEnabled(false);
    }

    private void updateTitle() {
        if (_productSceneView != null) {
            setTitle(getDescriptor().getTitle() + " - " + getCurrentRaster().getDisplayName());
        } else {
            setTitle(getDescriptor().getTitle());
        }
    }

    private void initParams() {

        _shapesEnabledParam = new Parameter("roi.shapesEnabled", Boolean.TRUE);
        _shapesEnabledParam.getProperties().setLabel("Include pixels in geometric shape"); /*I18N*/
        _shapesEnabledParam.getProperties().setDescription(
                "Select pixels within boundary given by geometric shape");/*I18N*/
        _paramGroup.addParameter(_shapesEnabledParam);

        _valueRangeEnabledParam = new Parameter("roi.valueRangeEnabled", Boolean.FALSE);
        _valueRangeEnabledParam.getProperties().setLabel("Include pixels in value range"); /*I18N*/
        _valueRangeEnabledParam.getProperties().setDescription("Select pixels in a given value range");/*I18N*/
        _paramGroup.addParameter(_valueRangeEnabledParam);

        _valueRangeMinParam = new Parameter("roi.valueRangeMin", new Float(0.0));
        _valueRangeMinParam.getProperties().setLabel("Min:"); /*I18N*/
        _valueRangeMinParam.getProperties().setDescription("Minimum sample value");/*I18N*/
        _paramGroup.addParameter(_valueRangeMinParam);

        _valueRangeMaxParam = new Parameter("roi.valueRangeMax", new Float(100.0));
        _valueRangeMaxParam.getProperties().setLabel("Max:"); /*I18N*/
        _valueRangeMaxParam.getProperties().setDescription("Maximum sample value");/*I18N*/
        _paramGroup.addParameter(_valueRangeMaxParam);

        _bitmaskEnabledParam = new Parameter("roi.bitmaskEnabled", Boolean.FALSE);
        _bitmaskEnabledParam.getProperties().setLabel("Include pixels by condition"); /*I18N*/
        _bitmaskEnabledParam.getProperties().setDescription(
                "Include pixels for which a given conditional expression is true"); /*I18N*/
        _paramGroup.addParameter(_bitmaskEnabledParam);

        _bitmaskExprParam = new Parameter("roi.bitmaskExpr", "");
        _bitmaskExprParam.getProperties().setNullValueAllowed(true);
        _bitmaskExprParam.getProperties().setEditorClass(BooleanExpressionEditor.class);
        _bitmaskExprParam.getProperties().setValidatorClass(BooleanExpressionValidator.class);
        _bitmaskExprParam.getProperties().setPropertyValue(BooleanExpressionEditor.PROPERTY_KEY_PREFERENCES,
                                                           _propertyMap);
        _bitmaskExprParam.getProperties().setLabel("Conditional expression:"); /*I18N*/
        _bitmaskExprParam.getProperties().setDescription("The conditional expression"); /*I18N*/
        _paramGroup.addParameter(_bitmaskExprParam);

        _pinsEnabledParam = new Parameter("roi.pinsEnabled", Boolean.FALSE);
        _pinsEnabledParam.getProperties().setLabel("Include pixels under pins"); /*I18N*/
        _pinsEnabledParam.getProperties().setDescription("Include pixels under pins"); /*I18N*/
        _paramGroup.addParameter(_pinsEnabledParam);

        _operatorParam = new Parameter("roi.operator", "OR");
        _operatorParam.getProperties().setLabel("Combine criteria with: "); /*I18N*/
        _operatorParam.getProperties().setDescription("Specify the criteria combination operator"); /*I18N*/
        _operatorParam.getProperties().setValueSet(_operatorValueSet);
        _operatorParam.getProperties().setValueSetBound(true);
        _operatorParam.getProperties().setEditorClass(org.esa.beam.framework.param.editors.ComboBoxEditor.class);
        _paramGroup.addParameter(_operatorParam);

        _invertParam = new Parameter("roi.invert", Boolean.FALSE);
        _invertParam.getProperties().setLabel("Invert"); /*I18N*/
        _invertParam.getProperties().setDescription("Select to invert the specified ROI (NOT operator)"); /*I18N*/
        _paramGroup.addParameter(_invertParam);
    }

    public JComponent createControl() {
        _shapeToolsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 1, 1));
        if (_shapeCommands != null) {
            for (final Command command : _shapeCommands) {
                if (command != null) {
                    _shapeToolsRow.add(command.createToolBarButton());
                } else {
                    _shapeToolsRow.add(new JLabel("  "));
                }
            }
        }

        final JPanel roiDefPane = createROIDefPane();

        _applyButton = new JButton("Apply");
        _applyButton.setToolTipText("Assign ROI to selected band"); /*I18N*/
        _applyButton.setMnemonic('A');
        _applyButton.setName("ApplyButton");
        _applyButton.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                apply();
            }
        });

        _undoButton = createButton("icons/Undo24.gif");
        _undoButton.setToolTipText("Undo ROI assignment"); /*I18N*/
        _undoButton.setName("UndoButton");
        _undoButton.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                undo();
            }
        });

        _multiAssignToBandsButton = createButton("icons/MultiAssignBands24.gif");
        _multiAssignToBandsButton.setToolTipText("Apply to other bands"); /*I18N*/
        _multiAssignToBandsButton.setName("AssignToBandsButton");
        _multiAssignToBandsButton.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                multiAssignToBands();
            }
        });

        _multiAssignToProductsButton = createButton("icons/MultiAssignProducts24.gif");
        _multiAssignToProductsButton.setToolTipText("Apply to other products"); /*I18N*/
        _multiAssignToProductsButton.setName("MultiAssignButton");
        _multiAssignToProductsButton.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                multiAssignToProducts();
            }
        });

        _resetButton = createButton("icons/Undo24.gif");
        _resetButton.setToolTipText("Reset ROI to default values"); /*I18N*/
        _resetButton.setName("ResetButton");
        _resetButton.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                reset();
            }
        });

        _importButton = createButton("icons/Import24.gif");
        _importButton.setToolTipText("Import ROI from text file."); /*I18N*/
        _importButton.setName("ImportButton");
        _importButton.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                importROIDef();
            }
        });
        _importButton.setEnabled(true);

        _exportButton = createButton("icons/Export24.gif");
        _exportButton.setToolTipText("Export ROI to text file."); /*I18N*/
        _exportButton.setName("ExportButton");
        _exportButton.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                exportROIDef();
            }
        });
        _exportButton.setEnabled(true);

        _zoomToButton = createButton("icons/ZoomTo24.gif");
        _zoomToButton.setToolTipText("Zoom to ROI."); /*I18N*/
        _zoomToButton.setName("ZoomButton");
        _zoomToButton.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                zoomToROI();
            }
        });
        _zoomToButton.setEnabled(false);

        final AbstractButton helpButton = createButton("icons/Help24.gif");
        helpButton.setName("HelpButton");

        final JPanel buttonPane = GridBagUtils.createPanel();
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridwidth = 2;
        gbc.weightx = 0.5;
        gbc.insets.bottom = 4;
        buttonPane.add(_applyButton, gbc);
        gbc.insets.bottom = 0;
        gbc.gridwidth = 1;
        gbc.gridy = 2;
        buttonPane.add(_undoButton, gbc);
        gbc.gridy++;
        buttonPane.add(_multiAssignToBandsButton, gbc);
        buttonPane.add(_multiAssignToProductsButton, gbc);
        gbc.gridy++;
        buttonPane.add(_importButton, gbc);
        buttonPane.add(_exportButton, gbc);
        gbc.gridy++;
        buttonPane.add(_zoomToButton, gbc);
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

        /**
         * This listener is required to add or remove the roiDefinitionPNL above when new products are opened or closed.
         */
        VisatApp.getApp().addProductTreeListener(new ROIDefinitionPTL());

        _paramGroup.addParamChangeListener(new ParamChangeListener() {
            public void parameterValueChanged(final ParamChangeEvent event) {
                updateUIState();
                setApplyEnabled(true);
            }
        });

        updateUIState();
        setProductSceneView(VisatApp.getApp().getSelectedProductSceneView());
        return mainPane;
    }

    private void multiAssignToBands() {
        final RasterDataNode[] protectedRasters = _productSceneView.getRasters();
        final ArrayList<Band> availableBandList = new ArrayList<Band>();
        Band[] availableBands = _productSceneView.getProduct().getBands();
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
            JOptionPane.showMessageDialog(getContentPane(), "No other bands available.",
                                          getDescriptor().getTitle(), JOptionPane.ERROR_MESSAGE);
            return;
        }
        final BandChooser bandChooser = new BandChooser(getWindowAncestor(),
                                                        "Apply to other Bands", getDescriptor().getHelpId(),
                                                        availableBands,
                                                        _bandsToBeModified);
        if (bandChooser.show() == BandChooser.ID_OK) {
            final ROIDefinition roiDefinition = getCurrentROIDefinition();
            _bandsToBeModified = bandChooser.getSelectedBands();
            for (final Band band : _bandsToBeModified) {
                band.setROIDefinition(roiDefinition.createCopy());
            }
        }
    }

    private void multiAssignToProducts() {
        final ProductSceneView selectedProductSceneView = _visatApp.getSelectedProductSceneView();
        final RasterDataNode currentRaster = selectedProductSceneView.getRaster();
        final Product currentProduct = currentRaster.getProduct();

        final String title = "Transfer ROI";   /*I18N*/
        final Product[] products = _visatApp.getProductManager().getProducts();
        final Product[] allProducts = extractAllProducts(products, currentProduct);

        final ProductChooser productChooser = new ProductChooser(getWindowAncestor(), title, null, allProducts, null,
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
            _visatApp.showErrorDialog("No ROI defined for '" + currentRaster.getDisplayName() + "'"); /*I18N*/
            return;
        }

        GeneralPath geoPath = null;
        for (final Product selectedProduct : selectedProducts) {
            GeneralPath pixelPath = null;
            if (!currentProduct.isCompatibleProduct(selectedProduct, 1f / (60f * 60f))) {
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

    private GeneralPath transformROIShape(final Product currentProduct, final ROIDefinition currentRoiDefinition) {
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

    private Product[] extractAllProducts(final Product[] products, final Product currentProduct) {
        final ArrayList<Product> allProducts = new ArrayList<Product>(products.length);
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
        final int result = fileChooser.showOpenDialog(getContentPane());
        if (result == JFileChooser.APPROVE_OPTION) {
            final File file = fileChooser.getSelectedFile();
            if (file != null) {
                setIODir(file.getAbsoluteFile().getParentFile());
                try {
                    final ROIDefinition roiDefinition = RoiManagerToolView.createROIFromFile(file);
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
            final int result = fileChooser.showSaveDialog(getContentPane());
            if (result == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                if (file != null) {
                    if (!_visatApp.promptForOverwrite(file)) {
                        return;
                    }
                    setIODir(file.getAbsoluteFile().getParentFile());
                    file = FileUtils.ensureExtension(file, RoiManagerToolView._FILE_EXTENSION);
                    try {
                        RoiManagerToolView.writeROIToFile(roiDefinition, file);
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
        if (_productSceneView == null) {
            return;
        }
        final RenderedImage roiImage = _productSceneView.getROIImage();
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
        _productSceneView.getImageDisplay().zoom(rect);
    }

    private BeamFileFilter getOrCreateRoiDefinitionFileFilter() {
        if (_roiDefinitionFileFilter == null) {
            final String formatName = "ROI_DEFINITION_FILE";
            final String description = "ROI definition files (*" + RoiManagerToolView._FILE_EXTENSION + ")";
            _roiDefinitionFileFilter = new BeamFileFilter(formatName, RoiManagerToolView._FILE_EXTENSION, description);
        }
        return _roiDefinitionFileFilter;
    }

    private static void writeROIToFile(final ROIDefinition roi, final File outputFile) throws IOException {
        Guardian.assertNotNull("roi", roi);
        Guardian.assertNotNull("outputFile", outputFile);

        XmlWriter writer;
        try {
            writer = new XmlWriter(outputFile);
            roi.writeXML(writer, 0);
        } catch (IOException e) {
            throw e;
        }
        writer.close();
    }

    private static ROIDefinition createROIFromFile(final File inputFile) throws IOException {
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

    private void showErrorDialog(final String message) {
        JOptionPane.showMessageDialog(getContentPane(),
                                      message,
                                      getDescriptor().getTitle() + " - Error",
                                      JOptionPane.ERROR_MESSAGE);
    }


    private void setApplyEnabled(final boolean enabled) {
        final boolean canApply = _productSceneView != null;
        _applyButton.setEnabled(canApply && enabled);
        _multiAssignToBandsButton.setEnabled(canApply && (!enabled && _visatApp != null));
    }

    private void setIODir(final File dir) {
        if (_propertyMap != null && dir != null) {
            _propertyMap.setPropertyString("roi.io.dir", dir.getPath());
        }
    }

    private File getIODir() {
        File dir = SystemUtils.getUserHomeDir();
        if (_propertyMap != null) {
            dir = new File(_propertyMap.getPropertyString("roi.io.dir", dir.getPath()));
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
        if (_roiDefinitionUndo != null) {
            resetImpl(_roiDefinitionUndo);
        } else {
            reset();
        }
    }

    private void applyImpl(final ROIDefinition roiDefNew) {
        if (_productSceneView == null) {
            return;
        }

        final ROIDefinition roiDefOld = getCurrentROIDefinition();
        if (roiDefOld == roiDefNew) {
            return;
        }

        final String warningMessage = checkApplicability(roiDefNew);
        if (warningMessage != null) {
            _visatApp.showWarningDialog(warningMessage);
            return;
        }

        setApplyEnabled(false);
        _resetButton.setEnabled(false);

        _productSceneView.setROIOverlayEnabled(true);
        setCurrentROIDefinition(roiDefNew);
// The folowing line is not necessary because in the line "setCurrentROIDefinition(roiDefNew);"
// the roi image is updated
//        _visatApp.updateROIImage(_productSceneView, true);
    }

    private void resetImpl(final ROIDefinition roiDefNew) {
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
        pane.add(_shapesEnabledParam.getEditor().getComponent(), gbc);

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
        pane.add(_valueRangeEnabledParam.getEditor().getComponent(), gbc);

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
        pane.add(_bitmaskEnabledParam.getEditor().getComponent(), gbc);

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
        pane.add(_pinsEnabledParam.getEditor().getComponent(), gbc);

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
        return _shapeToolsRow;
    }

    private JComponent createValueRangePane() {
        final JPanel pane = GridBagUtils.createPanel();
        final GridBagConstraints gbc = GridBagUtils.createConstraints("anchor=WEST,fill=HORIZONTAL");
        GridBagUtils.addToPanel(pane, _valueRangeMinParam.getEditor().getLabelComponent(), gbc,
                                "insets.left=0,weightx=0");
        GridBagUtils.addToPanel(pane, _valueRangeMinParam.getEditor().getComponent(), gbc, "insets.left=3,weightx=0.1");
        GridBagUtils.addToPanel(pane, new JLabel("  "), gbc, "insets.left=0,weightx=0.8");
        GridBagUtils.addToPanel(pane, _valueRangeMaxParam.getEditor().getLabelComponent(), gbc,
                                "insets.left=0,weightx=0");
        GridBagUtils.addToPanel(pane, _valueRangeMaxParam.getEditor().getComponent(), gbc, "insets.left=3,weightx=0.1");
        return pane;
    }

    private JComponent createBitmaskPane() {
        return _bitmaskExprParam.getEditor().getComponent();
    }

    private JPanel createOperatorPane() {
        final JPanel pane = GridBagUtils.createPanel();
        final GridBagConstraints gbc = GridBagUtils.createConstraints("anchor=WEST,fill=HORIZONTAL");
        GridBagUtils.addToPanel(pane, _operatorParam.getEditor().getLabelComponent(), gbc,
                                "gridx=0,gridy=0,gridwidth=1");
        GridBagUtils.addToPanel(pane, _operatorParam.getEditor().getComponent(), gbc, "gridx=1");
        GridBagUtils.addToPanel(pane, _invertParam.getEditor().getComponent(), gbc, "gridx=2,insets.left=7,weightx=1");
        return pane;
    }

    void updateUIState() {

        // Get boolean switches
        //
        final boolean roiPossible = (_productSceneView != null); //&& ProductData.isFloatingPointType(_productSceneView.getRaster().getDataType());
        final boolean valueRangeCritSet = (Boolean) _valueRangeEnabledParam.getValue();
        final boolean shapesCritPossible = (_shapeFigure != null);
        final boolean zoomToPossible = (_productSceneView != null && _productSceneView.getROIImage() != null);
        final boolean shapesCritSet = (Boolean) _shapesEnabledParam.getValue();
        boolean bitmaskCritPossible = false;
        boolean bitmaskCritSet = (Boolean) _bitmaskEnabledParam.getValue();
        final boolean pinCritPossible = getProduct() != null && getProduct().getPinGroup().getNodeCount() > 0;
        boolean pinCritSet = (Boolean) _pinsEnabledParam.getValue();

        if (roiPossible) {
            final Product product = getProduct();
            if (product != null && (product.getNumFlagCodings() > 0 || product.getNumBands() > 0 || product.getNumTiePointGrids() > 0))
            {
                bitmaskCritPossible = true;
            }
        }

        if (!bitmaskCritPossible) {
            _bitmaskEnabledParam.setValue(Boolean.FALSE, this);
            bitmaskCritSet = false;
        }

        if (!pinCritSet) {
            _pinsEnabledParam.setValue(Boolean.FALSE, this);
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
        _operatorParam.setUIEnabled(roiPossible && numCriteria > 1);
        _invertParam.setUIEnabled(roiPossible && numCriteria > 0);

        _shapesEnabledParam.setUIEnabled(roiPossible && shapesCritPossible);
        _valueRangeEnabledParam.setUIEnabled(roiPossible);
        _valueRangeMinParam.setUIEnabled(roiPossible && valueRangeCritSet);
        _valueRangeMaxParam.setUIEnabled(roiPossible && valueRangeCritSet);
        _bitmaskEnabledParam.setUIEnabled(roiPossible && bitmaskCritPossible);
        _bitmaskExprParam.setUIEnabled(roiPossible && bitmaskCritPossible && bitmaskCritSet);
        _pinsEnabledParam.setUIEnabled(roiPossible && pinCritPossible);
        _exportButton.setEnabled(roiPossible);
        _importButton.setEnabled(roiPossible);
        if (roiPossible) {
            _undoButton.setEnabled(_roiDefinitionUndo != null);
            _multiAssignToProductsButton.setEnabled(_visatApp != null
                                                    && _visatApp.getProductManager().getNumProducts() > 1);
        } else {
            _applyButton.setEnabled(false);
            _multiAssignToBandsButton.setEnabled(false);
            _multiAssignToProductsButton.setEnabled(false);
            _resetButton.setEnabled(false);
            _undoButton.setEnabled(false);
        }
        if (!shapesCritPossible) {
            _shapesEnabledParam.setValue(Boolean.FALSE, null);
        }

        _zoomToButton.setEnabled(zoomToPossible);
    }

    private ROIDefinition getCurrentROIDefinition() {
        return getCurrentRaster().getROIDefinition();
    }

    private void setCurrentROIDefinition(final ROIDefinition roiDefinition) {
        getCurrentRaster().setROIDefinition(roiDefinition);
    }

    private RasterDataNode getCurrentRaster() {
        return _productSceneView.getRaster();
    }

    public void setUIParameterValues(final ROIDefinition roiDefinition) {

        if (roiDefinition != null) {
            _shapeFigure = roiDefinition.getShapeFigure();
            _shapesEnabledParam.setValue(roiDefinition.isShapeEnabled(), null);
            _valueRangeEnabledParam.setValue(roiDefinition.isValueRangeEnabled(), null);
            _valueRangeMinParam.setValue(roiDefinition.getValueRangeMin(), null);
            _valueRangeMaxParam.setValue(roiDefinition.getValueRangeMax(), null);
            _bitmaskEnabledParam.setValue(roiDefinition.isBitmaskEnabled(), null);
            _bitmaskExprParam.setValue(roiDefinition.getBitmaskExpr(), null);
            _pinsEnabledParam.setValue(roiDefinition.isPinUseEnabled(), null);
            _operatorParam.setValue(roiDefinition.isOrCombined() ? _operatorValueSet[0] : _operatorValueSet[1], null);
            _invertParam.setValue(roiDefinition.isInverted(), null);
        } else {
            _shapeFigure = _productSceneView != null ? _productSceneView.getCurrentShapeFigure() : null;
            _shapesEnabledParam.setValue(Boolean.FALSE, this);
            _valueRangeEnabledParam.setValue(Boolean.FALSE, this);
            _valueRangeMinParam.setValue(0.0F, this);
            _valueRangeMaxParam.setValue(1.0F, this);
            _bitmaskEnabledParam.setValue(Boolean.FALSE, this);
            _bitmaskExprParam.setValue("", this);
            _pinsEnabledParam.setValue(Boolean.FALSE, this);
            _operatorParam.setValue("OR", this);
            _invertParam.setValue(Boolean.FALSE, this);
        }
    }

    private ROIDefinition createROIDefinition() {
        final ROIDefinition roiDefinition = new ROIDefinition();
        // View acts as a value-model for figures in this case
        roiDefinition.setShapeFigure(_shapeFigure);
        roiDefinition.setShapeEnabled((Boolean) _shapesEnabledParam.getValue());
        roiDefinition.setValueRangeEnabled((Boolean) _valueRangeEnabledParam.getValue());
        roiDefinition.setValueRangeMin(((Number) _valueRangeMinParam.getValue()).floatValue());
        roiDefinition.setValueRangeMax(((Number) _valueRangeMaxParam.getValue()).floatValue());
        roiDefinition.setBitmaskEnabled((Boolean) _bitmaskEnabledParam.getValue());
        final String bitmaskExpr = _bitmaskExprParam.getValueAsText();
        roiDefinition.setBitmaskExpr(bitmaskExpr == null ? "" : bitmaskExpr);
        roiDefinition.setPinUseEnabled((Boolean) _pinsEnabledParam.getValue());
        roiDefinition.setOrCombined("OR".equalsIgnoreCase((String) _operatorParam.getValue()));
        roiDefinition.setInverted((Boolean) _invertParam.getValue());
        return roiDefinition.isUsable() ? roiDefinition : null;
    }

    private Product getProduct() {
        return _productSceneView != null ? _productSceneView.getProduct() : null;
    }


    /**
     * Notifies a client if an exeption occured on a <code>Parameter</code>.
     *
     * @param e the exception
     *
     * @return <code>true</code> if the exception was handled successfully, <code>false</code> otherwise
     */
    public boolean handleParamException(final ParamException e) {
        Debug.trace(e);
        return true;
    }


    private void resetBitmaskFlagNames() {
        final Product product = getProduct();
        if (product != null) {
            final String[] flagNames = product.getAllFlagNames();
            _bitmaskExprParam.getProperties().setValueSet(flagNames);
        }
    }

    private AbstractButton createButton(final String path) {
        return ToolButtonFactory.createButton(UIUtils.loadImageIcon(path), false);
    }

    private String checkApplicability(final ROIDefinition roiDefNew) {
        if (roiDefNew != null) {
            if (roiDefNew.getValueRangeMin() >= roiDefNew.getValueRangeMax()) {
                return "Value Range:\nMinimum value is greater than or equal to selected maximum value."; /*I18N*/
            }

            final Product product = getProduct();
            if (product != null) {
                final String bitmaskExpr = roiDefNew.getBitmaskExpr();
                if (!StringUtils.isNullOrEmpty(bitmaskExpr)) {
                    if (!product.isCompatibleBandArithmeticExpression(bitmaskExpr)) {
                        return "The bitmask expression\n'" + bitmaskExpr + "'\n" +
                               "is not applicable for product\n" +
                               "'" + product.getName() + "'";
                    }
                }
            }
        }
        return null;
    }

    private ProductNodeListener createProductNodeListener() {
        return new ProductNodeListener() {
            public void nodeChanged(final ProductNodeEvent event) {
                final ProductNode sourceNode = event.getSourceNode();
                if (getCurrentRaster() == sourceNode) {
                    if (ProductNode.PROPERTY_NAME_NAME.equalsIgnoreCase(event.getPropertyName())) {
                        updateTitle();
                    } else if (RasterDataNode.PROPERTY_NAME_ROI_DEFINITION.equalsIgnoreCase(event.getPropertyName())) {
                        _roiDefinitionUndo = getCurrentROIDefinition();
                        setUIParameterValues(_roiDefinitionUndo);
                    }
                    updateUIState();
                }
            }

            public void nodeDataChanged(final ProductNodeEvent event) {
                updateUIState();
            }

            public void nodeAdded(final ProductNodeEvent event) {
                updateUIState();
            }

            public void nodeRemoved(final ProductNodeEvent event) {
                updateUIState();
            }
        };
    }

    private class LayerModelChangeHandler extends LayerModelChangeAdapter {

        @Override
        public void handleLayerModelChanged(final LayerModel layerModel) {
            if (_productSceneView != null) {
                final Figure currentShapeFigure = _productSceneView.getCurrentShapeFigure();
                if (currentShapeFigure != null) {
                    _shapeFigure = currentShapeFigure;
                }
                updateUIState();
                _applyButton.setEnabled(true);
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

    private class ROIDefinitionPML implements ProductManager.ProductManagerListener {

        public void productAdded(final ProductManager.ProductManagerEvent event) {
            updateUIState();
        }

        public void productRemoved(final ProductManager.ProductManagerEvent event) {
            updateUIState();
        }
    }


    private static class ROIDefinitionPNL extends ProductNodeListenerAdapter {

        public ROIDefinitionPNL() {
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


    private class ROIDefinitionPTL implements ProductTreeListener {

        public void productAdded(Product product) {
            product.addProductNodeListener(roiDefinitionPNL);
        }

        public void productRemoved(Product product) {
            product.removeProductNodeListener(roiDefinitionPNL);
        }

        public void productSelected(Product product, int clickCount) {
        }

        public void metadataElementSelected(MetadataElement group, int clickCount) {
        }

        public void tiePointGridSelected(TiePointGrid tiePointGrid, int clickCount) {
        }

        public void bandSelected(Band band, int clickCount) {
        }
    }
}
