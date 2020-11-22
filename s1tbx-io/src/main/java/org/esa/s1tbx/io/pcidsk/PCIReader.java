/*
 * Copyright (C) 2019 by SkyWatch Space Applications Inc. http://www.skywatch.com
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
package org.esa.s1tbx.io.pcidsk;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.s1tbx.commons.io.FileImageInputStreamExtImpl;
import org.esa.s1tbx.commons.io.SARReader;
import org.esa.s1tbx.io.binary.BinaryDBReader;
import org.esa.s1tbx.io.binary.BinaryFileReader;
import org.esa.s1tbx.io.binary.BinaryRecord;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.datamodel.Unit;
import org.jdom2.Document;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * The product reader for PCIDSK products.
 */
public class PCIReader extends SARReader {

    private int numBands = 1;

    private long startPosImageRecords = 0;
    private int imageHeaderLength = 0;
    private final Map<Band, Long> bandStartPosMap = new HashMap<>();
    private final Map<Band, Integer> bandOrder = new HashMap<>();
    private ImageInputStream imageInputStream = null;

    private BinaryFileReader binaryReader = null;
    private final static Document fileHeaderXML = BinaryDBReader.loadDefinitionFile("pcidsk", "fileHeader.xml");
    private final static Document imageHeaderXML = BinaryDBReader.loadDefinitionFile("pcidsk", "imageHeader.xml");
    private final static Document segmentHeaderXML = BinaryDBReader.loadDefinitionFile("pcidsk", "segmentHeader.xml");
    private final static Document geoPolySegmentXML = BinaryDBReader.loadDefinitionFile("pcidsk", "geoPolynomialSegment.xml");

    private enum INTERLEAVE {BAND, PIXEL, FILE}

    private INTERLEAVE bandInterleave = INTERLEAVE.BAND;

    private enum SegmentType {
        BIT(101), VEC(116), SIG(121), TEX(140), GEO(150), LUT(170), PCT(171), BIN(180), ARR(181), GCP(214);

        private final int theValue;

        int value() {
            return theValue;
        }

        SegmentType(int value) {
            theValue = value;
        }
    }

    /**
     * Constructs a new abstract product reader.
     *
     * @param readerPlugIn the reader plug-in which created this reader, can be <code>null</code> for internal reader
     *                     implementations
     */
    public PCIReader(final ProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
    }

    /**
     * Provides an implementation of the <code>readProductNodes</code> interface method. Clients implementing this
     * method can be sure that the input object and eventually the subset information has already been set.
     * <p>
     * <p>This method is called as a last step in the <code>readProductNodes(input, subsetInfo)</code> method.
     *
     * @throws java.io.IOException if an I/O error occurs
     */
    @Override
    protected Product readProductNodesImpl() throws IOException {

        final Path inputPath = getPathFromInput(getInput());

        imageInputStream = FileImageInputStreamExtImpl.createInputStream(inputPath.toFile());
        final BinaryFileReader binaryReader = new BinaryFileReader(imageInputStream);
        final BinaryRecord fileHeaderRecord = new BinaryRecord(binaryReader, -1, fileHeaderXML, "fileHeader.xml");

        startPosImageRecords = (fileHeaderRecord.getAttributeInt("Start block of image data") - 1) * 512;
        final long startPosImageHeaders = (fileHeaderRecord.getAttributeInt("Start block of image headers") - 1) * 512;
        final long startPosSegmentHeaders = (fileHeaderRecord.getAttributeInt("Start block of Segment Pointers") - 1) * 512;
        final int numSegmentBlocks = fileHeaderRecord.getAttributeInt("Number of blocks of Segment Pointers");

        final long rasterWidth = fileHeaderRecord.getAttributeInt("Image size in X (pixel) direction");
        final long rasterHeight = fileHeaderRecord.getAttributeInt("Image size in Y (line ) direction");
        numBands = fileHeaderRecord.getAttributeInt("Number of image channels");
        final String interleaving = fileHeaderRecord.getAttributeString("Interleaving").trim().toUpperCase();
        switch (interleaving) {
            case "BAND":
                bandInterleave = INTERLEAVE.BAND;
                break;
            case "PIXEL":
                bandInterleave = INTERLEAVE.PIXEL;
                break;
            case "FILE":
                bandInterleave = INTERLEAVE.FILE;
                break;
            default:
                throw new IOException(interleaving + " interleaving is not supported by this PCIDSK reader");
        }

        binaryReader.seek(startPosImageHeaders);
        final BinaryRecord[] imgHdrList = new BinaryRecord[numBands];
        for (int i = 0; i < numBands; ++i) {
            imgHdrList[i] = new BinaryRecord(binaryReader, -1, imageHeaderXML, "imageHeader.xml");
        }

        final Product product = new Product(inputPath.getFileName().toString(),
                "PCIDSK",
                (int) rasterWidth, (int) rasterHeight);

        final Long imagSize = rasterWidth * rasterHeight * 4;
        long cnt = 0;
        for (BinaryRecord rec : imgHdrList) {
            String bandName = rec.getAttributeString("Text describing Channel contents").trim();
            if(product.containsBand(bandName)) {
                bandName += "_"+String.valueOf(cnt+1);
            }
            int dataType = getDataType(rec.getAttributeString("Image data type").trim());
            final Band band = new Band(bandName, dataType, (int) rasterWidth, (int) rasterHeight);

            // Currently, "Data measurement units" is not implemented and contains blanks. This code is for the future when
            // it is implemented.
            final String dataUnit = rec.getAttributeString("Data measurement units").trim();
            if (!dataUnit.isEmpty()) {
                band.setUnit(dataUnit);
            }

            product.addBand(band);
            final Long bandOffset = cnt * imagSize;
            bandStartPosMap.put(band, startPosImageRecords + bandOffset);
            bandOrder.put(band, (int) cnt);
            ++cnt;
        }

        binaryReader.seek(startPosSegmentHeaders);
        final int numSegments = (numSegmentBlocks / 32) - 1;
        final BinaryRecord[] segPointList = new BinaryRecord[numSegments];
        for (int i = 0; i < numSegments; ++i) {
            segPointList[i] = new BinaryRecord(binaryReader, -1, segmentHeaderXML, "segmentHeader.xml");
        }

        final BinaryRecord[] segmentList = new BinaryRecord[numSegments];
        int i = 0;
        for (BinaryRecord rec : segPointList) {
            final long startBlock = (rec.getAttributeInt("Start block of segment") - 1) * 512;
            binaryReader.seek(startBlock);
            final int segType = rec.getAttributeInt("Type");
            if (segType == SegmentType.GEO.value()) {
                final BinaryRecord segRec = new BinaryRecord(binaryReader, -1, geoPolySegmentXML, "geoPolynomialSegment.xml");
                segRec.getBinaryDatabase().set("Name", rec.getAttributeString("Name"));
                addGeoCoding(product, segRec);
                segmentList[i++] = segRec;
            }
        }

        addMetaData(product, fileHeaderRecord, imgHdrList, segmentList);

        product.setDescription("PCIDSK");
        product.setProductReader(this);
        product.setModified(false);
        product.setFileLocation(inputPath.toFile());

        return product;
    }

    private int getDataType(final String dataTypeStr) {
        switch (dataTypeStr) {
            case "8":
                return ProductData.TYPE_INT8;
            case "16":
                return ProductData.TYPE_INT16;
            case "32":
                return ProductData.TYPE_INT32;
            case "8U":
                return ProductData.TYPE_UINT8;
            case "16U":
                return ProductData.TYPE_UINT16;
            case "32U":
                return ProductData.TYPE_UINT32;
            default:
                return ProductData.TYPE_FLOAT32;
        }
    }

    @Override
    public void close() throws IOException {
        super.close();
        if (binaryReader != null)
            binaryReader.close();
    }

    private static float[] getGeoCoefficients(final BinaryRecord segRec, final String dir, final int num) {
        final float[] coef = new float[num];
        for (int i = 0; i < num; ++i) {
            coef[i] = segRec.getAttributeDouble(dir + " transform coefficients " + (i + 1)).floatValue();
        }
        return coef;
    }

    private static void addGeoCoding(final Product product, final BinaryRecord segRec) {
        final int numX = segRec.getAttributeInt("Number of X coeficients");
        final int numY = segRec.getAttributeInt("Number of Y coeficients");
        final float[] xCoef = getGeoCoefficients(segRec, "X", numX);
        final float[] yCoef = getGeoCoefficients(segRec, "Y", numY);

        final int gridWidth = 10;
        final int gridHeight = 10;
        final float width = product.getSceneRasterWidth();
        final float height = product.getSceneRasterHeight();
        float subSamplingX = width / (gridWidth - 1);
        float subSamplingY = height / (gridHeight - 1);

        final float[] latTiePoints = new float[gridWidth * gridHeight];
        final float[] lonTiePoints = new float[gridWidth * gridHeight];

        final String coordSys = segRec.getAttributeString("Output coordinate system");
        final StringTokenizer tok = new StringTokenizer(coordSys, " ");
        final String projection = tok.nextToken();
        String zone = "";
        String row = "";
        if (tok.hasMoreTokens() && projection.equalsIgnoreCase("UTM")) {
            zone = tok.nextToken();
            if (tok.hasMoreTokens()) {
                row = tok.nextToken();
            }
        }

        // polynomial affine transform
        UTM2LatLon conv = new UTM2LatLon();
        int k = 0;
        for (float x = 0; x <= width; x += subSamplingX) {
            for (float y = 0; y <= height; y += subSamplingY) {

                //final float newX = xCoef[0] + xCoef[1]*x + xCoef[2]*x*x;
                //final float newY = yCoef[0] + yCoef[1]*y + yCoef[2]*y*y;

                final float newX = xCoef[0] + xCoef[1] * x;
                final float newY = yCoef[0] + yCoef[2] * y;
                if (zone.isEmpty()) {
                    latTiePoints[k] = newY;
                    lonTiePoints[k] = newX;
                } else {
                    final String utmStr = zone + " " + row + " " + newX + " " + newY;
                    final double latlon[] = conv.convertUTMToLatLong(utmStr);
                    latTiePoints[k] = (float) latlon[0];
                    lonTiePoints[k] = (float) latlon[1];
                }
                ++k;
            }
        }

        final TiePointGrid latGrid = new TiePointGrid("latitude", gridWidth, gridHeight, 0.5f, 0.5f,
                subSamplingX, subSamplingY, latTiePoints);
        latGrid.setUnit(Unit.DEGREES);

        final TiePointGrid lonGrid = new TiePointGrid("longitude", gridWidth, gridHeight, 0.5f, 0.5f,
                subSamplingX, subSamplingY, lonTiePoints, TiePointGrid.DISCONT_AT_180);
        lonGrid.setUnit(Unit.DEGREES);

        final TiePointGeoCoding tpGeoCoding = new TiePointGeoCoding(latGrid, lonGrid);

        product.addTiePointGrid(latGrid);
        product.addTiePointGrid(lonGrid);
        product.setSceneGeoCoding(tpGeoCoding);
    }

    private static void addMetaData(final Product product, final BinaryRecord fileRecord,
                                    final BinaryRecord[] imgHdrList,
                                    final BinaryRecord[] segmentList) throws IOException {
        final MetadataElement root = product.getMetadataRoot();
        final MetadataElement absRoot = AbstractMetadata.addAbstractedMetadataHeader(root);

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.PRODUCT, product.getName());
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.PRODUCT_TYPE, product.getProductType());
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.num_samples_per_line, product.getSceneRasterWidth());
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.num_output_lines, product.getSceneRasterHeight());

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.SPH_DESCRIPTOR, fileRecord.getAttributeString("First line of descriptive text"));

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.range_spacing, fileRecord.getAttributeDouble("Size of pixel in X direction"));
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.azimuth_spacing, fileRecord.getAttributeDouble("Size of pixel in Y direction"));

        final MetadataElement headerElem = new MetadataElement("fileHeader");
        fileRecord.assignMetadataTo(headerElem);
        root.addElement(headerElem);

        final MetadataElement imgRootElem = new MetadataElement("ImageRecords");
        root.addElement(imgRootElem);
        for (BinaryRecord rec : imgHdrList) {
            final MetadataElement imgElem = new MetadataElement(rec.getAttributeString("Text describing Channel contents"));
            rec.assignMetadataTo(imgElem);
            imgRootElem.addElement(imgElem);
        }

        for (BinaryRecord rec : segmentList) {
            final MetadataElement segElem = new MetadataElement(rec.getAttributeString("Name"));
            rec.assignMetadataTo(segElem);
            root.addElement(segElem);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight,
                                          int sourceStepX, int sourceStepY, Band destBand, int destOffsetX,
                                          int destOffsetY, int destWidth, int destHeight, ProductData destBuffer,
                                          ProgressMonitor pm) throws IOException {

        if (bandInterleave == INTERLEAVE.BAND) {
            final Long startPos = bandStartPosMap.get(destBand);
            readBandRasterDataBandInterleave(sourceOffsetX, sourceOffsetY,
                    sourceWidth, sourceHeight,
                    sourceStepX, sourceStepY,
                    startPos + imageHeaderLength, imageInputStream,
                    destBand, destWidth, destBuffer, pm);

        } else {
            readBandRasterDataPixelInterleave(sourceOffsetX, sourceOffsetY,
                    sourceWidth, sourceHeight,
                    sourceStepX, sourceStepY,
                    startPosImageRecords, imageInputStream,
                    numBands, destBand, bandOrder.get(destBand), destWidth, destBuffer, pm);
        }
    }

    private static void readBandRasterDataBandInterleave(final int sourceOffsetX, final int sourceOffsetY,
                                                         final int sourceWidth, final int sourceHeight,
                                                         final int sourceStepX, final int sourceStepY,
                                                         final long bandOffset, final ImageInputStream imageInputStream,
                                                         final Band destBand, final int destWidth, final ProductData destBuffer,
                                                         final ProgressMonitor pm) throws IOException {

        final int sourceMinX = sourceOffsetX;
        final int sourceMinY = sourceOffsetY;
        final int sourceMaxX = sourceOffsetX + sourceWidth - 1;
        final int sourceMaxY = sourceOffsetY + sourceHeight - 1;

        final long sourceRasterWidth = destBand.getProduct().getSceneRasterWidth();

        final long elemSize = destBuffer.getElemSize();
        int destPos = 0;

        pm.beginTask("Reading band '" + destBand.getName() + "'...", sourceMaxY - sourceMinY);
        try {
            for (int sourceY = sourceMinY; sourceY <= sourceMaxY; sourceY += sourceStepY) {
                if (pm.isCanceled()) {
                    break;
                }
                final long sourcePosY = sourceY * sourceRasterWidth;
                synchronized (imageInputStream) {
                    if (sourceStepX == 1) {
                        imageInputStream.seek(bandOffset + elemSize * (sourcePosY + sourceMinX));
                        //System.out.println("seek "+bandOffset + elemSize * (sourcePosY + sourceMinX));
                        destBuffer.readFrom(destPos, destWidth, imageInputStream);
                        destPos += destWidth;
                    } else {
                        for (int sourceX = sourceMinX; sourceX <= sourceMaxX; sourceX += sourceStepX) {
                            imageInputStream.seek(bandOffset + elemSize * (sourcePosY + sourceX));
                            destBuffer.readFrom(destPos, 1, imageInputStream);
                            destPos++;
                        }
                    }
                }
                pm.worked(1);
            }
        } finally {
            pm.done();
        }
    }

    private static void readBandRasterDataPixelInterleave(final int sourceOffsetX, final int sourceOffsetY,
                                                          final int sourceWidth, final int sourceHeight,
                                                          final int sourceStepX, final int sourceStepY,
                                                          final long bandOffset, final ImageInputStream imageInputStream,
                                                          final int numBands, final Band destBand, final int destbandOrder,
                                                          final int destWidth, final ProductData destBuffer,
                                                          final ProgressMonitor pm) throws IOException {

        //System.out.println("PCIReader: Reading band " + destBand.getName() + " ...");

        // sourceWidth is not used. It is assumed that it is equal to destWidth
        if (destWidth != sourceWidth) {
            System.out.println("PCIReader: WARNING destWidth = " + destWidth + " sourceWidth = " + sourceWidth);
        }

        // Width (in number of pixels) of the image
        final long sourceRasterWidth = destBand.getProduct().getSceneRasterWidth();

        // sourceOffsetX is in pixels
        if (sourceOffsetX >= sourceRasterWidth) {
            throw new IOException("sourceOffsetX = " + sourceOffsetX + " >= sourceRasterWidth = " + sourceRasterWidth);
        }

        // Size of each element in the binary source data in bytes, e.g., 32-bit float will be elemSize 4
        final long elemSize = destBuffer.getElemSize(); // bytes

        // It is assumed that the destination array is float
        int destPos = 0;
        final float[] destArray = (float[]) destBuffer.getElems();

        // The number of bytes in each row must be a multiple of 512 which is the block size, even if raster width is such
        // that it does not work out that way. The excess bytes at the end of the row is not used.
        // E.g., if numBands = 3 and sourceRasterWidth = 4, then one row would look like this...
        // e1 e2 e3 e1 e2 e3 e1 e2 e3 e1 e2 e3
        // where e1 is element1, e2 is element2, e3 is element 3.
        long rowSizeBytes = elemSize * sourceRasterWidth * numBands;
        if (rowSizeBytes % 512 != 0) {
            rowSizeBytes = ((rowSizeBytes / 512) + 1) * 512;
        }
        final long xOffsetBytes = elemSize * sourceOffsetX * numBands;

        pm.beginTask("Reading band '" + destBand.getName() + "'...", sourceHeight);
        try {
            for (int sourceY = sourceOffsetY; sourceY < sourceOffsetY + sourceHeight; sourceY += sourceStepY) {
                if (pm.isCanceled()) {
                    break;
                }

                final long seekPos = (sourceY * rowSizeBytes) + xOffsetBytes;

                final float[] array = new float[destWidth * numBands * sourceStepX];
                //imageInputStream.setByteOrder(ByteOrder.LITTLE_ENDIAN);

                synchronized (imageInputStream) {

                    imageInputStream.seek(bandOffset + seekPos + destbandOrder * elemSize);

                    imageInputStream.readFully(array, 0, array.length);

                    for (int i = 0; i < array.length; i += numBands * sourceStepX) {

                        destArray[destPos++] = array[i];
                    }
                    //System.out.println("dest=" + destArray.length + " array=" + array.length);
                }
                pm.worked(1);
            }
        } finally {
            pm.done();
        }
    }

}