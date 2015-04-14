/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.dataio.netcdf.nc;

import edu.ucar.ral.nujan.netcdf.NhVariable;
import ucar.ma2.DataType;

/**
 * Methods for converting netCDf 3 with netCDF 4 data types
 */
public class N4DataType {

    static DataType convert(int nhType) {
        if (nhType == NhVariable.TP_SBYTE) {
            return DataType.BYTE;
        } else if (nhType == NhVariable.TP_UBYTE) {
            return DataType.BYTE;
        } else if (nhType == NhVariable.TP_SHORT) {
            return DataType.SHORT;
        } else if (nhType == NhVariable.TP_INT) {
            return DataType.INT;
        } else if (nhType == NhVariable.TP_LONG) {
            return DataType.LONG;
        } else if (nhType == NhVariable.TP_FLOAT) {
            return DataType.FLOAT;
        } else if (nhType == NhVariable.TP_DOUBLE) {
            return DataType.DOUBLE;
        } else if (nhType == NhVariable.TP_CHAR) {
            return DataType.CHAR;
        } else if (nhType == NhVariable.TP_STRING_VAR) {
            return DataType.STRING;
        } else {
            throw new IllegalArgumentException("Unsupported nhType: " + nhType);
        }
    }

    static int convert(DataType tp, boolean isUnsigned) {
        int nhType = 0;
        if (tp == DataType.BYTE) {
            if (isUnsigned) {
                nhType = NhVariable.TP_UBYTE;
            } else {
                nhType = NhVariable.TP_SBYTE;
            }
        } else if (tp == DataType.SHORT) {
            nhType = NhVariable.TP_SHORT;
        } else if (tp == DataType.INT) {
            nhType = NhVariable.TP_INT;
        } else if (tp == DataType.LONG) {
            nhType = NhVariable.TP_LONG;
        } else if (tp == DataType.FLOAT) {
            nhType = NhVariable.TP_FLOAT;
        } else if (tp == DataType.DOUBLE) {
            nhType = NhVariable.TP_DOUBLE;
        } else if (tp == DataType.CHAR) {
            nhType = NhVariable.TP_CHAR;
        } else if (tp == DataType.STRING) {
            nhType = NhVariable.TP_STRING_VAR;
        }
        return nhType;
    }
}
