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

package org.esa.snap.statistics;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.Converter;
import com.bc.ceres.core.ProgressMonitor;
import java.util.Calendar;
import java.util.Collection;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.HistogramStxOp;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.QualitativeStxOp;
import org.esa.snap.core.datamodel.SummaryStxOp;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProducts;
import org.esa.snap.core.util.io.FileUtils;
import org.esa.snap.core.util.io.WildcardMatcher;
import org.esa.snap.statistics.output.BandNameCreator;
import org.esa.snap.statistics.output.CsvStatisticsWriter;
import org.esa.snap.statistics.output.FeatureStatisticsWriter;
import org.esa.snap.statistics.output.MetadataWriter;
import org.esa.snap.statistics.output.StatisticsOutputContext;
import org.esa.snap.statistics.output.StatisticsOutputter;
import org.esa.snap.statistics.output.Util;

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
import org.esa.snap.statistics.tools.TimeInterval;

/**
 * An operator that is used to compute statistics for any number of source products, restricted to time intervals and
 * regions given by an ESRI shapefile. If no time intervals are defined, statistics are aggregated over the whole period.
 * If no region is given, all pixels of a product are considered.
 *
 * <p>
 * The operator writes two different sorts of output:
 * <ul>
 * <li>an ASCII file in tab-separated CSV format, in which the statistics are mapped to the source regions, time intervals and bands</li>
 * <li>a shapefile that corresponds to the input shapefile, enriched with the statistics for the regions defined by the shapefile</li>
 * </ul>
 * <p>
 * Unlike most other operators, that can compute single {@link Tile tiles},
 * the statistics operator processes all of its source products in its {@link #doExecute(ProgressMonitor)} method.
 *
 * @author Sabine Embacher
 * @author Tonio Fincke
 * @author Thomas Storm
 */
@OperatorMetadata(alias = "StatisticsOp",
        category = "Raster",
        version = "1.0",
        authors = "Sabine Embacher, Tonio Fincke, Thomas Storm",
        copyright = "(c) 2012 by Brockmann Consult GmbH",
        description = "Computes statistics for an arbitrary number of source products.",
        autoWriteDisabled = true)
public class StatisticsOp extends Operator {

    public static final String DATETIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
    public static final String MAJORITY_CLASS = "majority_class";
    public static final String SECOND_MAJORITY_CLASS = "second_majority_class";
    public static final String MAXIMUM = "maximum";
    public static final String MINIMUM = "minimum";
    public static final String MEDIAN = "median";
    public static final String AVERAGE = "average";
    public static final String SIGMA = "sigma";
    public static final String MAX_ERROR = "max_error";
    public static final String TOTAL = "total";
    public static final String PERCENTILE_PREFIX = "p";
    public static final String PERCENTILE_SUFFIX = "_threshold";
    public static final String DEFAULT_PERCENTILES = "90,95";
    public static final int[] DEFAULT_PERCENTILES_INTS = new int[]{90, 95};

    private static final double FILL_VALUE = -999.0;
    static final int ALL_MEASURES = 0;
    static final int QUALITATIVE_MEASURES = 1;
    static final int QUANTITATIVE_MEASURES = 2;

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

    @Parameter(description = "If set, the StatisticsOp will divide the time between start and end time into time intervals" +
            "defined by this parameter. All measures will be aggregated from products within these intervals. " +
            "This parameter will only have an effect if the parameters start date and end date are set.")
    TimeIntervalDefinition interval;

    @Parameter(description = "If true, categorical measures and quantitative measures will be written separately.",
            defaultValue = "false")
    boolean writeDataTypesSeparately;

    final Set<StatisticsOutputter> allStatisticsOutputters = new HashSet<>();
    private final Set<StatisticsOutputter> qualitativeStatisticsOutputters = new HashSet<>();
    private final Set<StatisticsOutputter> quantitativeStatisticsOutputters = new HashSet<>();
    private final Set[] statisticsOutputters =
            new Set[]{allStatisticsOutputters, qualitativeStatisticsOutputters, quantitativeStatisticsOutputters};

    private final SortedSet<String> regionNames = new TreeSet<>();

    private PrintStream[] metadataOutputStreams = new PrintStream[3];
    private PrintStream[] csvOutputStreams = new PrintStream[3];
    private PrintStream[] bandMappingOutputStreams = new PrintStream[3];

    @Override
    public void initialize() throws OperatorException {
        setDummyTargetProduct();
        validateInput();
    }

    @Override
    public void doExecute(ProgressMonitor pm) throws OperatorException {
        TimeInterval[] timeIntervals = getTimeIntervals(interval, startDate, endDate);

        final StatisticComputer statisticComputer = new StatisticComputer(shapefile, bandConfigurations,
                Util.computeBinCount(accuracy), timeIntervals, getLogger());

        final ProductValidator productValidator = new ProductValidator(Arrays.asList(bandConfigurations), startDate, endDate, getLogger());
        final ProductLoop productLoop = new ProductLoop(new ProductLoader(), productValidator, statisticComputer, pm, getLogger());
        productLoop.loop(sourceProducts, getProductsToLoad());

        if (startDate == null) {
            startDate = productLoop.getOldestDate();
            timeIntervals[0].setIntervalStart(startDate);
        }

        if (endDate == null) {
            endDate = productLoop.getNewestDate();
            timeIntervals[0].setIntervalEnd(endDate);
        }

        final String[] productNames = productLoop.getProductNames();
        if (productNames.length == 0) {
            throw new OperatorException("No input products found that matches the criteria.");
        }

        final Map<BandConfiguration, StatisticComputer.StxOpMapping>[] stxOpsList = statisticComputer.getResultList();

        regionNames.clear();
        for (Map<BandConfiguration, StatisticComputer.StxOpMapping> stxOps : stxOpsList) {
            for (StatisticComputer.StxOpMapping stxOpMapping : stxOps.values()) {
                regionNames.addAll(stxOpMapping.summaryMap.keySet());
                regionNames.addAll(stxOpMapping.qualitativeMap.keySet());
            }
        }

        if (regionNames.size() == 0) {
            getLogger().warning("No statistics computed because no input product intersects any feature from the given shapefile.");
            return;
        }

        String[] regionIDS = regionNames.toArray(new String[0]);
        defineOutputters(timeIntervals, percentiles, productNames, regionIDS, stxOpsList);


        for (int i = 0; i < timeIntervals.length; i++) {
            final Map<BandConfiguration, StatisticComputer.StxOpMapping> stxOps = statisticComputer.getResults(i);
            for (Map.Entry<BandConfiguration, StatisticComputer.StxOpMapping> bandConfigurationStxOpMappingEntry : stxOps.entrySet()) {
                final BandConfiguration bandConfiguration = bandConfigurationStxOpMappingEntry.getKey();
                final String bandName;
                if (bandConfiguration.sourceBandName != null) {
                    bandName = bandConfiguration.sourceBandName;
                } else {
                    bandName = bandConfiguration.expression.replace(" ", "_");
                }
                final StatisticComputer.StxOpMapping stxOpMapping = bandConfigurationStxOpMappingEntry.getValue();
                final Map<String, QualitativeStxOp> qualitativeMap = stxOpMapping.qualitativeMap;
                for (String regionName : qualitativeMap.keySet()) {
                    final HashMap<String, Object> stxMap = new HashMap<>();
                    final QualitativeStxOp qualitativeStxOp = qualitativeMap.get(regionName);
                    if (!qualitativeStxOp.getMajorityClass().equals(QualitativeStxOp.NO_MAJORITY_CLASS)) {
                        String[] classNames = qualitativeStxOp.getClassNames();
                        for (String className : classNames) {
                            stxMap.put(className, qualitativeStxOp.getNumberOfMembers(className));
                        }
                        stxMap.put(MAJORITY_CLASS, qualitativeStxOp.getMajorityClass());
                        stxMap.put(SECOND_MAJORITY_CLASS, qualitativeStxOp.getSecondMajorityClass());
                        stxMap.put(TOTAL, qualitativeStxOp.getTotalNumClassMembers());
                    }
                    for (StatisticsOutputter statisticsOutputter : qualitativeStatisticsOutputters) {
                        statisticsOutputter.addToOutput(bandName, timeIntervals[i], regionName, stxMap);
                    }
                }
                final Map<String, SummaryStxOp> summaryMap = stxOpMapping.summaryMap;
                final Map<String, HistogramStxOp> histogramMap = stxOpMapping.histogramMap;
                for (String regionName : summaryMap.keySet()) {
                    final HashMap<String, Object> stxMap = new HashMap<>();
                    final SummaryStxOp summaryStxOp = summaryMap.get(regionName);
                    final Histogram histogram = histogramMap.get(regionName).getHistogram();
                    if (histogram.getTotals()[0] == 0) {
                        stxMap.put(MINIMUM, FILL_VALUE);
                        stxMap.put(MAXIMUM, FILL_VALUE);
                        stxMap.put(AVERAGE, FILL_VALUE);
                        stxMap.put(SIGMA, FILL_VALUE);
                        stxMap.put(TOTAL, 0);
                        stxMap.put(MEDIAN, FILL_VALUE);
                        for (int percentile : percentiles) {
                            stxMap.put(getPercentileName(percentile), FILL_VALUE);
                        }
                    } else {
                        stxMap.put(MINIMUM, summaryStxOp.getMinimum());
                        stxMap.put(MAXIMUM, summaryStxOp.getMaximum());
                        stxMap.put(AVERAGE, summaryStxOp.getMean());
                        stxMap.put(SIGMA, summaryStxOp.getStandardDeviation());
                        stxMap.put(TOTAL, histogram.getTotals()[0]);
                        stxMap.put(MEDIAN, histogram.getPTileThreshold(0.5)[0]);
                        for (int percentile : percentiles) {
                            stxMap.put(getPercentileName(percentile), computePercentile(percentile, histogram));
                        }
                    }
                    stxMap.put(MAX_ERROR, Util.getBinWidth(histogram));
                    for (StatisticsOutputter statisticsOutputter : quantitativeStatisticsOutputters) {
                        statisticsOutputter.addToOutput(bandName, timeIntervals[i], regionName, stxMap);
                    }
                }
            }
        }
        try {
            for (StatisticsOutputter statisticsOutputter : allStatisticsOutputters) {
                statisticsOutputter.finaliseOutput();
            }
        } catch (IOException e) {
            throw new OperatorException("Unable to write output.", e);
        } finally {
            for (int j = 0; j < 3; j++) {
                if (metadataOutputStreams[j] != null) {
                    metadataOutputStreams[j].close();
                }
                if (csvOutputStreams[j] != null) {
                    csvOutputStreams[j].close();
                }
                if (bandMappingOutputStreams[j] != null) {
                    bandMappingOutputStreams[j].close();
                }
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

    /* package local for testing*/
    static TimeInterval[] getTimeIntervals(TimeIntervalDefinition interval, ProductData.UTC startDate, ProductData.UTC endDate) {
        if (startDate == null || endDate == null) {
            return new TimeInterval[]{new TimeInterval(0, new ProductData.UTC(0), new ProductData.UTC(1000000))};
        } else if (interval == null) {
            return new TimeInterval[]{new TimeInterval(0, startDate, endDate)};
        } else {
            ArrayList<TimeInterval> timeIntervalList = new ArrayList<>();
            int timeField = getTimeField(interval);
            ProductData.UTC currentStartDate = new ProductData.UTC(startDate.getMJD());
            ProductData.UTC currentEndDate = getIncreasedDate(startDate, timeField, interval.amount);
            int counter = 0;
            while (currentEndDate.getAsDate().before(endDate.getAsDate())) {
                timeIntervalList.add(new TimeInterval(counter++, currentStartDate, currentEndDate));
                currentStartDate = new ProductData.UTC(currentEndDate.getMJD());
                currentEndDate = getIncreasedDate(currentEndDate, timeField, interval.amount);
            }
            timeIntervalList.add(new TimeInterval(counter, currentStartDate, endDate));
            return timeIntervalList.toArray(new TimeInterval[0]);
        }
    }

    private static ProductData.UTC getIncreasedDate(ProductData.UTC date, int timeField, int amount) {
        Calendar calendar = date.getAsCalendar();
        calendar.add(timeField, amount);
        return ProductData.UTC.create(calendar.getTime(), 0);
    }

    private static int getTimeField(TimeIntervalDefinition interval) {
        switch (interval.unit) {
            case "days":
                return Calendar.DAY_OF_MONTH;
            case "weeks":
                return Calendar.WEEK_OF_YEAR;
            case "months":
                return Calendar.MONTH;
            case "years":
                return Calendar.YEAR;
            default:
                throw new OperatorException("Invalid interval unit: " + interval.unit);
        }
    }

    private static String[] getMeasureNames(Map<BandConfiguration, StatisticComputer.StxOpMapping>[] stxOpsList,
                                            int[] percentiles, int qualifier) {
        final List<String> measures = new ArrayList<String>();
        for (Map<BandConfiguration, StatisticComputer.StxOpMapping> stxOps : stxOpsList) {
            for (StatisticComputer.StxOpMapping stxOpMapping : stxOps.values()) {
                if (qualifier != QUALITATIVE_MEASURES && !measures.contains(MINIMUM)) {
                    Collection<SummaryStxOp> summaryStxOps = stxOpMapping.summaryMap.values();
                    for (SummaryStxOp summaryStxOp : summaryStxOps) {
                        if (!Double.isNaN(summaryStxOp.getMean())) {
                            measures.add(MINIMUM);
                            measures.add(MAXIMUM);
                            measures.add(MEDIAN);
                            measures.add(AVERAGE);
                            measures.add(SIGMA);
                            for (int percentile : percentiles) {
                                measures.add(getPercentileName(percentile));
                            }
                            measures.add(MAX_ERROR);
                            if (!measures.contains(TOTAL)) {
                                measures.add(TOTAL);
                            }
                            break;
                        }
                    }
                }
                if (qualifier != QUANTITATIVE_MEASURES) {
                    Collection<QualitativeStxOp> qualitativeStxOps = stxOpMapping.qualitativeMap.values();
                    if (!qualitativeStxOps.isEmpty() && !measures.contains(StatisticsOp.MAJORITY_CLASS)) {
                        measures.add(StatisticsOp.MAJORITY_CLASS);
                        measures.add(StatisticsOp.SECOND_MAJORITY_CLASS);
                        if (!measures.contains(TOTAL)) {
                            measures.add(StatisticsOp.TOTAL);
                        }
                    }
                    for (QualitativeStxOp qualitativeStxOp : qualitativeStxOps) {
                        if (!qualitativeStxOp.getMajorityClass().equals(QualitativeStxOp.NO_MAJORITY_CLASS)) {
                            String[] classNames = qualitativeStxOp.getClassNames();
                            for (String className : classNames) {
                                if (!measures.contains(className)) {
                                    measures.add(className);
                                }
                            }
                        }
                    }
                }
            }
        }
        return measures.toArray(new String[0]);
    }

    @Deprecated
    /*
     * will receive no replacement (@since 6.0.3)
     */
    public static String[] getAlgorithmNames(int[] percentiles) {
        final List<String> algorithms = new ArrayList<>();
        algorithms.add(MINIMUM);
        algorithms.add(MAXIMUM);
        algorithms.add(MEDIAN);
        algorithms.add(AVERAGE);
        algorithms.add(SIGMA);
        for (int percentile : percentiles) {
            algorithms.add(getPercentileName(percentile));
        }
        algorithms.add(MAX_ERROR);
        algorithms.add(TOTAL);
        return algorithms.toArray(new String[algorithms.size()]);
    }

    private static String getPercentileName(int percentile) {
        return PERCENTILE_PREFIX + percentile + PERCENTILE_SUFFIX;
    }

    private void defineOutputters(TimeInterval[] timeIntervals, int[] percentiles, String[] productNames,
                                  String[] regionIDs,
                                  Map<BandConfiguration, StatisticComputer.StxOpMapping>[] stxOpsList) {
        if (writeDataTypesSeparately && hasQualitativeAndQuantitativeData()) {
            defineOutputterType(timeIntervals, percentiles, productNames, regionIDs, stxOpsList, QUALITATIVE_MEASURES);
            defineOutputterType(timeIntervals, percentiles, productNames, regionIDs, stxOpsList, QUANTITATIVE_MEASURES);
            allStatisticsOutputters.addAll(qualitativeStatisticsOutputters);
            allStatisticsOutputters.addAll(quantitativeStatisticsOutputters);
        } else {
            defineOutputterType(timeIntervals, percentiles, productNames, regionIDs, stxOpsList, ALL_MEASURES);
            qualitativeStatisticsOutputters.addAll(allStatisticsOutputters);
            quantitativeStatisticsOutputters.addAll(allStatisticsOutputters);
        }
    }

    private void defineOutputterType(TimeInterval[] timeIntervals, int[] percentiles, String[] productNames,
                                     String[] regionIDs,
                                     Map<BandConfiguration, StatisticComputer.StxOpMapping>[] stxOpsList, int qualifier) {
        String[] bandNames = getBandNames(qualifier);
        String[] measureNames = getMeasureNames(stxOpsList, percentiles, qualifier);
        StatisticsOutputContext statisticsOutputContext =
                StatisticsOutputContext.create(productNames, bandNames, measureNames, timeIntervals, regionIDs);
        setupOutputters(qualifier);
        Set<StatisticsOutputter> outputters = statisticsOutputters[qualifier];
        for (StatisticsOutputter statisticsOutputter : outputters) {
            statisticsOutputter.initialiseOutput(statisticsOutputContext);
        }
    }

    private String[] getBandNames(int quantifier) {
        if (quantifier == QUALITATIVE_MEASURES) {
            return getBandNames(true, true);
        } else if (quantifier == QUANTITATIVE_MEASURES) {
            return getBandNames(true, false);
        } else {
            return getBandNames(false, true);
        }
    }

    private String[] getBandNames(boolean considerMeasureType, boolean retrieveCategorical) {
        final List<String> bandNamesList = new ArrayList<>();
        for (BandConfiguration bandConfiguration : bandConfigurations) {
            if (!considerMeasureType || bandConfiguration.retrieveCategoricalStatistics == retrieveCategorical) {
                if (bandConfiguration.sourceBandName != null) {
                    bandNamesList.add(bandConfiguration.sourceBandName);
                } else {
                    bandNamesList.add(bandConfiguration.expression.replace(" ", "_"));
                }
            }
        }
        return bandNamesList.toArray(new String[0]);
    }

    private boolean hasQualitativeAndQuantitativeData() {
        boolean hasQuantitativeData = false;
        boolean hasQualitativeData = false;
        for (BandConfiguration bandConfiguration : bandConfigurations) {
            if (bandConfiguration.retrieveCategoricalStatistics) {
                hasQualitativeData = true;
                if (hasQuantitativeData) {
                    return true;
                }
            } else {
                hasQuantitativeData = true;
                if (hasQualitativeData) {
                    return true;
                }
            }
        }
        return false;
    }

    /* package local for testing */
    @SuppressWarnings("WeakerAccess")
    static File getOutputFile(File origFile, int qualifier) {
        if (origFile == null) {
            return null;
        }
        if (qualifier == QUALITATIVE_MEASURES) {
            return new File(origFile.getParent(),
                    FileUtils.getFilenameWithoutExtension(origFile) + "_categorical" +
                            FileUtils.getExtension(origFile));
        } else if (qualifier == QUANTITATIVE_MEASURES) {
            return new File(origFile.getParent(),
                    FileUtils.getFilenameWithoutExtension(origFile) + "_quantitative" +
                            FileUtils.getExtension(origFile));
        }
        return origFile;
    }

    private void setupOutputters(int qualifier) {
        Set<StatisticsOutputter> outputters = statisticsOutputters[qualifier];
        File asciiFile = getOutputFile(outputAsciiFile, qualifier);
        if (asciiFile != null) {
            try {
                final File metadataFile = new File(asciiFile.getParent(),
                        FileUtils.getFilenameWithoutExtension(asciiFile) + "_metadata.txt");
                metadataOutputStreams[qualifier] = new PrintStream(new FileOutputStream(metadataFile));
                csvOutputStreams[qualifier] = new PrintStream(new FileOutputStream(asciiFile));
                outputters.add(new CsvStatisticsWriter(csvOutputStreams[qualifier]));
                outputters.add(new MetadataWriter(metadataOutputStreams[qualifier]));
            } catch (FileNotFoundException e) {
                throw new OperatorException(e);
            }
        }
        File shapeFileOut = getOutputFile(outputShapefile, qualifier);
        if (shapeFileOut != null) {
            try {
                final String baseName = FileUtils.getFilenameWithoutExtension(shapeFileOut);
                final File bandMappingFile = new File(shapeFileOut.getParent(), baseName + "_band_mapping.txt");
                final FileOutputStream bandMappingFOS = new FileOutputStream(bandMappingFile);
                bandMappingOutputStreams[qualifier] = new PrintStream(bandMappingFOS);
                BandNameCreator bandNameCreator = new BandNameCreator(bandMappingOutputStreams[qualifier]);
                outputters.add(
                        FeatureStatisticsWriter.createFeatureStatisticsWriter(shapefile.toURI().toURL(), shapeFileOut.getAbsolutePath(),
                                bandNameCreator));
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
        if (interval != null && interval.amount < 1) {
            throw new OperatorException("interval amount must be larger than 0.");
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

        if (percentiles == null || percentiles.length == 0) {
            percentiles = DEFAULT_PERCENTILES_INTS;
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
     * in {@code /META-INF/services/OperatorSpi}.
     */
    public static class Spi extends OperatorSpi {

        public Spi() {
            super(StatisticsOp.class);
        }
    }

}
