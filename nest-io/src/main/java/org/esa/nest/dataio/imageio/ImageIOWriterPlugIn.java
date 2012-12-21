/*
 * Copyright (C) 2013 by Array Systems Computing Inc. http://www.array.ca
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.nest.dataio.imageio;

import org.esa.beam.framework.dataio.ProductWriter;
import org.esa.beam.framework.dataio.ProductWriterPlugIn;
import org.esa.beam.util.io.BeamFileFilter;

import javax.imageio.ImageIO;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;


public class ImageIOWriterPlugIn implements ProductWriterPlugIn {

    private static final String[] FORMAT_NAMES = { "JP2", "JPG", "PNG", "BMP", "GIF" };

    /**
     * Constructs a new product writer plug-in instance.
     */
    public ImageIOWriterPlugIn() {
    }

    public String[] getFormatNames() {
        return FORMAT_NAMES;
    }

    public String[] getDefaultFileExtensions() {
        final List<String> extList = new ArrayList<String>(20);
        extList.addAll(Arrays.asList(ImageIO.getWriterFileSuffixes()));

        final String[] exclude = { "pbm", "jpeg", "wbmp", "pgm", "ppm", "tiff", "gz"};
        for(String ext : exclude) {
            extList.remove(ext);    
        }

        return extList.toArray(new String[extList.size()]);
    }

    /**
     * Returns an array containing the classes that represent valid output types for this GDAL product writer.
     * <p/>
     * <p> Intances of the classes returned in this array are valid objects for the <code>writeProductNodes</code>
     * method of the <code>AbstractProductWriter</code> interface (the method will not throw an
     * <code>InvalidArgumentException</code> in this case).
     *
     * @return an array containing valid output types, never <code>null</code>
     *
     * @see org.esa.beam.framework.dataio.AbstractProductWriter#writeProductNodes
     */
    public Class[] getOutputTypes() {
        return new Class[]{
                String.class,
                File.class,
//            ImageOutputStream.class
        };
    }

    /**
     * Gets a short description of this plug-in. If the given locale is set to <code>null</code> the default locale is
     * used.
     * <p/>
     * <p> In a GUI, the description returned could be used as tool-tip text.
     *
     * @param name the local for the given decription string, if <code>null</code> the default locale is used
     *
     * @return a textual description of this product reader/writer
     */
    public String getDescription(Locale name) {
        return "ImageIO writer";
    }

    /**
     * Creates an instance of the actual product writer class.
     *
     * @return a new instance of the writer class
     */
    public ProductWriter createWriterInstance() {
        return new ImageIOWriter(this); 
    }

    public BeamFileFilter getProductFileFilter() {
        return new BeamFileFilter(getFormatNames()[0], getDefaultFileExtensions(), getDescription(null));
    }
}