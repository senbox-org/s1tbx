/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.beam.dataio.arcbin;

import com.bc.ceres.binio.CompoundData;
import com.bc.ceres.binio.CompoundType;
import com.bc.ceres.binio.DataContext;
import com.bc.ceres.binio.DataFormat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteOrder;

import static com.bc.ceres.binio.TypeBuilder.*;

/**
 * Contains the Raster statistics
 */
class RasterStatistics {

    public static final String FILE_NAME = "sta.adf";

    private static final CompoundType TYPE =
            COMPOUND("RasterStatistics",
                     MEMBER("MIN", DOUBLE),
                     MEMBER("MAX", DOUBLE),
                     MEMBER("MEAN", DOUBLE),
                     MEMBER("STDDEV", DOUBLE)
            );

    final double min;
    final double max;
    final double mean;
    final double stddev;

    private RasterStatistics(double min, double max, double mean, double stddev) {
        this.min = min;
        this.max = max;
        this.mean = mean;
        this.stddev = stddev;
    }

    public static RasterStatistics create(File file) {
        DataFormat dataFormat = new DataFormat(TYPE, ByteOrder.BIG_ENDIAN);
        DataContext context;
        try {
            context = dataFormat.createContext(file, "r");
        } catch (FileNotFoundException ignored) {
            return null;
        }
        CompoundData data = context.createData();
        try {
            return new RasterStatistics(data.getDouble(0), data.getDouble(1),
                                                                     data.getDouble(2), data.getDouble(3));
        } catch (IOException ignore) {
            return null;
        } finally {
            context.dispose();
        }
    }
}
