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
package org.esa.beam.visat.toolviews.bitmask;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.Window;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.text.JTextComponent;

import org.esa.beam.framework.param.ParamGroup;
import org.esa.beam.framework.param.ParamProperties;
import org.esa.beam.framework.param.Parameter;
import org.esa.beam.framework.ui.GridBagUtils;
import org.esa.beam.framework.ui.ModalDialog;

public class BitmaskDefEditDialog extends ModalDialog {

    private ParamGroup _paramGroup;
    private Parameter _bitmaskNameParam;
    private Parameter _bitmaskDescParam;
    private Parameter _bitmaskExprParam;
    private Parameter _bitmaskColorParam;
    private Parameter _bitmaskTransparencyParam;

    public BitmaskDefEditDialog(Window window, String title) {
        super(window, title, ID_OK_CANCEL, null); /*I18N*/

        _bitmaskNameParam = new Parameter("bitmaskName", "unnamed");
        _bitmaskNameParam.getProperties().setLabel("Name"); /*I18N*/
        _bitmaskNameParam.getProperties().setDescription("The name of this bitmask definition"); /*I18N*/
        _bitmaskNameParam.getProperties().setNullValueAllowed(false); /*I18N*/
        _bitmaskNameParam.getProperties().setPropertyValue(ParamProperties.IDENTIFIERSONLY_KEY, true); /*I18N*/

        _bitmaskDescParam = new Parameter("bitmaskDesc");
        _bitmaskDescParam.getProperties().setValueType(String.class);
        _bitmaskDescParam.getProperties().setLabel("Description"); /*I18N*/
        _bitmaskDescParam.getProperties().setDescription("The description of this bitmask definition"); /*I18N*/
        _bitmaskDescParam.getProperties().setNullValueAllowed(true); /*I18N*/

        _bitmaskExprParam = new Parameter("bitmaskExpr", "");
        _bitmaskExprParam.getProperties().setLabel("Expression"); /*I18N*/
        _bitmaskExprParam.getProperties().setDescription("Bitmask expression"); /*I18N*/
        _bitmaskExprParam.getProperties().setPropertyValue(ParamProperties.EDITORCLASS_KEY,
                                                           "org.esa.beam.framework.ui.product.BitmaskExprEditor");
        _bitmaskExprParam.getProperties().setPropertyValue(ParamProperties.VALIDATORCLASS_KEY,
                                                           "org.esa.beam.framework.ui.validators.BitmaskExprValidator");

        _bitmaskColorParam = new Parameter("bitmaskColor", Color.red);
        _bitmaskColorParam.getProperties().setLabel("Bitmask overlay colour"); /*I18N*/
        _bitmaskColorParam.getProperties().setDescription("Bitmask overlay colour"); /*I18N*/

        _bitmaskTransparencyParam = new Parameter("bitmaskTransparency", new Float(0.5));
        _bitmaskTransparencyParam.getProperties().setMinValue(new Float(0.0));
        _bitmaskTransparencyParam.getProperties().setMaxValue(new Float(0.95));
        _bitmaskTransparencyParam.getProperties().setLabel("Bitmask tranparency"); /*I18N*/
        _bitmaskTransparencyParam.getProperties().setDescription(
                "Bitmask tranparency: 0 is opaque, 1 is fully transparent"); /*I18N*/

        _paramGroup = new ParamGroup();
        _paramGroup.addParameter(_bitmaskNameParam);
        _paramGroup.addParameter(_bitmaskExprParam);
        _paramGroup.addParameter(_bitmaskColorParam);
        _paramGroup.addParameter(_bitmaskTransparencyParam);

        createUI();
    }

    public Parameter getBitmaskNameParam() {
        return _bitmaskNameParam;
    }

    public Parameter getBitmaskExprParam() {
        return _bitmaskExprParam;
    }

    public Parameter getBitmaskColorParam() {
        return _bitmaskColorParam;
    }

    public Parameter getBitmaskTransparencyParam() {
        return _bitmaskTransparencyParam;
    }

    public Parameter getBitmaskDescParam() {
        return _bitmaskDescParam;
    }

    private void createUI() {
        GridBagConstraints gbc;

        JPanel p1 = GridBagUtils.createPanel();
        gbc = GridBagUtils.createConstraints("gridwidth=2, anchor=WEST, fill=HORIZONTAL, weightx=1");

        int insetsTopDefault = gbc.insets.top;
        gbc.gridy++;
        GridBagUtils.addToPanel(p1, _bitmaskNameParam.getEditor().getLabelComponent(), gbc);
        gbc.gridy++;
        GridBagUtils.addToPanel(p1, _bitmaskNameParam.getEditor().getComponent(), gbc);

        gbc.gridy++;
        gbc.insets.top = 7;
        GridBagUtils.addToPanel(p1, _bitmaskDescParam.getEditor().getLabelComponent(), gbc);
        gbc.gridy++;
        gbc.insets.top = insetsTopDefault;
        GridBagUtils.addToPanel(p1, _bitmaskDescParam.getEditor().getComponent(), gbc);

        gbc.gridy++;
        gbc.insets.top = 7;
        GridBagUtils.addToPanel(p1, _bitmaskExprParam.getEditor().getLabelComponent(), gbc);
        gbc.gridy++;
        gbc.insets.top = insetsTopDefault;
        GridBagUtils.addToPanel(p1, _bitmaskExprParam.getEditor().getComponent(), gbc);

        gbc.gridy++;
        gbc.insets.top = 14;
        GridBagUtils.addToPanel(p1, _bitmaskColorParam.getEditor().getLabelComponent(), gbc, "weightx=0, gridwidth=1");
        GridBagUtils.addToPanel(p1, _bitmaskColorParam.getEditor().getComponent(), gbc, "weightx=1, anchor=EAST");

        gbc.gridy++;
        gbc.insets.top = 7;
        GridBagUtils.addToPanel(p1, _bitmaskTransparencyParam.getEditor().getLabelComponent(), gbc,
                                "weightx=0, anchor=WEST");
        GridBagUtils.addToPanel(p1, _bitmaskTransparencyParam.getEditor().getComponent(), gbc,
                                "weightx=1, anchor=EAST");

        setContent(p1);

        final JComponent editorComponent = _bitmaskNameParam.getEditor().getEditorComponent();
        if (editorComponent instanceof JTextComponent) {
            JTextComponent tf = (JTextComponent) editorComponent;
            tf.selectAll();
        }
    }
}
