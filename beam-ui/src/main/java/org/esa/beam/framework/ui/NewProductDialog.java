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
package org.esa.beam.framework.ui;

import org.esa.beam.framework.dataio.ProductSubsetBuilder;
import org.esa.beam.framework.dataio.ProductSubsetDef;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.param.ParamChangeEvent;
import org.esa.beam.framework.param.ParamChangeListener;
import org.esa.beam.framework.param.Parameter;
import org.esa.beam.framework.ui.product.ProductSubsetDialog;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.io.FileUtils;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

//@todo 1 se/** - add (more) class documentation

public class NewProductDialog extends ModalDialog {

    private static final String DEFAULT_NUMBER_TEXT = "####";
    private static final String DEFAULT_LATLON_TEXT = "##°";

    private static final String COPY_ALL_COMMAND = "all";
    private static final String COPY_GEOCODING_COMMAND = "geo";
    private static final String COPY_SUBSET_COMMAND = "sub";

    private static int numNewProducts = 0;

    private Parameter paramNewName;
    private Parameter paramNewDesc;
    private Parameter paramSourceProduct;

    private final ProductNodeList sourceProducts;
    private final Window parent;
    private String prefix;
    private Exception exception;
    private boolean sourceProductOwner;

    private ProductSubsetDef subsetDef;
    private Product editProduct; // currently not used, but possibly later for product editing
    private Product resultProduct;

    private JButton subsetButton;
    private JRadioButton copyAllRButton;
    private JRadioButton geocodingRButton;
    private JRadioButton subsetRButton;

    private JLabel labelWidthInfo;
    private JLabel labelHeightInfo;
    private JLabel labelCenterLatInfo;
    private JLabel labelCenterLonInfo;
    private int selectedProductIndex;

    public NewProductDialog(Window parent,
                            ProductNodeList<Product> sourceProducts,
                            final int selectedSourceIndex,
                            boolean sourceProductOwner) {
        this(parent, sourceProducts, selectedSourceIndex, sourceProductOwner, "subset");
    }

    public NewProductDialog(Window parent,
                            ProductNodeList<Product> sourceProducts,
                            final int selectedSourceIndex, boolean sourceProductOwner,
                            String prefix) {
        super(parent, "New Product", ModalDialog.ID_OK_CANCEL, null); /* I18N */
        Guardian.assertNotNull("sourceProducts", sourceProducts);
        Guardian.assertGreaterThan("sourceProducts.size()", sourceProducts.size(), 0);
        this.sourceProducts = sourceProducts;
        selectedProductIndex = selectedSourceIndex;
        this.parent = parent;
        this.prefix = prefix;
        this.sourceProductOwner = sourceProductOwner;
    }

    @Override
    public int show() {
        createParameter();
        createUI();
        updateUI();
        return super.show();
    }

    public void setSubsetDef(ProductSubsetDef subsetDef) {
        this.subsetDef = subsetDef;
    }

//    public void setProductToEdit(Product product) {
//        _editProduct = product;
//    }

    public boolean isSourceProductOwner() {
        return sourceProductOwner;
    }

    public Product getSourceProduct() {
        String displayName = paramSourceProduct.getValueAsText();
        return (Product) sourceProducts.getByDisplayName(displayName);
    }

    public Product getResultProduct() {
        return resultProduct;
    }

    @Override
    protected void onCancel() {
        super.onCancel();
        resultProduct = null;
    }

    @Override
    protected void onOK() {
        super.onOK();
        String prodName = paramNewName.getValueAsText();
        String prodDesc = paramNewDesc.getValueAsText();
        Product sourceProduct = getSourceProduct();
        resultProduct = null;
        try {
            if (geocodingRButton.isSelected()) {
                ProductSubsetDef def = new ProductSubsetDef();
                def.addNodeName("latitude");
                def.addNodeName("longitude");
                def.addNodeName(Product.HISTORY_ROOT_NAME);
                resultProduct = ProductSubsetBuilder.createProductSubset(sourceProduct, sourceProductOwner, def,
                                                                         prodName, prodDesc);
                // @todo 1 nf/** - check: do we really need the following code or is it done in the  ProductSubsetBuilder?
                TiePointGrid latGrid = resultProduct.getTiePointGrid("latitude");
                TiePointGrid lonGrid = resultProduct.getTiePointGrid("longitude");
                if (latGrid != null && lonGrid != null) {
                    resultProduct.setGeoCoding(
                            new TiePointGeoCoding(latGrid, lonGrid, sourceProduct.getGeoCoding().getDatum()));
                }
            } else if (subsetDef != null && subsetRButton.isSelected()) {
                resultProduct = ProductSubsetBuilder.createProductSubset(sourceProduct, sourceProductOwner,
                                                                         subsetDef, prodName, prodDesc);
            } else {
                resultProduct = ProductSubsetBuilder.createProductSubset(sourceProduct, sourceProductOwner, null,
                                                                         prodName, prodDesc);
            }
        } catch (Exception e) {
            exception = e;
        }
    }

    public Exception getException() {
        return exception;
    }

    @Override
    protected boolean verifyUserInput() {
        boolean b = super.verifyUserInput();
        String name = paramNewName.getValueAsText();
        boolean bName = name != null && name.length() > 0;
        boolean subset = true;
        if (subsetRButton.isSelected() && subsetDef == null) {
            subset = false;
            showErrorDialog("Please define a valid spatial or spectral subset."); /*I18N*/
        }
        return b && bName && subset;
    }

    private void createParameter() {
        numNewProducts++;

        String prodName;
        String prodDesc;
        String[] valueSet;
        boolean enableSourceProduct;
        if (editProduct == null) {
            valueSet = sourceProducts.getDisplayNames();
            Product product = (Product) sourceProducts.getAt(selectedProductIndex);
            prodName = createNewProductName(valueSet.length > 0 ? product.getName() : "");
            prodDesc = "";
            enableSourceProduct = true;
        } else {
            prodName = editProduct.getName();
            prodDesc = editProduct.getDescription();
            valueSet = new String[]{prodName};
            enableSourceProduct = false;
        }

        paramNewName = new Parameter("productName", prodName);
        paramNewName.getProperties().setLabel("Name"); /* I18N */
        paramNewName.getProperties().setNullValueAllowed(false);
        paramNewName.getProperties().setValidatorClass(ProductNodeNameValidator.class);

        paramNewDesc = new Parameter("productDesc", prodDesc);
        paramNewDesc.getProperties().setLabel("Description"); /* I18N */
        paramNewDesc.getProperties().setNullValueAllowed(false);

        paramSourceProduct = new Parameter("sourceProduct", valueSet[selectedProductIndex]);
        paramSourceProduct.getProperties().setValueSet(valueSet);
        paramSourceProduct.getProperties().setLabel("Derive from Product"); /* I18N */
        paramSourceProduct.getProperties().setValueSetBound(true);
        paramSourceProduct.setUIEnabled(enableSourceProduct);
        paramSourceProduct.addParamChangeListener(new ParamChangeListener() {

            public void parameterValueChanged(ParamChangeEvent event) {
                subsetDef = null;
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
        GridBagUtils.addToPanel(dialogPane, paramNewName.getEditor().getLabelComponent(), gbc,
                                "fill=BOTH, weightx=0, insets.top=3");
        GridBagUtils.addToPanel(dialogPane, paramNewName.getEditor().getComponent(), gbc, "weightx=1, gridwidth=3");

        gbc.gridy = ++line;
        GridBagUtils.addToPanel(dialogPane, paramNewDesc.getEditor().getLabelComponent(), gbc,
                                "weightx=0, gridwidth=1");
        GridBagUtils.addToPanel(dialogPane, paramNewDesc.getEditor().getComponent(), gbc, "weightx=1, gridwidth=3");

        gbc.gridy = ++line;
        GridBagUtils.addToPanel(dialogPane, paramSourceProduct.getEditor().getLabelComponent(), gbc,
                                "fill=NONE, gridwidth=4, insets.top=15");
        gbc.gridy = ++line;
        GridBagUtils.addToPanel(dialogPane, paramSourceProduct.getEditor().getComponent(), gbc,
                                "fill=HORIZONTAL, insets.top=3");
        gbc.gridy = ++line;
        final JPanel radioPanel = new JPanel(new BorderLayout());
        radioPanel.add(copyAllRButton, BorderLayout.WEST);
        radioPanel.add(geocodingRButton);
        GridBagUtils.addToPanel(dialogPane, radioPanel, gbc, "fill=NONE, gridwidth=2");
        GridBagUtils.addToPanel(dialogPane, subsetRButton, gbc, "gridwidth=1, weightx=300, anchor=EAST");
        GridBagUtils.addToPanel(dialogPane, subsetButton, gbc, "fill=NONE, weightx=1, anchor=EAST");

        gbc.gridy = ++line;
        GridBagUtils.addToPanel(dialogPane, createInfoPanel(), gbc,
                                "fill=BOTH, anchor=WEST, insets.top=10, gridwidth=4");

        setContent(dialogPane);

        final JComponent editorComponent = paramNewName.getEditor().getEditorComponent();
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
        GridBagUtils.addToPanel(infoPanel, labelWidthInfo, gbc2);
        GridBagUtils.addToPanel(infoPanel, new JLabel("pixel"), gbc2, "weightx=1");
        GridBagUtils.addToPanel(infoPanel, new JLabel("Scene Height:"), gbc2, "gridy=1, weightx=0");
        GridBagUtils.addToPanel(infoPanel, labelHeightInfo, gbc2);
        GridBagUtils.addToPanel(infoPanel, new JLabel("pixel"), gbc2, "weightx=1");
        GridBagUtils.addToPanel(infoPanel, new JLabel("Center Latitude:"), gbc2, "gridy=2, weightx=0");
        GridBagUtils.addToPanel(infoPanel, labelCenterLatInfo, gbc2, "weightx=1, gridwidth=2");
        GridBagUtils.addToPanel(infoPanel, new JLabel("Center Longitude:"), gbc2, "gridy=3, weightx=0, gridwidth=1");
        GridBagUtils.addToPanel(infoPanel, labelCenterLonInfo, gbc2, "weightx=1, gridwidth=2");
        return infoPanel;
    }

    private void createButtonsAndLabels() {
        copyAllRButton = new JRadioButton("Copy");
        geocodingRButton = new JRadioButton("Use Geocoding Only");
        subsetRButton = new JRadioButton("Use Subset");

        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(copyAllRButton);
        buttonGroup.add(geocodingRButton);
        buttonGroup.add(subsetRButton);

        copyAllRButton.setActionCommand(COPY_ALL_COMMAND);
        geocodingRButton.setActionCommand(COPY_GEOCODING_COMMAND);
        subsetRButton.setActionCommand(COPY_SUBSET_COMMAND);

        ActionListener listener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateUI();
            }
        };
        copyAllRButton.addActionListener(listener);
        geocodingRButton.addActionListener(listener);
        subsetRButton.addActionListener(listener);

        if (subsetDef != null) {
            subsetRButton.setSelected(true);
        } else {
            geocodingRButton.setSelected(true);
        }

        subsetButton = new JButton("Subset...");
        subsetButton.addActionListener(createSubsetButtonListener());

        labelWidthInfo = new JLabel(DEFAULT_NUMBER_TEXT);
        labelHeightInfo = new JLabel(DEFAULT_NUMBER_TEXT);
        labelCenterLatInfo = new JLabel(DEFAULT_LATLON_TEXT);
        labelCenterLonInfo = new JLabel(DEFAULT_LATLON_TEXT);
    }

    private void updateUI() {
        Product product = getSourceProduct();
        if (subsetDef == null) {
            subsetButton.setText("Define subset ...");
            if (product != null) {
                final int width = product.getSceneRasterWidth();
                final int height = product.getSceneRasterHeight();
                labelWidthInfo.setText("" + width);
                labelHeightInfo.setText("" + height);
                final GeoCoding geoCoding = product.getGeoCoding();
                if (geoCoding != null) {
                    final GeoPos pos = geoCoding.getGeoPos(new PixelPos(0.5f * width + 0.5f, 0.5f * height + 0.5f),
                                                           null);
                    labelCenterLatInfo.setText(pos.getLatString());
                    labelCenterLonInfo.setText(pos.getLonString());
                } else {
                    labelCenterLatInfo.setText(DEFAULT_LATLON_TEXT);
                    labelCenterLonInfo.setText(DEFAULT_LATLON_TEXT);
                }
            } else {
                labelWidthInfo.setText(DEFAULT_NUMBER_TEXT);
                labelHeightInfo.setText(DEFAULT_NUMBER_TEXT);
                labelCenterLatInfo.setText(DEFAULT_LATLON_TEXT);
                labelCenterLonInfo.setText(DEFAULT_LATLON_TEXT);
            }
        } else {
            subsetButton.setText("Edit Subset...");
            final Rectangle region = subsetDef.getRegion();
            final int subSamplingX = subsetDef.getSubSamplingX();
            final int subSamplingY = subsetDef.getSubSamplingY();
            labelWidthInfo.setText("" + ((region.width - 1) / subSamplingX + 1));
            labelHeightInfo.setText("" + ((region.height - 1) / subSamplingY + 1));
            if (product == null) {
                labelCenterLatInfo.setText(DEFAULT_LATLON_TEXT);
                labelCenterLonInfo.setText(DEFAULT_LATLON_TEXT);
            } else {
                final GeoCoding geoCoding = product.getGeoCoding();
                if (geoCoding == null) {
                    labelCenterLatInfo.setText(DEFAULT_LATLON_TEXT);
                    labelCenterLonInfo.setText(DEFAULT_LATLON_TEXT);
                } else {
                    final float centerX = 0.5f * region.width + region.x;
                    final float centerY = 0.5f * region.height + region.y;
                    final PixelPos centerPoint = new PixelPos(centerX + 0.5f, centerY + 0.5f);
                    final GeoPos pos = geoCoding.getGeoPos(centerPoint, null);
                    labelCenterLatInfo.setText(pos.getLatString());
                    labelCenterLonInfo.setText(pos.getLonString());
                }
            }
        }
        final boolean geocodingAvailable = product != null && product.getGeoCoding() != null;
        geocodingRButton.setEnabled(geocodingAvailable);
        if (geocodingRButton.isSelected() && !geocodingAvailable) {
            subsetRButton.setSelected(true);
        }
        subsetButton.setEnabled(subsetRButton.isSelected());
    }

    private ActionListener createSubsetButtonListener() {
        return new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                Product product = getSourceProduct();
                if (product == null) {
                    return;
                }
                ProductSubsetDialog dialog = new ProductSubsetDialog(parent, product, subsetDef);
                if (dialog.show() == ProductSubsetDialog.ID_OK) {
                    if (dialog.getProductSubsetDef().isEntireProductSelected()) {
                        subsetDef = null;
                    } else {
                        subsetDef = dialog.getProductSubsetDef();
                    }
                }
                updateUI();
            }
        };
    }

    private boolean hasPrefix() {
        return prefix != null && prefix.length() > 1;
    }

    private void setNewProductName() {
        final String newProductName = createNewProductName(getSourceProduct().getName());
        paramNewName.setValue(newProductName, null);
    }

    private String createNewProductName(String sourceProductName) {
        String newNameBase = "";
        if (sourceProductName != null && sourceProductName.length() > 0) {
            newNameBase = FileUtils.exchangeExtension(sourceProductName, "");
        }
        String newNamePrefix = "product";
        if (hasPrefix()) {
            newNamePrefix = prefix;
        }
        String newProductName;
        if (newNameBase.length() > 0) {
            newProductName = newNamePrefix + "_" + numNewProducts + "_" + newNameBase;
        } else {
            newProductName = newNamePrefix + "_" + numNewProducts;
        }
        return newProductName;
    }
}
