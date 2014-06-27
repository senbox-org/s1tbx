package org.esa.beam.binning.operator.metadata;

import org.esa.beam.framework.gpf.descriptor.OperatorDescriptor;
import org.esa.beam.util.io.FileUtils;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Logger;

public class GlobalMetadata {

    private static final String DATETIME_OUTPUT_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS";

    private final SortedMap<String, String> metaProperties;

    public static GlobalMetadata create(OperatorDescriptor descriptor, File file) {
        return new GlobalMetadata(descriptor, file);
    }

    public SortedMap<String, String> asSortedMap() {
        return metaProperties;
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

    GlobalMetadata() {
        metaProperties = new TreeMap<>();
    }

    private GlobalMetadata(OperatorDescriptor descriptor, File file) {
        this();

        metaProperties.put("product_name", FileUtils.getFilenameWithoutExtension(file));
        metaProperties.put("software_qualified_name", descriptor.getName());
        metaProperties.put("software_name", descriptor.getAlias());
        metaProperties.put("software_version", descriptor.getVersion());
        final SimpleDateFormat dateFormat = new SimpleDateFormat(DATETIME_OUTPUT_PATTERN, Locale.ENGLISH);
        metaProperties.put("processing_time", dateFormat.format(new Date()));
    }
}
