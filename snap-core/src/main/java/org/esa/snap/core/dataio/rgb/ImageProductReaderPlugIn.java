/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.snap.core.dataio.rgb;

import org.esa.snap.core.dataio.DecodeQualification;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.core.util.io.FileUtils;
import org.esa.snap.core.util.io.SnapFileFilter;

import java.io.File;
import java.util.Locale;

/**
 * @author Norman Fomferra
 */
public class ImageProductReaderPlugIn implements ProductReaderPlugIn {

    public static final String FORMAT_NAME = "IMAGE";

    @Override
    public String[] getFormatNames() {
        return new String[]{FORMAT_NAME};
    }

    @Override
    public String[] getDefaultFileExtensions() {
        return new String[]{".png", ".gif", ".jpg", ".bmp", ".pnm", ".pbm", ".pgm", ".ppm"};
    }

    @Override
    public String getDescription(Locale name) {
        return "Image product reader";
    }

    @Override
    public DecodeQualification getDecodeQualification(Object object) {
        File file = getFile(object);
        if (file != null) {
            String fileExt = FileUtils.getExtension(file);
            if (fileExt != null && StringUtils.contains(getDefaultFileExtensions(), fileExt.toLowerCase())) {
                return DecodeQualification.SUITABLE;
            }
        }
        return DecodeQualification.UNABLE;
    }

    static File getFile(Object object) {
        File file = null;
        if (object instanceof String) {
            file = new File((String) object);
        } else if (object instanceof File) {
            file = (File) object;
        }
        return file;
    }

    @Override
    public Class[] getInputTypes() {
        return new Class[]{String.class, File.class};
    }

    @Override
    public ProductReader createReaderInstance() {
        return new ImageProductReader(this);
    }

    @Override
    public SnapFileFilter getProductFileFilter() {
        return new SnapFileFilter(FORMAT_NAME, getDefaultFileExtensions(), "Image files");
    }
}
