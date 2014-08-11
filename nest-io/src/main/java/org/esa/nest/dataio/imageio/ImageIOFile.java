/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.nest.dataio.imageio;

import it.geosolutions.imageioimpl.plugins.tiff.TIFFImageReader;
import org.esa.beam.framework.datamodel.*;
import org.esa.snap.datamodel.Unit;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.*;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

/**
 * Reader for ImageIO File
 */
public class ImageIOFile {

    private final String name;
    private int sceneWidth = 0;
    private int sceneHeight = 0;
    private int dataType;
    private int numImages = 1;
    private int numBands = 1;
    private ImageInfo imageInfo = null;
    private IndexCoding indexCoding = null;
    private boolean isIndexed = false;

    private ImageInputStream stream = null;
    private ImageReader reader;

    public ImageIOFile(final File inputFile, final ImageReader iioReader) throws IOException {

        name = inputFile.getName();
        stream = ImageIO.createImageInputStream(inputFile);
        if (stream == null)
            throw new IOException("Unable to open "+name);

        createReader(iioReader);
    }

    public ImageIOFile(final String name, final ImageInputStream inputStream, final ImageReader iioReader) throws IOException {

        this.name = name;
        this.stream = inputStream;
        if (stream == null)
            throw new IOException("Unable to open ");

        createReader(iioReader);
    }

    public ImageIOFile(final String name, final ImageInputStream inputStream, final ImageReader iioReader,
                       final int numImages, final int numBands, final int dataType) throws IOException {

        this.name = name;
        this.stream = inputStream;
        if (stream == null)
            throw new IOException("Unable to open ");

        reader = iioReader;
        reader.setInput(stream);

        this.numImages = numImages;
        this.numBands = numBands;
        this.dataType = dataType;
    }

    private synchronized void createReader(final ImageReader iioReader) throws IOException {

        reader = iioReader;
        reader.setInput(stream);

        numImages = reader.getNumImages(true);
        numBands = 3;

        dataType = ProductData.TYPE_INT32;
        final ImageTypeSpecifier its = reader.getRawImageType(0);
        if (its != null) {
            numBands = reader.getRawImageType(0).getNumBands();
            dataType = bufferImageTypeToProductType(its.getBufferedImageType());

            if (its.getBufferedImageType() == BufferedImage.TYPE_BYTE_INDEXED) {
                isIndexed = true;
                createIndexedImageInfo(its.getColorModel());
            }
        }
    }

    public String getName() { return name; }

    public static ImageReader getIIOReader(final File inputFile) throws IOException {
        final ImageInputStream stream = ImageIO.createImageInputStream(inputFile);
        if (stream == null)
            throw new IOException("Unable to open " + inputFile.toString());

        final Iterator<ImageReader> imageReaders = ImageIO.getImageReaders(stream);
        if (!imageReaders.hasNext())
            throw new IOException("No ImageIO reader found for " + inputFile.toString());

        return imageReaders.next();
    }

    public static ImageReader getTiffIIOReader(final File inputFile) throws IOException {
        final ImageInputStream stream = ImageIO.createImageInputStream(inputFile);
        if (stream == null)
            throw new IOException("Unable to open " + inputFile.toString());
        return getTiffIIOReader(stream);
    }

    public static ImageReader getTiffIIOReader(final ImageInputStream stream) throws IOException {
        ImageReader reader = null;
        final Iterator<ImageReader> imageReaders = ImageIO.getImageReaders(stream);
        while (imageReaders.hasNext()) {
            final ImageReader iioReader = imageReaders.next();
            if (iioReader instanceof TIFFImageReader) {
                reader = iioReader;
                break;
            }
        }
        if (reader == null)
            throw new IOException("Unable to open " + stream.toString());
        return reader;
    }

    public ImageReader getReader() throws IOException {
        if (reader == null) {
            throw new IOException("no reader created");
        }
        return reader;
    }

    private static int bufferImageTypeToProductType(int biType) {
        switch (biType) {
            case BufferedImage.TYPE_CUSTOM:
            case BufferedImage.TYPE_INT_RGB:
            case BufferedImage.TYPE_INT_ARGB:
            case BufferedImage.TYPE_INT_ARGB_PRE:
            case BufferedImage.TYPE_INT_BGR:
                return ProductData.TYPE_INT32;
            case BufferedImage.TYPE_3BYTE_BGR:
            case BufferedImage.TYPE_4BYTE_ABGR:
            case BufferedImage.TYPE_4BYTE_ABGR_PRE:
                return ProductData.TYPE_INT16;
            case BufferedImage.TYPE_USHORT_565_RGB:
            case BufferedImage.TYPE_USHORT_555_RGB:
            case BufferedImage.TYPE_USHORT_GRAY:
                return ProductData.TYPE_UINT16;
            case BufferedImage.TYPE_BYTE_GRAY:
            case BufferedImage.TYPE_BYTE_BINARY:
            case BufferedImage.TYPE_BYTE_INDEXED:
                return ProductData.TYPE_INT8;
        }
        return ProductData.TYPE_UNDEFINED;
    }

    final void createIndexedImageInfo(ColorModel colorModel) {
        final IndexColorModel indexColorModel = (IndexColorModel) colorModel;
        indexCoding = new IndexCoding("color_map");
        final int colorCount = indexColorModel.getMapSize();
        final ColorPaletteDef.Point[] points = new ColorPaletteDef.Point[colorCount];
        for (int j = 0; j < colorCount; j++) {
            final String name = "I%3d";
            indexCoding.addIndex(String.format(name, j), j, "");
            points[j] = new ColorPaletteDef.Point(j, new Color(indexColorModel.getRGB(j)), name);
        }

        imageInfo = new ImageInfo(new ColorPaletteDef(points, points.length));
    }

    public boolean isIndexed() {
        return isIndexed;
    }

    public IndexCoding getIndexCoding() {
        return indexCoding;
    }

    public ImageInfo getImageInfo() {
        return imageInfo;
    }

    public void close() throws IOException {
        if (stream != null)
            stream.close();
        if (reader != null)
            reader.dispose();
    }

    public int getSceneWidth() throws IOException {
        if (sceneWidth == 0) {
            sceneWidth = reader.getWidth(0);
        }
        return sceneWidth;
    }

    public int getSceneHeight() throws IOException {
        if (sceneHeight == 0) {
            sceneHeight = reader.getHeight(0);
        }
        return sceneHeight;
    }

    public int getDataType() {
        return dataType;
    }

    public int getNumImages() {
        return numImages;
    }

    public int getNumBands() {
        return numBands;
    }

    public void readImageIORasterBand(final int sourceOffsetX, final int sourceOffsetY,
                                      final int sourceStepX, final int sourceStepY,
                                      final ProductData destBuffer,
                                      final int destOffsetX, final int destOffsetY,
                                      final int destWidth, final int destHeight,
                                      final int imageID,
                                      final int bandSampleOffset) throws IOException {
        final ImageReadParam param = reader.getDefaultReadParam();
        param.setSourceSubsampling(sourceStepX, sourceStepY,
                sourceOffsetX % sourceStepX,
                sourceOffsetY % sourceStepY);
        final Raster data = getData(param, destOffsetX, destOffsetY, destWidth, destHeight);


        final DataBuffer dataBuffer = data.getDataBuffer();
        final SampleModel sampleModel = data.getSampleModel();
        final int dataBufferType = dataBuffer.getDataType();
        final int sampleOffset = imageID + bandSampleOffset;
        final Object dest = destBuffer.getElems();

        if (dest instanceof int[] && (dataBufferType == DataBuffer.TYPE_USHORT || dataBufferType == DataBuffer.TYPE_SHORT
                || dataBufferType == DataBuffer.TYPE_INT)) {
            sampleModel.getSamples(0, 0, destWidth, destHeight, sampleOffset, (int[]) dest, dataBuffer);
        } else if (dataBufferType == DataBuffer.TYPE_FLOAT && dest instanceof float[]) {
            sampleModel.getSamples(0, 0, destWidth, destHeight, sampleOffset, (float[]) dest, dataBuffer);
        } else if (dataBufferType == DataBuffer.TYPE_DOUBLE && dest instanceof double[]) {
            sampleModel.getSamples(0, 0, destWidth, destHeight, sampleOffset, (double[]) dest, dataBuffer);
        } else {
            final double[] dArray = new double[destWidth * destHeight];
            sampleModel.getSamples(0, 0, data.getWidth(), data.getHeight(), sampleOffset, dArray, dataBuffer);

            int i = 0;
            for (double value : dArray) {
                destBuffer.setElemDoubleAt(i++, value);
            }
        }
    }

    private synchronized Raster getData(final ImageReadParam param,
                                        final int destOffsetX, final int destOffsetY,
                                        final int destWidth, final int destHeight) throws IOException {
        final RenderedImage image = reader.readAsRenderedImage(0, param);
        return image.getData(new Rectangle(destOffsetX, destOffsetY, destWidth, destHeight));
    }

    public static class BandInfo {
        public final int imageID;
        public final int bandSampleOffset;
        public final ImageIOFile img;
        public boolean isImaginary = false;

        public BandInfo(final Band band, final ImageIOFile imgFile, final int id, final int offset) {
            img = imgFile;
            imageID = id;
            bandSampleOffset = offset;
            isImaginary = band.getUnit() != null && band.getUnit().equals(Unit.IMAGINARY);
        }
    }
}