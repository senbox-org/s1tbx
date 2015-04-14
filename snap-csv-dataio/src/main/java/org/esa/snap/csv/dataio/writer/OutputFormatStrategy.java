package org.esa.snap.csv.dataio.writer;

/**
 * Strategy that allows formatting csv contents.
 *
 * @author Thomas Storm
 */
public interface OutputFormatStrategy {

    String formatProperty(String key, String value);
    String formatHeader(String[] attributes, Class[] types);
    String formatRecord(String recordId, String[] values);

}
