package org.esa.beam.dataio.landsat;

import org.esa.beam.util.io.FileUtils;

import java.io.File;
import java.io.IOException;

/*
 * $Id: LandsatHeaderStream.java,v 1.2 2007/02/09 09:35:18 marcop Exp $
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

/**
 * The class <code>LandsatHeaderStream</code> is used to generate inputstreams of the header files
 *
 * @author Christian Berwanger (ai0263@umwelt-campus.de)
 */
public final class LandsatHeaderStream {

    private LandsatImageInputStream inputStream;
    private String headerFileName;

    /**
     * @param header
     *
     * @throws IOException
     */
    public LandsatHeaderStream(final File header) throws
                                                  IOException {
        final LandsatImageInputStream inputStream = new LandsatImageInputStream(header);
        initLandsatHeaderStream(inputStream, FileUtils.getFilenameWithoutExtension(header));
    }

    /**
     * @param inputStream
     * @param fileName
     */
    public LandsatHeaderStream(final LandsatImageInputStream inputStream, final String fileName) {
        initLandsatHeaderStream(inputStream, fileName);
    }

    private void initLandsatHeaderStream(final LandsatImageInputStream inputStream, final String fileName) {
        this.inputStream = inputStream;
        headerFileName = fileName;
    }

    /**
     * @return the name of header files
     */
    public final String getHeaderFileName() {
        return headerFileName;
    }

    /**
     * @return the input stream of the header file
     */
    public final LandsatImageInputStream getInputStream() {
        return inputStream;
    }

    public void close() {
        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (IOException e) {
                // nothing to do here
            }
        }
    }
}
