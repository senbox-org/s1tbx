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

    private static final int NEIGHBOUR_BAND_COUNT = 2;

    private ChrisFile chrisFile;

    private int sceneRasterWidth;
    private int sceneRasterHeight;
    private int spectralBandCount;

    private int[][] radianceData;
    private short[][] maskData;
    private boolean[] corrected;

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

        radianceData = new int[spectralBandCount][];
        maskData = new short[spectralBandCount][];
        corrected = new boolean[spectralBandCount];

        maskRefinement = new MaskRefinement(1.5);
        dropoutCorrection = new DropoutCorrection(2, sceneRasterWidth, sceneRasterHeight, true);

        return createProduct();
    }

    @Override
    protected void readBandRasterDataImpl(int sourceOffsetX,
                                          int sourceOffsetY,
                                          int sourceWidth,
                                          int sourceHeight,
                                          int sourceStepX,
                                          int sourceStepY,
                                          Band destBand,
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

        final int bandIndex = destBand.getSpectralBandIndex();

        Assert.state(bandIndex >= 0, "bandIndex < 0");
        Assert.state(bandIndex < spectralBandCount, "bandIndex >= chrisFile.getSpectralBandCount()");

        pm.beginTask(MessageFormat.format("Preparing band {0}...", bandIndex + 1), getWorkload(bandIndex));

        try {
            if (!corrected[bandIndex]) {
                for (int i = 1; i <= NEIGHBOUR_BAND_COUNT; ++i) {
                    if (pm.isCanceled()) {
                        break;
                    }
                    final int lowerNeighbour = bandIndex - i;
                    if (lowerNeighbour >= 0) {
                        loadRadianceAndMaskData(lowerNeighbour);
                    }
                    pm.worked(1);
                    final int upperNeighbour = bandIndex + i;
                    if (upperNeighbour < spectralBandCount) {
                        loadRadianceAndMaskData(upperNeighbour);
                    }
                    pm.worked(1);
                }
                loadRadianceAndMaskData(bandIndex);
                pm.worked(1);

                dropoutCorrection.perform(radianceData, maskData, bandIndex, NEIGHBOUR_BAND_COUNT,
                                          new Rectangle(0, 0, sceneRasterWidth, sceneRasterHeight));
                corrected[bandIndex] = false;

                pm.worked(1);
            }

            Object data;

            if (destBand.getName().startsWith("rad")) {
                data = radianceData[bandIndex];
            } else {
                data = maskData[bandIndex];
            }
            for (int i = 0; i < targetHeight; ++i) {
                System.arraycopy(data, (sourceOffsetY + i) * sceneRasterWidth + sourceOffsetX,
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
            band.setDescription(MessageFormat.format("Radiance of band {0}", i + 1));
            band.setValidPixelExpression(new StringBuilder(getMaskBandName(i)).append(" == 0").toString());
        }
        for (int i = 0; i < spectralBandCount; ++i) {
            final String name = getMaskBandName(i);
            final Band band = product.addBand(name, ProductData.TYPE_INT16);

            band.setSpectralBandIndex(i);
            band.setDescription(MessageFormat.format("Quality mask of band {0}", i + 1));
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

    private int getWorkload(final int bandIndex) {
        if (corrected[bandIndex]) {
            return 1;
        }

        return 2 * NEIGHBOUR_BAND_COUNT + 3;
    }

    private void loadRadianceAndMaskData(final int bandIndex) throws IOException {
        final int length = sceneRasterWidth * sceneRasterHeight;

        if (radianceData[bandIndex] == null) {
            radianceData[bandIndex] = new int[length];
            chrisFile.readRciImageData(bandIndex, 0, 0, 1, 1, sceneRasterWidth, sceneRasterHeight,
                                       radianceData[bandIndex]);
        }

        if (maskData[bandIndex] == null) {
            maskData[bandIndex] = new short[length];

            if (chrisFile.hasMask()) {
                chrisFile.readMaskData(bandIndex, 0, 0, 1, 1, sceneRasterWidth, sceneRasterHeight, maskData[bandIndex]);
            }

            maskRefinement.perform(radianceData[bandIndex], sceneRasterWidth, maskData[bandIndex], 0, 0,
                                   sceneRasterWidth);
        }
    }

}
