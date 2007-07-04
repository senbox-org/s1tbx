/*
 * $Id: ChrisProductReader.java,v 1.17 2007/04/19 08:53:43 marcoz Exp $
 *
 * Copyright (C) 2002,2003  by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 */
package org.esa.beam.dataio.chris;

import com.bc.ceres.core.Assert;
import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.dataio.chris.internal.DropoutCorrection;
import org.esa.beam.dataio.chris.internal.MaskRefinement;
import org.esa.beam.framework.dataio.AbstractProductReader;
import org.esa.beam.framework.dataio.IllegalFileFormatException;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.BitmaskDef;
import org.esa.beam.framework.datamodel.FlagCoding;
import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.util.io.FileUtils;

import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import static java.lang.Math.max;
import static java.lang.Math.min;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.Date;

/**
 * Reader for CHRIS products.
 *
 * @author Norman Fomferra
 * @author Ralf Quast
 * @author Marco Zuehlke
 * @version $Revision: $ $Date: $
 */
public class ChrisProductReader extends AbstractProductReader {

    private static final int NEIGHBORING_BAND_COUNT = 1;

    private ChrisFile chrisFile;

    private int sceneRasterWidth;
    private int sceneRasterHeight;
    private int spectralBandCount;

    private MaskRefinement maskRefinement;
    private DropoutCorrection dropoutCorrection;

    /**
     * Constructor.
     *
     * @param productReaderPlugIn the product reader plug-in used to create this reader instance.
     */
    ChrisProductReader(final ChrisProductReaderPlugIn productReaderPlugIn) {
        super(productReaderPlugIn);
    }

    @Override
    protected Product readProductNodesImpl() throws IOException, IllegalFileFormatException {
        chrisFile = new ChrisFile(getInputFile());
        chrisFile.open();

        sceneRasterWidth = chrisFile.getSceneRasterWidth();
        sceneRasterHeight = chrisFile.getSceneRasterHeight();
        spectralBandCount = chrisFile.getSpectralBandCount();

        maskRefinement = new MaskRefinement(1.5);
        dropoutCorrection = new DropoutCorrection(2, true);

        return createProduct();
    }

    @Override
    protected void readBandRasterDataImpl(int sourceOffsetX,
                                          int sourceOffsetY,
                                          int sourceWidth,
                                          int sourceHeight,
                                          int sourceStepX,
                                          int sourceStepY,
                                          Band targetBand,
                                          int targetOffsetX,
                                          int targetOffsetY,
                                          int targetWidth,
                                          int targetHeight,
                                          ProductData targetBuffer,
                                          ProgressMonitor pm) throws IOException {
        Assert.state(sourceStepX == 1, "sourceStepX != 1");
        Assert.state(sourceStepY == 1, "sourceStepY != 1");
        Assert.state(sourceWidth == targetWidth, "sourceWidth != targetWidth");
        Assert.state(sourceHeight == targetHeight, "sourceHeight != targetHeight");

        final int bandIndex = targetBand.getSpectralBandIndex();

        int tileOffsetY = sourceOffsetY;
        int tileHeight = sourceHeight;

        if (tileOffsetY > 0) {
            tileOffsetY -= 1;
            tileHeight += 1;
        }
        if (tileOffsetY + tileHeight < sceneRasterHeight) {
            tileHeight += 1;
        }

        final int minBandIndex = max(bandIndex - NEIGHBORING_BAND_COUNT, 0);
        final int maxBandIndex = min(bandIndex + NEIGHBORING_BAND_COUNT, spectralBandCount - 1);

        final int[][] nr = new int[maxBandIndex - minBandIndex][sceneRasterWidth * tileHeight];
        final short[][] nm = new short[maxBandIndex - minBandIndex][sceneRasterWidth * tileHeight];

        final int[] radianceData = new int[sceneRasterWidth * tileHeight];
        final short[] maskData = new short[sceneRasterWidth * tileHeight];

        try {
            pm.beginTask(MessageFormat.format("Preparing band {0}...", bandIndex + 1), maxBandIndex - minBandIndex + 3);

            for (int i = minBandIndex, j = 0; i <= maxBandIndex; ++i) {
                if (i != bandIndex) {
                    readTile(i, tileOffsetY, tileHeight, nr[j], nm[j]);
                    ++j;
                } else {
                    readTile(i, tileOffsetY, tileHeight, radianceData, maskData);
                }
                pm.worked(1);
            }

            final Object data;

            if (targetBand.getName().startsWith("rad")) {
                final Rectangle sourceRectangle = new Rectangle(0, 0, sceneRasterWidth, tileHeight);
                final Rectangle targetRectangle = new Rectangle(sourceOffsetX,
                                                                sourceOffsetY - tileOffsetY,
                                                                targetWidth,
                                                                targetHeight);

                dropoutCorrection.perform(radianceData, maskData, nr, nm, sourceRectangle, radianceData, maskData,
                                          targetRectangle);
                data = radianceData;
            } else {
                data = maskData;
            }
            pm.worked(1);
            
            for (int i = 0; i < targetHeight; ++i) {
                System.arraycopy(data, (sourceOffsetY - tileOffsetY + i) * sceneRasterWidth + sourceOffsetX,
                                 targetBuffer.getElems(), i * targetWidth, targetWidth);
            }

            pm.worked(1);
        } finally {
            pm.done();
        }
    }

    @Override
    public void close() throws IOException {
        chrisFile.close();
        super.close();
    }

    private Product createProduct() {
        final String productName = FileUtils.getFilenameWithoutExtension(chrisFile.getFile());
        final String productType = "CHRIS_M" + chrisFile.getGlobalAttribute(ChrisConstants.ATTR_NAME_CHRIS_MODE, 0);

        final Product product = new Product(productName, productType, sceneRasterWidth, sceneRasterHeight, this);

        product.setFileLocation(chrisFile.getFile());
        addProductMetadata(product);
        setProductTime(product);
        addProductBands(product);
        addFlagCodingAndDefineBitmasks(product);

        return product;
    }

    private void addProductMetadata(final Product product) {
        final MetadataElement elem = new MetadataElement(ChrisConstants.MPH_NAME);

        for (final String name : chrisFile.getGlobalAttributeNames()) {
            final ProductData data = ProductData.createInstance(chrisFile.getGlobalAttribute(name));

            elem.addAttribute(new MetadataAttribute(name, data, true));
        }

        product.getMetadataRoot().addElement(elem);
    }

    private void setProductTime(final Product product) {
        final String dateStr = chrisFile.getGlobalAttribute(ChrisConstants.ATTR_NAME_IMAGE_DATE, "2000-01-01");
        final String timeStr = chrisFile.getGlobalAttribute(ChrisConstants.ATTR_NAME_IMAGE_CENTRE_TIME, "00:00:00");

        try {
            final DateFormat dateFormat = ProductData.UTC.createDateFormat("yyyy-MM-dd HH:mm:ss");
            final Date date = dateFormat.parse(dateStr + " " + timeStr);
            final ProductData.UTC utc = ProductData.UTC.create(date, 0);

            product.setStartTime(utc);
            product.setEndTime(utc);
        } catch (ParseException e) {
            // todo - collect warning
        }
    }

    private void addProductBands(final Product product) {
        final String units = chrisFile.getGlobalAttribute(ChrisConstants.ATTR_NAME_CALIBRATION_DATA_UNITS);

        for (int i = 0; i < spectralBandCount; ++i) {
            final String name = getRadianceBandName(i);
            final Band band = product.addBand(name, ProductData.TYPE_INT32);

            band.setSpectralBandIndex(i);
            band.setSpectralWavelength(chrisFile.getWavelength(i));
            band.setSpectralBandwidth(chrisFile.getBandwidth(i));
            band.setUnit(units);
            band.setDescription(MessageFormat.format("Radiance of spectral band {0}", i + 1));
            band.setValidPixelExpression(new StringBuilder(getMaskBandName(i)).append(" == 0").toString());
        }
        for (int i = 0; i < spectralBandCount; ++i) {
            final String name = getMaskBandName(i);
            final Band band = product.addBand(name, ProductData.TYPE_INT16);

            band.setSpectralBandIndex(i);
            band.setSpectralWavelength(chrisFile.getWavelength(i));
            band.setSpectralBandwidth(chrisFile.getBandwidth(i));
            band.setDescription(MessageFormat.format("Quality mask of spectral band {0}", i + 1));
        }
    }

    private static void addFlagCodingAndDefineBitmasks(final Product product) {
        final FlagCoding flagCoding = new FlagCoding("CHRIS");

        for (Flags flag : Flags.values()) {
            flagCoding.addFlag(flag.toString(), flag.getMask(), flag.getDescription());
        }

        product.addFlagCoding(flagCoding);

        for (final String name : product.getBandNames()) {
            if (name.startsWith("mask")) {
                final Band band = product.getBand(name);

                band.setFlagCoding(flagCoding);
                for (final Flags flag : Flags.values()) {
                    product.addBitmaskDef(
                            new BitmaskDef(
                                    new StringBuilder(name.replace("mask", "radiance")).append("_").append(
                                            flag.toString()).toString(),
                                    flag.getDescription(),
                                    new StringBuilder(name).append(".").append(flag.toString()).toString(),
                                    flag.getColor(),
                                    flag.getTransparency()));
                }
            }
        }
    }

    private File getInputFile() {
        final Object input = getInput();

        if (input instanceof String) {
            return new File((String) input);
        }
        if (input instanceof File) {
            return (File) input;
        }

        throw new IllegalArgumentException(MessageFormat.format("Unsupported input: {0}", input));
    }

    private static String getRadianceBandName(final int i) {
        return MessageFormat.format("radiance_{0}", i + 1);
    }

    private static String getMaskBandName(final int i) {
        return MessageFormat.format("mask_{0}", i + 1);
    }

    private void readTile(int bandIndex, int tileOffsetY, int tileHeight, int[] data, short[] mask)
            throws IOException {
        chrisFile.readRciImageData(bandIndex, 0, tileOffsetY, 1, 1, sceneRasterWidth, tileHeight, data);
        if (chrisFile.hasMask()) {
            chrisFile.readMaskData(bandIndex, 0, tileOffsetY, 1, 1, sceneRasterWidth, tileHeight, mask);
        }
        maskRefinement.refine(data, mask, sceneRasterWidth);
    }

}
