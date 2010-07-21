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
package org.esa.beam.framework.ui;

import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.ProductNodeNameValidator;
import org.esa.beam.framework.param.ParamProperties;
import org.esa.beam.framework.param.Parameter;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.StringUtils;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.text.JTextComponent;
import java.awt.GridBagConstraints;
import java.awt.Window;

//@todo 1 se/** - add (more) class documentation

public class NewBandDialog extends ModalDialog {

    private Parameter _paramName;
    private Parameter _paramDesc;
    private Parameter _paramUnit;
    private Parameter _paramDataType;
    private Product _currentProduct;

    private static int _numNewBands = 0;

    public NewBandDialog(final Window parent, Product product) {
        super(parent, "New Band", ModalDialog.ID_OK_CANCEL, null); /* I18N */

        Guardian.assertNotNull("product", product);
        _currentProduct = product;

        createParameter();

        createUI();
    }

    public String getNewBandsName() {
        return _paramName.getValueAsText();
    }

    public String getNewBandsDesc() {
        return _paramDesc.getValueAsText();
    }

    public String getNewBandsUnit() {
        return _paramUnit.getValueAsText();
    }

    public int getNewBandsDataType() {
        return ProductData.getType(_paramDataType.getValueAsText());
    }

    @Override
    protected boolean verifyUserInput() {
        String name = _paramName.getValueAsText();
        if (name == null || name.length() == 0) {
            showWarningDialog("The field '" + _paramName.getProperties().getLabel() + "' must not be empty"); /*I18N*/
            return false;
        }
        if (StringUtils.contains(_currentProduct.getBandNames(), name)) {
            showErrorDialog("A band with the name '" + name + "' already exists.\n"
                            + "Please choose a another one."); /*I18N*/
            return false;
        }
        return super.verifyUserInput();
    }

    private void createParameter() {
        _numNewBands++;

        ParamProperties paramProps;

        paramProps = new ParamProperties(String.class, "new_band_" + _numNewBands);
        paramProps.setLabel("Name"); /* I18N */
        paramProps.setDescription("The name for the new band."); /*I18N*/
        paramProps.setNullValueAllowed(false);
        paramProps.setValidatorClass(ProductNodeNameValidator.class);
        _paramName = new Parameter("bandName", paramProps);

        paramProps = new ParamProperties(String.class, "");
        paramProps.setLabel("Description"); /* I18N */
        paramProps.setDescription("The description for the new band.");  /*I18N*/
        paramProps.setNullValueAllowed(true);
        _paramDesc = new Parameter("bandDesc", paramProps);

        paramProps = new ParamProperties(String.class, "");
        paramProps.setLabel("Unit"); /* I18N */
        paramProps.setDescription("The physical unit for the new band."); /*I18N*/
        paramProps.setNullValueAllowed(true);
        _paramUnit = new Parameter("bandUnit", paramProps);

        paramProps = new ParamProperties(String.class, ProductData.TYPESTRING_FLOAT32,
                                         new String[]{ProductData.TYPESTRING_FLOAT32});
        paramProps.setLabel("Data type"); /* I18N */
        paramProps.setDescription("The data type for the new band."); /*I18N*/
        paramProps.setValueSetBound(true);
        _paramDataType = new Parameter("bandDataType", paramProps);
    }

    private void createUI() {
        final JPanel dialogPane = GridBagUtils.createPanel();
        final GridBagConstraints gbc = GridBagUtils.createDefaultConstraints();
        int line = 0;
        GridBagUtils.addToPanel(dialogPane, _paramName.getEditor().getLabelComponent(), gbc,
                                "fill=BOTH, weightx=1, insets.top=3");
        GridBagUtils.addToPanel(dialogPane, _paramName.getEditor().getComponent(), gbc);

        GridBagUtils.addToPanel(dialogPane, _paramDesc.getEditor().getLabelComponent(), gbc, "gridy=" + ++line);
        GridBagUtils.addToPanel(dialogPane, _paramDesc.getEditor().getComponent(), gbc);

        GridBagUtils.addToPanel(dialogPane, _paramUnit.getEditor().getLabelComponent(), gbc, "gridy=" + ++line);
        GridBagUtils.addToPanel(dialogPane, _paramUnit.getEditor().getComponent(), gbc);

        GridBagUtils.addToPanel(dialogPane, _paramDataType.getEditor().getLabelComponent(), gbc, "gridy=" + ++line);
        GridBagUtils.addToPanel(dialogPane, _paramDataType.getEditor().getComponent(), gbc);
        _paramDataType.setUIEnabled(false);

        GridBagUtils.addToPanel(dialogPane, createInfoPanel(), gbc, "gridy=" + ++line + ", insets.top=10, gridwidth=2");
        setContent(dialogPane);

        final JComponent editorComponent = _paramName.getEditor().getEditorComponent();
        if (editorComponent instanceof JTextComponent) {
            final JTextComponent tc = (JTextComponent) editorComponent;
            tc.selectAll();
            tc.requestFocus();
        }
    }

    private JPanel createInfoPanel() {
        final JLabel parentProductLabel = new JLabel(_currentProduct.getDisplayName());
        final JLabel widthValueLabel = new JLabel("" + _currentProduct.getSceneRasterWidth() + " pixel");
        final JLabel heightValueLabel = new JLabel("" + _currentProduct.getSceneRasterHeight() + " pixel");
        final JLabel centerValueLatLabel = new JLabel("##°");
        final JLabel centerValueLonLabel = new JLabel("##°");
        final GeoCoding geoCoding = _currentProduct.getGeoCoding();
        if (geoCoding != null) {
            final float centerX = 0.5f * _currentProduct.getSceneRasterWidth();
            final float centerY = 0.5f * _currentProduct.getSceneRasterHeight();
            final GeoPos pos = geoCoding.getGeoPos(new PixelPos(centerX + 0.5f, centerY + 0.5f), null);
            centerValueLatLabel.setText(pos.getLatString());
            centerValueLonLabel.setText(pos.getLonString());
        }

        final JPanel infoPanel = GridBagUtils.createDefaultEmptyBorderPanel();
        infoPanel.setBorder(UIUtils.createGroupBorder("Band Info"));
        int line = 0;
        final GridBagConstraints gbc = GridBagUtils.createDefaultConstraints();
        GridBagUtils.addToPanel(infoPanel, new JLabel("Parent Product:"), gbc, "weightx=0, gridy=" + ++line);
        GridBagUtils.addToPanel(infoPanel, parentProductLabel, gbc, "weightx=1");
        GridBagUtils.addToPanel(infoPanel, new JLabel("Raster Width:"), gbc, "weightx=0, gridy=" + ++line);
        GridBagUtils.addToPanel(infoPanel, widthValueLabel, gbc, "weightx=1");
        GridBagUtils.addToPanel(infoPanel, new JLabel("Raster Height:"), gbc, "weightx=0, gridy=" + ++line);
        GridBagUtils.addToPanel(infoPanel, heightValueLabel, gbc, "weightx=1");
        GridBagUtils.addToPanel(infoPanel, new JLabel("Center Latitude:"), gbc, "weightx=0, gridy=" + ++line);
        GridBagUtils.addToPanel(infoPanel, centerValueLatLabel, gbc, "weightx=1");
        GridBagUtils.addToPanel(infoPanel, new JLabel("Center Longitude:"), gbc, "weightx=0, gridy=" + ++line);
        GridBagUtils.addToPanel(infoPanel, centerValueLonLabel, gbc, "weightx=1");
        return infoPanel;
    }
}
