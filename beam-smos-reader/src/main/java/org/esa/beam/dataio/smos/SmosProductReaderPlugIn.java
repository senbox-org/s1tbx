/*
 * $Id: $
 *
 * Copyright (C) 2008 by Brockmann Consult (info@brockmann-consult.de)
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
package org.esa.beam.dataio.smos;

import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.util.io.BeamFileFilter;
import org.esa.beam.util.io.FileUtils;

import java.io.File;
import java.util.Locale;


public class SmosProductReaderPlugIn implements ProductReaderPlugIn {

    @Override
    public ProductReader createReaderInstance() {
        return new SmosProductReader(this);
    }

    @Override
    public DecodeQualification getDecodeQualification(Object input) {
        final File file = input instanceof File ? (File) input : new File(input.toString());
        if (file.getName().endsWith(".DBL") || file.getName().endsWith(".HDR")) {
            boolean bothExist = FileUtils.exchangeExtension(file, ".HDR").exists()
                    && FileUtils.exchangeExtension(file, ".DBL").exists();
            if (!bothExist) {
                return DecodeQualification.UNABLE;
            }
            final String[] formatNames = getFormatNames();
            for (String formatName : formatNames) {
                if (file.getName().contains(formatName)) {
                    return DecodeQualification.INTENDED;
                }
            }
            return DecodeQualification.UNABLE;
        } else {
            return DecodeQualification.UNABLE;
        }
    }

    @Override
    public Class[] getInputTypes() {
        return new Class[]{File.class, String.class};
    }

    @Override
    public String[] getDefaultFileExtensions() {
        return new String[]{".HDR", ".DBL"};
    }

    @Override
    public String getDescription(Locale locale) {
        return "SMOS Data Products";
    }

    @Override
    public String[] getFormatNames() {
        return SmosFormats.getInstance().getFormatNames();
    }

    @Override
    public BeamFileFilter getProductFileFilter() {
        return new BeamFileFilter(getFormatNames()[0], getDefaultFileExtensions(), getDescription(null));
    }
}
