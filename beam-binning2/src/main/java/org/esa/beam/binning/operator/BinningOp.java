/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.esa.beam.binning.BinningContext;
import org.esa.beam.binning.SpatialBin;
import org.esa.beam.binning.SpatialBinConsumer;
import org.esa.beam.binning.SpatialBinner;
import org.esa.beam.binning.TemporalBin;
import org.esa.beam.binning.TemporalBinSource;
import org.esa.beam.binning.TemporalBinner;
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
import org.esa.beam.framework.gpf.experimental.Output;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.util.StopWatch;
import org.esa.beam.util.StringUtils;
import org.esa.beam.util.converters.JtsGeometryConverter;
import org.esa.beam.util.io.FileUtils;
import org.esa.beam.util.io.WildcardMatcher;
import ucar.ma2.InvalidRangeException;

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
(2) We shall not only rely on the @SourceProducts annotation, but also use an input directory which we scan by
    globbing (using filename wildcards). This is important for windows users because the DOS shell does not allow
    for argument expansion using wildcard. PixExOp follows a similar approach but used a weird pattern. (check!)
(3) For simplicity, we shall not use BinningConfig and FormatterConfig but simply move their @Parameter declarations
    into the BinningOp class.
(4) It shall be possible to output both or either one, a mapped product file AND/OR the SeaDAS-like binned data
    file (SeaDAS).
(5) For dealing with really large amounts of bins (global binning), we need SpatialBinConsumer and TemporalBinSource
    implementations that write to and read from local files. (E.g. use memory-mapped file I/O, see
    MappedByteBufferTest.java)
(6) If the 'region' parameter is not given, the geographical extend of the mapped product shall be limited to the one
    given by the all the participating bin cells. This is in line with the case where the parameters 'startDate' and
    'endDate' are omitted: The actual start and end dates are computed from the source products.
(7) For simplicity, we shall introduce a Boolean parameter 'global'. If it is true, 'region' will be ignored.

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
@OperatorMetadata(alias = "Binning",
                  version = "0.8.0",
                  authors = "Norman Fomferra, Marco Zühlke, Thomas Storm",
                  copyright = "(c) 2012 by Brockmann Consult GmbH",
                  description = "Performs spatial and temporal aggregation of pixel values into 'bin' cells")
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

    @Parameter(converter = JtsGeometryConverter.class,
               description = "The considered geographical region as a geometry in well-known text format (WKT).\n" +
                       "If not given, the entire Globe is assumed.")
    Geometry region;

    @Parameter(description = "The start date. If not given, taken from the 'oldest' source product.",
               format = DATE_PATTERN)
    String startDate;

    @Parameter(description = "The end date. If not given, taken from the 'youngest' source product.",
               format = DATE_PATTERN)
    String endDate;

    @Parameter(description = "If true, a SeaDAS-style, binned data NetCDF file is written in addition to the\n" +
            "target product. The output file name will be <target>-bins.nc", defaultValue = "true")
    boolean outputBinnedData;

    @Parameter(notNull = true,
               description = "The configuration used for the binning process. Specifies the binning grid, any variables and their aggregators.")
    BinningConfig binningConfig;

    @Parameter(notNull = true,
               description = "The configuration used for the output formatting process.")
    FormatterConfig formatterConfig;

    @Parameter(description = "The name of the file containing metadata key-value pairs (google \"Java Properties file format\").", defaultValue = "./metadata.properties")
    File metadataPropertiesFile;

    @Parameter(description = "The name of the directory containing metadata templates (google \"Apache Velocity VTL format\").", defaultValue = ".")
    File metadataTemplateDir;

    private transient BinningContext binningContext;
    private transient final SpatialBinStore spatialBinStore;
    private transient int sourceProductCount;
    private transient ProductData.UTC minDateUtc;
    private transient ProductData.UTC maxDateUtc;
    private transient SortedMap<String, String> metadataProperties;

    public BinningOp() {
        this(new SpatialBinStoreImpl());
    }

    private BinningOp(SpatialBinStore spatialBinStore) {
        this.spatialBinStore = spatialBinStore;
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

    /**
     * Processes all source products and writes the output file.
     * The target product represents the written output file
     *
     * @throws org.esa.beam.framework.gpf.OperatorException
     *          If a processing error occurs.
     */
    @Override
    public void initialize() throws OperatorException {
        ProductData.UTC startDateUtc = getStartDateUtc("startDate");
        ProductData.UTC endDateUtc = getEndDateUtc("endDate");

        if (startDateUtc != null && endDateUtc != null && endDateUtc.getAsDate().before(startDateUtc.getAsDate())) {
            throw new OperatorException("End date '" + this.endDate + "' before start date '" + this.startDate + "'");
        }
        if (sourceProducts == null && (sourceProductPaths == null || sourceProductPaths.length == 0)) {
            throw new OperatorException("Either source products must be given or parameter 'sourceProductPaths' must be specified");
        }
        if (binningConfig == null) {
            throw new OperatorException("Missing operator parameter 'binningConfig'");
        }
        if (binningConfig.getMaskExpr() == null) {
            throw new OperatorException("Missing operator parameter 'binningConfig.maskExpr'");
        }
        if (binningConfig.getNumRows() <= 2) {
            throw new OperatorException("Operator parameter 'binningConfig.numRows' must be greater than 2");
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
            throw new OperatorException("Directory given by 'metadataTemplateDir' does not exist: " + metadataTemplateDir);
        }

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        binningContext = binningConfig.createBinningContext();
        metadataProperties = new TreeMap<String, String>();
        sourceProductCount = 0;

        try {
            // Step 1: Spatial binning - creates time-series of spatial bins for each bin ID ordered by ID. The tree map structure is <ID, time-series>
            SortedMap<Long, List<SpatialBin>> spatialBinMap = doSpatialBinning();
            if (!spatialBinMap.isEmpty()) {
                // Step 2: Temporal binning - creates a list of temporal bins, sorted by bin ID
                List<TemporalBin> temporalBins = doTemporalBinning(spatialBinMap);
                // Step 3: Formatting
                writeOutput(temporalBins, startDateUtc, endDateUtc);
                // TODO - Check efficiency of interface 'org.esa.beam.framework.gpf.experimental.Output'  (nf, 2012-03-02)
                // actually, the following line of code would be sufficient, but then, the
                // 'Output' interface implemented by this operator has no effect, because it already has a
                // 'ProductReader' instance set. The overall concept of 'Output' is not fully thought-out!
                //
                // this.targetProduct = readOutput();
                //
                // This is why I have to do the following
                Product writtenProduct = readOutput();
                this.targetProduct = copyProduct(writtenProduct);
            } else {
                getLogger().warning("No bins have been generated, no output has been written");
            }
        } catch (OperatorException e) {
            throw e;
        } catch (Exception e) {
            throw new OperatorException(e);
        }

        stopWatch.stopAndTrace(String.format("Total time for binning %d product(s)", sourceProductCount));

        processMetadataTemplates();
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
            getLogger().log(Level.SEVERE, String.format("Can't generate metadata file(s): Failed to initialise Velocity engine: %s", e.getMessage()), e);
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
            Writer writer = new FileWriter(outputName);
            try {
                ve.mergeTemplate(templateName, vc, writer);
            } finally {
                writer.close();
            }
        } catch (Exception e) {
            getLogger().log(Level.SEVERE,
                            String.format("Failed to generate metadata file from template '%s': %s", templateName, e.getMessage()),
                            e);
        }

    }

    private void initMetadataProperties() {
        final SimpleDateFormat dateFormat = new SimpleDateFormat(DATETIME_PATTERN, Locale.ENGLISH);

        metadataProperties.put("product_name", FileUtils.getFilenameWithoutExtension(new File(formatterConfig.getOutputFile())));
        metadataProperties.put("software_qualified_name", getSpi().getOperatorClass().getName());
        metadataProperties.put("software_name", getSpi().getOperatorClass().getAnnotation(OperatorMetadata.class).alias());
        metadataProperties.put("software_version", getSpi().getOperatorClass().getAnnotation(OperatorMetadata.class).version());
        metadataProperties.put("processing_time", dateFormat.format(new Date()));

        if (metadataPropertiesFile != null) {
            if (!metadataPropertiesFile.exists()) {
                getLogger().warning(String.format("Metadata properties file '%s' not found", metadataPropertiesFile));
            } else {
                try {
                    getLogger().info(String.format("Reading metadata properties file '%s'...", metadataPropertiesFile));
                    final FileReader reader = new FileReader(metadataPropertiesFile);
                    try {
                        final Properties properties = new Properties();
                        properties.load(reader);
                        for (String name : properties.stringPropertyNames()) {
                            metadataProperties.put(name, properties.getProperty(name));
                        }
                    } finally {
                        reader.close();
                    }
                } catch (IOException e) {
                    getLogger().warning(String.format("Failed to load metadata properties file '%s': %s", metadataPropertiesFile, e.getMessage()));
                }
            }
        }
    }

    private static Product copyProduct(Product writtenProduct) {
        Product targetProduct = new Product(writtenProduct.getName(), writtenProduct.getProductType(), writtenProduct.getSceneRasterWidth(), writtenProduct.getSceneRasterHeight());
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

    private Product readOutput() throws IOException {
        return ProductIO.readProduct(new File(formatterConfig.getOutputFile()));
    }

    private SortedMap<Long, List<SpatialBin>> doSpatialBinning() throws IOException {
        final SpatialBinner spatialBinner = new SpatialBinner(binningContext, spatialBinStore);
        if (sourceProducts != null) {
            for (Product sourceProduct : sourceProducts) {
                processSource(sourceProduct, spatialBinner);
            }
        }
        if (sourceProductPaths != null) {
            SortedSet<File> fileSet = new TreeSet<File>();
            for (String filePattern : sourceProductPaths) {
                WildcardMatcher.glob(filePattern, fileSet);
            }
            if (fileSet.isEmpty()) {
                getLogger().warning("The given source file patterns did not match any files");
            }
            for (File file : fileSet) {
                Product sourceProduct = ProductIO.readProduct(file);
                if (sourceProduct != null) {
                    try {
                        processSource(sourceProduct, spatialBinner);
                    } finally {
                        sourceProduct.dispose();
                    }
                } else {
                    getLogger().severe(String.format("Failed to read file '%s' (not a data product or reader missing)", file));
                }
            }
        }
        spatialBinStore.consumingCompleted();
        return spatialBinStore.getSpatialBinMap();
    }


    private void processSource(Product sourceProduct, SpatialBinner spatialBinner) throws IOException {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        updateDateRangeUtc(sourceProduct);
        getLogger().info(String.format("Spatial binning of product '%s'...", sourceProduct.getName()));
        final long numObs = SpatialProductBinner.processProduct(sourceProduct, spatialBinner, binningContext.getSuperSampling(), ProgressMonitor.NULL);
        stopWatch.stop();
        getLogger().info(String.format("Spatial binning of product '%s' done, %d observations seen, took %s", sourceProduct.getName(), numObs, stopWatch));
        sourceProductCount++;
    }

    private List<TemporalBin> doTemporalBinning(SortedMap<Long, List<SpatialBin>> spatialBinMap) throws IOException {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        getLogger().info(String.format("Spatial binning of %d bins", spatialBinMap.size()));
        final TemporalBinner temporalBinner = new TemporalBinner(binningContext);
        final ArrayList<TemporalBin> temporalBins = new ArrayList<TemporalBin>();
        for (Map.Entry<Long, List<SpatialBin>> entry : spatialBinMap.entrySet()) {
            final TemporalBin temporalBin = temporalBinner.processSpatialBins(entry.getKey(), entry.getValue());
            temporalBins.add(temporalBin);
        }
        stopWatch.stop();
        getLogger().info(String.format("Spatial binning of %d bins done, took %s", spatialBinMap.size(), stopWatch));

        return temporalBins;
    }

    private void writeOutput(List<TemporalBin> temporalBins, ProductData.UTC startTime, ProductData.UTC stopTime) throws Exception {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        initMetadataProperties();

        if (outputBinnedData) {
            File binnedDataFile = FileUtils.exchangeExtension(new File(formatterConfig.getOutputFile()), "-bins.nc");
            try {
                getLogger().info(String.format("Writing binned data to '%s'...", binnedDataFile));
                writeNetcdfBinFile(binnedDataFile,
                                   temporalBins, startTime, stopTime);
                getLogger().info(String.format("Writing binned data to '%s' done.", binnedDataFile));
            } catch (Exception e) {
                getLogger().log(Level.SEVERE, String.format("Failed to write binned data to '%s': %s", binnedDataFile, e.getMessage()), e);
            }
        }


        getLogger().info(String.format("Writing mapped product '%s'...", formatterConfig.getOutputFile()));
        final MetadataElement globalAttributes = createGlobalAttributesElement();
        Formatter.format(binningContext,
                         getTemporalBinSource(temporalBins),
                         formatterConfig,
                         region,
                         startTime,
                         stopTime,
                         globalAttributes);
        stopWatch.stop();

        getLogger().info(String.format("Writing mapped product '%s' done, took %s", formatterConfig.getOutputFile(), stopWatch));
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

    private void writeNetcdfBinFile(File file, List<TemporalBin> temporalBins, ProductData.UTC startTime, ProductData.UTC stopTime) throws IOException {
        final BinWriter writer = new BinWriter(binningContext, getLogger(), region, startTime != null ? startTime : minDateUtc, stopTime != null ? stopTime : maxDateUtc);
        try {
            writer.write(file, metadataProperties, temporalBins);
        } catch (InvalidRangeException e) {
            throw new IllegalArgumentException(e);
        }
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
            if (minDateUtc == null
                    || sourceProduct.getStartTime().getAsDate().before(minDateUtc.getAsDate())) {
                minDateUtc = sourceProduct.getStartTime();
            }
        }
        if (sourceProduct.getEndTime() != null) {
            if (maxDateUtc == null
                    || sourceProduct.getEndTime().getAsDate().after(maxDateUtc.getAsDate())) {
                maxDateUtc = sourceProduct.getStartTime();
            }
        }
    }

    private ProductData.UTC parseDateUtc(String name, String date) {
        try {
            return ProductData.UTC.parse(date, DATE_PATTERN);
        } catch (ParseException e) {
            throw new OperatorException(String.format("Invalid parameter '%s': %s", name, e.getMessage()));
        }
    }

    static interface SpatialBinStore extends SpatialBinConsumer {
        SortedMap<Long, List<SpatialBin>> getSpatialBinMap() throws IOException;

        void consumingCompleted() throws IOException;
    }

    private static class SpatialBinStoreImpl implements SpatialBinStore {
        // Note, we use a sorted map in order to sort entries on-the-fly
        final private SortedMap<Long, List<SpatialBin>> spatialBinMap = new TreeMap<Long, List<SpatialBin>>();

        @Override
        public SortedMap<Long, List<SpatialBin>> getSpatialBinMap() {
            return spatialBinMap;
        }

        @Override
        public void consumeSpatialBins(BinningContext binningContext, List<SpatialBin> spatialBins) {

            for (SpatialBin spatialBin : spatialBins) {
                List<SpatialBin> spatialBinList = spatialBinMap.get(spatialBin.getIndex());
                if (spatialBinList == null) {
                    spatialBinList = new ArrayList<SpatialBin>();
                    spatialBinMap.put(spatialBin.getIndex(), spatialBinList);
                }
                spatialBinList.add(spatialBin);
            }
        }

        @Override
        public void consumingCompleted() {
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
