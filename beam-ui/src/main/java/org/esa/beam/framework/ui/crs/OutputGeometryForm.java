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
package org.esa.beam.framework.ui.crs;

import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.swing.binding.BindingContext;
import com.bc.ceres.swing.binding.PropertyEditor;
import com.bc.ceres.swing.binding.PropertyEditorRegistry;

import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.GridBagUtils;
import org.geotools.referencing.CRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import java.awt.Container;
import java.awt.GridBagConstraints;

/**
 * @author Marco Zuehlke
 * @since BEAM 4.7
 */
public class OutputGeometryForm extends JPanel {
    
    private final BindingContext context;

    public OutputGeometryForm(OutputGeometryFormModel model) {
        context = new BindingContext(model.getPropertyContainer());
        createUI();
    }

    private void createUI() {
        int line = 0;
        JPanel dialogPane = GridBagUtils.createPanel();
        dialogPane.setBorder(new EmptyBorder(7, 7, 7, 7));
        final GridBagConstraints gbc = GridBagUtils.createDefaultConstraints();
        GridBagUtils.setAttributes(gbc, "insets.top=0,gridwidth=3");

        JRadioButton pixelRefULeftButton = new JRadioButton("Reference pixel is at scene upper left", false);
        JRadioButton pixelRefCenterButton = new JRadioButton("Reference pixel is at scene center", false);
        JRadioButton pixelRefOtherButton = new JRadioButton("Other reference pixel position", false);
        ButtonGroup g = new ButtonGroup();
        g.add(pixelRefULeftButton);
        g.add(pixelRefCenterButton);
        g.add(pixelRefOtherButton);
        context.bind("referencePixelLocation", g);
        context.bindEnabledState("referencePixelX", true, "referencePixelLocation", 2);
        context.bindEnabledState("referencePixelY", true, "referencePixelLocation", 2);

        gbc.gridy = ++line;
        GridBagUtils.addToPanel(dialogPane, pixelRefULeftButton, gbc, "fill=HORIZONTAL,weightx=1");
        gbc.gridy = ++line;
        GridBagUtils.addToPanel(dialogPane, pixelRefCenterButton, gbc);
        gbc.gridy = ++line;
        GridBagUtils.addToPanel(dialogPane, pixelRefOtherButton, gbc);

        gbc.gridy = ++line;
        JComponent[] components = createComponents("referencePixelX");
        JComponent unitcomponent = createUnitComponent("referencePixelX");
        GridBagUtils.addToPanel(dialogPane, components[1], gbc, "insets.top=1,gridwidth=1,fill=NONE,weightx=0");
        GridBagUtils.addToPanel(dialogPane, components[0], gbc, "fill=HORIZONTAL,weightx=1");
        GridBagUtils.addToPanel(dialogPane, unitcomponent, gbc, "fill=NONE,weightx=0");
        gbc.gridy = ++line;
        components = createComponents("referencePixelY");
        unitcomponent = createUnitComponent("referencePixelY");
        GridBagUtils.addToPanel(dialogPane, components[1], gbc, "insets.top=3");
        GridBagUtils.addToPanel(dialogPane, components[0], gbc, "fill=HORIZONTAL,weightx=1");
        GridBagUtils.addToPanel(dialogPane, unitcomponent, gbc, "fill=NONE,weightx=0");
        gbc.gridy = ++line;
        components = createComponents("easting");
        unitcomponent = createUnitComponent("easting");
        GridBagUtils.addToPanel(dialogPane, components[1], gbc, "insets.top=12");
        GridBagUtils.addToPanel(dialogPane, components[0], gbc, "fill=HORIZONTAL,weightx=1");
        GridBagUtils.addToPanel(dialogPane, unitcomponent, gbc, "fill=NONE,weightx=0");
        gbc.gridy = ++line;
        components = createComponents("northing");
        unitcomponent = createUnitComponent("northing");
        GridBagUtils.addToPanel(dialogPane, components[1], gbc, "insets.top=3");
        GridBagUtils.addToPanel(dialogPane, components[0], gbc, "fill=HORIZONTAL,weightx=1");
        GridBagUtils.addToPanel(dialogPane, unitcomponent, gbc, "fill=NONE,weightx=0");
        gbc.gridy = ++line;
        components = createComponents("orientation");
        unitcomponent = createUnitComponent("orientation");
        GridBagUtils.addToPanel(dialogPane, components[1], gbc, "insets.top=3");
        GridBagUtils.addToPanel(dialogPane, components[0], gbc, "fill=HORIZONTAL,weightx=1");
        GridBagUtils.addToPanel(dialogPane, unitcomponent, gbc, "fill=NONE,weightx=0");
        gbc.gridy = ++line;
        components = createComponents("pixelSizeX");
        unitcomponent = createUnitComponent("pixelSizeX");
        GridBagUtils.addToPanel(dialogPane, components[1], gbc, "insets.top=12");
        GridBagUtils.addToPanel(dialogPane, components[0], gbc, "fill=HORIZONTAL,weightx=1");
        GridBagUtils.addToPanel(dialogPane, unitcomponent, gbc, "fill=NONE,weightx=0");
        gbc.gridy = ++line;
        components = createComponents("pixelSizeY");
        unitcomponent = createUnitComponent("pixelSizeY");
        GridBagUtils.addToPanel(dialogPane, components[1], gbc, "insets.top=3");
        GridBagUtils.addToPanel(dialogPane, components[0], gbc, "fill=HORIZONTAL,weightx=1");
        GridBagUtils.addToPanel(dialogPane, unitcomponent, gbc, "fill=NONE,weightx=0");
        gbc.gridy = ++line;
        components = createComponents("fitProductSize");
        context.bindEnabledState("width", false, "fitProductSize", true);
        context.bindEnabledState("height", false, "fitProductSize", true);
        GridBagUtils.addToPanel(dialogPane, components[0], gbc, "insets.top=12, gridwidth=3,fill=HORIZONTAL,weightx=1");
        gbc.gridy = ++line;
        components = createComponents("width");
        unitcomponent = createUnitComponent("width");
        GridBagUtils.addToPanel(dialogPane, components[1], gbc, "insets.top=3, gridwidth=1,fill=NONE,weightx=0");
        GridBagUtils.addToPanel(dialogPane, components[0], gbc, "fill=HORIZONTAL,weightx=1");
        GridBagUtils.addToPanel(dialogPane, unitcomponent, gbc, "fill=NONE,weightx=0");
        gbc.gridy = ++line;
        components = createComponents("height");
        unitcomponent = createUnitComponent("height");
        GridBagUtils.addToPanel(dialogPane, components[1], gbc);
        GridBagUtils.addToPanel(dialogPane, components[0], gbc, "fill=HORIZONTAL,weightx=1");
        GridBagUtils.addToPanel(dialogPane, unitcomponent, gbc, "fill=NONE,weightx=0");

        add(dialogPane);
    }
    
    private JComponent[] createComponents(String propertyName) {
        PropertyDescriptor descriptor = context.getPropertySet().getDescriptor(propertyName);
        PropertyEditorRegistry propertyEditorRegistry = PropertyEditorRegistry.getInstance();
        PropertyEditor editor = propertyEditorRegistry.findPropertyEditor(descriptor);
        return editor.createComponents(descriptor, context);
    }

    private JComponent createUnitComponent(String propertyName) {
        PropertyDescriptor descriptor = context.getPropertySet().getDescriptor(propertyName);
        JLabel unitLabel = new JLabel(descriptor.getUnit());
        context.getBinding(propertyName).addComponent(unitLabel);
        return unitLabel;
    }
    
    
    // for testing the UI
    public static void main(String[] args) throws Exception {
        final JFrame jFrame = new JFrame("Output parameter Definition Form");
        Container contentPane = jFrame.getContentPane();
        
        Product sourceProduct = ProductIO.readProduct("C:\\Dokumente und Einstellungen\\Marco Peters\\Eigene Dateien\\EOData\\MER_RR__1PPBCM20090804__Kinneret_FUB.dim");
        CoordinateReferenceSystem targetCrs = CRS.decode("EPSG:32632");
        OutputGeometryFormModel model = new OutputGeometryFormModel(sourceProduct, targetCrs);
        OutputGeometryForm form = new OutputGeometryForm(model);
        contentPane.add(form);
        
        jFrame.setSize(400, 600);
        jFrame.setLocationRelativeTo(null);
        jFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                jFrame.setVisible(true);
            }
        });
    }
}
