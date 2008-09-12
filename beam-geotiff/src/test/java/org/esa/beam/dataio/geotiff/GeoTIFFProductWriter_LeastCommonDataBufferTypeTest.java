package org.esa.beam.dataio.geotiff;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.ProductData;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

import java.awt.image.DataBuffer;

public class GeoTIFFProductWriter_LeastCommonDataBufferTypeTest {

    private Band int8Band = new Band("b1", ProductData.TYPE_INT8, 1, 1);
    private Band uint8Band = new Band("b1", ProductData.TYPE_UINT8, 1, 1);
    private Band int16Band = new Band("b1", ProductData.TYPE_INT16, 1, 1);
    private Band uint16Band = new Band("b1", ProductData.TYPE_UINT16, 1, 1);
    private Band int32Band = new Band("b1", ProductData.TYPE_INT32, 1, 1);
    private Band uint32Band = new Band("b1", ProductData.TYPE_UINT32, 1, 1);
    private Band float32Band = new Band("b1", ProductData.TYPE_FLOAT32, 1, 1);
    private Band float64Band = new Band("b1", ProductData.TYPE_FLOAT64, 1, 1);

    @Test
    public void testSingleDataType() {
        assertEquals(DataBuffer.TYPE_BYTE, GeoTIFFProductWriter.getLeastCommonDataBufferType(new Band[]{int8Band}));
        assertEquals(DataBuffer.TYPE_SHORT, GeoTIFFProductWriter.getLeastCommonDataBufferType(new Band[]{int16Band}));
        assertEquals(DataBuffer.TYPE_INT, GeoTIFFProductWriter.getLeastCommonDataBufferType(new Band[]{int32Band}));
        assertEquals(DataBuffer.TYPE_BYTE, GeoTIFFProductWriter.getLeastCommonDataBufferType(new Band[]{uint8Band}));
        assertEquals(DataBuffer.TYPE_USHORT, GeoTIFFProductWriter.getLeastCommonDataBufferType(new Band[]{uint16Band}));
        assertEquals(DataBuffer.TYPE_INT, GeoTIFFProductWriter.getLeastCommonDataBufferType(new Band[]{uint32Band}));
        assertEquals(DataBuffer.TYPE_FLOAT, GeoTIFFProductWriter.getLeastCommonDataBufferType(new Band[]{float32Band}));
        assertEquals(DataBuffer.TYPE_DOUBLE, GeoTIFFProductWriter.getLeastCommonDataBufferType(new Band[]{float64Band}));
        assertEquals(DataBuffer.TYPE_UNDEFINED, GeoTIFFProductWriter.getLeastCommonDataBufferType(new Band[0]));
    }

    @Test
    public void testGreatestSignedInt() {
        assertEquals(DataBuffer.TYPE_BYTE,
                     GeoTIFFProductWriter.getLeastCommonDataBufferType(new Band[]{int8Band, int8Band}));
        assertEquals(DataBuffer.TYPE_SHORT,
                     GeoTIFFProductWriter.getLeastCommonDataBufferType(new Band[]{int8Band, int16Band}));
        assertEquals(DataBuffer.TYPE_INT,
                     GeoTIFFProductWriter.getLeastCommonDataBufferType(new Band[]{int32Band, int8Band, int16Band}));
    }

    @Test
    public void testGreatestUnsignedInt() {
        assertEquals(DataBuffer.TYPE_BYTE,
                     GeoTIFFProductWriter.getLeastCommonDataBufferType(new Band[]{uint8Band, uint8Band}));
        assertEquals(DataBuffer.TYPE_USHORT,
                     GeoTIFFProductWriter.getLeastCommonDataBufferType(new Band[]{uint8Band, uint16Band}));
        assertEquals(DataBuffer.TYPE_INT,
                     GeoTIFFProductWriter.getLeastCommonDataBufferType(new Band[]{uint32Band, uint8Band, uint16Band}));
    }

    @Test
    public void testReturnShort_If_Int8_and_Uint8() {
        assertEquals(DataBuffer.TYPE_SHORT,
                     GeoTIFFProductWriter.getLeastCommonDataBufferType(new Band[]{int8Band, uint8Band}));
    }

    @Test
    public void testReturnInt_If_Int16_and_Uint16() {
        assertEquals(DataBuffer.TYPE_INT,
                     GeoTIFFProductWriter.getLeastCommonDataBufferType(new Band[]{int16Band, uint16Band}));
    }

    @Test
    public void testReturnDouble_If_Int32_and_Uint32() {
        assertEquals(DataBuffer.TYPE_DOUBLE,
                     GeoTIFFProductWriter.getLeastCommonDataBufferType(new Band[]{int32Band, uint32Band}));
    }

    @Test
    public void testGreatestFloat() {
        assertEquals(DataBuffer.TYPE_DOUBLE,
                     GeoTIFFProductWriter.getLeastCommonDataBufferType(new Band[]{float64Band, float32Band}));
    }

    @Test
    public void testReturnFloat_If_Float32_And_any_int16_or_any_int8() {
        assertEquals(DataBuffer.TYPE_FLOAT,
                     GeoTIFFProductWriter.getLeastCommonDataBufferType(new Band[]{int8Band, float32Band}));
        assertEquals(DataBuffer.TYPE_FLOAT,
                     GeoTIFFProductWriter.getLeastCommonDataBufferType(new Band[]{uint8Band, float32Band}));
        assertEquals(DataBuffer.TYPE_FLOAT,
                     GeoTIFFProductWriter.getLeastCommonDataBufferType(new Band[]{int16Band, float32Band}));
        assertEquals(DataBuffer.TYPE_FLOAT,
                     GeoTIFFProductWriter.getLeastCommonDataBufferType(new Band[]{uint16Band, float32Band}));
        assertEquals(DataBuffer.TYPE_FLOAT,
                     GeoTIFFProductWriter.getLeastCommonDataBufferType(new Band[]{uint16Band, int16Band, float32Band}));
    }

    @Test
    public void testReturnDoubleIfFloat32TypeAndInt32() {
        assertEquals(DataBuffer.TYPE_DOUBLE,
                     GeoTIFFProductWriter.getLeastCommonDataBufferType(new Band[]{int32Band, float32Band}));
        assertEquals(DataBuffer.TYPE_DOUBLE,
                     GeoTIFFProductWriter.getLeastCommonDataBufferType(new Band[]{uint32Band, float32Band}));
    }

    @Test
    public void testReturnTheMaxSignedIntType_IfTheUnsignedTypesLessThanTheSignedType() {
        assertEquals(DataBuffer.TYPE_SHORT,
                     GeoTIFFProductWriter.getLeastCommonDataBufferType(new Band[]{int16Band, uint8Band}));
        assertEquals(DataBuffer.TYPE_INT,
                     GeoTIFFProductWriter.getLeastCommonDataBufferType(new Band[]{int32Band, uint8Band}));
        assertEquals(DataBuffer.TYPE_INT,
                     GeoTIFFProductWriter.getLeastCommonDataBufferType(new Band[]{int32Band, uint16Band}));
        assertEquals(DataBuffer.TYPE_INT,
                     GeoTIFFProductWriter.getLeastCommonDataBufferType(new Band[]{int32Band, uint16Band, uint8Band}));
    }
}
