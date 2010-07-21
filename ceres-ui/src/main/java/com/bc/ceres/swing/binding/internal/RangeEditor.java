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
package com.bc.ceres.swing.binding.internal;

import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.swing.binding.BindingContext;
import com.bc.ceres.swing.binding.ComponentAdapter;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JSlider;
import java.awt.Font;
import java.util.Dictionary;
import java.util.Hashtable;

/**
 * An editor for numeric values that have an associated range with lower and upper bounds.
 * The editor is realized using a JSLider.
 *
 * @author Marco Zuehlke
 * @version $Revision$ $Date$
 * @since BEAM 4.6
 */
public class RangeEditor extends NumericEditor {

    @Override
    public boolean isValidFor(PropertyDescriptor propertyDescriptor) {
// (08.04.2009, mz) For now disabled automatic selection of range editor
//                  must find better selection criteria.
//        
//        Class<?> type = propertyDescriptor.getType();
//        if (NumericEditor.isNumericType(type)) {
//            ValueRange vr = propertyDescriptor.getValueRange();
//            if (vr != null && vr.hasMin() && vr.hasMax()) {
//                return true;
//            }
//        }
        return false;
    }

    @Override
    public JComponent createEditorComponent(PropertyDescriptor propertyDescriptor, BindingContext bindingContext) {
        JSlider slider = new JSlider(0, 100);

        Dictionary<Integer, JLabel> sliderLabelTable = createSliderLabelTable();
        slider.setLabelTable(sliderLabelTable);
        slider.setPaintLabels(true);

        ComponentAdapter adapter = new SliderAdapter(slider);
        bindingContext.bind(propertyDescriptor.getName(), adapter);
        return slider;
    }

    private Dictionary<Integer, JLabel> createSliderLabelTable() {
        Dictionary<Integer, JLabel> sliderLabelTable = new Hashtable<Integer, JLabel>();
        sliderLabelTable.put(0, createSliderLabel("0%"));
        sliderLabelTable.put(50, createSliderLabel("50%"));
        sliderLabelTable.put(100, createSliderLabel("100%"));
        return sliderLabelTable;
    }

    private static JLabel createSliderLabel(String text) {
        JLabel label = new JLabel(text);
        Font oldFont = label.getFont();
        Font newFont = oldFont.deriveFont(oldFont.getSize2D() * 0.85f);
        label.setFont(newFont);
        return label;
    }

}
