package org.esa.beam.dataio.bigtiff.internal;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TiffId_dataTypeTest {

    private Product product;

    @Before
    public void setUp() {
        product = new Product("bla", "bla", 3, 3);
    }

    @Test
    public void testGetBandDataType_int8() {
        product.addBand("signed", ProductData.TYPE_INT8);

        final TiffIFD tiffIFD = new TiffIFD(product);
        assertEquals(ProductData.TYPE_INT8, tiffIFD.getBandDataType());
    }

    @Test
    public void testGetBandDataType_uint8() {
        product.addBand("unsigned", ProductData.TYPE_UINT8);

        final TiffIFD tiffIFD = new TiffIFD(product);
        assertEquals(ProductData.TYPE_UINT8, tiffIFD.getBandDataType());
    }

    @Test
    public void testGetBandDataType_mixed_8bits() {
        product.addBand("signed", ProductData.TYPE_INT8);
        product.addBand("usigned", ProductData.TYPE_UINT8);

        final TiffIFD tiffIFD = new TiffIFD(product);
        assertEquals(ProductData.TYPE_INT16, tiffIFD.getBandDataType());
    }

    @Test
    public void testGetBandDataType_int16() {
        product.addBand("signed", ProductData.TYPE_INT16);

        final TiffIFD tiffIFD = new TiffIFD(product);
        assertEquals(ProductData.TYPE_INT16, tiffIFD.getBandDataType());
    }

    @Test
    public void testGetBandDataType_uint16() {
        product.addBand("unsigned", ProductData.TYPE_UINT16);

        final TiffIFD tiffIFD = new TiffIFD(product);
        assertEquals(ProductData.TYPE_UINT16, tiffIFD.getBandDataType());
    }

    @Test
    public void testGetBandDataType_mixed_16bits() {
        product.addBand("signed", ProductData.TYPE_INT16);
        product.addBand("unsigned", ProductData.TYPE_UINT16);

        final TiffIFD tiffIFD = new TiffIFD(product);
        assertEquals(ProductData.TYPE_INT32, tiffIFD.getBandDataType());
    }

    @Test
    public void testGetBandDataType_int32() {
        product.addBand("signed", ProductData.TYPE_INT32);

        final TiffIFD tiffIFD = new TiffIFD(product);
        assertEquals(ProductData.TYPE_INT32, tiffIFD.getBandDataType());
    }

    @Test
    public void testGetBandDataType_uint32() {
        product.addBand("unsigned", ProductData.TYPE_UINT32);

        final TiffIFD tiffIFD = new TiffIFD(product);
        assertEquals(ProductData.TYPE_UINT32, tiffIFD.getBandDataType());
    }

    @Test
    public void testGetBandDataType_mixed_32bits() {
        product.addBand("signed", ProductData.TYPE_INT32);
        product.addBand("unsigned", ProductData.TYPE_UINT32);

        final TiffIFD tiffIFD = new TiffIFD(product);
        assertEquals(ProductData.TYPE_FLOAT64, tiffIFD.getBandDataType());
    }

    @Test
    public void testGetBandDataType_float32() {
        product.addBand("single", ProductData.TYPE_FLOAT32);

        final TiffIFD tiffIFD = new TiffIFD(product);
        assertEquals(ProductData.TYPE_FLOAT32, tiffIFD.getBandDataType());
    }

    @Test
    public void testGetBandDataType_float64() {
        product.addBand("double", ProductData.TYPE_FLOAT64);

        final TiffIFD tiffIFD = new TiffIFD(product);
        assertEquals(ProductData.TYPE_FLOAT64, tiffIFD.getBandDataType());
    }

    @Test
    public void testGetBandDataType_mixed_floatTypes() {
        product.addBand("single", ProductData.TYPE_FLOAT32);
        product.addBand("double", ProductData.TYPE_FLOAT64);

        final TiffIFD tiffIFD = new TiffIFD(product);
        assertEquals(ProductData.TYPE_FLOAT64, tiffIFD.getBandDataType());
    }

    @Test
    public void testGetBandDataType_mixed_integerTypes() {
        product.addBand("one", ProductData.TYPE_UINT8);
        product.addBand("two", ProductData.TYPE_INT32);
        product.addBand("double", ProductData.TYPE_INT16);
        product.addBand("three", ProductData.TYPE_INT16);
        product.addBand("four", ProductData.TYPE_UINT16);

        final TiffIFD tiffIFD = new TiffIFD(product);
        assertEquals(ProductData.TYPE_INT32, tiffIFD.getBandDataType());
    }

    @Test
    public void testGetBandDataType_mixed_integerAndFloatTypes() {
        product.addBand("one", ProductData.TYPE_UINT8);
        product.addBand("two", ProductData.TYPE_FLOAT32);
        product.addBand("double", ProductData.TYPE_INT16);
        product.addBand("three", ProductData.TYPE_INT32);
        product.addBand("four", ProductData.TYPE_UINT16);

        final TiffIFD tiffIFD = new TiffIFD(product);
        assertEquals(ProductData.TYPE_FLOAT64, tiffIFD.getBandDataType());
    }
}
