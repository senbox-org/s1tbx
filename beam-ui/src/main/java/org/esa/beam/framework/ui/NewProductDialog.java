/*
 * $Id: NewProductDialog.java,v 1.1 2006/10/10 14:47:38 norman Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.framework.ui;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.text.JTextComponent;

import org.esa.beam.framework.dataio.ProductSubsetBuilder;
import org.esa.beam.framework.dataio.ProductSubsetDef;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductNodeList;
import org.esa.beam.framework.datamodel.ProductNodeNameValidator;
import org.esa.beam.framework.datamodel.TiePointGeoCoding;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.esa.beam.framework.param.ParamChangeEvent;
import org.esa.beam.framework.param.ParamChangeListener;
import org.esa.beam.framework.param.Parameter;
import org.esa.beam.framework.ui.product.ProductSubsetDialog;
import org.esa.beam.framework.dataop.maptransf.Datum;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.io.FileUtils;

//@todo 1 he/** - add (more) class documentation

public class NewProductDialog extends ModalDialog {

    private static final String _defaultNumberText = "####";
    private static final String _defaultLatLonText = "##°";

    private static final String _CopyAllCommand = "all";
    private static final String _CopyGeocodingOnlyCommand = "geo";
    private static final String _CopySubsetCommand = "sub";

    private static int _numNewProducts = 0;

    private Parameter _paramNewName;
    private Parameter _paramNewDesc;
    private Parameter _paramSourceProduct;

    private final ProductNodeList _sourceProducts;
    private final Window _parent;
    private String _prefix;
    private Exception _exception;
    private boolean _sourceProductOwner;

    private ProductSubsetDef _subsetDef;
    private Product _editProduct; // currently not used, but possibly later for product editing
    private Product _resultProduct;

    private JButton _subsetButton;
    private JRadioButton _copyAllRButton;
    private JRadioButton _geocodingRButton;
    private JRadioButton _subsetRButton;

    private JLabel _labelWidthInfo;
    private JLabel _labelHeightInfo;
    private JLabel _labelCenterLatInfo;
    private JLabel _labelCenterLonInfo;
    private int _selectedProductIndex;

    public NewProductDialog(Window parent,
                            ProductNodeList sourceProducts,
                            final int selectedSourceIndex,
                            boolean sourceProductOwner) {
        this(parent, sourceProducts, selectedSourceIndex, sourceProductOwner, "subset");
    }

    public NewProductDialog(Window parent,
                            ProductNodeList sourceProducts,
                            final int selectedSourceIndex, boolean sourceProductOwner,
                            String prefix) {
        super(parent, "New Product", ModalDialog.ID_OK_CANCEL, null); /* I18N */
        Guardian.assertNotNull("sourceProducts", sourceProducts);
        Guardian.assertEquals("not the expected element type", sourceProducts.getElemType(), Product.class);
        Guardian.assertGreaterThan("sourceProducts.size()", sourceProducts.size(), 0);
        _sourceProducts = sourceProducts;
        _selectedProductIndex = selectedSourceIndex;
        _parent = parent;
        _prefix = prefix;
        _sourceProductOwner = sourceProductOwner;
    }

    public int show() {
        createParameter();
        createUI();
        updateUI();
        return super.show();
    }

    public void setSubsetDef(ProductSubsetDef subsetDef) {
        _subsetDef = subsetDef;
    }

//    public void setProductToEdit(Product product) {
//        _editProduct = product;
//    }

    public boolean isSourceProductOwner() {
        return _sourceProductOwner;
    }

    public Product getSourceProduct() {
        String displayName = _paramSourceProduct.getValueAsText();
        return (Product) _sourceProducts.getByDisplayName(displayName);
    }

    public Product getResultProduct() {
        return _resultProduct;
    }

    protected void onCancel() {
        super.onCancel();
        _resultProduct = null;
    }

    protected void onOK() {
        super.onOK();
        String prodName = _paramNewName.getValueAsText();
        String prodDesc = _paramNewDesc.getValueAsText();
        Product sourceProduct = getSourceProduct();
        _resultProduct = null;
        try {
            if (_geocodingRButton.isSelected()) {
                ProductSubsetDef def = new ProductSubsetDef();
                def.addNodeName("latitude");
                def.addNodeName("longitude");
                def.addNodeName(Product.HISTORY_ROOT_NAME);
                _resultProduct = ProductSubsetBuilder.createProductSubset(sourceProduct, _sourceProductOwner, def,
                                                                          prodName, prodDesc);
                // @todo 1 nf/** - check: do we really need the following code or is it done in the  ProductSubsetBuilder?
                TiePointGrid latGrid = _resultProduct.getTiePointGrid("latitude");
                TiePointGrid lonGrid = _resultProduct.getTiePointGrid("longitude");
                if (latGrid != null && lonGrid != null) {
                    _resultProduct.setGeoCoding(new TiePointGeoCoding(latGrid, lonGrid, sourceProduct.getGeoCoding().getDatum()));
                }
            } else if (_subsetDef != null && _subsetRButton.isSelected()) {
                _resultProduct = ProductSubsetBuilder.createProductSubset(sourceProduct, _sourceProductOwner,
                                                                          _subsetDef, prodName, prodDesc);
            } else {
                _resultProduct = ProductSubsetBuilder.createProductSubset(sourceProduct, _sourceProductOwner, null,
                                                                          prodName, prodDesc);
            }
        } catch (IOException e) {
            _exception = e;
        }
    }

    public Exception getException() {
        return _exception;
    }

    protected boolean verifyUserInput() {
        boolean b = super.verifyUserInput();
        String name = _paramNewName.getValueAsText();
        boolean bName = name != null && name.length() > 0;
        boolean subset = true;
        if (_subsetRButton.isSelected() && _subsetDef == null) {
            subset = false;
            showErrorDialog("Please define a valid spatial or spectral subset."); /*I18N*/
        }
        return b && bName && subset;
    }

    private void createParameter() {
        _numNewProducts++;

        String prodName;
        String prodDesc;
        String[] valueSet;
        boolean enableSourceProduct;
        if (_editProduct == null) {
            valueSet = _sourceProducts.getDisplayNames();
            Product product = (Product)_sourceProducts.getAt(_selectedProductIndex);
            prodName = createNewProductName(valueSet.length > 0 ? product.getName() : "");
            prodDesc = "";
            enableSourceProduct = true;
        } else {
            prodName = _editProduct.getName();
            prodDesc = _editProduct.getDescription();
            valueSet = new String[]{prodName};
            enableSourceProduct = false;
        }

        _paramNewName = new Parameter("productName", prodName);
        _paramNewName.getProperties().setLabel("Name"); /* I18N */
        _paramNewName.getProperties().setNullValueAllowed(false);
        _paramNewName.getProperties().setValidatorClass(ProductNodeNameValidator.class);

        _paramNewDesc = new Parameter("productDesc", prodDesc);
        _paramNewDesc.getProperties().setLabel("Description"); /* I18N */
        _paramNewDesc.getProperties().setNullValueAllowed(false);

        _paramSourceProduct = new Parameter("sourceProduct", valueSet[_selectedProductIndex]);
        _paramSourceProduct.getProperties().setValueSet(valueSet);
        _paramSourceProduct.getProperties().setLabel("Derive from Product"); /* I18N */
        _paramSourceProduct.getProperties().setValueSetBound(true);
        _paramSourceProduct.setUIEnabled(enableSourceProduct);
        _paramSourceProduct.addParamChangeListener(new ParamChangeListener() {

            public void parameterValueChanged(ParamChangeEvent event) {
                _subsetDef = null;
                updateUI();
                setNewProductName();
            }
        });
    }

    private void createUI() {
        createButtonsAndLabels();
        int line = 0;
        JPanel dialogPane = GridBagUtils.createPanel();
        final GridBagConstraints gbc = GridBagUtils.createDefaultConstraints();

        gbc.gridy = ++line;
        GridBagUtils.addToPanel(dialogPane, _paramNewName.getEditor().getLabelComponent(), gbc,
                                "fill=BOTH, weightx=0, insets.top=3");
        GridBagUtils.addToPanel(dialogPane, _paramNewName.getEditor().getComponent(), gbc, "weightx=1, gridwidth=3");

        gbc.gridy = ++line;
        GridBagUtils.addToPanel(dialogPane, _paramNewDesc.getEditor().getLabelComponent(), gbc,
                                "weightx=0, gridwidth=1");
        GridBagUtils.addToPanel(dialogPane, _paramNewDesc.getEditor().getComponent(), gbc, "weightx=1, gridwidth=3");

        gbc.gridy = ++line;
        GridBagUtils.addToPanel(dialogPane, _paramSourceProduct.getEditor().getLabelComponent(), gbc,
                                "fill=NONE, gridwidth=4, insets.top=15");
        gbc.gridy = ++line;
        GridBagUtils.addToPanel(dialogPane, _paramSourceProduct.getEditor().getComponent(), gbc,
                                "fill=HORIZONTAL, insets.top=3");
        gbc.gridy = ++line;
        final JPanel radioPanel = new JPanel(new BorderLayout());
        radioPanel.add(_copyAllRButton, BorderLayout.WEST);
        radioPanel.add(_geocodingRButton);
        GridBagUtils.addToPanel(dialogPane, radioPanel, gbc, "fill=NONE, gridwidth=2");
        GridBagUtils.addToPanel(dialogPane, _subsetRButton, gbc, "gridwidth=1, weightx=300, anchor=EAST");
        GridBagUtils.addToPanel(dialogPane, _subsetButton, gbc, "fill=NONE, weightx=1, anchor=EAST");

        gbc.gridy = ++line;
        GridBagUtils.addToPanel(dialogPane, createInfoPanel(), gbc,
                                "fill=BOTH, anchor=WEST, insets.top=10, gridwidth=4");

        setContent(dialogPane);

        final JComponent editorComponent = _paramNewName.getEditor().getEditorComponent();
        if (editorComponent instanceof JTextComponent) {
            JTextComponent tf = (JTextComponent) editorComponent;
            tf.selectAll();
            tf.requestFocus();
        }
    }

    private JPanel createInfoPanel() {
        final JPanel infoPanel = GridBagUtils.createDefaultEmptyBorderPanel();
        infoPanel.setBorder(UIUtils.createGroupBorder("Output Product Information"));
        final GridBagConstraints gbc2 = GridBagUtils.createDefaultConstraints();
        GridBagUtils.addToPanel(infoPanel, new JLabel("Scene Width:"), gbc2);
        GridBagUtils.addToPanel(infoPanel, _labelWidthInfo, gbc2);
        GridBagUtils.addToPanel(infoPanel, new JLabel("pixel"), gbc2, "weightx=1");
        GridBagUtils.addToPanel(infoPanel, new JLabel("Scene Height:"), gbc2, "gridy=1, weightx=0");
        GridBagUtils.addToPanel(infoPanel, _labelHeightInfo, gbc2);
        GridBagUtils.addToPanel(infoPanel, new JLabel("pixel"), gbc2, "weightx=1");
        GridBagUtils.addToPanel(infoPanel, new JLabel("Center Latitude:"), gbc2, "gridy=2, weightx=0");
        GridBagUtils.addToPanel(infoPanel, _labelCenterLatInfo, gbc2, "weightx=1, gridwidth=2");
        GridBagUtils.addToPanel(infoPanel, new JLabel("Center Longitude:"), gbc2, "gridy=3, weightx=0, gridwidth=1");
        GridBagUtils.addToPanel(infoPanel, _labelCenterLonInfo, gbc2, "weightx=1, gridwidth=2");
        return infoPanel;
    }

    private void createButtonsAndLabels() {
        _copyAllRButton = new JRadioButton("Copy");
        _geocodingRButton = new JRadioButton("Use Geocoding Only");
        _subsetRButton = new JRadioButton("Use Subset");

        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(_copyAllRButton);
        buttonGroup.add(_geocodingRButton);
        buttonGroup.add(_subsetRButton);

        _copyAllRButton.setActionCommand(_CopyAllCommand);
        _geocodingRButton.setActionCommand(_CopyGeocodingOnlyCommand);
        _subsetRButton.setActionCommand(_CopySubsetCommand);

        ActionListener listener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateUI();
            }
        };
        _copyAllRButton.addActionListener(listener);
        _geocodingRButton.addActionListener(listener);
        _subsetRButton.addActionListener(listener);

        if (_subsetDef != null) {
            _subsetRButton.setSelected(true);
        } else {
            _geocodingRButton.setSelected(true);
        }

        _subsetButton = new JButton("Subset...");
        _subsetButton.addActionListener(createSubsetButtonListener());

        _labelWidthInfo = new JLabel(_defaultNumberText);
        _labelHeightInfo = new JLabel(_defaultNumberText);
        _labelCenterLatInfo = new JLabel(_defaultLatLonText);
        _labelCenterLonInfo = new JLabel(_defaultLatLonText);
    }

    private void updateUI() {
        _subsetButton.setEnabled(_subsetRButton.isSelected());
        Product product = getSourceProduct();
        if (_subsetDef == null) {
            _subsetButton.setText("Define subset ...");
            if (product != null) {
                final int width = product.getSceneRasterWidth();
                final int height = product.getSceneRasterHeight();
                _labelWidthInfo.setText("" + width);
                _labelHeightInfo.setText("" + height);
                final GeoCoding geoCoding = product.getGeoCoding();
                if (geoCoding != null) {
                    final GeoPos pos = geoCoding.getGeoPos(new PixelPos(0.5f * width + 0.5f, 0.5f * height + 0.5f),
                                                           null);
                    _labelCenterLatInfo.setText(pos.getLatString());
                    _labelCenterLonInfo.setText(pos.getLonString());
                } else {
                    _labelCenterLatInfo.setText(_defaultLatLonText);
                    _labelCenterLonInfo.setText(_defaultLatLonText);
                }
            } else {
                _labelWidthInfo.setText(_defaultNumberText);
                _labelHeightInfo.setText(_defaultNumberText);
                _labelCenterLatInfo.setText(_defaultLatLonText);
                _labelCenterLonInfo.setText(_defaultLatLonText);
            }
        } else {
            _subsetButton.setText("Edit Subset...");
            final Rectangle region = _subsetDef.getRegion();
            final int subSamplingX = (int) _subsetDef.getSubSamplingX() ;
            final int subSamplingY = (int) _subsetDef.getSubSamplingY();
            _labelWidthInfo.setText("" + ((region.width - 1) / subSamplingX + 1));
            _labelHeightInfo.setText("" + ((region.height - 1) / subSamplingY + 1));
            if (product == null) {
                _labelCenterLatInfo.setText(_defaultLatLonText);
                _labelCenterLonInfo.setText(_defaultLatLonText);
            } else {
                final GeoCoding geoCoding = product.getGeoCoding();
                if (geoCoding == null) {
                    _labelCenterLatInfo.setText(_defaultLatLonText);
                    _labelCenterLonInfo.setText(_defaultLatLonText);
                } else {
                    final float centerX = 0.5f * region.width + region.x;
                    final float centerY = 0.5f * region.height + region.y;
                    final PixelPos centerPoint = new PixelPos(centerX + 0.5f, centerY + 0.5f);
                    final GeoPos pos = geoCoding.getGeoPos(centerPoint, null);
                    _labelCenterLatInfo.setText(pos.getLatString());
                    _labelCenterLonInfo.setText(pos.getLonString());
                }
            }
        }
        final boolean geocodingAvailable = product != null && product.getGeoCoding() != null;
        _geocodingRButton.setEnabled(geocodingAvailable);
        if (_geocodingRButton.isSelected() && !geocodingAvailable) {
            _subsetRButton.setSelected(true);
        }
    }

    private ActionListener createSubsetButtonListener() {
        return new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                Product product = getSourceProduct();
                if (product == null) {
                    return;
                }
                ProductSubsetDialog dialog = new ProductSubsetDialog(_parent, product, _subsetDef);
                if (dialog == null) {
                    return;
                }
                if (dialog.show() == ProductSubsetDialog.ID_OK) {
                    if (dialog.getProductSubsetDef().isEntireProductSelected()) {
                        _subsetDef = null;
                    } else {
                        _subsetDef = dialog.getProductSubsetDef();
                    }
                }
                updateUI();
            }
        };
    }

    private boolean hasPrefix() {
        return _prefix != null && _prefix.length() > 1;
    }

    private void setNewProductName() {
        final String newProductName = createNewProductName(getSourceProduct().getName());
        _paramNewName.setValue(newProductName, null);
    }

    private String createNewProductName(String sourceProductName) {
        String newNameBase = "";
        if (sourceProductName != null && sourceProductName.length() > 0) {
            newNameBase = FileUtils.exchangeExtension(sourceProductName, "");
        }
        String newNamePrefix = "product";
        if (hasPrefix()) {
            newNamePrefix = _prefix;
        }
        String newProductName;
        if (newNameBase.length() > 0) {
            newProductName = newNamePrefix + "_" + _numNewProducts + "_" + newNameBase;
        } else {
            newProductName = newNamePrefix + "_" + _numNewProducts;
        }
        return newProductName;
    }
}
