package org.esa.beam.binning.operator.metadata;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertySet;
import com.vividsolutions.jts.geom.Geometry;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.esa.beam.binning.AggregatorConfig;
import org.esa.beam.binning.operator.BinningConfig;
import org.esa.beam.binning.operator.BinningOp;
import org.esa.beam.binning.operator.VariableConfig;
import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.descriptor.OperatorDescriptor;
import org.esa.beam.util.StringUtils;
import org.esa.beam.util.io.FileUtils;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GlobalMetadata {

    private static final String DATETIME_OUTPUT_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    private static final SimpleDateFormat DATETIME_OUTPUT_FORMAT = new SimpleDateFormat(DATETIME_OUTPUT_PATTERN);
    static {
        DATETIME_OUTPUT_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    private static final double RE = 6378.145;

    private final SortedMap<String, String> metaProperties;

    public static GlobalMetadata create(BinningOp operator) {
        final GlobalMetadata globalMetadata = new GlobalMetadata();

        final OperatorSpi spi = operator.getSpi();
        if (spi != null) {
            globalMetadata.extractSpiMetadata(spi);
        }
        final BinningConfig config = operator.createConfig();
        globalMetadata.extractConfigMetadata(config);

        return globalMetadata;
    }

    public static GlobalMetadata create(BinningConfig config) {
        final GlobalMetadata globalMetadata = new GlobalMetadata();

        globalMetadata.extractConfigMetadata(config);

        return globalMetadata;
    }

    public void processMetadataTemplates(File metadataTemplateDir, BinningOp operator, Product targetProduct, Logger logger) {
        final File absTemplateDir = metadataTemplateDir.getAbsoluteFile();
        final File[] files = absTemplateDir.listFiles(new VelocityTemplateFilter());
        if (files == null || files.length == 0) {
            return;
        }

        final VelocityEngine ve = createVelocityEngine(absTemplateDir, logger);
        if (ve == null) {
            return;
        }

        final VelocityContext vc = new VelocityContext(metaProperties);
        vc.put("operator", operator);
        vc.put("targetProduct", targetProduct);
        vc.put("metadataProperties", metaProperties);

        for (File file : files) {
            processMetadataTemplate(file, ve, vc, logger);
        }
    }

    public SortedMap<String, String> asSortedMap() {
        return metaProperties;
    }

    public MetadataElement asMetadataElement() {
        final MetadataElement processingGraph = new MetadataElement("Processing_Graph");
        final MetadataElement node_0 = new MetadataElement("node.0");
        final MetadataElement parameters = new MetadataElement("parameters");
        for (final String name : metaProperties.keySet()) {
            final String value = metaProperties.get(name);
            parameters.addAttribute(new MetadataAttribute(name, ProductData.createInstance(value), true));
        }
        ProductData processingTime = ProductData.createInstance(DATETIME_OUTPUT_FORMAT.format(new Date()));
        node_0.addAttribute(new MetadataAttribute("processingTime", processingTime, false));
        node_0.addElement(parameters);
        processingGraph.addElement(node_0);
        return processingGraph;
    }

    public void load(File propertiesFile, Logger logger) {
        if (propertiesFile == null) {
            return;
        }
        if (!propertiesFile.isFile()) {
            logger.warning(String.format("Metadata properties file '%s' not found", propertiesFile));
            return;
        }

        logger.info(String.format("Reading metadata properties file '%s'...", propertiesFile));
        try (FileReader reader = new FileReader(propertiesFile)) {
            final Properties properties = new Properties();
            properties.load(reader);
            for (String name : properties.stringPropertyNames()) {
                metaProperties.put(name, properties.getProperty(name));
            }
        } catch (IOException e) {
            final String msgPattern = "Failed to load metadata properties file '%s': %s";
            logger.warning(String.format(msgPattern, propertiesFile, e.getMessage()));
        }
    }

    private static VelocityEngine createVelocityEngine(File absTemplateDir, Logger logger) {
        final Properties veConfig = new Properties();
        if (absTemplateDir.equals(new File(".").getAbsoluteFile())) {
            veConfig.setProperty("file.resource.loader.path", absTemplateDir.getPath());
        }

        final VelocityEngine ve = new VelocityEngine();
        try {
            ve.init(veConfig);
        } catch (Exception e) {
            final String msgPattern = "Can't generate metadata file(s): Failed to initialise Velocity engine: %s";
            logger.log(Level.SEVERE, String.format(msgPattern, e.getMessage()), e);
            return null;
        }
        return ve;
    }

    private static void processMetadataTemplate(File templateFile, VelocityEngine ve, VelocityContext vc, Logger logger) {
        final String templateName = templateFile.getName();
        final String outputName = templateName.substring(0, templateName.lastIndexOf('.'));
        logger.info(String.format("Writing metadata file '%s'...", outputName));

        try (Writer writer = new FileWriter(outputName)) {
            ve.mergeTemplate(templateName, RuntimeConstants.ENCODING_DEFAULT, vc, writer);
        } catch (Exception e) {
            final String msgPattern = "Failed to generate metadata file from template '%s': %s";
            logger.log(Level.SEVERE, String.format(msgPattern, templateName, e.getMessage()), e);
        }
    }

    private void extractConfigMetadata(BinningConfig config) {
        final String outputPath = config.getOutputFile();
        if (StringUtils.isNotNullAndNotEmpty(outputPath)) {
            final File outputFile = new File(outputPath);
            metaProperties.put("product_name", FileUtils.getFilenameWithoutExtension(outputFile.getName()));
        }

        // provisionally moved to node attributes as it is not a binning parameter, boe, 2014-11-04
        //final SimpleDateFormat dateFormat = new SimpleDateFormat(DATETIME_OUTPUT_PATTERN, Locale.ENGLISH);
        //metaProperties.put("processing_time", dateFormat.format(new Date()));

        final String startDateTime = config.getStartDateTime();
        if (StringUtils.isNotNullAndNotEmpty(startDateTime)) {
            metaProperties.put("aggregation_period_start", startDateTime);
        }

        final Double periodDuration = config.getPeriodDuration();
        if (periodDuration != null) {
            metaProperties.put("aggregation_period_duration", Double.toString(periodDuration) + " day(s)");
        }

        final Geometry region = config.getRegion();
        if (region != null) {
            metaProperties.put("region", region.toString());
        }

        final BinningOp.TimeFilterMethod timeFilterMethod = config.getTimeFilterMethod();
        if (isTimeFilterMetadataRequired(timeFilterMethod)) {
            metaProperties.put("time_filter_method", timeFilterMethod.toString());
            if (timeFilterMethod == BinningOp.TimeFilterMethod.SPATIOTEMPORAL_DATA_DAY) {
                final Double minDataHour = config.getMinDataHour();
                if (minDataHour != null) {
                    metaProperties.put("min_data_hour", Double.toString(minDataHour));
                }
            }
        }

        final int numRows = config.getNumRows();
        if (numRows > 0) {
            metaProperties.put("num_rows", Integer.toString(numRows));
            metaProperties.put("pixel_size_in_km", toPixelSizeString(numRows));
        }

        final Integer superSampling = config.getSuperSampling();
        if (superSampling != null) {
            metaProperties.put("super_sampling", Integer.toString(superSampling));
        }

        final String maskExpr = config.getMaskExpr();
        if (StringUtils.isNullOrEmpty(maskExpr)) {
            metaProperties.put("mask_expression", "");
        } else {
            metaProperties.put("mask_expression", maskExpr);
        }

        final VariableConfig[] variableConfigs = config.getVariableConfigs();
        if (variableConfigs != null) {
            int index = 0;
            for (final VariableConfig variableConfig : variableConfigs) {
                metaProperties.put("variable_config." + Integer.toString(index) + ":name", variableConfig.getName());
                metaProperties.put("variable_config." + Integer.toString(index) + ":expr", variableConfig.getExpr());
                ++index;
            }
        }

        final AggregatorConfig[] aggregatorConfigs = config.getAggregatorConfigs();
        if (aggregatorConfigs != null) {
            int index = 0;
            for (final AggregatorConfig aggregatorConfig : aggregatorConfigs) {
                final PropertySet propertySet = aggregatorConfig.asPropertySet();
                final Property[] properties = propertySet.getProperties();
                for (final Property property : properties) {
                    String value = property.getValueAsText();
                    if (StringUtils.isNotNullAndNotEmpty(value)) {
                        metaProperties.put("aggregator_config." + Integer.toString(index) + ":" + property.getName(), value);
                    }
                }
                ++index;
            }
        }

        final String aggregatorName = config.getMetadataAggregatorName();
        if (StringUtils.isNotNullAndNotEmpty(aggregatorName)) {
            metaProperties.put("metadata_aggregator_name", aggregatorName);
        }
    }

    static boolean isTimeFilterMetadataRequired(BinningOp.TimeFilterMethod timeFilterMethod) {
        return timeFilterMethod != null && timeFilterMethod != BinningOp.TimeFilterMethod.NONE;
    }

    static String toPixelSizeString(int numRows) {
        return Double.toString((RE * Math.PI) / (numRows - 1));
    }

    private void extractSpiMetadata(OperatorSpi spi) {
        final OperatorDescriptor descriptor = spi.getOperatorDescriptor();
        if (descriptor != null) {
            metaProperties.put("software_qualified_name", descriptor.getName());
            metaProperties.put("software_name", descriptor.getAlias());
            metaProperties.put("software_version", descriptor.getVersion());
        }
    }

    GlobalMetadata() {
        metaProperties = new TreeMap<>();
    }

    private static class VelocityTemplateFilter implements FilenameFilter {
        @Override
        public boolean accept(File dir, String name) {
            return name.endsWith(".vm");
        }
    }
}
