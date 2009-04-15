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
package org.esa.beam.visat.toolviews.layermanager.editors;

import com.bc.ceres.binding.ValueDescriptor;
import com.bc.ceres.binding.ValueRange;
import com.bc.ceres.binding.swing.Binding;
import com.bc.ceres.binding.swing.BindingContext;
import com.bc.ceres.binding.swing.internal.RangeEditor;
import org.esa.beam.glayer.GraticuleLayer;

import javax.swing.JComponent;
import java.awt.Color;
import java.util.List;

/**
 * Editor for graticule layer.
 *
 * @author Marco Zuehlke
 * @version $Revision$ $Date$
 * @since BEAM 4.6
 */
public class GraticuleLayerEditor extends AbstractValueDescriptorLayerEditor {


    @Override
    public void updateControl() {
        super.updateControl();
        BindingContext bindingContext = getBindingContext();

        boolean resAuto = (Boolean) bindingContext.getValueContainer().getValue(GraticuleLayer.PROPERTY_NAME_RES_AUTO);
        setEditorEnableState(GraticuleLayer.PROPERTY_NAME_RES_PIXELS, resAuto);
        setEditorEnableState(GraticuleLayer.PROPERTY_NAME_RES_LAT, !resAuto);
        setEditorEnableState(GraticuleLayer.PROPERTY_NAME_RES_LON, !resAuto);

        boolean textEnabled = (Boolean) bindingContext.getValueContainer().getValue(
                GraticuleLayer.PROPERTY_NAME_TEXT_ENABLED);
        setEditorEnableState(GraticuleLayer.PROPERTY_NAME_TEXT_FG_COLOR, textEnabled);
        setEditorEnableState(GraticuleLayer.PROPERTY_NAME_TEXT_BG_COLOR, textEnabled);
        setEditorEnableState(GraticuleLayer.PROPERTY_NAME_TEXT_BG_TRANSPARENCY, textEnabled);
    }

    @Override
    protected void collectValueDescriptors(List<ValueDescriptor> descriptorList) {

        ValueDescriptor vd0 = new ValueDescriptor(GraticuleLayer.PROPERTY_NAME_RES_AUTO, Boolean.class);
        vd0.setDefaultValue(GraticuleLayer.DEFAULT_RES_AUTO);
        vd0.setDisplayName("Compute latitude and longitude steps");
        vd0.setDefaultConverter();
        descriptorList.add(vd0);

        ValueDescriptor vd1 = new ValueDescriptor(GraticuleLayer.PROPERTY_NAME_RES_PIXELS, Integer.class);
        vd1.setDefaultValue(GraticuleLayer.DEFAULT_RES_PIXELS);
        vd1.setValueRange(new ValueRange(16, 512));
        vd1.setDisplayName("Average grid size in pixels");
        vd1.setDefaultConverter();
        descriptorList.add(vd1);

        ValueDescriptor vd2 = new ValueDescriptor(GraticuleLayer.PROPERTY_NAME_RES_LAT, Double.class);
        vd2.setDefaultValue(GraticuleLayer.DEFAULT_RES_LAT);
        vd2.setValueRange(new ValueRange(0.01, 90.00));
        vd2.setDisplayName("Latitude step (dec. degree)");
        vd2.setDefaultConverter();
        descriptorList.add(vd2);

        ValueDescriptor vd3 = new ValueDescriptor(GraticuleLayer.PROPERTY_NAME_RES_LON, Double.class);
        vd3.setDefaultValue(GraticuleLayer.DEFAULT_RES_LON);
        vd3.setValueRange(new ValueRange(0.01, 180.00));
        vd3.setDisplayName("Longitude step (dec. degree)");
        vd3.setDefaultConverter();
        descriptorList.add(vd3);

        ValueDescriptor vd4 = new ValueDescriptor(GraticuleLayer.PROPERTY_NAME_LINE_COLOR, Color.class);
        vd4.setDefaultValue(GraticuleLayer.DEFAULT_LINE_COLOR);
        vd4.setDisplayName("Line colour");
        vd4.setDefaultConverter();
        descriptorList.add(vd4);

        ValueDescriptor vd5 = new ValueDescriptor(GraticuleLayer.PROPERTY_NAME_LINE_WIDTH, Double.class);
        vd5.setDefaultValue(GraticuleLayer.DEFAULT_LINE_WIDTH);
        vd5.setDisplayName("Line width");
        vd5.setDefaultConverter();
        descriptorList.add(vd5);

        ValueDescriptor vd6 = new ValueDescriptor(GraticuleLayer.PROPERTY_NAME_LINE_TRANSPARENCY, Double.class);
        vd6.setDefaultValue(GraticuleLayer.DEFAULT_LINE_TRANSPARENCY);
        vd6.setValueRange(new ValueRange(0, 1));
        vd6.setDisplayName("Line transparency");
        vd6.setDefaultConverter();
        vd6.setProperty("valueEditor", RangeEditor.class.getName());
        descriptorList.add(vd6);

        ValueDescriptor vd7 = new ValueDescriptor(GraticuleLayer.PROPERTY_NAME_TEXT_ENABLED, Boolean.class);
        vd7.setDefaultValue(GraticuleLayer.DEFAULT_TEXT_ENABLED);
        vd7.setDisplayName("Show text labels");
        vd7.setDefaultConverter();
        descriptorList.add(vd7);

        ValueDescriptor vd8 = new ValueDescriptor(GraticuleLayer.PROPERTY_NAME_TEXT_FG_COLOR, Color.class);
        vd8.setDefaultValue(GraticuleLayer.DEFAULT_TEXT_FG_COLOR);
        vd8.setDisplayName("Text foreground colour");
        vd8.setDefaultConverter();
        descriptorList.add(vd8);

        ValueDescriptor vd9 = new ValueDescriptor(GraticuleLayer.PROPERTY_NAME_TEXT_BG_COLOR, Color.class);
        vd9.setDefaultValue(GraticuleLayer.DEFAULT_TEXT_BG_COLOR);
        vd9.setDisplayName("Text background colour");
        vd9.setDefaultConverter();
        descriptorList.add(vd9);

        ValueDescriptor vd10 = new ValueDescriptor(GraticuleLayer.PROPERTY_NAME_TEXT_BG_TRANSPARENCY, Double.class);
        vd10.setDefaultValue(GraticuleLayer.DEFAULT_TEXT_BG_TRANSPARENCY);
        vd10.setValueRange(new ValueRange(0, 1));
        vd10.setDisplayName("Text background transparency");
        vd10.setDefaultConverter();
        vd10.setProperty("valueEditor", RangeEditor.class.getName());
        descriptorList.add(vd10);
    }

    private void setEditorEnableState(String propertyName, boolean enabled) {
        Binding pixelBinding = getBindingContext().getBinding(propertyName);
        JComponent[] components = pixelBinding.getComponents();
        for (JComponent component : components) {
            component.setEnabled(enabled);
        }
    }

}
