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

import org.esa.beam.framework.dataio.ProductFlipper;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductNodeNameValidator;
import org.esa.beam.framework.param.Parameter;
import org.esa.beam.framework.ui.GridBagUtils;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.util.Guardian;

import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.text.JTextComponent;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.Window;
import java.io.IOException;

public class ProductFlipDialog extends ModalDialog {

    private Parameter _paramNewName;
    private Parameter _paramNewDesc;

    private final Product _sourceProduct;
    private Product _resultProduct;
    private Exception _exception;

    private static int _numNewFlippings = 0;

    private JLabel _labelWidthInfo;
    private JLabel _labelHeightInfo;
    private JLabel _labelCenterLatInfo;
    private JLabel _labelCenterLonInfo;
    private static final String _defaultNumberText = "####";
    private static final String _defaultLatLonText = "##°/##°";

    private JRadioButton _buttonHorizontal;
    private JRadioButton _buttonVertical;
    private JRadioButton _buttonBoth;

    public ProductFlipDialog(Window parent, Product sourceProduct) {
        super(parent, "Data Flip", ModalDialog.ID_OK_CANCEL_HELP, "dataFlip"); /* I18N */
        Guardian.assertNotNull("sourceProduct", sourceProduct);
        _sourceProduct = sourceProduct;
    }

    @Override
    public int show() {
        createParameter();
        createUI();
        updateUI();
        return super.show();
    }

    public Product getSourceProduct() {
        return _sourceProduct;
    }

    public Product getResultProduct() {
        return _resultProduct;
    }

    public Exception getException() {
        return _exception;
    }

    @Override
    protected void onCancel() {
        super.onCancel();
        _resultProduct = null;
    }

    @Override
    protected void onOK() {
        super.onOK();
        String prodName = _paramNewName.getValueAsText();
        String prodDesc = _paramNewDesc.getValueAsText();
        int flipType = 0;
        if (_buttonHorizontal.isSelected()) {
            flipType = ProductFlipper.FLIP_HORIZONTAL;
        } else if (_buttonVertical.isSelected()) {
            flipType = ProductFlipper.FLIP_VERTICAL;
        } else if (_buttonBoth.isSelected()) {
            flipType = ProductFlipper.FLIP_BOTH;
        }
        _resultProduct = null;
        try {
            _resultProduct = getSourceProduct().createFlippedProduct(flipType, prodName, prodDesc);
        } catch (IOException e) {
            _exception = e;
        }
    }

    @Override
    protected boolean verifyUserInput() {
        boolean b = super.verifyUserInput();
        String name = _paramNewName.getValueAsText();
        return b && (name != null && name.length() > 0);
    }

    private void createParameter() {
        _numNewFlippings++;

        _paramNewName = new Parameter("productName",
                                      "flip_" + _numNewFlippings + "_"
                                      + getSourceProduct().getName());
        _paramNewName.getProperties().setLabel("Name"); /* I18N */
        _paramNewName.getProperties().setNullValueAllowed(false);
        _paramNewName.getProperties().setValidatorClass(ProductNodeNameValidator.class);

        _paramNewDesc = new Parameter("productDesc", getSourceProduct().getDescription());
        _paramNewDesc.getProperties().setLabel("Description"); /* I18N */
        _paramNewDesc.getProperties().setNullValueAllowed(false);
    }

    private void createUI() {
        _buttonHorizontal = new JRadioButton("horizontally");
        _buttonVertical = new JRadioButton("vertically");
        _buttonBoth = new JRadioButton("horizontally & vertically");
        final ButtonGroup group = new ButtonGroup();
        group.add(_buttonHorizontal);
        group.add(_buttonVertical);
        group.add(_buttonBoth);

        _buttonHorizontal.setSelected(true);

        int line = 0;
        JPanel dialogPane = GridBagUtils.createPanel();
        final GridBagConstraints gbc = GridBagUtils.createDefaultConstraints();

        gbc.gridy = ++line;
        GridBagUtils.addToPanel(dialogPane, new JLabel("Input Product:"), gbc, "fill=BOTH, gridwidth=4");

        gbc.gridy = ++line;
        GridBagUtils.addToPanel(dialogPane, new JLabel(getSourceProduct().getDisplayName()), gbc);

        gbc.gridy = ++line;
        GridBagUtils.addToPanel(dialogPane, getNameAndDescPanel(), gbc, "insets.top=15");

        gbc.gridy = ++line;
        GridBagUtils.addToPanel(dialogPane, new JLabel("Flip Data "), gbc, "gridwidth=1");
        GridBagUtils.addToPanel(dialogPane, _buttonHorizontal, gbc);
        GridBagUtils.addToPanel(dialogPane, _buttonVertical, gbc);
        GridBagUtils.addToPanel(dialogPane, _buttonBoth, gbc);

        gbc.gridy = ++line;
        GridBagUtils.addToPanel(dialogPane, createInfoPanel(), gbc, "gridwidth=4");

        setContent(dialogPane);
    }

    private JPanel getNameAndDescPanel() {
        int line = 0;
        final JPanel nameAndDescPanel = GridBagUtils.createPanel();
        final GridBagConstraints gbc = GridBagUtils.createDefaultConstraints();
        GridBagUtils.setAttributes(gbc, "insets.top=3, fill=HORIZONTAL");
        gbc.gridy = ++line;
        GridBagUtils.addToPanel(nameAndDescPanel, new JLabel("Output Product"), gbc, "weightx=1, gridwidth=2");
        gbc.gridy = ++line;
        GridBagUtils.addToPanel(nameAndDescPanel, _paramNewName.getEditor().getLabelComponent(), gbc, "gridwidth=1");
        GridBagUtils.addToPanel(nameAndDescPanel, _paramNewName.getEditor().getComponent(), gbc, "weightx=999");
        gbc.gridy = ++line;
        GridBagUtils.addToPanel(nameAndDescPanel, _paramNewDesc.getEditor().getLabelComponent(), gbc, "weightx=1");
        GridBagUtils.addToPanel(nameAndDescPanel, _paramNewDesc.getEditor().getComponent(), gbc, "weightx=999");

        JComponent jComponent = _paramNewName.getEditor().getEditorComponent();
        if (jComponent instanceof JTextComponent) {
            JTextComponent tc = (JTextComponent) jComponent;
            tc.selectAll();
        }
        return nameAndDescPanel;
    }

    private JPanel createInfoPanel() {
        _labelWidthInfo = new JLabel(_defaultNumberText);
        _labelHeightInfo = new JLabel(_defaultNumberText);
        _labelCenterLatInfo = new JLabel(_defaultLatLonText);
        _labelCenterLonInfo = new JLabel(_defaultLatLonText);

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

    private void updateUI() {
        Product product = getSourceProduct();
        Dimension outputRasterSize = new Dimension(product.getSceneRasterWidth(), product.getSceneRasterHeight());
        // @todo 1 nf/se - although this code looks stupid,
        // it will help us to derive a base class from this class
        // which can then be used by the MapProjectionDialog too
        if (outputRasterSize != null) {
            final int width = outputRasterSize.width;
            final int height = outputRasterSize.height;
            _labelWidthInfo.setText("" + width);
            _labelHeightInfo.setText("" + height);
            final GeoCoding geoCoding = product.getGeoCoding();
            if (geoCoding != null) {
                final GeoPos pos = geoCoding.getGeoPos(new PixelPos(0.5f * width + 0.5f, 0.5f * height + 0.5f), null);
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
    }
}
