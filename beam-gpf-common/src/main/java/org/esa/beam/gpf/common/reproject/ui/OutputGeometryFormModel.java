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

import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.ValueSet;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.gpf.common.reproject.ImageGeometry;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * @author Marco Zuehlke
 * @since BEAM 4.7
 */
class OutputGeometryFormModel {

    private class ChangeListener implements PropertyChangeListener {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            Double pixelSizeX = (Double) propertyContainer.getValue("pixelSizeX");
            Double pixelSizeY = (Double) propertyContainer.getValue("pixelSizeY");
            Integer width = (Integer) propertyContainer.getValue("width");
            Integer height = (Integer) propertyContainer.getValue("height");
            Double easting = (Double) propertyContainer.getValue("easting");
            Double northing = (Double) propertyContainer.getValue("northing");
            Double referencePixelX = (Double) propertyContainer.getValue("referencePixelX");
            Double referencePixelY = (Double) propertyContainer.getValue("referencePixelY");
            Double orientation = (Double) propertyContainer.getValue("orientation");
            if (fitProductSize) {
                width = null;
                height = null;
            }
            if (referencePixelLocation == 0) {
                referencePixelX = 0.5;
                referencePixelY = 0.5;
                easting = null;
                northing = null;
            } else if (referencePixelLocation == 1) {
                referencePixelX = 0.5 * (Integer) propertyContainer.getValue("width");
                referencePixelY = 0.5 * (Integer) propertyContainer.getValue("height");
                easting = null;
                northing = null;
            }
            ImageGeometry ig = ImageGeometry.createTargetGeometry(sourceProduct, targetCrs, 
                                                                  pixelSizeX, pixelSizeY, 
                                                                  width, height, orientation, 
                                                                  easting, northing, 
                                                                  referencePixelX, referencePixelY);
            
            updateImgaeGeometry(ig);
        }
    }
    
    private final transient Product sourceProduct;
    private final transient CoordinateReferenceSystem targetCrs;

    private boolean fitProductSize = true;
    private int referencePixelLocation = 1;
    
    private transient PropertyContainer propertyContainer;
    private transient ImageGeometry imageGeometry;

    OutputGeometryFormModel(Product sourceProduct, CoordinateReferenceSystem targetCrs) {
        this.sourceProduct = sourceProduct;
        this.targetCrs = targetCrs;
        imageGeometry = ImageGeometry.createTargetGeometry(sourceProduct, targetCrs,
                                                           null, null, null, null, null, null, null, null, null);
        propertyContainer =  PropertyContainer.createObjectBacked(imageGeometry);
        PropertyContainer thisVC = PropertyContainer.createObjectBacked(this);
        propertyContainer.addProperties(thisVC.getProperties());
        PropertyDescriptor descriptor = propertyContainer.getDescriptor("referencePixelLocation");
        descriptor.setValueSet(new ValueSet(new Integer[] {0, 1, 2}));
        propertyContainer.addPropertyChangeListener(new ChangeListener());
    }
    
    PropertyContainer getValueContainer() {
        return propertyContainer;
    }

    private void updateImgaeGeometry(ImageGeometry newImageGeometry) {
        PropertyContainer newVC=  PropertyContainer.createObjectBacked(newImageGeometry);
        Property[] properties = newVC.getProperties();
        for (Property property : properties) {
            Property targetModel = propertyContainer.getProperty(property.getDescriptor().getName());
            try {
                targetModel.setValue(property.getValue());
            } catch (ValidationException ignored) {
            }
        }
    }
}
