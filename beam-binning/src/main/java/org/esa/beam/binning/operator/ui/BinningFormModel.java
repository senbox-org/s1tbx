/*
 * Copyright (C) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
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
import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.accessors.DefaultPropertyAccessor;
import com.bc.ceres.swing.binding.BindingContext;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import org.esa.beam.binning.AggregatorConfig;
import org.esa.beam.binning.operator.BinningOp;
import org.esa.beam.binning.operator.VariableConfig;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.FlagCoding;
import org.esa.beam.framework.datamodel.Mask;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.esa.beam.framework.datamodel.VectorDataNode;
import org.esa.beam.framework.gpf.annotations.ParameterDescriptorFactory;
import org.esa.beam.util.StringUtils;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * The model responsible for managing the binning parameters.
 *
 * @author Thomas Storm
 */
class BinningFormModel {

    static final String PROPERTY_KEY_WEST_BOUND = "westBound";
    static final String PROPERTY_KEY_NORTH_BOUND = "northBound";
    static final String PROPERTY_KEY_EAST_BOUND = "eastBound";
    static final String PROPERTY_KEY_SOUTH_BOUND = "southBound";
    static final String PROPERTY_KEY_WKT = "manualWkt";
    static final String PROPERTY_KEY_AGGREGATOR_CONFIGS = "aggregatorConfigs";
    static final String PROPERTY_KEY_VARIABLE_CONFIGS = "variableConfigs";
    static final String PROPERTY_KEY_REGION = "region";
    static final String PROPERTY_KEY_BOUNDS = "bounds";
    static final String PROPERTY_KEY_COMPUTE_REGION = "compute";
    static final String PROPERTY_KEY_GLOBAL = "global";
    static final String PROPERTY_KEY_MASK_EXPR = "maskExpr";
    static final String PROPERTY_KEY_TIME_FILTER_METHOD = "timeFilterMethod";
    static final String PROPERTY_KEY_START_DATE_TIME = "startDateTime";
    static final String PROPERTY_KEY_PERIOD_DURATION = "periodDuration";
    static final String PROPERTY_KEY_MIN_DATA_HOUR = "minDataHour";
    static final String PROPERTY_KEY_NUM_ROWS = "numRows";
    static final String PROPERTY_KEY_SUPERSAMPLING = "superSampling";
    static final String PROPERTY_KEY_MANUAL_WKT = "manualWktKey";
    static final String PROPERTY_KEY_SOURCE_PRODUCTS = "sourceProducts";
    static final String PROPERTY_KEY_SOURCE_PRODUCT_PATHS = "sourceProductPaths";
    static final String PROPERTY_KEY_CONTEXT_SOURCE_PRODUCT = "contextSourceProduct";

    static final String GLOBAL_WKT = "polygon((-180 -90, 180 -90, 180 90, -180 90, -180 -90))";

    static final int DEFAULT_NUM_ROWS = 2160;

    private PropertySet propertySet;
    private BindingContext bindingContext;

    public BinningFormModel() {
        propertySet = ParameterDescriptorFactory.createMapBackedOperatorPropertyContainer("Binning");
        // Just for GUI
        propertySet.addProperty(createProperty(PROPERTY_KEY_GLOBAL, Boolean.class));                                    // temp
        propertySet.addProperty(createProperty(PROPERTY_KEY_COMPUTE_REGION, Boolean.class));                            // temp
        propertySet.addProperty(createProperty(PROPERTY_KEY_MANUAL_WKT, Boolean.class));                                // temp
        propertySet.addProperty(createProperty(PROPERTY_KEY_WKT, String.class));                                        // temp
        propertySet.addProperty(createProperty(PROPERTY_KEY_BOUNDS, Boolean.class));                                    // temp
        propertySet.addProperty(createProperty(PROPERTY_KEY_EAST_BOUND, Double.class));                                 // temp
        propertySet.addProperty(createProperty(PROPERTY_KEY_NORTH_BOUND, Double.class));                                // temp
        propertySet.addProperty(createProperty(PROPERTY_KEY_WEST_BOUND, Double.class));                                 // temp
        propertySet.addProperty(createProperty(PROPERTY_KEY_SOUTH_BOUND, Double.class));                                // temp
        propertySet.addProperty(createProperty(PROPERTY_KEY_SOURCE_PRODUCTS, Product[].class));                         // temp
        propertySet.addProperty(createProperty(PROPERTY_KEY_CONTEXT_SOURCE_PRODUCT, Product.class));                    // temp

        propertySet.setDefaultValues();

        propertySet.getProperty(PROPERTY_KEY_REGION).addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                Geometry newGeometry = (Geometry) evt.getNewValue();
                propertySet.setValue(PROPERTY_KEY_MANUAL_WKT, true);
                propertySet.setValue(PROPERTY_KEY_WKT, newGeometry.toText());
            }
        });
    }

    public Product[] getSourceProducts() {
        final Product[] products = getPropertyValue(BinningFormModel.PROPERTY_KEY_SOURCE_PRODUCTS);
        if (products == null) {
            return new Product[0];
        }
        return products;
    }

    public String[] getSourceProductPath() {
        return getPropertyValue(BinningFormModel.PROPERTY_KEY_SOURCE_PRODUCT_PATHS);
    }

    public Product getContextProduct() {
        return getPropertyValue(BinningFormModel.PROPERTY_KEY_CONTEXT_SOURCE_PRODUCT);
    }

    public void useAsContextProduct(Product contextProduct) {
        Product currentContextProduct = createContextProduct(contextProduct);
        propertySet.setValue(BinningFormModel.PROPERTY_KEY_CONTEXT_SOURCE_PRODUCT, currentContextProduct);
    }

    private Product createContextProduct(Product srcProduct) {
        if (srcProduct == null) {
            return null;
        }
        Product contextProduct = new Product("contextProduct", srcProduct.getProductType(), 10, 10);

        for (String flagCodingName : srcProduct.getFlagCodingGroup().getNodeNames()) {
            FlagCoding srcFC = srcProduct.getFlagCodingGroup().get(flagCodingName);
            String[] flagNames = srcFC.getFlagNames();
            FlagCoding contextFC = new FlagCoding(flagCodingName);
            for (String flagName : flagNames) {
                contextFC.addFlag(flagName, srcFC.getFlagMask(flagName), srcFC.getFlag(flagName).getDescription());
            }
            contextProduct.getFlagCodingGroup().add(contextFC);
        }
        for (Band srcBand : srcProduct.getBands()) {
            Band contextBand = contextProduct.addBand(srcBand.getName(), srcBand.getDataType());
            if (srcBand.isFlagBand()) {
                contextBand.setSampleCoding(contextProduct.getFlagCodingGroup().get(srcBand.getFlagCoding().getName()));
            }
        }
        for (TiePointGrid grid : srcProduct.getTiePointGrids()) {
            contextProduct.addTiePointGrid(new TiePointGrid(grid.getName(), 10, 10, 0, 0, 1, 1, new float[100]));
        }
        for (String vectorDataName : srcProduct.getVectorDataGroup().getNodeNames()) {
            VectorDataNode vectorData = srcProduct.getVectorDataGroup().get(vectorDataName);
            contextProduct.getVectorDataGroup().add(new VectorDataNode(vectorDataName, vectorData.getFeatureType()));
        }
        for (String maskName : srcProduct.getMaskGroup().getNodeNames()) {
            Mask mask = srcProduct.getMaskGroup().get(maskName);
            contextProduct.addMask(maskName, mask.getImageType());
        }
        VariableConfig[] variableConfigs = getVariableConfigs();
        for (VariableConfig variableConfig : variableConfigs) {
            contextProduct.addBand(new VariableConfigBand(variableConfig.getName()));
        }
        return contextProduct;
    }

    public AggregatorConfig[] getAggregatorConfigs() {
        AggregatorConfig[] aggregatorConfigs = getPropertyValue(PROPERTY_KEY_AGGREGATOR_CONFIGS);
        if (aggregatorConfigs == null) {
            aggregatorConfigs = new AggregatorConfig[0];
        }
        return aggregatorConfigs;
    }

    public VariableConfig[] getVariableConfigs() {
        VariableConfig[] variableConfigs = getPropertyValue(PROPERTY_KEY_VARIABLE_CONFIGS);
        if (variableConfigs == null) {
            variableConfigs = new VariableConfig[0];
        }
        return variableConfigs;
    }

    public void setVariableConfigs(VariableConfig[] variableConfigs) throws ValidationException {
        if (variableConfigs == null) {
            variableConfigs = new VariableConfig[0];
        }
        setProperty(PROPERTY_KEY_VARIABLE_CONFIGS, variableConfigs);
        Product contextProduct = getContextProduct();
        if (contextProduct != null) {
            removeAllVariableConfigBands(contextProduct);
            for (VariableConfig variableConfig : variableConfigs) {
                contextProduct.addBand(new VariableConfigBand(variableConfig.getName()));
            }
        }
    }

    void removeAllVariableConfigBands(Product contextProduct) {
        Band[] bands = contextProduct.getBands();
        for (Band band : bands) {
            if(band instanceof VariableConfigBand) {
                contextProduct.removeBand(band);
            }
        }
    }

    public Geometry getRegion() {
        if (Boolean.TRUE.equals(getPropertyValue(PROPERTY_KEY_GLOBAL))) {
            return toGeometry(GLOBAL_WKT);
        } else if (Boolean.TRUE.equals(getPropertyValue(PROPERTY_KEY_COMPUTE_REGION))) {
            return null;
        } else if (Boolean.TRUE.equals(getPropertyValue(PROPERTY_KEY_BOUNDS))) {
            final double westValue = getPropertyValue(PROPERTY_KEY_WEST_BOUND);
            final double eastValue = getPropertyValue(PROPERTY_KEY_EAST_BOUND);
            final double northValue = getPropertyValue(PROPERTY_KEY_NORTH_BOUND);
            final double southValue = getPropertyValue(PROPERTY_KEY_SOUTH_BOUND);
            Coordinate[] coordinates = {
                    new Coordinate(westValue, southValue), new Coordinate(westValue, northValue),
                    new Coordinate(eastValue, northValue), new Coordinate(eastValue, southValue),
                    new Coordinate(westValue, southValue)
            };

            final GeometryFactory geometryFactory = new GeometryFactory();
            return geometryFactory.createPolygon(geometryFactory.createLinearRing(coordinates), null);
        } else if (Boolean.TRUE.equals(getPropertyValue(PROPERTY_KEY_MANUAL_WKT))) {
            return toGeometry((String) getPropertyValue(PROPERTY_KEY_WKT));
        }
        throw new IllegalStateException("Should never come here");
    }

    Geometry toGeometry(String wkt) {
        try {
            return new WKTReader().read(wkt);
        } catch (ParseException e) {
            throw new IllegalStateException("WKT for region is not valid:\n" + wkt);
        }
    }

    public String getMaskExpr() {
        final String propertyValue = getPropertyValue(PROPERTY_KEY_MASK_EXPR);
        if (StringUtils.isNullOrEmpty(propertyValue)) {
            return "true";
        }
        return propertyValue;
    }

    public BinningOp.TimeFilterMethod getTimeFilterMethod() {
        return propertySet.getProperty(PROPERTY_KEY_TIME_FILTER_METHOD).getValue();
    }

    public String getStartDateTime() {
        BinningOp.TimeFilterMethod temporalFilter = getPropertyValue(PROPERTY_KEY_TIME_FILTER_METHOD);
        switch (temporalFilter) {
            case NONE: {
                return null;
            }
            case TIME_RANGE:
            case SPATIOTEMPORAL_DATA_DAY: {
                return getPropertyValue(PROPERTY_KEY_START_DATE_TIME);
            }
        }
        throw new IllegalStateException("Illegal temporal filter method: '" + temporalFilter + "'");
    }

    public Double getPeriodDuration() {
        return getPropertyValue(PROPERTY_KEY_PERIOD_DURATION);
    }

    public Double getMinDataHour() {
        return getPropertyValue(PROPERTY_KEY_MIN_DATA_HOUR);
    }

    public int getSuperSampling() {
        if (getPropertyValue(PROPERTY_KEY_SUPERSAMPLING) == null) {
            return 1;
        }
        return (Integer) getPropertyValue(PROPERTY_KEY_SUPERSAMPLING);
    }

    public int getNumRows() {
        if (getPropertyValue(PROPERTY_KEY_NUM_ROWS) == null) {
            return DEFAULT_NUM_ROWS;
        }
        return (Integer) getPropertyValue(PROPERTY_KEY_NUM_ROWS);
    }

    public void setProperty(String key, Object value) throws ValidationException {
        if (propertySet.isPropertyDefined(key)) {
            final Property property = propertySet.getProperty(key);
            property.setValue(value);
        } else {
            throw new IllegalStateException("Unknown property: " + key);
        }
    }

    public void addPropertyChangeListener(PropertyChangeListener propertyChangeListener) {
        propertySet.addPropertyChangeListener(propertyChangeListener);
    }

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

    private static Property createProperty(String name, Class type) {
        final DefaultPropertyAccessor defaultAccessor = new DefaultPropertyAccessor();
        final PropertyDescriptor descriptor = new PropertyDescriptor(name, type);
        descriptor.setDefaultConverter();
        return new Property(descriptor, defaultAccessor);
    }

    private static class VariableConfigBand extends Band {

        public VariableConfigBand(String varName) {
            super(varName, ProductData.TYPE_FLOAT32, 10, 10);
        }
    }

}
