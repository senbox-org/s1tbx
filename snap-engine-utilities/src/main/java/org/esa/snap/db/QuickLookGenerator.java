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
import com.bc.ceres.core.SubProgressMonitor;
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
    private static final int MAX_WIDTH = 400;
    private static final int MULTILOOK_FACTOR = 1;

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
        // check if quicklook exist with product
        File browseFile = findProductBrowseImage(product.getFileLocation());
        if (browseFile != null) {
            try {
                final Product sourceProduct = ProductIO.readProduct(browseFile);
                if (sourceProduct != null) {
                    BufferedImage img = createQuickLookImage(product, true, false);
                    sourceProduct.dispose();
                    return img;
                }
            } catch (IOException e) {
                //
            }
        }
        try {
            return createQuickLookImage(product, true, true);
        } catch (IOException e) {
            return null;
        }
    }

    public static BufferedImage createQuickLook(final int id, final File productFile) throws IOException {
        boolean preprocess = false;
        // check if quicklook exist with product
        File browseFile = findProductBrowseImage(productFile);
        if (browseFile == null) {
            browseFile = productFile;
            preprocess = true;
        }

        final Product sourceProduct = ProductIO.readProduct(browseFile);
        if (sourceProduct != null) {
            BufferedImage img = createQuickLook(id, sourceProduct, preprocess);
            sourceProduct.dispose();
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

    private static String[] getQuicklookBand(final Product product) {
        final String[] bandNames = product.getBandNames();
        final List<String> nameList = new ArrayList<>(3);
        for(String name : bandNames) {
            if(name.toLowerCase().startsWith("intensity") || name.toLowerCase().startsWith("band")) {
                nameList.add(name);
                if(nameList.size() > 2)
                    break;
            }
        }
        if(!nameList.isEmpty()) {
            return nameList.toArray(new String[nameList.size()]);
        }
        String quicklookBandName = ProductUtils.findSuitableQuicklookBandName(product);

        // db
       // final String expression = quicklookBandName + "==0 ? 0 : 10 * log10(abs(" + quicklookBandName + "))";
      //  final VirtualBand virtBand = new VirtualBand("QuickLook",
      //          ProductData.TYPE_FLOAT32,
      //          srcBand.getSceneRasterWidth(),
      //          srcBand.getSceneRasterHeight(),
      //          expression);
      //  product.addBand(virtBand);
      //  srcBandName = virtBand.getName();


        return new String[] {quicklookBandName};
    }

    private static BufferedImage createQuickLookImage(final Product product, final boolean subsample, final boolean preprocess) throws IOException {

        final String[] quicklookBandNames = getQuicklookBand(product);
        Product productSubset = product;

        if (subsample) {
            final int maxWidth = preprocess ? MAX_WIDTH*(MULTILOOK_FACTOR*2) : MAX_WIDTH;
            final ProductSubsetDef productSubsetDef = new ProductSubsetDef("subset");
            int scaleFactor = Math.max(product.getSceneRasterWidth(), product.getSceneRasterHeight()) / maxWidth;
            if (scaleFactor < 1) {
                scaleFactor = 1;
            }
            productSubsetDef.setSubSampling(scaleFactor, scaleFactor);

            if(quicklookBandNames.length == 1) {
                final Band srcBand = product.getBand(quicklookBandNames[0]);
                // if not virtual set as single band in subset
                if (!(srcBand instanceof VirtualBand)) {
                    productSubsetDef.setNodeNames(new String[]{srcBand.getName()});
                }
            }
            productSubset = product.createSubset(productSubsetDef, null, null);
        }

        final BufferedImage image;
        if(quicklookBandNames.length == 1) {
            image = ProductUtils.createColorIndexedImage(productSubset.getBand(quicklookBandNames[0]), ProgressMonitor.NULL);
            productSubset.dispose();
        } else {
            List<Band> bandList = new ArrayList<>(3);
            for(int i=0; i < Math.min(3,quicklookBandNames.length); ++i) {
                bandList.add(productSubset.getBand(quicklookBandNames[i]));
            }
            final Band[] bands = bandList.toArray(new Band[bandList.size()]);
            ImageInfo imageInfo = ProductUtils.createImageInfo(bands, true, ProgressMonitor.NULL);
            image = ProductUtils.createRgbImage(bands, imageInfo, ProgressMonitor.NULL);
            productSubset.dispose();
        }

        return image;
    }

    private static BufferedImage average(final Product product, final BufferedImage image) {

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

        if (ZipUtils.isZip(productFile)) {
            try {
                if (ZipUtils.findInZip(productFile, "s1", "quick-look.png")) {
                    VirtualDir zipDir = VirtualDir.create(productFile);
                    String rootFolder = ZipUtils.getRootFolder(productFile, "manifest.safe");
                    return zipDir.getFile(rootFolder + "preview/quick-look.png");
                } else if (ZipUtils.findInZip(productFile, "rs2", "browseimage.tif")) {
                    VirtualDir zipDir = VirtualDir.create(productFile);
                    String rootFolder = ZipUtils.getRootFolder(productFile, "product.xml");
                    return zipDir.getFile(rootFolder + "BrowseImage.tif");
                }
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
            browseFile = new File(parentFolder, "BrowseImage.tif");
            if (browseFile.exists())
                return browseFile;
            return null;
        }
    }

    private static BufferedImage createQuickLook(final int id, final Product product, final boolean preprocess) {
        final File quickLookFile = getQuickLookFile(dbStorageDir, id);
        try {
            if (!dbStorageDir.exists())
                dbStorageDir.mkdirs();
            quickLookFile.createNewFile();
            final BufferedImage bufferedImage = createQuickLookImage(product, true, preprocess);

            if (preprocess) {
                ImageIO.write(average(product, bufferedImage), "JPG", quickLookFile);
            } else {
                ImageIO.write(bufferedImage, "JPG", quickLookFile);
            }
            return bufferedImage;
        } catch (Exception e) {
            System.out.println("Quicklook create data failed :" + product.getFileLocation() + "\n" + e.getMessage());
            quickLookFile.delete();
        }
        return null;
    }
}
