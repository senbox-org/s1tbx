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
package org.esa.snap.dataio.arcbin;

import com.bc.ceres.binio.CompoundData;
import com.bc.ceres.binio.CompoundType;
import com.bc.ceres.binio.DataContext;
import com.bc.ceres.binio.DataFormat;

import java.io.File;
import java.io.IOException;
import java.nio.ByteOrder;

import static com.bc.ceres.binio.TypeBuilder.*;


/**
 * Contains the Georef Bounds
 */
public class GeorefBounds {

    public static final String FILE_NAME = "dblbnd.adf";

    private static final CompoundType TYPE =
            COMPOUND("GeorefBounds",
                     MEMBER("D_LLX", DOUBLE),
                     MEMBER("D_LLY", DOUBLE),
                     MEMBER("D_URX", DOUBLE),
                     MEMBER("D_URY", DOUBLE)
            );

    public final double lowerLeftX;
    public final double lowerLeftY;
    public final double upperRightX;
    public final double upperRightY;

    private GeorefBounds(double lowerLeftX, double lowerLeftY, double upperRightX, double upperRightY) {
        this.lowerLeftX = lowerLeftX;
        this.lowerLeftY = lowerLeftY;
        this.upperRightX = upperRightX;
        this.upperRightY = upperRightY;
    }

    public static GeorefBounds create(File file) throws IOException {
        DataFormat dataFormat = new DataFormat(TYPE, ByteOrder.BIG_ENDIAN);
        DataContext context = dataFormat.createContext(file, "r");
        CompoundData data = context.createData();

        GeorefBounds georefBounds = new GeorefBounds(data.getDouble(0), data.getDouble(1),
                                                     data.getDouble(2), data.getDouble(3));
        context.dispose();
        return georefBounds;
    }
}
