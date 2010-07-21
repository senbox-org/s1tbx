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
package org.esa.beam.framework.ui.crs;

import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.ValueSet;
import org.esa.beam.framework.datamodel.ImageGeometry;
import org.esa.beam.framework.datamodel.Product;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * @author Marco Zuehlke
 * @since BEAM 4.7
 */
public class OutputGeometryFormModel {

    private class ChangeListener implements PropertyChangeListener {
        @Override
        public void propertyChange(PropertyChangeEvent event) {
            String propertyName = event.getPropertyName();

            if (fitProductSize && propertyName.startsWith("pixelSize")) {
                Double pixelSizeX = (Double) propertyContainer.getValue("pixelSizeX");
                Double pixelSizeY = (Double) propertyContainer.getValue("pixelSizeY");
                Rectangle productSize = ImageGeometry.calculateProductSize(sourceProduct, targetCrs, pixelSizeX, pixelSizeY);
                propertyContainer.setValue("width", productSize.width);
                propertyContainer.setValue("height", productSize.height);
            }
            if (propertyName.startsWith("referencePixelLocation")) {
                double pixelSizeX = (Double) propertyContainer.getValue("pixelSizeX");
                double pixelSizeY = (Double) propertyContainer.getValue("pixelSizeY");
                double referencePixelX = (Double) propertyContainer.getValue("referencePixelX");
                double referencePixelY = (Double) propertyContainer.getValue("referencePixelY");
                if (referencePixelLocation == 0) {
                    referencePixelX = 0.5;
                    referencePixelY = 0.5;
                } else if (referencePixelLocation == 1) {
                    referencePixelX = 0.5 * (Integer) propertyContainer.getValue("width");
                    referencePixelY = 0.5 * (Integer) propertyContainer.getValue("height");
                }
                Point2D eastingNorthing = ImageGeometry.calculateEastingNorthing(sourceProduct, targetCrs, referencePixelX, referencePixelY, pixelSizeX, pixelSizeY);
                propertyContainer.setValue("easting", eastingNorthing.getX());
                propertyContainer.setValue("northing", eastingNorthing.getY());
                propertyContainer.setValue("referencePixelX", referencePixelX);
                propertyContainer.setValue("referencePixelY", referencePixelY);
            }
        }
    }
    
    private final transient Product sourceProduct;
    private final transient CoordinateReferenceSystem targetCrs;

    private boolean fitProductSize = true;
    private int referencePixelLocation = 1;
    
    private transient PropertyContainer propertyContainer;

    public OutputGeometryFormModel(Product sourceProduct, Product collocationProduct) {
       this(sourceProduct, ImageGeometry.createCollocationTargetGeometry(sourceProduct, collocationProduct));
    }
    
    public OutputGeometryFormModel(Product sourceProduct, CoordinateReferenceSystem targetCrs) {
        this(sourceProduct, ImageGeometry.createTargetGeometry(sourceProduct, targetCrs,
                                                               null, null, null, null,
                                                               null, null, null, null, null));
    }
    
    private OutputGeometryFormModel(Product sourceProduct, ImageGeometry imageGeometry) {
        this.sourceProduct = sourceProduct;
        this.targetCrs = imageGeometry.getMapCrs();

        propertyContainer =  PropertyContainer.createObjectBacked(imageGeometry);
        PropertyContainer thisVC = PropertyContainer.createObjectBacked(this);
        propertyContainer.addProperties(thisVC.getProperties());
        PropertyDescriptor descriptor = propertyContainer.getDescriptor("referencePixelLocation");
        descriptor.setValueSet(new ValueSet(new Integer[] {0, 1, 2}));
        propertyContainer.addPropertyChangeListener(new ChangeListener());

    }
    public PropertyContainer getPropertyContainer() {
        return propertyContainer;
    }
}
