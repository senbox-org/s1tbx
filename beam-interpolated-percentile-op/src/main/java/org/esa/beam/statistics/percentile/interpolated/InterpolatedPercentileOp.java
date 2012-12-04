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

package org.esa.beam.statistics.percentile.interpolated;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.Converter;
import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.dataio.dimap.DimapProductConstants;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.dataio.ProductWriter;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.CrsGeoCoding;
import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProducts;
import org.esa.beam.util.DateTimeUtils;
import org.esa.beam.util.StringUtils;
import org.esa.beam.util.jai.JAIUtils;
import org.esa.beam.util.math.MathUtils;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;

/**
 * An operator that is used to compute statistics for any number of source products, restricted to regions given by an
 * ESRI shapefile.
 * <p/>
 * It writes two different sorts of output:<br/>
 * <ul>
 * <li>an ASCII file in tab-separated CSV format, in which the statistics are mapped to the source regions</li>
 * <li>a shapefile that corresponds to the input shapefile, enriched with the statistics for the regions defined by the shapefile</li>
 * </ul>
 * <p/>
 * Unlike most other operators, that can compute single {@link org.esa.beam.framework.gpf.Tile tiles},
 * the statistics operator processes all of its source products in its {@link #initialize()} method.
 *
 * @author Sabine Embacher
 * @author Tonio Fincke
 * @author Thomas Storm
 */
@OperatorMetadata(alias = "InterpolatedPercentile",
                  version = "1.0",
                  authors = "Sabine Embacher, Marco Peters, Tonio Fincke",
                  copyright = "(c) 2012 by Brockmann Consult GmbH",
                  description = "Computes percentiles over the time for an arbitrary number of source products.")
public class InterpolatedPercentileOp extends Operator {

    // todo start ... these two constant fields are copied from time series tool
    public static final String BAND_DATE_FORMAT = "yyyyMMdd.HHmmss.SSS";
    public static final String TIME_SERIES_PRODUCT_TYPE = "org.esa.beam.glob.timeseries";
    public static final String TIME_SERIES_METADATA_ROOT_NAME = "TIME_SERIES";
    public static final String PRODUCT_LOCATIONS = "PRODUCT_LOCATIONS";
    public static final String TIME_SERIES_METADATA_VARIABLES_NAME = "VARIABLES";
    public static final String TIME_SERIES_METADATA_VARIABLE_ATTRIBUTE_NAME = "NAME";
    public static final String VARIABLE_SELECTION = "SELECTION";
    // todo end

    public static final String DATETIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
    private static final String SUFFIX_PERCENTILE_OP_DATA_PRODUCT = "_PercentileOpDataProduct";
    private static final String UNABLE_TO_WRITE_TIMESERIES_DATA_PRODUCT = "Unable to write timeseries data product.";

    @SourceProducts(description = "Don't use this parameter. Use sourceProductPaths instead")
    Product[] sourceProducts;

    @Parameter(description = "A comma-separated list of file paths specifying the source products.\n" +
                             "Source products to be considered for percentile computation. \n" +
                             "Each path may contain the wildcards '**' (matches recursively any directory),\n" +
                             "'*' (matches any character sequence in path names) and\n" +
                             "'?' (matches any single character).\n" +
                             "If, for example, all NetCDF files under /eodata/ shall be considered, use '/eodata/**/*.nc'.")
    String[] sourceProductPaths;

    @Parameter(description = "The start date. If not given, taken from the 'oldest' source product. Products that " +
                             "have a start date before the start date given by this parameter are not considered.",
               format = DATETIME_PATTERN, converter = UtcConverter.class)
    ProductData.UTC startDate;

    @Parameter(description = "The end date. If not given, taken from the 'youngest' source product. Products that " +
                             "have an end date after the end date given by this parameter are not considered.",
               format = DATETIME_PATTERN, converter = UtcConverter.class)
    ProductData.UTC endDate;

    @Parameter(description = "The band configuration. This configuration determines the input of the operator.",
               alias = "bandConfiguration", notNull = true)
    BandConfiguration bandConfiguration;

    @Parameter(description = "A text specifying the target Coordinate Reference System, either in WKT or as an " +
                             "authority code. For appropriate EPSG authority codes see (www.epsg-registry.org). " +
                             "AUTO authority can be used with code 42001 (UTM), and 42002 (Transverse Mercator) " +
                             "where the scene center is used as reference. Examples: EPSG:4326, AUTO:42001",
               defaultValue = "EPSG:4326")
    String crs;

    @Parameter(alias = "resampling",
               label = "Resampling Method",
               description = "The method used for resampling of floating-point raster data, if source products must" +
                             "be reprojected to the target CRS.",
               valueSet = {"Nearest", "Bilinear", "Bicubic"},
               defaultValue = "Nearest")
    private String resamplingName;

    @Parameter(description = "The western longitude.", interval = "[-180,180]", defaultValue = "-15.0")
    double westBound;
    @Parameter(description = "The northern latitude.", interval = "[-90,90]", defaultValue = "75.0")
    double northBound;
    @Parameter(description = "The eastern longitude.", interval = "[-180,180]", defaultValue = "30.0")
    double eastBound;
    @Parameter(description = "The southern latitude.", interval = "[-90,90]", defaultValue = "35.0")
    double southBound;

    @Parameter(description = "Size of a pixel in X-direction in map units.", defaultValue = "0.05")
    double pixelSizeX;
    @Parameter(description = "Size of a pixel in Y-direction in map units.", defaultValue = "0.05")
    double pixelSizeY;

    private TreeMap<Long, List<Product>> dailyGroupedProducts;
    private long interpolationStartMJD;
    private long interpolationEndMJD;
    private int interpolationLength;
    private int targetWidth;
    private int targetHeight;
    private Product timeSeriesDataProduct;
    private int year;
    private HashMap<String, Integer> dpBandNameIndexMapping;

    @Override
    public void initialize() throws OperatorException {
        validateInput();

        final Product targetProduct = createTargetProduct();
        final Area targetArea = Utils.createProductArea(targetProduct);
        setTargetProduct(targetProduct);

        final ProductValidator productValidator = new ProductValidator(Arrays.asList(bandConfiguration), startDate, endDate, targetArea, getLogger());
        final ProductLoader productLoader = new ProductLoader(sourceProductPaths, productValidator, getLogger());
        final Product[] products = productLoader.loadProducts();
        gc();

        dailyGroupedProducts = groupProductsDaily(products);

        if (dailyGroupedProducts.size() < 2) {
            throw new OperatorException("For interpolated daily percentile calculation" +
                                        "at least two days must contain valid input products.");
        }

        initInterpolationStartAndEnd();
        initYearAndDoyOffset();
        dpBandNameIndexMapping = new HashMap<String, Integer>();
        initTimeSeriesDataProduct();

        targetWidth = targetProduct.getSceneRasterWidth();
        targetHeight = targetProduct.getSceneRasterHeight();

        addInputMetadataToTargetProduct();
        getLogger().log(Level.INFO, "Successfully initialized target product.");

        for (long mjd : dailyGroupedProducts.keySet()) {
            final List<Product> dailyProducts = dailyGroupedProducts.get(mjd);
            final List<Product> colocatedProducts = createColocatedProducts(dailyProducts);
            final float[] doyData = computeDailyMean(colocatedProducts);
            final String sourceBandName = bandConfiguration.sourceBandName;
            final String meanBandName = createNameForMeanBand(sourceBandName, mjd);
            final Band band = timeSeriesDataProduct.getBand(meanBandName);
            band.setData(new ProductData.Float(doyData));
            band.setModified(true);
            try {
                timeSeriesDataProduct.getProductWriter().writeBandRasterData(band, 0, 0, targetWidth, targetHeight, band.getData(), ProgressMonitor.NULL);
            } catch (IOException e) {
                throw new OperatorException(UNABLE_TO_WRITE_TIMESERIES_DATA_PRODUCT, e);
            }
            band.getData().dispose();
            band.setData(null);
            gc();
        }
        try {
            final File timeSeriesDataProductLocation = new File(timeSeriesDataProduct.getName() + DimapProductConstants.DIMAP_HEADER_FILE_EXTENSION);
            timeSeriesDataProduct.getProductWriter().close();
            timeSeriesDataProduct.dispose();
            timeSeriesDataProduct = null;
            timeSeriesDataProduct = ProductIO.readProduct(timeSeriesDataProductLocation);
        } catch (IOException e) {
            throw new OperatorException(UNABLE_TO_WRITE_TIMESERIES_DATA_PRODUCT, e);
        }
        dailyGroupedProducts.clear();

        getLogger().log(Level.INFO, "Input products colocated with target product.");

    }

    @Override
    public void computeTileStack(Map<Band, Tile> targetTiles, Rectangle targetRectangle, ProgressMonitor pm) throws OperatorException {
        final float[] interpolationFloats = new float[interpolationLength];
        final Rectangle r = targetRectangle;

        final float[][] sourceTiles = new float[interpolationLength][0];
        for (String bandName : dpBandNameIndexMapping.keySet()) {
            final float[] sourceTile;
            try {
                sourceTile = new float[r.width * r.height];
                timeSeriesDataProduct.getBand(bandName).readPixels(r.x, r.y, r.width, r.height, sourceTile);
            } catch (IOException e) {
                throw new OperatorException("Unable to load source tiles.", e);
            }
            final int index = dpBandNameIndexMapping.get(bandName);
            sourceTiles[index] = sourceTile;
        }

        for (int targetY = r.y, sourceY = 0; targetY < (r.y + r.height); targetY++, sourceY++) {
            for (int targetX = r.x, sourceX = 0; targetX < (r.x + r.width); targetX++, sourceX++) {
                clear(interpolationFloats);
                int idx = sourceY * r.width + sourceX;
                fillWithAvailableValues(idx, interpolationFloats, sourceTiles);
                GapFiller.fillGaps(interpolationFloats, bandConfiguration);
                Arrays.sort(interpolationFloats);
                for (Band band : targetTiles.keySet()) {
                    int percentile = extractPercentileFromBandName(band.getName());
                    Tile targetTile = targetTiles.get(band);
                    final float p = PercentileComputer.compute(percentile, interpolationFloats);
                    targetTile.setSample(targetX, targetY, p);
                }
            }
        }
        gc();
    }

    private void dispose(List<Product> products) {
        for (Product colocatedProduct : products) {
            colocatedProduct.dispose();
        }
        products.clear();
    }

    private TreeMap<Long, List<Product>> groupProductsDaily(Product[] products) {
        final TreeMap<Long, List<Product>> groupedProducts = new TreeMap<Long, List<Product>>();
        for (Product product : products) {
            final long centerDay = getCenterDateAsModifiedJulianDay(product);
            List<Product> productList = groupedProducts.get(centerDay);
            if (productList == null) {
                productList = new ArrayList<Product>();
                groupedProducts.put(centerDay, productList);
            }
            productList.add(product);
        }
        return groupedProducts;
    }

    private void initTimeSeriesDataProduct() {
        timeSeriesDataProduct = createOutputProduct();
        final String sourceBandName = bandConfiguration.sourceBandName;
        timeSeriesDataProduct.setName(year + "_" + sourceBandName + SUFFIX_PERCENTILE_OP_DATA_PRODUCT);
        addExpectedMetadataForTimeSeriesTool(sourceBandName);
        timeSeriesDataProduct.setAutoGrouping(sourceBandName);
        for (long mjd : dailyGroupedProducts.keySet()) {
            final String bandName = createNameForMeanBand(sourceBandName, mjd);
            final int dayIdx = (int) (mjd - interpolationStartMJD);
            dpBandNameIndexMapping.put(bandName, dayIdx);
            final Band band = timeSeriesDataProduct.addBand(bandName, ProductData.TYPE_FLOAT32);
            final List<Product> products = dailyGroupedProducts.get(mjd);
            final Product product = products.get(0);
            final Band sourceBand = product.getBand(sourceBandName);
            band.setUnit(sourceBand.getUnit());
            band.setDescription(sourceBand.getDescription());
        }
        final ProductWriter productWriter = ProductIO.getProductWriter(DimapProductConstants.DIMAP_FORMAT_NAME);
        try {
            productWriter.writeProductNodes(timeSeriesDataProduct, new File(timeSeriesDataProduct.getName()));
        } catch (IOException e) {
            throw new OperatorException(UNABLE_TO_WRITE_TIMESERIES_DATA_PRODUCT, e);
        }
    }

    private void addExpectedMetadataForTimeSeriesTool(String sourceBandName) {
        timeSeriesDataProduct.setProductType(TIME_SERIES_PRODUCT_TYPE);
        final MetadataElement tsMetadataRoot = new MetadataElement(TIME_SERIES_METADATA_ROOT_NAME);
        tsMetadataRoot.addElement(new MetadataElement(PRODUCT_LOCATIONS));
        final MetadataElement eoVariablesElement = new MetadataElement(TIME_SERIES_METADATA_VARIABLES_NAME);
        MetadataElement elem = new MetadataElement(TIME_SERIES_METADATA_VARIABLES_NAME + "." + 0);
        elem.addAttribute(new MetadataAttribute(TIME_SERIES_METADATA_VARIABLE_ATTRIBUTE_NAME,
                                                ProductData.createInstance(sourceBandName), true));
        final ProductData isSelected = ProductData.createInstance(Boolean.toString(true));
        elem.addAttribute(new MetadataAttribute(VARIABLE_SELECTION, isSelected, true));
        eoVariablesElement.addElement(elem);
        tsMetadataRoot.addElement(eoVariablesElement);
        timeSeriesDataProduct.getMetadataRoot().addElement(tsMetadataRoot);
    }

    private String createNameForMeanBand(String sourceBandName, long mjd) {
        final double jd = DateTimeUtils.mjdToJD(mjd);
        final Date utc = DateTimeUtils.jdToUTC(jd);
        final SimpleDateFormat dateFormat = new SimpleDateFormat(BAND_DATE_FORMAT, Locale.ENGLISH);
        final String timeString = dateFormat.format(utc);
        return sourceBandName + "_" + timeString;
    }

    private void initYearAndDoyOffset() {
        final double startJD = DateTimeUtils.mjdToJD(interpolationStartMJD);
        final Date startUTC = DateTimeUtils.jdToUTC(startJD);
        final Calendar calendar = Calendar.getInstance();
        calendar.setTime(startUTC);
        year = calendar.get(Calendar.YEAR);
        calendar.set(year, 0, 0, 0, 0, 0);
    }

    @Override
    public void dispose() {
        super.dispose();

        timeSeriesDataProduct.dispose();

        dailyGroupedProducts.clear();
        dailyGroupedProducts = null;
    }

    private void clear(float[] interpolationFloats) {
        Arrays.fill(interpolationFloats, Float.NaN);
    }

    private void fillWithAvailableValues(int idx, float[] interpolationFloats, float[][] sourceTiles) {
        for (int i = 0; i < interpolationFloats.length; i++) {
            float[] floats = sourceTiles[i];
            if (floats.length == 0) {
                continue;
            }
            interpolationFloats[i] = floats[idx];
        }
    }

    private float[] computeDailyMean(List<Product> colocatedProducts) {

        final String sourceBandName = bandConfiguration.sourceBandName;

        final Band[] bands = new Band[colocatedProducts.size()];
        for (int i = 0; i < bands.length; i++) {
            bands[i] = colocatedProducts.get(i).getBand(sourceBandName);
        }

        float[] meanData = new float[targetHeight * targetWidth];
        for (int y = 0; y < targetHeight; y++) {
            for (int x = 0; x < targetWidth; x++) {
                meanData[y * targetWidth + x] = computeMean(x, y, bands);
            }
        }

        dispose(colocatedProducts);

        return meanData;
    }

    private float computeMean(int x, int y, Band[] bands) {
        float mean = Float.NaN;
        int count = 0;

        for (Band band : bands) {
            final float currentValue = band.getPixelFloat(x, y);
            if (!Float.isNaN(currentValue)) {
                if (count == 0) {
                    mean = currentValue;
                } else {
                    mean += currentValue;
                }
                count++;
            }
        }
        if (count == 0) {
            return Float.NaN;
        } else {
            return mean / count;
        }
    }

    private void initInterpolationStartAndEnd() {
        final long oldestMJD = dailyGroupedProducts.firstKey();
        final long youngestMJD = dailyGroupedProducts.lastKey();
        if (startDate != null) {
            interpolationStartMJD = utcToModifiedJulianDay(startDate.getAsDate());
        } else {
            interpolationStartMJD = oldestMJD;
        }
        if (endDate != null) {
            interpolationEndMJD = utcToModifiedJulianDay(endDate.getAsDate());
        } else {
            interpolationEndMJD = youngestMJD;
        }
        interpolationLength = (int) (interpolationEndMJD - interpolationStartMJD + 1);
    }

    private List<Product> createColocatedProducts(List<Product> dailyProducts) {
        final ArrayList<Product> colocatedProducts = new ArrayList<Product>();
        final HashMap<String, Object> projParameters = createProjectionParameters();
        for (Product dailyProduct : dailyProducts) {
            HashMap<String, Product> projProducts = new HashMap<String, Product>();
            projProducts.put("source", dailyProduct);
            projProducts.put("collocateWith", getTargetProduct());
            final Product colocatedProduct = GPF.createProduct("Reproject", projParameters, projProducts);
            try {
                Band band = colocatedProduct.getBand(bandConfiguration.sourceBandName);
                String validPixelExpression = bandConfiguration.validPixelExpression;
                if (StringUtils.isNotNullAndNotEmpty(validPixelExpression)) {
                    band.setValidPixelExpression(validPixelExpression);
                }
                band.readRasterDataFully();
            } catch (IOException e) {
                throw new OperatorException(e);
            }
            colocatedProducts.add(colocatedProduct);
        }
        dispose(dailyProducts);
        return colocatedProducts;
    }

    private HashMap<String, Object> createProjectionParameters() {
        HashMap<String, Object> projParameters = new HashMap<String, Object>();
        // @todo clean up
        projParameters.put("resamplingName", resamplingName);
        projParameters.put("includeTiePointGrids", false);  // ensure tie-points are reprojected
//        projParameters.put("includeTiePointGrids", true);  // ensure tie-points are reprojected
//        if (orthorectify) {
//            projParameters.put("orthorectify", orthorectify);
//            projParameters.put("elevationModelName", elevationModelName);
//        }
        return projParameters;
    }

    private void addInputMetadataToTargetProduct() {
        addInputProductPathsToMetadata();
        addBandConfigurationToMetadata();
    }


    private void addBandConfigurationToMetadata() {
        final MetadataElement bandConfigurationElem = new MetadataElement("BandConfiguration");

        final ProductData sourceBandData = ProductData.createInstance(bandConfiguration.sourceBandName);
        bandConfigurationElem.addAttribute(new MetadataAttribute("sourceBandName", sourceBandData, true));

        final ProductData interpolationData = ProductData.createInstance(bandConfiguration.interpolationMethod);
        bandConfigurationElem.addAttribute(new MetadataAttribute("interpolation", interpolationData, true));

        String expr = bandConfiguration.validPixelExpression;
        final ProductData validPixelExpressionData = ProductData.createInstance(expr == null ? "" : expr);
        bandConfigurationElem.addAttribute(new MetadataAttribute("validPixelExpression", validPixelExpressionData, true));

        final ProductData percentilesData = ProductData.createInstance(bandConfiguration.percentiles);
        bandConfigurationElem.addAttribute(new MetadataAttribute("percentiles", percentilesData, true));

        final ProductData endValueData = ProductData.createInstance(new double[]{bandConfiguration.endValueFallback});
        bandConfigurationElem.addAttribute(new MetadataAttribute("endValueFallback", endValueData, true));

        final ProductData startValueData = ProductData.createInstance(new double[]{bandConfiguration.startValueFallback});
        bandConfigurationElem.addAttribute(new MetadataAttribute("startValueFallback", startValueData, true));

        getTargetProduct().getMetadataRoot().addElement(bandConfigurationElem);
    }

    private void addInputProductPathsToMetadata() {
        final MetadataElement productsElem = new MetadataElement("Input products");
        final String[] absInputProductPaths = getAbsInputProductPaths();
        for (int i = 0; i < absInputProductPaths.length; i++) {
            String inputProductAbsPath = absInputProductPaths[i];
            final ProductData pathData = ProductData.createInstance(inputProductAbsPath);
            final MetadataAttribute pathAttribute = new MetadataAttribute("product_" + i, pathData, true);
            productsElem.addAttribute(pathAttribute);
        }
        getTargetProduct().getMetadataRoot().addElement(productsElem);
    }

    private String[] getAbsInputProductPaths() {
        final ArrayList<String> absolutPaths = new ArrayList<String>();
        for (List<Product> products : dailyGroupedProducts.values()) {
            for (Product product : products) {
                absolutPaths.add(product.getFileLocation().getAbsolutePath());
            }
        }
        return absolutPaths.toArray(new String[absolutPaths.size()]);
    }

    private Product createTargetProduct() {
        final Product product = createOutputProduct();
        addTargetBandsAndCreateBandMapping(product);
        return product;
    }


    private Product createOutputProduct() {
        try {
            CoordinateReferenceSystem targetCRS;
            try {
                targetCRS = CRS.parseWKT(crs);
            } catch (FactoryException e) {
                targetCRS = CRS.decode(crs, true);
            }
            final Rectangle2D bounds = new Rectangle2D.Double();
            bounds.setFrameFromDiagonal(westBound, northBound, eastBound, southBound);
            final ReferencedEnvelope boundsEnvelope = new ReferencedEnvelope(bounds, DefaultGeographicCRS.WGS84);
            final ReferencedEnvelope targetEnvelope = boundsEnvelope.transform(targetCRS, true);
            final int width = MathUtils.floorInt(targetEnvelope.getSpan(0) / pixelSizeX);
            final int height = MathUtils.floorInt(targetEnvelope.getSpan(1) / pixelSizeY);
            final CrsGeoCoding geoCoding = new CrsGeoCoding(targetCRS,
                                                            width,
                                                            height,
                                                            targetEnvelope.getMinimum(0),
                                                            targetEnvelope.getMaximum(1),
                                                            pixelSizeX, pixelSizeY);

            final Product product = new Product("Percentile", "InterpolatedPercentile", width, height);
            product.setGeoCoding(geoCoding);
            final Dimension tileSize = JAIUtils.computePreferredTileSize(width, height, 1);
            product.setPreferredTileSize(tileSize);
            return product;
        } catch (Exception e) {
            throw new OperatorException(e);
        }
    }

    private void addTargetBandsAndCreateBandMapping(Product product) {
        final String prefix = bandConfiguration.sourceBandName;
        for (Integer percentile : bandConfiguration.percentiles) {
            final String name = getPercentileBandName(prefix, percentile);
            final Band band = product.addBand(name, ProductData.TYPE_FLOAT32);
            band.setSpectralWavelength(percentile);
            band.setSpectralBandwidth(2);
        }
    }

    private static long getCenterDateAsModifiedJulianDay(Product product) {
        final ProductData.UTC startTime = product.getStartTime();
        final ProductData.UTC endTime = product.getEndTime();
        final long endMillies = endTime.getAsDate().getTime();
        final long startMillies = startTime.getAsDate().getTime();
        final long centerMillies = (endMillies - startMillies) / 2 + startMillies;
        final Date centerUTCDate = new Date(centerMillies);
        return utcToModifiedJulianDay(centerUTCDate);
    }

    private static long utcToModifiedJulianDay(Date centerUTCDate) {
        final double julianDate = DateTimeUtils.utcToJD(centerUTCDate);
        final double modifiedJulianDate = DateTimeUtils.jdToMJD(julianDate);
        final double modifiedJulianDay = Math.floor(modifiedJulianDate);
        return (long) modifiedJulianDay;
    }

    private String getPercentileBandName(String prefix, int percentile) {
        return prefix + "_p" + percentile + "_threshold";
    }

    private int extractPercentileFromBandName(String name) {
        final String percentileStart = name.substring(name.lastIndexOf("_p") + 2);
        final String percentileString = percentileStart.substring(0, percentileStart.indexOf("_"));
        return Integer.parseInt(percentileString);
    }

    private void validateInput() {
        if (sourceProducts != null && sourceProducts.length > 0) {
            throw new OperatorException("Use this operator only with source product paths defined in the graph.xml file.");
        }
        if (startDate != null && endDate != null && endDate.getAsDate().before(startDate.getAsDate())) {
            throw new OperatorException("End date '" + this.endDate + "' before start date '" + this.startDate + "'");
        }
        if (sourceProductPaths == null || sourceProductPaths.length == 0) {
            throw new OperatorException("The parameter 'sourceProductPaths' must be specified");
        }
        if (bandConfiguration == null) {
            throw new OperatorException("Parameter 'bandConfiguration' must be specified.");
        }
        if (bandConfiguration.sourceBandName == null) {
            throw new OperatorException("Configuration must contain a source band.");
        }
    }


    private void gc() {
        System.gc();
    }

    public static class UtcConverter implements Converter<ProductData.UTC> {

        @Override
        public ProductData.UTC parse(String text) throws ConversionException {
            try {
                return ProductData.UTC.parse(text, DATETIME_PATTERN);
            } catch (ParseException e) {
                throw new ConversionException(e);
            }
        }

        @Override
        public String format(ProductData.UTC value) {
            if (value != null) {
                return value.format();
            }
            return "";
        }

        @Override
        public Class<ProductData.UTC> getValueType() {
            return ProductData.UTC.class;
        }

    }

    /**
     * The service provider interface (SPI) which is referenced
     * in {@code /META-INF/services/org.esa.beam.framework.gpf.OperatorSpi}.
     */
    public static class Spi extends OperatorSpi {

        public Spi() {
            super(InterpolatedPercentileOp.class);
        }
    }

}
