package org.esa.pfa.fe.op.out;

import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertySet;
import org.esa.beam.framework.datamodel.Product;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * A factory for PatchWriters.
 * @author Norman Fomferra
 */
public abstract class PatchWriterFactory {

    public static final String PROPERTY_TARGET_PATH = "targetPath";
    public static final String PROPERTY_OVERWRITE_MODE = "overwriteMode";
    public static final String PROPERTY_SKIP_FEATURE_OUTPUT = "skipFeatureOutput";
    public static final String PROPERTY_SKIP_PRODUCT_OUTPUT = "skipProductOutput";
    public static final String PROPERTY_SKIP_QUICKLOOK_OUTPUT = "skipQuicklookOutput";

    private PropertySet configuration;

    public void configure(Map<String, Object> properties) {
        this.configuration = PropertyContainer.createMapBacked(new HashMap<String, Object>(properties));
    }

    public PropertySet getConfiguration() {
        return configuration;
    }

    public String getTargetPath() {
        return getProperty(PROPERTY_TARGET_PATH, null);
    }

    public boolean isOverwriteMode() {
        return getProperty(PROPERTY_OVERWRITE_MODE, false);
    }

    public boolean getSkipFeatureOutput() {
        return getProperty(PROPERTY_SKIP_FEATURE_OUTPUT, false);
    }

    public boolean getSkipProductOutput() {
        return getProperty(PROPERTY_SKIP_PRODUCT_OUTPUT, false);
    }

    public boolean getSkipQuicklookOutput() {
        return getProperty(PROPERTY_SKIP_QUICKLOOK_OUTPUT, false);
    }

    protected String getProperty(String key, String defaultValue) {
        Object value = configuration.getValue(key);
        return value instanceof String ? (String) value
                : defaultValue;
    }

    protected boolean getProperty(String key, boolean defaultValue) {
        Object value = configuration.getValue(key);
        return value instanceof Boolean ? (Boolean) value
                : value instanceof String ? Boolean.parseBoolean((String) value)
                : defaultValue;
    }

    public abstract PatchWriter createFeatureOutput(Product sourceProduct) throws IOException;
}
