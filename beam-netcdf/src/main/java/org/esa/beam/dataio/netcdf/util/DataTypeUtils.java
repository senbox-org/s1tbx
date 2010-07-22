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

package org.esa.beam.dataio.netcdf.util;

import org.esa.beam.framework.datamodel.DataNode;
import org.esa.beam.framework.datamodel.ProductData;
import ucar.ma2.DataType;
import ucar.nc2.Variable;

/**
 * Contains utility methods dealing with netCDF DataType and BEAM ProductData types
 */
public class DataTypeUtils {

    public static boolean isValidRasterDataType(final DataType dataType) {
        final boolean unsigned = false;
        return getRasterDataType(dataType, unsigned) != -1;
    }

    public static int getRasterDataType(Variable variable) {
        return getRasterDataType(variable.getDataType(), variable.isUnsigned());    
    }

    public static int getRasterDataType(final DataType dataType, boolean unsigned) {
        final boolean rasterDataOnly = true;
        return getEquivalentProductDataType(dataType, unsigned, rasterDataOnly);
    }

    public static int getEquivalentProductDataType(DataType dataType, boolean unsigned, boolean rasterDataOnly) {
        if (dataType == DataType.BYTE) {
            return unsigned ? ProductData.TYPE_UINT8 : ProductData.TYPE_INT8;
        } else if (dataType == DataType.SHORT) {
            return unsigned ? ProductData.TYPE_UINT16 : ProductData.TYPE_INT16;
        } else if (dataType == DataType.INT) {
            return unsigned ? ProductData.TYPE_UINT32 : ProductData.TYPE_INT32;
        } else if (dataType == DataType.FLOAT) {
            return ProductData.TYPE_FLOAT32;
        } else if (dataType == DataType.DOUBLE) {
            return ProductData.TYPE_FLOAT64;
        } else if (!rasterDataOnly) {
            if (dataType == DataType.CHAR) {
                return ProductData.TYPE_ASCII;
            } else if (dataType == DataType.STRING) {
                return ProductData.TYPE_ASCII;
            }
        }
        return -1;
    }

    public static DataType getNetcdfDataType(DataNode dataNode) {
        final int nodeType = dataNode.getDataType();
        if (nodeType == ProductData.TYPE_INT8 || nodeType == ProductData.TYPE_UINT8) {
            return DataType.BYTE;
        } else if (nodeType == ProductData.TYPE_INT16 || nodeType == ProductData.TYPE_UINT16) {
            return DataType.SHORT;
        } else if (nodeType == ProductData.TYPE_INT32 || nodeType == ProductData.TYPE_UINT32) {
            return DataType.INT;
        } else if (nodeType == ProductData.TYPE_FLOAT32) {
            return DataType.FLOAT;
        } else if (nodeType == ProductData.TYPE_FLOAT64) {
            return DataType.DOUBLE;
        } else if (nodeType == ProductData.TYPE_ASCII) {
            return DataType.STRING;
        } else if (nodeType == ProductData.TYPE_UTC) {
            return DataType.STRING;
        } else {
            return null;
        }
    }
}
