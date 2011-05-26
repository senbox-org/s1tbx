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
package org.esa.beam.visat.toolviews.layermanager.editors;

import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.ValueRange;
import com.bc.ceres.swing.binding.BindingContext;
import com.bc.ceres.swing.binding.PropertyEditorRegistry;
import com.bc.ceres.swing.binding.internal.RangeEditor;
import org.esa.beam.framework.ui.layer.AbstractLayerConfigurationEditor;
import org.esa.beam.glayer.GraticuleLayerType;

import java.awt.Color;

/**
 * Editor for graticule layer.
 *
 * @author Marco Zuehlke
 * @version $Revision$ $Date$
 * @since BEAM 4.6
 */
public class GraticuleLayerEditor extends AbstractLayerConfigurationEditor {

    @Override
    protected void addEditablePropertyDescriptors() {

        PropertyDescriptor vd0 = new PropertyDescriptor(GraticuleLayerType.PROPERTY_NAME_RES_AUTO, Boolean.class);
        vd0.setDefaultValue(GraticuleLayerType.DEFAULT_RES_AUTO);
        vd0.setDisplayName("Compute latitude and longitude steps");
        vd0.setDefaultConverter();
        addPropertyDescriptor(vd0);

        PropertyDescriptor vd1 = new PropertyDescriptor(GraticuleLayerType.PROPERTY_NAME_RES_PIXELS, Integer.class);
        vd1.setDefaultValue(GraticuleLayerType.DEFAULT_RES_PIXELS);
        vd1.setValueRange(new ValueRange(16, 512));
        vd1.setDisplayName("Average grid size in pixels");
        vd1.setDefaultConverter();
        addPropertyDescriptor(vd1);

        PropertyDescriptor vd2 = new PropertyDescriptor(GraticuleLayerType.PROPERTY_NAME_RES_LAT, Double.class);
        vd2.setDefaultValue(GraticuleLayerType.DEFAULT_RES_LAT);
        vd2.setValueRange(new ValueRange(0.01, 90.00));
        vd2.setDisplayName("Latitude step (dec. degree)");
        vd2.setDefaultConverter();
        addPropertyDescriptor(vd2);

        PropertyDescriptor vd3 = new PropertyDescriptor(GraticuleLayerType.PROPERTY_NAME_RES_LON, Double.class);
        vd3.setDefaultValue(GraticuleLayerType.DEFAULT_RES_LON);
        vd3.setValueRange(new ValueRange(0.01, 180.00));
        vd3.setDisplayName("Longitude step (dec. degree)");
        vd3.setDefaultConverter();
        addPropertyDescriptor(vd3);

        PropertyDescriptor vd4 = new PropertyDescriptor(GraticuleLayerType.PROPERTY_NAME_LINE_COLOR, Color.class);
        vd4.setDefaultValue(GraticuleLayerType.DEFAULT_LINE_COLOR);
        vd4.setDisplayName("Line colour");
        vd4.setDefaultConverter();
        addPropertyDescriptor(vd4);

        PropertyDescriptor vd5 = new PropertyDescriptor(GraticuleLayerType.PROPERTY_NAME_LINE_WIDTH, Double.class);
        vd5.setDefaultValue(GraticuleLayerType.DEFAULT_LINE_WIDTH);
        vd5.setDisplayName("Line width");
        vd5.setDefaultConverter();
        addPropertyDescriptor(vd5);

        final PropertyEditorRegistry propertyEditorRegistry = PropertyEditorRegistry.getInstance();

        PropertyDescriptor vd6 = new PropertyDescriptor(GraticuleLayerType.PROPERTY_NAME_LINE_TRANSPARENCY, Double.class);
        vd6.setDefaultValue(GraticuleLayerType.DEFAULT_LINE_TRANSPARENCY);
        vd6.setValueRange(new ValueRange(0, 1));
        vd6.setDisplayName("Line transparency");
        vd6.setDefaultConverter();
        vd6.setAttribute("propertyEditor", propertyEditorRegistry.getPropertyEditor(RangeEditor.class.getName()));
        addPropertyDescriptor(vd6);

        PropertyDescriptor vd7 = new PropertyDescriptor(GraticuleLayerType.PROPERTY_NAME_TEXT_ENABLED, Boolean.class);
        vd7.setDefaultValue(GraticuleLayerType.DEFAULT_TEXT_ENABLED);
        vd7.setDisplayName("Show text labels");
        vd7.setDefaultConverter();
        addPropertyDescriptor(vd7);

        PropertyDescriptor vd8 = new PropertyDescriptor(GraticuleLayerType.PROPERTY_NAME_TEXT_FG_COLOR, Color.class);
        vd8.setDefaultValue(GraticuleLayerType.DEFAULT_TEXT_FG_COLOR);
        vd8.setDisplayName("Text foreground colour");
        vd8.setDefaultConverter();
        addPropertyDescriptor(vd8);

        PropertyDescriptor vd9 = new PropertyDescriptor(GraticuleLayerType.PROPERTY_NAME_TEXT_BG_COLOR, Color.class);
        vd9.setDefaultValue(GraticuleLayerType.DEFAULT_TEXT_BG_COLOR);
        vd9.setDisplayName("Text background colour");
        vd9.setDefaultConverter();
        addPropertyDescriptor(vd9);

        PropertyDescriptor vd10 = new PropertyDescriptor(GraticuleLayerType.PROPERTY_NAME_TEXT_BG_TRANSPARENCY, Double.class);
        vd10.setDefaultValue(GraticuleLayerType.DEFAULT_TEXT_BG_TRANSPARENCY);
        vd10.setValueRange(new ValueRange(0, 1));
        vd10.setDisplayName("Text background transparency");
        vd10.setDefaultConverter();
        vd10.setAttribute("propertyEditor", propertyEditorRegistry.getPropertyEditor(RangeEditor.class.getName()));
        addPropertyDescriptor(vd10);

        BindingContext bindingContext = getBindingContext();
        boolean resAuto = (Boolean) bindingContext.getPropertySet().getValue(
                GraticuleLayerType.PROPERTY_NAME_RES_AUTO);
        bindingContext.bindEnabledState(GraticuleLayerType.PROPERTY_NAME_RES_PIXELS, resAuto,
                                        GraticuleLayerType.PROPERTY_NAME_RES_AUTO, resAuto);
        bindingContext.bindEnabledState(GraticuleLayerType.PROPERTY_NAME_RES_LAT, !resAuto,
                                        GraticuleLayerType.PROPERTY_NAME_RES_AUTO, resAuto);
        bindingContext.bindEnabledState(GraticuleLayerType.PROPERTY_NAME_RES_LON, !resAuto,
                                        GraticuleLayerType.PROPERTY_NAME_RES_AUTO, resAuto);

        boolean textEnabled = (Boolean) bindingContext.getPropertySet().getValue(
                GraticuleLayerType.PROPERTY_NAME_TEXT_ENABLED);
        bindingContext.bindEnabledState(GraticuleLayerType.PROPERTY_NAME_TEXT_FG_COLOR, textEnabled,
                                        GraticuleLayerType.PROPERTY_NAME_TEXT_ENABLED, textEnabled);
        bindingContext.bindEnabledState(GraticuleLayerType.PROPERTY_NAME_TEXT_BG_COLOR, textEnabled,
                                        GraticuleLayerType.PROPERTY_NAME_TEXT_ENABLED, textEnabled);
        bindingContext.bindEnabledState(GraticuleLayerType.PROPERTY_NAME_TEXT_BG_TRANSPARENCY, textEnabled,
                                        GraticuleLayerType.PROPERTY_NAME_TEXT_ENABLED, textEnabled);

    }

}
