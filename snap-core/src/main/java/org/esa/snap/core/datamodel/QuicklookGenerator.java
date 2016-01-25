/*
 * Copyright (C) 2016 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.core.datamodel;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import org.esa.snap.core.dataio.ProductSubsetDef;
import org.esa.snap.core.dataop.downloadable.StatusProgressMonitor;
import org.esa.snap.core.util.ProductUtils;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by luis on 16/01/2016.
 */
public class QuicklookGenerator {

    private static final String QUICKLOOK_PREFIX = "QL_";
    private static final String QUICKLOOK_EXT = ".jpg";
    private static final int MAX_WIDTH = 300;
    private static final int MULTILOOK_FACTOR = 2;
    private static final double DTOR = Math.PI / 180.0;

    private static final String[] defaultQuickLookBands = new String[] { "intensity", "band", "t11", "t22", "t33", "c11", "c22", "c33"};

    public QuicklookGenerator() {

    }

    public BufferedImage createQuickLookFromBrowseProduct(final Product browseProduct,
                                                          final boolean subsample) throws IOException {
        Product productSubset = browseProduct;

        if (subsample) {
            final int maxWidth = MAX_WIDTH;
            final ProductSubsetDef productSubsetDef = new ProductSubsetDef("subset");
            int scaleFactor = Math.round(Math.max(browseProduct.getSceneRasterWidth(),
                                                  browseProduct.getSceneRasterHeight()) / (float) maxWidth);
            if (scaleFactor < 1) {
                scaleFactor = 1;
            }
            productSubsetDef.setSubSampling(scaleFactor, scaleFactor);

            productSubset = browseProduct.createSubset(productSubsetDef, null, null);
        }

        final BufferedImage image;
        if (productSubset.getNumBands() < 3) {
            image = ProductUtils.createColorIndexedImage(productSubset.getBandAt(0), ProgressMonitor.NULL);
        } else {
            final List<Band> bandList = new ArrayList<>(3);
            for (int i = 0; i < Math.min(3, productSubset.getNumBands()); ++i) {
                final Band band = productSubset.getBandAt(i);
                if (band.getStx().getMean() != 0) {
                    bandList.add(band);
                }
            }
            final Band[] bands = bandList.toArray(new Band[bandList.size()]);

            if (bands.length < 3) {
                image = ProductUtils.createColorIndexedImage(bands[0], ProgressMonitor.NULL);
            } else {
                ImageInfo imageInfo = ProductUtils.createImageInfo(bands, true, ProgressMonitor.NULL);
                image = ProductUtils.createRgbImage(bands, imageInfo, ProgressMonitor.NULL);
            }
        }

        productSubset.dispose();
        return image;
    }

    public BufferedImage createQuickLookImage(final Product product, final boolean subsample) throws IOException {

        final StatusProgressMonitor pm = new StatusProgressMonitor(StatusProgressMonitor.TYPE.SUBTASK);
        pm.beginTask("Creating quicklook " + product.getName() + "... ", 100);

        Product productSubset = product;

        if (subsample) {
            final int maxWidth = MAX_WIDTH * (MULTILOOK_FACTOR * 2);
            final ProductSubsetDef productSubsetDef = new ProductSubsetDef("subset");
            int scaleFactor = Math.round(Math.max(product.getSceneRasterWidth(), product.getSceneRasterHeight()) / (float) maxWidth);
            if (scaleFactor < 1) {
                scaleFactor = 1;
            }
            productSubsetDef.setSubSampling(scaleFactor, scaleFactor);

            productSubset = product.createSubset(productSubsetDef, null, null);
        }
        final Band[] quicklookBands = getQuicklookBand(productSubset);

        final BufferedImage image;
        if (quicklookBands.length < 3) {
            image = ProductUtils.createColorIndexedImage(average(productSubset, quicklookBands[0],
                                                                 SubProgressMonitor.create(pm, 50)),
                                                         SubProgressMonitor.create(pm, 50));
        } else {
            final List<Band> bandList = new ArrayList<>(3);
            for (int i = 0; i < Math.min(3, quicklookBands.length); ++i) {
                final Band band = average(productSubset, quicklookBands[i], SubProgressMonitor.create(pm, 50));
                if (band.getStx().getMean() != 0) {
                    bandList.add(band);
                }
            }

            final Band[] bands = bandList.toArray(new Band[bandList.size()]);

            if (bands.length < 3) {
                image = ProductUtils.createColorIndexedImage(bands[0], SubProgressMonitor.create(pm, 50));
            } else {
                ImageInfo imageInfo = ProductUtils.createImageInfo(bands, true, SubProgressMonitor.create(pm, 25));
                image = ProductUtils.createRgbImage(bands, imageInfo, SubProgressMonitor.create(pm, 25));
            }
        }

        if (subsample) {
            productSubset.dispose();
        }

        pm.done();

        return image;
    }

    private static Band[] getQuicklookBand(final Product product) {

        final String[] bandNames = product.getBandNames();
        final List<Band> bandList = new ArrayList<>(3);
        for(String name : bandNames) {
            for(String qlBand : defaultQuickLookBands) {
                if (name.toLowerCase().startsWith(qlBand)) {
                    bandList.add(product.getBand(name));
                    break;
                }
            }
            if (bandList.size() > 2) {
                break;
            }
        }
        if(!bandList.isEmpty()) {
            return bandList.toArray(new Band[bandList.size()]);
        }
        String quicklookBandName = ProductUtils.findSuitableQuicklookBandName(product);
        return new Band[] { product.getBand(quicklookBandName)};
    }

    private static Band average(final Product product, final Band srcBand, final ProgressMonitor pm) {

        int rangeFactor = MULTILOOK_FACTOR;
        int azimuthFactor = MULTILOOK_FACTOR;

  /*      if (AbstractMetadata.hasAbstractedMetadata(product)) {
            final MetadataElement abs = AbstractMetadata.getAbstractedMetadata(product);
            final boolean srgrFlag = abs.getAttributeInt(AbstractMetadata.srgr_flag, 0) == 1;
            double rangeSpacing = abs.getAttributeDouble(AbstractMetadata.range_spacing, 1);
            double azimuthSpacing = abs.getAttributeDouble(AbstractMetadata.azimuth_spacing, 1);

            double groundRangeSpacing = rangeSpacing;
            if (rangeSpacing == AbstractMetadata.NO_METADATA) {
                azimuthSpacing = 1;
                groundRangeSpacing = 1;
            } else if (!srgrFlag) {
                final TiePointGrid incidenceAngle = product.getTiePointGrid("incident_angle");
                if (incidenceAngle != null) {
                    final float x = product.getSceneRasterWidth() / 2f;
                    final float y = product.getSceneRasterHeight() / 2f;
                    final double incidenceAngleAtCentreRangePixel = incidenceAngle.getPixelDouble(x, y);

                    groundRangeSpacing /= FastMath.sin(incidenceAngleAtCentreRangePixel * DTOR);
                }
            }

            final double nAzLooks = MULTILOOK_FACTOR * groundRangeSpacing / azimuthSpacing;
            if (nAzLooks < 1.0) {
                azimuthFactor = 1;
                rangeFactor = (int) Math.min(MULTILOOK_FACTOR * 2, Math.round(azimuthSpacing / groundRangeSpacing));
            } else {
                azimuthFactor = (int) Math.min(MULTILOOK_FACTOR * 2, Math.round(nAzLooks));
            }
        }*/

        final int rangeAzimuth = rangeFactor * azimuthFactor;

        final int srcW = srcBand.getRasterWidth();
        final int srcH = srcBand.getRasterHeight();
        final int w = srcW / rangeFactor;
        final int h = srcH / azimuthFactor;
        int index = 0;
        final float[] data = new float[w * h];

        try {
            boolean bandAddedToProduct = false;
            if (product.getBand(srcBand.getName()) == null) {
                product.addBand(srcBand);
                bandAddedToProduct = true;
            }

            final float[] floatValues = new float[srcW * srcH];
            srcBand.readPixels(0, 0, srcW, srcH, floatValues, pm);

            //pm.beginTask("QL Averaging...", h);
            for (int ty = 0; ty < h; ++ty) {
                final int yStart = ty * azimuthFactor;
                final int yEnd = yStart + azimuthFactor;

                for (int tx = 0; tx < w; ++tx) {
                    final int xStart = tx * rangeFactor;
                    final int xEnd = xStart + rangeFactor;

                    double meanValue = 0.0;
                    for (int y = yStart; y < yEnd; ++y) {
                        for (int x = xStart; x < xEnd; ++x) {

                            meanValue += floatValues[y * srcW + x];
                        }
                    }
                    meanValue /= rangeAzimuth;

                    data[index++] = (float) meanValue;
                }
                //pm.worked(1);
            }
            //pm.done();

            if (bandAddedToProduct) {
                product.removeBand(srcBand);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        Band b = new Band("averaged", ProductData.TYPE_FLOAT32, w, h);
        b.setData(ProductData.createInstance(data));
        return b;
    }
}
