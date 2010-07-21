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

package com.bc.ceres.swing.binding;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.ValueRange;
import com.bc.ceres.binding.ValueSet;

import java.awt.Component;
import java.io.File;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import junit.framework.TestCase;


public class PropertyPaneTest extends TestCase {

    public void testComponentsInPanel() throws ConversionException {
        PropertyPane parametersPane = createPane(new BindingContext.SilentProblemHandler());
        JPanel panel = parametersPane.createPanel();
        Component[] components = panel.getComponents();
        assertEquals(14, components.length);

        assertEquals(JCheckBox.class, components[0].getClass());
        assertEquals(JLabel.class, components[1].getClass());
        assertEquals(JTextField.class, components[2].getClass());
        assertEquals(JLabel.class, components[3].getClass());
        assertEquals(JTextField.class, components[4].getClass());
        assertEquals(JLabel.class, components[5].getClass());
        assertEquals(JTextField.class, components[6].getClass());
        assertEquals(JLabel.class, components[7].getClass());
        assertEquals(JComboBox.class, components[8].getClass());
        assertEquals(JLabel.class, components[9].getClass());
        assertEquals(JTextField.class, components[10].getClass());
        assertEquals(JLabel.class, components[11].getClass());
        assertEquals(JPanel.class, components[12].getClass());
        assertEquals(JPanel.class, components[13].getClass()); // Spacer!

        assertEquals("useLogFile", components[0].getName());
        assertEquals("Use log file", ((JCheckBox) components[0]).getText());
        assertEquals("Threshold:", ((JLabel) components[1]).getText());
        assertEquals("threshold", components[2].getName());
        assertEquals("Iteration limit:", ((JLabel) components[3]).getText());
        assertEquals("iterationLimit", components[4].getName());
        assertEquals("Max iteration count:", ((JLabel) components[5]).getText());
        assertEquals("maxIterationCount", components[6].getName());
        assertEquals("Resampling method:", ((JLabel) components[7]).getText());
        assertEquals("resamplingMethod", components[8].getName());
        assertEquals("Product description:", ((JLabel) components[9]).getText());
        assertEquals("productDescription", components[10].getName());
        assertEquals("Image file:", ((JLabel) components[11]).getText());
    }

    private static class V {

        boolean useLogFile = true;
        double threshold = 0.5;
        float iterationLimit = 0.1f;
        int maxIterationCount = 10;
        String resamplingMethod = "NN";
        String productDescription = "All purpose";
        File imageFile = new File(".").getAbsoluteFile();
    }

    private static PropertyPane createPane(BindingProblemListener bpl) throws ConversionException {
        PropertyContainer vc = PropertyContainer.createObjectBacked(new V());

        vc.getDescriptor("threshold").setValueRange(
                ValueRange.parseValueRange("[0,1)")); // todo - not recognised (nf - 24.10.2007)
        vc.getDescriptor("resamplingMethod").setValueSet(
                new ValueSet(new String[]{"NN", "CC", "BQ"}));

        BindingContext sbc = new BindingContext(vc, bpl);
        return new PropertyPane(sbc);
    }

}
