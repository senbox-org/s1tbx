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

package org.esa.beam.binning.operator;

import com.bc.ceres.core.ProgressMonitor;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.esa.beam.binning.BinningContext;
import org.esa.beam.binning.DataPeriod;
import org.esa.beam.binning.SpatialBin;
import org.esa.beam.binning.SpatialBinner;
import org.esa.beam.binning.TemporalBin;
import org.esa.beam.binning.TemporalBinSource;
import org.esa.beam.binning.TemporalBinner;
import org.esa.beam.binning.cellprocessor.CellProcessorChain;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProducts;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.framework.gpf.descriptor.OperatorDescriptor;
import org.esa.beam.framework.gpf.experimental.Output;
import org.esa.beam.gpf.operators.standard.SubsetOp;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.util.StopWatch;
import org.esa.beam.util.StringUtils;
import org.esa.beam.util.converters.JtsGeometryConverter;
import org.esa.beam.util.io.FileUtils;
import org.esa.beam.util.io.WildcardMatcher;
import org.geotools.geometry.jts.JTS;

import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.Writer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;

/*

todo - address the following BinningOp requirements (nf, 2012-03-09)

(1) allow for reading a metadata attributes file (e.g. Java Properties file) whose content will be converted
    to NetCDF global attributes. See http://oceancolor.gsfc.nasa.gov/DOCS/Ocean_Level-3_Binned_Data_Products.pdf
    for possible attributes. Ideally, we treat the metadata file as a template and fill in placeholders, e.g.
    ${operatorParameters}, or ${operatorName} or ${operatorVersion} ...
(2) For simplicity, we shall not use BinningConfig and FormatterConfig but simply move their @Parameter declarations
    into the BinningOp class.
(3) For dealing with really large amounts of bins (global binning), we need SpatialBinConsumer and TemporalBinSource
    implementations that write to and read from local files. (E.g. use memory-mapped file I/O, see
    MappedByteBufferTest.java)
(4) For simplicity, we shall introduce a Boolean parameter 'global'. If it is true, 'region' will be ignored.

*/

/**
 * An operator that is used to perform spatial and temporal aggregations into "bin" cells for any number of source
 * product. The output is either a file comprising the resulting bins or a reprojected "map" of the bin cells
 * represented by a usual data product.
 * <p/>
 * Unlike most other operators, that can compute single {@link org.esa.beam.framework.gpf.Tile tiles},
 * the binning operator processes all
 * of its source products in its {@link #initialize()} method.
 *
 * @author Norman Fomferra
 * @author Marco Zühlke
 * @author Thomas Storm
 */
@SuppressWarnings("UnusedDeclaration")
@OperatorMetadata(alias = "Binning",
                  version = "0.8.2",
                  authors = "Norman Fomferra, Marco Zühlke, Thomas Storm",
                  copyright = "(c) 2012 by Brockmann Consult GmbH",
                  description = "Performs spatial and temporal aggregation of pixel values into 'bin' cells",
                  suppressWrite = true)
public class BinningOp extends Operator implements Output {

    public static final String DATE_PATTERN = "yyyy-MM-dd";
    public static final String DATETIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS";

    @SourceProducts(description = "The source products to be binned. Must be all of the same structure. " +
                                  "If not given, the parameter 'sourceProductPaths' must be provided.")
    Product[] sourceProducts;

    @TargetProduct
    Product targetProduct;

    @Parameter(description = "A comma-separated list of file paths specifying the source products.\n" +
                             "Each path may contain the wildcards '**' (matches recursively any directory),\n" +
                             "'*' (matches any character sequence in path names) and\n" +
                             "'?' (matches any single character).")
    String[] sourceProductPaths;

    // TODO nf/mz 2013-11-05 review this before BEAM 5, this could be a common Operator parameter
    @Parameter(description = "The common product format of all source products. This parameter is optional and may be used in conjunction " +
                             "with parameter 'sourceProductPaths' and only to speed up source product opening." +
                             "Try \"NetCDF-CF\", \"GeoTIFF\", \"BEAM-DIMAP\", or \"ENVISAT\", etc.",
               defaultValue = "")
    private String sourceProductFormat;

    @Parameter(converter = JtsGeometryConverter.class,
               description = "The considered geographical region as a geometry in well-known text format (WKT).\n" +
                             "If not given, the geographical region will be computed according to the extents of the " +
                             "input products.")
    Geometry region;

    @Parameter(description = "The start date. If not given, taken from the 'oldest' source product. Products that have " +
                             "a start date before the start date given by this parameter are not considered.",
               format = DATE_PATTERN)
    String startDate;

    @Parameter(description = "The end date. If not given, taken from the 'youngest' source product. Products that have " +
                             "an end date after the end date given by this parameter are not considered.",
               format = DATE_PATTERN)
    String endDate;

    @Parameter(description = "If true, a SeaDAS-style, binned data NetCDF file is written in addition to the\n" +
                             "target product. The output file name will be <target>-bins.nc",
               defaultValue = "true")
    boolean outputBinnedData;

    @Parameter(description = "If true, a mapped product is written. Set this to 'false' if only a binned product is needed.",
               alias = "outputMappedProduct",
               defaultValue = "true")
    boolean outputTargetProduct;

    @Parameter(notNull = true,
               description = "The configuration used for the binning process. Specifies the binning grid, any variables and their aggregators.")
    BinningConfig binningConfig;

    @Parameter(notNull = true,
               description = "The configuration used for the output formatting process.")
    FormatterConfig formatterConfig;

    @Parameter(description = "The name of the file containing metadata key-value pairs (google \"Java Properties file format\").",
               defaultValue = "./metadata.properties")
    File metadataPropertiesFile;

    @Parameter(description = "The name of the directory containing metadata templates (google \"Apache Velocity VTL format\").",
               defaultValue = ".")
    File metadataTemplateDir;

    @Parameter(description = "Applies a sensor-dependent, spatial data-day definition to the given time range. " +
                             "The decision, whether a source pixel contributes to a bin or not, is a functions of the pixel's observation longitude and time." +
                             "If true, the parameters 'startDate', 'endDate' must also be given.",
               defaultValue = "false")
    boolean useSpatialDataDay;

    // TODO mz 2013-11-06 unused !?!
    @Parameter(description = "The time in hours of a day (0 to 24) at which a given sensor has a minimum number of " +
                             "observations at the date line (the 180 degree meridian). Only used if parameters 'startDate' and 'useSpatialDataDay' are set.")
    private Double minDataHour;

    private transient BinningContext binningContext;
    private transient List<String> sourceProductNames;
    private transient ProductData.UTC minDateUtc;
    private transient ProductData.UTC maxDateUtc;
    private transient SortedMap<String, String> metadataProperties;
    private transient BinWriter binWriter;
    private transient Area regionArea;

    // TODO nf/mz 2013-11-05 review before BEAM 5 with thomas, discuss use of this field
    private final Map<Product, List<Band>> addedBands;
    private Product writtenProduct;

    public BinningOp() throws OperatorException {
        addedBands = new HashMap<>();
    }

    public Geometry getRegion() {
        return region;
    }

    public void setRegion(Geometry region) {
        this.region = region;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public BinningConfig getBinningConfig() {
        return binningConfig;
    }

    public void setBinningConfig(BinningConfig binningConfig) {
        this.binningConfig = binningConfig;
    }

    public FormatterConfig getFormatterConfig() {
        return formatterConfig;
    }

    public void setFormatterConfig(FormatterConfig formatterConfig) {
        this.formatterConfig = formatterConfig;
    }

    SortedMap<String, String> getMetadataProperties() {
        return metadataProperties;
    }

    public void setBinWriter(BinWriter binWriter) {
        this.binWriter = binWriter;
    }

    public void setOutputTargetProduct(boolean outputTargetProduct) {
        this.outputTargetProduct = outputTargetProduct;
    }

    /**
     * Processes all source products and writes the output file.
     * The target product represents the written output file
     *
     * @throws org.esa.beam.framework.gpf.OperatorException If a processing error occurs.
     */
    @Override
    public void initialize() throws OperatorException {
        ProductData.UTC startDateUtc = getStartDateUtc("startDate");
        ProductData.UTC endDateUtc = getEndDateUtc("endDate");

        validateInput(startDateUtc, endDateUtc);

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        if (region == null) {
            // TODO use JTS directly (nf 2013-11-06)
            regionArea = new Area();
        }

        if (startDate != null && useSpatialDataDay) {
            binningConfig.setStartDate(startDate);
        }

        binningContext = binningConfig.createBinningContext(region);

        BinningProductFilter productFilter = createSourceProductFilter(useSpatialDataDay ? binningContext.getDataPeriod() : null,
                                                                startDateUtc,
                                                                endDateUtc,
                                                                region);

        metadataProperties = new TreeMap<>();
        sourceProductNames = new ArrayList<>();

        try {
            // Step 1: Spatial binning - creates time-series of spatial bins for each bin ID ordered by ID. The tree map structure is <ID, time-series>
            SpatialBinCollection spatialBinMap = doSpatialBinning(productFilter);
            if (!spatialBinMap.isEmpty()) {
                // update region
                if (region == null && regionArea != null) {
                    region = JTS.shapeToGeometry(regionArea, new GeometryFactory());
                }
                // Step 2: Temporal binning - creates a list of temporal bins, sorted by bin ID
                TemporalBinList temporalBins = doTemporalBinning(spatialBinMap);
                // Step 3: Formatting
                try {
                    writeOutput(temporalBins, startDateUtc, endDateUtc);
                } finally {
                    temporalBins.close();

                }
            } else {
                getLogger().warning("No bins have been generated, no output has been written");
            }
        } catch (OperatorException e) {
            throw e;
        } catch (Exception e) {
            throw new OperatorException(e);
        } finally {
            cleanSourceProducts();
        }

        stopWatch.stopAndTrace(String.format("Total time for binning %d product(s)", sourceProductNames.size()));

        processMetadataTemplates();
    }

    @Override
    public void dispose() {
        if (writtenProduct != null) {
            writtenProduct.dispose();
        }
        super.dispose();
    }

    private void validateInput(ProductData.UTC startDateUtc, ProductData.UTC endDateUtc) {
        if (startDateUtc != null && endDateUtc != null && endDateUtc.getAsDate().before(startDateUtc.getAsDate())) {
            throw new OperatorException(String.format("Parameter 'endDate=%s' is before 'startDate=%s'", this.endDate, this.startDate));
        }
        if (useSpatialDataDay) {
            if (startDateUtc == null || endDateUtc == null) {
                throw new OperatorException("If parameter 'useSpatialDataDay=true' then parameters 'startDate' and 'endDate' must be given");
            }
        }
        if (sourceProducts == null && (sourceProductPaths == null || sourceProductPaths.length == 0)) {
            String msg = "Either source products must be given or parameter 'sourceProductPaths' must be specified";
            throw new OperatorException(msg);
        }
        if (binningConfig == null) {
            throw new OperatorException("Missing operator parameter 'binningConfig'");
        }
        if (binningConfig.getNumRows() <= 2) {
            throw new OperatorException("Operator parameter 'binningConfig.numRows' must be greater than 2");
        }
        if (hasNoVariableConfigs(binningConfig) && hasNoAggregatorConfigs(binningConfig)) {
            throw new OperatorException("Operator config does not define any output variable");
        }
        if (formatterConfig == null) {
            throw new OperatorException("Missing operator parameter 'formatterConfig'");
        }
        if (formatterConfig.getOutputFile() == null) {
            throw new OperatorException("Missing operator parameter 'formatterConfig.outputFile'");
        }
        if (metadataTemplateDir == null || "".equals(metadataTemplateDir.getPath())) {
            metadataTemplateDir = new File(".");
        }
        if (!metadataTemplateDir.exists()) {
            String msgPattern = "Directory given by 'metadataTemplateDir' does not exist: %s";
            throw new OperatorException(String.format(msgPattern, metadataTemplateDir));
        }
        if (!outputBinnedData && !outputTargetProduct) {
            throw new OperatorException("At least one of the parameters 'outputBinnedData' and 'outputTargetProduct' must be 'true'");
        }
    }

    // package access for testing only tb 2013-07-29
    static boolean hasNoAggregatorConfigs(BinningConfig binningConfig) {
        return binningConfig.getAggregatorConfigs() == null || binningConfig.getAggregatorConfigs().length == 0;
    }

    // package access for testing only tb 2013-07-29
    static boolean hasNoVariableConfigs(BinningConfig binningConfig) {
        return binningConfig.getVariableConfigs() == null || binningConfig.getVariableConfigs().length == 0;
    }

    static BinningProductFilter createSourceProductFilter(DataPeriod dataPeriod, ProductData.UTC startTime, ProductData.UTC endTime, Geometry region) {
        BinningProductFilter productFilter = new GeoCodingProductFilter();

        if (dataPeriod != null) {
            productFilter = new SpatialDataDaySourceProductFilter(productFilter, dataPeriod);
        } else if (startTime != null || endTime != null) {
            productFilter = new TimeRangeProductFilter(productFilter, startTime, endTime);
        }

        if (region != null) {
            productFilter = new RegionProductFilter(productFilter, region);
        }

        return productFilter;
    }

    private void cleanSourceProducts() {
        for (Map.Entry<Product, List<Band>> entry : addedBands.entrySet()) {
            for (Band band : entry.getValue()) {
                entry.getKey().removeBand(band);
            }
        }
    }

    private void processMetadataTemplates() {
        final File absTemplateDir = metadataTemplateDir.getAbsoluteFile();
        File[] files = absTemplateDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".vm");
            }
        });
        if (files == null || files.length == 0) {
            return;
        }

        final Properties veConfig = new Properties();
        if (absTemplateDir.equals(new File(".").getAbsoluteFile())) {
            veConfig.setProperty("file.resource.loader.path", absTemplateDir.getPath());
        }

        VelocityEngine ve = new VelocityEngine();
        try {
            ve.init(veConfig);
        } catch (Exception e) {
            String msgPattern = "Can't generate metadata file(s): Failed to initialise Velocity engine: %s";
            getLogger().log(Level.SEVERE, String.format(msgPattern, e.getMessage()), e);
            return;
        }

        VelocityContext vc = new VelocityContext(metadataProperties);

        vc.put("operator", this);
        vc.put("targetProduct", targetProduct);
        vc.put("metadataProperties", metadataProperties);

        for (File file : files) {
            processMetadataTemplate(file, ve, vc);
        }
    }

    private void processMetadataTemplate(File templateFile, VelocityEngine ve, VelocityContext vc) {
        String templateName = templateFile.getName();
        String outputName = templateName.substring(0, templateName.lastIndexOf('.'));
        try {
            getLogger().info(String.format("Writing metadata file '%s'...", outputName));
            try (Writer writer = new FileWriter(outputName)) {
                ve.mergeTemplate(templateName, RuntimeConstants.ENCODING_DEFAULT, vc, writer);
            }
        } catch (Exception e) {
            String msgPattern = "Failed to generate metadata file from template '%s': %s";
            getLogger().log(Level.SEVERE, String.format(msgPattern, templateName, e.getMessage()), e);
        }

    }

    private void initMetadataProperties() {
        final SimpleDateFormat dateFormat = new SimpleDateFormat(DATETIME_PATTERN, Locale.ENGLISH);

        File outputFile = new File(formatterConfig.getOutputFile());
        OperatorDescriptor operatorDescriptor = getSpi().getOperatorDescriptor();
        metadataProperties.put("product_name", FileUtils.getFilenameWithoutExtension(outputFile));
        metadataProperties.put("software_qualified_name", operatorDescriptor.getName());
        metadataProperties.put("software_name", operatorDescriptor.getAlias());
        metadataProperties.put("software_version", operatorDescriptor.getVersion());
        metadataProperties.put("processing_time", dateFormat.format(new Date()));
        metadataProperties.put("source_products", StringUtils.join(sourceProductNames, ","));

        if (metadataPropertiesFile != null) {
            if (!metadataPropertiesFile.exists()) {
                getLogger().warning(String.format("Metadata properties file '%s' not found", metadataPropertiesFile));
            } else {
                try {
                    getLogger().info(String.format("Reading metadata properties file '%s'...", metadataPropertiesFile));
                    try (FileReader reader = new FileReader(metadataPropertiesFile)) {
                        final Properties properties = new Properties();
                        properties.load(reader);
                        for (String name : properties.stringPropertyNames()) {
                            metadataProperties.put(name, properties.getProperty(name));
                        }
                    }
                } catch (IOException e) {
                    String msgPattern = "Failed to load metadata properties file '%s': %s";
                    getLogger().warning(String.format(msgPattern, metadataPropertiesFile, e.getMessage()));
                }
            }
        }
    }

    private static Product copyProduct(Product writtenProduct) {
        Product targetProduct = new Product(writtenProduct.getName(), writtenProduct.getProductType(),
                                            writtenProduct.getSceneRasterWidth(),
                                            writtenProduct.getSceneRasterHeight());
        targetProduct.setStartTime(writtenProduct.getStartTime());
        targetProduct.setEndTime(writtenProduct.getEndTime());
        ProductUtils.copyMetadata(writtenProduct, targetProduct);
        ProductUtils.copyGeoCoding(writtenProduct, targetProduct);
        ProductUtils.copyTiePointGrids(writtenProduct, targetProduct);
        ProductUtils.copyMasks(writtenProduct, targetProduct);
        ProductUtils.copyVectorData(writtenProduct, targetProduct);
        for (Band band : writtenProduct.getBands()) {
            // Force setting source image, otherwise GPF will set an OperatorImage and invoke computeTile()!!
            ProductUtils.copyBand(band.getName(), writtenProduct, targetProduct, true);
        }
        return targetProduct;
    }

    private SpatialBinCollection doSpatialBinning(BinningProductFilter productFilter) throws IOException {
        SpatialBinCollector spatialBinCollector = new GeneralSpatialBinCollector(binningContext.getPlanetaryGrid().getNumBins());
        final SpatialBinner spatialBinner = new SpatialBinner(binningContext, spatialBinCollector);
        if (sourceProducts != null) {
            for (Product sourceProduct : sourceProducts) {
                if (productFilter.accept(sourceProduct)) {
                    processSource(sourceProduct, spatialBinner);
                } else {
                    getLogger().warning("Filtered out product '" + sourceProduct.getFileLocation() + "'");
                    getLogger().warning("              reason: " + productFilter.getReason());
                }
            }
        }
        if (sourceProductPaths != null) {
            SortedSet<File> fileSet = new TreeSet<>();
            for (String filePattern : sourceProductPaths) {
                WildcardMatcher.glob(filePattern, fileSet);
            }
            if (fileSet.isEmpty()) {
                getLogger().warning("The given source file patterns did not match any files");
            }
            for (File file : fileSet) {
                Product sourceProduct;
                if (sourceProductFormat != null) {
                    sourceProduct = ProductIO.readProduct(file, sourceProductFormat);
                } else {
                    sourceProduct = ProductIO.readProduct(file);
                }
                if (sourceProduct != null) {
                    try {
                        if (productFilter.accept(sourceProduct)) {
                            processSource(sourceProduct, spatialBinner);
                        } else {
                            getLogger().warning("Filtered out product '" + sourceProduct.getFileLocation() + "'");
                            getLogger().warning("              reason: " + productFilter.getReason());
                        }
                    } finally {
                        sourceProduct.dispose();
                    }
                } else {
                    String msgPattern = "Failed to read file '%s' (not a data product or reader missing)";
                    getLogger().severe(String.format(msgPattern, file));
                }
            }
        }
        spatialBinCollector.consumingCompleted();
        return spatialBinCollector.getSpatialBinCollection();
    }


    private void processSource(Product sourceProduct, SpatialBinner spatialBinner) throws IOException {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        updateDateRangeUtc(sourceProduct);
        String productName = sourceProduct.getName();
        sourceProductNames.add(productName);
        getLogger().info(String.format("Spatial binning of product '%s'...", productName));
        getLogger().fine(String.format("Product start time: '%s'", sourceProduct.getStartTime()));
        getLogger().fine(String.format("Product end time:   '%s'", sourceProduct.getEndTime()));
        if (region != null) {
            SubsetOp subsetOp = new SubsetOp();
            subsetOp.setSourceProduct(sourceProduct);
            subsetOp.setGeoRegion(region);
            sourceProduct = subsetOp.getTargetProduct();
            // TODO mz/nf/mp 2013-11-06
            // TODO replace suvbset with rectangle as paramter to SpatialProductBinner
            // TODO grow rectangle by binSize in pixel units (see lc-tools solution and integrate here)
        }
        final long numObs = SpatialProductBinner.processProduct(sourceProduct, spatialBinner,
                                                                binningContext.getSuperSampling(), addedBands,
                                                                ProgressMonitor.NULL);
        stopWatch.stop();
        getLogger().info(String.format("Spatial binning of product '%s' done, %d observations seen, took %s",
                                       productName, numObs, stopWatch));

        if (region == null && regionArea != null) {
            for (GeneralPath generalPath : ProductUtils.createGeoBoundaryPaths(sourceProduct)) {
                regionArea.add(new Area(generalPath));
            }
        }

    }

    private TemporalBinList doTemporalBinning(SpatialBinCollection spatialBinMap) throws IOException {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        long numberOfBins = spatialBinMap.size();
        final TemporalBinner temporalBinner = new TemporalBinner(binningContext);
        final CellProcessorChain cellChain = new CellProcessorChain(binningContext);
        final TemporalBinList temporalBins = new TemporalBinList((int) numberOfBins);
        Iterable<List<SpatialBin>> spatialBinListCollection = spatialBinMap.getBinCollection();
        int binCounter = 0;
        int percentCounter = 0;
        long hundredthOfNumBins = numberOfBins / 100;
        for (List<SpatialBin> spatialBinList : spatialBinListCollection) {
            binCounter += spatialBinList.size();

            SpatialBin spatialBin = spatialBinList.get(0);
            long spatialBinIndex = spatialBin.getIndex();
            TemporalBin temporalBin = temporalBinner.processSpatialBins(spatialBinIndex, spatialBinList);

            temporalBin = temporalBinner.computeOutput(spatialBinIndex, temporalBin);
            temporalBin = cellChain.process(temporalBin);

            temporalBins.add(temporalBin);
            if (binCounter >= hundredthOfNumBins) {
                binCounter = 0;
                getLogger().info(String.format("Finished %d%% of temporal bins", ++percentCounter));
            }
        }
        stopWatch.stop();
        getLogger().info(String.format("Temporal binning of %d bins done, took %s", numberOfBins, stopWatch));

        return temporalBins;
    }

    private void writeOutput(List<TemporalBin> temporalBins, ProductData.UTC startTime, ProductData.UTC stopTime) throws
                                                                                                                  Exception {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        initMetadataProperties();

        if (outputBinnedData) {
            try {
                writeNetCDFBinFile(temporalBins, startTime, stopTime);
            } catch (Exception e) {
                getLogger().log(Level.SEVERE, String.format("Failed to write binned data: %s", e.getMessage()), e);
            }
        }

        if (outputTargetProduct) {
            getLogger().info(String.format("Writing mapped product '%s'...", formatterConfig.getOutputFile()));
            final MetadataElement globalAttributes = createGlobalAttributesElement();
            Formatter.format(binningContext.getPlanetaryGrid(),
                             getTemporalBinSource(temporalBins),
                             binningContext.getBinManager().getResultFeatureNames(),
                             formatterConfig,
                             region,
                             startTime,
                             stopTime,
                             globalAttributes);
            stopWatch.stop();

            String msgPattern = "Writing mapped product '%s' done, took %s";
            getLogger().info(String.format(msgPattern, formatterConfig.getOutputFile(), stopWatch));

            // TODO - Check efficiency of interface 'org.esa.beam.framework.gpf.experimental.Output'  (nf, 2012-03-02)
            // actually, the following line of code would be sufficient, but then, the
            // 'Output' interface implemented by this operator has no effect, because it already has a
            // 'ProductReader' instance set. The overall concept of 'Output' is not fully thought-out!
            //
            // this.targetProduct = readOutput();
            //
            // This is why I have to do the following

            final String outputType = formatterConfig.getOutputType();
            if (outputType.equalsIgnoreCase("Product")) {
                final File outputFile = new File(formatterConfig.getOutputFile());
                final String outputFormat = Formatter.getOutputFormat(formatterConfig, outputFile);

                writtenProduct = ProductIO.readProduct(outputFile, outputFormat);
                this.targetProduct = copyProduct(writtenProduct);
            } else {
                this.targetProduct = new Product("Dummy", "t", 10, 10);
            }
        } else {
            this.targetProduct = new Product("Dummy", "t", 10, 10);
        }
    }

    private MetadataElement createGlobalAttributesElement() {
        final MetadataElement globalAttributes = new MetadataElement("Global_Attributes");
        for (String name : metadataProperties.keySet()) {
            final String value = metadataProperties.get(name);
            globalAttributes.addAttribute(new MetadataAttribute(name, ProductData.createInstance(value), true));
        }
        return globalAttributes;
    }

    private TemporalBinSource getTemporalBinSource(List<TemporalBin> temporalBins) throws IOException {
        return new SimpleTemporalBinSource(temporalBins);
    }

    private void writeNetCDFBinFile(List<TemporalBin> temporalBins, ProductData.UTC startTime,
                                    ProductData.UTC stopTime) throws IOException {
        initBinWriter(startTime, stopTime);
        getLogger().info(String.format("Writing binned data to '%s'...", binWriter.getTargetFilePath()));
        binWriter.write(metadataProperties, temporalBins);
        getLogger().info(String.format("Writing binned data to '%s' done.", binWriter.getTargetFilePath()));

    }

    private void initBinWriter(ProductData.UTC startTime, ProductData.UTC stopTime) {
        if (binWriter == null) {
            binWriter = new SeaDASLevel3BinWriter(region,
                                                  startTime != null ? startTime : minDateUtc,
                                                  stopTime != null ? stopTime : maxDateUtc);
        }

        binWriter.setBinningContext(binningContext);
        binWriter.setTargetFileTemplatePath(formatterConfig.getOutputFile());
        binWriter.setLogger(getLogger());

    }

    private ProductData.UTC getStartDateUtc(String parameterName) throws OperatorException {
        if (!StringUtils.isNullOrEmpty(startDate)) {
            return parseDateUtc(parameterName, startDate);
        } else {
            return null;
        }
    }

    private ProductData.UTC getEndDateUtc(String parameterName) {
        if (!StringUtils.isNullOrEmpty(endDate)) {
            return parseDateUtc(parameterName, endDate);
        } else {
            return null;
        }
    }

    private void updateDateRangeUtc(Product sourceProduct) {
        if (sourceProduct.getStartTime() != null) {
            if (minDateUtc == null || sourceProduct.getStartTime().getAsDate().before(minDateUtc.getAsDate())) {
                minDateUtc = sourceProduct.getStartTime();
            }
        }
        if (sourceProduct.getEndTime() != null) {
            if (maxDateUtc == null || sourceProduct.getEndTime().getAsDate().after(maxDateUtc.getAsDate())) {
                maxDateUtc = sourceProduct.getStartTime();
            }
        }
    }

    // package access for tessting only tb 2013-05-07
    static ProductData.UTC parseDateUtc(String name, String date) {
        try {
            return ProductData.UTC.parse(date, DATE_PATTERN);
        } catch (ParseException e) {
            throw new OperatorException(String.format("Invalid parameter '%s': %s", name, e.getMessage()));
        }
    }

    private static class SimpleTemporalBinSource implements TemporalBinSource {

        private final List<TemporalBin> temporalBins;

        public SimpleTemporalBinSource(List<TemporalBin> temporalBins) {
            this.temporalBins = temporalBins;
        }

        @Override
        public int open() throws IOException {
            return 1;
        }

        @Override
        public Iterator<? extends TemporalBin> getPart(int index) throws IOException {
            return temporalBins.iterator();
        }

        @Override
        public void partProcessed(int index, Iterator<? extends TemporalBin> part) throws IOException {
        }

        @Override
        public void close() throws IOException {
        }
    }

    /**
     * The service provider interface (SPI) which is referenced
     * in {@code /META-INF/services/org.esa.beam.framework.gpf.OperatorSpi}.
     */
    public static class Spi extends OperatorSpi {

        public Spi() {
            super(BinningOp.class);
        }
    }

}
