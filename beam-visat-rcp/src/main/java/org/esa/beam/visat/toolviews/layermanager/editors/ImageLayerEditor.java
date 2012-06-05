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
import com.bc.ceres.glayer.support.ImageLayer;
import org.esa.beam.framework.ui.layer.AbstractLayerConfigurationEditor;

import java.awt.Color;

/**
 * Basic editor for image layers.
 *
 * @author Ralf Quast
 * @version $ Revision: $ $ Date: $
 * @since BEAM 4.6
 */
public class ImageLayerEditor extends AbstractLayerConfigurationEditor {

    @Override
    protected void addEditablePropertyDescriptors() {

        addDescriptor(ImageLayer.PROPERTY_NAME_BORDER_SHOWN, Boolean.class, ImageLayer.DEFAULT_BORDER_SHOWN, "Show image border");
        addDescriptor(ImageLayer.PROPERTY_NAME_BORDER_COLOR, Color.class, ImageLayer.DEFAULT_BORDER_COLOR, "Image border colour");
        addDescriptor(ImageLayer.PROPERTY_NAME_BORDER_WIDTH, Double.class, ImageLayer.DEFAULT_BORDER_WIDTH, "Image border size");

        addDescriptor(ImageLayer.PROPERTY_NAME_PIXEL_BORDER_SHOWN, Boolean.class, ImageLayer.DEFAULT_PIXEL_BORDER_SHOWN, "Show pixel borders");
        addDescriptor(ImageLayer.PROPERTY_NAME_PIXEL_BORDER_COLOR, Color.class, ImageLayer.DEFAULT_PIXEL_BORDER_COLOR, "Pixel border colour");
        addDescriptor(ImageLayer.PROPERTY_NAME_PIXEL_BORDER_WIDTH, Double.class, ImageLayer.DEFAULT_PIXEL_BORDER_WIDTH, "Pixel border size");
    }

    private void addDescriptor(String name, Class<?> type, Object defaultValue, String displayName) {
        PropertyDescriptor propertyDescriptor = new PropertyDescriptor(name, type);
        propertyDescriptor.setDefaultValue(defaultValue);
        propertyDescriptor.setDisplayName(displayName);
        propertyDescriptor.setDefaultConverter();
        addPropertyDescriptor(propertyDescriptor);
    }
}