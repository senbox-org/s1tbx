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
        PropertyDescriptor vd0 = new PropertyDescriptor(ImageLayer.PROPERTY_NAME_BORDER_SHOWN, Boolean.class);
        vd0.setDefaultValue(ImageLayer.DEFAULT_BORDER_SHOWN);
        vd0.setDisplayName("Show image border");
        vd0.setDefaultConverter();
        addPropertyDescriptor(vd0);

        PropertyDescriptor vd1 = new PropertyDescriptor(ImageLayer.PROPERTY_NAME_BORDER_COLOR, Color.class);
        vd1.setDefaultValue(ImageLayer.DEFAULT_BORDER_COLOR);
        vd1.setDisplayName("Image border colour");
        vd1.setDefaultConverter();
        addPropertyDescriptor(vd1);

        PropertyDescriptor vd2 = new PropertyDescriptor(ImageLayer.PROPERTY_NAME_BORDER_WIDTH, Double.class);
        vd2.setDefaultValue(ImageLayer.DEFAULT_BORDER_WIDTH);
        vd2.setDisplayName("Image border size");
        vd2.setDefaultConverter();
        addPropertyDescriptor(vd2);
    }
}