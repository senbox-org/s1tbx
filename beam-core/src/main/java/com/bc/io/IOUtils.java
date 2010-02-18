/*
 * $Id: IOUtils.java,v 1.1.1.1 2006/09/11 08:16:43 norman Exp $
 *
 * Copyright (c) 2003 Brockmann Consult GmbH. All right reserved.
 * http://www.brockmann-consult.de
 */
package com.bc.io;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * A utility class for I/O related functionality.
 * @author Norman Fomferra
 * @version
 */
public class IOUtils {

    /**
     * Reads bytes from the given input stream and writes them to the given output stream until
     * the end of the input stream is reached.
     *
     * @param is the input stream
     * @param os the output stream
     * @throws IOException if an I/O error occurs
     */
    public static void copyBytes(final InputStream is, final OutputStream os) throws IOException {
        while (true) {
            final int b = is.read();
            if (b == -1) {
                break;
            }
            os.write(b);
        }
    }

    /**
     * Calls {@link #copyBytes(java.io.InputStream, java.io.OutputStream)} and safely closes both streams.
     * @param is the input stream
     * @param os the output stream
     * @throws IOException if an I/O error occurs
     */
    public static void copyBytesAndClose(final InputStream is, final OutputStream os) throws IOException {
        try {
            copyBytes(is, os);
        } finally {
            try {
                os.close();
            } finally {
                is.close();
            }
        }
    }

    /**
     * Creates the specified directory. Does nothing if the directory already exists otherwise it is created.
     * If creation fails, an I/O exception is thrown.
     *
     * @param dir the directory to be created
     * @return <code>true</code> if it has been created, <code>false</code> if the directory already exists
     * @throws IOException if the directory could not be created
     */
    public static boolean createDir(final File dir) throws IOException {
        boolean created = dir.mkdirs();
        if (!dir.exists()) {
            throw new IOException("failed to create output directory " + dir);
        }
        return created;
    }
}
