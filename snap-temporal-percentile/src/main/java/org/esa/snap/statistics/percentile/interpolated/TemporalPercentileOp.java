/*
 * Copyright (C) 2013 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.statistics.percentile.interpolated;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.Converter;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glevel.MultiLevelImage;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.dataio.ProductWriter;
import org.esa.snap.core.dataio.dimap.DimapProductConstants;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.CrsGeoCoding;
import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProducts;
import org.esa.snap.core.image.ImageManager;
import org.esa.snap.core.util.DateTimeUtils;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.core.util.io.FileUtils;
import org.esa.snap.core.util.jai.JAIUtils;
import org.esa.snap.core.util.math.MathUtils;
import org.esa.snap.interpolators.Interpolator;
import org.esa.snap.interpolators.InterpolatorFactory;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
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
import java.util.Vector;
import java.util.logging.Level;

/**
 * An operator that is used to compute percentiles over a given time period. Products with different observation times
 * serve as computation base. All the input products are sorted chronologically and grouped per day. For each day
 * inside the given time period, a collocated mean band from the grouped products is computed. By this means, a intermediate
 * time series product is created successively.
 * <p>
 * This time series product is used to create time series' per pixel. Days with missing values or with values that have been
 * masked out using the valid pixel expression will cause gaps in a time series. To improve the percentile calculation
 * results, such gaps can be filled.
 * Three gap filling strategies are available.
 * <ul>
 * <li>gap filling by linear interpolation</li>
 * <li>gap filling by spline interpolation</li>
 * <li>gap filling by quadratic interpolation</li>
 * </ul>
 * <p>
 * Based on these time series', for each percentile a band is written to the target product.
 * In these bands, each pixel holds the threshold of the respective percentile.
 * <p>
 *
 * @author Sabine Embacher
 * @author Tonio Fincke
 * @author Thomas Storm
 */
@OperatorMetadata(alias = "TemporalPercentile",
                  category = "Raster",
                  version = "1.0",
                  authors = "Sabine Embacher, Marco Peters, Tonio Fincke",
                  copyright = "(c) 2013 by Brockmann Consult GmbH",
                  description = "Computes percentiles over a given time period.")
public class TemporalPercentileOp extends Operator {

    // todo start ... these constant fields are copied from time series tool
    // see org.esa.snap.timeseries.core.timeseries.datamodel.AbstractTimeSeries
    public static final String BAND_DATE_FORMAT = "yyyyMMdd.HHmmss.SSS";
    public static final String TIME_SERIES_PRODUCT_TYPE = "org.esa.snap.glob.timeseries";
    public static final String TIME_SERIES_METADATA_ROOT_NAME = "TIME_SERIES";
    public static final String PRODUCT_LOCATIONS = "PRODUCT_LOCATIONS";
    public static final String TIME_SERIES_METADATA_VARIABLES_NAME = "VARIABLES";
    public static final String TIME_SERIES_METADATA_VARIABLE_ATTRIBUTE_NAME = "NAME";
    public static final String VARIABLE_SELECTION = "SELECTION";
    // todo end

    public final static String GAP_FILLING_METHOD_NO_GAP_FILLING = "noGapFilling";
    public final static String GAP_FILLING_METHOD_LINEAR_INTERPOLATION = "gapFillingLinearInterpolation";
    public final static String GAP_FILLING_METHOD_SPLINE_INTERPOLATION = "gapFillingSplineInterpolation";
    public final static String GAP_FILLING_METHOD_QUADRATIC_INTERPOLATION = "gapFillingQuadraticInterpolation";

    public static final String DATETIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
    private static final String SUFFIX_PERCENTILE_OP_DATA_PRODUCT = "_PercentileOpDataProduct";
    private static final String UNABLE_TO_WRITE_TIMESERIES_DATA_PRODUCT = "Unable to write timeseries data product.";
    private static final String UNABLE_TO_READ_TIMESERIES_DATA_PRODUCT = "Unable to read timeseries data product.";
    private final static String BAND_MATH_EXPRESSION_BAND_NAME = "bandMathExpressionBandName";
    private final static String COUNT_BAND_NAME = "values_count";


    @SourceProducts(description = "Using this parameter is discouraged. For performance reasons use sourceProductPaths instead")
    Product[] sourceProducts;

    @Parameter(description = "A comma-separated list of file paths specifying the source products.\n" +
                             "Source products to be considered for percentile computation. \n" +
                             "Each path may contain the wildcards '**' (matches recursively any directory),\n" +
                             "'*' (matches any character sequence in path names) and\n" +
                             "'?' (matches any single character).\n" +
                             "If, for example, all NetCDF files under /eodata/ shall be considered, use '/eodata/**/*.nc'.")
    String[] sourceProductPaths;

    @Parameter(description = "The start date. If not given, it is taken from the 'oldest' source product. Products that\n" +
                             "have a start date earlier than the start date given by this parameter are not considered.",
               format = DATETIME_PATTERN, converter = UtcConverter.class)
    ProductData.UTC startDate;

    @Parameter(description = "The end date. If not given, it is taken from the 'newest' source product. Products that\n" +
                             "have an end date later than the end date given by this parameter are not considered.",
               format = DATETIME_PATTERN, converter = UtcConverter.class)
    ProductData.UTC endDate;

    @Parameter(description = "Determines whether the time series product which is created during computation\n" +
                             "should be written to disk.",
               defaultValue = "true")
    boolean keepIntermediateTimeSeriesProduct;

    @Parameter(description = "The output directory for the intermediate time series product. If not given, the time\n" +
                             "series product will be written to the working directory.")
    File timeSeriesOutputDir;

    @Parameter(description = "A text specifying the target Coordinate Reference System, either in WKT or as an\n" +
                             "authority code. For appropriate EPSG authority codes see (www.epsg-registry.org).\n" +
                             "AUTO authority can be used with code 42001 (UTM), and 42002 (Transverse Mercator)\n" +
                             "where the scene center is used as reference. Examples: EPSG:4326, AUTO:42001",
               defaultValue = "EPSG:4326")
    String crs;

    @Parameter(alias = "resampling",
               label = "Resampling Method",
               description = "The method used for resampling of floating-point raster data, if source products must\n" +
                             "be reprojected to the target CRS.",
               valueSet = {"Nearest", "Bilinear", "Bicubic"},
               defaultValue = "Nearest")
    private String resamplingMethodName;

    @Parameter(description = "The most-western longitude. All values west of this longitude will not be considered.",
               interval = "[-180,180]", defaultValue = "-15.0")
    double westBound;
    @Parameter(description = "The most-northern latitude. All values north of this latitude will not be considered.",
               interval = "[-90,90]", defaultValue = "75.0")
    double northBound;
    @Parameter(description = "The most-eastern longitude. All values east of this longitude will not be considered.",
               interval = "[-180,180]", defaultValue = "30.0")
    double eastBound;
    @Parameter(description = "The most-southern latitude. All values south of this latitude will not be considered.",
               interval = "[-90,90]", defaultValue = "35.0")
    double southBound;

    @Parameter(description = "Size of a pixel in X-direction in map units.", defaultValue = "0.05")
    double pixelSizeX;
    @Parameter(description = "Size of a pixel in Y-direction in map units.", defaultValue = "0.05")
    double pixelSizeY;

    @Parameter(description = "The name of the band in the source products. Either this or 'bandMathsExpression' must be provided.")
    String sourceBandName;

    @Parameter(description = "A band maths expression serving as input band. Either this or 'sourceBandName' must be provided.")
    String bandMathsExpression;

    @Parameter(description = "If given, this is the percentile band name prefix. If empty, the resulting percentile band’s name\n" +
                             "prefix will be either the 'sourceBandName' or created from the 'bandMathsExpression'.")
    String percentileBandNamePrefix;

    @Parameter(description = "The valid pixel expression serving as criterion for whether to consider pixels for computation.",
               defaultValue = "true")
    String validPixelExpression;

    @Parameter(description = "The percentiles.", defaultValue = "90")
    int[] percentiles;

    @Parameter(description = "The gap filling method for percentile calculation.",
               defaultValue = GAP_FILLING_METHOD_LINEAR_INTERPOLATION,
               valueSet = {
                           GAP_FILLING_METHOD_NO_GAP_FILLING, GAP_FILLING_METHOD_LINEAR_INTERPOLATION,
                           GAP_FILLING_METHOD_SPLINE_INTERPOLATION, GAP_FILLING_METHOD_QUADRATIC_INTERPOLATION
               }
    )
    String gapFillingMethod;

    @Parameter(description = "The fallback value for the start of a pixel time series. It will be considered if\n" +
                             "there is no valid value at the pixel of the oldest collocated mean band. This would be\n" +
                             "the case, if, e.g., there is a cloudy day at the time period start.",
               defaultValue = "0.0")
    Double startValueFallback;

    @Parameter(description = "The fallback value for the end of a pixel time series. It will be considered if" +
                             "there is no valid value at the pixel of the newest collocated mean band. This would be\n" +
                             "the case, if, e.g., there is a cloudy day at the time period end.",
               defaultValue = "0.0")
    Double endValueFallback;


    private TreeMap<Long, List<Product>> dailyGroupedSourceProducts;
    private long timeSeriesStartMJD;
    private long timeSeriesEndMJD;
    private int timeSeriesLength;
    private Product timeSeriesDataProduct;
    private HashMap<String, Integer> timeSeriesBandNameToDayIndexMap;
    private PercentileComputer percentileComputer;
    private Interpolator interpolator;

    @Override
    public void initialize() throws OperatorException {
        validateInput();
        interpolator = InterpolatorFactory.createInterpolator(gapFillingMethod);

        final Product targetProduct = createTargetProduct();

        checkMemNeeds(targetProduct);

        final Area targetArea = Utils.createProductArea(targetProduct);
        setTargetProduct(targetProduct);


        final ProductValidator productValidator = new ProductValidator(sourceBandName, bandMathsExpression, validPixelExpression, startDate, endDate, targetArea, getLogger());
        final ProductLoader productLoader = new ProductLoader(sourceProductPaths, productValidator, getLogger());
        final Product[] products = productLoader.loadProducts();
        gc();

        dailyGroupedSourceProducts = Utils.groupProductsDaily(products);

        if (dailyGroupedSourceProducts.size() < 2) {
            throw new OperatorException("For temporal percentile calculation " +
                                        "at least two days must contain valid input products.");
        }
        if (dailyGroupedSourceProducts.size() == 2 && splineOrQuadraticInterpolationIsSelected()) {
            throw new OperatorException("For temporal percentile calculation " +
                                        "with percentileCalculationMethod='" + gapFillingMethod + "' " +
                                        "at least three days must contain valid input products.");
        }

        initTimeSeriesStartAndEnd();
        addInputMetadataToProduct(targetProduct);
        initTimeSeriesDataProduct();

        getLogger().log(Level.INFO, "Successfully initialized target product.");

        computeMeanDataForEachDayAndWriteDataToTimeSeriesProduct();

        reloadIntermediateTimeSeriesProduct();

        dailyGroupedSourceProducts.clear();

        getLogger().log(Level.INFO, "Input products colocated with target product.");

        initPercentileComputer();
    }

    private void checkMemNeeds(Product targetProduct) {
        final long _1kb = 1024;
        final long _1Mb = _1kb * _1kb;
        final long _1Gb = _1Mb * _1kb;
        final long baseMemoryRequirement = _1Gb;
        final Runtime runtime = Runtime.getRuntime();
        final long _Xmx = runtime.maxMemory();
        final long meanBandRawStorageSize = targetProduct.getBandAt(0).getRawStorageSize();
        if (meanBandRawStorageSize + baseMemoryRequirement > _Xmx) {
            final long width = targetProduct.getSceneRasterWidth();
            final long height = targetProduct.getSceneRasterHeight();
            if (width * height >= Integer.MAX_VALUE) {
                throw new OperatorException("The CRS settings result in a too large product (" + width + " * " + height + " pixels). " +
                                            "Please choose a smaller scene.");
            } else {
                final int needed = (int) Math.ceil((meanBandRawStorageSize + baseMemoryRequirement) / _1Gb);
                throw new OperatorException("The CRS settings result in a too large product (" + width + " * " + height + " pixels). " +
                                            "The memory needed to compute such a product is " + needed + " GB. " +
                                            "Please choose a smaller scene or increase the Java VM heap space parameter '-Xmx' accordingly.");
            }
        }
    }

    private boolean splineOrQuadraticInterpolationIsSelected() {
        return GAP_FILLING_METHOD_SPLINE_INTERPOLATION.equals(gapFillingMethod)
               || GAP_FILLING_METHOD_QUADRATIC_INTERPOLATION.equals(gapFillingMethod);
    }

    private void reloadIntermediateTimeSeriesProduct() {
        final File timeSeriesDataProductLocation = getTimeSeriesDataProductLocation();
        try {
            timeSeriesDataProduct.getProductWriter().close();
            timeSeriesDataProduct.dispose();
            timeSeriesDataProduct = null;
        } catch (IOException e) {
            throw new OperatorException(UNABLE_TO_WRITE_TIMESERIES_DATA_PRODUCT, e);
        }
        try {
            timeSeriesDataProduct = ProductIO.readProduct(timeSeriesDataProductLocation);
        } catch (IOException e) {
            throw new OperatorException(UNABLE_TO_READ_TIMESERIES_DATA_PRODUCT, e);
        }
    }

    private void initPercentileComputer() {
        if (GAP_FILLING_METHOD_NO_GAP_FILLING.equalsIgnoreCase(gapFillingMethod)) {
            percentileComputer = new PercentileComputer() {
                @Override
                public float[] computeThresholds(int[] targetPercentiles, float[] availableValues, int numAvailableValues) {
                    final float[] onlyValues = getValuesNotNaN(availableValues, numAvailableValues);
                    return computePercentileThresholds(targetPercentiles, onlyValues);
                }
            };
        } else {
            percentileComputer = new PercentileComputer() {
                @Override
                public float[] computeThresholds(int[] targetPercentiles, float[] availableValues, int numAvailableValues) {
                    GapFiller.fillGaps(availableValues, interpolator, startValueFallback.floatValue(), endValueFallback.floatValue());
                    return computePercentileThresholds(targetPercentiles, availableValues);
                }
            };
        }
    }

    private void computeMeanDataForEachDayAndWriteDataToTimeSeriesProduct() {
        for (long mjd : dailyGroupedSourceProducts.keySet()) {

            final List<Product> dailyGroupedProducts = dailyGroupedSourceProducts.get(mjd);
            getLogger().info("Compute collocated mean band for products: " + getProductNames(dailyGroupedProducts) + "");

            final List<Product> collocatedProducts = createCollocatedProducts(dailyGroupedProducts);

            final Band band = timeSeriesDataProduct.getBand(createNameForMeanBand(mjd));
            band.setSourceImage(createDailyMeanSourceImage(collocatedProducts));
            final int height = timeSeriesDataProduct.getSceneRasterHeight();
            final int width = timeSeriesDataProduct.getSceneRasterWidth();
            try {
                int bufferHeight = Math.max(1, Math.min(5000000 / width, height));
                ProductData rasterData = band.createCompatibleRasterData(width, bufferHeight);
                for (int y = 0; y < height; y += bufferHeight) {
                    if (height - y < bufferHeight) {
                        bufferHeight = height - y;
                        rasterData = band.createCompatibleRasterData(width, bufferHeight);
                    }
                    band.readRasterData(0, y, width, bufferHeight, rasterData, ProgressMonitor.NULL);
                    timeSeriesDataProduct.getProductWriter().writeBandRasterData(band, 0, y, width, bufferHeight, rasterData, ProgressMonitor.NULL);
                }
            } catch (IOException e) {
                throw new OperatorException(UNABLE_TO_WRITE_TIMESERIES_DATA_PRODUCT, e);
            } finally {
                dispose(collocatedProducts);
                dispose(dailyGroupedProducts);
                final ProductData bandData = band.getData();
                if (bandData != null) {
                    bandData.dispose();
                    band.setData(null);
                }
                gc();
            }
        }
    }

    private String getProductNames(List<Product> dailyGroupedProducts) {
        final StringWriter stringWriter = new StringWriter();

        for (Product dailyGroupedProduct : dailyGroupedProducts) {
            if (stringWriter.getBuffer().length() > 0) {
                stringWriter.append(", ");
            }
            stringWriter.append(dailyGroupedProduct.getFileLocation().getName());
        }
        return stringWriter.toString();
    }

    private RenderedImage createDailyMeanSourceImage(List<Product> collocatedProducts) {
        final Vector<RenderedImage> sources = new Vector<RenderedImage>();
        for (Product collocatedProduct : collocatedProducts) {
            final Band band;
            if (sourceBandName != null) {
                band = collocatedProduct.getBand(sourceBandName);
            } else {
                band = collocatedProduct.getBand(BAND_MATH_EXPRESSION_BAND_NAME);
            }
            MultiLevelImage nanImage = ImageManager.createMaskedGeophysicalImage(band, Float.NaN);
            sources.add(nanImage);
        }
        return new MeanOpImage(sources);
    }

    @Override
    public void computeTileStack(Map<Band, Tile> targetTilesMap, Rectangle targetRectangle, ProgressMonitor pm) throws OperatorException {
        //noinspection UnnecessaryLocalVariable
        final Rectangle r = targetRectangle;

        final Band[] targetPercentileBands = new Band[targetTilesMap.size() - 1];
        final Tile[] targetPercentileTiles = new Tile[targetTilesMap.size() - 1];
        Tile targetCountTile = null;
        int percentilesIDX = 0;
        for (Map.Entry<Band, Tile> bandTileEntry : targetTilesMap.entrySet()) {
            final Band band = bandTileEntry.getKey();
            final Tile tile = bandTileEntry.getValue();
            if (COUNT_BAND_NAME.equals(band.getName())) {
                targetCountTile = tile;
            } else {
                targetPercentileBands[percentilesIDX] = band;
                targetPercentileTiles[percentilesIDX] = tile;
                percentilesIDX++;
            }
        }

        final float[][] sourceTiles = new float[timeSeriesLength][0];
        for (String bandName : timeSeriesBandNameToDayIndexMap.keySet()) {
            final float[] sourceTile;
            try {
                sourceTile = new float[r.width * r.height];
                timeSeriesDataProduct.getBand(bandName).readPixels(r.x, r.y, r.width, r.height, sourceTile);
            } catch (IOException e) {
                throw new OperatorException("Unable to load source tiles.", e);
            }
            final int index = timeSeriesBandNameToDayIndexMap.get(bandName);
            sourceTiles[index] = sourceTile;
        }

        final float[] interpolationFloats = new float[timeSeriesLength];
        final int minNumValues = interpolator.getMinNumPoints();
        for (int targetY = r.y, sourceY = 0; targetY < (r.y + r.height); targetY++, sourceY++) {
            for (int targetX = r.x, sourceX = 0; targetX < (r.x + r.width); targetX++, sourceX++) {
                clear(interpolationFloats);
                int idx = sourceY * r.width + sourceX;
                final int valueCount = fillWithAvailableValues(idx, interpolationFloats, sourceTiles);

                final float[] percentileThresholds;
                final int[] targetPercentiles = new int[targetPercentileBands.length];
                if (valueCount < minNumValues) {
                    percentileThresholds = new float[targetPercentiles.length];
                    Arrays.fill(percentileThresholds, Float.NaN);
                } else {
                    for (int i = 0; i < targetPercentileBands.length; i++) {
                        targetPercentiles[i] = extractPercentileFromBandName(targetPercentileBands[i].getName());
                    }
                    percentileThresholds = percentileComputer.computeThresholds(targetPercentiles, interpolationFloats, valueCount);
                }
                for (int i = 0; i < targetPercentileTiles.length; i++) {
                    Tile percentileTile = targetPercentileTiles[i];
                    percentileTile.setSample(targetX, targetY, percentileThresholds[i]);
                }
                targetCountTile.setSample(targetX, targetY, valueCount);
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

    private void initTimeSeriesDataProduct() {
        timeSeriesBandNameToDayIndexMap = new HashMap<String, Integer>();
        timeSeriesDataProduct = createOutputProduct();
        addInputMetadataToProduct(timeSeriesDataProduct);
        final String targetName = getTargetBandNamePrefix();
        final int year = getYearOfTimePeriod();
        timeSeriesDataProduct.setName(year + "_" + targetName + SUFFIX_PERCENTILE_OP_DATA_PRODUCT);
        addExpectedMetadataForTimeSeriesTool(targetName);
        timeSeriesDataProduct.setAutoGrouping(targetName);
        timeSeriesDataProduct.setStartTime(ProductData.UTC.create(DateTimeUtils.jdToUTC(DateTimeUtils.mjdToJD(timeSeriesStartMJD)), 0));
        timeSeriesDataProduct.setEndTime(ProductData.UTC.create(DateTimeUtils.jdToUTC(DateTimeUtils.mjdToJD(timeSeriesEndMJD)), 0));
        for (long mjd : dailyGroupedSourceProducts.keySet()) {
            final String dayMeanBandName = createNameForMeanBand(mjd);
            final int dayIdx = (int) (mjd - timeSeriesStartMJD);
            timeSeriesBandNameToDayIndexMap.put(dayMeanBandName, dayIdx);
            final Band band = timeSeriesDataProduct.addBand(dayMeanBandName, ProductData.TYPE_FLOAT32);
            final List<Product> products = dailyGroupedSourceProducts.get(mjd);
            final Product product = products.get(0);
            if (sourceBandName != null) {
                final Band sourceBand = product.getBand(sourceBandName);
                band.setUnit(sourceBand.getUnit());
                band.setDescription(sourceBand.getDescription());
            }
            band.setNoDataValue(Double.NaN);
            band.setNoDataValueUsed(true);
        }
        final ProductWriter productWriter = ProductIO.getProductWriter(DimapProductConstants.DIMAP_FORMAT_NAME);
        final File timeSeriesDataProductLocation = getTimeSeriesDataProductLocation();
        try {
            productWriter.writeProductNodes(timeSeriesDataProduct, timeSeriesDataProductLocation);
        } catch (IOException e) {
            throw new OperatorException(UNABLE_TO_WRITE_TIMESERIES_DATA_PRODUCT, e);
        }
    }

    private File getTimeSeriesDataProductLocation() {
        final String filename = timeSeriesDataProduct.getName() + DimapProductConstants.DIMAP_HEADER_FILE_EXTENSION;
        final File location;
        if (timeSeriesOutputDir != null) {
            location = new File(timeSeriesOutputDir, filename);
        } else {
            location = new File(filename);
        }
        return location;
    }

    /**
     * This method adds metadata to the intermediate time series product.
     * This metadata is needed to meet the requirements of the time series tool.
     * <p>
     * The time series tool can be used to examine products that contain time series.
     * These time series have the form of bands where the timestamp of the band is
     * encoded as suffix of the band's name.
     *
     * @param bandName
     */
    private void addExpectedMetadataForTimeSeriesTool(final String bandName) {
        timeSeriesDataProduct.setProductType(TIME_SERIES_PRODUCT_TYPE);
        final MetadataElement tsMetadataRoot = new MetadataElement(TIME_SERIES_METADATA_ROOT_NAME);
        tsMetadataRoot.addElement(new MetadataElement(PRODUCT_LOCATIONS));
        final MetadataElement eoVariablesElement = new MetadataElement(TIME_SERIES_METADATA_VARIABLES_NAME);
        MetadataElement elem = new MetadataElement(TIME_SERIES_METADATA_VARIABLES_NAME + "." + 0);
        elem.addAttribute(new MetadataAttribute(TIME_SERIES_METADATA_VARIABLE_ATTRIBUTE_NAME,
                                                ProductData.createInstance(bandName), true));
        final ProductData isSelected = ProductData.createInstance(Boolean.toString(true));
        elem.addAttribute(new MetadataAttribute(VARIABLE_SELECTION, isSelected, true));
        eoVariablesElement.addElement(elem);
        tsMetadataRoot.addElement(eoVariablesElement);
        timeSeriesDataProduct.getMetadataRoot().addElement(tsMetadataRoot);
    }

    private String createNameForMeanBand(long mjd) {
        final double jd = DateTimeUtils.mjdToJD(mjd);
        final Date utc = DateTimeUtils.jdToUTC(jd);
        final SimpleDateFormat dateFormat = new SimpleDateFormat(BAND_DATE_FORMAT, Locale.ENGLISH);
        final String timeString = dateFormat.format(utc);
        return getTargetBandNamePrefix() + "_" + timeString;
    }

    private int getYearOfTimePeriod() {
        final double startJD = DateTimeUtils.mjdToJD(timeSeriesStartMJD);
        final Date startUTC = DateTimeUtils.jdToUTC(startJD);
        final Calendar calendar = Calendar.getInstance();
        calendar.setTime(startUTC);
        return calendar.get(Calendar.YEAR);
    }

    @Override
    public void dispose() {
        super.dispose();

        final File timeSeriesDataProductLocation = getTimeSeriesDataProductLocation();
        timeSeriesDataProduct.dispose();
        if (!keepIntermediateTimeSeriesProduct) {
            final String filenameWithoutExtension = FileUtils.getFilenameWithoutExtension(timeSeriesDataProductLocation);
            final File parentFile = timeSeriesDataProductLocation.getParentFile();
            final File dataDir = new File(parentFile, filenameWithoutExtension + ".data");
            Utils.safelyDeleteTree(dataDir);
            timeSeriesDataProductLocation.delete();
            timeSeriesDataProductLocation.deleteOnExit();
        }

        dailyGroupedSourceProducts.clear();
        dailyGroupedSourceProducts = null;
    }

    private void clear(float[] interpolationFloats) {
        Arrays.fill(interpolationFloats, Float.NaN);
    }

    private int fillWithAvailableValues(int idx, float[] interpolationFloats, float[][] sourceTiles) {
        int count = 0;
        for (int i = 0; i < interpolationFloats.length; i++) {
            float[] floats = sourceTiles[i];
            if (floats.length == 0) {
                continue;
            }
            interpolationFloats[i] = floats[idx];
            if (!Float.isNaN(interpolationFloats[i])) {
                count++;
            }
        }
        return count;
    }

    private void initTimeSeriesStartAndEnd() {
        final long oldestMJD = dailyGroupedSourceProducts.firstKey();
        final long newestMJD = dailyGroupedSourceProducts.lastKey();
        if (startDate != null) {
            timeSeriesStartMJD = Utils.utcToModifiedJulianDay(startDate.getAsDate());
        } else {
            timeSeriesStartMJD = oldestMJD;
        }
        if (endDate != null) {
            timeSeriesEndMJD = Utils.utcToModifiedJulianDay(endDate.getAsDate());
        } else {
            timeSeriesEndMJD = newestMJD;
        }
        timeSeriesLength = (int) (timeSeriesEndMJD - timeSeriesStartMJD + 1);
    }

    private List<Product> createCollocatedProducts(List<Product> dailyGroupedProducts) {
        final ArrayList<Product> collocatedProducts = new ArrayList<Product>();
        final HashMap<String, Object> projectionParameters = createProjectionParameters();
        for (Product product : dailyGroupedProducts) {
            HashMap<String, Product> productToBeReprojectedMap = new HashMap<String, Product>();
            productToBeReprojectedMap.put("source", product);
            productToBeReprojectedMap.put("collocateWith", timeSeriesDataProduct);
            final Product collocatedProduct = GPF.createProduct("Reproject", projectionParameters, productToBeReprojectedMap);
            Band band;
            if (sourceBandName != null) {
                band = collocatedProduct.getBand(sourceBandName);
            } else {
                band = collocatedProduct.addBand(BAND_MATH_EXPRESSION_BAND_NAME, bandMathsExpression);
            }
            if (StringUtils.isNotNullAndNotEmpty(validPixelExpression)) {
                band.setValidPixelExpression(validPixelExpression);
            }
            collocatedProducts.add(collocatedProduct);
        }
        return collocatedProducts;
    }

    private String getTargetBandNamePrefix() {
        if (percentileBandNamePrefix != null) {
            return percentileBandNamePrefix;
        }
        if (sourceBandName != null) {
            return sourceBandName;
        }
        return bandMathsExpression.replaceAll(" ", "_");
    }

    private HashMap<String, Object> createProjectionParameters() {
        HashMap<String, Object> projParameters = new HashMap<String, Object>();
        projParameters.put("resamplingName", resamplingMethodName);
        projParameters.put("includeTiePointGrids", false);
        return projParameters;
    }

    private void addInputMetadataToProduct(final Product product) {
        addInputProductPathsToMetadata(product);
        addBandConfigurationToMetadata(product);
    }

    private void addBandConfigurationToMetadata(Product product) {
        final MetadataElement bandConfigurationElem = new MetadataElement("BandConfiguration");

        if (sourceBandName != null) {
            final ProductData sourceBandData = ProductData.createInstance(sourceBandName);
            bandConfigurationElem.addAttribute(new MetadataAttribute("sourceBandName", sourceBandData, true));
        }

        if (bandMathsExpression != null) {
            final ProductData bandMathsData = ProductData.createInstance(bandMathsExpression);
            bandConfigurationElem.addAttribute(new MetadataAttribute("bandMathsExpression", bandMathsData, true));
        }

        if (percentileBandNamePrefix != null) {
            final ProductData percentileNameData = ProductData.createInstance(percentileBandNamePrefix);
            bandConfigurationElem.addAttribute(new MetadataAttribute("percentileBandNamePrefix", percentileNameData, true));
        }

        final ProductData interpolationData = ProductData.createInstance(gapFillingMethod);
        bandConfigurationElem.addAttribute(new MetadataAttribute("gapFillingMethod", interpolationData, true));

        String expr = validPixelExpression;
        final ProductData validPixelExpressionData = ProductData.createInstance(expr == null ? "" : expr);
        bandConfigurationElem.addAttribute(new MetadataAttribute("validPixelExpression", validPixelExpressionData, true));

        final ProductData percentilesData = ProductData.createInstance(percentiles);
        bandConfigurationElem.addAttribute(new MetadataAttribute("percentiles", percentilesData, true));

        final ProductData endValueData = ProductData.createInstance(new double[]{endValueFallback});
        bandConfigurationElem.addAttribute(new MetadataAttribute("endValueFallback", endValueData, true));

        final ProductData startValueData = ProductData.createInstance(new double[]{startValueFallback});
        bandConfigurationElem.addAttribute(new MetadataAttribute("startValueFallback", startValueData, true));

        product.getMetadataRoot().addElement(bandConfigurationElem);
    }

    private void addInputProductPathsToMetadata(Product product) {
        final MetadataElement productsElement = new MetadataElement("Input products");
        final String[] absoluteInputProductPaths = getAbsoluteInputProductPaths();
        for (int i = 0; i < absoluteInputProductPaths.length; i++) {
            String inputProductAbsPath = absoluteInputProductPaths[i];
            final ProductData pathData = ProductData.createInstance(inputProductAbsPath);
            final MetadataAttribute pathAttribute = new MetadataAttribute("product_" + i, pathData, true);
            productsElement.addAttribute(pathAttribute);
        }
        product.getMetadataRoot().addElement(productsElement);
    }

    private String[] getAbsoluteInputProductPaths() {
        final ArrayList<String> absolutePaths = new ArrayList<String>();
        for (List<Product> products : dailyGroupedSourceProducts.values()) {
            for (Product product : products) {
                absolutePaths.add(product.getFileLocation().getAbsolutePath());
            }
        }
        return absolutePaths.toArray(new String[absolutePaths.size()]);
    }

    private Product createTargetProduct() {
        final Product product = createOutputProduct();
        addTargetBands(product);
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

            final Product product = new Product("Percentile", "TemporalPercentile", width, height);
            product.setSceneGeoCoding(geoCoding);
            final Dimension tileSize = JAIUtils.computePreferredTileSize(width, height, 1);
            product.setPreferredTileSize(tileSize);
            return product;
        } catch (Exception e) {
            throw new OperatorException(e);
        }
    }

    private void addTargetBands(Product product) {
        final String bandNamePrefix = getTargetBandNamePrefix();
        for (Integer percentile : percentiles) {
            final String name = getTargetPercentileBandName(bandNamePrefix, percentile);
            final Band band = product.addBand(name, ProductData.TYPE_FLOAT32);
            band.setNoDataValue(Double.NaN);
            band.setNoDataValueUsed(true);
        }
        product.addBand(COUNT_BAND_NAME, ProductData.TYPE_UINT16);
    }

    private String getTargetPercentileBandName(String prefix, int percentile) {
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
        if (sourceBandName == null && bandMathsExpression == null || sourceBandName != null && bandMathsExpression != null) {
            throw new OperatorException("Either parameter 'sourceBandName' or 'bandMathExpression' must be specified.");
        }
        if (timeSeriesOutputDir != null && !timeSeriesOutputDir.isDirectory()) {
            throw new OperatorException("The output dir '" + timeSeriesOutputDir.getAbsolutePath() + "' does not exist.");
        }
        if (westBound == eastBound) {
            throw new OperatorException("Most western longitude must be different from most eastern longitude.");
        }
        if (northBound <= southBound) {
            throw new OperatorException("Most northern latitude must be larger than most southern latitude.");
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
     * in {@code /META-INF/services/OperatorSpi}.
     */
    public static class Spi extends OperatorSpi {

        public Spi() {
            super(TemporalPercentileOp.class);
        }
    }

    private static abstract class PercentileComputer {

        abstract float[] computeThresholds(int[] targetPercentiles, float[] availableValues, int numAvailableValues);

        protected float[] computePercentileThresholds(int[] targetPercentiles, float[] availableValues) {
            Arrays.sort(availableValues);
            final float[] thresholds = new float[targetPercentiles.length];
            for (int i = 0; i < targetPercentiles.length; i++) {
                int percentile = targetPercentiles[i];
                int percentileIndex = (int) Math.floor(percentile / 100f * availableValues.length);
                thresholds[i] = availableValues[percentileIndex];
            }
            return thresholds;
        }

        protected float[] getValuesNotNaN(float[] availableValues, int numAvailableValues) {
            final float[] onlyValues = new float[numAvailableValues];
            int i = 0;
            for (float availableValue : availableValues) {
                if (!Float.isNaN(availableValue)) {
                    onlyValues[i] = availableValue;
                    i++;
                }
            }
            return onlyValues;
        }
    }
}
