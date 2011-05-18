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

package org.esa.beam.dataio.netcdf.util;

import org.esa.beam.framework.datamodel.ProductData;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
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

    /**
     * Return the NetCDF equivalent to the given dataType.
     *
     * @param dataType must be one of {@code ProductData.TYPE_*}
     *
     * @return the NetCDF equivalent to the given dataType or {@code null} if not {@code dataType} is
     *         not one of {@code ProductData.TYPE_*}
     *
     * @see ProductData
     */
    public static DataType getNetcdfDataType(int dataType) {
        if (dataType == ProductData.TYPE_INT8 || dataType == ProductData.TYPE_UINT8) {
            return DataType.BYTE;
        } else if (dataType == ProductData.TYPE_INT16 || dataType == ProductData.TYPE_UINT16) {
            return DataType.SHORT;
        } else if (dataType == ProductData.TYPE_INT32 || dataType == ProductData.TYPE_UINT32) {
            return DataType.INT;
        } else if (dataType == ProductData.TYPE_FLOAT32) {
            return DataType.FLOAT;
        } else if (dataType == ProductData.TYPE_FLOAT64) {
            return DataType.DOUBLE;
        } else if (dataType == ProductData.TYPE_ASCII) {
            return DataType.STRING;
        } else if (dataType == ProductData.TYPE_UTC) {
            return DataType.STRING;
        } else {
            return null;
        }
    }

    /**
     * Creates a ProductData instance for the given netcdf attribute.
     *
     * @param attribute A netcdf attribute.
     *
     * @return A new ProductData instance with the attribute's data type and value.
     */
    public static ProductData createProductData(Attribute attribute) {
        ProductData attributeValue;
        int productDataType = DataTypeUtils.getEquivalentProductDataType(attribute.getDataType(), false, false);
        if (productDataType == ProductData.TYPE_ASCII) {
            attributeValue = ProductData.createInstance(attribute.getStringValue());
        } else {
            attributeValue = ProductData.createInstance(productDataType, attribute.getValues().copyTo1DJavaArray());
        }
        return attributeValue;
    }

}
