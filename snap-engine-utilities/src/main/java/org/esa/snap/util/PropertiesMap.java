package org.esa.snap.util;

import org.esa.beam.util.PropertyMap;
import org.esa.beam.util.SystemUtils;

import java.io.File;
import java.util.regex.Matcher;

/**
 *
 */
public class PropertiesMap extends PropertyMap {

    public String getPropertyPath(final String key) {
        String val = getPropertyString(key);
        if (val != null) {
            if (val.contains("${")) {
                val = resolve(val);
            }

            val = val.replaceAll(Matcher.quoteReplacement("/"), Matcher.quoteReplacement(File.separator));
            val = val.replaceAll(Matcher.quoteReplacement("\\"), Matcher.quoteReplacement(File.separator));
        }
        return val;
    }

    /**
     * Gets a value of type <code>String</code>.
     *
     * @param key the key
     *
     * @return the value for the given key, or <code>""</code> (empty string) if the key is not contained in this
     *         property set, never <code>null</code>.
     */
    @Override
    public String getPropertyString(String key) {
        String val = getPropertyString(key, "");
        if (val != null && val.contains("${")) {
            val = resolve(val);
        }
        return val;
    }

    private String resolve(final String value) {
        final int idx1 = value.indexOf("${");
        final int idx2 = value.indexOf('}') + 1;
        final String keyWord = value.substring(idx1 + 2, idx2 - 1);
        final String fullKey = value.substring(idx1, idx2);

        String out;
        final String property = System.getProperty(keyWord);
        if (property != null && property.length() > 0) {
            out = value.replace(fullKey, property);
        } else {
            final String env = null; //System.getenv(keyWord);
            if (env != null && env.length() > 0) {
                out = value.replace(fullKey, env);
            } else {
                final String settingStr = getPropertyString(keyWord);
                if (settingStr != null && settingStr.length() > 0) {
                    out = value.replace(fullKey, settingStr);
                } else {
                    if (keyWord.equalsIgnoreCase("AuxDataPath")) {
                        File auxFolder = Settings.getAuxDataFolder();
                        out = value.replace(fullKey, auxFolder.getPath());
                    } else if (keyWord.equalsIgnoreCase(SystemUtils.getApplicationContextId() + ".home") || keyWord.equalsIgnoreCase("SNAP_HOME")) {
                        out = value.replace(fullKey, ResourceUtils.findHomeFolder().getAbsolutePath());
                    } else {
                        out = value.replace(fullKey, keyWord);
                    }
                }
            }
        }

        if (out.contains("${"))
            out = resolve(out);

        return out;
    }
}
