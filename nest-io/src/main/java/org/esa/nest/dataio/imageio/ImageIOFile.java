/*
 * Copyright (C) 2012 by Array Systems Computing Inc. http://www.array.ca
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

import com.sun.media.imageioimpl.plugins.tiff.TIFFImageReader;
import org.esa.beam.framework.datamodel.ColorPaletteDef;
import org.esa.beam.framework.datamodel.ImageInfo;
import org.esa.beam.framework.datamodel.IndexCoding;
import org.esa.beam.framework.datamodel.ProductData;

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
 *
 */
public class ImageIOFile {

    private int sceneWidth = 0;
    private int sceneHeight = 0;
    private int dataType;
    private final int numImages;
    private int numBands;
    private final String name;
    private ImageInfo imageInfo = null;
    private IndexCoding indexCoding = null;
    private boolean isIndexed = false;

    private ImageInputStream stream = null;
    private ImageReader reader = null;

    public static ImageReader getIIOReader(final File inputFile) throws IOException {
        final ImageInputStream stream = ImageIO.createImageInputStream(inputFile);
        if(stream == null)
            throw new IOException("Unable to open " + inputFile.toString());

        final Iterator<ImageReader> imageReaders = ImageIO.getImageReaders(stream);
        if(!imageReaders.hasNext())
            throw new IOException("No ImageIO reader found for " + inputFile.toString());
        
        return imageReaders.next();
    }

    public static ImageReader getTiffIIOReader(final File inputFile) throws IOException {
        final ImageInputStream stream = ImageIO.createImageInputStream(inputFile);
        if(stream == null)
            throw new IOException("Unable to open " + inputFile.toString());

        ImageReader reader = null;
        final Iterator<ImageReader> imageReaders = ImageIO.getImageReaders(stream);
        while(imageReaders.hasNext()) {
            final ImageReader iioReader = imageReaders.next();
            if(iioReader instanceof TIFFImageReader) {
                reader = iioReader;
                break;
            }
        }
        if(reader == null)
            throw new IOException("Unable to open " + inputFile.toString());
        return reader;
    }

    public ImageReader getReader() {
        return reader;
    }

    public ImageIOFile(final File inputFile, final ImageReader iioReader) throws IOException {

        stream = ImageIO.createImageInputStream(inputFile);
        if(stream == null)
            throw new IOException("Unable to open " + inputFile.toString());

        reader = iioReader;
        reader.setInput(stream);

        name = inputFile.getName();
        numImages = reader.getNumImages(true);
        numBands = 3;

        dataType = ProductData.TYPE_INT32;
        final ImageTypeSpecifier its = reader.getRawImageType(0); 
        if(its != null) {
            numBands = reader.getRawImageType(0).getNumBands();
            dataType = bufferImageTypeToProductType(its.getBufferedImageType());

            if(its.getBufferedImageType() == BufferedImage.TYPE_BYTE_INDEXED) {
                isIndexed = true;
                createIndexedImageInfo(its.getColorModel());
            }
        }
    }

    private static int bufferImageTypeToProductType(int biType) {
        switch(biType) {
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
        final IndexColorModel indexColorModel = (IndexColorModel)colorModel;
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
        stream.close();
        reader.dispose();
    }

    public String getName() {
        return name;
    }

    public int getSceneWidth() throws IOException {
        if(sceneWidth == 0) {
            sceneWidth = reader.getWidth(0);
        }
        return sceneWidth;
    }

    public int getSceneHeight() throws IOException {
        if(sceneHeight == 0) {
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

    public synchronized void readImageIORasterBand(final int sourceOffsetX, final int sourceOffsetY,
                                                   final int sourceWidth, final int sourceHeight,
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

        final RenderedImage image = reader.readAsRenderedImage(0, param);
        final Raster data = image.getData(new Rectangle(destOffsetX, destOffsetY, destWidth, destHeight));

        final DataBuffer dataBuffer = data.getDataBuffer();
        final SampleModel sampleModel = data.getSampleModel();
        final int dataBufferType = dataBuffer.getDataType();
        final int destSize = destWidth * destHeight;
        final int sampleOffset = imageID + bandSampleOffset;

        if(dataBufferType == DataBuffer.TYPE_FLOAT &&
                destBuffer.getElems() instanceof float[]) {
            sampleModel.getSamples(0, 0, destWidth, destHeight, sampleOffset, (float[])destBuffer.getElems(), dataBuffer);

        } else if(dataBufferType == DataBuffer.TYPE_INT &&
                destBuffer.getElems() instanceof int[]) {
            sampleModel.getSamples(0, 0, destWidth, destHeight, sampleOffset, (int[])destBuffer.getElems(), dataBuffer);
        } else if(dataBufferType == DataBuffer.TYPE_SHORT &&
                destBuffer.getElems() instanceof int[]) {
            sampleModel.getSamples(0, 0, destWidth, destHeight, sampleOffset, (int[])destBuffer.getElems(), dataBuffer);
        } else {

            final double[] dArray = new double[destSize];
            sampleModel.getSamples(0, 0, destWidth, destHeight, sampleOffset, dArray, dataBuffer);

            final int length = dArray.length;
            if(destBuffer.getElems() instanceof double[]) {
                System.arraycopy(dArray, 0, destBuffer.getElems(), 0, length);
            } else {
                int i=0;
                for (double val : dArray) {
                    destBuffer.setElemDoubleAt(i++, val);
                }
            }
        }
    }

    public static class BandInfo {
        public final int imageID;
        public final int bandSampleOffset;
        public final ImageIOFile img;
        
        public BandInfo(ImageIOFile imgFile, int id, int offset) {
            img = imgFile;
            imageID = id;
            bandSampleOffset = offset;
        }
    }
}