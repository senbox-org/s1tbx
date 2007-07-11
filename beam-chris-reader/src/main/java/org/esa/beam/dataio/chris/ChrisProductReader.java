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

    private Band[] rciBands;
    private Band[] maskBands;

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
    protected Product readProductNodesImpl() throws IOException {
        final File inputFile = getInputFile();

        chrisFile = new ChrisFile(inputFile);
        chrisFile.open();

        sceneRasterWidth = chrisFile.getSceneRasterWidth();
        sceneRasterHeight = chrisFile.getSceneRasterHeight();
        spectralBandCount = chrisFile.getSpectralBandCount();

        rciBands = new Band[spectralBandCount];
        maskBands = new Band[spectralBandCount];

        maskRefinement = new MaskRefinement(1.5);
        dropoutCorrection = new DropoutCorrection(DropoutCorrection.Type.TWO, true);

        final String name = FileUtils.getFilenameWithoutExtension(inputFile);
        final String type = "CHRIS_M" + chrisFile.getGlobalAttribute(ChrisConstants.ATTR_NAME_CHRIS_MODE, 0);

        final Product product = new Product(name, type, sceneRasterWidth, sceneRasterHeight, this);
        product.setFileLocation(chrisFile.getFile());

        setMetadataElements(product);
        setStartAndEndTimes(product);
        setRciAndMaskBands(product);
        setFlagCodingsAndDefineBitmasks(product);

        return product;
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
        Assert.state(sourceOffsetX == targetOffsetX, "sourceOffsetX != targetOffsetX");
        Assert.state(sourceOffsetY == targetOffsetY, "sourceOffsetY != targetOffsetY");
        Assert.state(sourceStepX == 1, "sourceStepX != 1");
        Assert.state(sourceStepY == 1, "sourceStepY != 1");
        Assert.state(sourceWidth == targetWidth, "sourceWidth != targetWidth");
        Assert.state(sourceHeight == targetHeight, "sourceHeight != targetHeight");

        final int index = targetBand.getSpectralBandIndex();

        if (targetBand.equals(rciBands[index])) {
            readRciBandRasterData(index, targetBuffer, sourceOffsetX, sourceOffsetY, targetWidth, targetHeight, pm);
        } else {
            readMaskBandRasterData(index, targetBuffer, sourceOffsetX, sourceOffsetY, targetWidth, targetHeight, pm);
        }
    }

    @Override
    public void close() throws IOException {
        rciBands = null;
        maskBands = null;

        chrisFile.close();
        super.close();
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

    private void setMetadataElements(final Product product) {
        final MetadataElement element = new MetadataElement(ChrisConstants.MPH_NAME);

        for (final String name : chrisFile.getGlobalAttributeNames()) {
            if (!ChrisConstants.ATTR_NAME_KEY_TO_MASK.equals(name)) {
                final ProductData data = ProductData.createInstance(chrisFile.getGlobalAttribute(name));
                element.addAttribute(new MetadataAttribute(name, data, true));
            }
        }

        product.getMetadataRoot().addElement(element);
    }

    private void setStartAndEndTimes(final Product product) {
        final String dateStr = chrisFile.getGlobalAttribute(ChrisConstants.ATTR_NAME_IMAGE_DATE, "2000-01-01");
        final String timeStr = chrisFile.getGlobalAttribute(ChrisConstants.ATTR_NAME_IMAGE_CENTRE_TIME, "00:00:00");

        try {
            final DateFormat dateFormat = ProductData.UTC.createDateFormat("yyyy-MM-dd HH:mm:ss");
            final Date date = dateFormat.parse(dateStr + " " + timeStr);
            final ProductData.UTC utc = ProductData.UTC.create(date, 0);

            product.setStartTime(utc);
            product.setEndTime(utc);
        } catch (ParseException e) {
            // todo - warning
        }
    }

    private void setRciAndMaskBands(final Product product) {
        final String units = chrisFile.getGlobalAttribute(ChrisConstants.ATTR_NAME_CALIBRATION_DATA_UNITS);

        for (int i = 0; i < spectralBandCount; ++i) {
            final String name = MessageFormat.format("radiance_{0}", i + 1);
            final Band band = product.addBand(name, ProductData.TYPE_INT32);

            band.setSpectralBandIndex(i);
            band.setSpectralWavelength(chrisFile.getWavelength(i));
            band.setSpectralBandwidth(chrisFile.getBandwidth(i));
            band.setUnit(units);
            band.setDescription(MessageFormat.format("Radiance of spectral band {0}", i + 1));
            band.setValidPixelExpression(MessageFormat.format("mask_{0} == 0", i + 1));

            rciBands[i] = band;
        }
        for (int i = 0; i < spectralBandCount; ++i) {
            final String name = MessageFormat.format("mask_{0}", i + 1);
            final Band band = product.addBand(name, ProductData.TYPE_INT16);

            band.setSpectralBandIndex(i);
            band.setSpectralWavelength(chrisFile.getWavelength(i));
            band.setSpectralBandwidth(chrisFile.getBandwidth(i));
            band.setDescription(MessageFormat.format("Quality mask of spectral band {0}", i + 1));

            maskBands[i] = band;
        }
    }

    private void setFlagCodingsAndDefineBitmasks(final Product product) {
        final FlagCoding flagCoding = new FlagCoding("CHRIS");

        for (Flags flag : Flags.values()) {
            flagCoding.addFlag(flag.toString(), flag.getMask(), flag.getDescription());
        }

        product.addFlagCoding(flagCoding);

        for (int i = 0; i < spectralBandCount; ++i) {
            maskBands[i].setFlagCoding(flagCoding);
            for (final Flags flag : Flags.values()) {
                product.addBitmaskDef(new BitmaskDef(
                        new StringBuilder(rciBands[i].getName()).append("_").append(flag).toString(),
                        flag.getDescription(),
                        new StringBuilder(maskBands[i].getName()).append(".").append(flag).toString(),
                        flag.getColor(),
                        flag.getTransparency()));
            }
        }
        defineCompositeBitmask(product, Flags.DROPOUT, "Spectrum dropout",
                               "Spectrum contains a dropout pixel");
        defineCompositeBitmask(product, Flags.SATURATED, "Spectrum saturated",
                               "Spectrum contains a saturated pixel");
        defineCompositeBitmask(product, Flags.CORRECTED, "Spectrum corrected",
                               "Spectrum contains a corrected dropout pixel");
    }

    private void defineCompositeBitmask(Product product, Flags flag, String name, String description) {
        final StringBuilder expression = new StringBuilder();
        for (int i = 0; i < spectralBandCount; ++i) {
            if (i > 0) {
                expression.append(" || ");
            }
            expression.append(maskBands[i].getName()).append(".").append(flag);
        }
        product.addBitmaskDef(new BitmaskDef(
                name,
                description,
                expression.toString(),
                flag.getColor(),
                flag.getTransparency()));
    }

    private void readRciBandRasterData(int bandIndex,
                                       ProductData targetBuffer,
                                       int targetOffsetX,
                                       int targetOffsetY,
                                       int targetWidth,
                                       int targetHeight,
                                       ProgressMonitor pm)
            throws IOException {
        final int minBandIndex = max(bandIndex - NEIGHBORING_BAND_COUNT, 0);
        final int maxBandIndex = min(bandIndex + NEIGHBORING_BAND_COUNT, spectralBandCount - 1);
        final int bandCount = maxBandIndex - minBandIndex + 1;

        // compute tile size and offset
        int tileOffsetY = targetOffsetY;
        int tileHeight = targetHeight;
        if (tileOffsetY > 0) {
            tileOffsetY -= 1;
            tileHeight += 1;
        }
        if (tileOffsetY + tileHeight < sceneRasterHeight) {
            tileHeight += 1;
        }
        targetOffsetY -= tileOffsetY; // make target offset relative to tile

        try {
            pm.beginTask(MessageFormat.format("Preparing radiance band {0}...", bandIndex + 1), bandCount + 4);
            final int[][] rciData = new int[bandCount][sceneRasterWidth * tileHeight];
            final short[][] maskData = new short[bandCount][sceneRasterWidth * tileHeight];

            for (int i = minBandIndex, j = 1; i <= maxBandIndex; ++i) {
                if (i != bandIndex) {
                    // mask refinement requires a full-width tile
                    readTile(i, rciData[j], maskData[j], 0, tileOffsetY, sceneRasterWidth, tileHeight);
                    ++j;
                } else {
                    // mask refinement requires a full-width tile
                    readTile(i, rciData[0], maskData[0], 0, tileOffsetY, sceneRasterWidth, tileHeight);
                }
                pm.worked(1);
            }
            dropoutCorrection.compute(rciData, maskData, sceneRasterWidth, tileHeight,
                                      new Rectangle(targetOffsetX, targetOffsetY, targetWidth, targetHeight));
            pm.worked(3);

            for (int i = 0; i < targetHeight; ++i) {
                System.arraycopy(rciData[0], targetOffsetX + (targetOffsetY + i) * sceneRasterWidth,
                                 targetBuffer.getElems(), i * targetWidth, targetWidth);
            }
            pm.worked(1);
        } finally {
            pm.done();
        }
    }

    private void readMaskBandRasterData(int bandIndex,
                                        ProductData targetBuffer,
                                        int targetOffsetX,
                                        int targetOffsetY,
                                        int targetWidth,
                                        int targetHeight,
                                        ProgressMonitor pm)
            throws IOException {
        try {
            pm.beginTask(MessageFormat.format("Preparing mask band {0}...", bandIndex + 1), targetHeight);
            final int[] rciData = new int[sceneRasterWidth * targetHeight];
            final short[] maskData = new short[sceneRasterWidth * targetHeight];

            // mask refinement requires a full-width tile
            readTile(bandIndex, rciData, maskData, 0, targetOffsetY, sceneRasterWidth, targetHeight);

            for (int i = 0; i < targetHeight; ++i) {
                System.arraycopy(maskData, targetOffsetX + i * sceneRasterWidth, targetBuffer.getElems(),
                                 i * targetWidth, targetWidth);
                pm.worked(1);
            }
        } finally {
            pm.done();
        }
    }

    private void readTile(int bandIndex,
                          int[] rciData,
                          short[] maskData,
                          int tileOffsetX,
                          int tileOffsetY,
                          int tileWidth,
                          int tileHeight)
            throws IOException {
        final Band rciBand = rciBands[bandIndex];
        final Band maskBand = maskBands[bandIndex];

        if (maskBand.hasRasterData()) {
            System.arraycopy(maskBand.getRasterData().getElems(), tileOffsetX + tileOffsetY * tileWidth,
                             maskData, 0, maskData.length);
            if (rciBand.hasRasterData()) {
                System.arraycopy(rciBand.getRasterData().getElems(), tileOffsetX + tileOffsetY * tileWidth,
                                 rciData, 0, rciData.length);
            } else {
                chrisFile.readRciData(bandIndex, 0, tileOffsetY, 1, 1, tileWidth, tileHeight, rciData);
            }
        } else {
            chrisFile.readRciData(bandIndex, tileOffsetX, tileOffsetY, 1, 1, tileWidth, tileHeight, rciData);
            if (chrisFile.hasMask()) {
                chrisFile.readMaskData(bandIndex, tileOffsetX, tileOffsetY, 1, 1, tileWidth, tileHeight, maskData);
            }
            maskRefinement.refine(rciData, maskData, tileWidth);
        }
    }

}
