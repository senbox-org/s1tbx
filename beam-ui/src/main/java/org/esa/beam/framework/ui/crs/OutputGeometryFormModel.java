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

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.ValueSet;
import org.esa.beam.framework.datamodel.ImageGeometry;
import org.esa.beam.framework.datamodel.Product;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.awt.Rectangle;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;

/**
 * @author Marco Zuehlke
 * @since BEAM 4.7
 */
public class OutputGeometryFormModel {

    private static final int REFERENCE_PIXEL_DEFAULT = 1;
    private static final boolean FIT_PRODUCT_SIZE_DEFAULT = true;

    private int referencePixelLocation;
    private boolean fitProductSize;

    private transient Product sourceProduct;
    private transient CoordinateReferenceSystem targetCrs;
    private transient PropertyContainer propertyContainer;

    public OutputGeometryFormModel(Product sourceProduct, Product collocationProduct) {
        this(sourceProduct, ImageGeometry.createCollocationTargetGeometry(sourceProduct, collocationProduct));
    }

    public OutputGeometryFormModel(Product sourceProduct, CoordinateReferenceSystem targetCrs) {
        this(sourceProduct, ImageGeometry.createTargetGeometry(sourceProduct, targetCrs,
                null, null, null, null,
                null, null, null, null, null));
    }

    public OutputGeometryFormModel(OutputGeometryFormModel formModel) {
        this.sourceProduct = formModel.sourceProduct;
        this.targetCrs = formModel.targetCrs;
        this.fitProductSize = formModel.fitProductSize;
        this.referencePixelLocation = formModel.referencePixelLocation;
        this.propertyContainer = PropertyContainer.createMapBacked(new HashMap<String, Object>(), ImageGeometry.class);
        configurePropertyContainer(propertyContainer);
        Property[] properties = formModel.getPropertyContainer().getProperties();
        for (Property property : properties) {
            propertyContainer.setValue(property.getName(), property.getValue());
        }
    }

    private OutputGeometryFormModel(Product sourceProduct, ImageGeometry imageGeometry) {
        this.sourceProduct = sourceProduct;
        this.targetCrs = imageGeometry.getMapCrs();
        this.referencePixelLocation = REFERENCE_PIXEL_DEFAULT;
        this.fitProductSize = FIT_PRODUCT_SIZE_DEFAULT;
        this.propertyContainer =  PropertyContainer.createObjectBacked(imageGeometry);
        configurePropertyContainer(propertyContainer);
    }

    public PropertyContainer getPropertyContainer() {
        return propertyContainer;
    }

    public void setSourceProduct(Product sourceProduct) {
        this.sourceProduct = sourceProduct;
        updateProductSize();
    }

    public void setTargetCrs(CoordinateReferenceSystem targetCrs) {
        this.targetCrs = targetCrs;
        updateProductSize();
        setAxisUnits(propertyContainer);
    }

    public void resetToDefaults(ImageGeometry ig) {
        PropertyContainer pc =  PropertyContainer.createObjectBacked(ig);
        Property[] properties = pc.getProperties();
        for (Property property : properties) {
            propertyContainer.setValue(property.getName(), property.getValue());
        }
        propertyContainer.setValue("referencePixelLocation", REFERENCE_PIXEL_DEFAULT);
        propertyContainer.setValue("fitProductSize", FIT_PRODUCT_SIZE_DEFAULT);
    }

    private void configurePropertyContainer(PropertyContainer pc) {
        PropertyContainer thisVC = PropertyContainer.createObjectBacked(this);
        pc.addProperties(thisVC.getProperties());

        pc.getDescriptor("referencePixelLocation").setValueSet(new ValueSet(new Integer[]{0, 1, 2}));
        setAxisUnits(pc);
        pc.getDescriptor("orientation").setUnit("°");

        pc.addPropertyChangeListener(new ChangeListener());
    }

    private void setAxisUnits(PropertyContainer pc) {
        if (targetCrs != null) {
            String crsAxisUnit = targetCrs.getCoordinateSystem().getAxis(0).getUnit().toString();
            pc.getDescriptor("easting").setUnit(crsAxisUnit);
            pc.getDescriptor("northing").setUnit(crsAxisUnit);
            pc.getDescriptor("pixelSizeX").setUnit(crsAxisUnit);
            pc.getDescriptor("pixelSizeY").setUnit(crsAxisUnit);
        }
    }

    private void updateProductSize() {
        if (targetCrs != null && sourceProduct != null) {
            Double pixelSizeX = (Double) propertyContainer.getValue("pixelSizeX");
            Double pixelSizeY = (Double) propertyContainer.getValue("pixelSizeY");
            Rectangle productSize = ImageGeometry.calculateProductSize(sourceProduct, targetCrs, pixelSizeX, pixelSizeY);
            propertyContainer.setValue("width", productSize.width);
            propertyContainer.setValue("height", productSize.height);
        }
    }

    private void updateReferencePixel() {
        double referencePixelX = (Double) propertyContainer.getValue("referencePixelX");
        double referencePixelY = (Double) propertyContainer.getValue("referencePixelY");
        if (referencePixelLocation == 0) {
            referencePixelX = 0.5;
            referencePixelY = 0.5;
        } else if (referencePixelLocation == 1) {
            referencePixelX = 0.5 * (Integer) propertyContainer.getValue("width");
            referencePixelY = 0.5 * (Integer) propertyContainer.getValue("height");
        }
        propertyContainer.setValue("referencePixelX", referencePixelX);
        propertyContainer.setValue("referencePixelY", referencePixelY);
    }

    private class ChangeListener implements PropertyChangeListener {
        @Override
        public void propertyChange(PropertyChangeEvent event) {
            String propertyName = event.getPropertyName();

            if (fitProductSize && propertyName.startsWith("pixelSize")) {
                updateProductSize();
            }
            if (propertyName.startsWith("referencePixelLocation")) {
                updateReferencePixel();
            }
        }
    }
}
