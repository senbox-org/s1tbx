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
import org.esa.beam.util.Debug;
import org.esa.beam.util.jai.JAIUtils;
import org.esa.beam.util.math.MathUtils;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import javax.media.jai.JAI;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
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

    public static final String DATETIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

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

    @Parameter(description = "The band configurations. These configurations determine the input of the operator.",
               alias = "bandConfigurations", itemAlias = "bandConfiguration", notNull = true)
    BandConfiguration[] bandConfigurations;

    @Parameter(description = "A text specifying the target Coordinate Reference System, either in WKT or as an " +
                             "authority code. For appropriate EPSG authority codes see (www.epsg-registry.org). " +
                             "AUTO authority can be used with code 42001 (UTM), and 42002 (Transverse Mercator) " +
                             "where the scene center is used as reference. Examples: EPSG:4326, AUTO:42001",
               defaultValue = "EPSG:4326")
    String crs;


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

    private HashMap<Band, BandConfiguration> bandMapping;
    private TreeMap<Long, List<Product>> dailyGroupedProducts;
    private TreeMap<Long, List<Product>> colocatedDailyGroupedProducts;
    private long interpolationStartMJD;
    private long interpolationEndMJD;
    private int interpolationLength;
    private Area targetArea;
//    private final Dimension preferredTileSize = new Dimension(25, 25);

    @Override
    public void initialize() throws OperatorException {
//        JAI.setDefaultTileSize(preferredTileSize);
//        JAIUtils.setDefaultTileCacheCapacity(1000);
        validateInput();

        final Product targetProduct = createTargetProduct();
        targetArea = Utils.createProductArea(targetProduct);
        setTargetProduct(targetProduct);

        final ProductValidator productValidator = new ProductValidator(Arrays.asList(bandConfigurations), startDate, endDate, targetArea, getLogger());
        final ProductLoader productLoader = new ProductLoader(sourceProductPaths, productValidator, getLogger());
        final Product[] products = productLoader.loadProducts();
//        for (Product product : products) {
//            product.setPreferredTileSize(preferredTileSize);
//        }

        dailyGroupedProducts = groupProductsDaily(products);

        if (dailyGroupedProducts.size() < 2) {
            throw new OperatorException("For interpolated daily percentile calculation" +
                                        "at least two days must contain valid input products.");
        }

        initInterpolationStartAndEnd();
        addInputMetadataToTargetProduct();
        getLogger().log(Level.INFO, "Successfully initialized target product.");

        colocatedDailyGroupedProducts = new TreeMap<Long, List<Product>>();
        for (Long key : dailyGroupedProducts.keySet()) {
            final List<Product> dailyProducts = dailyGroupedProducts.get(key);
            final List<Product> colocatedProducts = createColocatedProducts(dailyProducts);
            colocatedDailyGroupedProducts.put(key, colocatedProducts);
        }

        getLogger().log(Level.INFO, "Input products colocated with target product.");
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

    @Override
    public void computeTileStack(Map<Band, Tile> targetTiles, Rectangle targetRectangle, ProgressMonitor pm) throws OperatorException {
        System.out.println("targetRectangle = " + targetRectangle);
        HashMap<String, Map<Integer, Tile>> groupedTiles = new HashMap<String, Map<Integer, Tile>>();
        for (Band targetBand : targetTiles.keySet()) {
            Tile targetTile = targetTiles.get(targetBand);
            int percentile = extractPercentileFromBandName(targetBand.getName());
            final BandConfiguration bandConfiguration = bandMapping.get(targetBand);
            final String sourceBandName = bandConfiguration.sourceBandName;
            Map<Integer, Tile> tiles = groupedTiles.get(sourceBandName);
            if (tiles == null) {
                tiles = new HashMap<Integer, Tile>();
                groupedTiles.put(sourceBandName, tiles);
            }
            tiles.put(percentile, targetTile);
        }

        for (String sourceBandName : groupedTiles.keySet()) {
            final float[] interpolationFloats = new float[interpolationLength];
            final Rectangle r = targetRectangle;
            Map<Integer, Tile> tileMap = groupedTiles.get(sourceBandName);
            for (BandConfiguration bandConfiguration : bandConfigurations) {
                if (!bandConfiguration.sourceBandName.equals(sourceBandName)) {
                    continue;
                }
//                Map<Long, List<float[]>> sourceFloats;
//                try {
//                    sourceFloats = createSourceFloats(r, sourceBandName);
//                } catch (IOException e) {
//                    throw new OperatorException(e.getMessage(), e);
//                }
                for (int y = r.y; y < (r.y + r.height); y++) {
//                for (int y = r.y, y0 = 0; y < (r.y + r.height); y++, y0++) {
                    for (int x = r.x; x < (r.x + r.width); x++) {
//                    for (int x = r.x, x0 = 0; x < (r.x + r.width); x++, x0++) {
                        clear(interpolationFloats);
//                        int idx = y0 * r.width + x0;
                        fillWithAvailableValues(x,y,sourceBandName, interpolationFloats);
                        GapFiller.fillGaps(interpolationFloats, bandConfiguration);
                        Arrays.sort(interpolationFloats);
                        for (Integer percentile : tileMap.keySet()) {
                            Tile targetTile = tileMap.get(percentile);
                            final float p = PercentileComputer.compute(percentile, interpolationFloats);
                            targetTile.setSample(x, y, p);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void dispose() {
        super.dispose();

        bandMapping.clear();
        bandMapping = null;

        disposeProducts(colocatedDailyGroupedProducts);
        colocatedDailyGroupedProducts.clear();
        colocatedDailyGroupedProducts = null;

        disposeProducts(dailyGroupedProducts);
        dailyGroupedProducts.clear();
        dailyGroupedProducts = null;
    }

    private void disposeProducts(TreeMap<Long, List<Product>> productsMap) {
        for (Long key : productsMap.keySet()) {
            final List<Product> products = productsMap.get(key);
            for (Product product : products) {
                product.dispose();
            }
        }
    }

    private void clear(float[] interpolationFloats) {
        Arrays.fill(interpolationFloats, Float.NaN);
    }

//    private TreeMap<Long, List<float[]>> createSourceFloats(Rectangle r, String sourceBandName) throws IOException {
//        TreeMap<Long, List<float[]>> sourceTiles = new TreeMap<Long, List<float[]>>();
//        for (long keyMJD : colocatedDailyGroupedProducts.keySet()) {
//            final List<Product> products = colocatedDailyGroupedProducts.get(keyMJD);
//            ArrayList<float[]> sourceFloats = new ArrayList<float[]>();
//            sourceTiles.put(keyMJD, sourceFloats);
//            for (Product product : products) {
//                Band band = product.getBand(sourceBandName);
//                float[] floats = band.readPixels(r.x, r.y, r.width, r.height, new float[r.width * r.height]);
//                sourceFloats.add(floats);
//            }
//        }
//        return sourceTiles;
//    }

    private void fillWithAvailableValues(int x, int y, String sourceBandName, float[] interpolationFloats) {
        for (long keyMJD : colocatedDailyGroupedProducts.keySet()) {
            final List<Product> products = colocatedDailyGroupedProducts.get(keyMJD);
            final float mean = computeMean(x,y, products, sourceBandName);
            interpolationFloats[((int) (keyMJD - interpolationStartMJD))] = mean;
        }
    }

//    private void fillWithAvailableValues(int idx, Map<Long, List<float[]>> sourceFloats, float[] interpolationFloats) {
//        for (long keyMJD : sourceFloats.keySet()) {
//            final List<float[]> floats = sourceFloats.get(keyMJD);
//            final float mean = computeMean(idx, floats);
//            interpolationFloats[((int) (keyMJD - interpolationStartMJD))] = mean;
//        }
//    }

    private float computeMean(int x, int y, List<Product> products, String sourceBandName) {
        float mean = Float.NaN;
        int count = 0;
        for (Product product : products) {
            final float currentValue = product.getBand(sourceBandName).getPixelFloat(x, y);
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

//    private float computeMean(int idx, List<float[]> floats) {
//        float mean = Float.NaN;
//        int count = 0;
//        for (float[] fa : floats) {
//            final float currentValue = fa[idx];
//            if (!Float.isNaN(currentValue)) {
//                if (count == 0) {
//                    mean = currentValue;
//                } else {
//                    mean += currentValue;
//                }
//                count++;
//            }
//        }
//        if (count == 0) {
//            return Float.NaN;
//        } else {
//            return mean / count;
//        }
//    }

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
            for (BandConfiguration bandConfiguration : bandConfigurations) {
                try {
                    colocatedProduct.getBand(bandConfiguration.sourceBandName).readRasterDataFully();
                } catch (IOException e) {
                    throw new OperatorException(e);
                }
            }
            dailyProduct.dispose();
//            colocatedProduct.setPreferredTileSize(preferredTileSize);
            colocatedProducts.add(colocatedProduct);
        }
        dailyProducts.clear();
        return colocatedProducts;
    }

    private HashMap<String, Object> createProjectionParameters() {
        HashMap<String, Object> projParameters = new HashMap<String, Object>();
        // @todo clean up
//        projParameters.put("resamplingName", resamplingName);
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
        addBandConfigurationsToMetadata();
    }

    private void addBandConfigurationsToMetadata() {
        final MetadataElement bandConfigurationsElem = new MetadataElement("BandConfigurations");

        for (int i = 0, bandConfigurationsLength = bandConfigurations.length; i < bandConfigurationsLength; i++) {
            final BandConfiguration bandConfig = bandConfigurations[i];

            final MetadataElement configElem = new MetadataElement("Band configuration " + i);
            bandConfigurationsElem.addElement(configElem);

            final ProductData sourceBandData = ProductData.createInstance(bandConfig.sourceBandName);
            configElem.addAttribute(new MetadataAttribute("sourceBandName", sourceBandData, true));

            final ProductData interpolationData = ProductData.createInstance(bandConfig.interpolationMethod);
            configElem.addAttribute(new MetadataAttribute("interpolation", interpolationData, true));

            String expr = bandConfig.validPixelExpression;
            final ProductData validPixelExpressionData = ProductData.createInstance(expr == null ? "" : expr);
            configElem.addAttribute(new MetadataAttribute("validPixelExpression", validPixelExpressionData, true));

            final ProductData percentilesData = ProductData.createInstance(bandConfig.percentiles);
            configElem.addAttribute(new MetadataAttribute("percentiles", percentilesData, true));

            final ProductData endValueData = ProductData.createInstance(new double[]{bandConfig.endValueFallback});
            configElem.addAttribute(new MetadataAttribute("endValueFallback", endValueData, true));

            final ProductData startValueData = ProductData.createInstance(new double[]{bandConfig.startValueFallback});
            configElem.addAttribute(new MetadataAttribute("startValueFallback", startValueData, true));
        }
        getTargetProduct().getMetadataRoot().addElement(bandConfigurationsElem);
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
//            final Dimension tileSize = preferredTileSize;
            final Dimension tileSize = JAIUtils.computePreferredTileSize(width, height, 1);
            product.setPreferredTileSize(tileSize);
            addTargetBandsAndCreateBandMapping(product);

            return product;
        } catch (Exception e) {
            throw new OperatorException(e);
        }
    }

    private void addTargetBandsAndCreateBandMapping(Product product) {
        bandMapping = new HashMap<Band, BandConfiguration>();
        for (BandConfiguration bandConfiguration : bandConfigurations) {
            final String prefix = bandConfiguration.sourceBandName;
            for (Integer percentile : bandConfiguration.percentiles) {
                final String name = getPercentileBandName(prefix, percentile);
                final Band targetBand = product.addBand(name, ProductData.TYPE_FLOAT32);
                bandMapping.put(targetBand, bandConfiguration);
            }
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


    void validateInput() {
        if (sourceProducts != null && sourceProducts.length > 0) {
            throw new OperatorException("Use this operator only with source product paths defined in the graph.xml file.");
        }
        if (startDate != null && endDate != null && endDate.getAsDate().before(startDate.getAsDate())) {
            throw new OperatorException("End date '" + this.endDate + "' before start date '" + this.startDate + "'");
        }
        if (sourceProductPaths == null || sourceProductPaths.length == 0) {
            throw new OperatorException("The parameter 'sourceProductPaths' must be specified");
        }
        if (bandConfigurations == null) {
            throw new OperatorException("Parameter 'bandConfigurations' must be specified.");
        }
        for (BandConfiguration configuration : bandConfigurations) {
            if (configuration.sourceBandName == null) {
                throw new OperatorException("Configuration must contain a source band.");
            }
        }
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
