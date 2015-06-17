/*
 * Copyright (C) 2015 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.s1tbx.io.ceos.alos;

import org.esa.s1tbx.io.ceos.CEOSProductReaderPlugIn;
import org.esa.snap.framework.dataio.DecodeQualification;
import org.esa.snap.framework.dataio.ProductReader;

import java.io.File;

/**
 * The ReaderPlugIn for ALOS PALSAR CEOS products.
 */
public class AlosPalsarProductReaderPlugIn extends CEOSProductReaderPlugIn {

    public AlosPalsarProductReaderPlugIn() {
        constants = new AlosPalsarConstants();
    }

    /**
     * Creates an instance of the actual product reader class. This method should never return <code>null</code>.
     *
     * @return a new reader instance, never <code>null</code>
     */
    @Override
    public ProductReader createReaderInstance() {
        return new AlosPalsarProductReader(this);
    }

    @Override
    protected DecodeQualification checkProductQualification(File file) {
        final String name = file.getName().toUpperCase();
        for (String prefix : constants.getVolumeFilePrefix()) {
            if (name.startsWith(prefix) && !name.contains("ALOS2")) {
                final AlosPalsarProductReader reader = new AlosPalsarProductReader(this);
                return reader.checkProductQualification(file);
            }
        }
        return DecodeQualification.UNABLE;
    }

}
