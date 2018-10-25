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
package org.esa.snap.dataio.envisat;

import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.util.Debug;

import java.io.IOException;

/**
 * A <code>BandLineReader</code> instance is used read and decode single lines of the geophysical raster data stored in
 * ENVISAT product files.
 * <p> Band line reader instances are obtained through the <code>getBandLineReader</code> method of the
 * <code>ProductFile</code> class:
 * <pre>
 *    ProductFile file = ProductFile.open("MERIS_L2.prd");
 *    BandLineReader reader = file.getBandLineReader("radiance_9");
 * </pre>
 * <p> Band readers internally use a <code>RecordReader</code> to read the raw data records providing the pixel data for
 * a certain geophysical parameter.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 * @see org.esa.snap.dataio.envisat.ProductFile#getBandLineReader
 * @see org.esa.snap.dataio.envisat.RecordReader
 */
public class BandLineReader {

    public static class Virtual extends BandLineReader {

        private String _expression;

        public Virtual(BandInfo bandInfo, String expression) {
            super(bandInfo);
            this._expression = expression;
        }

        public String getExpression() {
            return _expression;
        }

        @Override
        public boolean isTiePointBased() {
            return false;
        }

        @Override
        public BandLineDecoder ensureBandLineDecoder() {
            throw new IllegalStateException();
        }

        @Override
        public void readLineRecord(int sourceY) throws IOException {
            throw new IllegalStateException();
        }

        @Override
        public ProductFile getProductFile() {
            throw new IllegalStateException();
        }

        @Override
        public void readRasterLine(int sourceMinX, int sourceMaxX, int sourceStepX, int sourceY,
                                   ProductData destRaster, int destRasterPos) throws IOException {
            throw new IllegalStateException();
        }

        @Override
        public int getRasterWidth() {
            throw new IllegalStateException();
        }

        @Override
        public int getRasterHeight() {
            throw new IllegalStateException();
        }

        @Override
        public Record getPixelDataRecord() {
            throw new IllegalStateException();
        }

        @Override
        public RecordReader getPixelDataReader() {
            throw new IllegalStateException();
        }

        @Override
        public Field getPixelDataField() {
            throw new IllegalStateException();
        }
    }

    /**
     * Band meta information
     */
    private final BandInfo _bandInfo;
    /**
     * The record reader for reading this band's raw pixel values
     */
    private RecordReader _pixelDataReader;
    private Record _pixelDataRecord;
    private Field _pixelDataField;
    private BandLineDecoder _bandLineDecoder;
    private int _maxRecordIndex;
    private long fieldOffset;
    private int dataFieldSampleSize;

    private BandLineReader(BandInfo bandInfo) {
        _bandInfo = bandInfo;
    }

    /**
     * Constructs a new band line reader instance.
     */
    BandLineReader(BandInfo bandInfo,
                   RecordReader pixelDataReader,
                   int pixelDataFieldIndex) {
        _bandInfo = bandInfo;
        _pixelDataReader = pixelDataReader;
        _pixelDataRecord = _pixelDataReader.createCompatibleRecord();
        _pixelDataField = _pixelDataRecord.getFieldAt(pixelDataFieldIndex);
        _bandLineDecoder = null; // will be lazily created for 'real' bands only (on demand)
        _maxRecordIndex = pixelDataReader.getDSD().getNumRecords() - 1;
        dataFieldSampleSize = getDataFieldSampleSize(getBandInfo());
        fieldOffset = getDataFieldOffset();
    }

    /**
     * Returns the band name.
     *
     * @return the band name, never <code>null</code>
     */
    public String getBandName() {
        return getBandInfo().getName();
    }

    /**
     * Returns band meta-information.
     *
     * @return the band information object, never <code>null</code>
     */
    public BandInfo getBandInfo() {
        return _bandInfo;
    }

    /**
     * Returns the pixel data reader object.
     *
     * @return the pixel data reader, never <code>null</code>
     */
    public RecordReader getPixelDataReader() {
        return _pixelDataReader;
    }

    /**
     * Retrieves the pixel data record from the current line reader.
     */
    public Record getPixelDataRecord() {
        return _pixelDataRecord;
    }

    /**
     * Retrieves the pixel data field from the current line reader.
     */
    public Field getPixelDataField() {
        return _pixelDataField;
    }

    /**
     * Retrieves a decoder instance for the current band line. Creates a new instance if not done so far.
     */
    public synchronized BandLineDecoder ensureBandLineDecoder() {
        if (_bandLineDecoder == null) {
            _bandLineDecoder = createBandLineDecoder(getPixelDataField(), getBandInfo());
            if (_bandLineDecoder == null) {
                StringBuffer sb = new StringBuffer();
                sb.append("createBandLineDecoder(sourceField=");
                sb.append(getPixelDataField());
                sb.append(", bandInfo=");
                sb.append(getBandInfo());
                sb.append(")");
                Debug.trace("WARNING: no appropriate band line decoder implemented: "); /*I18N*/
                Debug.trace(sb.toString());
                throw new IllegalStateException("no appropriate band line decoder implemented"); /*I18N*/
            }
        }
        return _bandLineDecoder;
    }

    /**
     * Returns the product file object from which ths reader was originally obtained.
     *
     * @return the product file, never <code>null</code>
     */
    public ProductFile getProductFile() {
        return getPixelDataReader().getProductFile();
    }

    /**
     * Returns the prospective width of a raster big enough to store the pixels of the band
     *
     * @return the prospective raster width
     */
    public int getRasterWidth() {
        return getPixelDataField().getNumElems();
    }

    /**
     * Returns the prospective height of a raster big enough to store the pixels of the band
     *
     * @return the prospective raster height
     */
    public int getRasterHeight() {
        return getPixelDataReader().getNumRecords();
    }

    /**
     * Asks whether this band reader creates pixel values by interpolating between the values of a certain tie-point
     * grid as provided in MERIS/AATSR metadata datasets (ADS).
     *
     * @return <code>true</code> if so
     */
    public boolean isTiePointBased() {
        return getPixelDataReader().getDSD().getDatasetType() != EnvisatConstants.DS_TYPE_MEASUREMENT;
    }

    /**
     * Reads a geophysical band from a measurement dataset (MDS).
     * <p>The method reads the sample values of the line <code>sourceY</code> from column <code>sourceMinX</code> to
     * <code>sourceMaxX</code> inclusively with a sub-sampling of <code>sourceStepX</code> pixels into the specified
     * raster data buffer <code>destRaster</code> beginning at offset <code>destRasterPos</code>.
     * <p> The maximum number of samples read is therefore given by the formula <code>1+(sourceMaxX-sourceMinX)/sourceStepX</code>.
     *
     * @param sourceMinX    the minimum X offset in source raster co-ordinates
     * @param sourceMaxX    the maximum X offset in source raster co-ordinates
     * @param sourceStepX   the sub-sampling in X direction within the region providing samples to be read
     * @param sourceY       the Y-offset (zero-based line index) in source raster co-ordinates
     * @param destRaster    the destination raster which receives the sample values to be read
     * @param destRasterPos the current line offset within the destination raster
     * @throws java.io.IOException if an I/O error occurs
     */
    public synchronized void readRasterLine(final int sourceMinX,
                                            final int sourceMaxX,
                                            final int sourceStepX,
                                            final int sourceY,
                                            final ProductData destRaster,
                                            final int destRasterPos) throws IOException {
        final ProductFile productFile = getProductFile();
        final int mappedMdsrIndex = productFile.getMappedMDSRIndex(sourceY);
        if (mappedMdsrIndex >= 0 && mappedMdsrIndex <= _maxRecordIndex) {

            final int destRasterIncr;
            final int destPos;
            final int sMinX;
            final int sMaxX;
            if (!productFile.storesPixelsInChronologicalOrder()) {
                destRasterIncr = 1;
                destPos = destRasterPos;
                sMinX = sourceMinX;
                sMaxX = sourceMaxX;
            } else {
                destRasterIncr = -1;

                destPos = destRasterPos + (sourceMaxX - sourceMinX) / sourceStepX;
                sMinX = _bandInfo.getWidth() - 1 - sourceMaxX;
                sMaxX = _bandInfo.getWidth() - 1 - sourceMinX;
            }

            // sourceY is mapped again later when reading the record
            readDataFieldSegment(sourceY, sMinX, sMaxX);

            ensureBandLineDecoder().computeLine(
                    getPixelDataField().getElems(),
                    sMinX,
                    sMaxX,
                    sourceStepX,
                    destRaster.getElems(),
                    destPos,
                    destRasterIncr);
        } else {
            int inkrement = destRasterPos;
            final double missingValue = productFile.getMissingMDSRPixelValue();
            for (int index = sourceMinX; index <= sourceMaxX; index += sourceStepX) {
                destRaster.setElemDoubleAt(inkrement, missingValue);
                inkrement++;
            }
        }
    }

    /**
     * Reads the record providing the pixels for the line at the given zero-based line index.
     *
     * @param sourceY the line index
     * @throws java.io.IOException if an I/O error occurs
     */
    public synchronized void readLineRecord(int sourceY) throws IOException {
        getPixelDataReader().readRecord(sourceY, getPixelDataRecord());
    }


    private void readDataFieldSegment(int sourceY, int minX, int maxX) throws IOException {
        getPixelDataReader().readFieldSegment(sourceY, fieldOffset, dataFieldSampleSize, minX, maxX, getPixelDataField());
    }

    private long getDataFieldOffset() {
        long offset = 0;
        Record pixelDataRecord = getPixelDataRecord();
        for (int i = 0; i < pixelDataRecord.getNumFields(); i++) {
            Field field = pixelDataRecord.getFieldAt(i);
            if (field == getPixelDataField()) {
                break;
            }
            ProductData data = field.getData();
            offset += data.getElemSize() * data.getNumElems();
        }
        return offset;
    }

    private int getDataFieldSampleSize(BandInfo bandInfo) {
        final int sampleModel = bandInfo.getSampleModel();
        if (sampleModel == BandInfo.SMODEL_1OF1) {
            return 1;
        } else if (sampleModel == BandInfo.SMODEL_1OF2
                || sampleModel == BandInfo.SMODEL_2OF2
                || sampleModel == BandInfo.SMODEL_2UB_TO_S) {
            return 2;
        } else if (sampleModel == BandInfo.SMODEL_3UB_TO_I) {
            return 3;
        }
        throw new IllegalStateException("unknown sample model ID: " + sampleModel); /*I18N*/
    }

    /**
     * Creates a new band decoder appropriate for the band to be read.
     */
    private static BandLineDecoder createBandLineDecoder(Field sourceField, BandInfo bandInfo) {
        BandLineDecoder lineDecoder = null;
        final int sampleModel = bandInfo.getSampleModel();
        final int srcDataType = sourceField.getDataType();
        if (sampleModel == BandInfo.SMODEL_1OF1) {
            if (srcDataType == ProductData.TYPE_INT8 || srcDataType == ProductData.TYPE_UINT8) {
                lineDecoder = new ByteToByteBandDecoder();
            } else if (srcDataType == ProductData.TYPE_INT16 || srcDataType == ProductData.TYPE_UINT16) {
                lineDecoder = new ShortToShortBandDecoder();
            } else if (srcDataType == ProductData.TYPE_INT32 || srcDataType == ProductData.TYPE_UINT32) {
                lineDecoder = new IntToIntBandDecoder();
            } else if (srcDataType == ProductData.TYPE_FLOAT32) {
                lineDecoder = new FloatToFloatBandDecoder();
            } else {
                assert false;
            }
        } else if (sampleModel == BandInfo.SMODEL_1OF2) {
            if (srcDataType == ProductData.TYPE_INT8 || srcDataType == ProductData.TYPE_UINT8) {
                lineDecoder = new Byte1Of2ToByteBandDecoder();
            } else if (srcDataType == ProductData.TYPE_INT16 || srcDataType == ProductData.TYPE_UINT16) {
                lineDecoder = new Short1Of2ToShortBandDecoder();
            } else {
                assert false;
            }
        } else if (sampleModel == BandInfo.SMODEL_2OF2) {
            if (srcDataType == ProductData.TYPE_INT8 || srcDataType == ProductData.TYPE_UINT8) {
                lineDecoder = new Byte2Of2ToByteBandDecoder();
            } else if (srcDataType == ProductData.TYPE_INT16 || srcDataType == ProductData.TYPE_UINT16) {
                lineDecoder = new Short2Of2ToShortBandDecoder();
            } else {
                assert false;
            }
        } else if (sampleModel == BandInfo.SMODEL_2UB_TO_S) {
            assert srcDataType == ProductData.TYPE_INT8 || srcDataType == ProductData.TYPE_UINT8;
            lineDecoder = new UByte12ToShortBandDecoder();
        } else if (sampleModel == BandInfo.SMODEL_3UB_TO_I) {
            assert srcDataType == ProductData.TYPE_INT8 || srcDataType == ProductData.TYPE_UINT8;
            lineDecoder = new UByte123ToIntBandDecoder();
        } else {
            throw new IllegalStateException("unknown sample model ID: " + sampleModel); /*I18N*/
        }

        return lineDecoder;
    }

    static class ByteToByteBandDecoder implements BandLineDecoder {

        public void computeLine(Object sourceArray, int sourceMinX, int sourceMaxX, int sourceStepX,
                                Object rasterArray, int rasterPos, int rasterIncr) {
            final byte[] bytes1 = (byte[]) sourceArray;
            final byte[] bytes2 = (byte[]) rasterArray;
            for (int sourceX = sourceMinX; sourceX <= sourceMaxX; sourceX += sourceStepX) {
                bytes2[rasterPos] = bytes1[sourceX];
                rasterPos += rasterIncr;
            }
        }
    }

    static class ShortToShortBandDecoder implements BandLineDecoder {

        public void computeLine(Object sourceArray, int sourceMinX, int sourceMaxX, int sourceStepX,
                                Object rasterArray, int rasterPos, int rasterIncr) {
            final short[] shorts1 = (short[]) sourceArray;
            final short[] shorts2 = (short[]) rasterArray;
            for (int sourceX = sourceMinX; sourceX <= sourceMaxX; sourceX += sourceStepX) {
                shorts2[rasterPos] = shorts1[sourceX];
                rasterPos += rasterIncr;
            }
        }
    }

    static class FloatToFloatBandDecoder implements BandLineDecoder {

        public void computeLine(Object sourceArray, int sourceMinX, int sourceMaxX, int sourceStepX,
                                Object rasterArray, int rasterPos, int rasterIncr) {
            final float[] floats1 = (float[]) sourceArray;
            final float[] floats2 = (float[]) rasterArray;
            for (int sourceX = sourceMinX; sourceX <= sourceMaxX; sourceX += sourceStepX) {
                floats2[rasterPos] = floats1[sourceX];
                rasterPos += rasterIncr;
            }
        }
    }

    static class IntToIntBandDecoder implements BandLineDecoder {

        public void computeLine(Object sourceArray, int sourceMinX, int sourceMaxX, int sourceStepX,
                                Object rasterArray, int rasterPos, int rasterIncr) {
            final int[] ints1 = (int[]) sourceArray;
            final int[] ints2 = (int[]) rasterArray;
            for (int sourceX = sourceMinX; sourceX <= sourceMaxX; sourceX += sourceStepX) {
                ints2[rasterPos] = ints1[sourceX];
                rasterPos += rasterIncr;
            }
        }
    }

    static class Byte1Of2ToByteBandDecoder implements BandLineDecoder {

        public void computeLine(Object sourceArray, int sourceMinX, int sourceMaxX, int sourceStepX,
                                Object rasterArray, int rasterPos, int rasterIncr) {
            final byte[] bytes1 = (byte[]) sourceArray;
            final byte[] bytes2 = (byte[]) rasterArray;
            for (int sourceX = sourceMinX; sourceX <= sourceMaxX; sourceX += sourceStepX) {
                bytes2[rasterPos] = bytes1[2 * sourceX];
                rasterPos += rasterIncr;
            }
        }
    }

    static class Byte2Of2ToByteBandDecoder implements BandLineDecoder {

        public void computeLine(Object sourceArray, int sourceMinX, int sourceMaxX, int sourceStepX,
                                Object rasterArray, int rasterPos, int rasterIncr) {
            final byte[] bytes1 = (byte[]) sourceArray;
            final byte[] bytes2 = (byte[]) rasterArray;
            for (int sourceX = sourceMinX; sourceX <= sourceMaxX; sourceX += sourceStepX) {
                bytes2[rasterPos] = bytes1[2 * sourceX + 1];
                rasterPos += rasterIncr;
            }
        }
    }


    static class Short1Of2ToShortBandDecoder implements BandLineDecoder {

        public void computeLine(Object sourceArray, int sourceMinX, int sourceMaxX, int sourceStepX,
                                Object rasterArray, int rasterPos, int rasterIncr) {
            final short[] shorts1 = (short[]) sourceArray;
            final short[] shorts2 = (short[]) rasterArray;

            for (int sourceX = sourceMinX; sourceX <= sourceMaxX; sourceX += sourceStepX) {
                shorts2[rasterPos] = shorts1[2 * sourceX];
                rasterPos += rasterIncr;
            }
        }
    }

    static class Short2Of2ToShortBandDecoder implements BandLineDecoder {

        public void computeLine(Object sourceArray, int sourceMinX, int sourceMaxX, int sourceStepX,
                                Object rasterArray, int rasterPos, int rasterIncr) {
            final short[] shorts1 = (short[]) sourceArray;
            final short[] shorts2 = (short[]) rasterArray;

            for (int sourceX = sourceMinX; sourceX <= sourceMaxX; sourceX += sourceStepX) {
                shorts2[rasterPos] = shorts1[2 * sourceX + 1];
                rasterPos += rasterIncr;
            }
        }
    }

    static class UByte12ToShortBandDecoder implements BandLineDecoder {

        public void computeLine(Object sourceArray, int sourceMinX, int sourceMaxX, int sourceStepX,
                                Object rasterArray, int rasterPos, int rasterIncr) {
            final byte[] bytes = (byte[]) sourceArray;
            final short[] shorts = (short[]) rasterArray;
            int i;
            for (int sourceX = sourceMinX; sourceX <= sourceMaxX; sourceX += sourceStepX) {
                i = 2 * sourceX;
                shorts[rasterPos] = (short) (
                        ((bytes[i + 0] & 0xff)) |
                                ((bytes[i + 1] & 0xff) << 8));
                rasterPos += rasterIncr;
            }
        }
    }

    static class UByte123ToIntBandDecoder implements BandLineDecoder {

        public void computeLine(Object sourceArray, int sourceMinX, int sourceMaxX, int sourceStepX,
                                Object rasterArray, int rasterPos, int rasterIncr) {
            final byte[] bytes = (byte[]) sourceArray;
            final int[] ints = (int[]) rasterArray;
            int i;
            for (int sourceX = sourceMinX; sourceX <= sourceMaxX; sourceX += sourceStepX) {
                i = 3 * sourceX;
                ints[rasterPos] =
                        ((bytes[i + 0] & 0xff) << 16) |
                                ((bytes[i + 1] & 0xff) << 8) |
                                ((bytes[i + 2] & 0xff));
                rasterPos += rasterIncr;
            }
        }
    }
}
