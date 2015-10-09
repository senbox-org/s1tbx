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

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.dataio.dimap.DimapHeaderWriter;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.ColorPaletteDef;
import org.esa.snap.core.datamodel.ImageInfo;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.util.Guardian;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.geotiff.GeoTIFFMetadata;
import org.esa.snap.dataio.geotiff.Utils;

import javax.imageio.stream.ImageOutputStream;
import java.awt.Color;
import java.awt.image.DataBuffer;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A TIFF IFD implementation for the GeoTIFF format.
 *
 * @author Marco Peters
 * @author Sabine Embacher
 * @author Norman Fomferra
 * @version $Revision: 2932 $ $Date: 2008-08-28 16:43:48 +0200 (Do, 28 Aug 2008) $
 */
public class TiffIFD {

    private static final long MAX_FILE_SIZE = 4294967296L;
    private static final int TIFF_COLORMAP_SIZE = 256;
    private static final int BYTES_FOR_NEXT_IFD_OFFSET = 4;
    private static final int BYTES_FOR_NUMBER_OF_ENTRIES = 2;

    private final TiffDirectoryEntrySet entrySet;
    private int maxElemSizeBandDataType;

    public TiffIFD(final Product product) {
        entrySet = new TiffDirectoryEntrySet();
        initEntrys(product);
    }

    public void write(final ImageOutputStream ios, final long ifdOffset, final long nextIfdOffset) throws IOException {
        Guardian.assertGreaterThan("ifdOffset", ifdOffset, -1);
        computeOffsets(ifdOffset);
        ios.seek(ifdOffset);
        final TiffDirectoryEntry[] entries = entrySet.getEntries();
        new TiffShort(entries.length).write(ios);
        long entryPosition = ios.getStreamPosition();
        for (TiffDirectoryEntry entry : entries) {
            ios.seek(entryPosition);
            entry.write(ios);
            entryPosition += TiffDirectoryEntry.BYTES_PER_ENTRY;
        }
        writeNextIfdOffset(ios, ifdOffset, nextIfdOffset);
    }

    private void writeNextIfdOffset(final ImageOutputStream ios, final long ifdOffset, final long nextIfdOffset) throws IOException {
        ios.seek(getPosForNextIfdOffset(ifdOffset));
        new TiffLong(nextIfdOffset).write(ios);
    }

    private long getPosForNextIfdOffset(final long ifdOffset) {
        return ifdOffset + getRequiredIfdSize() - 4;
    }

    public TiffDirectoryEntry getEntry(final TiffShort tag) {
        return entrySet.getEntry(tag);
    }

    public long getRequiredIfdSize() {
        final TiffDirectoryEntry[] entries = entrySet.getEntries();
        return BYTES_FOR_NUMBER_OF_ENTRIES + entries.length * TiffDirectoryEntry.BYTES_PER_ENTRY + BYTES_FOR_NEXT_IFD_OFFSET;
    }

    public long getRequiredReferencedValuesSize() {
        final TiffDirectoryEntry[] entries = entrySet.getEntries();
        long size = 0;
        for (final TiffDirectoryEntry entry : entries) {
            if (entry.mustValuesBeReferenced()) {
                size += entry.getValuesSizeInBytes();
            }
        }
        return size;
    }

    public long getRequiredSizeForStrips() {
        final TiffLong[] counts = (TiffLong[]) getEntry(TiffTag.STRIP_BYTE_COUNTS).getValues();
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
        final TiffDirectoryEntry[] entries = entrySet.getEntries();
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
        return ifdOffset + BYTES_FOR_NUMBER_OF_ENTRIES + bytesForEntries + BYTES_FOR_NEXT_IFD_OFFSET;
    }

    private void setEntry(final TiffDirectoryEntry entry) {
        entrySet.set(entry);
    }

    public int getBandDataType() {
        return maxElemSizeBandDataType;
    }

    private void initEntrys(final Product product) {
        maxElemSizeBandDataType = getMaxElemSizeBandDataType(product.getBands());
        final int width = product.getSceneRasterWidth();
        final int height = product.getSceneRasterHeight();

        setEntry(new TiffDirectoryEntry(TiffTag.IMAGE_WIDTH, new TiffLong(width)));
        setEntry(new TiffDirectoryEntry(TiffTag.IMAGE_LENGTH, new TiffLong(height)));
        setEntry(new TiffDirectoryEntry(TiffTag.BITS_PER_SAMPLE, calculateBitsPerSample(product)));
        setEntry(new TiffDirectoryEntry(TiffTag.COMPRESSION, new TiffShort(1)));
        setEntry(new TiffDirectoryEntry(TiffTag.IMAGE_DESCRIPTION, new TiffAscii(product.getName())));
        setEntry(new TiffDirectoryEntry(TiffTag.SAMPLES_PER_PIXEL, new TiffShort(getNumBands(product))));

        setEntry(new TiffDirectoryEntry(TiffTag.STRIP_OFFSETS, calculateStripOffsets()));
        setEntry(new TiffDirectoryEntry(TiffTag.ROWS_PER_STRIP, new TiffLong(height)));
        setEntry(new TiffDirectoryEntry(TiffTag.STRIP_BYTE_COUNTS, calculateStripByteCounts()));

        setEntry(new TiffDirectoryEntry(TiffTag.X_RESOLUTION, new TiffRational(1, 1)));
        setEntry(new TiffDirectoryEntry(TiffTag.Y_RESOLUTION, new TiffRational(1, 1)));
        setEntry(new TiffDirectoryEntry(TiffTag.RESOLUTION_UNIT, new TiffShort(1)));
        setEntry(new TiffDirectoryEntry(TiffTag.PLANAR_CONFIGURATION, TiffCode.PLANAR_CONFIG_PLANAR));
        setEntry(new TiffDirectoryEntry(TiffTag.SAMPLE_FORMAT, calculateSampleFormat(product)));
        setEntry(new TiffDirectoryEntry(TiffTag.BEAM_METADATA, getBeamMetadata(product)));

        TiffShort[] colorMap = null;
        if (isValidColorMapProduct(product)) {
            colorMap = createColorMap(product);
        }

        if (colorMap != null) {
            setEntry(new TiffDirectoryEntry(TiffTag.PHOTOMETRIC_INTERPRETATION, TiffCode.PHOTOMETRIC_RGB_PALETTE));
            setEntry(new TiffDirectoryEntry(TiffTag.COLOR_MAP, colorMap));
        } else {
            setEntry(new TiffDirectoryEntry(TiffTag.PHOTOMETRIC_INTERPRETATION, TiffCode.PHOTOMETRIC_BLACK_IS_ZERO));
        }

        addGeoTiffTags(product);
    }

    private static int getNumBands(Product product) {
        final Band[] bands = product.getBands();
        final List<Band> bandList = new ArrayList<Band>(bands.length);
        for (Band band : bands) {
            if (Utils.shouldWriteNode(band)) {
                bandList.add(band);
            }
        }
        return bandList.size();
    }

    private TiffShort[] createColorMap(Product product) {
        final ImageInfo imageInfo = product.getBandAt(0).getImageInfo(null, ProgressMonitor.NULL);
        final ColorPaletteDef paletteDef = imageInfo.getColorPaletteDef();
        final TiffShort[] redColor = new TiffShort[TIFF_COLORMAP_SIZE];
        Arrays.fill(redColor, new TiffShort(0));
        final TiffShort[] greenColor = new TiffShort[TIFF_COLORMAP_SIZE];
        Arrays.fill(greenColor, new TiffShort(0));
        final TiffShort[] blueColor = new TiffShort[TIFF_COLORMAP_SIZE];
        Arrays.fill(blueColor, new TiffShort(0));
        final float factor = 65535.0f / 255.0f;
        for (ColorPaletteDef.Point point : paletteDef.getPoints()) {
            final Color color = point.getColor();
            final int red = (int) (color.getRed() * factor);
            final int green = (int) (color.getGreen() * factor);
            final int blue = (int) (color.getBlue() * factor);
            int mapIndex = (int) Math.floor(point.getSample());
            redColor[mapIndex] = new TiffShort(red);
            greenColor[mapIndex] = new TiffShort(green);
            blueColor[mapIndex] = new TiffShort(blue);
        }
        final TiffShort[] colorMap = new TiffShort[TIFF_COLORMAP_SIZE * 3];
        System.arraycopy(redColor, 0, colorMap, 0, redColor.length);
        System.arraycopy(greenColor, 0, colorMap, TIFF_COLORMAP_SIZE, greenColor.length);
        System.arraycopy(blueColor, 0, colorMap, TIFF_COLORMAP_SIZE * 2, blueColor.length);
        return colorMap;
    }

    private static boolean isValidColorMapProduct(Product product) {
        return getNumBands(product) == 1 && product.getBandAt(0).getIndexCoding() != null &&
               product.getBandAt(0).getDataType() == ProductData.TYPE_UINT8;
    }

    static TiffAscii getBeamMetadata(final Product product) {
        final StringWriter stringWriter = new StringWriter();
        final DimapHeaderWriter writer = new DimapHeaderWriter(product, stringWriter, "");
        writer.writeHeader();
        writer.close();
        return new TiffAscii(stringWriter.getBuffer().toString());
    }

    private void addGeoTiffTags(final Product product) {
        final GeoTIFFMetadata geoTIFFMetadata = ProductUtils.createGeoTIFFMetadata(product);
        if (geoTIFFMetadata == null) {
            return;
        }

//  for debug purpose
//        geoTIFFMetadata.dump();

        final int numEntries = geoTIFFMetadata.getNumGeoKeyEntries();
        final TiffShort[] directoryTagValues = new TiffShort[numEntries * 4];
        final ArrayList<TiffDouble> doubleValues = new ArrayList<TiffDouble>();
        final ArrayList<String> asciiValues = new ArrayList<String>();
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
                int sizeInBytes = 0;
                for (String asciiValue : asciiValues) {
                    sizeInBytes += asciiValue.length() + 1;
                }
                directoryTagValues[i * 4 + 3] = new TiffShort(sizeInBytes);
                asciiValues.add(geoTIFFMetadata.getGeoAsciiParam(data[0]));
            }
        }
        setEntry(new TiffDirectoryEntry(TiffTag.GeoKeyDirectoryTag, directoryTagValues));
        if (!doubleValues.isEmpty()) {
            final TiffDouble[] tiffDoubles = doubleValues.toArray(new TiffDouble[doubleValues.size()]);
            setEntry(new TiffDirectoryEntry(TiffTag.GeoDoubleParamsTag, tiffDoubles));
        }
        if (!asciiValues.isEmpty()) {
            final String[] tiffAsciies = asciiValues.toArray(new String[asciiValues.size()]);
            setEntry(new TiffDirectoryEntry(TiffTag.GeoAsciiParamsTag, new GeoTiffAscii(tiffAsciies)));
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

    static int getMaxElemSizeBandDataType(final Band[] bands) {
        int maxSignedIntType = -1;
        int maxUnsignedIntType = -1;
        int maxFloatType = -1;
        for (Band band : bands) {
            int dt = band.getDataType();
            if (ProductData.isIntType(dt)) {
                if (ProductData.isUIntType(dt)) {
                    maxUnsignedIntType = Math.max(maxUnsignedIntType, dt);
                } else {
                    maxSignedIntType = Math.max(maxSignedIntType, dt);
                }
            }
            if (ProductData.isFloatingPointType(dt)) {
                maxFloatType = Math.max(maxFloatType, dt);
            }
        }

        if (maxFloatType != -1) {
            return ProductData.TYPE_FLOAT32;
        }

        if (maxUnsignedIntType != -1) {
            if (maxSignedIntType == -1) {
                return maxUnsignedIntType;
            }
            if (ProductData.getElemSize(maxUnsignedIntType) >= ProductData.getElemSize(maxSignedIntType)) {
                int returnType = maxUnsignedIntType - 10 + 1;
                if (returnType > 12) {
                    return ProductData.TYPE_FLOAT32;
                } else {
                    return returnType;
                }
            }
        }

        if (maxSignedIntType != -1) {
            return maxSignedIntType;
        }

        return DataBuffer.TYPE_UNDEFINED;
    }

    private TiffShort[] calculateSampleFormat(final Product product) {
        int dataType = getBandDataType();
        TiffShort sampleFormat;
        if (ProductData.isUIntType(dataType)) {
            sampleFormat = TiffCode.SAMPLE_FORMAT_UINT;
        } else if (ProductData.isIntType(dataType)) {
            sampleFormat = TiffCode.SAMPLE_FORMAT_INT;
        } else {
            sampleFormat = TiffCode.SAMPLE_FORMAT_FLOAT;
        }

        final TiffShort[] tiffValues = new TiffShort[getNumBands(product)];
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
            if (offset > MAX_FILE_SIZE) {
                String msg = String.format("File size too big. TIFF file size is limited to [%d] bytes!", MAX_FILE_SIZE);
                throw new IllegalStateException(msg);
            }
        }
        return tiffValues;
    }

    private long getByteCount(TiffValue[] bitsPerSample, int i) {
        long bytesPerSample = ((TiffShort) bitsPerSample[i]).getValue() / 8;
        return getWidth() * getHeight() * bytesPerSample;
    }

    private TiffShort[] calculateBitsPerSample(final Product product) {
        int dataType = getBandDataType();
        int elemSize = ProductData.getElemSize(dataType);
        final TiffShort[] tiffValues = new TiffShort[getNumBands(product)];
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
