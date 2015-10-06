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

package org.esa.snap.dataio.geotiff.internal;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.ProductData;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

public class TiffIFD_DataTypeTest {

    private static Band int8Band;
    private static Band uint8Band;
    private static Band int16Band;
    private static Band uint16Band;
    private static Band int32Band;
    private static Band uint32Band;
    private static Band float32Band;
    private static Band float64Band;

    @BeforeClass
    public static void setup() {
        int8Band = new Band("b1", ProductData.TYPE_INT8, 1, 1);
        uint8Band = new Band("b1", ProductData.TYPE_UINT8, 1, 1);
        int16Band = new Band("b1", ProductData.TYPE_INT16, 1, 1);
        uint16Band = new Band("b1", ProductData.TYPE_UINT16, 1, 1);
        int32Band = new Band("b1", ProductData.TYPE_INT32, 1, 1);
        uint32Band = new Band("b1", ProductData.TYPE_UINT32, 1, 1);
        float32Band = new Band("b1", ProductData.TYPE_FLOAT32, 1, 1);
        float64Band = new Band("b1", ProductData.TYPE_FLOAT64, 1, 1);
    }

    @Test
    public void testSingleDataType() {
        assertEquals(ProductData.TYPE_INT8, TiffIFD.getMaxElemSizeBandDataType(new Band[]{int8Band}));
        assertEquals(ProductData.TYPE_INT16, TiffIFD.getMaxElemSizeBandDataType(new Band[]{int16Band}));
        assertEquals(ProductData.TYPE_INT32, TiffIFD.getMaxElemSizeBandDataType(new Band[]{int32Band}));
        assertEquals(ProductData.TYPE_UINT8, TiffIFD.getMaxElemSizeBandDataType(new Band[]{uint8Band}));
        assertEquals(ProductData.TYPE_UINT16, TiffIFD.getMaxElemSizeBandDataType(new Band[]{uint16Band}));
        assertEquals(ProductData.TYPE_UINT32, TiffIFD.getMaxElemSizeBandDataType(new Band[]{uint32Band}));
        assertEquals(ProductData.TYPE_FLOAT32, TiffIFD.getMaxElemSizeBandDataType(new Band[]{float32Band}));
        assertEquals(ProductData.TYPE_FLOAT32, TiffIFD.getMaxElemSizeBandDataType(new Band[]{float64Band}));
    }


    @Test
    public void testGreatestSignedInt() {
        assertEquals(ProductData.TYPE_INT8,
                     TiffIFD.getMaxElemSizeBandDataType(new Band[]{int8Band, int8Band}));
        assertEquals(ProductData.TYPE_INT16,
                     TiffIFD.getMaxElemSizeBandDataType(new Band[]{int8Band, int16Band}));
        assertEquals(ProductData.TYPE_INT32,
                     TiffIFD.getMaxElemSizeBandDataType(new Band[]{int32Band, int8Band, int16Band}));
    }

    @Test
    public void testGreatestUnsignedInt() {
        assertEquals(ProductData.TYPE_UINT8,
                     TiffIFD.getMaxElemSizeBandDataType(new Band[]{uint8Band, uint8Band}));
        assertEquals(ProductData.TYPE_UINT16,
                     TiffIFD.getMaxElemSizeBandDataType(new Band[]{uint8Band, uint16Band}));
        assertEquals(ProductData.TYPE_UINT32,
                     TiffIFD.getMaxElemSizeBandDataType(new Band[]{uint32Band, uint8Band, uint16Band}));
    }

    @Test
    public void testReturnShort_If_Int8_and_Uint8() {
        assertEquals(ProductData.TYPE_INT16,
                     TiffIFD.getMaxElemSizeBandDataType(new Band[]{int8Band, uint8Band}));
    }

    @Test
    public void testReturnInt_If_Int16_and_Uint16() {
        assertEquals(ProductData.TYPE_INT32,
                     TiffIFD.getMaxElemSizeBandDataType(new Band[]{int16Band, uint16Band}));
    }

    @Test
    public void testReturnDouble_If_Int32_and_Uint32() {
        assertEquals(ProductData.TYPE_FLOAT32,
                     TiffIFD.getMaxElemSizeBandDataType(new Band[]{int32Band, uint32Band}));
    }

    @Test
    public void testGreatestFloat() {
        assertEquals(ProductData.TYPE_FLOAT32,
                     TiffIFD.getMaxElemSizeBandDataType(new Band[]{float64Band, float32Band}));
    }

    @Test
    public void testReturnFloat_If_Float32_And_any_int16_or_any_int8() {
        assertEquals(ProductData.TYPE_FLOAT32,
                     TiffIFD.getMaxElemSizeBandDataType(new Band[]{int8Band, float32Band}));
        assertEquals(ProductData.TYPE_FLOAT32,
                     TiffIFD.getMaxElemSizeBandDataType(new Band[]{uint8Band, float32Band}));
        assertEquals(ProductData.TYPE_FLOAT32,
                     TiffIFD.getMaxElemSizeBandDataType(new Band[]{int16Band, float32Band}));
        assertEquals(ProductData.TYPE_FLOAT32,
                     TiffIFD.getMaxElemSizeBandDataType(new Band[]{uint16Band, float32Band}));
        assertEquals(ProductData.TYPE_FLOAT32,
                     TiffIFD.getMaxElemSizeBandDataType(new Band[]{uint16Band, int16Band, float32Band}));
    }

    @Test
    public void testReturnDoubleIfFloat32TypeAndInt32() {
        assertEquals(ProductData.TYPE_FLOAT32,
                     TiffIFD.getMaxElemSizeBandDataType(new Band[]{int32Band, float32Band}));
        assertEquals(ProductData.TYPE_FLOAT32,
                     TiffIFD.getMaxElemSizeBandDataType(new Band[]{uint32Band, float32Band}));
    }

    @Test
    public void testReturnTheMaxSignedIntType_IfTheUnsignedTypesLessThanTheSignedType() {
        assertEquals(ProductData.TYPE_INT16,
                     TiffIFD.getMaxElemSizeBandDataType(new Band[]{int16Band, uint8Band}));
        assertEquals(ProductData.TYPE_INT32,
                     TiffIFD.getMaxElemSizeBandDataType(new Band[]{int32Band, uint8Band}));
        assertEquals(ProductData.TYPE_INT32,
                     TiffIFD.getMaxElemSizeBandDataType(new Band[]{int32Band, uint16Band}));
        assertEquals(ProductData.TYPE_INT32,
                     TiffIFD.getMaxElemSizeBandDataType(new Band[]{int32Band, uint16Band, uint8Band}));
    }


}
