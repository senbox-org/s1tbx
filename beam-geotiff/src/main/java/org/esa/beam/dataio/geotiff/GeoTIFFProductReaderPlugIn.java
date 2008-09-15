/*
 * $Id$
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
package org.esa.beam.dataio.geotiff;

import com.sun.media.jai.codec.FileSeekableStream;
import com.sun.media.jai.codec.TIFFDecodeParam;
import com.sun.media.jai.codec.TIFFDirectory;
import com.sun.media.jai.codec.SeekableStream;
import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.util.io.BeamFileFilter;

import javax.media.jai.JAI;
import javax.media.jai.RenderedOp;
import java.awt.image.renderable.ParameterBlock;
import java.io.File;
import java.util.Locale;

public class GeoTIFFProductReaderPlugIn implements ProductReaderPlugIn {

    private static final String[] FORMAT_NAMES = new String[]{"GeoTIFF"};

    public DecodeQualification getDecodeQualification(Object input) {
        try {
            final File file = Utils.getFile(input);
            FileSeekableStream stream = new FileSeekableStream(file);
            try {
                return getDecodeQualification(stream);
            } finally {
                stream.close();
            }
        } catch (Exception ignore) {
            // nothing to do, return value is already UNABLE
        }

        return DecodeQualification.UNABLE;
    }

    static DecodeQualification getDecodeQualification(SeekableStream stream) {
        final ParameterBlock pb = new ParameterBlock();
        pb.add(stream);
        pb.add(new TIFFDecodeParam());

        try {
            final RenderedOp op = JAI.create("tiff", pb);

            final TIFFDirectory dir = (TIFFDirectory) op.getProperty("tiff_directory");
            final TIFFFileInfo info = new TIFFFileInfo(dir);

            if (info.isGeotiff()) {
                return DecodeQualification.INTENDED;
            }
        } catch (Exception ignore) {
            return DecodeQualification.UNABLE;
        }
        return DecodeQualification.SUITABLE;
    }

    public Class[] getInputTypes() {
        return new Class[]{String.class, File.class};
    }

    public ProductReader createReaderInstance() {
        return new GeoTIFFProductReader(this);
    }

    public String[] getFormatNames() {
        return FORMAT_NAMES;
    }

    public String[] getDefaultFileExtensions() {
        return new String[]{".tif", ".tiff"};
    }

    public String getDescription(Locale locale) {
        return "GeoTIFF data product.";
    }

    public BeamFileFilter getProductFileFilter() {
        return new BeamFileFilter(FORMAT_NAMES[0], getDefaultFileExtensions(), getDescription(null));
    }
}
