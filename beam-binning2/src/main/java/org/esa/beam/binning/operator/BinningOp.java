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
import com.vividsolutions.jts.geom.GeometryFactory;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.esa.beam.binning.BinningContext;
import org.esa.beam.binning.SpatialBin;
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
import org.esa.beam.framework.datamodel.ProductFilter;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProducts;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.framework.gpf.experimental.Output;
import org.esa.beam.util.Debug;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.util.StopWatch;
import org.esa.beam.util.StringUtils;
import org.esa.beam.util.converters.JtsGeometryConverter;
import org.esa.beam.util.io.FileUtils;
import org.esa.beam.util.io.WildcardMatcher;
import org.geotools.geometry.jts.JTS;
import ucar.ma2.InvalidRangeException;

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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
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
                  version = "0.8.1",
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
                             "If not given, the geographical region will be computed according to the extents of the " +
                             "input products.")
    Geometry region;

    @Parameter(description =
                       "The start date. If not given, taken from the 'oldest' source product. Products that have " +
                       "a start date before the start date given by this parameter are not considered.",
               format = DATE_PATTERN)
    String startDate;

    @Parameter(description =
                       "The end date. If not given, taken from the 'youngest' source product. Products that have " +
                       "an end date after the end date given by this parameter are not considered.",
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

    @Parameter(
            description = "The name of the file containing metadata key-value pairs (google \"Java Properties file format\").",
            defaultValue = "./metadata.properties")
    File metadataPropertiesFile;

    @Parameter(
            description = "The name of the directory containing metadata templates (google \"Apache Velocity VTL format\").",
            defaultValue = ".")
    File metadataTemplateDir;

    private transient BinningContext binningContext;
    private transient int sourceProductCount;
    private transient ProductData.UTC minDateUtc;
    private transient ProductData.UTC maxDateUtc;
    private transient SortedMap<String, String> metadataProperties;

    private final Map<Product, List<Band>> addedBands;

    public BinningOp() throws OperatorException {
        addedBands = new HashMap<Product, List<Band>>();
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

        validateInput(startDateUtc, endDateUtc);

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        sourceProducts = filterSourceProducts(sourceProducts, startDateUtc, endDateUtc);

        if (region == null) {
            try {
                setRegionToProductsExtent();
            } catch (IOException e) {
                throw new OperatorException(e);
            }
        }

        binningContext = binningConfig.createBinningContext();
        metadataProperties = new TreeMap<String, String>();
        sourceProductCount = 0;

        try {
            // Step 1: Spatial binning - creates time-series of spatial bins for each bin ID ordered by ID. The tree map structure is <ID, time-series>
            SpatialBinCollection spatialBinMap = doSpatialBinning();
            if (!spatialBinMap.isEmpty()) {
                // Step 2: Temporal binning - creates a list of temporal bins, sorted by bin ID
                TemporalBinList temporalBins = doTemporalBinning(spatialBinMap);
                // Step 3: Formatting
                try {
                    writeOutput(temporalBins, startDateUtc, endDateUtc);
                } finally {
                    temporalBins.close();

                }
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
        } finally {
            cleanSourceProducts();
        }

        stopWatch.stopAndTrace(String.format("Total time for binning %d product(s)", sourceProductCount));

        processMetadataTemplates();
    }

    void setRegionToProductsExtent() throws IOException {
        Set<GeneralPath[]> extents = new HashSet<GeneralPath[]>();
        if (sourceProductPaths != null) {
            SortedSet<File> fileSet = new TreeSet<File>();
            for (String filePattern : sourceProductPaths) {
                WildcardMatcher.glob(filePattern, fileSet);
            }
            for (File file : fileSet) {
                Product sourceProduct = ProductIO.readProduct(file);
                if (sourceProduct != null) {
                    try {
                        extents.add(ProductUtils.createGeoBoundaryPaths(sourceProduct));
                    } finally {
                        sourceProduct.dispose();
                    }
                } else {
                    String msgPattern = "Failed to read file '%s' (not a data product or reader missing)";
                    getLogger().severe(String.format(msgPattern, file));
                }
            }
        }

        if (sourceProducts != null) {
            for (Product sourceProduct : sourceProducts) {
                extents.add(ProductUtils.createGeoBoundaryPaths(sourceProduct));
            }
        }

        Area area = new Area();
        for (GeneralPath[] extent : extents) {
            for (GeneralPath generalPath : extent) {
                area.add(new Area(generalPath));
            }
        }
        region = JTS.shapeToGeometry(area, new GeometryFactory());
    }

    private void validateInput(ProductData.UTC startDateUtc, ProductData.UTC endDateUtc) {
        if (startDateUtc != null && endDateUtc != null && endDateUtc.getAsDate().before(startDateUtc.getAsDate())) {
            throw new OperatorException("End date '" + this.endDate + "' before start date '" + this.startDate + "'");
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
    }

    static Product[] filterSourceProducts(Product[] sourceProducts, ProductData.UTC startTime,
                                          ProductData.UTC endTime) {
        if (sourceProducts == null) {
            return null;
        }
        if (startTime == null || endTime == null) {
            return sourceProducts;
        }

        final List<Product> acceptedProductList = new ArrayList<Product>();
        for (Product sourceProduct : sourceProducts) {
            final ProductFilter filter = new SourceProductFilter(startTime, endTime);
            if (filter.accept(sourceProduct)) {
                acceptedProductList.add(sourceProduct);
            } else {
                Debug.trace("Filtered out product '" + sourceProduct.getName() + "'.");
                sourceProduct.dispose();
            }
        }
        return acceptedProductList.toArray(new Product[acceptedProductList.size()]);
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
            Writer writer = new FileWriter(outputName);
            try {
                ve.mergeTemplate(templateName, RuntimeConstants.ENCODING_DEFAULT, vc, writer);
            } finally {
                writer.close();
            }
        } catch (Exception e) {
            String msgPattern = "Failed to generate metadata file from template '%s': %s";
            getLogger().log(Level.SEVERE, String.format(msgPattern, templateName, e.getMessage()), e);
        }

    }

    private void initMetadataProperties() {
        final SimpleDateFormat dateFormat = new SimpleDateFormat(DATETIME_PATTERN, Locale.ENGLISH);

        File outputFile = new File(formatterConfig.getOutputFile());
        Class<? extends Operator> operatorClass = getSpi().getOperatorClass();
        metadataProperties.put("product_name", FileUtils.getFilenameWithoutExtension(outputFile));
        metadataProperties.put("software_qualified_name", operatorClass.getName());
        metadataProperties.put("software_name", operatorClass.getAnnotation(OperatorMetadata.class).alias());
        metadataProperties.put("software_version", operatorClass.getAnnotation(OperatorMetadata.class).version());
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

    private Product readOutput() throws IOException {
        return ProductIO.readProduct(new File(formatterConfig.getOutputFile()));
    }

    private SpatialBinCollection doSpatialBinning() throws IOException {
        SpatialBinCollector spatialBinCollector =  new GeneralSpatialBinCollector(binningContext.getPlanetaryGrid().getNumBins());
        final SpatialBinner spatialBinner = new SpatialBinner(binningContext, spatialBinCollector);
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
        getLogger().info(String.format("Spatial binning of product '%s'...", sourceProduct.getName()));
        final long numObs = SpatialProductBinner.processProduct(sourceProduct, spatialBinner,
                                                                binningContext.getSuperSampling(), addedBands,
                                                                ProgressMonitor.NULL);
        stopWatch.stop();
        getLogger().info(String.format("Spatial binning of product '%s' done, %d observations seen, took %s",
                                       sourceProduct.getName(), numObs, stopWatch));
        sourceProductCount++;
    }

    private TemporalBinList doTemporalBinning(SpatialBinCollection spatialBinMap) throws IOException {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        long numberOfBins = spatialBinMap.size();
        final TemporalBinner temporalBinner = new TemporalBinner(binningContext);
        final TemporalBinList temporalBins = new TemporalBinList((int) spatialBinMap.size());
        Iterable<List<SpatialBin>> spatialBinListCollection = spatialBinMap.getBinCollection();
        for (List<SpatialBin> spatialBinList : spatialBinListCollection) {
            SpatialBin spatialBin = spatialBinList.get(0);
            long spatialBinIndex = spatialBin.getIndex();
            final TemporalBin temporalBin = temporalBinner.processSpatialBins(spatialBinIndex, spatialBinList);
            temporalBins.add(temporalBin);
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
            File binnedDataFile = FileUtils.exchangeExtension(new File(formatterConfig.getOutputFile()), "-bins.nc");
            try {
                getLogger().info(String.format("Writing binned data to '%s'...", binnedDataFile));
                writeNetCDFBinFile(binnedDataFile, temporalBins, startTime, stopTime);
                getLogger().info(String.format("Writing binned data to '%s' done.", binnedDataFile));
            } catch (Exception e) {
                getLogger().log(Level.SEVERE, String.format("Failed to write binned data to '%s': %s", binnedDataFile,
                                                            e.getMessage()), e);
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

        String msgPattern = "Writing mapped product '%s' done, took %s";
        getLogger().info(String.format(msgPattern, formatterConfig.getOutputFile(), stopWatch));
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

    private void writeNetCDFBinFile(File file, List<TemporalBin> temporalBins, ProductData.UTC startTime,
                                    ProductData.UTC stopTime) throws IOException {
        final BinWriter writer = new BinWriter(binningContext, getLogger(), region,
                                               startTime != null ? startTime : minDateUtc,
                                               stopTime != null ? stopTime : maxDateUtc);
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

    private ProductData.UTC parseDateUtc(String name, String date) {
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
