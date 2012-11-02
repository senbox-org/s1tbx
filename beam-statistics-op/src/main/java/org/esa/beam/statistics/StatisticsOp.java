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
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.HistogramStxOp;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.SummaryStxOp;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProducts;
import org.esa.beam.framework.gpf.experimental.Output;
import org.esa.beam.statistics.output.BandNameCreator;
import org.esa.beam.statistics.output.CsvStatisticsWriter;
import org.esa.beam.statistics.output.FeatureStatisticsWriter;
import org.esa.beam.statistics.output.MetadataWriter;
import org.esa.beam.statistics.output.StatisticsOutputContext;
import org.esa.beam.statistics.output.StatisticsOutputter;
import org.esa.beam.statistics.output.Util;
import org.esa.beam.util.io.FileUtils;
import org.esa.beam.util.io.WildcardMatcher;

import javax.media.jai.Histogram;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
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
 * @author Sabine Embacher
 * @author Tonio Fincke
 * @author Thomas Storm
 */
@OperatorMetadata(alias = "StatisticsOp",
                  version = "1.0",
                  authors = "Sabine Embacher, Tonio Fincke, Thomas Storm",
                  copyright = "(c) 2012 by Brockmann Consult GmbH",
                  description = "Computes statistics for an arbitrary number of source products.")
public class StatisticsOp extends Operator implements Output {

    public static final String DATETIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
    public static final String DEFAULT_PERCENTILES = "90,95";

    @SourceProducts(description = "The source products to be considered for statistics computation. If not given, " +
                                  "the parameter 'sourceProductPaths' must be provided.")
    Product[] sourceProducts;

    @Parameter(description = "A comma-separated list of file paths specifying the source products.\n" +
                             "Each path may contain the wildcards '**' (matches recursively any directory),\n" +
                             "'*' (matches any character sequence in path names) and\n" +
                             "'?' (matches any single character).\n" +
                             "If, for example, all NetCDF files under /eodata/ shall be considered, use '/eodata/**/*.nc'.")
    String[] sourceProductPaths;

    @Parameter(description = "An ESRI shapefile, providing the considered geographical region(s) given as polygons. " +
                             "If null, all pixels are considered.")
    File shapefile;

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

    @Parameter(description = "The target file for shapefile output. Shapefile output will only be written if this " +
                             "parameter is set. The band mapping file will have the suffix _band_mapping.txt.",
               notNull = false)
    File outputShapefile;

    @Parameter(description = "The target file for ASCII output." +
                             "The metadata file will have the suffix _metadata.txt.\n" +
                             "ASCII output will only be written if this parameter is set.", notNull = false)
    File outputAsciiFile;

    @Parameter(description = "The percentile levels that shall be created. Must be in the interval [0..100]",
               notNull = false, defaultValue = DEFAULT_PERCENTILES)
    int[] percentiles;

    @Parameter(description = "The degree of accuracy used for statistics computation. Higher numbers " +
                             "indicate higher accuracy but may lead to a considerably longer computation time.",
               defaultValue = "3")
    int accuracy;

    final Set<StatisticsOutputter> statisticsOutputters = new HashSet<StatisticsOutputter>();

    final SortedSet<String> regionNames = new TreeSet<String>();

    private PrintStream metadataOutputStream;
    private PrintStream csvOutputStream;
    private PrintStream bandMappingOutputStream;

    @Override
    public void initialize() throws OperatorException {
        setDummyTargetProduct();
        validateInput();

        final StatisticComputer statisticComputer = new StatisticComputer(shapefile, bandConfigurations, Util.computeBinCount(accuracy));

        final ProductValidator productValidator = new ProductValidator(Arrays.asList(bandConfigurations), startDate, endDate, getLogger());
        final ProductLoop productLoop = new ProductLoop(new ProductLoader(), productValidator, statisticComputer, getLogger());
        productLoop.loop(sourceProducts, getProductsToLoad());

        if (startDate == null) {
            startDate = productLoop.getOldestDate();
        }

        if (endDate == null) {
            endDate = productLoop.getNewestDate();
        }

        final String[] productNames = productLoop.getProductNames();
        if (productNames.length == 0) {
            throw new OperatorException("No input products found that matches the criteria.");
        }

        final Map<BandConfiguration, StatisticComputer.StxOpMapping> results = statisticComputer.getResults();
        final String[] algorithmNames = getAlgorithmNames();


        final List<String> bandNamesList = new ArrayList<String>();
        for (BandConfiguration bandConfiguration : bandConfigurations) {
            if (bandConfiguration.sourceBandName != null) {
                bandNamesList.add(bandConfiguration.sourceBandName);
            } else {
                bandNamesList.add(bandConfiguration.expression.replace(" ", "_"));
            }
        }
        final String[] bandNames = bandNamesList.toArray(new String[bandNamesList.size()]);


        regionNames.clear();
        for (StatisticComputer.StxOpMapping stxOpMapping : results.values()) {
            regionNames.addAll(stxOpMapping.summaryMap.keySet());
        }

        if (regionNames.size() == 0) {
            getLogger().warning("No statistics computed because no input product intersects any feature from the given shapefile.");
            return;
        }

        final StatisticsOutputContext statisticsOutputContext = StatisticsOutputContext.create(productNames,
                                                                                               bandNames,
                                                                                               algorithmNames,
                                                                                               startDate,
                                                                                               endDate,
                                                                                               regionNames.toArray(new String[regionNames.size()]));

        setupOutputter();

        for (StatisticsOutputter statisticsOutputter : statisticsOutputters) {
            statisticsOutputter.initialiseOutput(statisticsOutputContext);
        }

        for (Map.Entry<BandConfiguration, StatisticComputer.StxOpMapping> bandConfigurationStxOpMappingEntry : results.entrySet()) {
            final BandConfiguration bandConfiguration = bandConfigurationStxOpMappingEntry.getKey();
            final String bandName;
            if (bandConfiguration.sourceBandName != null) {
                bandName = bandConfiguration.sourceBandName;
            } else {
                bandName = bandConfiguration.expression.replace(" ", "_");
            }
            final StatisticComputer.StxOpMapping stxOpMapping = bandConfigurationStxOpMappingEntry.getValue();
            final Map<String, SummaryStxOp> summaryMap = stxOpMapping.summaryMap;
            final Map<String, HistogramStxOp> histogramMap = stxOpMapping.histogramMap;
            for (String regionName : summaryMap.keySet()) {

                final SummaryStxOp summaryStxOp = summaryMap.get(regionName);
                final Histogram histogram = histogramMap.get(regionName).getHistogram();


                final HashMap<String, Number> stxMap = new HashMap<String, Number>();
                stxMap.put("minimum", summaryStxOp.getMinimum());
                stxMap.put("maximum", summaryStxOp.getMaximum());
                stxMap.put("average", summaryStxOp.getMean());
                stxMap.put("sigma", summaryStxOp.getStandardDeviation());
                stxMap.put("total", histogram.getTotals()[0]);
                stxMap.put("median", histogram.getPTileThreshold(0.5)[0]);
                for (int percentile : percentiles) {
                    stxMap.put(getPercentileName(percentile), computePercentile(percentile, histogram));
                }
                stxMap.put("max_error", Util.getBinWidth(histogram));
                for (StatisticsOutputter statisticsOutputter : statisticsOutputters) {
                    statisticsOutputter.addToOutput(bandName, regionName, stxMap);
                }
            }
        }

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

        getLogger().log(Level.INFO, "Successfully computed statistics.");
    }

    private File[] getProductsToLoad() {
        SortedSet<File> fileSet = new TreeSet<File>();
        if (sourceProductPaths != null) {
            for (String filePattern : sourceProductPaths) {
                if (filePattern == null) {
                    continue;
                }
                try {
                    WildcardMatcher.glob(filePattern, fileSet);
                } catch (IOException e) {
                    logReadProductError(filePattern);
                }
            }
        }
        return fileSet.toArray(new File[fileSet.size()]);
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
        algorithms.add("max_error");
        algorithms.add("total");
        return algorithms.toArray(new String[algorithms.size()]);
    }

    private String getPercentileName(int percentile) {
        return "p" + percentile + "_threshold";
    }

    void setupOutputter() {
        if (outputAsciiFile != null) {
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
        if (outputShapefile != null) {
            try {
                final String baseName = FileUtils.getFilenameWithoutExtension(outputShapefile);
                final File file = new File(outputShapefile.getParent(), baseName + "_band_mapping.txt");
                final FileOutputStream outputStream = new FileOutputStream(file);
                bandMappingOutputStream = new PrintStream(outputStream);
                BandNameCreator bandNameCreator = new BandNameCreator(bandMappingOutputStream);
                statisticsOutputters.add(FeatureStatisticsWriter.createFeatureStatisticsWriter(shapefile.toURI().toURL(), outputShapefile.getAbsolutePath(), bandNameCreator));
            } catch (MalformedURLException e) {
                throw new OperatorException(
                        "Unable to create shapefile outputter: shapefile '" + shapefile.getName() + "' is invalid.", e);
            } catch (FileNotFoundException e) {
                throw new OperatorException("Unable to create shapefile outputter: maybe shapefile output directory does not exist?", e);
            }
        }
    }

    private Number computePercentile(int percentile, Histogram histogram) {
        return histogram.getPTileThreshold(percentile * 0.01)[0];
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

    void validateInput() {
        if (startDate != null && endDate != null && endDate.getAsDate().before(startDate.getAsDate())) {
            throw new OperatorException("End date '" + this.endDate + "' before start date '" + this.startDate + "'");
        }
        if (accuracy < 0) {
            throw new OperatorException("Parameter 'accuracy' must be greater than or equal to " + 0);
        }
        if (accuracy > Util.MAX_ACCURACY) {
            throw new OperatorException("Parameter 'accuracy' must be less than or equal to " + Util.MAX_ACCURACY);
        }
        if ((sourceProducts == null || sourceProducts.length == 0) &&
            (sourceProductPaths == null || sourceProductPaths.length == 0)) {
            throw new OperatorException(
                    "Either source products must be given or parameter 'sourceProductPaths' must be specified");
        }
        if (bandConfigurations == null) {
            throw new OperatorException("Parameter 'bandConfigurations' must be specified.");
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
        if (outputShapefile != null) {
            if (outputShapefile.isDirectory()) {
                throw new OperatorException("Parameter 'outputShapefile' must point to a file.");
            }
            if (shapefile == null) {
                throw new OperatorException("Parameter 'shapefile' must be provided if an output shapefile shall be created.");
            }
        }

        if (shapefile != null && shapefile.isDirectory()) {
            throw new OperatorException("Parameter 'shapefile' must point to a file.");
        }

        for (int percentile : percentiles) {
            if (percentile < 0 || percentile > 100) {
                throw new OperatorException("Percentile '" + percentile + "' outside of interval [0..100].");
            }
        }
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
