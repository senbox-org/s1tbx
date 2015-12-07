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
package org.esa.snap.landcover.dataio;

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.dataio.ProductIOPlugInManager;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.dataop.resamp.Resampling;
import org.esa.snap.core.util.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

public class FileLandCoverModel implements LandCoverModel {

    protected final Resampling resampling;
    protected final Resampling.Index resamplingIndex;
    protected final File[] fileList;
    private final String archiveExt;

    protected final LandCoverModelDescriptor descriptor;
    protected FileLandCoverTile[] tileList = null;

    protected static final ProductReaderPlugIn productReaderPlugIn = getReaderPlugIn("GeoTIFF");

    public FileLandCoverModel(final LandCoverModelDescriptor descriptor, final File[] files,
                              final Resampling resamplingMethod) throws IOException {
        this(descriptor, files, resamplingMethod, ".zip");
    }
    public FileLandCoverModel(final LandCoverModelDescriptor descriptor, final File[] files,
                              final Resampling resamplingMethod, final String archiveExt) throws IOException {

        this.descriptor = descriptor;
        this.resampling = resamplingMethod;
        this.resamplingIndex = resampling.createIndex();
        this.fileList = files;
        this.archiveExt = archiveExt;
    }

    public void dispose() {
        for (FileLandCoverTile tile : tileList) {
            if (tile != null)
                tile.dispose();
        }
    }

    public LandCoverModelDescriptor getDescriptor() {
        return descriptor;
    }

    public Resampling getResampling() {
        return resampling;
    }

    public synchronized double getLandCover(final GeoPos geoPos) throws Exception {
        try {
            if (tileList == null) {
                loadProducts();
            }
            for (FileLandCoverTile tile : tileList) {
                if (tile.getTileGeocoding() == null)
                    continue;

                final PixelPos pix = tile.getTileGeocoding().getPixelPos(geoPos, null);
                if (!pix.isValid() || pix.x < 0 || pix.y < 0 || pix.x >= tile.getWidth() || pix.y >= tile.getHeight())
                    continue;

                resampling.computeIndex(pix.x, pix.y, tile.getWidth(), tile.getHeight(), resamplingIndex);

                final double value = resampling.resample(tile, resamplingIndex);
                if (Double.isNaN(value)) {
                    return tile.getNoDataValue();
                }
                return value;
            }
            return tileList[0].getNoDataValue();
        } catch (Exception e) {
            throw new Exception("Problem reading : " + e.getMessage());
        }
    }

    private void loadProducts() throws Exception {
        tileList = new FileLandCoverTile[fileList.length];
        for (int i = 0; i < fileList.length; ++i) {
            try {
                String ext = FileUtils.getExtension(fileList[i]).toLowerCase();
                if (ext != null && ext.contains("tif") || ext.contains("zip")) {
                    tileList[i] = new FileLandCoverTile(this, fileList[i],
                                                        productReaderPlugIn.createReaderInstance(), archiveExt);
                } else {
                    final ProductReader reader = ProductIO.getProductReaderForInput(fileList[i]);
                    tileList[i] = new FileLandCoverTile(this, fileList[i], reader, archiveExt);
                }
            } catch (IOException e) {
                tileList[i] = null;
            }
        }
    }

    public PixelPos getIndex(final GeoPos geoPos) {
        return null;
    }

    public GeoPos getGeoPos(final PixelPos pixelPos) {
        return null;
    }

    public float getSample(double pixelX, double pixelY) throws IOException {
        return 0;
    }

    public boolean getSamples(final int[] x, final int[] y, final double[][] samples) throws IOException {
        return false;
    }

    protected static ProductReaderPlugIn getReaderPlugIn(final String formatName) {
        final Iterator readerPlugIns = ProductIOPlugInManager.getInstance().getReaderPlugIns(formatName);
        return (ProductReaderPlugIn) readerPlugIns.next();
    }
}