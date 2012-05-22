/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.binning.operator.ui;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.accessors.DefaultPropertyAccessor;
import com.bc.ceres.swing.binding.BindingContext;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;
import org.esa.beam.binning.operator.BinningOp;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.BoundsInputPanel;
import org.esa.beam.util.FeatureUtils;
import org.esa.beam.util.logging.BeamLogManager;

import java.beans.PropertyChangeListener;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * The model responsible for managing the binning parameters.
 *
 * @author Olaf Danne
 * @author Thomas Storm
 */
class BinningModelImpl implements BinningModel {

    private PropertySet propertySet;
    private BindingContext bindingContext;

    public BinningModelImpl() {
        propertySet = new PropertyContainer();
        propertySet.addProperty(BinningDialog.createProperty(BoundsInputPanel.PROPERTY_EAST_BOUND, Float.class));
        propertySet.addProperty(BinningDialog.createProperty(BoundsInputPanel.PROPERTY_NORTH_BOUND, Float.class));
        propertySet.addProperty(BinningDialog.createProperty(BoundsInputPanel.PROPERTY_WEST_BOUND, Float.class));
        propertySet.addProperty(BinningDialog.createProperty(BoundsInputPanel.PROPERTY_SOUTH_BOUND, Float.class));
        propertySet.addProperty(BinningDialog.createProperty(BoundsInputPanel.PROPERTY_PIXEL_SIZE_X, Float.class));
        propertySet.addProperty(BinningDialog.createProperty(BoundsInputPanel.PROPERTY_PIXEL_SIZE_Y, Float.class));
        propertySet.addProperty(BinningDialog.createProperty(BinningModel.PROPERTY_KEY_GLOBAL, Boolean.class));
        propertySet.addProperty(BinningDialog.createProperty(BinningModel.PROPERTY_KEY_COMPUTE_REGION, Boolean.class));
        propertySet.addProperty(BinningDialog.createProperty(BinningModel.PROPERTY_KEY_REGION, Boolean.class));
        propertySet.addProperty(BinningDialog.createProperty(BinningModel.PROPERTY_KEY_EXPRESSION, String.class));
        propertySet.setDefaultValues();
    }

    @Override
    public Product[] getSourceProducts() {
        final Product[] products = getPropertyValue(BinningModel.PROPERTY_KEY_SOURCE_PRODUCTS);
        if (products == null) {
            return new Product[0];
        }
        return products;
    }

    @Override
    public TableRow[] getTableRows() {
        TableRow[] tableRows = getPropertyValue(PROPERTY_KEY_VARIABLE_CONFIGS);
        if (tableRows == null) {
            tableRows = new TableRow[0];
        }
        return tableRows;
    }

    @Override
    public String getRegion() {
        if (getPropertyValue(PROPERTY_KEY_GLOBAL) != null && (Boolean) getPropertyValue(PROPERTY_KEY_GLOBAL)) {
            return null;
        } else if (getPropertyValue(PROPERTY_KEY_COMPUTE_REGION) != null &&
                   (Boolean) getPropertyValue(PROPERTY_KEY_COMPUTE_REGION)) {
            final Product[] products = getPropertyValue(PROPERTY_KEY_SOURCE_PRODUCTS);
            Geometry currentGeometry = null;
            for (Product product : products) {
                if (product.getGeoCoding() == null) {
                    final String msg = MessageFormat.format(
                            "Product ''{0}'' contains no geo-information. Using the entire globe as region.",
                            product.getName());
                    BeamLogManager.getSystemLogger().warning(msg);
                    return null;
                }
                final Geometry geometry = FeatureUtils.createGeoBoundaryPolygon(product);
                if (currentGeometry == null) {
                    currentGeometry = geometry;
                } else {
                    currentGeometry = currentGeometry.union(geometry);
                }
            }

            return currentGeometry.toText();
        } else if (getPropertyValue(PROPERTY_KEY_REGION) != null && (Boolean) getPropertyValue(PROPERTY_KEY_REGION)) {
            final double westValue = (Double) getPropertyValue(BinningRegionPanel.PROPERTY_WEST_BOUND);
            final double eastValue = (Double) getPropertyValue(BinningRegionPanel.PROPERTY_EAST_BOUND);
            final double northValue = (Double) getPropertyValue(BinningRegionPanel.PROPERTY_NORTH_BOUND);
            final double southValue = (Double) getPropertyValue(BinningRegionPanel.PROPERTY_SOUTH_BOUND);
            Coordinate[] coordinates = {
                    new Coordinate(westValue, southValue), new Coordinate(westValue, northValue),
                    new Coordinate(eastValue, northValue), new Coordinate(eastValue, southValue),
                    new Coordinate(westValue, southValue)
            };

            final GeometryFactory geometryFactory = new GeometryFactory();
            final Polygon polygon = geometryFactory.createPolygon(geometryFactory.createLinearRing(coordinates), null);
            return polygon.toText();
        }
        throw new IllegalStateException("Cannot come here");
    }

    @Override
    public String getValidExpression() {
        return getPropertyValue(PROPERTY_KEY_EXPRESSION);
    }

    @Override
    public String getStartDate() {
        return getDate(PROPERTY_KEY_START_DATE);
    }

    @Override
    public String getEndDate() {
        return getDate(PROPERTY_KEY_END_DATE);
    }

    @Override
    public boolean shallOutputBinnedData() {
        if (getPropertyValue(PROPERTY_KEY_OUTPUT_BINNED_DATA) == null) {
            return false;
        }
        return (Boolean) getPropertyValue(PROPERTY_KEY_OUTPUT_BINNED_DATA);
    }

    @Override
    public int getSuperSampling() {
        if (getPropertyValue(PROPERTY_KEY_SUPERSAMPLING) == null) {
            return 1;
        }
        return (Integer) getPropertyValue(PROPERTY_KEY_SUPERSAMPLING);
    }

    private String getDate(String propertyKey) {
        if (getPropertyValue(PROPERTY_KEY_TEMPORAL_FILTER) != null &&
            (Boolean) getPropertyValue(PROPERTY_KEY_TEMPORAL_FILTER)) {
            final Calendar propertyValue = getPropertyValue(propertyKey);
            if (propertyValue == null) {
                return null;
            }
            final Date date = propertyValue.getTime();
            return new SimpleDateFormat(BinningOp.DATE_PATTERN).format(date);
        }
        return null;
    }

    @Override
    public int getNumRows() {
        if (getPropertyValue(PROPERTY_KEY_TARGET_HEIGHT) == null) {
            return 2160;
        }
        return (Integer) getPropertyValue(PROPERTY_KEY_TARGET_HEIGHT);
    }

    @Override
    public void setProperty(String key, Object value) throws ValidationException {
        final PropertyDescriptor descriptor;
        if (value == null) {
            descriptor = new PropertyDescriptor(key, Object.class);
        } else {
            descriptor = new PropertyDescriptor(key, value.getClass());
        }
        final Property property = new Property(descriptor, new DefaultPropertyAccessor());
        propertySet.addProperty(property);
        property.setValue(value);
        // todo -- replace by debug-level logging
        if (value != null && value.getClass().isArray()) {
            for (Object _value : (Object[]) value) {
                System.out.println("set property: 'key = " + key + ", value = " + _value + "'.");
            }
        } else {
            System.out.println("set property: 'key = " + key + ", value = " + value + "'.");
        }
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener propertyChangeListener) {
        propertySet.addPropertyChangeListener(propertyChangeListener);
    }

    @Override
    public BindingContext getBindingContext() {
        if (bindingContext == null) {
            bindingContext = new BindingContext(propertySet);
        }
        return bindingContext;
    }

    @SuppressWarnings("unchecked")
    <T> T getPropertyValue(String key) {
        final Property property = propertySet.getProperty(key);
        if (property != null) {
            return (T) property.getValue();
        }
        return null;
    }
}
