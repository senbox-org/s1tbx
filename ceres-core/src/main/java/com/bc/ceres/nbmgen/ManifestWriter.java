package com.bc.ceres.nbmgen;

import java.io.FilterWriter;
import java.io.IOException;
import java.io.Writer;

/**
* @author Norman
*/
class ManifestWriter extends FilterWriter {
    private int col;

    ManifestWriter(Writer out) {
        super(out);
    }

    public void write(String key, String value) throws IOException {
        write(key);
        write(':');
        write(' ');
        write(value);
        write('\n');
    }

    @Override
    public void write(int c) throws IOException {
        if (col == 70 && c != '\n') {
            super.write('\n');
            super.write(' ');
            col = 1;
        }
        super.write(c);
        if (c == '\n') {
            col = 0;
        } else {
            col++;
        }
    }

    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        int n = Math.min(off + len, cbuf.length);
        for (int i = off; i < n; i++) {
            write(cbuf[i]);
        }
    }

    @Override
    public void write(String str, int off, int len) throws IOException {
        int n = Math.min(off + len, str.length());
        for (int i = off; i < n; i++) {
            write(str.charAt(i));
        }
    }
}
