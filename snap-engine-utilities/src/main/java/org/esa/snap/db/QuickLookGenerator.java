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
package org.esa.snap.db;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.VirtualDir;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.dataio.ProductSubsetDef;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.util.math.MathUtils;
import org.esa.snap.datamodel.AbstractMetadata;
import org.esa.snap.gpf.OperatorUtils;
import org.esa.snap.util.ResourceUtils;
import org.esa.snap.util.ZipUtils;

import javax.imageio.ImageIO;
import javax.media.jai.PlanarImage;
import javax.media.jai.RasterFactory;
import java.awt.*;
import java.awt.image.*;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Generates Quicklooks
 */
public class QuickLookGenerator {

    private static final String QUICKLOOK_PREFIX = "QL_";
    private static final String QUICKLOOK_EXT = ".jpg";
    private static final int MAX_WIDTH = 300;
    private static final int MULTILOOK_FACTOR = 2;

    private static final String[] defaultQuickLookBands = new String[] { "Intensity", "band", "T11", "T22", "T33", "C11", "C22", "C33"};

    private static final File dbStorageDir = new File(ResourceUtils.getApplicationUserDir(true),
            ProductDB.DEFAULT_PRODUCT_DATABASE_NAME + File.separator + "QuickLooks");

    public static boolean quickLookExists(final ProductEntry entry) {
        final File quickLookFile = getQuickLookFile(dbStorageDir, entry.getId());
        return quickLookFile.exists() && quickLookFile.length() > 0;
    }

    public static BufferedImage loadQuickLook(final ProductEntry entry) {
        final File quickLookFile = getQuickLookFile(dbStorageDir, entry.getId());
        BufferedImage bufferedImage = null;
        if (quickLookFile.exists() && quickLookFile.length() > 0) {
            bufferedImage = loadFile(quickLookFile);
        } else {
            if(entry.getFile() != null && entry.getId() >= 0) {
                try {
                    bufferedImage = createQuickLook(entry.getId(), entry.getFile());
                } catch (IOException e) {
                    //return null;
                }
            }
        }
        return bufferedImage;
    }

    public static void deleteQuickLook(final int id) {
        final File quickLookFile = getQuickLookFile(dbStorageDir, id);
        if (quickLookFile.exists())
            quickLookFile.delete();
    }

    private static File getQuickLookFile(final File storageDir, final int id) {
        return new File(storageDir, QUICKLOOK_PREFIX + id + QUICKLOOK_EXT);
    }

    public static BufferedImage createQuickLook(final Product product) {
        // check if quicklook exists with the product
        final File browseFile = findProductBrowseImage(product.getFileLocation());
        if (browseFile != null) {
            try {
                final Product sourceProduct = ProductIO.readProduct(browseFile);
                if (sourceProduct != null) {
                    BufferedImage img = createQuickLookImage(product, true, true);
                    sourceProduct.dispose();
                    return img;
                }
            } catch (IOException e) {
                //
            }
        }
        try {
            return createQuickLookImage(product, true, false);
        } catch (IOException e) {
            return null;
        }
    }

    public static BufferedImage createQuickLook(final int id, final File productFile) throws IOException {
        boolean isBrowseFile = true;
        // check if quicklook exist with product
        File browseFile = findProductBrowseImage(productFile);
        if (browseFile == null) {
            browseFile = productFile;
            isBrowseFile = false;
        }

        final Product sourceProduct = ProductIO.readProduct(browseFile);
        if (sourceProduct != null) {
            BufferedImage img = createQuickLook(id, sourceProduct, isBrowseFile);
            return img;
        }
        return null;
    }

    private static BufferedImage loadFile(final File file) {
        BufferedImage bufferedImage = null;
        if (file.canRead()) {
            try {
                try (BufferedInputStream fis = new BufferedInputStream(new FileInputStream(file))) {
                    bufferedImage = ImageIO.read(fis);
                }
            } catch (Exception e) {
                //
            }
        }
        return bufferedImage;
    }

    private static Band[] getQuicklookBand(final Product product) {

        if(OperatorUtils.isQuadPol(product)) {
            return pauliVirtualBands(product);
        }

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

    private static Band[] pauliVirtualBands(final Product product) {

        final VirtualBand r = new VirtualBand("pauli_r",
                ProductData.TYPE_FLOAT32,
                product.getSceneRasterWidth(),
                product.getSceneRasterHeight(),
                "((i_HH-i_VV)*(i_HH-i_VV)+(q_HH-q_VV)*(q_HH-q_VV))/2");

        final VirtualBand g = new VirtualBand("pauli_g",
                ProductData.TYPE_FLOAT32,
                product.getSceneRasterWidth(),
                product.getSceneRasterHeight(),
                "((i_HV+i_VH)*(i_HV+i_VH)+(q_HV+q_VH)*(q_HV+q_VH))/2");

        final VirtualBand b = new VirtualBand("pauli_b",
                ProductData.TYPE_FLOAT32,
                product.getSceneRasterWidth(),
                product.getSceneRasterHeight(),
                "((i_HH+i_VV)*(i_HH+i_VV)+(q_HH+q_VV)*(q_HH+q_VV))/2");

        return new Band[] {r, g, b};
    }

    private static BufferedImage createQuickLookImage(final Product product, final boolean subsample, final boolean isBrowseFile) throws IOException {

        Product productSubset = product;

        if (subsample) {
            final int maxWidth = isBrowseFile ? MAX_WIDTH : MAX_WIDTH*(MULTILOOK_FACTOR*2);
            final ProductSubsetDef productSubsetDef = new ProductSubsetDef("subset");
            int scaleFactor = Math.round(Math.max(product.getSceneRasterWidth(), product.getSceneRasterHeight()) / (float)maxWidth);
            if (scaleFactor < 1) {
                scaleFactor = 1;
            }
            productSubsetDef.setSubSampling(scaleFactor, scaleFactor);

            productSubset = product.createSubset(productSubsetDef, null, null);
        }
        final Band[] quicklookBands = getQuicklookBand(productSubset);

        final BufferedImage image;
        if(quicklookBands.length < 3) {

            image = ProductUtils.createColorIndexedImage(average(productSubset, quicklookBands[0], isBrowseFile), ProgressMonitor.NULL);
            productSubset.dispose();
        } else {
            final List<Band> bandList = new ArrayList<>(3);
            for(int i=0; i < Math.min(3,quicklookBands.length); ++i) {
                final Band band = average(productSubset, quicklookBands[i], isBrowseFile);
                if(!isBrowseFile || band.getStx().getMean() != 0) {
                    bandList.add(band);
                }
            }
            final Band[] bands = bandList.toArray(new Band[bandList.size()]);

            if(bands.length < 3) {
                image = ProductUtils.createColorIndexedImage(bands[0], ProgressMonitor.NULL);
            } else {
                ImageInfo imageInfo = ProductUtils.createImageInfo(bands, true, ProgressMonitor.NULL);
                image = ProductUtils.createRgbImage(bands, imageInfo, ProgressMonitor.NULL);
            }
        }

        productSubset.dispose();
        return image;
    }

    private static Band average(final Product product, final Band srcBand, final boolean isBrowseFile) {

        if(isBrowseFile) {
            return srcBand;
        }

        int rangeFactor = MULTILOOK_FACTOR;
        int azimuthFactor = MULTILOOK_FACTOR;

        if(AbstractMetadata.hasAbstractedMetadata(product)) {
            final MetadataElement abs = AbstractMetadata.getAbstractedMetadata(product);
            final boolean srgrFlag = abs.getAttributeInt(AbstractMetadata.srgr_flag, 0) == 1;
            double rangeSpacing = abs.getAttributeDouble(AbstractMetadata.range_spacing, 1);
            double azimuthSpacing = abs.getAttributeDouble(AbstractMetadata.azimuth_spacing, 1);

            double groundRangeSpacing = rangeSpacing;
            if (rangeSpacing == AbstractMetadata.NO_METADATA) {
                azimuthSpacing = 1;
                groundRangeSpacing = 1;
            } else if (!srgrFlag) {
                final TiePointGrid incidenceAngle = OperatorUtils.getIncidenceAngle(product);
                if (incidenceAngle != null) {
                    final float x = product.getSceneRasterWidth() / 2f;
                    final float y = product.getSceneRasterHeight() / 2f;
                    final double incidenceAngleAtCentreRangePixel = incidenceAngle.getPixelFloat(x, y);

                    groundRangeSpacing /= Math.sin(incidenceAngleAtCentreRangePixel * MathUtils.DTOR);
                }
            }

            final double nAzLooks = MULTILOOK_FACTOR * groundRangeSpacing / azimuthSpacing;
            if (nAzLooks < 1.0) {
                azimuthFactor = 1;
                rangeFactor = (int) Math.min(MULTILOOK_FACTOR * 2, Math.round(azimuthSpacing / groundRangeSpacing));
            } else {
                azimuthFactor = (int) Math.min(MULTILOOK_FACTOR * 2, Math.round(nAzLooks));
            }
        }

        final int rangeAzimuth = rangeFactor * azimuthFactor;

        final int srcW = srcBand.getRasterWidth();
        final int srcH = srcBand.getRasterHeight();
        final int w = srcW / rangeFactor;
        final int h = srcH / azimuthFactor;
        int index = 0;
        final float[] data = new float[w * h];

        try {
            boolean bandAddedToProduct = false;
            if(product.getBand(srcBand.getName()) == null) {
                product.addBand(srcBand);
                bandAddedToProduct = true;
            }

            final float[] floatValues = new float[srcW*srcH];
            srcBand.readPixels(0, 0, srcW, srcH, floatValues, ProgressMonitor.NULL);

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
            }

            if(bandAddedToProduct) {
                product.removeBand(srcBand);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        Band b = new Band("averaged", ProductData.TYPE_FLOAT32, w, h);
        b.setData(ProductData.createInstance(data));
        return b;
    }

    private static BufferedImage createRenderedImage(byte[] array, int w, int h) {

        // create rendered image with dimension being width by height
        final SampleModel sm = RasterFactory.createBandedSampleModel(DataBuffer.TYPE_BYTE, w, h, 1);
        final ColorModel cm = PlanarImage.createColorModel(sm);
        final DataBufferByte dataBuffer = new DataBufferByte(array, array.length);
        final WritableRaster writeraster = RasterFactory.createWritableRaster(sm, dataBuffer, new Point(0, 0));

        return new BufferedImage(cm, writeraster, cm.isAlphaPremultiplied(), null);
    }

    private static File findProductBrowseImage(final File productFile) {

        if (ZipUtils.isZip(productFile)) {
            try {
                if (ZipUtils.findInZip(productFile, "s1", "quick-look.png")) {
                    VirtualDir zipDir = VirtualDir.create(productFile);
                    String rootFolder = ZipUtils.getRootFolder(productFile, "manifest.safe");
                    return zipDir.getFile(rootFolder + "preview/quick-look.png");
                } //else if (ZipUtils.findInZip(productFile, "rs2", "browseimage.tif")) {
                  //  VirtualDir zipDir = VirtualDir.create(productFile);
                  //  String rootFolder = ZipUtils.getRootFolder(productFile, "product.xml");
                  //  return zipDir.getFile(rootFolder + "BrowseImage.tif");
                //}
            } catch (Exception e) {
                return null;
            }
            return null; //todo read quicklook from stream not yet supported
        } else {
            final File parentFolder = productFile.getParentFile();
            // try Sentinel-1
            File browseFile = new File(parentFolder, "preview" + File.separator + "quick-look.png");
            if (browseFile.exists())
                return browseFile;
            // try TerraSAR-X
            browseFile = new File(parentFolder, "PREVIEW" + File.separator + "BROWSE.tif");
            if (browseFile.exists())
                return browseFile;
            // try Radarsat-2
            //browseFile = new File(parentFolder, "BrowseImage.tif");
            //if (browseFile.exists())
            //    return browseFile;
            return null;
        }
    }

    private static BufferedImage createQuickLook(final int id, final Product product, final boolean isBrowseFile) {
        final File quickLookFile = getQuickLookFile(dbStorageDir, id);
        try {
            if (!dbStorageDir.exists())
                dbStorageDir.mkdirs();
            quickLookFile.createNewFile();
            final BufferedImage bufferedImage = createQuickLookImage(product, true, isBrowseFile);

            ImageIO.write(bufferedImage, "JPG", quickLookFile);
            return bufferedImage;
        } catch (Exception e) {
            System.out.println("Quicklook create data failed :" + product.getFileLocation() + "\n" + e.getMessage());
            quickLookFile.delete();
        }
        return null;
    }
}
