package org.esa.beam.dataio.modis.netcdf;

import org.esa.beam.framework.datamodel.ProductData;
import ucar.ma2.DataType;

public class NetCdfUtils {
    public static int decodeDataType(DataType dataType) {
        switch (dataType) {
            case BYTE:
            case CHAR:
                return ProductData.TYPE_INT8;

            case SHORT:
                return ProductData.TYPE_INT16;

            case INT:
                return ProductData.TYPE_INT32;

            case FLOAT:
                return ProductData.TYPE_FLOAT32;

            case DOUBLE:
                return ProductData.TYPE_FLOAT64;

            case STRING:
                return ProductData.TYPE_ASCII;

            default:
                return ProductData.TYPE_UNDEFINED;
        }
    }
}
