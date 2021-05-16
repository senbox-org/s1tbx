/*
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
package org.esa.s1tbx.io.gaofen3;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.s1tbx.commons.io.ImageIOFile;
import org.esa.s1tbx.commons.io.S1TBXProductReaderPlugIn;
import org.esa.s1tbx.commons.io.SARReader;
import org.esa.s1tbx.io.DataCache;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.util.SystemUtils;

import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import java.awt.*;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;


/**
 * @author Jakob Grahn
 */
public class Gaofen3ProductReader extends SARReader {

    private final S1TBXProductReaderPlugIn readerPlugIn;
    private Gaofen3ProductDirectory dataDir;
    private final DataCache cache;

    /**
     * Constructs a new abstract product reader.
     *
     * @param readerPlugIn the reader plug-in which created this reader, can be <code>null</code> for internal reader
     *                     implementations
     */
    public Gaofen3ProductReader(final S1TBXProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
        cache = new DataCache();
        this.readerPlugIn = readerPlugIn;
    }

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
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected Product readProductNodesImpl() throws IOException {

        Object input = getInput();
        if (input instanceof InputStream) {
            throw new IOException("InputStream not supported");
        }

        final Path path = getPathFromInput(input);
        File metadataFile = readerPlugIn.findMetadataFile(path);

        dataDir = new Gaofen3ProductDirectory(metadataFile);
        dataDir.readProductDirectory();
        final Product product = dataDir.createProduct();

        addCommonSARMetadata(product);
        product.getGcpGroup();
        product.setFileLocation(metadataFile);
        product.setProductReader(this);

        return product;
    }

    @Override
    protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY,
                                          int sourceWidth, int sourceHeight,
                                          int sourceStepX, int sourceStepY,
                                          Band destBand,
                                          int destOffsetX, int destOffsetY,
                                          int destWidth, int destHeight,
                                          ProductData destBuffer,
                                          ProgressMonitor pm) throws IOException {
        if (dataDir == null) {
            return;
        }

        final int[] srcArray;
        final ImageIOFile.BandInfo bandInfo = dataDir.getBandInfo(destBand);
        final Rectangle destRect = new Rectangle(destOffsetX, destOffsetY, destWidth, destHeight);
        final DataCache.DataKey datakey = new DataCache.DataKey(bandInfo.img, destRect);
        DataCache.Data cachedData = cache.get(datakey);
        if (cachedData != null && cachedData.valid) {
            srcArray = cachedData.intArray;
        } else {
            cachedData = readRect(datakey, bandInfo, sourceOffsetX, sourceOffsetY, sourceStepX, sourceStepY, destRect);
            srcArray = cachedData.intArray;
        }
        final boolean isSLC = dataDir.isSLC();
        final float[] elems = (float[]) destBuffer.getElems();
        final int numElems = elems.length;
        final double scaleFactor;

        // Get "QualifyValue" and "CalibrationConstant" for this polarisation:
        double qVal = Double.NaN;
        double calConstDB = Double.NaN;
        String[] polarisations = dataDir.getPolarisations();
        if (polarisations.length == 1) {
            String p = polarisations[0];
            qVal = dataDir.getQVal(p);
            calConstDB = dataDir.getCalConst(p);
        } else {
            for (String p : polarisations) {
                if (bandInfo.img.getName().contains("_" + p + "_")) {
                    qVal = dataDir.getQVal(p);
                    calConstDB = dataDir.getCalConst(p);
                    break;
                }
            }
        }
        if(Double.isNaN(qVal)) {
            SystemUtils.LOG.severe("Invalid QualifyValue");
        }
        if(Double.isNaN(calConstDB)) {
            SystemUtils.LOG.severe("Invalid CalibrationConstant");
        }

        // Compute scale factor:
        double calConstLin = Math.sqrt(Math.pow(10.0,calConstDB/10.0));
        if (isSLC){
            scaleFactor = qVal/(32767.0 * calConstLin);
        } else {
            scaleFactor = qVal/(65535.0 * calConstLin);
        }

        // Read and calibrate:
        for (int i = 0; i < numElems; ++i) {
            double val = srcArray[i];
            elems[i] = (float) (val * scaleFactor);
        }

    }

    private synchronized DataCache.Data readRect(final DataCache.DataKey datakey, final ImageIOFile.BandInfo bandInfo,
                                                 int sourceOffsetX, int sourceOffsetY, int sourceStepX, int sourceStepY,
                                                 final Rectangle destRect) {
        try {
            final ImageReader imageReader = bandInfo.img.getReader();
            final ImageReadParam readParam = imageReader.getDefaultReadParam();
            if (sourceStepX == 1 && sourceStepY == 1) {
                readParam.setSourceRegion(destRect);
            }
            readParam.setSourceSubsampling(sourceStepX, sourceStepY, sourceOffsetX % sourceStepX,
                    sourceOffsetY % sourceStepY);
            final RenderedImage subsampledImage = imageReader.readAsRenderedImage(0, readParam);
            final Raster data = subsampledImage.getData(destRect);

            final SampleModel sampleModel = data.getSampleModel();
            final int destWidth = Math.min((int) destRect.getWidth(), sampleModel.getWidth());
            final int destHeight = Math.min((int) destRect.getHeight(), sampleModel.getHeight());

            final int length = destWidth * destHeight;
            final int[] srcArray = new int[length];
            sampleModel.getSamples(0, 0, destWidth, destHeight, bandInfo.bandSampleOffset, srcArray,
                    data.getDataBuffer());

            DataCache.Data cachedData = new DataCache.Data(srcArray);
            if (datakey != null) {
                cache.put(datakey, cachedData);
            }
            return cachedData;
        } catch (Exception e) {
            final int[] srcArray = new int[(int) destRect.getWidth() * (int) destRect.getHeight()];
            DataCache.Data cachedData = new DataCache.Data(srcArray);
            if (datakey != null) {
                cache.put(datakey, cachedData);
            }
            return cachedData;
        }
    }
}

