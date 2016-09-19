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

package org.esa.snap.dataio.netcdf.util;

import org.esa.snap.core.datamodel.ProductData;
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
        if (DataType.BYTE.equals(dataType)) {
            return unsigned ? ProductData.TYPE_UINT8 : ProductData.TYPE_INT8;
        } else if (DataType.SHORT.equals(dataType)) {
            return unsigned ? ProductData.TYPE_UINT16 : ProductData.TYPE_INT16;
        } else if (DataType.INT.equals(dataType)) {
            return unsigned ? ProductData.TYPE_UINT32 : ProductData.TYPE_INT32;
        } else if (DataType.LONG.equals(dataType)) {
            return ProductData.TYPE_INT64;
        } else if (DataType.FLOAT.equals(dataType)) {
            return ProductData.TYPE_FLOAT32;
        } else if (DataType.DOUBLE.equals(dataType)) {
            return ProductData.TYPE_FLOAT64;
        } else if (!rasterDataOnly) {
            if (DataType.CHAR.equals(dataType)) {
                return ProductData.TYPE_ASCII;
            } else if (DataType.STRING.equals(dataType)) {
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
        switch (dataType) {
            case ProductData.TYPE_INT8:
            case ProductData.TYPE_UINT8:
                return DataType.BYTE;
            case ProductData.TYPE_INT16:
            case ProductData.TYPE_UINT16:
                return DataType.SHORT;
            case ProductData.TYPE_INT32:
            case ProductData.TYPE_UINT32:
                return DataType.INT;
            case ProductData.TYPE_INT64:
                return DataType.LONG;
            case ProductData.TYPE_FLOAT32:
                return DataType.FLOAT;
            case ProductData.TYPE_FLOAT64:
                return DataType.DOUBLE;
            case ProductData.TYPE_ASCII:
                return DataType.STRING;
            case ProductData.TYPE_UTC:
                return DataType.STRING;
            default:
                return null;
        }
    }

    /**
     * Converts the given double value to the Java type corresponding to the the given {@link DataType data type}.
     *
     * @param value    The value to be converted.
     * @param dataType The NetCDF data type.
     *
     * @return The converted value corresponding to the given {@link DataType data type}.
     *
     * @throws IllegalArgumentException if {@link DataType dataType} is not one of {@link DataType#BYTE},
     *                                  {@link DataType#SHORT}, {@link DataType#INT}, {@link DataType#LONG},
     *                                  {@link DataType#FLOAT} or {@link DataType#DOUBLE}.
     */
    public static Number convertTo(double value, DataType dataType) {
        switch (dataType) {
            case BYTE:
                return (byte) value;
            case SHORT:
                return (short) value;
            case INT:
                return (int) value;
            case LONG:
                return (long) value;
            case FLOAT:
                return (float) value;
            case DOUBLE:
                return value;
            default:
                throw new IllegalArgumentException("Can not convert data type:" + dataType.name());
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
