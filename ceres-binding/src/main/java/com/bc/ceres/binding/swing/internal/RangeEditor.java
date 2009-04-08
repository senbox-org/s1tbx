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
package com.bc.ceres.binding.swing.internal;

import com.bc.ceres.binding.ValueDescriptor;
import com.bc.ceres.binding.ValueRange;
import com.bc.ceres.binding.swing.BindingContext;
import com.bc.ceres.binding.swing.ComponentAdapter;

import java.awt.Font;
import java.util.Hashtable;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JSlider;

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
    public boolean isValidFor(ValueDescriptor valueDescriptor) {
        Class<?> type = valueDescriptor.getType();
        if (NumericEditor.isNumericType(type)) {
            ValueRange vr = valueDescriptor.getValueRange();
            if (vr != null && vr.hasMin() && vr.hasMax()) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    public JComponent createEditorComponent(ValueDescriptor valueDescriptor, BindingContext bindingContext) {
        JSlider slider = new JSlider(0, 100);
        
        Hashtable<Integer, JLabel> sliderLabelTable = createSliderLabelTable(valueDescriptor.getValueRange());
        slider.setLabelTable(sliderLabelTable);
        slider.setPaintLabels(true);
        
        ComponentAdapter adapter = new SliderAdapter(slider);
        bindingContext.bind(valueDescriptor.getName(), adapter);
        return slider;
    }

    private Hashtable<Integer, JLabel> createSliderLabelTable(ValueRange valueRange) {
        Hashtable<Integer, JLabel> sliderLabelTable = new Hashtable<Integer, JLabel>();
        sliderLabelTable.put(Integer.valueOf(0), createSliderLabel(valueRange.getMin()));
        sliderLabelTable.put(Integer.valueOf(100), createSliderLabel(valueRange.getMax()));
        return sliderLabelTable;
    }
    
    private JLabel createSliderLabel(double value) {
        String text = toString(value);
        JLabel label = new JLabel(text);
        Font oldFont = label.getFont();
        Font newFont = oldFont.deriveFont(oldFont.getSize2D() * 0.85f);
        label.setFont(newFont);
        return label;
    }
    
    private static String toString(double d) {
        final long l = Math.round(d);
        return d == l ? Long.toString(l) : Double.toString(d);
    }
}
