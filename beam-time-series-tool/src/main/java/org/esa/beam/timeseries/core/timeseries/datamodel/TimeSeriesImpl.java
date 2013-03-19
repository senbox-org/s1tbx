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

package org.esa.beam.timeseries.core.timeseries.datamodel;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.ImageInfo;
import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Placemark;
import org.esa.beam.framework.datamodel.PlacemarkGroup;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.datamodel.ProductNodeEvent;
import org.esa.beam.framework.datamodel.ProductNodeListenerAdapter;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.timeseries.core.insitu.InsituSource;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.util.StringUtils;
import org.esa.beam.util.logging.BeamLogManager;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <p><i>Note that this class is not yet public API. Interface may change in future releases.</i></p>
 *
 * @author Thomas Storm
 */
final class TimeSeriesImpl extends AbstractTimeSeries {

    private final Map<RasterDataNode, TimeCoding> rasterTimeMap = new WeakHashMap<RasterDataNode, TimeCoding>();
    private final List<TimeSeriesListener> listeners = new ArrayList<TimeSeriesListener>();
    private final AxisMapping axisMapping = new AxisMapping();
    private final Map<Placemark, GeoPos> pinRelationMap = new HashMap<Placemark, GeoPos>();

    private Product tsProduct;
    private List<ProductLocation> productLocationList;
    private Map<String, Product> productTimeMap;
    private InsituSource insituSource;

    private Set<String> insituVariablesSelections = new HashSet<String>();
    private volatile boolean isAdjustingImageInfos;

    /**
     * Used to create a TimeSeries from within a ProductReader
     *
     * @param tsProduct the product read
     */
    TimeSeriesImpl(Product tsProduct) {
        init(tsProduct);
        productLocationList = getProductLocations();
        storeProductsInMap();
        setSourceImages();
        fixBandTimeCodings();
        updateAutoGrouping();
        initImageInfos();
    }

    /**
     * Used to create a new TimeSeries from the user interface.
     *
     * @param tsProduct        the newly created time series product
     * @param productLocations the product location to be used
     * @param variableNames    the currently selected names of variables
     */
    TimeSeriesImpl(Product tsProduct, List<ProductLocation> productLocations, List<String> variableNames) {
        init(tsProduct);
        for (ProductLocation location : productLocations) {
            addProductLocation(location);
        }
        storeProductsInMap();
        for (String variable : variableNames) {
            setEoVariableSelected(variable, true);
        }
        setProductTimeCoding(tsProduct);
        initImageInfos();
    }

    @Override
    public Product getTsProduct() {
        return tsProduct;
    }

    @Override
    public List<ProductLocation> getProductLocations() {
        MetadataElement tsElem = tsProduct.getMetadataRoot().getElement(TIME_SERIES_ROOT_NAME);
        MetadataElement productListElem = tsElem.getElement(PRODUCT_LOCATIONS);
        MetadataElement[] productElems = productListElem.getElements();
        List<ProductLocation> productLocations = new ArrayList<ProductLocation>(productElems.length);
        for (MetadataElement productElem : productElems) {
            String path = productElem.getAttributeString(PL_PATH);
            String type = productElem.getAttributeString(PL_TYPE);
            productLocations.add(new ProductLocation(ProductLocationType.valueOf(type), path));
        }
        return productLocations;
    }

    @Override
    public List<String> getEoVariables() {
        MetadataElement[] variableElems = getVariableMetadataElements();
        List<String> variables = new ArrayList<String>();
        for (MetadataElement varElem : variableElems) {
            variables.add(varElem.getAttributeString(VARIABLE_NAME));
        }
        return variables;
    }

    @Override
    public void addProductLocation(ProductLocation productLocation) {
        if (productLocationList == null) {
            productLocationList = new ArrayList<ProductLocation>();
        }
        if (!productLocationList.contains(productLocation)) {
            final Logger logger = BeamLogManager.getSystemLogger();
            final ProductLocationType type = productLocation.getProductLocationType();
            final String path = productLocation.getPath();
            logger.log(Level.INFO, "Try to load product location type: '" + type + "' at path: '" + path + "'");
            addProductLocationMetadata(productLocation);
            productLocationList.add(productLocation);
            List<String> variables = getEoVariables();

            final Map<String, Product> products = productLocation.getProducts();
            for (Map.Entry<String, Product> productEntry : products.entrySet()) {
                final Product product = productEntry.getValue();

//                  todo - see jira issue AQUAMAR-4
//                  test if the added source product is compatible with the existing time series product (CRS, width, height)
//                  If not ... reproject the product before it can be added.
                if (product.getStartTime() != null) {
                    addProductMetadata(productEntry);
                    addToVariableList(product);
                    for (String variable : variables) {
                        if (isEoVariableSelected(variable)) {
                            addSpecifiedBandOfGivenProduct(variable, product);
                        }
                    }
                } else {
                    // todo log in gui as well as in console
                    final String absolutePath = product.getFileLocation().getAbsolutePath();
                    logger.log(Level.WARNING, "The product '" + absolutePath + "' does not contain time information.");
                }
            }
            fireChangeEvent(new TimeSeriesChangeEvent(TimeSeriesChangeEvent.PROPERTY_PRODUCT_LOCATIONS,
                                                      productLocationList, this));
        }
    }

    private void addProductMetadata(Map.Entry<String, Product> productEntry) {
        MetadataElement productElement = tsProduct.getMetadataRoot().
                    getElement(TIME_SERIES_ROOT_NAME).
                    getElement(SOURCE_PRODUCT_PATHS);
        ProductData productPath = ProductData.createInstance(productEntry.getKey());
        int length = productElement.getElements().length + 1;
        MetadataElement elem = new MetadataElement(String.format("%s.%s", SOURCE_PRODUCT_PATHS, Integer.toString(length)));
        elem.addAttribute(new MetadataAttribute(PL_PATH, productPath, true));
        productElement.addElement(elem);
    }

    @Override
    public void removeProductLocation(ProductLocation productLocation) {
        // remove metadata
        final MetadataElement timeSeriesRootElement = tsProduct.getMetadataRoot().getElement(TIME_SERIES_ROOT_NAME);
        MetadataElement productLocationsElement = timeSeriesRootElement.getElement(PRODUCT_LOCATIONS);
        removeAttributeWithValue(PL_PATH, productLocation.getPath(), productLocationsElement);
        // remove variables for this productLocation
        updateAutoGrouping(); // TODO ???

        final Band[] bands = tsProduct.getBands();
        final MetadataElement sourceProductPaths = timeSeriesRootElement.getElement(SOURCE_PRODUCT_PATHS);
        for (Map.Entry<String, Product> productEntry : productLocation.getProducts().entrySet()) {
            final Product product = productEntry.getValue();
            removeAttributeWithValue(PL_PATH, productEntry.getKey(), sourceProductPaths);
            String timeString = formatTimeString(product);
            productTimeMap.remove(timeString);
            for (Band band : bands) {
                if (band.getName().endsWith(timeString)) {
                    tsProduct.removeBand(band);
                }
            }
        }
        productLocation.closeProducts();
        productLocationList.remove(productLocation);

        fireChangeEvent(new TimeSeriesChangeEvent(TimeSeriesChangeEvent.PROPERTY_PRODUCT_LOCATIONS,
                                                  productLocationList, this));
    }

    private void removeAttributeWithValue(String attributeName, String value, MetadataElement parentElement) {
        final MetadataElement[] childElements = parentElement.getElements();
        for (MetadataElement elem : childElements) {
            if (elem.getAttributeString(attributeName).equals(value)) {
                parentElement.removeElement(elem);
                return;
            }
        }
    }

    private Band getSourceBand(String destBandName) {
        final int lastUnderscore = destBandName.lastIndexOf(SEPARATOR);
        String normalizedBandName = destBandName.substring(0, lastUnderscore);
        String timePart = destBandName.substring(lastUnderscore + 1);
        Product srcProduct = productTimeMap.get(timePart);
        if (srcProduct == null) {
            return null;
        }
        for (Band band : srcProduct.getBands()) {
            if (normalizedBandName.equals(band.getName())) {
                return band;
            }
        }
        return null;
    }

    private void setSourceImages() {
        for (Band destBand : tsProduct.getBands()) {
            final Band raster = getSourceBand(destBand.getName());
            if (raster != null) {
                destBand.setSourceImage(raster.getSourceImage());
            }
        }
    }

    private void fixBandTimeCodings() {
        for (Band destBand : tsProduct.getBands()) {
            final String destBandName = destBand.getName();
            final Band raster = getSourceBand(destBandName);
            final TimeCoding timeCoding;
            if (raster != null) {
                timeCoding = GridTimeCoding.create(raster.getProduct());
            } else {
                ProductData.UTC time = extractUtcTime(destBandName);
                timeCoding = new GridTimeCoding(time, time);
            }
            rasterTimeMap.put(destBand, timeCoding);
        }
    }

    private ProductData.UTC extractUtcTime(String name) {
        final String timePart = name.substring(name.length() - DATE_FORMAT.length());
        try {
            return ProductData.UTC.parse(timePart, DATE_FORMAT.substring(0, DATE_FORMAT.lastIndexOf(".")));
        } catch (ParseException e) {
            throw new IllegalStateException("The raster name '" + name + "' does not contain the time sequence. " + DATE_FORMAT);
        }
    }

    private void init(Product product) {
        this.tsProduct = product;
        productTimeMap = new HashMap<String, Product>();
        createTimeSeriesMetadataStructure(product);

        // to reconstruct the source image which will be nulled when
        // a product is reopened after saving
        tsProduct.addProductNodeListener(new SourceImageReconstructor());
        axisMapping.addAxisMappingListener(new AxisMappingListener());
    }

    private void storeProductsInMap() {
        for (Product product : getAllProducts()) {
            productTimeMap.put(formatTimeString(product), product);
        }
    }

    @Override
    public boolean isEoVariableSelected(String variableName) {
        final MetadataElement[] variables = getVariableMetadataElements();
        for (MetadataElement elem : variables) {
            if (elem.getAttributeString(VARIABLE_NAME).equals(variableName)) {
                return Boolean.parseBoolean(elem.getAttributeString(VARIABLE_SELECTION));
            }
        }
        return false;
    }

    @Override
    public void setEoVariableSelected(String variableName, boolean selected) {
        // set in metadata
        final MetadataElement[] variables = getVariableMetadataElements();
        for (MetadataElement elem : variables) {
            if (elem.getAttributeString(VARIABLE_NAME).equals(variableName)) {
                elem.setAttributeString(VARIABLE_SELECTION, String.valueOf(selected));
            }
        }
        // set in product
        if (selected) {
            for (Product product : getAllProducts()) {
                addSpecifiedBandOfGivenProduct(variableName, product);
            }
        } else {
            final Band[] bands = tsProduct.getBands();
            for (Band band : bands) {
                if (variableName.equals(rasterToVariableName(band.getName()))) {
                    tsProduct.removeBand(band);
                }
            }
        }
        fireChangeEvent(new TimeSeriesChangeEvent(TimeSeriesChangeEvent.PROPERTY_EO_VARIABLE_SELECTION, null, this));
    }

    @Override
    public boolean isInsituVariableSelected(String variableName) {
        return insituVariablesSelections.contains(variableName);
    }

    @Override
    public void setInsituVariableSelected(String variableName, boolean selected) {
        boolean hasChanged;
        if (selected) {
            hasChanged = insituVariablesSelections.add(variableName);
        } else {
            hasChanged = insituVariablesSelections.remove(variableName);
        }
        if (hasChanged) {
            fireChangeEvent(new TimeSeriesChangeEvent(TimeSeriesChangeEvent.PROPERTY_INSITU_VARIABLE_SELECTION, variableName, this));
        }
    }

    @Override
    public List<Band> getBandsForVariable(String variableName) {
        final List<Band> bands = new ArrayList<Band>();
        for (Band band : tsProduct.getBands()) {
            if (variableName.equals(rasterToVariableName(band.getName()))) {
                bands.add(band);
            }
        }
        sortBands(bands);
        return bands;
    }

    @Override
    public List<Band> getBandsForProductLocation(ProductLocation location) {
        final List<Band> bands = new ArrayList<Band>();
        Map<String, Product> products = location.getProducts();
        for (Product product : products.values()) {
            String timeString = formatTimeString(product);
            // TODO relies on one timecoding per product... thats not good (mz, ts, 2010-07-12)
            for (Band band : tsProduct.getBands()) {
                if (band.getName().endsWith(timeString)) {
                    bands.add(band);
                }
            }
        }
        return bands;
    }

    @Override
    public Map<RasterDataNode, TimeCoding> getRasterTimeMap() {
        return Collections.unmodifiableMap(rasterTimeMap);
    }

    @Override
    public boolean isAutoAdjustingTimeCoding() {
        final MetadataElement tsRootElement = tsProduct.getMetadataRoot().getElement(TIME_SERIES_ROOT_NAME);
        if (!tsRootElement.containsAttribute(AUTO_ADJUSTING_TIME_CODING)) {
            setAutoAdjustingTimeCoding(true);
        }
        final String autoAdjustString = tsRootElement.getAttributeString(AUTO_ADJUSTING_TIME_CODING);
        return Boolean.parseBoolean(autoAdjustString);
    }

    @Override
    public void setAutoAdjustingTimeCoding(boolean autoAdjust) {
        final MetadataElement tsRootElement = tsProduct.getMetadataRoot().getElement(TIME_SERIES_ROOT_NAME);
        tsRootElement.setAttributeString(AUTO_ADJUSTING_TIME_CODING, Boolean.toString(autoAdjust));
    }


    @Override
    public boolean isProductCompatible(Product product, String rasterName) {
        return product.containsRasterDataNode(rasterName) &&
               tsProduct.isCompatibleProduct(product, 0.1e-6f);
    }

    @Override
    public TimeCoding getTimeCoding() {
        return GridTimeCoding.create(tsProduct);
    }

    @Override
    public void setTimeCoding(TimeCoding timeCoding) {
        final ProductData.UTC startTime = timeCoding.getStartTime();
        if (tsProduct.getStartTime().getAsCalendar().compareTo(startTime.getAsCalendar()) != 0) {
            tsProduct.setStartTime(startTime);
            fireChangeEvent(new TimeSeriesChangeEvent(TimeSeriesChangeEvent.START_TIME_PROPERTY_NAME, startTime, this));
        }
        final ProductData.UTC endTime = timeCoding.getEndTime();
        if (tsProduct.getEndTime().getAsCalendar().compareTo(endTime.getAsCalendar()) != 0) {
            tsProduct.setEndTime(endTime);
            fireChangeEvent(new TimeSeriesChangeEvent(TimeSeriesChangeEvent.END_TIME_PROPERTY_NAME, endTime, this));
        }
        List<String> variables = getEoVariables();
        for (Product product : getAllProducts()) {
            for (String variable : variables) {
                if (isEoVariableSelected(variable)) {
                    addSpecifiedBandOfGivenProduct(variable, product);
                }
            }
        }
        for (Band band : tsProduct.getBands()) {
            final TimeCoding bandTimeCoding = getRasterTimeMap().get(band);
            if (!timeCoding.contains(bandTimeCoding)) {
                fireChangeEvent(new TimeSeriesChangeEvent(TimeSeriesChangeEvent.BAND_TO_BE_REMOVED, band, this));
                tsProduct.removeBand(band);
            }
        }
    }


    @Override
    public void addTimeSeriesListener(TimeSeriesListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
            tsProduct.addProductNodeListener(listener);
        }
    }

    @Override
    public void removeTimeSeriesListener(TimeSeriesListener listener) {
        listeners.remove(listener);
        tsProduct.removeProductNodeListener(listener);
    }

    @Override
    public void setInsituSource(InsituSource insituSource) {
        if (this.insituSource != insituSource) {
            this.insituSource = insituSource;
            fireChangeEvent(new TimeSeriesChangeEvent(TimeSeriesChangeEvent.INSITU_SOURCE_CHANGED, this, this));
        }
    }

    @Override
    public InsituSource getInsituSource() {
        return insituSource;
    }

    @Override
    public void clearInsituPlacemarks() {
        final PlacemarkGroup pinGroup = tsProduct.getPinGroup();
        for (Placemark insituPin : pinRelationMap.keySet()) {
            pinGroup.remove(insituPin);
        }
        pinRelationMap.clear();
    }

    @Override
    public GeoPos getInsituGeoposFor(Placemark placemark) {
        return pinRelationMap.get(placemark);
    }

    @Override
    public void registerRelation(Placemark placemark, GeoPos insituGeopos) {
        pinRelationMap.put(placemark, insituGeopos);
        tsProduct.getPinGroup().add(placemark);
    }

    @Override
    public boolean hasInsituData() {
        return insituSource != null && !insituVariablesSelections.isEmpty();
    }

    @Override
    public Set<String> getSelectedInsituVariables() {
        return Collections.unmodifiableSet(insituVariablesSelections);
    }

    @Override
    public AxisMapping getAxisMapping() {
        return axisMapping;
    }

    /////////////////////////////////////////////////////////////////////////////////
    // private methods
    /////////////////////////////////////////////////////////////////////////////////

    private MetadataElement[] getVariableMetadataElements() {
        MetadataElement variableListElement = tsProduct.getMetadataRoot().
                    getElement(TIME_SERIES_ROOT_NAME).
                    getElement(VARIABLES);
        return variableListElement.getElements();
    }

    private List<Product> getAllProducts() {
        List<Product> result = new ArrayList<Product>();
        for (ProductLocation productLocation : productLocationList) {
            for (Product product : productLocation.getProducts().values()) {
                result.add(product);
            }
        }
        return result;
    }

    private boolean isTimeCodingSet() {
        return tsProduct.getStartTime() != null;
    }

    private void adjustImageInfos(RasterDataNode raster) {
        if (!isAdjustingImageInfos) {
            try {
                isAdjustingImageInfos = true;
                final String variableName = AbstractTimeSeries.rasterToVariableName(raster.getName());
                final List<Band> bandList = getBandsForVariable(variableName);
                final ImageInfo imageInfo = raster.getImageInfo(ProgressMonitor.NULL);
                if (imageInfo != null) {
                    for (Band band : bandList) {
                        if (band != raster) {
                            band.setImageInfo(imageInfo.createDeepCopy());
                        }
                    }
                }
            } finally {
                isAdjustingImageInfos = false;
            }
        }
    }

    private void sortBands(List<Band> bandList) {
        Collections.sort(bandList, new Comparator<Band>() {
            @Override
            public int compare(Band band1, Band band2) {
                final Date date1 = rasterTimeMap.get(band1).getStartTime().getAsDate();
                final Date date2 = rasterTimeMap.get(band2).getStartTime().getAsDate();
                return date1.compareTo(date2);
            }
        });
    }

    private void updateAutoGrouping() {
        tsProduct.setAutoGrouping(StringUtils.join(getEoVariables(), ":"));
    }

    private void setProductTimeCoding(Product tsProduct) {
        for (Band band : tsProduct.getBands()) {
            final ProductData.UTC rasterStartTime = getRasterTimeMap().get(band).getStartTime();
            final ProductData.UTC rasterEndTime = getRasterTimeMap().get(band).getEndTime();

            ProductData.UTC tsStartTime = tsProduct.getStartTime();
            if (tsStartTime == null || rasterStartTime.getAsDate().before(tsStartTime.getAsDate())) {
                tsProduct.setStartTime(rasterStartTime);
            }
            ProductData.UTC tsEndTime = tsProduct.getEndTime();
            if (rasterEndTime != null) {
                if (tsEndTime == null || rasterEndTime.getAsDate().after(tsEndTime.getAsDate())) {
                    tsProduct.setEndTime(rasterEndTime);
                }
            }
        }
    }

    private static void createTimeSeriesMetadataStructure(Product tsProduct) {
        if (!tsProduct.getMetadataRoot().containsElement(TIME_SERIES_ROOT_NAME)) {
            final MetadataElement timeSeriesRoot = new MetadataElement(TIME_SERIES_ROOT_NAME);
            final MetadataElement productListElement = new MetadataElement(PRODUCT_LOCATIONS);
            final MetadataElement sourceProductPathsElement = new MetadataElement(SOURCE_PRODUCT_PATHS);
            final MetadataElement variablesListElement = new MetadataElement(VARIABLES);
            timeSeriesRoot.addElement(productListElement);
            timeSeriesRoot.addElement(sourceProductPathsElement);
            timeSeriesRoot.addElement(variablesListElement);
            tsProduct.getMetadataRoot().addElement(timeSeriesRoot);
        }
    }

    private void addProductLocationMetadata(ProductLocation productLocation) {
        MetadataElement productLocationsElement = tsProduct.getMetadataRoot().
                    getElement(TIME_SERIES_ROOT_NAME).
                    getElement(PRODUCT_LOCATIONS);
        // @todo - nur produkt pfade, keine Verzeichnisse
        ProductData productPath = ProductData.createInstance(productLocation.getPath());
        ProductData productType = ProductData.createInstance(productLocation.getProductLocationType().toString());
        int length = productLocationsElement.getElements().length + TimeSeriesChangeEvent.BAND_TO_BE_REMOVED;
        MetadataElement elem = new MetadataElement(
                    String.format("%s.%s", PRODUCT_LOCATIONS, Integer.toString(length)));
        elem.addAttribute(new MetadataAttribute(PL_PATH, productPath, true));
        elem.addAttribute(new MetadataAttribute(PL_TYPE, productType, true));
        productLocationsElement.addElement(elem);
    }

    private static String formatTimeString(Product product) {
        final SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT, Locale.ENGLISH);
        final ProductData.UTC startTime = product.getStartTime();
        return dateFormat.format(startTime.getAsDate());
    }

    private void addToVariableList(Product product) {
        final List<String> newVariables = new ArrayList<String>();
        final List<String> variables = getEoVariables();
        final Band[] bands = product.getBands();
        for (Band band : bands) {
            final String bandName = band.getName();
            boolean varExist = false;
            for (String variable : variables) {
                varExist |= variable.equals(bandName);
            }
            if (!varExist) {
                newVariables.add(bandName);
            }
        }
        for (String variable : newVariables) {
            addVariableToMetadata(variable);
        }
        if (!newVariables.isEmpty()) {
            updateAutoGrouping();
        }
    }

    private void addVariableToMetadata(String variable) {
        MetadataElement variableListElement = tsProduct.getMetadataRoot().
                    getElement(TIME_SERIES_ROOT_NAME).
                    getElement(VARIABLES);
        final ProductData variableName = ProductData.createInstance(variable);
        final ProductData isSelected = ProductData.createInstance(Boolean.toString(false));
        int length = variableListElement.getElements().length + 1;
        MetadataElement elem = new MetadataElement(String.format("%s.%s", VARIABLES, Integer.toString(length)));
        elem.addAttribute(new MetadataAttribute(VARIABLE_NAME, variableName, true));
        elem.addAttribute(new MetadataAttribute(VARIABLE_SELECTION, isSelected, true));
        variableListElement.addElement(elem);
    }

    private void addSpecifiedBandOfGivenProduct(String nodeName, Product product) {
        if (isProductCompatible(product, nodeName)) {
            final RasterDataNode raster = product.getRasterDataNode(nodeName);
            TimeCoding rasterTimeCoding = GridTimeCoding.create(product);
            final ProductData.UTC rasterStartTime = rasterTimeCoding.getStartTime();
            final ProductData.UTC rasterEndTime = rasterTimeCoding.getEndTime();
            Guardian.assertNotNull("rasterStartTime", rasterStartTime);
            final String bandName = variableToRasterName(nodeName, rasterTimeCoding);

            if (!tsProduct.containsBand(bandName)) {
                // band not already contained
                if (isAutoAdjustingTimeCoding() || !isTimeCodingSet()) {
                    // automatically setting time coding
                    // OR
                    // first band to add to time series; time bounds of this band will be used
                    // as ts-product's time bounds, no matter if auto adjust is true or false
                    autoAdjustTimeInformation(rasterStartTime, rasterEndTime);
                }
                if (getTimeCoding().contains(rasterTimeCoding)) {
                    // add only bands which are in the time bounds
                    final Band addedBand = addBand(raster, rasterTimeCoding, bandName);
                    final List<Band> bandsForVariable = getBandsForVariable(nodeName);
                    if (!bandsForVariable.isEmpty()) {
                        final ImageInfo imageInfo = bandsForVariable.get(0).getImageInfo(ProgressMonitor.NULL);
                        addedBand.setImageInfo(imageInfo.createDeepCopy());
                    }

                }
                // todo no bands added message
            }
        }
    }

    private Band addBand(RasterDataNode raster, TimeCoding rasterTimeCoding, String bandName) {
        final Band band = new Band(bandName, raster.getDataType(), tsProduct.getSceneRasterWidth(),
                                   tsProduct.getSceneRasterHeight());
        band.setSourceImage(raster.getSourceImage());
        ProductUtils.copyRasterDataNodeProperties(raster, band);
//                todo copy also referenced band in valid pixel expression
        band.setValidPixelExpression(null);
        rasterTimeMap.put(band, rasterTimeCoding);
        tsProduct.addBand(band);
        return band;
    }

    private void autoAdjustTimeInformation(ProductData.UTC rasterStartTime, ProductData.UTC rasterEndTime) {
        ProductData.UTC tsStartTime = tsProduct.getStartTime();
        if (tsStartTime == null || rasterStartTime.getAsDate().before(tsStartTime.getAsDate())) {
            tsProduct.setStartTime(rasterStartTime);
        }
        ProductData.UTC tsEndTime = tsProduct.getEndTime();
        if (tsEndTime == null || rasterEndTime.getAsDate().after(tsEndTime.getAsDate())) {
            tsProduct.setEndTime(rasterEndTime);
        }
    }

    private void initImageInfos() {
        for (String variable : getEoVariables()) {
            if (isEoVariableSelected(variable)) {
                final List<Band> bandList = getBandsForVariable(variable);
                adjustImageInfos(bandList.get(0));
            }
        }
    }

    private void fireChangeEvent(TimeSeriesChangeEvent event) {
        final ArrayList<TimeSeriesListener> listenersCopy = new ArrayList<TimeSeriesListener>();
        listenersCopy.addAll(listeners);
        for (TimeSeriesListener listener : listenersCopy) {
            listener.timeSeriesChanged(event);
        }
    }

    private class SourceImageReconstructor extends ProductNodeListenerAdapter {

        @Override
        public void nodeChanged(ProductNodeEvent event) {
            if ("sourceImage".equals(event.getPropertyName()) &&
                event.getOldValue() != null &&
                event.getNewValue() == null) {
                ProductNode productNode = event.getSourceNode();
                if (productNode instanceof Band) {
                    Band destBand = (Band) productNode;
                    final Band sourceBand = getSourceBand(destBand.getName());
                    if (sourceBand != null) {
                        destBand.setSourceImage(sourceBand.getSourceImage());
                    }
                }
            }
            if (RasterDataNode.PROPERTY_NAME_IMAGE_INFO.equals(event.getPropertyName())) {
                if (event.getSourceNode() instanceof RasterDataNode) {
                    adjustImageInfos((RasterDataNode) event.getSourceNode());
                }
            }
        }
    }

    private class AxisMappingListener implements AxisMapping.AxisMappingListener {

        @Override
        public void hasChanged() {
            fireChangeEvent(new TimeSeriesChangeEvent(TimeSeriesChangeEvent.PROPERTY_AXIS_MAPPING_CHANGED, null, TimeSeriesImpl.this));
        }
    }
}
