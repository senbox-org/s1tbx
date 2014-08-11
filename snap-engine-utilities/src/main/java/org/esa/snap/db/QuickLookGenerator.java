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
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.dataio.ProductSubsetDef;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.VirtualBand;
import org.esa.beam.util.ProductUtils;
import org.esa.snap.util.ResourceUtils;

import javax.imageio.ImageIO;
import javax.media.jai.PlanarImage;
import javax.media.jai.RasterFactory;
import java.awt.*;
import java.awt.image.*;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Generates Quicklooks
 */
public class QuickLookGenerator {

    private static final String QUICKLOOK_PREFIX = "QL_";
    private static final String QUICKLOOK_EXT = ".jpg";
    private static final int MAX_WIDTH = 400;

    private static final File dbStorageDir = new File(ResourceUtils.getApplicationUserDir(true),
            ProductDB.DEFAULT_PRODUCT_DATABASE_NAME +
                    File.separator + "QuickLooks"
    );

    public static boolean quickLookExists(final ProductEntry entry) {
        final File quickLookFile = getQuickLookFile(dbStorageDir, entry.getId());
        return quickLookFile.exists() && quickLookFile.length() > 0;
    }

    public static BufferedImage loadQuickLook(final ProductEntry entry) {
        final File quickLookFile = getQuickLookFile(dbStorageDir, entry.getId());
        BufferedImage bufferedImage = null;
        if (quickLookFile.exists() && quickLookFile.length() > 0) {
            bufferedImage = loadFile(quickLookFile);
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

    private static BufferedImage createQuickLookImage(final Product product, final boolean preprocess) throws IOException {

        final String quicklookBandName = ProductUtils.findSuitableQuicklookBandName(product);
        String srcBandName = quicklookBandName;
        Product productSubset = product;

        if (preprocess) {
            final ProductSubsetDef productSubsetDef = new ProductSubsetDef("subset");
            int scaleFactor = Math.max(product.getSceneRasterWidth(), product.getSceneRasterHeight()) / MAX_WIDTH;
            if (scaleFactor < 1) {
                scaleFactor = 1;
            }
            productSubsetDef.setSubSampling(scaleFactor, scaleFactor);

            final Band srcBand = product.getBand(quicklookBandName);
            // if not db make db using a virtual band
            if (false) { //!srcBand.getUnit().contains("db")) {
                final String expression = quicklookBandName + "==0 ? 0 : 10 * log10(abs(" + quicklookBandName + "))";
                final VirtualBand virtBand = new VirtualBand("QuickLook",
                        ProductData.TYPE_FLOAT32,
                        srcBand.getSceneRasterWidth(),
                        srcBand.getSceneRasterHeight(),
                        expression);
                product.addBand(virtBand);
                srcBandName = virtBand.getName();
            } else {
                // if not virtual set as single band in subset
                if (!(srcBand instanceof VirtualBand)) {
                    productSubsetDef.setNodeNames(new String[]{quicklookBandName});
                }
            }

            productSubset = product.createSubset(productSubsetDef, null, null);
        }
        final BufferedImage image = ProductUtils.createColorIndexedImage(productSubset.getBand(srcBandName),
                ProgressMonitor.NULL);
        //if (productSubset.isCorrupt()) {
        //    product.setCorrupt(true);
        //}
        productSubset.dispose();

        return image;
    }

    private static BufferedImage average(BufferedImage image) {

        final int rangeFactor = 4;
        final int azimuthFactor = 4;
        final int rangeAzimuth = rangeFactor * azimuthFactor;
        final Raster raster = image.getData();

        final int w = image.getWidth() / rangeFactor;
        final int h = image.getHeight() / azimuthFactor;
        int index = 0;
        final byte[] data = new byte[w * h];

        for (int ty = 0; ty < h; ++ty) {
            final int yStart = ty * azimuthFactor;
            final int yEnd = yStart + azimuthFactor;

            for (int tx = 0; tx < w; ++tx) {
                final int xStart = tx * rangeFactor;
                final int xEnd = xStart + rangeFactor;

                double meanValue = 0.0;
                for (int y = yStart; y < yEnd; ++y) {
                    for (int x = xStart; x < xEnd; ++x) {

                        meanValue += raster.getSample(x, y, 0);
                    }
                }
                meanValue /= rangeAzimuth;

                data[index++] = (byte) meanValue;
            }
        }

        return createRenderedImage(data, w, h);
    }

    private static BufferedImage createRenderedImage(byte[] array, int w, int h) {

        // create rendered image with demension being width by height
        final SampleModel sm = RasterFactory.createBandedSampleModel(DataBuffer.TYPE_BYTE, w, h, 1);
        final ColorModel cm = PlanarImage.createColorModel(sm);
        final DataBufferByte dataBuffer = new DataBufferByte(array, array.length);
        final WritableRaster writeraster = RasterFactory.createWritableRaster(sm, dataBuffer, new Point(0, 0));

        return new BufferedImage(cm, writeraster, cm.isAlphaPremultiplied(), null);
    }

    private static File findProductBrowseImage(final File productFile) {

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
        browseFile = new File(parentFolder, "BrowseImage.tif");
        if (browseFile.exists())
            return browseFile;
        return null;
    }

    public static boolean createQuickLook(final int id, final File productFile) throws IOException {
        boolean preprocess = false;
        // check if quicklook exist with product
        File browseFile = findProductBrowseImage(productFile);
        if (browseFile == null) {
            browseFile = productFile;
            preprocess = true;
        }

        boolean isCorrupt = false;
        final Product sourceProduct = ProductIO.readProduct(browseFile);
        if (sourceProduct != null) {
            createQuickLook(id, sourceProduct, preprocess);
            //isCorrupt = sourceProduct.isCorrupt();

            sourceProduct.dispose();
        }
        return isCorrupt;
    }

    public static void createQuickLook(final int id, final Product product, final boolean preprocess) {
        final File quickLookFile = getQuickLookFile(dbStorageDir, id);
        try {
            if (!dbStorageDir.exists())
                dbStorageDir.mkdirs();
            quickLookFile.createNewFile();
            final BufferedImage bufferedImage = createQuickLookImage(product, true);

            if (true) {
                ImageIO.write(average(bufferedImage), "JPG", quickLookFile);
            } else {
                ImageIO.write(bufferedImage, "JPG", quickLookFile);
            }
        } catch (Exception e) {
            System.out.println("Quicklook create data failed :" + product.getFileLocation() + "\n" + e.getMessage());
            quickLookFile.delete();
        }
    }
}
