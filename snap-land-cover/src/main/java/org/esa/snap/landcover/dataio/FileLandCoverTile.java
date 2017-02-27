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

import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.dataop.resamp.Resampling;
import org.esa.snap.core.util.CachingObjectArray;
import org.esa.snap.engine_utilities.download.downloadablecontent.DownloadableContentImpl;
import org.esa.snap.engine_utilities.download.downloadablecontent.DownloadableFile;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FileLandCoverTile extends DownloadableContentImpl implements Resampling.Raster {

    private CachingObjectArray linesCache;
    private Product product = null;
    private static final int maxLines = 500;
    private final List<Integer> indexList = new ArrayList<>(maxLines);

    private final GeoCoding tileGeocoding;
    private final int width;
    private final int height;
    private Double noDataValue;

    private final LandCoverModel model;
    private final ProductReader reader;

    public FileLandCoverTile(final LandCoverModel model, final File localFile, final ProductReader reader)
            throws IOException {
        this(model, localFile, reader, ".zip");
    }

    public FileLandCoverTile(final LandCoverModel model, final File localFile, final ProductReader reader,
                             final String archiveExt)
            throws IOException {
        super(localFile, model.getDescriptor().getArchiveUrl(), archiveExt);
        this.model = model;
        this.reader = reader;
        noDataValue = model.getDescriptor().getNoDataValue();

        final DownloadableFile downloadableFile = getContentFile();
        if (downloadableFile != null && product != null) {
            width = product.getBandAt(0).getRasterWidth();
            height = product.getBandAt(0).getRasterHeight();
            tileGeocoding = product.getSceneGeoCoding();

            if (noDataValue == 0)
                noDataValue = product.getBandAt(0).getNoDataValue();

            linesCache = new CachingObjectArray(getLineFactory());
            linesCache.setCachedRange(0, product.getBandAt(0).getRasterHeight());
        } else {
            tileGeocoding = null;
            width = 0;
            height = 0;
        }
    }

    protected DownloadableFile createContentFile(final File file) {
        try {
            product = reader.readProductNodes(file, null);

            return new BaseLandCoverTile(model, product);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public GeoCoding getTileGeocoding() {
        return tileGeocoding;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public double getNoDataValue() {
        return noDataValue;
    }

    public void dispose() {
        clearCache();
        linesCache = null;
        if (product != null) {
            product.dispose();
            product = null;
        }
    }

    public void clearCache() {
        linesCache.clear();
    }

    private CachingObjectArray.ObjectFactory getLineFactory() {
        final Band band = product.getBandAt(0);
        final int width = product.getSceneRasterWidth();
        return new CachingObjectArray.ObjectFactory() {
            public Object createObject(final int index) throws Exception {
                updateCache(index);
                return band.getSourceImage().getData(new Rectangle(0, index, width, 1)).getPixels(0, index, width, 1, new float[width]);
            }
        };
    }

    private void updateCache(int index) {
        indexList.remove((Object) index);
        indexList.add(0, index);
        if (indexList.size() > maxLines) {
            final int i = indexList.size() - 1;
            linesCache.setObject(i, null);
            indexList.remove(i);
        }
    }

    public boolean getSamples(final int[] x, final int[] y, final double[][] samples) throws Exception {
        boolean allValid = true;
        for (int i = 0; i < y.length; i++) {
            final float[] line = (float[]) linesCache.getObject(y[i]);
            for (int j = 0; j < x.length; j++) {
                samples[i][j] = line[x[j]];
                if (noDataValue.equals(samples[i][j])) {
                    samples[i][j] = Double.NaN;
                    allValid = false;
                }
            }
        }
        return allValid;
    }
}