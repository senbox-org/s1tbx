/*
 * $Id: LandsatImageInputStream.java,v 1.2 2007/02/09 09:35:18 marcop Exp $
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

package org.esa.beam.dataio.landsat;

import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * The class <code>LandsatImageInputStream</code> is used to store the inputstream of the Landsat TM data
 * Purpose: Other than the ImageInputstream the standard inputstream
 * and the zipinputstream doesn't save the length of the stream
 * in the object. The inputstream length helps to identify the header file.
 *
 * @author Christian Berwanger (ai0263@umwelt-campus.de)
 */

public final class LandsatImageInputStream {

    private ImageInputStream imageInputStream = null;
    private final long length;

    /**
     * @param stream
     * @param length
     */

    public LandsatImageInputStream(final ImageInputStream stream, final long length) {
        imageInputStream = stream;
        this.length = length;
    }

    /**
     * @param file
     *
     * @throws FileNotFoundException
     * @throws IOException
     */
    public LandsatImageInputStream(File file) throws
                                              IOException {
        imageInputStream = new FileImageInputStream(file);
        length = file.length();
    }

    /**
     * @return ImageInputStream inputStream
     */

    public final ImageInputStream getImageInputStream() {
        return imageInputStream;
    }

    /**
     * @return length of the ImageInputStream
     */
    public final long length() {
        return length;
    }

    /**
     * closes the imageInputStream
     *
     * @throws IOException
     */

    public final void close() throws
                              IOException {
        imageInputStream.close();
    }

}
