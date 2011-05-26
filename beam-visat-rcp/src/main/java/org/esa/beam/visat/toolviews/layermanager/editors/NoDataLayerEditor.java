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
import org.esa.beam.glayer.NoDataLayerType;

import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * @author Marco Peters
 * @version $ Revision: $ Date: $
 * @since BEAM 4.6
 */
public class NoDataLayerEditor extends AbstractLayerConfigurationEditor {

    @Override
    protected void addEditablePropertyDescriptors() {

        PropertyDescriptor vd = new PropertyDescriptor(NoDataLayerType.PROPERTY_NAME_COLOR, Color.class);
        vd.setDefaultValue(Color.ORANGE);
        vd.setDisplayName("No-data colour");
        vd.setDefaultConverter();

        addPropertyDescriptor(vd);
        getBindingContext().getPropertySet().addPropertyChangeListener(NoDataLayerType.PROPERTY_NAME_COLOR,
                                                                       new UpdateImagePropertyChangeListener());
    }

    private class UpdateImagePropertyChangeListener implements PropertyChangeListener {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (getCurrentLayer() != null) {
                final Color newColor = (Color) evt.getNewValue();
                final ImageLayer layer = (ImageLayer) getCurrentLayer();
                NoDataLayerType.renewMultiLevelSource(layer, newColor);
            }
        }

    }
}
