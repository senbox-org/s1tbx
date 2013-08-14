package org.esa.beam.dataio;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
* @author Marco Peters
*/
class CustomLogFormatter extends Formatter {

    private final static String LINE_SEPARATOR = System.getProperty("line.separator", "\r\n");

    @Override
    public synchronized String format(LogRecord record) {
        StringBuilder sb = new StringBuilder();
        String message = formatMessage(record);
        sb.append(record.getLevel().getName());
        sb.append(": ");
        sb.append(message);
        sb.append(LINE_SEPARATOR);
        if (record.getThrown() != null) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            record.getThrown().printStackTrace(pw);
            pw.close();
            sb.append(sw.toString());
        }
        return sb.toString();
    }
}
