/*
 * $Id: $
 *
 * Copyright (C) 2009 by Brockmann Consult (info@brockmann-consult.de)
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
package org.esa.beam.gpf.common.reproject.ui;

import com.bc.ceres.binding.ValueDescriptor;
import com.bc.ceres.binding.swing.BindingContext;
import com.bc.ceres.binding.swing.ValueEditor;
import com.bc.ceres.binding.swing.ValueEditorRegistry;

import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.GridBagUtils;
import org.esa.beam.framework.ui.ValueEditorsPane;
import org.esa.beam.gpf.common.reproject.ImageGeometry;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

/**
 * @author Marco Zuehlke
 * @since BEAM 4.7
 */
class OutputSizeForm extends JPanel {
    
    private final BindingContext context;
    
    private JRadioButton pixelRefULeftButton;
    private JRadioButton pixelRefCenterButton;
    private JRadioButton pixelRefOtherButton;

    public OutputSizeForm(OutputSizeFormModel model) {
        context = new BindingContext(model.getvalueContainer());
        createUI();
    }
    
    private void createUI() {
        int line = 0;
        JPanel dialogPane = GridBagUtils.createPanel();
        dialogPane.setBorder(new EmptyBorder(7, 7, 7, 7));
        final GridBagConstraints gbc = GridBagUtils.createDefaultConstraints();
        GridBagUtils.setAttributes(gbc, "insets.top=0,gridwidth=3");

        pixelRefULeftButton = new JRadioButton("Reference pixel is at scene upper left", false);
        pixelRefCenterButton = new JRadioButton("Reference pixel is at scene center", false);
        pixelRefOtherButton = new JRadioButton("Other reference pixel position", false);
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
        JComponent[] components = createComponents("referencePixelX", null);
        JComponent unitcomponent = createUnitComponent("referencePixelX");
        GridBagUtils.addToPanel(dialogPane, components[1], gbc, "insets.top=1,gridwidth=1,fill=NONE,weightx=0");
        GridBagUtils.addToPanel(dialogPane, components[0], gbc, "fill=HORIZONTAL,weightx=1");
        GridBagUtils.addToPanel(dialogPane, unitcomponent, gbc, "fill=NONE,weightx=0");
        gbc.gridy = ++line;
        components = createComponents("referencePixelY", null);
        unitcomponent = createUnitComponent("referencePixelY");
        GridBagUtils.addToPanel(dialogPane, components[1], gbc, "insets.top=3");
        GridBagUtils.addToPanel(dialogPane, components[0], gbc, "fill=HORIZONTAL,weightx=1");
        GridBagUtils.addToPanel(dialogPane, unitcomponent, gbc, "fill=NONE,weightx=0");
        gbc.gridy = ++line;
        components = createComponents("northing", null);
        unitcomponent = createUnitComponent("northing");
        GridBagUtils.addToPanel(dialogPane, components[1], gbc, "insets.top=12");
        GridBagUtils.addToPanel(dialogPane, components[0], gbc, "fill=HORIZONTAL,weightx=1");
        GridBagUtils.addToPanel(dialogPane, unitcomponent, gbc, "fill=NONE,weightx=0");
        gbc.gridy = ++line;
        components = createComponents("easting", null);
        unitcomponent = createUnitComponent("easting");
        GridBagUtils.addToPanel(dialogPane, components[1], gbc, "insets.top=3");
        GridBagUtils.addToPanel(dialogPane, components[0], gbc, "fill=HORIZONTAL,weightx=1");
        GridBagUtils.addToPanel(dialogPane, unitcomponent, gbc, "fill=NONE,weightx=0");
        gbc.gridy = ++line;
        components = createComponents("orientation", null);
        unitcomponent = createUnitComponent("orientation");
        GridBagUtils.addToPanel(dialogPane, components[1], gbc, "insets.top=3");
        GridBagUtils.addToPanel(dialogPane, components[0], gbc, "fill=HORIZONTAL,weightx=1");
        GridBagUtils.addToPanel(dialogPane, unitcomponent, gbc, "fill=NONE,weightx=0");
        gbc.gridy = ++line;
        components = createComponents("pixelSizeX", null);
        unitcomponent = createUnitComponent("pixelSizeX");
        GridBagUtils.addToPanel(dialogPane, components[1], gbc, "insets.top=12");
        GridBagUtils.addToPanel(dialogPane, components[0], gbc, "fill=HORIZONTAL,weightx=1");
        GridBagUtils.addToPanel(dialogPane, unitcomponent, gbc, "fill=NONE,weightx=0");
        gbc.gridy = ++line;
        components = createComponents("pixelSizeY", null);
        unitcomponent = createUnitComponent("pixelSizeY");
        GridBagUtils.addToPanel(dialogPane, components[1], gbc, "insets.top=3");
        GridBagUtils.addToPanel(dialogPane, components[0], gbc, "fill=HORIZONTAL,weightx=1");
        GridBagUtils.addToPanel(dialogPane, unitcomponent, gbc, "fill=NONE,weightx=0");
        gbc.gridy = ++line;
        components = createComponents("fitProductSize", null);
        context.bindEnabledState("width", true, "fitProductSize", true);
        context.bindEnabledState("height", true, "fitProductSize", true);
        GridBagUtils.addToPanel(dialogPane, components[0], gbc, "insets.top=12, gridwidth=3,fill=HORIZONTAL,weightx=1");
        gbc.gridy = ++line;
        components = createComponents("width", null);
        unitcomponent = createUnitComponent("width");
        GridBagUtils.addToPanel(dialogPane, components[1], gbc, "insets.top=3, gridwidth=1,fill=NONE,weightx=0");
        GridBagUtils.addToPanel(dialogPane, components[0], gbc, "fill=HORIZONTAL,weightx=1");
        GridBagUtils.addToPanel(dialogPane, unitcomponent, gbc, "fill=NONE,weightx=0");
        gbc.gridy = ++line;
        components = createComponents("height", null);
        unitcomponent = createUnitComponent("height");
        GridBagUtils.addToPanel(dialogPane, components[1], gbc);
        GridBagUtils.addToPanel(dialogPane, components[0], gbc, "fill=HORIZONTAL,weightx=1");
        GridBagUtils.addToPanel(dialogPane, unitcomponent, gbc, "fill=NONE,weightx=0");
        gbc.gridy = ++line;
        components = createComponents("noDataValue", null);
        unitcomponent = createUnitComponent("noDataValue");
        GridBagUtils.addToPanel(dialogPane, components[1], gbc, "insets.top=12, gridwidth=1");
        GridBagUtils.addToPanel(dialogPane, components[0], gbc, "fill=HORIZONTAL,weightx=1");
        GridBagUtils.addToPanel(dialogPane, unitcomponent, gbc, "fill=NONE,weightx=0");

        add(dialogPane);
    }
    
    private JComponent[] createComponents(String propertyName, Class<? extends ValueEditor> editorClass) {
        ValueDescriptor descriptor = context.getValueContainer().getDescriptor(propertyName);
        ValueEditorRegistry valueEditorRegistry = ValueEditorRegistry.getInstance();
        ValueEditor editor;
        if (editorClass == null) {
            editor = valueEditorRegistry.findValueEditor(descriptor);
        } else {
            editor = valueEditorRegistry.getValueEditor(editorClass.getName());
        }
        return editor.createComponents(descriptor, context);
    }

    private JComponent createUnitComponent(String propertyName) {
        ValueDescriptor descriptor = context.getValueContainer().getDescriptor(propertyName);
        JLabel unitLabel = new JLabel(descriptor.getUnit());
        context.getBinding(propertyName).addComponent(unitLabel);
        return unitLabel;
    }
    
    
    // for testing the UI
    public static void main(String[] args) throws Exception {
        final JFrame jFrame = new JFrame("Output parameter Definition Form");
        Container contentPane = jFrame.getContentPane();
        
        Product sourceProduct = ProductIO.readProduct("/home/marcoz/EOData/Meris/Pairs/MER_RR__1alpen.N1", null);
        CoordinateReferenceSystem targetCrs = CRS.decode("EPSG:32632");
        OutputSizeFormModel model = new OutputSizeFormModel(sourceProduct, targetCrs);
        OutputSizeForm form = new OutputSizeForm(model);
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
