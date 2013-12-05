package org.esa.beam.framework.datamodel;/*
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

import com.bc.ceres.core.ProgressMonitor;

import java.io.IOException;

public class GeoCodingFactory {

    public static BasicPixelGeoCoding createPixelGeoCoding(final Band latBand,
                                                           final Band lonBand,
                                                           final String validMask,
                                                           final int searchRadius) {
        if ("true".equals(System.getProperty("beam.usePixelGeoCoding2"))) {
            return new PixelGeoCoding2(latBand, lonBand, validMask);
        } else {
            return new PixelGeoCoding(latBand, lonBand, validMask, searchRadius);
        }
    }

    public static BasicPixelGeoCoding createPixelGeoCoding(final Band latBand,
                                                           final Band lonBand,
                                                           final String validMask,
                                                           final int searchRadius,
                                                           ProgressMonitor pm) throws IOException {
        if ("true".equals(System.getProperty("beam.usePixelGeoCoding2"))) {
            return new PixelGeoCoding2(latBand, lonBand, validMask);
        } else {
            return new PixelGeoCoding(latBand, lonBand, validMask, searchRadius, pm);
        }
    }

}
