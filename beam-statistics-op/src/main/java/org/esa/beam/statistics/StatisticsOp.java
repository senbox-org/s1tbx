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

package org.esa.beam.statistics;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.Converter;
import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Mask;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.Stx;
import org.esa.beam.framework.datamodel.StxFactory;
import org.esa.beam.framework.datamodel.VectorDataNode;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProducts;
import org.esa.beam.framework.gpf.experimental.Output;
import org.esa.beam.jai.ImageManager;
import org.esa.beam.statistics.output.BandNameCreator;
import org.esa.beam.statistics.output.CsvStatisticsWriter;
import org.esa.beam.statistics.output.FeatureStatisticsWriter;
import org.esa.beam.statistics.output.MetadataWriter;
import org.esa.beam.statistics.output.StatisticsOutputContext;
import org.esa.beam.statistics.output.StatisticsOutputter;
import org.esa.beam.statistics.output.Util;
import org.esa.beam.util.FeatureUtils;
import org.esa.beam.util.io.FileUtils;
import org.esa.beam.util.io.WildcardMatcher;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import javax.media.jai.Histogram;
import java.awt.Color;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
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
 * @author Thomas Storm
 */
@OperatorMetadata(alias = "StatisticsOp",
                  version = "1.0",
                  authors = "Thomas Storm",
                  copyright = "(c) 2012 by Brockmann Consult GmbH",
                  description = "Computes statistics for an arbitrary number of source products.")
public class StatisticsOp extends Operator implements Output {

    public static final String DATETIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
    public static final String DEFAULT_PERCENTILES = "90,95";
    private static final int MAX_PRECISION = 6;

    @SourceProducts(description = "The source products to be considered for statistics computation. If not given, " +
                                  "the parameter 'sourceProductPaths' must be provided.")
    Product[] sourceProducts;

    @Parameter(description = "A comma-separated list of file paths specifying the source products.\n" +
                             "Each path may contain the wildcards '**' (matches recursively any directory),\n" +
                             "'*' (matches any character sequence in path names) and\n" +
                             "'?' (matches any single character).")
    String[] sourceProductPaths;

    @Parameter(description = "An ESRI shapefile, providing the considered geographical region(s) given as polygons. " +
                             "If null, all pixels are considered.")
    File shapefile;

    // todo se ask really true?
    @Parameter(description = "The start date. If not given, taken from the 'oldest' source product. Products that " +
                             "have a start date before the start date given by this parameter are not considered.",
               format = DATETIME_PATTERN, converter = UtcConverter.class)
    ProductData.UTC startDate;

    // todo se ask really true?
    @Parameter(description = "The end date. If not given, taken from the 'youngest' source product. Products that " +
                             "have an end date after the end date given by this parameter are not considered.",
               format = DATETIME_PATTERN, converter = UtcConverter.class)
    ProductData.UTC endDate;

    @Parameter(description = "The band configurations. These configurations determine the input of the operator.",
               alias = "bandConfigurations", itemAlias = "bandConfiguration")
    BandConfiguration[] bandConfigurations;

    @Parameter(description = "Determines if a copy of the input shapefile shall be created and augmented with the " +
                             "statistical data.")
    boolean doOutputShapefile;

    @Parameter(description = "The target file for shapefile output.")
    File outputShapefile;

    @Parameter(description = "Determines if the output shall be written into an ASCII file.")
    boolean doOutputAsciiFile;

    @Parameter(description = "The target file for ASCII output.")
    File outputAsciiFile;

    @Parameter(description = "The percentile levels that shall be created. Must be in the interval [0..100]",
               notNull = false, defaultValue = DEFAULT_PERCENTILES)
    int[] percentiles;

    @Parameter(description = "The number of significant figures used for statistics computation. Higher numbers " +
                             "indicate higher precision but may lead to a considerably longer computation time.",
               defaultValue = "3")
    int precision;

    Set<Product> collectedProducts;

    final Set<StatisticsOutputter> statisticsOutputters = new HashSet<StatisticsOutputter>();

    final Map<Product, VectorDataNode[]> productVdnMap = new HashMap<Product, VectorDataNode[]>();

    final SortedSet<String> regionNames = new TreeSet<String>();

    private PrintStream metadataOutputStream;

    private PrintStream csvOutputStream;

    private PrintStream bandMappingOutputStream;

    @Override
    public void initialize() throws OperatorException {
        setDummyTargetProduct();
        validateInput();
        setupOutputter();
        final Product[] allSourceProducts = collectSourceProducts();
        initializeVectorDataNodes(allSourceProducts);
        initializeOutput(allSourceProducts);
        computeOutput(allSourceProducts);
        writeOutput();
        getLogger().log(Level.INFO, "Successfully computed statistics.");
    }


    @Override
    public void dispose() {
        super.dispose();
        for (Product collectedProduct : collectedProducts) {
            collectedProduct.dispose();
        }
    }

    void initializeVectorDataNodes(Product[] allSourceProducts) {
        if (shapefile != null) {
            for (Product sourceProduct : allSourceProducts) {
                productVdnMap.put(sourceProduct, createVectorDataNodes(sourceProduct));
            }
        } else {
            regionNames.add("world");
        }
    }

    private VectorDataNode[] createVectorDataNodes(Product product) {
        final FeatureUtils.FeatureCrsProvider crsProvider = new FeatureUtils.FeatureCrsProvider() {
            @Override
            public CoordinateReferenceSystem getFeatureCrs(Product targetProduct) {
                if (ImageManager.getModelCrs(targetProduct.getGeoCoding()) == ImageManager.DEFAULT_IMAGE_CRS) {
                    return ImageManager.DEFAULT_IMAGE_CRS;
                }
                return DefaultGeographicCRS.WGS84;
            }
        };

        final FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection;
        try {
            featureCollection = FeatureUtils.loadShapefileForProduct(shapefile, product, crsProvider,
                                                                     ProgressMonitor.NULL);
        } catch (Exception e) {
            throw new OperatorException("Unable to load shapefile '" + shapefile.getAbsolutePath() + "'.", e);
        }

        final FeatureIterator<SimpleFeature> featureIterator = featureCollection.features();
        final List<VectorDataNode> result = new ArrayList<VectorDataNode>();
        while (featureIterator.hasNext()) {
            final SimpleFeature simpleFeature = featureIterator.next();
            final DefaultFeatureCollection fc = new DefaultFeatureCollection(simpleFeature.getID(),
                                                                             simpleFeature.getFeatureType());
            fc.add(simpleFeature);
            String name = Util.getFeatureName(simpleFeature);
            result.add(new VectorDataNode(name, fc));
        }

        for (final VectorDataNode vectorDataNode : result) {
            regionNames.add(vectorDataNode.getName());
        }
        return result.toArray(new VectorDataNode[result.size()]);
    }

    void initializeOutput(Product[] allSourceProducts) {
        final List<String> bandNamesList = new ArrayList<String>();
        for (BandConfiguration bandConfiguration : bandConfigurations) {
            if (bandConfiguration.sourceBandName != null) {
                bandNamesList.add(bandConfiguration.sourceBandName);
            } else {
                bandNamesList.add(bandConfiguration.expression);
            }
        }
        final String[] algorithmNames = getAlgorithmNames();
        final String[] bandNames = bandNamesList.toArray(new String[bandNamesList.size()]);
        final StatisticsOutputContext statisticsOutputContext = StatisticsOutputContext.create(allSourceProducts,
                                                                                               bandNames,
                                                                                               algorithmNames,
                                                                                               startDate,
                                                                                               endDate,
                                                                                               regionNames.toArray(new String[regionNames.size()]));
        for (StatisticsOutputter statisticsOutputter : statisticsOutputters) {
            statisticsOutputter.initialiseOutput(statisticsOutputContext);
        }
    }

    private String[] getAlgorithmNames() {
        final List<String> algorithms = new ArrayList<String>();
        algorithms.add("minimum");
        algorithms.add("maximum");
        algorithms.add("median");
        algorithms.add("average");
        algorithms.add("sigma");
        for (int percentile : percentiles) {
            algorithms.add(getPercentileName(percentile));
        }
        algorithms.add("pxx_max_error");
        algorithms.add("total");
        return algorithms.toArray(new String[algorithms.size()]);
    }

    private String getPercentileName(int percentile) {
        return "p" + percentile;
    }

    void setupOutputter() {
        if (doOutputAsciiFile) {
            try {
                final StringBuilder metadataFileName = new StringBuilder(
                        FileUtils.getFilenameWithoutExtension(outputAsciiFile));
                metadataFileName.append("_metadata.txt");
                final File metadataFile = new File(outputAsciiFile.getParent(), metadataFileName.toString());
                metadataOutputStream = new PrintStream(new FileOutputStream(metadataFile));
                csvOutputStream = new PrintStream(new FileOutputStream(outputAsciiFile));
                statisticsOutputters.add(new CsvStatisticsWriter(csvOutputStream));
                statisticsOutputters.add(new MetadataWriter(metadataOutputStream));
            } catch (FileNotFoundException e) {
                throw new OperatorException(e);
            }
        }
        if (doOutputShapefile) {
            try {
                final String baseName = FileUtils.getFilenameWithoutExtension(outputShapefile);
                final File file = new File(outputShapefile.getParent(), baseName + "_band_mapping.txt");
                final FileOutputStream outputStream = new FileOutputStream(file);
                bandMappingOutputStream = new PrintStream(outputStream);
                BandNameCreator bandNameCreator = new BandNameCreator(bandMappingOutputStream);
                statisticsOutputters.add(FeatureStatisticsWriter.createFeatureStatisticsWriter(shapefile.toURI().toURL(), outputShapefile.getAbsolutePath(), bandNameCreator));
            } catch (MalformedURLException e) {
                throw new OperatorException("Unable to create shapefile outputter", e);
            } catch (FileNotFoundException e) {
                throw new OperatorException("Unable to create shapefile outputter", e);
            }
        }

    }

    void computeOutput(Product[] allSourceProducts) {
        final List<Band> bands = new ArrayList<Band>();
        for (BandConfiguration configuration : bandConfigurations) {
            final HashMap<String, List<Mask>> regionNameToMasks = new HashMap<String, List<Mask>>();
            for (Product product : allSourceProducts) {
                final Band band = getBand(configuration, product);
                band.setValidPixelExpression(configuration.validPixelExpression);
                bands.add(band);
                fillRegionToMaskMap(regionNameToMasks, product);
            }
            for (String regionName : regionNames) {
                final List<Mask> maskList = regionNameToMasks.get(regionName);
                final Mask[] roiMasks = getMasksForBands(maskList, bands);
                final Band[] bandsArray = bands.toArray(new Band[bands.size()]);
                final Stx stx = new StxFactory()
                        .withHistogramBinCount(computeBinCount(precision))
                        .create(ProgressMonitor.NULL, roiMasks, bandsArray);
                final HashMap<String, Number> stxMap = new HashMap<String, Number>();
                Histogram histogram = stx.getHistogram();
                stxMap.put("minimum", histogram.getLowValue(0));
                stxMap.put("maximum", histogram.getHighValue(0));
                stxMap.put("average", histogram.getMean()[0]);
                stxMap.put("sigma", histogram.getStandardDeviation()[0]);
                stxMap.put("total", histogram.getTotals()[0]);
                stxMap.put("median", histogram.getPTileThreshold(0.5)[0]);
                for (int percentile : percentiles) {
                    if (precision > MAX_PRECISION) {
                        final PrecisePercentile precisePercentile = PrecisePercentile.createPrecisePercentile(
                                bandsArray,
                                histogram,
                                percentile * 0.01);
                        stxMap.put(getPercentileName(percentile), precisePercentile.percentile);
                        stxMap.put("pxx_max_error", precisePercentile.maxError);
                    } else {
                        stxMap.put(getPercentileName(percentile), computePercentile(percentile, histogram));
                        stxMap.put("pxx_max_error", Util.getBinWidth(histogram));
                    }
                }
                for (StatisticsOutputter statisticsOutputter : statisticsOutputters) {
                    statisticsOutputter.addToOutput(bands.get(0).getName(), regionName, stxMap);
                }
            }
            bands.clear();
        }
    }

    private Number computePercentile(int percentile, Histogram histogram) {
        return histogram.getPTileThreshold(percentile * 0.01)[0];
    }

    public static int computeBinCount(int precision) {
        if (precision < 0) {
            throw new IllegalArgumentException("precision < 0");
        } else if (precision > MAX_PRECISION) {
            return 1024 * 1024;
        }
        return (int) Math.pow(10, precision);
    }

    private Mask[] getMasksForBands(List<Mask> allMasks, List<Band> bands) {
        Mask[] masks = new Mask[bands.size()];
        for (int i = 0; i < bands.size(); i++) {
            final Band band = bands.get(i);
            for (Mask mask : allMasks) {
                if (band.getProduct() == mask.getProduct()) {
                    masks[i] = mask;
                    break;
                } else {
                    masks[i] = band.getProduct().addMask("emptyMask", "false", "mask that accepts no value",
                                                         Color.RED, 0.5);
                }
            }
        }

        return masks;
    }

    private void fillRegionToMaskMap(HashMap<String, List<Mask>> regionNameToMasks, Product product) {
        if (shapefile != null) {
            for (VectorDataNode vectorDataNode : productVdnMap.get(product)) {
                product.getVectorDataGroup().add(vectorDataNode);
                if (!regionNameToMasks.containsKey(vectorDataNode.getName())) {
                    regionNameToMasks.put(vectorDataNode.getName(), new ArrayList<Mask>());
                }
                Mask currentMask = product.getMaskGroup().get(vectorDataNode.getName());
                regionNameToMasks.get(vectorDataNode.getName()).add(currentMask);
            }
        } else {
            regionNameToMasks.put("world", new ArrayList<Mask>());
        }
    }

    static Band getBand(BandConfiguration configuration, Product product) {
        final Band band;
        if (configuration.sourceBandName != null) {
            band = product.getBand(configuration.sourceBandName);
            band.setNoDataValueUsed(true);
        } else {
            band = product.addBand(configuration.expression.replace(" ", "_"), configuration.expression,
                                   ProductData.TYPE_FLOAT64);
        }
        if (band == null) {
            throw new OperatorException(MessageFormat.format("Band ''{0}'' does not exist in product ''{1}''.",
                                                             configuration.sourceBandName, product.getName()));
        }
        return band;
    }

    void writeOutput() {
        try {
            for (StatisticsOutputter statisticsOutputter : statisticsOutputters) {
                statisticsOutputter.finaliseOutput();
            }
        } catch (IOException e) {
            throw new OperatorException("Unable to write output.", e);
        } finally {
            if (metadataOutputStream != null) {
                metadataOutputStream.close();
            }
            if (csvOutputStream != null) {
                csvOutputStream.close();
            }
            if (bandMappingOutputStream != null) {
                bandMappingOutputStream.close();
            }
        }
    }

    void validateInput() {
        if (startDate != null && endDate != null && endDate.getAsDate().before(startDate.getAsDate())) {
            throw new OperatorException("End date '" + this.endDate + "' before start date '" + this.startDate + "'");
        }
        if (precision < 0) {
            throw new OperatorException("Parameter 'precision' must be greater than or equal to 0");
        }
        if ((sourceProducts == null || sourceProducts.length == 0) &&
            (sourceProductPaths == null || sourceProductPaths.length == 0)) {
            throw new OperatorException(
                    "Either source products must be given or parameter 'sourceProductPaths' must be specified");
        }
        for (BandConfiguration configuration : bandConfigurations) {
            if (configuration.sourceBandName == null && configuration.expression == null) {
                throw new OperatorException("Configuration must contain either a source band name or an expression.");
            }
            if (configuration.sourceBandName != null && configuration.expression != null) {
                throw new OperatorException("Configuration must contain either a source band name or an expression.");
            }
        }
        if (outputAsciiFile != null && outputAsciiFile.isDirectory()) {
            throw new OperatorException("Parameter 'outputAsciiFile' must not point to a directory.");
        }
        if (outputShapefile != null && outputShapefile.isDirectory()) {
            throw new OperatorException("Parameter 'outputShapefile' must not point to a directory.");
        }


        for (int percentile : percentiles) {
            if (percentile < 0 || percentile > 100) {
                throw new OperatorException("Percentile '" + percentile + "' outside of interval [0..100].");
            }
        }
    }

    Product[] collectSourceProducts() {
        final List<Product> products = new ArrayList<Product>();
        collectedProducts = new HashSet<Product>();
        if (sourceProducts != null) {
            Collections.addAll(products, sourceProducts);
        }
        if (sourceProductPaths != null) {
            SortedSet<File> fileSet = new TreeSet<File>();
            for (String filePattern : sourceProductPaths) {
                try {
                    WildcardMatcher.glob(filePattern, fileSet);
                } catch (IOException e) {
                    logReadProductError(filePattern);
                }
            }
            for (File file : fileSet) {
                if (!isProductAlreadyOpened(products, file)) {
                    try {
                        Product sourceProduct = ProductIO.readProduct(file);
                        if (sourceProduct != null) {
                            products.add(sourceProduct);
                            collectedProducts.add(sourceProduct);
                        } else {
                            logReadProductError(file.getAbsolutePath());
                        }
                    } catch (Exception e) {
                        logReadProductError(file.getAbsolutePath());
                    }
                }
            }
        }

        if (products.size() == 0) {
            throw new OperatorException("No input products found.");
        }

        return products.toArray(new Product[products.size()]);
    }

    static boolean isProductAlreadyOpened(List<Product> products, File file) {
        for (Product product : products) {
            if (product.getFileLocation().getAbsolutePath().equals(file.getAbsolutePath())) {
                return true;
            }
        }
        return false;
    }

    private void logReadProductError(String file) {
        getLogger().severe(String.format("Failed to read from '%s' (not a data product or reader missing)", file));
    }

    private void setDummyTargetProduct() {
        final Product product = new Product("dummy", "dummy", 2, 2);
        product.addBand("dummy", ProductData.TYPE_INT8);
        setTargetProduct(product);
    }

    public static class BandConfiguration {

        @Parameter(description = "The name of the band in the source products. If empty, parameter 'expression' must " +
                                 "be provided.")
        String sourceBandName;

        @Parameter(description =
                           "The band maths expression serving as input band. If empty, parameter 'sourceBandName'" +
                           "must be provided.")
        String expression;

        @Parameter(description = "The band maths expression serving as criterion for whether to consider pixels for " +
                                 "computation.")
        String validPixelExpression;

        public BandConfiguration() {
            // used by DOM converter
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
            super(StatisticsOp.class);
        }
    }

}
