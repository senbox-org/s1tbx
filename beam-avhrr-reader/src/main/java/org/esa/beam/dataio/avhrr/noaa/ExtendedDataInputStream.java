/*
 * $Id: ExtendedDataInputStream.java,v 1.1 2006/09/12 11:42:42 marcop Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.dataio.avhrr.noaa;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * A <code>DataInputStream</code> that can also read strings with a well known
 * length from an underlying stream of bytes.
 */
class ExtendedDataInputStream extends DataInputStream {
    /**
     * Creates a ExtendedDataInputStream that uses the specified
     * underlying InputStream.
     *
     * @param in the specified input stream
     */
    public ExtendedDataInputStream(InputStream in) {
        super(in);
    }

    /**
     * Reads a String of  the iven length in bytes from the inputStream. The returtned string is trimmed.
     *
     * @param length of the string
     * @return a string
     * @throws IOException
     */
    public String readString(final int length) throws IOException {
        byte[] byteArray = new byte[length];
        String resultString;
        in.read(byteArray, 0, length);
        resultString = new String(byteArray);
        return resultString.trim();
    }
}
