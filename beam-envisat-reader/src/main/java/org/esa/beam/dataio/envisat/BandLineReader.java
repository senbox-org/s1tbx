/*
 * $Id: BandLineReader.java,v 1.1 2006/09/18 06:34:32 marcop Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.dataio.envisat;

import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.util.Debug;

import java.io.IOException;
import java.util.Arrays;

/**
 * A <code>BandLineReader</code> instance is used read and decode single lines of the geophysical raster data stored in
 * ENVISAT product files.
 * <p/>
 * <p> Band line reader instances are obtained through the <code>getBandLineReader</code> method of the
 * <code>ProductFile</code> class:
 * <pre>
 *    ProductFile file = ProductFile.open("MERIS_L2.prd");
 *    BandLineReader reader = file.getBandLineReader("radiance_9");
 * </pre>
 * <p/>
 * <p> Band readers internally use a <code>RecordReader</code> to read the raw data records providing the pixel data for
 * a certain geophysical parameter.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 * @see org.esa.beam.dataio.envisat.ProductFile#getBandLineReader
 * @see org.esa.beam.dataio.envisat.RecordReader
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

        public boolean isTiePointBased() {
            return false;
        }

        public BandLineDecoder ensureBandLineDecoder() {
            throw new IllegalStateException();
        }

        public void readLineRecord(int sourceY) throws IOException {
            throw new IllegalStateException();
        }

        public ProductFile getProductFile() {
            throw new IllegalStateException();
        }

        public void readRasterLine(int sourceMinX, int sourceMaxX, int sourceStepX, int sourceY,
                                   ProductData destRaster, int destRasterPos) throws IOException {
            throw new IllegalStateException();
        }

        public int getRasterWidth() {
            throw new IllegalStateException();
        }

        public int getRasterHeight() {
            throw new IllegalStateException();
        }

        public Record getPixelDataRecord() {
            throw new IllegalStateException();
        }

        public RecordReader getPixelDataReader() {
            throw new IllegalStateException();
        }

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
     * <p/>
     * <p>The method reads the sample values of the line <code>sourceY</code> from column <code>sourceMinX</code> to
     * <code>sourceMaxX</code> inclusively with a sub-sampling of <code>sourceStepX</code> pixels into the specified
     * raster data buffer <code>destRaster</code> beginning at offset <code>destRasterPos</code>.
     * <p/>
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
    public synchronized void readRasterLine(int sourceMinX,
                                            int sourceMaxX,
                                            int sourceStepX,
                                            int sourceY,
                                            ProductData destRaster,
                                            int destRasterPos) throws IOException {
        final ProductFile productFile = getProductFile();
        final int mappedMdsrIndex = productFile.getMappedMDSRIndex(sourceY);
        if (mappedMdsrIndex >= 0 && mappedMdsrIndex <= _maxRecordIndex) {
            readLineRecord(sourceY);

            int destRasterIncr = 1;
            if (productFile.storesPixelsInChronologicalOrder()) {
                destRasterIncr = -1;
                destRasterPos += (sourceMaxX - sourceMinX) / sourceStepX;
                int oldSourceMinX = sourceMinX;
                sourceMinX = productFile.getSceneRasterWidth() - 1 - sourceMaxX;
                sourceMaxX = productFile.getSceneRasterWidth() - 1 - oldSourceMinX;
            }

            ensureBandLineDecoder().computeLine(getPixelDataField().getElems(),
                    sourceMinX,
                    sourceMaxX,
                    sourceStepX,
                    destRaster.getElems(),
                    destRasterPos,
                    destRasterIncr);
        } else {
            final double missingValue = productFile.getMissingMDSRPixelValue();
            for (int index = sourceMinX; index <= sourceMaxX; index += sourceStepX) {
                destRaster.setElemDoubleAt(destRasterPos, missingValue);
                destRasterPos++;
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
        getPixelDataReader().readRecord(sourceY,
                getPixelDataRecord());
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
