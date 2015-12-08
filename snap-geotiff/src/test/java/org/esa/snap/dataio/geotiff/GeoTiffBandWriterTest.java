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

package org.esa.snap.dataio.geotiff;

import com.bc.ceres.core.ProgressMonitor;
import junit.framework.TestCase;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.dataio.geotiff.internal.TiffDirectoryEntry;
import org.esa.snap.dataio.geotiff.internal.TiffHeader;
import org.esa.snap.dataio.geotiff.internal.TiffIFD;
import org.esa.snap.dataio.geotiff.internal.TiffLong;
import org.esa.snap.dataio.geotiff.internal.TiffTag;

import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.Rectangle;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * GeoTiffBandWriter Tester.
 *
 * @author <Authors name>
 * @version 1.0
 * @since <pre>02/11/2005</pre>
 */

public class GeoTiffBandWriterTest extends TestCase {

    private static final int _WIDTH = 20;
    private static final int _HEIGHT = 35;
    private MemoryCacheImageOutputStream _ios;
    private Product _product;

    @Override
    protected void setUp() throws Exception {
        _ios = new MemoryCacheImageOutputStream(new ByteArrayOutputStream());
        _product = new Product("name", "type", _WIDTH, _HEIGHT);

    }

    public void testCreation() {
        _product.addBand("b1", ProductData.TYPE_UINT32);
        final TiffIFD ifd = new TiffIFD(_product);

        new GeoTiffBandWriter(ifd, _ios, _product);
    }

    public void testWriteBandRasterData_3DifferentTypedBands() throws IOException {
        final int startValues[] = new int[]{0, 30, 100};
        _product.addBand("b1", ProductData.TYPE_UINT32);
        _product.addBand("b2", ProductData.TYPE_UINT16);
        _product.addBand("b3", ProductData.TYPE_UINT32);
        final ProductData data1 = createProductDataForBand(_product.getBand("b1"), startValues[0]);
        final ProductData data2 = createProductDataForBand(_product.getBand("b2"), startValues[1]);
        final ProductData data3 = createProductDataForBand(_product.getBand("b3"), startValues[2]);
        final TiffHeader tiffHeader = new TiffHeader(new Product[]{_product});
        final GeoTiffBandWriter bandWriter = new GeoTiffBandWriter(tiffHeader.getIfdAt(0), _ios, _product);

        tiffHeader.write(_ios);
        bandWriter.writeBandRasterData(_product.getBand("b1"), 0, 0, _WIDTH, _HEIGHT, data1, ProgressMonitor.NULL);
        bandWriter.writeBandRasterData(_product.getBand("b2"), 0, 0, _WIDTH, _HEIGHT, data2, ProgressMonitor.NULL);
        bandWriter.writeBandRasterData(_product.getBand("b3"), 0, 0, _WIDTH, _HEIGHT, data3, ProgressMonitor.NULL);

        final TiffIFD ifd = tiffHeader.getIfdAt(0);
        final int firstIFDOffset = 10;
        final long expSize = ifd.getRequiredEntireSize() + firstIFDOffset;
        final TiffIFD tiffIFD = tiffHeader.getIfdAt(0);
        final TiffDirectoryEntry entry = tiffIFD.getEntry(TiffTag.STRIP_OFFSETS);
        final TiffLong[] offsets = (TiffLong[]) entry.getValues();


        assertEquals(expSize, _ios.length());
        _ios.seek(offsets[0].getValue());
        for (int j = 0; j < _WIDTH * _HEIGHT; j++) {
            assertEquals("band at [0] - " + "index at [" + j + "]", startValues[0] + j, _ios.readUnsignedInt());
        }
        _ios.seek(offsets[1].getValue());
        for (int j = 0; j < _WIDTH * _HEIGHT; j++) {
            assertEquals("band at [1] - " + "index at [" + j + "]", startValues[1] + j, _ios.readUnsignedInt());
        }
        _ios.seek(offsets[2].getValue());
        for (int j = 0; j < _WIDTH * _HEIGHT; j++) {
            assertEquals("band at [2] - " + "index at [" + j + "]", startValues[2] + j, _ios.readUnsignedInt());
        }
    }

    public void testWriteBandRasterData_WithUINT8() throws IOException {
        _product.addBand("b1", ProductData.TYPE_UINT8);
        final int startValue = 1;
        final ProductData data = createProductDataForBand(_product.getBand("b1"), startValue);
        final TiffHeader tiffHeader = new TiffHeader(new Product[]{_product});
        final GeoTiffBandWriter bandWriter = new GeoTiffBandWriter(tiffHeader.getIfdAt(0), _ios, _product);
        tiffHeader.write(_ios);

        bandWriter.writeBandRasterData(_product.getBand("b1"), 0, 0, _WIDTH, _HEIGHT, data, ProgressMonitor.NULL);


        final TiffIFD ifd = tiffHeader.getIfdAt(0);
        final int firstIFDOffset = 10;
        final long expSize = ifd.getRequiredEntireSize() + firstIFDOffset;
        final TiffIFD tiffIFD = tiffHeader.getIfdAt(0);
        final TiffDirectoryEntry entry = tiffIFD.getEntry(TiffTag.STRIP_OFFSETS);
        final TiffLong[] stripOffsets = (TiffLong[]) entry.getValues();

        assertEquals(expSize, _ios.length());
        _ios.seek(stripOffsets[0].getValue());
        for (int j = 0; j < _WIDTH * _HEIGHT; j++) {
            final int expValue = (startValue + j) & 0x000000FF;
            assertEquals("index at [" + j + "]", expValue, _ios.readUnsignedByte());
        }
    }

    public void testWriteBandRasterData_WithUINT16() throws IOException {
        _product.addBand("b1", ProductData.TYPE_UINT16);
        final int startValue = 1;
        final ProductData data = createProductDataForBand(_product.getBand("b1"), startValue);
        final TiffHeader tiffHeader = new TiffHeader(new Product[]{_product});
        final GeoTiffBandWriter bandWriter = new GeoTiffBandWriter(tiffHeader.getIfdAt(0), _ios, _product);
        tiffHeader.write(_ios);

        bandWriter.writeBandRasterData(_product.getBand("b1"), 0, 0, _WIDTH, _HEIGHT, data, ProgressMonitor.NULL);


        final TiffIFD ifd = tiffHeader.getIfdAt(0);
        final int firstIFDOffset = 10;
        final long expSize = ifd.getRequiredEntireSize() + firstIFDOffset;
        final TiffIFD tiffIFD = tiffHeader.getIfdAt(0);
        final TiffDirectoryEntry entry = tiffIFD.getEntry(TiffTag.STRIP_OFFSETS);
        final TiffLong[] offsets = (TiffLong[]) entry.getValues();

        assertEquals(expSize, _ios.length());
        _ios.seek(offsets[0].getValue());
        for (int j = 0; j < _WIDTH * _HEIGHT; j++) {
            final int expValue = (startValue + j) & 0x0000FFFF;
            assertEquals("index at [" + j + "]", expValue, _ios.readUnsignedShort());
        }
    }

    public void testWriteBandRasterData_WithUINT32() throws IOException {
        _product.addBand("b1", ProductData.TYPE_UINT32);
        final int startValue = 1;
        final ProductData data = createProductDataForBand(_product.getBand("b1"), startValue);
        final TiffHeader tiffHeader = new TiffHeader(new Product[]{_product});
        final GeoTiffBandWriter bandWriter = new GeoTiffBandWriter(tiffHeader.getIfdAt(0), _ios, _product);
        tiffHeader.write(_ios);

        bandWriter.writeBandRasterData(_product.getBand("b1"), 0, 0, _WIDTH, _HEIGHT, data, ProgressMonitor.NULL);


        final TiffIFD ifd = tiffHeader.getIfdAt(0);
        final int firstIFDOffset = 10;
        final long expSize = ifd.getRequiredEntireSize() + firstIFDOffset;
        final TiffIFD tiffIFD = tiffHeader.getIfdAt(0);
        final TiffDirectoryEntry entry = tiffIFD.getEntry(TiffTag.STRIP_OFFSETS);
        final TiffLong[] offsets = (TiffLong[]) entry.getValues();

        assertEquals(expSize, _ios.length());
        _ios.seek(offsets[0].getValue());
        for (int j = 0; j < _WIDTH * _HEIGHT; j++) {
            final long expValue = (startValue + j);
            assertEquals("index at [" + j + "]", expValue, _ios.readUnsignedInt());
        }
    }

    public void testWriteBandRasterData_WithINT8() throws IOException {
        _product.addBand("b1", ProductData.TYPE_INT8);
        final int startValue = 1;
        final ProductData data = createProductDataForBand(_product.getBand("b1"), startValue);
        final TiffHeader tiffHeader = new TiffHeader(new Product[]{_product});
        final GeoTiffBandWriter bandWriter = new GeoTiffBandWriter(tiffHeader.getIfdAt(0), _ios, _product);
        tiffHeader.write(_ios);

        bandWriter.writeBandRasterData(_product.getBand("b1"), 0, 0, _WIDTH, _HEIGHT, data, ProgressMonitor.NULL);


        final TiffIFD ifd = tiffHeader.getIfdAt(0);
        final int firstIFDOffset = 10;
        final long expSize = ifd.getRequiredEntireSize() + firstIFDOffset;
        final TiffIFD tiffIFD = tiffHeader.getIfdAt(0);
        final TiffDirectoryEntry entry = tiffIFD.getEntry(TiffTag.STRIP_OFFSETS);
        final TiffLong[] offsets = (TiffLong[]) entry.getValues();

        assertEquals(expSize, _ios.length());
        _ios.seek(offsets[0].getValue());
        for (int j = 0; j < _WIDTH * _HEIGHT; j++) {
            final byte expValue = (byte) (startValue + j);
            assertEquals("index at [" + j + "]", expValue, _ios.readByte());
        }
    }

    public void testWriteBandRasterData_WithINT16() throws IOException {
        _product.addBand("b1", ProductData.TYPE_INT16);
        final int startValue = 1;
        final ProductData data = createProductDataForBand(_product.getBand("b1"), startValue);
        final TiffHeader tiffHeader = new TiffHeader(new Product[]{_product});
        final GeoTiffBandWriter bandWriter = new GeoTiffBandWriter(tiffHeader.getIfdAt(0), _ios, _product);
        tiffHeader.write(_ios);

        bandWriter.writeBandRasterData(_product.getBand("b1"), 0, 0, _WIDTH, _HEIGHT, data, ProgressMonitor.NULL);


        final TiffIFD ifd = tiffHeader.getIfdAt(0);
        final int firstIFDOffset = 10;
        final long expSize = ifd.getRequiredEntireSize() + firstIFDOffset;
        final TiffIFD tiffIFD = tiffHeader.getIfdAt(0);
        final TiffDirectoryEntry entry = tiffIFD.getEntry(TiffTag.STRIP_OFFSETS);
        final TiffLong[] offsets = (TiffLong[]) entry.getValues();

        assertEquals(expSize, _ios.length());
        _ios.seek(offsets[0].getValue());
        for (int j = 0; j < _WIDTH * _HEIGHT; j++) {
            final short expValue = (short) (startValue + j);
            assertEquals("index at [" + j + "]", expValue, _ios.readShort());
        }
    }

    public void testWriteBandRasterData_WithINT32() throws IOException {
        _product.addBand("b1", ProductData.TYPE_INT32);
        final int startValue = 1;
        final ProductData data = createProductDataForBand(_product.getBand("b1"), startValue);
        final TiffHeader tiffHeader = new TiffHeader(new Product[]{_product});
        final GeoTiffBandWriter bandWriter = new GeoTiffBandWriter(tiffHeader.getIfdAt(0), _ios, _product);
        tiffHeader.write(_ios);

        bandWriter.writeBandRasterData(_product.getBand("b1"), 0, 0, _WIDTH, _HEIGHT, data, ProgressMonitor.NULL);


        final TiffIFD ifd = tiffHeader.getIfdAt(0);
        final int firstIFDOffset = 10;
        final long expSize = ifd.getRequiredEntireSize() + firstIFDOffset;
        final TiffIFD tiffIFD = tiffHeader.getIfdAt(0);
        final TiffDirectoryEntry entry = tiffIFD.getEntry(TiffTag.STRIP_OFFSETS);
        final TiffLong[] offsets = (TiffLong[]) entry.getValues();

        assertEquals(expSize, _ios.length());
        _ios.seek(offsets[0].getValue());
        for (int j = 0; j < _WIDTH * _HEIGHT; j++) {
            final int expValue = startValue + j;
            assertEquals("index at [" + j + "]", expValue, _ios.readInt());
        }
    }

    public void testWriteBandRasterData_WithFLOAT32() throws IOException {
        _product.addBand("b1", ProductData.TYPE_FLOAT32);
        final float startValue = 1.5f;
        final ProductData data = createProductDataForBand(_product.getBand("b1"), startValue);
        final TiffHeader tiffHeader = new TiffHeader(new Product[]{_product});
        final GeoTiffBandWriter bandWriter = new GeoTiffBandWriter(tiffHeader.getIfdAt(0), _ios, _product);
        tiffHeader.write(_ios);

        bandWriter.writeBandRasterData(_product.getBand("b1"), 0, 0, _WIDTH, _HEIGHT, data, ProgressMonitor.NULL);


        final TiffIFD ifd = tiffHeader.getIfdAt(0);
        final int firstIFDOffset = 10;
        final long expSize = ifd.getRequiredEntireSize() + firstIFDOffset;
        final TiffIFD tiffIFD = tiffHeader.getIfdAt(0);
        final TiffDirectoryEntry entry = tiffIFD.getEntry(TiffTag.STRIP_OFFSETS);
        final TiffLong[] offsets = (TiffLong[]) entry.getValues();

        assertEquals(expSize, _ios.length());
        _ios.seek(offsets[0].getValue());
        for (int j = 0; j < _WIDTH * _HEIGHT; j++) {
            final float expValue = startValue + j;
            assertEquals("index at [" + j + "]", expValue, _ios.readFloat(), 1.0e-6);
        }
    }

    public void testWriteBandRasterData_WithFLOAT64() throws IOException {
        _product.addBand("b1", ProductData.TYPE_FLOAT64);
        final double startValue = 1.5f;
        final ProductData data = createProductDataForBand(_product.getBand("b1"), startValue);
        final TiffHeader tiffHeader = new TiffHeader(new Product[]{_product});
        final GeoTiffBandWriter bandWriter = new GeoTiffBandWriter(tiffHeader.getIfdAt(0), _ios, _product);
        tiffHeader.write(_ios);

        bandWriter.writeBandRasterData(_product.getBand("b1"), 0, 0, _WIDTH, _HEIGHT, data, ProgressMonitor.NULL);


        final TiffIFD ifd = tiffHeader.getIfdAt(0);
        final int firstIFDOffset = 10;
        final long expSize = ifd.getRequiredEntireSize() + firstIFDOffset;
        final TiffIFD tiffIFD = tiffHeader.getIfdAt(0);
        final TiffDirectoryEntry entry = tiffIFD.getEntry(TiffTag.STRIP_OFFSETS);
        final TiffLong[] offsets = (TiffLong[]) entry.getValues();

        assertEquals(expSize, _ios.length());
        _ios.seek(offsets[0].getValue());
        for (int j = 0; j < _WIDTH * _HEIGHT; j++) {
            final float expValue = (float) (startValue + j);
            assertEquals("index at [" + j + "]", expValue, _ios.readFloat(), 1.0e-6);
        }
    }

    public void testWriteBandRasterData_INT16() throws IOException {
        _product.addBand("b1", ProductData.TYPE_INT16);
        final float scalingFactor = 1.2f;
        _product.getBand("b1").setScalingFactor(scalingFactor);
        final int startValue = 1;
        final ProductData data = createProductDataForBand(_product.getBand("b1"), startValue);
        final TiffHeader tiffHeader = new TiffHeader(new Product[]{_product});
        final GeoTiffBandWriter bandWriter = new GeoTiffBandWriter(tiffHeader.getIfdAt(0), _ios, _product);
        tiffHeader.write(_ios);

        bandWriter.writeBandRasterData(_product.getBand("b1"), 0, 0, _WIDTH, _HEIGHT, data, ProgressMonitor.NULL);


        final TiffIFD ifd = tiffHeader.getIfdAt(0);
        final int firstIFDOffset = 10;
        final long expSize = ifd.getRequiredEntireSize() + firstIFDOffset;
        final TiffIFD tiffIFD = tiffHeader.getIfdAt(0);
        final TiffDirectoryEntry entry = tiffIFD.getEntry(TiffTag.STRIP_OFFSETS);
        final TiffLong[] offsets = (TiffLong[]) entry.getValues();

        assertEquals(expSize, _ios.length());
        _ios.seek(offsets[0].getValue());
        for (int j = 0; j < _WIDTH * _HEIGHT; j++) {
            assertEquals("index at [" + j + "]", startValue + j, _ios.readChar(), 1.0e-6);
        }
    }

    public void testWriteBandRasterData_WithOutHeader() throws IOException {
        final ProductData data = ProductData.createInstance(ProductData.TYPE_UINT32, _WIDTH * _HEIGHT);
        final Band band = new Band("b1", ProductData.TYPE_UINT32, _WIDTH, _HEIGHT);
        _product.addBand(band);
        final GeoTiffBandWriter bandWriter = new GeoTiffBandWriter(new TiffIFD(_product), _ios, _product);

        bandWriter.writeBandRasterData(band, 0, 0, _WIDTH, _HEIGHT, data, ProgressMonitor.NULL);

        final long expSize = _WIDTH * _HEIGHT * 4;
        assertEquals(expSize, _ios.length());
    }

    public void testWriteBandRasterData_WithBandNotInProduct() throws IOException {
        _product.addBand("b1", ProductData.TYPE_INT16);
        _product.addBand("b2", ProductData.TYPE_INT16);
        final int startValue = 1;
        final ProductData data = createProductDataForBand(_product.getBand("b1"), startValue);
        final TiffHeader tiffHeader = new TiffHeader(new Product[]{_product});
        final GeoTiffBandWriter bandWriter = new GeoTiffBandWriter(tiffHeader.getIfdAt(0), _ios, _product);
        tiffHeader.write(_ios);
        final Band bandNotInProd = new Band("notInProduct", ProductData.TYPE_INT16, 10, 15);
        final ProductData dataNotInProd = createProductDataForBand(bandNotInProd, startValue);

        bandWriter.writeBandRasterData(_product.getBand("b1"), 0, 0, _WIDTH, _HEIGHT, data, ProgressMonitor.NULL);
        try {
            bandWriter.writeBandRasterData(bandNotInProd, 0, 0, 10, 15, dataNotInProd, ProgressMonitor.NULL);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().indexOf("band") != -1);
        } catch (Exception notExpected) {
            fail("IllegalArgumentException expected, but was " + notExpected.getClass().getName());
        }
    }


    public void testWriteBandRasterData_InParts() throws IOException {
        final int startValue = 12;
        _product.addBand("b1", ProductData.TYPE_UINT32);
        createProductDataForBand(_product.getBand("b1"), startValue);
        //
        // Write sequence
        //      /-------------------\
        //      |   2     |     4   |
        //      |-------------------|
        //      |   1     |     3   |
        //      |-------------------|
        //      |         5         |
        //      \-------------------/
        final Rectangle region1 = new Rectangle(0, 15, 10, 10);
        final Rectangle region2 = new Rectangle(0, 0, 10, 15);
        final Rectangle region3 = new Rectangle(10, 15, 10, 10);
        final Rectangle region4 = new Rectangle(10, 0, 10, 15);
        final Rectangle region5 = new Rectangle(0, 25, 20, 10);
        final ProductData data1 = createProductDataPartAsUINT32(_product.getBand("b1"), region1);
        final ProductData data2 = createProductDataPartAsUINT32(_product.getBand("b1"), region2);
        final ProductData data3 = createProductDataPartAsUINT32(_product.getBand("b1"), region3);
        final ProductData data4 = createProductDataPartAsUINT32(_product.getBand("b1"), region4);
        final ProductData data5 = createProductDataPartAsUINT32(_product.getBand("b1"), region5);
        final TiffHeader tiffHeader = new TiffHeader(new Product[]{_product});
        final GeoTiffBandWriter bandWriter = new GeoTiffBandWriter(tiffHeader.getIfdAt(0), _ios, _product);

        tiffHeader.write(_ios);
        bandWriter.writeBandRasterData(_product.getBand("b1"), 0, 15, 10, 10, data1, ProgressMonitor.NULL);
        bandWriter.writeBandRasterData(_product.getBand("b1"), 0, 0, 10, 15, data2, ProgressMonitor.NULL);
        bandWriter.writeBandRasterData(_product.getBand("b1"), 10, 15, 10, 10, data3, ProgressMonitor.NULL);
        bandWriter.writeBandRasterData(_product.getBand("b1"), 10, 0, 10, 15, data4, ProgressMonitor.NULL);
        bandWriter.writeBandRasterData(_product.getBand("b1"), 0, 25, 20, 10, data5, ProgressMonitor.NULL);

        final TiffIFD ifd = tiffHeader.getIfdAt(0);
        final int firstIFDOffset = 10;
        final long expSize = ifd.getRequiredEntireSize() + firstIFDOffset;
        final TiffIFD tiffIFD = tiffHeader.getIfdAt(0);
        final TiffDirectoryEntry entry = tiffIFD.getEntry(TiffTag.STRIP_OFFSETS);
        final TiffLong[] offsets = (TiffLong[]) entry.getValues();


        assertEquals(expSize, _ios.length());
        _ios.seek(offsets[0].getValue());
        for (int j = 0; j < _WIDTH * _HEIGHT; j++) {
            assertEquals("index at [" + j + "]", startValue + j, _ios.readInt());
        }
    }

    public void testWriteBandRasterData_ThrowsIOExceptionWhenWriting() {
        final ProductData data = ProductData.createInstance(ProductData.TYPE_UINT32, _WIDTH * _HEIGHT);
        final Band band = new Band("b1", ProductData.TYPE_UINT32, _WIDTH, _HEIGHT);
        _product.addBand(band);
        final WriteIOExceptionImageOutputStream exceptionStream = new WriteIOExceptionImageOutputStream(
                new ByteArrayOutputStream());
        final GeoTiffBandWriter bandWriter = new GeoTiffBandWriter(new TiffIFD(_product), exceptionStream, _product);

        try {
            bandWriter.writeBandRasterData(band, 0, 0, _WIDTH, _HEIGHT, data, ProgressMonitor.NULL);
            fail("IOException expected");
        } catch (IOException expected) {
            assertEquals(expected.getMessage(), "IOException write");
        } catch (Exception notExpected) {
            fail("IOException expected, but was" + notExpected.getClass().getName());
        }
    }

    public void testWriteBandRasterData_ThrowsIOExceptionWhenSeeking() {
        final ProductData data = ProductData.createInstance(ProductData.TYPE_UINT32, _WIDTH * _HEIGHT);
        final Band band = new Band("b1", ProductData.TYPE_UINT32, _WIDTH, _HEIGHT);
        _product.addBand(band);
        final SeekIOExceptionImageOutputStream exceptionStream = new SeekIOExceptionImageOutputStream(
                new ByteArrayOutputStream());
        final GeoTiffBandWriter bandWriter = new GeoTiffBandWriter(new TiffIFD(_product), exceptionStream, _product);

        try {
            bandWriter.writeBandRasterData(band, 0, 0, _WIDTH, _HEIGHT, data, ProgressMonitor.NULL);
            fail("IOException expected");
        } catch (IOException expected) {
            assertEquals(expected.getMessage(), "IOException seek");
        } catch (Exception notExpected) {
            fail("IOException expected, but was" + notExpected.getClass().getName());
        }
    }

    public void testDispose() throws Exception {
        _product.addBand("b1", ProductData.TYPE_UINT16);
        final ProductData data = createProductDataForBand(_product.getBand("b1"), 0);
        final GeoTiffBandWriter bandWriter = new GeoTiffBandWriter(new TiffIFD(_product), _ios, _product);
        bandWriter.dispose();

        try {
            bandWriter.writeBandRasterData(_product.getBand("b1"), 0, 0, _WIDTH, _HEIGHT, data, ProgressMonitor.NULL);
            fail("NullPointerException expected");
        } catch (NullPointerException expected) {

        } catch (Exception notExpected) {
            fail("NullPointerException expected, but was " + notExpected.getClass().getName());
        }
    }

    private static ProductData createProductDataPartAsUINT32(final Band band, final Rectangle region) {
        int[] intData = null;
        intData = band.getPixels((int) region.getX(), (int) region.getY(),
                                 (int) region.getWidth(), (int) region.getHeight(),
                                 intData, ProgressMonitor.NULL);
        final ProductData data = ProductData.createInstance(ProductData.TYPE_UINT32, intData.length);
        data.setElems(intData);
        return data;
    }

    private static ProductData createProductDataForBand(final Band band, final int start) {
        final ProductData data = band.createCompatibleRasterData();
        for (int i = 0; i < band.getRasterWidth() * band.getRasterHeight(); i++) {
            data.setElemIntAt(i, start + i);
        }
        band.setData(data);
        return data;
    }

    private static ProductData createProductDataForBand(final Band band, final float start) {
        final ProductData data = band.createCompatibleRasterData();
        for (int i = 0; i < band.getRasterWidth() * band.getRasterHeight(); i++) {
            data.setElemFloatAt(i, start + i);
        }
        band.setData(data);
        return data;
    }

    private static ProductData createProductDataForBand(final Band band, final double start) {
        final ProductData data = band.createCompatibleRasterData();
        for (int i = 0; i < band.getRasterWidth() * band.getRasterHeight(); i++) {
            data.setElemDoubleAt(i, start + i);
        }
        band.setData(data);
        return data;
    }

    private static class WriteIOExceptionImageOutputStream extends MemoryCacheImageOutputStream {

        public WriteIOExceptionImageOutputStream(final OutputStream outStream) {
            super(outStream);
        }

        @Override
        public void write(final int b) throws IOException {
            throw new IOException("IOException write");
        }

        @Override
        public void write(final byte b[], final int off, final int len) throws IOException {
            throw new IOException("IOException write");
        }
    }

    private static class SeekIOExceptionImageOutputStream extends MemoryCacheImageOutputStream {

        public SeekIOExceptionImageOutputStream(final OutputStream outStream) {
            super(outStream);
        }

        @Override
        public void seek(final long pos) throws IOException {
            throw new IOException("IOException seek");
        }
    }
}
