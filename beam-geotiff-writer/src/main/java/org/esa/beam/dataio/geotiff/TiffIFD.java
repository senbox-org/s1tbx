package org.esa.beam.dataio.geotiff;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.util.geotiff.GeoTIFFMetadata;

import javax.imageio.stream.ImageOutputStream;
import java.io.IOException;
import java.util.ArrayList;

/**
 * A TIFF IFD implementation for the GeoTIFF format.
 *
 * @author Marco Peters
 * @author Sabine Embacher
 * @author Norman Fomferra
 * @version $Revision: 1.1 $ $Date: 2006/09/14 13:19:21 $
 */
class TiffIFD {

    private final TiffDirectoryEntrySet _entrySet;
    private static final int _BYTES_FOR_NEXT_IFD_OFFSET = 4;
    private static final int _BYTES_FOR_NUMBER_OF_ENTRIES = 2;

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
        for (int i = 0; i < entries.length; i++) {
            ios.seek(entryPosition);
            final TiffDirectoryEntry entry = entries[i];
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
        for (int i = 0; i < entries.length; i++) {
            final TiffDirectoryEntry entry = entries[i];
            if (entry.mustValuesBeReferenced()) {
                size += entry.getValuesSizeInBytes();
            }
        }
        return size;
    }

    public long getRequiredSizeForStrips() {
        final TiffLong[] counts = ((TiffLong[]) getEntry(TiffTag.STRIP_BYTE_COUNTS).getValues());
        long size = 0;
        for (int i = 0; i < counts.length; i++) {
            size += counts[i].getValue();
        }
        return size;
    }

    public long getRequiredEntireSize() {
        return getRequiredIfdSize() + getRequiredReferencedValuesSize() + getRequiredSizeForStrips();
    }

    private void computeOffsets(final long ifdOffset) {
        final TiffDirectoryEntry[] entries = _entrySet.getEntries();
        long valuesOffset = computeStartOffsetForValues(entries.length, ifdOffset);
        for (int i = 0; i < entries.length; i++) {
            final TiffDirectoryEntry entry = entries[i];
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

    private void initEntrys(final Product product) {
        final int width = product.getSceneRasterWidth();
        final int height = product.getSceneRasterHeight();
        setEntry(new TiffDirectoryEntry(TiffTag.IMAGE_WIDTH, new TiffLong(width)));
        setEntry(new TiffDirectoryEntry(TiffTag.IMAGE_LENGTH, new TiffLong(height)));
        setEntry(new TiffDirectoryEntry(TiffTag.BITS_PER_SAMPLE, createBitsPerSampleValues(product)));
        setEntry(new TiffDirectoryEntry(TiffTag.COMPRESSION, new TiffShort(1)));
        setEntry(new TiffDirectoryEntry(TiffTag.PHOTOMETRIC_INTERPRETATION, TiffCode.PHOTOMETRIC_BLACK_IS_ZERO));
        setEntry(new TiffDirectoryEntry(TiffTag.STRIP_OFFSETS, createStripOffsets()));
        setEntry(new TiffDirectoryEntry(TiffTag.SAMPLES_PER_PIXEL, new TiffShort(product.getNumBands())));
        setEntry(new TiffDirectoryEntry(TiffTag.ROWS_PER_STRIP, new TiffLong(height)));
        setEntry(new TiffDirectoryEntry(TiffTag.STRIP_BYTE_COUNTS, calculateStripByteCounts()));
        //todo: getPixelSize for resolution
        setEntry(new TiffDirectoryEntry(TiffTag.X_RESOLUTION, new TiffRational(1, 1)));
        setEntry(new TiffDirectoryEntry(TiffTag.Y_RESOLUTION, new TiffRational(1, 1)));
        setEntry(new TiffDirectoryEntry(TiffTag.RESOLUTION_UNIT, new TiffShort(1)));
        setEntry(new TiffDirectoryEntry(TiffTag.PLANAR_CONFIGURATION, TiffCode.PLANAR_CONFIG_PLANAR));
        setEntry(new TiffDirectoryEntry(TiffTag.SAMPLE_FORMAT, createSampleFormatValues(product)));
        addGeoTiffTags(product);
    }

    private void addGeoTiffTags(final Product product) {
        final GeoTIFFMetadata geoTIFFMetadata = ProductUtils.createGeoTIFFMetadata(product);
        if (geoTIFFMetadata == null) {
            return;
        }
        final int numEntries = geoTIFFMetadata.getNumGeoKeyEntries();
        final TiffShort[] directoryTagValues = new TiffShort[numEntries * 4];
        final ArrayList doubleValues = new ArrayList();
        final ArrayList asciiValues = new ArrayList();
        for (int i = 0; i < numEntries; i++) {
            final GeoTIFFMetadata.KeyEntry entry = geoTIFFMetadata.getGeoKeyEntryAt(i);
            final int[] data = entry.getData();
            for (int j = 0; j < data.length; j++) {
                directoryTagValues[i * 4 + j] = new TiffShort(data[j]);
            }
            if (data[1] == TiffTag.GeoDoubleParamsTag.getValue()) {
                final double[] geoDoubleParams = geoTIFFMetadata.getGeoDoubleParams(data[0]);
                for (int j = 0; j < geoDoubleParams.length; j++) {
                    doubleValues.add(new TiffDouble(geoDoubleParams[j]));
                }
            }
            if (data[1] == TiffTag.GeoAsciiParamsTag.getValue()) {
                asciiValues.add(new TiffAscii(geoTIFFMetadata.getGeoAsciiParam(data[0])));
            }
        }
        setEntry(new TiffDirectoryEntry(TiffTag.GeoKeyDirectoryTag, directoryTagValues));
        if (doubleValues.size() > 0) {
            final TiffDouble[] tiffDoubles = (TiffDouble[]) doubleValues.toArray(new TiffDouble[doubleValues.size()]);
            setEntry(new TiffDirectoryEntry(TiffTag.GeoDoubleParamsTag, tiffDoubles));
        }
        if (asciiValues.size() > 0) {
            final TiffAscii[] tiffAsciies = (TiffAscii[]) asciiValues.toArray(new TiffAscii[asciiValues.size()]);
            setEntry(new TiffDirectoryEntry(TiffTag.GeoAsciiParamsTag, tiffAsciies));
        }
        final int numModelTiePoints = geoTIFFMetadata.getNumModelTiePoints();
        final TiffDouble[] tiePoints = new TiffDouble[numModelTiePoints * 6];
        for (int i = 0; i < numModelTiePoints; i++) {
            final GeoTIFFMetadata.TiePoint modelTiePoint = geoTIFFMetadata.getModelTiePointAt(i);
            final double[] data = modelTiePoint.getData();
            for (int j = 0; j < data.length; j++) {
                tiePoints[i * 6 + j] = new TiffDouble(data[j]);
            }
        }
        setEntry(new TiffDirectoryEntry(TiffTag.ModelTiepointTag, tiePoints));

        setPixelScaleIfSpecified(geoTIFFMetadata);
    }

    private void setPixelScaleIfSpecified(final GeoTIFFMetadata geoTIFFMetadata) {
        final TiffDouble[] pixelScaleValues = new TiffDouble[]{
                new TiffDouble(geoTIFFMetadata.getModelPixelScaleX()),
                new TiffDouble(geoTIFFMetadata.getModelPixelScaleY()),
                new TiffDouble(geoTIFFMetadata.getModelPixelScaleZ()),
        };

        for (int i = 0; i < pixelScaleValues.length; i++) {
            if (pixelScaleValues[i].getValue() != 0.0 && pixelScaleValues[i].getValue() != 1.0) {
                setEntry(new TiffDirectoryEntry(TiffTag.ModelPixelScaleTag, pixelScaleValues));
                break;
            }
        }
    }

    private TiffShort[] createSampleFormatValues(final Product product) {
        final Band[] bands = product.getBands();
        final TiffShort[] tiffValues = new TiffShort[bands.length];
        for (int i = 0; i < bands.length; i++) {
            tiffValues[i] = TiffCode.SAMPLE_FORMAT_FLOAT;
            // remarked because geotiff product writer must write all bands as floats
//            tiffValues[i] = TiffCode.getSampleFormat(TiffType.getTiffTypeFrom(bands[i]));
        }
        return tiffValues;
    }

    private TiffLong[] calculateStripByteCounts() {
        final TiffLong[] tiffValues = new TiffLong[getBitsPerSampleValues().length];
        for (int i = 0; i < tiffValues.length; i++) {
            final long stripSize = getWidth() * getHeight() * getBytesPerSample(i);
            tiffValues[i] = new TiffLong(stripSize);
        }
        return tiffValues;
    }

    private TiffLong[] createStripOffsets() {
        final TiffLong[] tiffValues = new TiffLong[getBitsPerSampleValues().length];
        long offset = 0;
        for (int i = 0; i < tiffValues.length; i++) {
            tiffValues[i] = new TiffLong(offset);
            final int bytesPerSample = getBytesPerSample(i);
            offset += getWidth() * getHeight() * bytesPerSample;
        }
        return tiffValues;
    }

    private TiffShort[] createBitsPerSampleValues(final Product product) {
        final Band[] bands = product.getBands();
        final TiffShort[] tiffValues = new TiffShort[bands.length];
        for (int i = 0; i < bands.length; i++) {
            tiffValues[i] = new TiffShort(32);
            // remarked because geotiff product writer must write all bands as floats
//            final TiffShort tiffType = TiffType.getTiffTypeFrom(bands[i]);
//            tiffValues[i] = new TiffShort(TiffType.getBytesForType(tiffType) * 8);
        }
        return tiffValues;
    }

    private int getBytesPerSample(final int i) {
        final TiffValue[] bitsPerSample = getBitsPerSampleValues();
        return ((TiffShort) bitsPerSample[i]).getValue() / 8;
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
