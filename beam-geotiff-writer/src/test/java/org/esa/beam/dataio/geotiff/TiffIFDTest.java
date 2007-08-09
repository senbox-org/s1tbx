package org.esa.beam.dataio.geotiff;

/**
 * Created by IntelliJ IDEA.
 * User: marco
 * Date: 09.02.2005
 * Time: 09:01:07
 */

import junit.framework.TestCase;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;

import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;

public class TiffIFDTest extends TestCase {

    private Product _product;
    private static final int WIDTH = 10;
    private static final int HEIGHT = 20;

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


        final TiffIFD ifd = new TiffIFD(_product);


        final double[] expWidth = new double[]{WIDTH};
        final double[] expHeight = new double[]{HEIGHT};
        final double[] expBitsPerSample = new double[]{
                32, 32, 32, 32, 32, 32, 32,
        };
        final double[] expCompression = new double[]{1};
        final double[] expPhotoInter = new double[]{TiffCode.PHOTOMETRIC_BLACK_IS_ZERO.getValue()};
        final double[] expStripOffsets = new double[]{
                0, 800, 1600, 2400, 3200, 4000, 4800,
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


        checkTag(TiffTag.IMAGE_WIDTH, TiffLong.class, expWidth, null, ifd);
        checkTag(TiffTag.IMAGE_LENGTH, TiffLong.class, expHeight, null, ifd);
        checkTag(TiffTag.BITS_PER_SAMPLE, TiffShort[].class, expBitsPerSample, null, ifd);
        checkTag(TiffTag.COMPRESSION, TiffShort.class, expCompression, null, ifd);
        checkTag(TiffTag.PHOTOMETRIC_INTERPRETATION, TiffShort.class, expPhotoInter, null, ifd);
        checkTag(TiffTag.STRIP_OFFSETS, TiffLong[].class, expStripOffsets, null, ifd);
        checkTag(TiffTag.SAMPLES_PER_PIXEL, TiffShort.class, expSamplesPerPixel, null, ifd);
        checkTag(TiffTag.ROWS_PER_STRIP, TiffLong.class, expRowsPerStrip, null, ifd);
        checkTag(TiffTag.STRIP_BYTE_COUNTS, TiffLong[].class, expStripByteCounts, null, ifd);
        checkTag(TiffTag.PLANAR_CONFIGURATION, TiffShort.class, expPlanarConfig, null, ifd);
        checkTag(TiffTag.SAMPLE_FORMAT, TiffShort[].class, expSampleFormat, null, ifd);
        checkTag(TiffTag.X_RESOLUTION, TiffRational.class, expXResolution, null, ifd);
        checkTag(TiffTag.Y_RESOLUTION, TiffRational.class, expYResolution, null, ifd);
        checkTag(TiffTag.RESOLUTION_UNIT, TiffShort.class, expResolutionUnit, null, ifd);


        final long ifdSize = ifd.getRequiredIfdSize();
        final long expRequiredReferencedValuesSize =
                computeRequiredValuesSize(expBitsPerSample, expStripOffsets, expStripByteCounts, expSampleFormat,
                                          expXResolution, expYResolution);
        final long referencedValuesSize = ifd.getRequiredReferencedValuesSize();
        final long sizeForStrips = ifd.getRequiredSizeForStrips();
        final long expEntireSize = ifdSize + referencedValuesSize + sizeForStrips;

        assertEquals(2 + 12 * 14 + 4, ifdSize);
        assertEquals(expRequiredReferencedValuesSize, referencedValuesSize);
        assertEquals(sumOf(expStripByteCounts), sizeForStrips);
        assertEquals(expEntireSize, ifd.getRequiredEntireSize());
    }

    public void testTiffIFDCreationUByte() throws Exception {
        _product.addBand("b1", ProductData.TYPE_UINT8);
        _product.addBand("b2", ProductData.TYPE_UINT8);
        _product.addBand("b3", ProductData.TYPE_UINT8);

        final TiffIFD ifd = new TiffIFD(_product);


        final double[] expWidth = new double[]{WIDTH};
        final double[] expHeight = new double[]{HEIGHT};
        final double[] expBitsPerSample = new double[]{
                8, 8, 8
        };
        final double[] expCompression = new double[]{1};
        final double[] expPhotoInter = new double[]{TiffCode.PHOTOMETRIC_BLACK_IS_ZERO.getValue()};
        final double[] expStripOffsets = new double[]{
                0, 200, 400
        };
        final double[] expSamplesPerPixel = new double[]{_product.getNumBands()};
        final double[] expRowsPerStrip = new double[]{HEIGHT};
        final double[] expStripByteCounts = new double[]{
                200, 200, 200
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


        checkTag(TiffTag.IMAGE_WIDTH, TiffLong.class, expWidth, null, ifd);
        checkTag(TiffTag.IMAGE_LENGTH, TiffLong.class, expHeight, null, ifd);
        checkTag(TiffTag.BITS_PER_SAMPLE, TiffShort[].class, expBitsPerSample, null, ifd);
        checkTag(TiffTag.COMPRESSION, TiffShort.class, expCompression, null, ifd);
        checkTag(TiffTag.PHOTOMETRIC_INTERPRETATION, TiffShort.class, expPhotoInter, null, ifd);
        checkTag(TiffTag.STRIP_OFFSETS, TiffLong[].class, expStripOffsets, null, ifd);
        checkTag(TiffTag.SAMPLES_PER_PIXEL, TiffShort.class, expSamplesPerPixel, null, ifd);
        checkTag(TiffTag.ROWS_PER_STRIP, TiffLong.class, expRowsPerStrip, null, ifd);
        checkTag(TiffTag.STRIP_BYTE_COUNTS, TiffLong[].class, expStripByteCounts, null, ifd);
        checkTag(TiffTag.PLANAR_CONFIGURATION, TiffShort.class, expPlanarConfig, null, ifd);
        checkTag(TiffTag.SAMPLE_FORMAT, TiffShort[].class, expSampleFormat, null, ifd);
        checkTag(TiffTag.X_RESOLUTION, TiffRational.class, expXResolution, null, ifd);
        checkTag(TiffTag.Y_RESOLUTION, TiffRational.class, expYResolution, null, ifd);
        checkTag(TiffTag.RESOLUTION_UNIT, TiffShort.class, expResolutionUnit, null, ifd);


        final long ifdSize = ifd.getRequiredIfdSize();
        final long expRequiredReferencedValuesSize =
                computeRequiredValuesSize(expBitsPerSample, expStripOffsets, expStripByteCounts, expSampleFormat,
                                          expXResolution, expYResolution);
        final long referencedValuesSize = ifd.getRequiredReferencedValuesSize();
        final long sizeForStrips = ifd.getRequiredSizeForStrips();
        final long expEntireSize = ifdSize + referencedValuesSize + sizeForStrips;

        assertEquals(2 + 12 * 14 + 4, ifdSize);
        assertEquals(expRequiredReferencedValuesSize, referencedValuesSize);
        assertEquals(sumOf(expStripByteCounts), sizeForStrips);
        assertEquals(expEntireSize, ifd.getRequiredEntireSize());
    }

    public void testWriteToStream() throws Exception {
        _product.addBand("b1", ProductData.TYPE_UINT16);
        _product.addBand("b2", ProductData.TYPE_FLOAT32);
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
                // remarked because geotiff product writer must write all bands as floats
//            16, 32
        };
        final double[] expCompression = new double[]{1};
        final double[] expPhotoInter = new double[]{TiffCode.PHOTOMETRIC_BLACK_IS_ZERO.getValue()};
        final double[] expStripOffsets = new double[]{
                0 + expectedStreamLength, 800 + expectedStreamLength
                // remarked because geotiff product writer must write all bands as floats
//            0 + expectedStreamLength, 400 + expectedStreamLength
        };
        final double[] expSamplesPerPixel = new double[]{_product.getNumBands()};
        final double[] expRowsPerStrip = new double[]{HEIGHT};
        final double[] expStripByteCounts = new double[]{
                800, 800
                // remarked because geotiff product writer must write all bands as floats
//            400, 800
        };
        final double[] expPlanarConfig = new double[]{TiffCode.PLANAR_CONFIG_PLANAR.getValue()};
        final double[] expSampleFormat = new double[]{
                TiffCode.SAMPLE_FORMAT_FLOAT.getValue(),
                // remarked because geotiff product writer must write all bands as floats
//            TiffCode.SAMPLE_FORMAT_UINT.getValue(),
                TiffCode.SAMPLE_FORMAT_FLOAT.getValue(),
        };
        final double[] expXResolution = new double[]{1 / 1};
        final double[] expYResolution = new double[]{1 / 1};
        final double[] expResolutionUnit = new double[]{1};

        final long valuesStart = ifd.getRequiredIfdSize() + startOffset;
        final TiffLong expOffset1 = null;
        final TiffLong expOffset2 = null;
        final TiffLong expOffset3 = null;
        final TiffLong expOffset4 = null;
        final TiffLong expOffset5 = null;
        final TiffLong expOffset6 = new TiffLong(valuesStart);
        final TiffLong expOffset7 = null;
        final TiffLong expOffset8 = null;
        final TiffLong expOffset9 = new TiffLong(valuesStart + 8);
        final TiffLong expOffset10 = null;
        final TiffLong expOffset11 = null;
        final TiffLong expOffset12 = new TiffLong(valuesStart + 16);
        final TiffLong expOffset13 = new TiffLong(valuesStart + 24);
        final TiffLong expOffset14 = null;

        checkTag(TiffTag.IMAGE_WIDTH, TiffLong.class, expWidth, expOffset1, ifd);
        checkTag(TiffTag.IMAGE_LENGTH, TiffLong.class, expHeight, expOffset2, ifd);
        checkTag(TiffTag.BITS_PER_SAMPLE, TiffShort[].class, expBitsPerSample, expOffset3, ifd);
        checkTag(TiffTag.COMPRESSION, TiffShort.class, expCompression, expOffset4, ifd);
        checkTag(TiffTag.PHOTOMETRIC_INTERPRETATION, TiffShort.class, expPhotoInter, expOffset5, ifd);
        checkTag(TiffTag.STRIP_OFFSETS, TiffLong[].class, expStripOffsets, expOffset6, ifd);
        checkTag(TiffTag.SAMPLES_PER_PIXEL, TiffShort.class, expSamplesPerPixel, expOffset7, ifd);
        checkTag(TiffTag.ROWS_PER_STRIP, TiffLong.class, expRowsPerStrip, expOffset8, ifd);
        checkTag(TiffTag.STRIP_BYTE_COUNTS, TiffLong[].class, expStripByteCounts, expOffset9, ifd);
        checkTag(TiffTag.PLANAR_CONFIGURATION, TiffShort.class, expPlanarConfig, expOffset10, ifd);
        checkTag(TiffTag.SAMPLE_FORMAT, TiffShort[].class, expSampleFormat, expOffset11, ifd);
        checkTag(TiffTag.X_RESOLUTION, TiffRational.class, expXResolution, expOffset12, ifd);
        checkTag(TiffTag.Y_RESOLUTION, TiffRational.class, expYResolution, expOffset13, ifd);
        checkTag(TiffTag.RESOLUTION_UNIT, TiffShort.class, expResolutionUnit, expOffset14, ifd);

        assertEquals(expectedStreamLength, stream.length());
        stream.seek(startOffset);
        final byte[] expIfdBytes = createIFDBytes(ifd);
        final byte[] actBytes = new byte[expIfdBytes.length];
        stream.read(actBytes);
        assertTrue(Arrays.equals(expIfdBytes, actBytes));
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
                                           final double[] expYResolution) {
        final long expBitsPerSampleValuesSize = TiffType.getBytesForType(TiffType.SHORT) * expBitsPerSample.length;
        final long expStripOffsetsValuesSize = TiffType.getBytesForType(TiffType.LONG) * expStripOffsets.length;
        final long expStripByteCountsValuesSize = TiffType.getBytesForType(TiffType.LONG) * expStripByteCounts.length;
        final long expSampleFormatValuesSize = TiffType.getBytesForType(TiffType.SHORT) * expSampleFormat.length;
        final long expXResolutionValuesSize = TiffType.getBytesForType(TiffType.RATIONAL) * expXResolution.length;
        final long expYResolutionValuesSize = TiffType.getBytesForType(TiffType.RATIONAL) * expYResolution.length;
        return expBitsPerSampleValuesSize + expStripOffsetsValuesSize +
               expStripByteCountsValuesSize + expSampleFormatValuesSize +
               expXResolutionValuesSize + expYResolutionValuesSize;
    }

    private long sumOf(final double[] doubles) {
        double sum = 0;
        for (int i = 0; i < doubles.length; i++) {
            sum += doubles[i];
        }
        return Math.round(sum);
    }

    private byte[] createIFDBytes(final TiffIFD ifd) throws IOException {
        final ByteArrayOutputStream bout = new ByteArrayOutputStream();
        final MemoryCacheImageOutputStream ios = new MemoryCacheImageOutputStream(bout);
        final int nextIFDOffset = 0;

        final TiffShort[] entryTags = new TiffShort[]{
                TiffTag.IMAGE_WIDTH,
                TiffTag.IMAGE_LENGTH,
                TiffTag.BITS_PER_SAMPLE,
                TiffTag.COMPRESSION,
                TiffTag.PHOTOMETRIC_INTERPRETATION,
                TiffTag.STRIP_OFFSETS,
                TiffTag.SAMPLES_PER_PIXEL,
                TiffTag.ROWS_PER_STRIP,
                TiffTag.STRIP_BYTE_COUNTS,
                TiffTag.X_RESOLUTION,
                TiffTag.Y_RESOLUTION,
                TiffTag.PLANAR_CONFIGURATION,
                TiffTag.RESOLUTION_UNIT,
                TiffTag.SAMPLE_FORMAT
        };
        ios.writeShort(entryTags.length);

        long nextEntryPos = 2;
        for (int i = 0; i < entryTags.length; i++) {
            ios.seek(nextEntryPos);
            ifd.getEntry(entryTags[i]).write(ios);
            nextEntryPos += TiffDirectoryEntry.BYTES_PER_ENTRY;
        }
        ios.writeInt(nextIFDOffset);

        ios.flush();
        return bout.toByteArray();
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
        for (int i = 00; i < WIDTH * HEIGHT; i++) {
            data.setElemIntAt(i, start + i);
        }
        band.setData(data);
    }

}