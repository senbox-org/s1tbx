/*
 * $Id: Landsat5CEOSImageSource.java,v 1.3 2007/04/05 07:07:16 marcop Exp $
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

package org.esa.beam.dataio.landsat.ceos;

import org.esa.beam.dataio.landsat.AbstractLandsatImageSources;
import org.esa.beam.dataio.landsat.LandsatTMFile;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.zip.ZipException;

/**
 * @author cberwang
 */
public final class Landsat5CEOSImageSource extends AbstractLandsatImageSources {

    /**
     * @throws ZipException
     * @throws IOException
     */
    public Landsat5CEOSImageSource(final LandsatTMFile file) throws
                                                             ZipException,
                                                             IOException {
        super(file);
        setImageLocations();
    }

    @Override
    protected final void setImageFiles() {
        final File folder = new File(file.getFileLocation());
        File[] files = folder.listFiles(new FileFilter() {
            Pattern bandFilenamePattern = Pattern.compile("dat_0[\\d].001");

            public boolean accept(File file) {
                if (file.isFile() && bandFilenamePattern.matcher(file.getName().toLowerCase()).matches()) {
                    return true;
                }
                return false;
            }
        });
        Arrays.sort(files);
        imageSources = files;
    }

    @Override
    protected final void setImageZipEntries() throws IOException {

    }
}
