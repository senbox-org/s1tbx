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


import junit.framework.TestCase;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.util.jai.JAIUtils;
import org.junit.Assert;

import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.Dimension;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;

public class TiffIFDTest extends TestCase {

    private Product _product;
    private static final int WIDTH = 10;
    private static final int HEIGHT = 20;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        _product = new Product("name", "type", WIDTH, HEIGHT);
    }

    public void testTiffIFDCreation_WithEmptyProduct() {
        try {
            new TiffIFD(_product);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException expected) {

        } catch (Exception e) {
            fail("IllegalArgumentException expected");
        }
    }

    public void testTiffIFDCreationMixedTypes() throws Exception {
        _product.addBand("b1", ProductData.TYPE_UINT8);
        _product.addBand("b2", ProductData.TYPE_UINT16);
        _product.addBand("b3", ProductData.TYPE_UINT32);
        _product.addBand("b4", ProductData.TYPE_INT8);
        _product.addBand("b5", ProductData.TYPE_INT16);
        _product.addBand("b6", ProductData.TYPE_INT32);
        _product.addBand("b7", ProductData.TYPE_FLOAT32);
        final Dimension tileSize = JAIUtils.computePreferredTileSize(WIDTH, HEIGHT, 1);
        _product.setPreferredTileSize(tileSize);


        final TiffIFD ifd = new TiffIFD(_product);


        final double[] expWidth = new double[]{WIDTH};
        final double[] expHeight = new double[]{HEIGHT};
        final double[] expBitsPerSample = new double[]{
                32, 32, 32, 32, 32, 32, 32,
        };
        final double[] expCompression = new double[]{1};
        final double[] expPhotoInter = new double[]{TiffCode.PHOTOMETRIC_BLACK_IS_ZERO.getValue()};
        final TiffAscii expImageDescription = new TiffAscii(_product.getName());
        final double[] expStripOffsets = new double[]{
                0, 800, 1600, 2400, 3200, 4000, 4800
        };
        final double[] expSamplesPerPixel = new double[]{_product.getNumBands()};
        final double[] expRowsPerStrip = new double[]{HEIGHT};
        final double[] expStripByteCounts = new double[]{
                800, 800, 800, 800, 800, 800, 800,
        };
        final double[] expPlanarConfig = new double[]{TiffCode.PLANAR_CONFIG_PLANAR.getValue()};
        final double[] expSampleFormat = new double[]{
                TiffCode.SAMPLE_FORMAT_FLOAT.getValue(),
                TiffCode.SAMPLE_FORMAT_FLOAT.getValue(),
                TiffCode.SAMPLE_FORMAT_FLOAT.getValue(),
                TiffCode.SAMPLE_FORMAT_FLOAT.getValue(),
                TiffCode.SAMPLE_FORMAT_FLOAT.getValue(),
                TiffCode.SAMPLE_FORMAT_FLOAT.getValue(),
                TiffCode.SAMPLE_FORMAT_FLOAT.getValue(),
        };
        final double[] expXResolution = new double[]{1 / 1};
        final double[] expYResolution = new double[]{1 / 1};
        final double[] expResolutionUnit = new double[]{1};

        final TiffAscii beamMetadata = TiffIFD.getBeamMetadata(_product);

        checkTag(TiffTag.IMAGE_WIDTH, TiffLong.class, expWidth, null, ifd);
        checkTag(TiffTag.IMAGE_LENGTH, TiffLong.class, expHeight, null, ifd);
        checkTag(TiffTag.BITS_PER_SAMPLE, TiffShort[].class, expBitsPerSample, null, ifd);
        checkTag(TiffTag.COMPRESSION, TiffShort.class, expCompression, null, ifd);
        checkTag(TiffTag.PHOTOMETRIC_INTERPRETATION, TiffShort.class, expPhotoInter, null, ifd);
        checkAsciiTag(TiffTag.IMAGE_DESCRIPTION, expImageDescription, null, ifd);
        checkTag(TiffTag.STRIP_OFFSETS, TiffLong[].class, expStripOffsets, null, ifd);
        checkTag(TiffTag.SAMPLES_PER_PIXEL, TiffShort.class, expSamplesPerPixel, null, ifd);
        checkTag(TiffTag.ROWS_PER_STRIP, TiffLong.class, expRowsPerStrip, null, ifd);
        checkTag(TiffTag.STRIP_BYTE_COUNTS, TiffLong[].class, expStripByteCounts, null, ifd);
        checkTag(TiffTag.PLANAR_CONFIGURATION, TiffShort.class, expPlanarConfig, null, ifd);
        checkTag(TiffTag.SAMPLE_FORMAT, TiffShort[].class, expSampleFormat, null, ifd);
        checkTag(TiffTag.X_RESOLUTION, TiffRational.class, expXResolution, null, ifd);
        checkTag(TiffTag.Y_RESOLUTION, TiffRational.class, expYResolution, null, ifd);
        checkTag(TiffTag.RESOLUTION_UNIT, TiffShort.class, expResolutionUnit, null, ifd);
        checkAsciiTag(TiffTag.BEAM_METADATA, beamMetadata, null, ifd);


        final long ifdSize = ifd.getRequiredIfdSize();
        final long expRequiredReferencedValuesSize =
                computeRequiredValuesSize(expBitsPerSample, expStripOffsets, expStripByteCounts, expSampleFormat,
                                          expXResolution, expYResolution, expImageDescription, beamMetadata);
        final long referencedValuesSize = ifd.getRequiredReferencedValuesSize();
        final long sizeForStrips = ifd.getRequiredSizeForStrips();
        final long expEntireSize = ifdSize + referencedValuesSize + sizeForStrips;

        assertEquals(2 + 12 * 16 + 4, ifdSize);
        assertEquals(expRequiredReferencedValuesSize, referencedValuesSize);
        assertEquals(sumOf(expStripByteCounts), sizeForStrips);
        assertEquals(expEntireSize, ifd.getRequiredEntireSize());
    }

    public void testTiffIFDCreationUByte() throws Exception {
        _product.addBand("b1", ProductData.TYPE_UINT8);
        _product.addBand("b2", ProductData.TYPE_UINT16);
        _product.addBand("b3", ProductData.TYPE_UINT8);
        final Dimension tileSize = JAIUtils.computePreferredTileSize(WIDTH, HEIGHT, 1);
        _product.setPreferredTileSize(tileSize);

        final TiffIFD ifd = new TiffIFD(_product);


        final double[] expWidth = new double[]{WIDTH};
        final double[] expHeight = new double[]{HEIGHT};
        final double[] expBitsPerSample = new double[]{
                16, 16, 16
        };
        final double[] expCompression = new double[]{1};
        final double[] expPhotoInter = new double[]{TiffCode.PHOTOMETRIC_BLACK_IS_ZERO.getValue()};
        final TiffAscii expImageDescription = new TiffAscii(_product.getName());;
        final double[] expStripOffsets = new double[]{
                0, 400, 800
        };
        final double[] expSamplesPerPixel = new double[]{_product.getNumBands()};
        final double[] expRowsPerStrip = new double[]{HEIGHT};
        final double[] expStripByteCounts = new double[]{
                400, 400, 400
        };
        final double[] expPlanarConfig = new double[]{TiffCode.PLANAR_CONFIG_PLANAR.getValue()};
        final double[] expSampleFormat = new double[]{
                TiffCode.SAMPLE_FORMAT_UINT.getValue(),
                TiffCode.SAMPLE_FORMAT_UINT.getValue(),
                TiffCode.SAMPLE_FORMAT_UINT.getValue()
        };
        final double[] expXResolution = new double[]{1 / 1};
        final double[] expYResolution = new double[]{1 / 1};
        final double[] expResolutionUnit = new double[]{1};
        final TiffAscii beamMetadata = TiffIFD.getBeamMetadata(_product);

        checkTag(TiffTag.IMAGE_WIDTH, TiffLong.class, expWidth, null, ifd);
        checkTag(TiffTag.IMAGE_LENGTH, TiffLong.class, expHeight, null, ifd);
        checkTag(TiffTag.BITS_PER_SAMPLE, TiffShort[].class, expBitsPerSample, null, ifd);
        checkTag(TiffTag.COMPRESSION, TiffShort.class, expCompression, null, ifd);
        checkTag(TiffTag.PHOTOMETRIC_INTERPRETATION, TiffShort.class, expPhotoInter, null, ifd);
        checkAsciiTag(TiffTag.IMAGE_DESCRIPTION, expImageDescription, null, ifd);
        checkTag(TiffTag.STRIP_OFFSETS, TiffLong[].class, expStripOffsets, null, ifd);
        checkTag(TiffTag.SAMPLES_PER_PIXEL, TiffShort.class, expSamplesPerPixel, null, ifd);
        checkTag(TiffTag.ROWS_PER_STRIP, TiffLong.class, expRowsPerStrip, null, ifd);
        checkTag(TiffTag.STRIP_BYTE_COUNTS, TiffLong[].class, expStripByteCounts, null, ifd);
        checkTag(TiffTag.PLANAR_CONFIGURATION, TiffShort.class, expPlanarConfig, null, ifd);
        checkTag(TiffTag.SAMPLE_FORMAT, TiffShort[].class, expSampleFormat, null, ifd);
        checkTag(TiffTag.X_RESOLUTION, TiffRational.class, expXResolution, null, ifd);
        checkTag(TiffTag.Y_RESOLUTION, TiffRational.class, expYResolution, null, ifd);
        checkTag(TiffTag.RESOLUTION_UNIT, TiffShort.class, expResolutionUnit, null, ifd);
        checkAsciiTag(TiffTag.BEAM_METADATA, beamMetadata, null, ifd);


        final long ifdSize = ifd.getRequiredIfdSize();
        final long expRequiredReferencedValuesSize =
                computeRequiredValuesSize(expBitsPerSample, expStripOffsets, expStripByteCounts, expSampleFormat,
                                          expXResolution, expYResolution, expImageDescription, beamMetadata);
        final long referencedValuesSize = ifd.getRequiredReferencedValuesSize();
        final long sizeForStrips = ifd.getRequiredSizeForStrips();
        final long expEntireSize = ifdSize + referencedValuesSize + sizeForStrips;

        assertEquals(2 + 12 * 16 + 4, ifdSize);
        assertEquals(expRequiredReferencedValuesSize, referencedValuesSize);
        assertEquals(sumOf(expStripByteCounts), sizeForStrips);
        assertEquals(expEntireSize, ifd.getRequiredEntireSize());
    }

    public void testWriteToStream() throws Exception {
        _product.addBand("b1", ProductData.TYPE_UINT16);
        _product.addBand("b2", ProductData.TYPE_FLOAT32);
        final Dimension tileSize = JAIUtils.computePreferredTileSize(WIDTH, HEIGHT, 1);
        _product.setPreferredTileSize(tileSize);
        fillBandWithData(_product.getBandAt(0), 20);
        fillBandWithData(_product.getBandAt(1), 1000);
        final int startOffset = 50;
        final MemoryCacheImageOutputStream stream = new MemoryCacheImageOutputStream(new ByteArrayOutputStream());


        final TiffIFD ifd = new TiffIFD(_product);
        ifd.write(stream, startOffset, 0);


        final long expectedStreamLength = ifd.getRequiredIfdSize() + ifd.getRequiredReferencedValuesSize() + startOffset;

        final double[] expWidth = new double[]{WIDTH};
        final double[] expHeight = new double[]{HEIGHT};
        final double[] expBitsPerSample = new double[]{
                32, 32
        };
        final double[] expCompression = new double[]{1};
        final double[] expPhotoInter = new double[]{TiffCode.PHOTOMETRIC_BLACK_IS_ZERO.getValue()};
        final TiffAscii expImageDescription = new TiffAscii(_product.getName());
        final double[] expStripOffsets = new double[]{
                0 + expectedStreamLength, 800 + expectedStreamLength
        };
        final double[] expSamplesPerPixel = new double[]{_product.getNumBands()};
        final double[] expRowsPerStrip = new double[]{HEIGHT};
        final double[] expStripByteCounts = new double[]{
                800, 800
        };
        final double[] expPlanarConfig = new double[]{TiffCode.PLANAR_CONFIG_PLANAR.getValue()};
        final double[] expSampleFormat = new double[]{
                TiffCode.SAMPLE_FORMAT_FLOAT.getValue(),
                TiffCode.SAMPLE_FORMAT_FLOAT.getValue(),
        };
        final double[] expXResolution = new double[]{1 / 1};
        final double[] expYResolution = new double[]{1 / 1};
        final double[] expResolutionUnit = new double[]{1};

        long offset = ifd.getRequiredIfdSize() + startOffset;
        final TiffLong expOffset1 = null;
        final TiffLong expOffset2 = null;
        final TiffLong expOffset3 = null;
        final TiffLong expOffset4 = null;
        final TiffLong expOffset5 = null;
        final TiffLong expOffset6 = new TiffLong(offset);
        offset += expImageDescription.getSizeInBytes();
        final TiffLong expOffset7 = new TiffLong(offset);
        final TiffLong expOffset8 = null;
        final TiffLong expOffset9 = null;
        offset += 8;
        final TiffLong expOffset10 = new TiffLong(offset);
        final TiffLong expOffset11 = null;
        final TiffLong expOffset12 = null;
        final TiffLong expOffset13 = null;
        final TiffLong expOffset14 = null;
        offset += 8;
        final TiffLong expOffset15 = new TiffLong(offset);
        offset += 8;
        final TiffLong expOffset16 = new TiffLong(offset);
        final TiffLong expOffset17 = null;
        offset += 8;
        final TiffLong expOffset18 = new TiffLong(offset);
        final TiffAscii beamMetadata = TiffIFD.getBeamMetadata(_product);

        checkTag(TiffTag.IMAGE_WIDTH, TiffLong.class, expWidth, expOffset1, ifd);
        checkTag(TiffTag.IMAGE_LENGTH, TiffLong.class, expHeight, expOffset2, ifd);
        checkTag(TiffTag.BITS_PER_SAMPLE, TiffShort[].class, expBitsPerSample, expOffset3, ifd);
        checkTag(TiffTag.COMPRESSION, TiffShort.class, expCompression, expOffset4, ifd);
        checkTag(TiffTag.PHOTOMETRIC_INTERPRETATION, TiffShort.class, expPhotoInter, expOffset5, ifd);
        checkAsciiTag(TiffTag.IMAGE_DESCRIPTION, expImageDescription, expOffset6, ifd);
        checkTag(TiffTag.STRIP_OFFSETS, TiffLong[].class, expStripOffsets, expOffset7, ifd);
        checkTag(TiffTag.SAMPLES_PER_PIXEL, TiffShort.class, expSamplesPerPixel, expOffset8, ifd);
        checkTag(TiffTag.ROWS_PER_STRIP, TiffLong.class, expRowsPerStrip, expOffset9, ifd);
        checkTag(TiffTag.STRIP_BYTE_COUNTS, TiffLong[].class, expStripByteCounts, expOffset10, ifd);
        checkTag(TiffTag.PLANAR_CONFIGURATION, TiffShort.class, expPlanarConfig, expOffset11, ifd);
        checkTag(TiffTag.SAMPLE_FORMAT, TiffShort[].class, expSampleFormat, expOffset14, ifd);
        checkTag(TiffTag.X_RESOLUTION, TiffRational.class, expXResolution, expOffset15, ifd);
        checkTag(TiffTag.Y_RESOLUTION, TiffRational.class, expYResolution, expOffset16, ifd);
        checkTag(TiffTag.RESOLUTION_UNIT, TiffShort.class, expResolutionUnit, expOffset17, ifd);
        checkAsciiTag(TiffTag.BEAM_METADATA, beamMetadata, expOffset18, ifd);

        assertEquals(expectedStreamLength, stream.length());

        final byte[] expIfdBytes = createIFDBytes(ifd, startOffset);
        final byte[] actBytes = new byte[expIfdBytes.length];
        stream.seek(startOffset);
        stream.read(actBytes);
        Assert.assertArrayEquals(expIfdBytes, actBytes);
    }

    public void testWriteToStream_WithIllegalOffset() throws Exception {
        _product.addBand("b1", ProductData.TYPE_UINT16);
        fillBandWithData(_product.getBandAt(0), 20);
        final MemoryCacheImageOutputStream stream = new MemoryCacheImageOutputStream(new ByteArrayOutputStream());

        final TiffIFD ifd = new TiffIFD(_product);
        final int illegalOffset = -1;
        try {
            ifd.write(stream, illegalOffset, 0);
            fail("IllegalArgumentException expected because the ifd offset is illegal");
        } catch (IllegalArgumentException expected) {

        } catch (Exception notExpected) {
            fail("IllegalArgumentException expected but was [" + notExpected.getClass().getName() + "]");
        }
    }

    public void testGetStripOffsets_AfterWrite() throws IOException {
        final long firstIFDOffset = 10;
        _product.addBand("b1", ProductData.TYPE_UINT16);
        fillBandWithData(_product.getBandAt(0), 20);
        final MemoryCacheImageOutputStream stream = new MemoryCacheImageOutputStream(new ByteArrayOutputStream());
        final TiffIFD ifd = new TiffIFD(_product);

        ifd.write(stream, firstIFDOffset, 0);
        final TiffLong[] stripOffsets = (TiffLong[]) ifd.getEntry(TiffTag.STRIP_OFFSETS).getValues();

        assertNotNull(stripOffsets);
        assertEquals(1, stripOffsets.length);
        assertEquals(ifd.getRequiredIfdSize() + firstIFDOffset + ifd.getRequiredReferencedValuesSize(),
                     stripOffsets[0].getValue());
    }

    private long computeRequiredValuesSize(final double[] expBitsPerSample,
                                           final double[] expStripOffsets,
                                           final double[] expStripByteCounts,
                                           final double[] expSampleFormat,
                                           final double[] expXResolution,
                                           final double[] expYResolution,
                                           TiffAscii imageDescription, TiffAscii metadata) {

        long size = TiffType.getBytesForType(TiffType.SHORT) * expBitsPerSample.length;
        size += TiffType.getBytesForType(TiffType.LONG) * expStripOffsets.length;
        size += TiffType.getBytesForType(TiffType.LONG) * expStripByteCounts.length;
        size += TiffType.getBytesForType(TiffType.SHORT) * expSampleFormat.length;
        size += TiffType.getBytesForType(TiffType.RATIONAL) * expXResolution.length;
        size += TiffType.getBytesForType(TiffType.RATIONAL) * expYResolution.length;
        size += imageDescription.getSizeInBytes();
        size += metadata.getSizeInBytes();

        return size;
    }

    private long sumOf(final double[] doubles) {
        double sum = 0;
        for (int i = 0; i < doubles.length; i++) {
            sum += doubles[i];
        }
        return Math.round(sum);
    }

    private byte[] createIFDBytes(final TiffIFD ifd, final int startOffset) throws IOException {
        final ByteArrayOutputStream bout = new ByteArrayOutputStream();
        final MemoryCacheImageOutputStream ios = new MemoryCacheImageOutputStream(bout);
        ios.seek(startOffset);
        final int nextIFDOffset = 0;

        final TiffShort[] entryTags = new TiffShort[]{
                TiffTag.IMAGE_WIDTH,
                TiffTag.IMAGE_LENGTH,
                TiffTag.BITS_PER_SAMPLE,
                TiffTag.COMPRESSION,
                TiffTag.PHOTOMETRIC_INTERPRETATION,
                TiffTag.IMAGE_DESCRIPTION,
                TiffTag.STRIP_OFFSETS,
                TiffTag.SAMPLES_PER_PIXEL,
                TiffTag.ROWS_PER_STRIP,
                TiffTag.STRIP_BYTE_COUNTS,
                TiffTag.X_RESOLUTION,
                TiffTag.Y_RESOLUTION,
                TiffTag.PLANAR_CONFIGURATION,
                TiffTag.RESOLUTION_UNIT,
                TiffTag.SAMPLE_FORMAT,
                TiffTag.BEAM_METADATA
        };
        ios.writeShort(entryTags.length);

        long nextEntryPos = 2 +startOffset;
        for (int i = 0; i < entryTags.length; i++) {
            ios.seek(nextEntryPos);
            ifd.getEntry(entryTags[i]).write(ios);
            nextEntryPos += TiffDirectoryEntry.BYTES_PER_ENTRY;
        }
        ios.writeInt(nextIFDOffset);

        ios.flush();
        byte[] bytes = bout.toByteArray();
        return Arrays.copyOfRange(bytes, startOffset, bytes.length);
    }

    private void checkAsciiTag(final TiffShort tag, final TiffAscii expectedValue,
                          final TiffLong expectedOffset, final TiffIFD tiffIFD) throws Exception {
        final TiffDirectoryEntry entry = tiffIFD.getEntry(tag);
        assertNotNull(entry);
        final TiffLong valuesOffset = entry.getValuesOffset();
        if (expectedOffset != null) {
            assertNotNull(valuesOffset);
            assertEquals(expectedOffset.getValue(), valuesOffset.getValue());
        } else {
            assertNull(valuesOffset);
        }
        final TiffValue actualValue = entry.getValues()[0];
        final TiffAscii value = (TiffAscii)actualValue;
        assertEquals(expectedValue.getValue(), value.getValue());
    }

    private void checkTag(final TiffShort tag, final Class expectedClassType, final double[] expectedValues,
                          final TiffLong expectedValuesOffset, final TiffIFD tiffIFD) throws Exception {
        final TiffDirectoryEntry entry = tiffIFD.getEntry(tag);
        assertNotNull(entry);
        final TiffLong valuesOffset = entry.getValuesOffset();
        if (expectedValuesOffset == null) {
            assertNull(valuesOffset);
        } else {
            assertNotNull(valuesOffset);
            assertEquals(expectedValuesOffset.getValue(), valuesOffset.getValue());
        }
        final TiffValue[] values = entry.getValues();
        assertEquals(expectedValues.length, values.length);
        if (expectedValues.length > 1) {
            assertTrue(expectedClassType.isInstance(values));
        } else {
            assertTrue(expectedClassType.isInstance(values[0]));
        }
        for (int i = 0; i < expectedValues.length; i++) {
            assertEquals("failure at index " + i, expectedValues[i], getValue(values[i]), 1e-10);
        }
    }

    private double getValue(final TiffValue object) throws Exception {
        final Method getter = object.getClass().getMethod("getValue");
        final Object result = getter.invoke(object);
        final Number numResult = (Number) result;
        return numResult.doubleValue();
    }

    private void fillBandWithData(final Band band, final int start) {
        final ProductData data = band.createCompatibleRasterData();
        for (int i = 0; i < WIDTH * HEIGHT; i++) {
            data.setElemIntAt(i, start + i);
        }
        band.setData(data);
    }

}
