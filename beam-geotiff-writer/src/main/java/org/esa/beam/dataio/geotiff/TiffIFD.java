package org.esa.beam.dataio.geotiff;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.util.geotiff.GeoTIFFMetadata;

import javax.imageio.stream.ImageOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;

/**
 * A TIFF IFD implementation for the GeoTIFF format.
 *
 * @author Marco Peters
 * @author Sabine Embacher
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 */
class TiffIFD {

    private final TiffDirectoryEntrySet _entrySet;
    private static final int _BYTES_FOR_NEXT_IFD_OFFSET = 4;
    private static final int _BYTES_FOR_NUMBER_OF_ENTRIES = 2;
    private int maxElemSizeBandDataType;

    public TiffIFD(final Product product) {
        _entrySet = new TiffDirectoryEntrySet();
        initEntrys(product);
    }

    public void write(final ImageOutputStream ios, final long ifdOffset, final long nextIfdOffset) throws IOException {
        Guardian.assertGreaterThan("ifdOffset", ifdOffset, -1);
        computeOffsets(ifdOffset);
        ios.seek(ifdOffset);
        final TiffDirectoryEntry[] entries = _entrySet.getEntries();
        new TiffShort(entries.length).write(ios);
        long entryPosition = ios.getStreamPosition();
        for (TiffDirectoryEntry entry : entries) {
            ios.seek(entryPosition);
            entry.write(ios);
            entryPosition += TiffDirectoryEntry.BYTES_PER_ENTRY;
        }
        writeNextIfdOffset(ios, ifdOffset, nextIfdOffset);
    }

    private void writeNextIfdOffset(final ImageOutputStream ios, final long ifdOffset, final long nextIfdOffset) throws
            IOException {
        ios.seek(getPosForNextIfdOffset(ifdOffset));
        new TiffLong(nextIfdOffset).write(ios);
    }

    private long getPosForNextIfdOffset(final long ifdOffset) {
        return ifdOffset + getRequiredIfdSize() - 4;
    }

    public TiffDirectoryEntry getEntry(final TiffShort tag) {
        return _entrySet.getEntry(tag);
    }

    public long getRequiredIfdSize() {
        final TiffDirectoryEntry[] entries = _entrySet.getEntries();
        return _BYTES_FOR_NUMBER_OF_ENTRIES + entries.length * TiffDirectoryEntry.BYTES_PER_ENTRY + _BYTES_FOR_NEXT_IFD_OFFSET;
    }

    public long getRequiredReferencedValuesSize() {
        final TiffDirectoryEntry[] entries = _entrySet.getEntries();
        long size = 0;
        for (final TiffDirectoryEntry entry : entries) {
            if (entry.mustValuesBeReferenced()) {
                size += entry.getValuesSizeInBytes();
            }
        }
        return size;
    }

    public long getRequiredSizeForStrips() {
        final TiffLong[] counts = ((TiffLong[]) getEntry(TiffTag.STRIP_BYTE_COUNTS).getValues());
        long size = 0;
        for (TiffLong count : counts) {
            size += count.getValue();
        }
        return size;
    }

    public long getRequiredEntireSize() {
        return getRequiredIfdSize() + getRequiredReferencedValuesSize() + getRequiredSizeForStrips();
    }

    private void computeOffsets(final long ifdOffset) {
        final TiffDirectoryEntry[] entries = _entrySet.getEntries();
        long valuesOffset = computeStartOffsetForValues(entries.length, ifdOffset);
        for (final TiffDirectoryEntry entry : entries) {
            if (entry.mustValuesBeReferenced()) {
                entry.setValuesOffset(valuesOffset);
                valuesOffset += entry.getValuesSizeInBytes();
            }
        }
        moveStripsTo(valuesOffset);
    }

    private void moveStripsTo(final long stripsStart) {
        final TiffLong[] values = (TiffLong[]) getEntry(TiffTag.STRIP_OFFSETS).getValues();
        for (int i = 0; i < values.length; i++) {
            final long oldValue = values[i].getValue();
            final long newValue = oldValue + stripsStart;
            values[i] = new TiffLong(newValue);
        }
    }

    private long computeStartOffsetForValues(final int numEntries, final long ifdOffset) {
        final short bytesPerEntry = TiffDirectoryEntry.BYTES_PER_ENTRY;
        final int bytesForEntries = numEntries * bytesPerEntry;
        return ifdOffset + _BYTES_FOR_NUMBER_OF_ENTRIES + bytesForEntries + _BYTES_FOR_NEXT_IFD_OFFSET;
    }

    private void setEntry(final TiffDirectoryEntry entry) {
        _entrySet.set(entry);
    }

    public int getMaxElemSizeBandDataType() {
        return maxElemSizeBandDataType;
    }

    private void initEntrys(final Product product) {
        maxElemSizeBandDataType = getMaxElemSizeBandDataType(product);
        final int width = product.getSceneRasterWidth();
        final int height = product.getSceneRasterHeight();
        setEntry(new TiffDirectoryEntry(TiffTag.IMAGE_WIDTH, new TiffLong(width)));
        setEntry(new TiffDirectoryEntry(TiffTag.IMAGE_LENGTH, new TiffLong(height)));
        setEntry(new TiffDirectoryEntry(TiffTag.BITS_PER_SAMPLE, calculateBitsPerSample(product)));
        setEntry(new TiffDirectoryEntry(TiffTag.COMPRESSION, new TiffShort(1)));
        setEntry(new TiffDirectoryEntry(TiffTag.PHOTOMETRIC_INTERPRETATION, TiffCode.PHOTOMETRIC_BLACK_IS_ZERO));
        setEntry(new TiffDirectoryEntry(TiffTag.STRIP_OFFSETS, calculateStripOffsets()));
        setEntry(new TiffDirectoryEntry(TiffTag.SAMPLES_PER_PIXEL, new TiffShort(product.getNumBands())));
        setEntry(new TiffDirectoryEntry(TiffTag.ROWS_PER_STRIP, new TiffLong(height)));
        setEntry(new TiffDirectoryEntry(TiffTag.STRIP_BYTE_COUNTS, calculateStripByteCounts()));
        setEntry(new TiffDirectoryEntry(TiffTag.X_RESOLUTION, new TiffRational(1, 1)));
        setEntry(new TiffDirectoryEntry(TiffTag.Y_RESOLUTION, new TiffRational(1, 1)));
        setEntry(new TiffDirectoryEntry(TiffTag.RESOLUTION_UNIT, new TiffShort(1)));
        setEntry(new TiffDirectoryEntry(TiffTag.PLANAR_CONFIGURATION, TiffCode.PLANAR_CONFIG_PLANAR));
        setEntry(new TiffDirectoryEntry(TiffTag.SAMPLE_FORMAT, calculateSampleFormat(product)));
        addGeoTiffTags(product);
    }

    private void addGeoTiffTags(final Product product) {
        final GeoTIFFMetadata geoTIFFMetadata = ProductUtils.createGeoTIFFMetadata(product);
        if (geoTIFFMetadata == null) {
            return;
        }
        geoTIFFMetadata.dump(new PrintWriter(new OutputStreamWriter(System.out), true));
        final int numEntries = geoTIFFMetadata.getNumGeoKeyEntries();
        final TiffShort[] directoryTagValues = new TiffShort[numEntries * 4];
        final ArrayList<TiffDouble> doubleValues = new ArrayList<TiffDouble>();
        final ArrayList<TiffAscii> asciiValues = new ArrayList<TiffAscii>();
        for (int i = 0; i < numEntries; i++) {
            final GeoTIFFMetadata.KeyEntry entry = geoTIFFMetadata.getGeoKeyEntryAt(i);
            final int[] data = entry.getData();
            for (int j = 0; j < data.length; j++) {
                directoryTagValues[i * 4 + j] = new TiffShort(data[j]);
            }
            if (data[1] == TiffTag.GeoDoubleParamsTag.getValue()) {
                directoryTagValues[i * 4 + 3] = new TiffShort(doubleValues.size());
                final double[] geoDoubleParams = geoTIFFMetadata.getGeoDoubleParams(data[0]);
                for (double geoDoubleParam : geoDoubleParams) {
                    doubleValues.add(new TiffDouble(geoDoubleParam));
                }
            }
            if (data[1] == TiffTag.GeoAsciiParamsTag.getValue()) {
                directoryTagValues[i * 4 + 3] = new TiffShort(doubleValues.size());
                asciiValues.add(new TiffAscii(geoTIFFMetadata.getGeoAsciiParam(data[0])));
            }
        }
        setEntry(new TiffDirectoryEntry(TiffTag.GeoKeyDirectoryTag, directoryTagValues));
        if (doubleValues.size() > 0) {
            final TiffDouble[] tiffDoubles = doubleValues.toArray(new TiffDouble[doubleValues.size()]);
            setEntry(new TiffDirectoryEntry(TiffTag.GeoDoubleParamsTag, tiffDoubles));
        }
        if (asciiValues.size() > 0) {
            final TiffAscii[] tiffAsciies = asciiValues.toArray(new TiffAscii[asciiValues.size()]);
            setEntry(new TiffDirectoryEntry(TiffTag.GeoAsciiParamsTag, tiffAsciies));
        }
        double[] modelTransformation = geoTIFFMetadata.getModelTransformation();
        if (!isZeroArray(modelTransformation)) {
            setEntry(new TiffDirectoryEntry(TiffTag.ModelTransformationTag, toTiffDoubles(modelTransformation)));
        } else {
            double[] modelPixelScale = geoTIFFMetadata.getModelPixelScale();
            if (!isZeroArray(modelPixelScale)) {
                setEntry(new TiffDirectoryEntry(TiffTag.ModelPixelScaleTag, toTiffDoubles(modelPixelScale)));
            }
            final int numModelTiePoints = geoTIFFMetadata.getNumModelTiePoints();
            if (numModelTiePoints > 0) {
                final TiffDouble[] tiePoints = new TiffDouble[numModelTiePoints * 6];
                for (int i = 0; i < numModelTiePoints; i++) {
                    final GeoTIFFMetadata.TiePoint modelTiePoint = geoTIFFMetadata.getModelTiePointAt(i);
                    final double[] data = modelTiePoint.getData();
                    for (int j = 0; j < data.length; j++) {
                        tiePoints[i * 6 + j] = new TiffDouble(data[j]);
                    }
                }
                setEntry(new TiffDirectoryEntry(TiffTag.ModelTiepointTag, tiePoints));
            }
        }
    }

    private static TiffDouble[] toTiffDoubles(double[] a) {
        final TiffDouble[] td = new TiffDouble[a.length];
        for (int i = 0; i < a.length; i++) {
            td[i] = new TiffDouble(a[i]);
        }
        return td;
    }

    private static boolean isZeroArray(double[] a) {
        for (double v : a) {
            if (v != 0.0) {
                return false;
            }
        }
        return true;
    }

    private static int getMaxElemSizeBandDataType(final Product product) {
        final Band[] bands = product.getBands();
        int dataType = -1;
        int maxSize = 0;
        for (Band band : bands) {
            int dt = band.getGeophysicalDataType();
            int es = ProductData.getElemSize(dt);
            if (es > maxSize && ProductData.isFloatingPointType(dt)) {
                dataType = dt;
                maxSize = es;
            }
        }
        if (dataType != -1) {
            return dataType;
        }
        dataType = ProductData.TYPE_UINT8;
        maxSize = 1;
        for (Band band : bands) {
            int dt = band.getGeophysicalDataType();
            int es = ProductData.getElemSize(dt);
            if (es > maxSize) {
                dataType = dt;
                maxSize = es;
            }
        }
        return dataType;
    }

    private TiffShort[] calculateSampleFormat(final Product product) {
        int dataType = getMaxElemSizeBandDataType();
        TiffShort sampleFormat;
        if (ProductData.isUIntType(dataType)) {
            sampleFormat = TiffCode.SAMPLE_FORMAT_UINT;
        } else if (ProductData.isIntType(dataType)) {
            sampleFormat = TiffCode.SAMPLE_FORMAT_INT;
        } else {
            sampleFormat = TiffCode.SAMPLE_FORMAT_FLOAT;
        }

        final TiffShort[] tiffValues = new TiffShort[product.getNumBands()];
        for (int i = 0; i < tiffValues.length; i++) {
            tiffValues[i] = sampleFormat;
        }

        return tiffValues;
    }

    private TiffLong[] calculateStripByteCounts() {
        TiffValue[] bitsPerSample = getBitsPerSampleValues();
        final TiffLong[] tiffValues = new TiffLong[bitsPerSample.length];
        for (int i = 0; i < tiffValues.length; i++) {
            long byteCount = getByteCount(bitsPerSample, i);
            tiffValues[i] = new TiffLong(byteCount);
        }
        return tiffValues;
    }

    private TiffLong[] calculateStripOffsets() {
        TiffValue[] bitsPerSample = getBitsPerSampleValues();
        final TiffLong[] tiffValues = new TiffLong[bitsPerSample.length];
        long offset = 0;
        for (int i = 0; i < tiffValues.length; i++) {
            tiffValues[i] = new TiffLong(offset);
            long byteCount = getByteCount(bitsPerSample, i);
            offset += byteCount;
        }
        return tiffValues;
    }

    private long getByteCount(TiffValue[] bitsPerSample, int i) {
        long bytesPerSample = ((TiffShort) bitsPerSample[i]).getValue() / 8;
        long byteCount = getWidth() * getHeight() * bytesPerSample;
        return byteCount;
    }

    private TiffShort[] calculateBitsPerSample(final Product product) {
        int dataType = getMaxElemSizeBandDataType();
        int elemSize = ProductData.getElemSize(dataType);
        final TiffShort[] tiffValues = new TiffShort[product.getNumBands()];
        for (int i = 0; i < tiffValues.length; i++) {
            tiffValues[i] = new TiffShort(8 * elemSize);
        }
        return tiffValues;
    }

    private TiffValue[] getBitsPerSampleValues() {
        return getEntry(TiffTag.BITS_PER_SAMPLE).getValues();
    }

    private long getHeight() {
        return ((TiffLong) getEntry(TiffTag.IMAGE_LENGTH).getValues()[0]).getValue();
    }

    private long getWidth() {
        return ((TiffLong) getEntry(TiffTag.IMAGE_WIDTH).getValues()[0]).getValue();
    }
}
