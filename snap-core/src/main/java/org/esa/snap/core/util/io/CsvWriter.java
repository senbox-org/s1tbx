package org.esa.snap.core.util.io;

import java.io.IOException;
import java.io.Writer;

/**
 * @author Norman Fomferra
 * @see CsvReader
 */
public class CsvWriter extends Writer {

    private Writer out;
    private String separator;

    public CsvWriter(Writer out, String separator) {
        this.out = out;
        this.separator = separator;
    }

    public void writeRecord(String... values) throws IOException {
        for (int i = 0; i < values.length; i++) {
            String value = values[i];
            if (i > 0) {
                write(separator);
            }
            write(value);
        }
        write("\n");
    }

    public void writeRecord(double... values) throws IOException {
        for (int i = 0; i < values.length; i++) {
            double value = values[i];
            if (i > 0) {
                write(separator);
            }
            write(Double.toString(value));
        }
        write("\n");
    }


    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        out.write(cbuf, off, len);
    }

    @Override
    public void flush() throws IOException {
        out.flush();
    }

    @Override
    public void close() throws IOException {
        out.close();
    }
}
