/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.dataio.getasse30;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import org.esa.snap.core.dataio.AbstractProductReader;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.MapGeoCoding;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.dataop.maptransf.Datum;
import org.esa.snap.core.dataop.maptransf.IdentityTransformDescriptor;
import org.esa.snap.core.dataop.maptransf.MapInfo;
import org.esa.snap.core.dataop.maptransf.MapProjectionRegistry;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.core.util.io.FileUtils;

import javax.imageio.stream.FileCacheImageInputStream;
import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * A product reader for GETASSE30 data.
 *
 * @author Norman Fomferra
 */
public class GETASSE30Reader extends AbstractProductReader {

    private ZipFile _zipFile;
    private ImageInputStream _imageInputStream;
    private GETASSE30FileInfo _fileInfo;
    private Product _product;

    public GETASSE30Reader(final GETASSE30ReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
    }

    /**
     * Provides an implementation of the <code>readProductNodes</code> interface method. Clients implementing this
     * method can be sure that the input object and eventually the subset information has already been set.
     * <p>This method is called as a last step in the <code>readProductNodes(input, subsetInfo)</code> method.
     *
     * @throws java.io.IOException if an I/O error occurs
     */
    @Override
    protected Product readProductNodesImpl() throws IOException {
        initReader();

        final File dataFile = GETASSE30ReaderPlugIn.getInputFile(getInput());
        _fileInfo = GETASSE30FileInfo.create(dataFile);

        final String fileName;
        try {
            final String ext = FileUtils.getExtension(dataFile);
            if (".zip".equalsIgnoreCase(ext)) {
                final String entryName = FileUtils.getFilenameWithoutExtension(dataFile.getName()) + ".GETASSE30";
                _zipFile = new ZipFile(dataFile);
                final ZipEntry entry = getZipEntryIgnoreCase(entryName);
                final InputStream inputStream = _zipFile.getInputStream(entry);
                _imageInputStream = new FileCacheImageInputStream(inputStream, createCacheDir());
                fileName = FileUtils.getFilenameWithoutExtension(entryName);
            } else {
                _imageInputStream = new FileImageInputStream(dataFile);
                fileName = FileUtils.getFilenameWithoutExtension(dataFile);
            }
        } catch (IOException e) {
            try {
                close();
            } catch (IOException ignored) {
            }
            throw e;
        }

        initProduct(GETASSE30ReaderPlugIn.FORMAT_NAME + "_" + fileName);
        _product.setFileLocation(dataFile);
        return _product;
    }

    /**
     * The template method which is called by the method after an optional spatial subset has been applied to the input
     * parameters.
     * <p>The destination band, buffer and region parameters are exactly the ones passed to the original  call. Since
     * the <code>destOffsetX</code> and <code>destOffsetY</code> parameters are already taken into acount in the
     * <code>sourceOffsetX</code> and <code>sourceOffsetY</code> parameters, an implementor of this method is free to
     * ignore them.
     *
     * @param sourceOffsetX the absolute X-offset in source raster co-ordinates
     * @param sourceOffsetY the absolute Y-offset in source raster co-ordinates
     * @param sourceWidth   the width of region providing samples to be read given in source raster co-ordinates
     * @param sourceHeight  the height of region providing samples to be read given in source raster co-ordinates
     * @param sourceStepX   the sub-sampling in X direction within the region providing samples to be read
     * @param sourceStepY   the sub-sampling in Y direction within the region providing samples to be read
     * @param destBand      the destination band which identifies the data source from which to read the sample values
     * @param destOffsetX   the X-offset in the band's raster co-ordinates
     * @param destOffsetY   the Y-offset in the band's raster co-ordinates
     * @param destWidth     the width of region to be read given in the band's raster co-ordinates
     * @param destHeight    the height of region to be read given in the band's raster co-ordinates
     * @param destBuffer    the destination buffer which receives the sample values to be read
     * @param pm            a monitor to inform the user about progress
     * @throws java.io.IOException if an I/O error occurs
     * @see #readBandRasterData
     * @see #getSubsetDef
     */
    @Override
    protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight,
                                          int sourceStepX, int sourceStepY, Band destBand, int destOffsetX,
                                          int destOffsetY, int destWidth, int destHeight, ProductData destBuffer,
                                          ProgressMonitor pm) throws IOException {
        pm.beginTask("Reading DEM Data...", 1);
        try {
            final short[] elems = (short[]) destBuffer.getElems();
            readRasterDataImpl(elems, sourceOffsetY, sourceOffsetX, sourceStepX, sourceStepY, destWidth, destHeight,
                               SubProgressMonitor.create(pm, 1));
            maskInt32Extrema(elems);
        } finally {
            pm.done();
        }

    }


    /**
     * Closes the access to all currently opened resources such as file input streams and all resources of this children
     * directly owned by this reader. Its primary use is to allow the garbage collector to perform a vanilla job.
     * <p>This method should be called only if it is for sure that this object instance will never be used again. The
     * results of referencing an instance of this class after a call to <code>close()</code> are undefined.
     * <p>Overrides of this method should always call <code>super.close();</code> after disposing this instance.
     *
     * @throws java.io.IOException if an I/O error occurs
     */
    @Override
    public void close() throws IOException {
        super.close();
        closeImageInputStream();
        closeZipFile();
        _product = null;
    }

    private void closeImageInputStream() throws IOException {
        if (_imageInputStream != null) {
            _imageInputStream.close();
            _imageInputStream = null;
        }
    }

    private void closeZipFile() throws IOException {
        if (_zipFile != null) {
            _zipFile.close();
            _zipFile = null;
        }
    }

    private void initReader() {
        _zipFile = null;
        _imageInputStream = null;
        _product = null;
        _fileInfo = null;
    }

    private static File createCacheDir() throws IOException {
        final File cacheDir = new File(SystemUtils.getCacheDir(), "temp");
        if (!cacheDir.exists() && !cacheDir.mkdirs()) {
            throw new IOException("Failed to create directory '" + cacheDir + "'.");
        }
        return cacheDir;
    }

    private void initProduct(final String productName) {
        final int width = _fileInfo.getWidth();
        final int height = _fileInfo.getHeight();
        _product = new Product(productName, GETASSE30ReaderPlugIn.FORMAT_NAME, width, height, this);
        final MapInfo mapInfo = new MapInfo(MapProjectionRegistry.getProjection(IdentityTransformDescriptor.NAME),
                                            0.5F, 0.5F,
                                            _fileInfo.getEasting(),
                                            _fileInfo.getNorthing() + height * _fileInfo.getPixelSizeY(),
                                            _fileInfo.getPixelSizeX(),
                                            _fileInfo.getPixelSizeY(),
                                            Datum.WGS_84);
        mapInfo.setSceneWidth(width);
        mapInfo.setSceneHeight(height);
        _product.setSceneGeoCoding(new MapGeoCoding(mapInfo));
        _product.setDescription("GETASSE30 DEM");
        Band elevationBand = new Band("elevation", ProductData.TYPE_INT16, width, height);
        elevationBand.setUnit("m");
        elevationBand.setDescription("GETASSE30 Elevation");
        // setting geo-physical no-data value to prevent for scaling
        elevationBand.setGeophysicalNoDataValue(_fileInfo.getNoDataValue());
        _product.addBand(elevationBand);
    }


    private synchronized void readRasterDataImpl(final short[] elems,
                                                 final int sourceOffsetY,
                                                 final int sourceOffsetX,
                                                 final int sourceStepX,
                                                 final int sourceStepY,
                                                 final int destWidth,
                                                 final int destHeight,
                                                 ProgressMonitor pm) throws IOException {
        final int sceneWidth = _product.getSceneRasterWidth();
        pm.beginTask("Reading raster data...", destHeight);
        try {
            if (sourceStepX == 1) {
                int sourceY = sourceOffsetY;
                for (int destY = 0; destY < destHeight; destY++) {
                    if (pm.isCanceled()) {
                        break;
                    }
                    final long sourcePos = sourceY * sceneWidth + sourceOffsetX;
                    final int destPos = destY * destWidth;
                    _imageInputStream.seek(2 * sourcePos);      // 2 byte
                    _imageInputStream.readFully(elems, destPos, destWidth);
                    sourceY += sourceStepY;
//                    _pc.fireProcessInProgress(destY);
                    pm.worked(sourceStepY);
                }
            } else {
                int sourceY = sourceOffsetY;
                for (int destY = 0; destY < destHeight; destY++) {
                    if (pm.isCanceled()) {
                        break;
                    }
                    int sourceX = sourceOffsetX;
                    for (int destX = 0; destX < destWidth; destX++) {
                        final long sourcePos = sourceY * sceneWidth + sourceX;
                        final int destPos = destY * destWidth + destX;
                        _imageInputStream.seek(2 * sourcePos);
                        elems[destPos] = _imageInputStream.readShort();
                        sourceX += sourceStepX;
                    }
                    sourceY += sourceStepY;
//                    _pc.fireProcessInProgress(destY);
                    pm.worked(sourceStepY);
                }
            }
        } finally {
            pm.done();
        }
    }

    private void maskInt32Extrema(final short[] elems) {
        final short noDataValue = (short) _fileInfo.getNoDataValue();
        for (int i = 0; i < elems.length; i++) {
            if (elems[i] == Short.MIN_VALUE || elems[i] == Short.MAX_VALUE) {
                elems[i] = noDataValue;
            }
        }
    }

    private ZipEntry getZipEntryIgnoreCase(final String entryName) {
        final Enumeration enumeration = _zipFile.entries();
        while (enumeration.hasMoreElements()) {
            final ZipEntry zipEntry = (ZipEntry) enumeration.nextElement();
            if (zipEntry.getName().equalsIgnoreCase(entryName)) {
                return zipEntry;
            }
        }
        return null;
    }

}
