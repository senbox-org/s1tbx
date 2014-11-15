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
package org.esa.nest.dataio.radarsat2;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.util.StringUtils;
import org.esa.beam.util.SystemUtils;
import org.esa.nest.dataio.SARReader;
import org.esa.nest.dataio.imageio.ImageIOFile;
import org.esa.snap.datamodel.AbstractMetadata;
import org.esa.snap.gpf.ReaderUtils;
import org.esa.snap.util.XMLSupport;
import org.jdom2.Document;
import org.jdom2.Element;

import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import java.awt.*;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * The product reader for Radarsat2 products.
 */
public class Radarsat2ProductReader extends SARReader {

    protected Radarsat2ProductDirectory dataDir = null;

    private static final String lutsigma = "lutSigma";
    private static final String lutgamma = "lutGamma";
    private static final String lutbeta = "lutBeta";

    private boolean isAscending;
    private boolean isAntennaPointingRight;

    private static final boolean flipToSARGeometry = System.getProperty(SystemUtils.getApplicationContextId() +
            ".flip.to.sar.geometry", "false").equals("true");

    /**
     * Constructs a new abstract product reader.
     *
     * @param readerPlugIn the reader plug-in which created this reader, can be <code>null</code> for internal reader
     *                     implementations
     */
    public Radarsat2ProductReader(final ProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
    }

    /**
     * Closes the access to all currently opened resources such as file input streams and all resources of this children
     * directly owned by this reader. Its primary use is to allow the garbage collector to perform a vanilla job.
     * <p>
     * <p>This method should be called only if it is for sure that this object instance will never be used again. The
     * results of referencing an instance of this class after a call to <code>close()</code> are undefined.
     * <p>
     * <p>Overrides of this method should always call <code>super.close();</code> after disposing this instance.
     *
     * @throws java.io.IOException if an I/O error occurs
     */
    @Override
    public void close() throws IOException {
        if (dataDir != null) {
            dataDir.close();
            dataDir = null;
        }
        super.close();
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

        try {
            final File fileFromInput = ReaderUtils.getFileFromInput(getInput());
            dataDir = createDirectory(fileFromInput);
            dataDir.readProductDirectory();
            final Product product = dataDir.createProduct();

            final MetadataElement absMeta = AbstractMetadata.getAbstractedMetadata(product);
            isAscending = absMeta.getAttributeString(AbstractMetadata.PASS).equals("ASCENDING");
            isAntennaPointingRight = absMeta.getAttributeString(AbstractMetadata.antenna_pointing).equals("right");
            addCalibrationLUT(product);

            product.getGcpGroup();
            product.setFileLocation(fileFromInput);
            product.setProductReader(this);
            setQuicklookBandName(product);

            return product;
        } catch (Exception e) {
            handleReaderException(e);
        }
        return null;
    }

    protected Radarsat2ProductDirectory createDirectory(final File fileFromInput) {
        return new Radarsat2ProductDirectory(fileFromInput);
    }

    /**
     * Read the LUT for use in calibration
     *
     * @param product the target product
     * @throws IOException if can't read lut
     */
    private void addCalibrationLUT(final Product product) throws IOException {

        final boolean flipLUT = flipToSARGeometry && ((isAscending && !isAntennaPointingRight) || (!isAscending && isAntennaPointingRight));
        final MetadataElement origProdRoot = AbstractMetadata.getOriginalProductMetadata(product);

        readCalibrationLUT(lutsigma, origProdRoot, flipLUT);
        readCalibrationLUT(lutgamma, origProdRoot, flipLUT);
        readCalibrationLUT(lutbeta, origProdRoot, flipLUT);
    }

    private void readCalibrationLUT(final String lutName, final MetadataElement root,
                                           final boolean flipLUT) throws IOException {
        InputStream is;
        if(dataDir.exists(dataDir.getRootFolder() + lutName + ".xml")) {
            is = dataDir.getInputStream(dataDir.getRootFolder() + lutsigma + ".xml");
        } else if(dataDir.exists(dataDir.getRootFolder() + lutName.toLowerCase() + ".xml")) {
            is = dataDir.getInputStream(dataDir.getRootFolder() + lutsigma.toLowerCase() + ".xml");
        } else {
            return;
        }

        final Document xmlDoc = XMLSupport.LoadXML(is);
        final Element rootElement = xmlDoc.getRootElement();

        final Element offsetElem = rootElement.getChild("offset");
        final double offset = Double.parseDouble(offsetElem.getValue());

        final Element gainsElem = rootElement.getChild("gains");
        final String gainsValue = gainsElem.getValue().trim().replace("  ", " ");
        final double[] gainsArray = StringUtils.toDoubleArray(gainsValue, " ");
        if (flipLUT) {
            double tmp;
            for (int i = 0; i < gainsArray.length / 2; i++) {
                tmp = gainsArray[i];
                gainsArray[i] = gainsArray[gainsArray.length - i - 1];
                gainsArray[gainsArray.length - i - 1] = tmp;
            }
        }

        final MetadataElement lut = new MetadataElement(lutName);
        root.addElement(lut);

        final MetadataAttribute offsetAttrib = new MetadataAttribute("offset", ProductData.TYPE_FLOAT64);
        offsetAttrib.getData().setElemDouble(offset);
        lut.addAttribute(offsetAttrib);

        final MetadataAttribute gainsAttrib = new MetadataAttribute("gains", ProductData.TYPE_FLOAT64, gainsArray.length);
        gainsAttrib.getData().setElems(gainsArray);
        lut.addAttribute(gainsAttrib);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight,
                                          int sourceStepX, int sourceStepY, Band destBand, int destOffsetX,
                                          int destOffsetY, int destWidth, int destHeight, ProductData destBuffer,
                                          ProgressMonitor pm) throws IOException {

        final ImageIOFile.BandInfo bandInfo = dataDir.getBandInfo(destBand);
        if (bandInfo != null && bandInfo.img != null) {
            if (isAscending) {
                readAscendingRasterBand(sourceOffsetX, sourceOffsetY, sourceStepX, sourceStepY,
                        destBuffer, destOffsetX, destOffsetY, destWidth, destHeight,
                        0, bandInfo.img, bandInfo.bandSampleOffset, isAntennaPointingRight);
            } else {
                readDescendingRasterBand(sourceOffsetX, sourceOffsetY, sourceStepX, sourceStepY,
                        destBuffer, destOffsetX, destOffsetY, destWidth, destHeight,
                        0, bandInfo.img, bandInfo.bandSampleOffset, isAntennaPointingRight);
            }
        }
    }

    public void readAscendingRasterBand(final int sourceOffsetX, final int sourceOffsetY,
                                        final int sourceStepX, final int sourceStepY,
                                        final ProductData destBuffer,
                                        final int destOffsetX, final int destOffsetY,
                                        final int destWidth, final int destHeight,
                                        final int imageID, final ImageIOFile img,
                                        final int bandSampleOffset,
                                        final boolean isAntennaPointingRight) throws IOException {

        final Raster data;

        synchronized (dataDir) {
            final ImageReader reader = img.getReader();
            final ImageReadParam param = reader.getDefaultReadParam();
            param.setSourceSubsampling(sourceStepX, sourceStepY,
                    sourceOffsetX % sourceStepX,
                    sourceOffsetY % sourceStepY);

            final RenderedImage image = reader.readAsRenderedImage(0, param);
            if (flipToSARGeometry) {
                if (isAntennaPointingRight) { // flip the image up side down
                    data = image.getData(new Rectangle(destOffsetX,
                            Math.max(0, img.getSceneHeight() - destOffsetY - destHeight),
                            destWidth, destHeight));
                } else { // flip the image upside down, then flip it left to right
                    data = image.getData(new Rectangle(Math.max(0, img.getSceneWidth() - destOffsetX - destWidth),
                            Math.max(0, img.getSceneHeight() - destOffsetY - destHeight),
                            destWidth, destHeight));
                }
            } else {
                data = image.getData(new Rectangle(destOffsetX, destOffsetY, destWidth, destHeight));
            }
        }

        final int w = data.getWidth();
        final int h = data.getHeight();
        final DataBuffer dataBuffer = data.getDataBuffer();
        final SampleModel sampleModel = data.getSampleModel();
        final int sampleOffset = imageID + bandSampleOffset;

        if (flipToSARGeometry) {
            final int[] dArray = new int[dataBuffer.getSize()];
            sampleModel.getSamples(0, 0, w, h, imageID + bandSampleOffset, dArray, dataBuffer);

            int srcStride, destStride;
            if (isAntennaPointingRight) { // flip the image upside down
                for (int r = 0; r < h; r++) {
                    srcStride = r * w;
                    destStride = (h - r - 1) * w;
                    for (int c = 0; c < w; c++) {
                        destBuffer.setElemIntAt(destStride + c, dArray[srcStride + c]);
                    }
                }
            } else { // flip the image upside down, then flip it left to right
                for (int r = 0; r < h; r++) {
                    srcStride = r * w;
                    destStride = (h - r) * w;
                    for (int c = 0; c < w; c++) {
                        destBuffer.setElemIntAt(destStride - c - 1, dArray[srcStride + c]);
                    }
                }
            }
        } else { // no flipping is needed
            sampleModel.getSamples(0, 0, w, h, sampleOffset, (int[]) destBuffer.getElems(), dataBuffer);
        }
    }

    public void readDescendingRasterBand(final int sourceOffsetX, final int sourceOffsetY,
                                         final int sourceStepX, final int sourceStepY,
                                         final ProductData destBuffer,
                                         final int destOffsetX, final int destOffsetY,
                                         final int destWidth, final int destHeight,
                                         final int imageID, final ImageIOFile img,
                                         final int bandSampleOffset,
                                         final boolean isAntennaPointingRight) throws IOException {

        final Raster data;
    try {
        synchronized (dataDir) {
            final ImageReader reader = img.getReader();
            final ImageReadParam param = reader.getDefaultReadParam();
            param.setSourceSubsampling(sourceStepX, sourceStepY,
                    sourceOffsetX % sourceStepX,
                    sourceOffsetY % sourceStepY);

            final RenderedImage image = reader.readAsRenderedImage(0, param);
            if (flipToSARGeometry && isAntennaPointingRight) {  // flip the image left to right
                data = image.getData(new Rectangle(Math.max(0, img.getSceneWidth() - destOffsetX - destWidth),
                        destOffsetY, destWidth, destHeight));
            } else {
                data = image.getData(new Rectangle(destOffsetX, destOffsetY, destWidth, destHeight));
            }
        }

        final int w = data.getWidth();
        final int h = data.getHeight();
        final DataBuffer dataBuffer = data.getDataBuffer();
        final SampleModel sampleModel = data.getSampleModel();
        final int sampleOffset = imageID + bandSampleOffset;

        if(destBuffer.getType() == ProductData.TYPE_FLOAT32) {
            if (flipToSARGeometry && isAntennaPointingRight) { // flip the image left to right
                final float[] dArray = new float[dataBuffer.getSize()];
                sampleModel.getSamples(0, 0, w, h, sampleOffset, dArray, dataBuffer);

                int srcStride, destStride;
                for (int r = 0; r < h; r++) {
                    srcStride = r * w;
                    destStride = r * w + w;
                    for (int c = 0; c < w; c++) {
                        destBuffer.setElemFloatAt(destStride - c - 1, dArray[srcStride + c]);
                    }
                }
            } else { // no flipping is needed
                sampleModel.getSamples(0, 0, w, h, sampleOffset, (float[]) destBuffer.getElems(), dataBuffer);
            }
        } else {
            if (flipToSARGeometry && isAntennaPointingRight) { // flip the image left to right
                final int[] dArray = new int[dataBuffer.getSize()];
                sampleModel.getSamples(0, 0, w, h, sampleOffset, dArray, dataBuffer);

                int srcStride, destStride;
                for (int r = 0; r < h; r++) {
                    srcStride = r * w;
                    destStride = r * w + w;
                    for (int c = 0; c < w; c++) {
                        destBuffer.setElemIntAt(destStride - c - 1, dArray[srcStride + c]);
                    }
                }
            } else { // no flipping is needed
                sampleModel.getSamples(0, 0, w, h, sampleOffset, (int[]) destBuffer.getElems(), dataBuffer);
            }
        }
    } catch (Exception e) {
        e.printStackTrace();
    }
    }

}