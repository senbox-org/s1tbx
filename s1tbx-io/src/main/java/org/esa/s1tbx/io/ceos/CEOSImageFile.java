/*
 * Copyright (C) 2015 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.s1tbx.io.ceos;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.s1tbx.io.binary.BinaryFileReader;
import org.esa.s1tbx.io.binary.BinaryRecord;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.engine_utilities.eo.Constants;

import java.io.IOException;


/**
 * This class represents an image file of a CEOS product.
 *
 * @version $Revision: 1.3 $ $Date: 2012-01-10 21:22:00 $
 */
public abstract class CEOSImageFile {

    protected BinaryRecord imageFDR = null;
    protected BinaryFileReader binaryReader = null;
    protected BinaryRecord[] imageRecords = null;

    protected long _imageRecordLength = 0;
    protected long startPosImageRecords = 0;
    protected int imageHeaderLength = 0;

    public BinaryRecord getImageFileDescriptor() {
        return imageFDR;
    }

    public int getRasterWidth() {
        int width = imageFDR.getAttributeInt("Number of pixels per line per SAR channel");
        if (width == 0)
            width = imageFDR.getAttributeInt("SAR DATA record length");
        return width;
    }

    public int getRasterHeight() {
        return imageFDR.getAttributeInt("Number of lines per data set");
    }

    public int getBitsPerSample() {
        return imageFDR.getAttributeInt("Number of bits per sample");
    }

    public int getSamplesPerDataGroup() {
        return imageFDR.getAttributeInt("Number of samples per data group");
    }

    protected abstract BinaryRecord createNewImageRecord(final int line) throws IOException;

    BinaryRecord getImageRecord(int line) throws IOException {
        if (imageRecords[line] == null) {

            binaryReader.seek(imageFDR.getAbsolutPosition(imageFDR.getRecordLength()));
            imageRecords[line] = createNewImageRecord(line);
        }
        return imageRecords[line];
    }

    public double getSlantRangeToFirstPixel(int line) {
        try {
            final BinaryRecord imgRec = getImageRecord(line);
            return imgRec.getAttributeInt("Slant range to 1st pixel");
        } catch (Exception e) {
            return 0;
        }
    }

    public double getSlantRangeToMidPixel(int line) {
        try {
            final BinaryRecord imgRec = getImageRecord(line);
            return imgRec.getAttributeInt("Slant range to mid-pixel");
        } catch (Exception e) {
            return 0;
        }
    }

    public double getSlantRangeToLastPixel(int line) {
        try {
            final BinaryRecord imgRec = getImageRecord(line);
            return imgRec.getAttributeInt("Slant range to last pixel");
        } catch (Exception e) {
            return 0;
        }
    }

    public float[] getLatCorners() {
        try {
            final BinaryRecord imgRec0 = getImageRecord(0);
            final BinaryRecord imgRecN = getImageRecord(imageRecords.length - 1);

            final float latUL = imgRec0.getAttributeInt("First pixel latitude") / (float) Constants.oneMillion;
            final float latUR = imgRec0.getAttributeInt("Last pixel latitude") / (float) Constants.oneMillion;
            final float latLL = imgRecN.getAttributeInt("First pixel latitude") / (float) Constants.oneMillion;
            final float latLR = imgRecN.getAttributeInt("Last pixel latitude") / (float) Constants.oneMillion;
            return new float[]{latUL, latUR, latLL, latLR};
        } catch (Throwable e) {
            return null;
        }
    }

    public float[] getLonCorners() {
        try {
            final BinaryRecord imgRec0 = getImageRecord(0);
            final BinaryRecord imgRecN = getImageRecord(imageRecords.length - 1);

            final float lonUL = imgRec0.getAttributeInt("First pixel longitude") / (float) Constants.oneMillion;
            final float lonUR = imgRec0.getAttributeInt("Last pixel longitude") / (float) Constants.oneMillion;
            final float lonLL = imgRecN.getAttributeInt("First pixel longitude") / (float) Constants.oneMillion;
            final float lonLR = imgRecN.getAttributeInt("Last pixel longitude") / (float) Constants.oneMillion;
            return new float[]{lonUL, lonUR, lonLL, lonLR};
        } catch (Throwable e) {
            return null;
        }
    }

    public void assignMetadataTo(MetadataElement rootElem, int count) {
        final MetadataElement imgDescElem = new MetadataElement("Image Descriptor " + count);
        imageFDR.assignMetadataTo(imgDescElem);
        rootElem.addElement(imgDescElem);

        if (imageRecords.length > 0 && imageRecords[0] != null) {
            final MetadataElement imgRecElem = new MetadataElement("Image Record ");
            imageRecords[0].assignMetadataTo(imgRecElem);
            imgDescElem.addElement(imgRecElem);
        }
    }

    public void readBandRasterDataShort(final int sourceOffsetX, final int sourceOffsetY,
                                        final int sourceWidth, final int sourceHeight,
                                        final int sourceStepX, final int sourceStepY,
                                        final int destWidth, final ProductData destBuffer, ProgressMonitor pm) {
        final int sourceMaxY = sourceOffsetY + sourceHeight - 1;
        final int x = sourceOffsetX * ProductData.getElemSize(destBuffer.getType());
        final long xpos = startPosImageRecords + imageHeaderLength + x;

        pm.beginTask("Reading band...", sourceMaxY - sourceOffsetY);
        try {
            final short[] srcLine = new short[sourceWidth];
            short[] destLine = null;
            if (sourceStepX != 1)
                destLine = new short[destWidth];
            for (int y = sourceOffsetY; y <= sourceMaxY; y += sourceStepY) {
                if (pm.isCanceled()) {
                    break;
                }

                // Read source line
                synchronized (binaryReader) {
                    binaryReader.seek(_imageRecordLength * y + xpos);
                    binaryReader.read(srcLine);
                }

                // Copy source line into destination buffer
                final int currentLineIndex = (y - sourceOffsetY) * destWidth;
                if (sourceStepX == 1) {

                    System.arraycopy(srcLine, 0, destBuffer.getElems(), currentLineIndex, destWidth);
                } else {
                    copyLine(srcLine, destLine, sourceStepX);

                    System.arraycopy(destLine, 0, destBuffer.getElems(), currentLineIndex, destWidth);
                }

                pm.worked(1);
            }
        } catch (Throwable e) {
            //e.printStackTrace();
        } finally {
            pm.done();
        }
    }

    public void readBandRasterDataInt(final int sourceOffsetX, final int sourceOffsetY,
                                      final int sourceWidth, final int sourceHeight,
                                      final int sourceStepX, final int sourceStepY,
                                      final int destWidth, final ProductData destBuffer,
                                      final ProgressMonitor pm)
            throws IOException {
        final int sourceMaxY = sourceOffsetY + sourceHeight - 1;
        final int x = sourceOffsetX * ProductData.getElemSize(destBuffer.getType());
        final long xpos = startPosImageRecords + imageHeaderLength + x;

        pm.beginTask("Reading band...", sourceMaxY - sourceOffsetY);
        try {
            final int[] srcLine = new int[sourceWidth];
            int[] destLine = null;
            if (sourceStepX != 1)
                destLine = new int[destWidth];
            for (int y = sourceOffsetY; y <= sourceMaxY; y += sourceStepY) {
                if (pm.isCanceled()) {
                    break;
                }

                // Read source line
                synchronized (binaryReader) {
                    binaryReader.seek(_imageRecordLength * y + xpos);
                    binaryReader.read(srcLine);
                }

                // Copy source line into destination buffer
                final int currentLineIndex = (y - sourceOffsetY) * destWidth;
                if (sourceStepX == 1) {

                    System.arraycopy(srcLine, 0, destBuffer.getElems(), currentLineIndex, destWidth);
                } else {
                    copyLine(srcLine, destLine, sourceStepX);

                    System.arraycopy(destLine, 0, destBuffer.getElems(), currentLineIndex, destWidth);
                }
                pm.worked(1);
            }
        } finally {
            pm.done();
        }
    }

    public void readBandRasterDataFloat(final int sourceOffsetX, final int sourceOffsetY,
                                        final int sourceWidth, final int sourceHeight,
                                        final int sourceStepX, final int sourceStepY,
                                        final int destWidth, final ProductData destBuffer,
                                        final ProgressMonitor pm)
            throws IOException {
        final int sourceMaxY = sourceOffsetY + sourceHeight - 1;
        final int x = sourceOffsetX * ProductData.getElemSize(destBuffer.getType());
        final long xpos = startPosImageRecords + imageHeaderLength + x;

        pm.beginTask("Reading band...", sourceMaxY - sourceOffsetY);
        try {
            final float[] srcLine = new float[sourceWidth];
            float[] destLine = null;
            if (sourceStepX != 1)
                destLine = new float[destWidth];
            for (int y = sourceOffsetY; y <= sourceMaxY; y += sourceStepY) {
                if (pm.isCanceled()) {
                    break;
                }

                // Read source line
                synchronized (binaryReader) {
                    binaryReader.seek(_imageRecordLength * y + xpos);
                    binaryReader.read(srcLine);
                }

                // Copy source line into destination buffer
                final int currentLineIndex = (y - sourceOffsetY) * destWidth;
                if (sourceStepX == 1) {

                    System.arraycopy(srcLine, 0, destBuffer.getElems(), currentLineIndex, destWidth);
                } else {
                    copyLine(srcLine, destLine, sourceStepX);

                    System.arraycopy(destLine, 0, destBuffer.getElems(), currentLineIndex, destWidth);
                }
                pm.worked(1);
            }
        } finally {
            pm.done();
        }
    }

    public void readBandRasterDataByte(final int sourceOffsetX, final int sourceOffsetY,
                                       final int sourceWidth, final int sourceHeight,
                                       final int sourceStepX, final int sourceStepY,
                                       final int destWidth, final ProductData destBuffer, ProgressMonitor pm)
            throws IOException {
        final int sourceMaxY = sourceOffsetY + sourceHeight - 1;
        final int x = sourceOffsetX * ProductData.getElemSize(destBuffer.getType());
        final long xpos = startPosImageRecords + imageHeaderLength + x;

        pm.beginTask("Reading band...", sourceMaxY - sourceOffsetY);
        try {
            final byte[] srcLine = new byte[sourceWidth];
            byte[] destLine = null;
            if (sourceStepX != 1)
                destLine = new byte[destWidth];
            for (int y = sourceOffsetY; y <= sourceMaxY; y += sourceStepY) {
                if (pm.isCanceled()) {
                    break;
                }

                // Read source line
                synchronized (binaryReader) {
                    binaryReader.seek(_imageRecordLength * y + xpos);
                    binaryReader.read(srcLine);
                }

                // Copy source line into destination buffer
                final int currentLineIndex = (y - sourceOffsetY) * destWidth;
                if (sourceStepX == 1) {

                    System.arraycopy(srcLine, 0, destBuffer.getElems(), currentLineIndex, destWidth);
                } else {
                    copyLine(srcLine, destLine, sourceStepX);

                    System.arraycopy(destLine, 0, destBuffer.getElems(), currentLineIndex, destWidth);
                }

                pm.worked(1);
            }

        } finally {
            pm.done();
        }
    }

    public void readBandRasterDataSLC(final int sourceOffsetX, final int sourceOffsetY,
                                      final int sourceWidth, final int sourceHeight,
                                      final int sourceStepX, final int sourceStepY,
                                      final int destWidth, final ProductData destBuffer, boolean oneOf2,
                                      final int elemSize)  {
        final int sourceMaxY = sourceOffsetY + sourceHeight - 1;
        final int x = sourceOffsetX * elemSize;
        final long xpos = startPosImageRecords + imageHeaderLength + x;

        try {
            final short[] srcLine = new short[sourceWidth * 2];
            for (int y = sourceOffsetY; y <= sourceMaxY; y += sourceStepY) {

                /*
                synchronized (binaryReader) {
                    Integer cacheIndex = indexMap.get(y);
                    if(cacheIndex == null) {
                            if (cachePos >= cacheSize) {
                                cachePos = 0;
                            }

                            srcLine = new short[sourceWidth * 2];
                            binaryReader.seek(_imageRecordLength * y + xpos);
                            binaryReader.read(srcLine);

                            if(linesCache[cachePos] != null) {
                                indexMap.put(indexList[cachePos], null);
                            }
                            linesCache[cachePos] = srcLine;
                            indexList[cachePos] = y;
                            indexMap.put(y, cachePos);
                            ++cachePos;
                    } else {
                        srcLine = (short[]) linesCache[cacheIndex];
                    }
                }   */

                // Read source line
                synchronized (binaryReader) {
                    binaryReader.seek(_imageRecordLength * y + xpos);
                    binaryReader.read(srcLine);
                }

                // Copy source line into destination buffer
                final int currentLineIndex = (y - sourceOffsetY) * destWidth;
                if (oneOf2)
                    copyLine1Of2(srcLine, destBuffer, currentLineIndex, sourceStepX);
                else
                    copyLine2Of2(srcLine, destBuffer, currentLineIndex, sourceStepX);

            }
        } catch (Throwable e) {
            System.out.println(e.getMessage());
        }
    }

    public synchronized void readBandRasterDataSLCFloat(final int sourceOffsetX, final int sourceOffsetY,
                                           final int sourceWidth, final int sourceHeight,
                                           final int sourceStepX, final int sourceStepY,
                                           final int destWidth, final ProductData destBuffer, boolean oneOf2,
                                           ProgressMonitor pm) throws IOException {
        final int sourceMaxY = sourceOffsetY + sourceHeight - 1;
        final int x = sourceOffsetX * 8;
        final long xpos = startPosImageRecords + imageHeaderLength + x;

        pm.beginTask("Reading band...", sourceMaxY - sourceOffsetY);
        try {
            final float[] srcLine = new float[sourceWidth * 2];
            final float[] destLine = new float[destWidth];
            for (int y = sourceOffsetY; y <= sourceMaxY; y += sourceStepY) {
                // Read source line
                //synchronized (binaryReader) {
                    binaryReader.seek(_imageRecordLength * y + xpos);
                    binaryReader.read(srcLine);
                //}

                // Copy source line into destination buffer
                final int currentLineIndex = (y - sourceOffsetY) * destWidth;
                if (oneOf2)
                    copyLine1Of2(srcLine, destLine, sourceStepX);
                else
                    copyLine2Of2(srcLine, destLine, sourceStepX);

                System.arraycopy(destLine, 0, destBuffer.getElems(), currentLineIndex, destWidth);
            }
        } finally {
            pm.done();
        }
    }

    public void readBandRasterDataSLCByte(final int sourceOffsetX, final int sourceOffsetY,
                                          final int sourceWidth, final int sourceHeight,
                                          final int sourceStepX, final int sourceStepY,
                                          final int destWidth, final ProductData destBuffer, boolean oneOf2,
                                          ProgressMonitor pm) throws IOException {
        final int sourceMaxY = sourceOffsetY + sourceHeight - 1;
        final int x = sourceOffsetX * 2;
        final long xpos = startPosImageRecords + imageHeaderLength + x;

        pm.beginTask("Reading band...", sourceMaxY - sourceOffsetY);
        try {
            final byte[] srcLine = new byte[sourceWidth * 2];
            final byte[] destLine = new byte[destWidth];
            for (int y = sourceOffsetY; y <= sourceMaxY; y += sourceStepY) {
                if (pm.isCanceled()) {
                    break;
                }

                // Read source line
                synchronized (binaryReader) {
                    binaryReader.seek(_imageRecordLength * y + xpos);
                    binaryReader.read(srcLine);
                }

                // Copy source line into destination buffer
                final int currentLineIndex = (y - sourceOffsetY) * destWidth;
                if (oneOf2)
                    copyLine1Of2(srcLine, destLine, sourceStepX);
                else
                    copyLine2Of2(srcLine, destLine, sourceStepX);

                System.arraycopy(destLine, 0, destBuffer.getElems(), currentLineIndex, destWidth);

                pm.worked(1);
            }

        } finally {
            pm.done();
        }
    }

    private static void copyLine(final short[] srcLine, final short[] destLine, final int sourceStepX) {
        for (int x = 0, i = 0; x < destLine.length; ++x, i += sourceStepX) {
            destLine[x] = srcLine[i];
        }
    }

    private static void copyLine(final byte[] srcLine, final byte[] destLine, final int sourceStepX) {
        for (int x = 0, i = 0; x < destLine.length; ++x, i += sourceStepX) {
            destLine[x] = srcLine[i];
        }
    }

    private static void copyLine(final int[] srcLine, final int[] destLine, final int sourceStepX) {
        for (int x = 0, i = 0; x < destLine.length; ++x, i += sourceStepX) {
            destLine[x] = srcLine[i];
        }
    }

    private static void copyLine(final float[] srcLine, final float[] destLine, final int sourceStepX) {
        for (int x = 0, i = 0; x < destLine.length; ++x, i += sourceStepX) {
            destLine[x] = srcLine[i];
        }
    }

    private static void copyLine1Of2(final short[] srcLine, final ProductData destBuffer,
                                     final int currentLineIndex, final int sourceStepX) {
        final int destLength = destBuffer.getNumElems();
        final int srcLength = srcLine.length / 2;
        for (int x = currentLineIndex, i = 0; x < destLength && i < srcLength; ++x, i += sourceStepX) {
            destBuffer.setElemDoubleAt(x, srcLine[i << 1]);
        }
    }

    private static void copyLine1Of2(final short[] srcLine, final short[] destLine, final int sourceStepX) {
        for (int x = 0, i = 0; x < destLine.length; ++x, i += sourceStepX) {
            destLine[x] = srcLine[i << 1];
        }
    }

    private static void copyLine1Of2(final byte[] srcLine, final byte[] destLine, final int sourceStepX) {
        for (int x = 0, i = 0; x < destLine.length; ++x, i += sourceStepX) {
            destLine[x] = srcLine[i << 1];
        }
    }

    private static void copyLine1Of2(final float[] srcLine, final float[] destLine, final int sourceStepX) {
        for (int x = 0, i = 0; x < destLine.length; ++x, i += sourceStepX) {
            destLine[x] = srcLine[i << 1];
        }
    }

    private static void copyLine2Of2(final short[] srcLine, final ProductData destBuffer,
                                     final int currentLineIndex, final int sourceStepX) {
        final int destLength = destBuffer.getNumElems();
        final int srcLength = srcLine.length / 2;
        for (int x = currentLineIndex, i = 0; x < destLength && i < srcLength; ++x, i += sourceStepX) {
            destBuffer.setElemDoubleAt(x, srcLine[(i << 1) + 1]);
        }
    }

    private static void copyLine2Of2(final short[] srcLine, final short[] destLine, final int sourceStepX) {
        final int length = destLine.length;
        for (int x = 0, i = 0; x < length; ++x, i += sourceStepX) {
            destLine[x] = srcLine[(i << 1) + 1];
        }
    }

    private static void copyLine2Of2(final byte[] srcLine, final byte[] destLine, final int sourceStepX) {
        final int length = destLine.length;
        for (int x = 0, i = 0; x < length; ++x, i += sourceStepX) {
            destLine[x] = srcLine[(i << 1) + 1];
        }
    }

    private static void copyLine2Of2(final float[] srcLine, final float[] destLine, final int sourceStepX) {
        final int length = destLine.length;
        for (int x = 0, i = 0; x < length; ++x, i += sourceStepX) {
            destLine[x] = srcLine[(i << 1) + 1];
        }
    }

    public void close() throws IOException {
        binaryReader.close();
        binaryReader = null;
    }
}
