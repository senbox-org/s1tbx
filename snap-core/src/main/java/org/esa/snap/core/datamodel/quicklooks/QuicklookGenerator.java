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
package org.esa.snap.core.datamodel.quicklooks;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import net.coobird.thumbnailator.makers.FixedSizeThumbnailMaker;
import org.esa.snap.core.dataio.ProductSubsetDef;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.dataop.barithm.BandArithmetic;
import org.esa.snap.core.jexp.ParseException;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.runtime.Config;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * Generates Quicklook images
 */
public class QuicklookGenerator {

    /**
     * Preferences key for saving quicklook with product or not
     */
    public static final String PREFERENCE_KEY_QUICKLOOKS_SAVE_WITH_PRODUCT = "quicklooks.save.with.product";
    /**
     * Preferences key for maximum quicklook width
     */
    public static final String PREFERENCE_KEY_QUICKLOOKS_MAX_WIDTH = "quicklooks.max.width";

    public static final boolean DEFAULT_VALUE_QUICKLOOKS_SAVE_WITH_PRODUCT = true;

    public static final int DEFAULT_VALUE_QUICKLOOKS_MAX_WIDTH = 300;

    private static final int MULTILOOK_FACTOR = 2;
    private static final double DTOR = Math.PI / 180.0;

    private static final RGBImageProfile[] registeredProfiles = RGBImageProfileManager.getInstance().getAllProfiles();

    private static final String[] defaultQuickLookBands = new String[]{
            "intensity", "band", "sigma0",
            "t11", "t22", "t33", "c11", "c22", "c33"
    };

    private final int maxWidth;

    public QuicklookGenerator() {
        final Preferences preferences = Config.instance().preferences();
        maxWidth = preferences.getInt(PREFERENCE_KEY_QUICKLOOKS_MAX_WIDTH, DEFAULT_VALUE_QUICKLOOKS_MAX_WIDTH);
    }

    public BufferedImage createQuickLookFromBrowseProduct(final Product browseProduct) throws IOException {

        final BufferedImage image;
        if (browseProduct.getNumBands() < 3) {
            image = ProductUtils.createColorIndexedImage(browseProduct.getBandAt(0), ProgressMonitor.NULL);
        } else {
            final List<Band> bandList = new ArrayList<>(3);
            for (int i = 0; i < Math.min(3, browseProduct.getNumBands()); ++i) {
                final Band band = browseProduct.getBandAt(i);
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

        return new FixedSizeThumbnailMaker()
                .size(maxWidth, maxWidth)
                .keepAspectRatio(true)
                .fitWithinDimensions(true)
                .make(image);
    }

    public BufferedImage createQuickLookImage(final Product product, Band[] quicklookBands,
                                       final ProgressMonitor pm) throws IOException {
        Product productSubset = product;

        final boolean subsample = true;
        if (subsample) {
            final int width = maxWidth * 2;// * (MULTILOOK_FACTOR * 2);
            final ProductSubsetDef productSubsetDef = new ProductSubsetDef("subset");
            int scaleFactor = Math.round(Math.max(product.getSceneRasterWidth(), product.getSceneRasterHeight()) / (float) width);
            if (scaleFactor < 1) {
                scaleFactor = 1;
            }
            productSubsetDef.setSubSampling(scaleFactor, scaleFactor);

            productSubset = product.createSubset(productSubsetDef, null, null);

            final List<Band> bandList = new ArrayList<>();
            for (Band band : quicklookBands) {
                if (productSubset.getBand(band.getName()) != null) {
                    bandList.add(productSubset.getBand(band.getName()));
                } else if (band instanceof VirtualBand) {
                    ProductUtils.copyVirtualBand(productSubset, (VirtualBand) band, band.getName());
                    bandList.add(productSubset.getBand(band.getName()));
                }
            }
            quicklookBands = bandList.toArray(new Band[bandList.size()]);
        }

        final BufferedImage image;
        if(quicklookBands.length < 3 && quicklookBands[0].getIndexCoding() != null) {
            image = ProductUtils.createColorIndexedImage(quicklookBands[0], ProgressMonitor.NULL);
        } else {
            final ImageInfo imageInfo = ProductUtils.createImageInfo(quicklookBands, true, SubProgressMonitor.create(pm, 20));
            image = ProductUtils.createRgbImage(quicklookBands, imageInfo, SubProgressMonitor.create(pm, 80));
        }

        if (subsample) {
            productSubset.dispose();
        }

        return new FixedSizeThumbnailMaker()
                .size(maxWidth, maxWidth)
                .keepAspectRatio(true)
                .fitWithinDimensions(true)
                .make(image);
    }

    public static Band[] findQuicklookBands(final Product product) {

        Band[] rgbBands = findSuitableRGBProfileBands(product);
        if (rgbBands != null && rgbBands.length > 0) {
            return rgbBands;
        }

        String bandName = product.getQuicklookBandName();
        if (bandName != null && product.containsBand(bandName)) {
            return new Band[]{product.getBand(bandName)};
        }

        final String[] bandNames = product.getBandNames();
        final List<Band> bandList = new ArrayList<>(3);
        for (String name : bandNames) {
            name = name.toLowerCase();
            for (String qlBand : defaultQuickLookBands) {
                if (name.startsWith(qlBand)) {
                    bandList.add(product.getBand(name));
                    break;
                }
            }
            if (bandList.size() > 2) {
                return bandList.toArray(new Band[bandList.size()]);
            }
        }

        String quicklookBandName = ProductUtils.findSuitableQuicklookBandName(product);
        if (bandList.size() > 1 && !bandList.get(0).getName().equals(quicklookBandName)) {
            return bandList.toArray(new Band[bandList.size()]);
        }
        if(quicklookBandName == null) {
            return null;
        }
        return new Band[]{product.getBand(quicklookBandName)};
    }

    private static Band[] findSuitableRGBProfileBands(final Product product) {
        if (product.isMultiSize()) {
            return null;
        }
        for (RGBImageProfile profile : registeredProfiles) {
            if (profile.isApplicableTo(product)) {

                Band r = createBand(product, "r", profile.getRedExpression());
                Band g = createBand(product, "g", profile.getGreenExpression());
                Band b = createBand(product, "b", profile.getBlueExpression());

                return new Band[]{r, g, b};
            }
        }
        return null;
    }

    private static Dimension determineDimensions(final Product product, final String expression) {
        int width = product.getSceneRasterWidth();
        int height = product.getSceneRasterHeight();
        try {
            if (product.isMultiSize()) {
                final RasterDataNode[] refRasters = BandArithmetic.getRefRasters(expression, product);
                if (refRasters.length > 0) {
                    width = refRasters[0].getRasterWidth();
                    height = refRasters[0].getRasterHeight();
                }
            }
        } catch (ParseException e) {
            throw new IllegalArgumentException("Invalid expression: " + expression);
        }
        return new Dimension(width, height);
    }

    private static Band createBand(final Product product, final String name, final String expression) {
        Band band = product.getBand(expression);
        if (band == null) {
            Dimension dim = determineDimensions(product, expression);
            band = new VirtualBand(name, ProductData.TYPE_FLOAT32, dim.width, dim.height, expression);
            band.setOwner(product);
            band.setModified(false);
        }
        return band;
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

    static BufferedImage loadImage(final File quickLookFile) {
        if (quickLookFile.exists()) {
            try {
                try (BufferedInputStream fis = new BufferedInputStream(new FileInputStream(quickLookFile))) {
                    return ImageIO.read(fis);
                }
            } catch (Exception e) {
                SystemUtils.LOG.severe("Unable to load quicklook: " + quickLookFile);
            }
        }
        return null;
    }

    public static boolean writeImage(final BufferedImage bufferedImage, final File quickLookFile) {
        try {
            if (quickLookFile.createNewFile()) {
                //ImageIO.write(bufferedImage, "JPG", quickLookFile);

                ImageWriter jpgWriter = ImageIO.getImageWritersByFormatName("jpg").next();
                ImageWriteParam jpgWriteParam = jpgWriter.getDefaultWriteParam();
                jpgWriteParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                jpgWriteParam.setCompressionQuality(0.8f);

                ImageOutputStream outputStream = ImageIO.createImageOutputStream(quickLookFile);
                jpgWriter.setOutput(outputStream);
                IIOImage outputImage = new IIOImage(bufferedImage, null, null);
                jpgWriter.write(null, outputImage, jpgWriteParam);
                jpgWriter.dispose();

                return true;
            } else {
                SystemUtils.LOG.severe("Unable to save quicklook: " + quickLookFile);
            }
        } catch (IOException e) {
            SystemUtils.LOG.severe("Unable to save quicklook: " + quickLookFile);
        }
        return false;
    }
}
