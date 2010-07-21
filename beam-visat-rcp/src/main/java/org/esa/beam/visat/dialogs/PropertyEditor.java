/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.beam.visat.dialogs;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.swing.progress.DialogProgressMonitor;
import com.bc.jexp.ParseException;
import com.bc.jexp.Term;
import com.bc.jexp.WritableNamespace;
import com.bc.jexp.impl.ParserImpl;
import com.bc.jexp.impl.SymbolFactory;
import org.esa.beam.framework.datamodel.AbstractBand;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.FlagCoding;
import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.datamodel.ProductNodeEvent;
import org.esa.beam.framework.datamodel.ProductNodeListenerAdapter;
import org.esa.beam.framework.datamodel.ProductNodeNameValidator;
import org.esa.beam.framework.datamodel.ProductVisitorAdapter;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.esa.beam.framework.datamodel.VirtualBand;
import org.esa.beam.framework.dataop.barithm.BandArithmetic;
import org.esa.beam.framework.param.ParamChangeEvent;
import org.esa.beam.framework.param.ParamChangeListener;
import org.esa.beam.framework.param.ParamProperties;
import org.esa.beam.framework.param.Parameter;
import org.esa.beam.framework.param.editors.BooleanExpressionEditor;
import org.esa.beam.framework.param.editors.GeneralExpressionEditor;
import org.esa.beam.framework.ui.GridBagUtils;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.util.Debug;
import org.esa.beam.util.StringUtils;
import org.esa.beam.visat.VisatApp;

import javax.swing.JComponent;
import javax.swing.JInternalFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PropertyEditor {

    private final VisatApp _visatApp;
    private ModalDialog _dialog;

    public PropertyEditor(final VisatApp visatApp) {
        _visatApp = visatApp;
    }

    public void show(final ProductNode selectedProductNode) {
        if (isValidNode(selectedProductNode)) {
            final EditorContent editorContent = new EditorContent(selectedProductNode);
            show(editorContent);
        }
    }

    private void show(final EditorContent editorContent) {
        _dialog = new PropertyEditorDialog(editorContent);
        if (_dialog.show() == ModalDialog.ID_OK) {
            editorContent.changeProperties();
        }
        _dialog = null;
    }

    public ModalDialog getDialog() {
        return _dialog;
    }

    public static boolean isValidNode(final ProductNode node) {
        return node instanceof RasterDataNode || node instanceof Product;
    }

    private static String getTitleText(final EditorContent editorContent) {
        final ProductNode productNode = editorContent.getProductNode();
        final String type = getProductNodeTypeText(productNode);
        final String pattern = "{0} Properties - {1}"; /*I18N*/
        return new MessageFormat(pattern).format(new Object[]{type, productNode.getName()});
    }

    // @todo nf/nf make this a general utility function (e.g. Information pane should also use it)
    private static String getProductNodeTypeText(final ProductNode productNode) {
        final String type;
        if (productNode instanceof Product) {
            type = "Product";  /*I18N*/
        } else if (productNode instanceof TiePointGrid) {
            type = "Tie Point Grid"; /*I18N*/
        } else if (productNode instanceof VirtualBand) {
            type = "Virtual Band"; /*I18N*/
        } else if (productNode instanceof AbstractBand) {
            type = "Band";   /*I18N*/
        } else if (productNode instanceof FlagCoding) {
            type = "Flag Coding";  /*I18N*/
        } else if (productNode instanceof MetadataAttribute) {
            type = "Metadata Attribute";  /*I18N*/
        } else if (productNode instanceof MetadataElement) {
            type = "Metadata Element";  /*I18N*/
        } else {
            type = "Product Node";  /*I18N*/
        }
        return type;
    }

    class EditorContent extends JPanel {

        private static final int GROUP_GAP = 10;

        private final ProductNode _node;
        private Product _product;
        private RasterDataNode _rasterDataNode;
        private Band _band;
        private VirtualBand _virtualBand;

        private GridBagConstraints _gbc;

        public Parameter _paramName;
        public Parameter _paramDescription;

        public Parameter _paramSpectralBandwidth;
        public Parameter _paramSpectralWavelength;
        public Parameter _paramSpectralIndex;
        public Parameter _paramSunSpectralFlux;

        public Parameter _paramProductType;
        public Parameter _paramBandSubGroupPaths;
        public Parameter _paramNoDataValueUsed;
        public Parameter _paramNoDataValue;
        public Parameter _paramGeophysUnit;
        public Parameter _paramValidPixelExpr;
        private Parameter _paramVBExpression;
        private boolean _virtualBandPropertyChanged;
        private boolean _validMaskPropertyChanged;

        public EditorContent(final ProductNode node) {
            _node = node;
            initParameters(node);
            initUi(node);
        }

        public ProductNode getProductNode() {
            return _node;
        }

        private void initParameters(final ProductNode node) {
            initProductNodeParameters();
            node.acceptVisitor(createParamVisitor());
        }

        private ProductVisitorAdapter createParamVisitor() {
            return new ProductVisitorAdapter() {
                @Override
                public void visit(final Band band) {
                    if (!ignoreVisit()) {
                        initParamsForRasterDataNode(band);
                        initParamsForBand(band);
                    }
                }

                @Override
                public void visit(final TiePointGrid grid) {
                    if (!ignoreVisit()) {
                        initParamsForRasterDataNode(grid);
                    }
                }

                @Override
                public void visit(final Product product) {
                    _product = product;
                    initProductTypeParam();
                    initProductBandGroupingParam();
                }

                @Override
                public void visit(final VirtualBand virtualBand) {
                    if (!ignoreVisit()) {
                        initParamsForRasterDataNode(virtualBand);
                        initParamsForBand(virtualBand);
                        initParamsForVirtualBand(virtualBand);
                    }
                }
            };
        }

        private void initUi(final ProductNode node) {
            initProductNodeUI();
            node.acceptVisitor(creatUiVisitor());
        }

        private ProductVisitorAdapter creatUiVisitor() {
            return new ProductVisitorAdapter() {
                @Override
                public void visit(final Band band) {
                    if (!ignoreVisit()) {
                        initRasterDataNodeUI();
                        initBandUI();
                    }
                }

                @Override
                public void visit(final TiePointGrid grid) {
                    if (!ignoreVisit()) {
                        initRasterDataNodeUI();
                    }
                }

                @Override
                public void visit(final Product product) {
                    initProductUI();
                }

                @Override
                public void visit(final VirtualBand virtualBand) {
                    if (!ignoreVisit()) {
                        initRasterDataNodeUI();
                        initBandUI();
                        initVirtualBandUI();
                    }
                }
            };
        }

        public boolean validateProperties() {
            if (_rasterDataNode != null) {
                final String expression = _paramValidPixelExpr.getValueAsText();
                if (expression != null && expression.trim().length() != 0) {
                    final Product product = _rasterDataNode.getProduct();
                    try {
                        Product[] products = getCompatibleProducts(_rasterDataNode);
                        int defaultProductIndex = Arrays.asList(products).indexOf(product);
                        final WritableNamespace namespace = BandArithmetic.createDefaultNamespace(products, defaultProductIndex);
                        namespace.registerSymbol(SymbolFactory.createConstant(_paramName.getValueAsText(), 0));
                        final Term term = new ParserImpl(namespace, false).parse(expression);
                        if (!term.isB()) {
                            JOptionPane.showMessageDialog(_dialog.getJDialog(),
                                                          "The expression must be of boolean type."); /*I18N*/
                            return false;
                        }
                    } catch (ParseException e) {
                        JOptionPane.showMessageDialog(_dialog.getJDialog(),
                                                      "Invalid expression syntax:\n" + e.getMessage()); /*I18N*/
                        return false;
                    }
                }
            }

            if (_virtualBand != null) {
                final String expression = _paramVBExpression.getValueAsText();
                if (expression != null && expression.trim().length() != 0) {
                    final Product product = _virtualBand.getProduct();
                    try {
                        Product[] products = getCompatibleProducts(_virtualBand);
                        int defaultProductIndex = Arrays.asList(products).indexOf(product);
                        BandArithmetic.getValidMaskExpression(expression, products, defaultProductIndex, null);
                    } catch (ParseException e) {
                        JOptionPane.showMessageDialog(_dialog.getJDialog(),
                                                      "Invalid expression syntax:\n" + e.getMessage()); /*I18N*/
                        return false;
                    }
                }
            }
            return true;
        }

        private Product[] getCompatibleProducts(RasterDataNode rasterDataNode) {
            List<Product> compatibleProducts = new ArrayList<Product>(12);
            Product vbProduct = rasterDataNode.getProduct();
            compatibleProducts.add(vbProduct);
            Product[] products = vbProduct.getProductManager().getProducts();
            final float geolocationEps = getGeolocationEps();
            for (Product product : products) {
                if (vbProduct != product) {
                    if (vbProduct.isCompatibleProduct(product, geolocationEps)) {
                        compatibleProducts.add(product);
                    }
                }
            }
            return compatibleProducts.toArray(new Product[compatibleProducts.size()]);
        }

        private float getGeolocationEps() {
            return (float) VisatApp.getApp().getPreferences().getPropertyDouble(VisatApp.PROPERTY_KEY_GEOLOCATION_EPS,
                                                                                VisatApp.PROPERTY_DEFAULT_GEOLOCATION_EPS);
        }

        public void changeProperties() {
            _virtualBandPropertyChanged = false;
            _validMaskPropertyChanged = false;

            final ProductNodeHandler listener = new ProductNodeHandler();

            try {
                _node.getProduct().addProductNodeListener(listener);
                _node.setName(_paramName.getValueAsText());
                _node.setDescription(_paramDescription.getValueAsText());
                if (_product != null) {
                    _product.setProductType(_paramProductType.getValueAsText());
                    _product.setAutoGrouping(_paramBandSubGroupPaths.getValueAsText());
                }
                if (_rasterDataNode != null) {
                    final boolean noDataValueUsed = ((Boolean) _paramNoDataValueUsed.getValue()).booleanValue();
                    _rasterDataNode.setNoDataValueUsed(noDataValueUsed);
                    if (noDataValueUsed) {
                        _rasterDataNode.setGeophysicalNoDataValue(((Double) _paramNoDataValue.getValue()).doubleValue());
                    }
                    _rasterDataNode.setUnit(_paramGeophysUnit.getValueAsText());
                    _rasterDataNode.setValidPixelExpression(_paramValidPixelExpr.getValueAsText());
                }
                if (_band != null) {
                    _band.setSpectralWavelength(((Float) _paramSpectralWavelength.getValue()).floatValue());
                    _band.setSpectralBandwidth(((Float) _paramSpectralBandwidth.getValue()).floatValue());
                }
                if (_virtualBand != null) {
                    _virtualBand.setExpression(_paramVBExpression.getValueAsText());
                }
            } finally {
                _node.getProduct().removeProductNodeListener(listener);
            }

            if (_rasterDataNode != null && (_virtualBandPropertyChanged || _validMaskPropertyChanged)) {
                updateImages();
            }
        }

        private String formatBandSubGroupPaths() {
            final Product.AutoGrouping autoGrouping = _product.getAutoGrouping();
            if (autoGrouping != null) {
                return autoGrouping.toString();
            } else {
                return "";
            }
        }

        private void updateImages() {
            final SwingWorker<Exception, Object> worker = new SwingWorker<Exception, Object>() {

                @Override
                protected Exception doInBackground() throws Exception {
                    final ProgressMonitor pm = new DialogProgressMonitor(_visatApp.getMainFrame(), "Applying changes",
                                                                         Dialog.ModalityType.APPLICATION_MODAL);

                    pm.beginTask("Recomputing image(s)...", 3);
                    try {
                        if (_virtualBandPropertyChanged && _virtualBand != null) {
                            if (_virtualBand.hasRasterData()) {
                                _virtualBand.readRasterDataFully(ProgressMonitor.NULL);
                            }
                        }
                        pm.worked(1);
                        if (_validMaskPropertyChanged) {
                            final JInternalFrame internalFrame = _visatApp.findInternalFrame(_rasterDataNode);
                            if (internalFrame != null) {
                                final ProductSceneView psv = getProductSceneView(internalFrame);
                                psv.updateNoDataImage();
                                pm.worked(1);
                            } else {
                                pm.worked(1);
                            }
                        }
                        _visatApp.updateImages(new RasterDataNode[]{_rasterDataNode});
                        pm.worked(1);
                    } catch (IOException e) {
                        return e;
                    } finally {
                        pm.done();
                    }
                    return null;
                }

                @Override
                public void done() {
                    Exception exception;
                    try {
                        exception = get();
                    } catch (Exception e) {
                        exception = e;
                    }
                    if (exception != null) {
                        Debug.trace(exception);
                        _visatApp.showErrorDialog("Failed to compute band '" + _node.getDisplayName() + "':\n"
                                + exception.getMessage()); /*I18N*/
                    }
                }
            };
            worker.execute();
        }

        private void initParamsForRasterDataNode(final RasterDataNode rasterDataNode) {
            _rasterDataNode = rasterDataNode;
            initNoDataValueUsedParam();
            initNoDataValueParam();
            initUnitParam();
            initValidPixelExpressionParam();
        }

        private void initParamsForBand(Band band) {
            _band = band;

            _paramSpectralWavelength = new Parameter("SpectralWavelength", new Float(_band.getSpectralWavelength()));
            _paramSpectralWavelength.getProperties().setLabel("Spectral wavelength");
            _paramSpectralWavelength.getProperties().setPhysicalUnit("nm");
            _paramSpectralWavelength.getProperties().setDescription("Spectral wavelength in nanometers");
            _paramSpectralWavelength.getProperties().setNumCols(13);

            _paramSpectralBandwidth = new Parameter("SpectralBandwidth", new Float(_band.getSpectralBandwidth()));
            _paramSpectralBandwidth.getProperties().setLabel("Spectral bandwidth");
            _paramSpectralBandwidth.getProperties().setPhysicalUnit("nm");
            _paramSpectralBandwidth.getProperties().setDescription("Spectral bandwidth in nanometers");
            _paramSpectralBandwidth.getProperties().setNumCols(13);
        }

        private void initParamsForVirtualBand(final VirtualBand virtualBand) {
            _virtualBand = virtualBand;
            initVirtualBandExpressionParam();
        }

        private boolean ignoreVisit() {
            return _product != null;
        }

        private void initProductTypeParam() {
            final ParamProperties properties = new ParamProperties(String.class);
            properties.setNullValueAllowed(false);
            properties.setEmptyValuesNotAllowed(true);
            properties.setLabel("Product type"); /*I18N*/
            _paramProductType = new Parameter("productType", _product.getProductType(), properties);
        }

        private void initProductBandGroupingParam() {
            final ParamProperties properties = new ParamProperties(String.class);
            properties.setNullValueAllowed(true);
            properties.setEmptyValuesNotAllowed(false);
            properties.setLabel("Band grouping"); /*I18N*/
            properties.setDescription("Colon-separated (':') list of band name parts which are used to auto-create band groups."); /*I18N*/
            properties.setNumRows(2);
            properties.setPropertyValue(ParamProperties.WORD_WRAP_KEY, true);
            _paramBandSubGroupPaths = new Parameter("bandGrouping", formatBandSubGroupPaths(), properties);
        }

        private void initVirtualBandExpressionParam() {
            final ParamProperties properties = new ParamProperties(String.class);
            properties.setNullValueAllowed(true);
            properties.setLabel("Virtual band expression"); /*I18N*/
            properties.setNumRows(2);
            properties.setNumCols(42);
            properties.setDescription("The expression used to compute the pixel values of this band."); /*I18N*/
            properties.setEditorClass(GeneralExpressionEditor.class);
            // todo setting namespace as property to the ExpressionEditor for validating the expression
            properties.setPropertyValue(GeneralExpressionEditor.PROPERTY_KEY_SELECTED_PRODUCT,
                                        _virtualBand.getProduct());
            properties.setPropertyValue(GeneralExpressionEditor.PROPERTY_KEY_INPUT_PRODUCTS,
                                        getCompatibleProducts(_virtualBand));
            properties.setPropertyValue(GeneralExpressionEditor.PROPERTY_KEY_PREFERENCES,
                                        VisatApp.getApp().getPreferences());
            _paramVBExpression = new Parameter("virtualBandExpr", _virtualBand.getExpression(), properties);
            _paramName.addParamChangeListener(new ParamChangeListener() {
                public void parameterValueChanged(final ParamChangeEvent event) {
                    final String expresion = _paramVBExpression.getValueAsText();
                    final String newExpression = StringUtils.replaceWord(expresion, (String) event.getOldValue(),
                                                                         _paramName.getValueAsText());
                    _paramVBExpression.setValueAsText(newExpression, null);
                }
            });
        }

        private void initValidPixelExpressionParam() {
            final ParamProperties properties = new ParamProperties(String.class);
            properties.setNullValueAllowed(true);
            properties.setLabel("Valid pixel expression"); /*I18N*/
            properties.setDescription("Boolean expression which is used to identify valid pixels"); /*I18N*/
            properties.setNumRows(2);
            properties.setEditorClass(BooleanExpressionEditor.class);
            // todo setting namespace as property to the ExpressionEditor for validating the expression
            properties.setPropertyValue(BooleanExpressionEditor.PROPERTY_KEY_SELECTED_PRODUCT,
                                        _rasterDataNode.getProduct());
            properties.setPropertyValue(BooleanExpressionEditor.PROPERTY_KEY_INPUT_PRODUCTS,
                                        getCompatibleProducts(_rasterDataNode));
            _paramValidPixelExpr = new Parameter("validMaskExpr",
                                                 _rasterDataNode.getValidPixelExpression(),
                                                 properties);
            _paramName.addParamChangeListener(new ParamChangeListener() {
                public void parameterValueChanged(final ParamChangeEvent event) {
                    final String expresion = _paramValidPixelExpr.getValueAsText();
                    final String newExpression = StringUtils.replaceWord(expresion, (String) event.getOldValue(),
                                                                         _paramName.getValueAsText());
                    _paramValidPixelExpr.setValueAsText(newExpression, null);
                }
            });
        }

        private void initUnitParam() {
            final ParamProperties properties = new ParamProperties(String.class);
            properties.setLabel("Geophysical unit");       /*I18N*/
            properties.setDescription("The geophysical unit of pixel values"); /*I18N*/
            _paramGeophysUnit = new Parameter("unit",
                                              _rasterDataNode.getUnit() == null ? "" : _rasterDataNode.getUnit(),
                                              properties); /*I18N*/
        }

        private void initNoDataValueUsedParam() {
            final ParamProperties properties = new ParamProperties(Boolean.class);
            properties.setLabel("Use no-data value:"); /*I18N*/
            properties.setDescription("Indicates that the no-data value is used"); /*I18N*/
            _paramNoDataValueUsed = new Parameter("noDataValueUsed",
                                                  Boolean.valueOf(_rasterDataNode.isNoDataValueUsed()),
                                                  properties);
            _paramNoDataValueUsed.addParamChangeListener(new ParamChangeListener() {
                public void parameterValueChanged(final ParamChangeEvent event) {
                    _paramNoDataValue.getEditor().setEnabled(
                            ((Boolean) _paramNoDataValueUsed.getValue()).booleanValue());
                }
            });
        }

        private void initNoDataValueParam() {
            final Double noDataValue = _rasterDataNode.getGeophysicalNoDataValue();
            final ParamProperties properties = new ParamProperties(Double.class);
            properties.setLabel("No-data value"); /*I18N*/
            properties.setDescription("The value used to indicate no-data"); /*I18N*/
            properties.setNumCols(13);
            _paramNoDataValue = new Parameter("noDataValue", noDataValue, properties);
            _paramNoDataValue.getEditor().setEnabled(_rasterDataNode.isNoDataValueUsed());
        }

        private void initProductNodeParameters() {
            final ParamProperties nameProp = new ParamProperties(String.class);
            nameProp.setLabel("Name"); /*I18N*/
            _paramName = new Parameter("nameParam", _node.getName(), nameProp);
            if (_node instanceof RasterDataNode) {
                addNameValidator();
            }

            final ParamProperties descProp = new ParamProperties(String.class);
            descProp.setLabel("Description"); /*I18N*/
            descProp.setNumRows(2);
            descProp.setPropertyValue(ParamProperties.WORD_WRAP_KEY, true);
            _paramDescription = new Parameter("descParam", _node.getDescription(), descProp);
        }


        private void initProductNodeUI() {
            setLayout(new GridBagLayout());
            _gbc = GridBagUtils.createDefaultConstraints();
            _gbc.fill = GridBagConstraints.HORIZONTAL;
            _gbc.anchor = GridBagConstraints.NORTHWEST;
            _gbc.weighty = 1;
            _gbc.insets.top = 2;
            _gbc.insets.bottom = 2;

            _gbc.gridy++;
            _gbc.weightx = 0;
            add(_paramName.getEditor().getLabelComponent(), _gbc);
            _gbc.weightx = 1;
            add(_paramName.getEditor().getComponent(), _gbc);
            _gbc.gridy++;
            _gbc.weightx = 0;
            add(_paramDescription.getEditor().getLabelComponent(), _gbc);
            _gbc.fill = GridBagConstraints.BOTH;
            _gbc.weightx = 1;
            _gbc.weighty = 500;
            add(_paramDescription.getEditor().getComponent(), _gbc);
            _gbc.fill = GridBagConstraints.HORIZONTAL;
            _gbc.weighty = 1;

        }

        private void initRasterDataNodeUI() {
            _gbc.gridy++;
            _gbc.weightx = 0;
            add(_paramGeophysUnit.getEditor().getLabelComponent(), _gbc);
            _gbc.weightx = 1;
            add(_paramGeophysUnit.getEditor().getComponent(), _gbc);

            _gbc.insets.top += GROUP_GAP;
            _gbc.gridy++;
            _gbc.weightx = 0;
            add(_paramNoDataValueUsed.getEditor().getComponent(), _gbc);
            _gbc.weightx = 1;
            add(_paramNoDataValue.getEditor().getComponent(), _gbc);
            _gbc.insets.top -= GROUP_GAP;

            _gbc.gridy++;
            _gbc.weightx = 0;
            add(_paramValidPixelExpr.getEditor().getLabelComponent(), _gbc);
            _gbc.weightx = 1;
            _gbc.weighty = 2000;
            _gbc.fill = GridBagConstraints.BOTH;
            add(_paramValidPixelExpr.getEditor().getComponent(), _gbc);
            _gbc.fill = GridBagConstraints.HORIZONTAL;
            _gbc.weighty = 1;
        }

        private void initProductUI() {
            _gbc.gridy++;
            _gbc.weightx = 0;
            add(_paramProductType.getEditor().getLabelComponent(), _gbc);
            _gbc.weightx = 1;
            add(_paramProductType.getEditor().getComponent(), _gbc);
            _gbc.fill = GridBagConstraints.HORIZONTAL;
            _gbc.weighty = 1;

            _gbc.gridy++;
            _gbc.weightx = 0;
            add(_paramBandSubGroupPaths.getEditor().getLabelComponent(), _gbc);
            _gbc.weightx = 1;
            add(_paramBandSubGroupPaths.getEditor().getComponent(), _gbc);
            _gbc.fill = GridBagConstraints.HORIZONTAL;
            _gbc.weighty = 1;
        }

        private void initBandUI() {
            _gbc.insets.top += GROUP_GAP;
            _gbc.fill = GridBagConstraints.HORIZONTAL;

            _gbc.gridy++;
            _gbc.weightx = 0;
            add(_paramSpectralWavelength.getEditor().getLabelComponent(), _gbc);
            _gbc.weightx = 1;
            add(createValueUnitPair(_paramSpectralWavelength.getEditor().getComponent(),
                                    _paramSpectralWavelength.getEditor().getPhysUnitLabelComponent()), _gbc);

            _gbc.insets.top = 2;
            _gbc.gridy++;
            _gbc.weightx = 0;
            add(_paramSpectralBandwidth.getEditor().getLabelComponent(), _gbc);
            _gbc.weightx = 1;
            add(createValueUnitPair(_paramSpectralBandwidth.getEditor().getComponent(),
                                    _paramSpectralBandwidth.getEditor().getPhysUnitLabelComponent()), _gbc);

            _gbc.insets.top -= GROUP_GAP;
        }

        private JPanel createValueUnitPair(JComponent c1, JComponent c2) {
            final JPanel panel = new JPanel(new BorderLayout(2, 2));
            panel.add(c1, BorderLayout.CENTER);
            panel.add(c2, BorderLayout.EAST);
            return panel;
        }

        private void initVirtualBandUI() {
            _gbc.insets.top += GROUP_GAP;
            _gbc.gridy++;
            _gbc.weightx = 0;
            add(_paramVBExpression.getEditor().getLabelComponent(), _gbc);
            _gbc.weightx = 1;
            _gbc.weighty = 2000;
            _gbc.fill = GridBagConstraints.BOTH;
            add(_paramVBExpression.getEditor().getComponent(), _gbc);
            _gbc.fill = GridBagConstraints.HORIZONTAL;
            _gbc.weighty = 1;
            _gbc.insets.top -= GROUP_GAP;
        }

        private void addNameValidator() {
            _paramName.getProperties().setValidatorClass(ProductNodeNameValidator.class);
            _paramName.getProperties().setPropertyValue(ProductNodeNameValidator.PRODUCT_PROPERTY_KEY,
                                                        _node.getProduct());
        }


        private class ProductNodeHandler extends ProductNodeListenerAdapter {

            @Override
            public void nodeChanged(ProductNodeEvent event) {
                if (isVirtualBandRelevantPropertyName(event.getPropertyName())) {
                    _virtualBandPropertyChanged = true;
                }
                ProductNode productNode = event.getSourceNode();
                if (productNode instanceof RasterDataNode) {
                    if (RasterDataNode.isValidMaskProperty(event.getPropertyName())) {
                        _validMaskPropertyChanged = true;
                    }
                }
            }
        }
    }

    private static ProductSceneView getProductSceneView(final JInternalFrame internalFrame) {
        final Container contentPane = internalFrame.getContentPane();
        if (contentPane instanceof ProductSceneView) {
            return (ProductSceneView) contentPane;
        }
        return null;
    }


    private static boolean isVirtualBandRelevantPropertyName(final String propertyName) {
        return VirtualBand.PROPERTY_NAME_EXPRESSION.equals(propertyName);
    }

    class PropertyEditorDialog extends ModalDialog {
        private final EditorContent editorContent;

        public PropertyEditorDialog(EditorContent editorContent) {
            super(PropertyEditor.this._visatApp.getMainFrame(), PropertyEditor.getTitleText(editorContent), editorContent, ModalDialog.ID_OK_CANCEL_HELP, "propertyEditor");
            this.editorContent = editorContent;
        }

        @Override
        protected boolean verifyUserInput() {
            return editorContent.validateProperties();
        }
    }
}

