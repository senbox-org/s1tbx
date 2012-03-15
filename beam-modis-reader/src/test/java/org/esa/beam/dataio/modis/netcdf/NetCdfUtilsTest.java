package org.esa.beam.dataio.modis.netcdf;

import junit.framework.TestCase;
import org.esa.beam.framework.datamodel.ProductData;
import ucar.ma2.DataType;

public class NetCdfUtilsTest extends TestCase {

    public void testDecodeDataType() {
        assertEquals(ProductData.TYPE_UNDEFINED, NetCdfUtils.decodeDataType(DataType.BOOLEAN));
        assertEquals(ProductData.TYPE_INT8, NetCdfUtils.decodeDataType(DataType.BYTE));
        assertEquals(ProductData.TYPE_INT8, NetCdfUtils.decodeDataType(DataType.CHAR));
        assertEquals(ProductData.TYPE_FLOAT64, NetCdfUtils.decodeDataType(DataType.DOUBLE));
        assertEquals(ProductData.TYPE_FLOAT32, NetCdfUtils.decodeDataType(DataType.FLOAT));
        assertEquals(ProductData.TYPE_INT32, NetCdfUtils.decodeDataType(DataType.INT));
        assertEquals(ProductData.TYPE_UNDEFINED, NetCdfUtils.decodeDataType(DataType.LONG));
        assertEquals(ProductData.TYPE_INT16, NetCdfUtils.decodeDataType(DataType.SHORT));
        assertEquals(ProductData.TYPE_ASCII, NetCdfUtils.decodeDataType(DataType.STRING));
    }
}
