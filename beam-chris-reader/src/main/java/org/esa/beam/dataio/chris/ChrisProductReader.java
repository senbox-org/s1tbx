/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.beam.dataio.chris;

import com.bc.ceres.core.Assert;
import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.dataio.chris.internal.DropoutCorrection;
import org.esa.beam.dataio.chris.internal.MaskRefinement;
import org.esa.beam.dataio.chris.internal.SunPositionCalculator;
import org.esa.beam.framework.dataio.AbstractProductReader;
import org.esa.beam.framework.datamodel.Band;
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
import java.util.Calendar;
import java.util.Date;

import static java.lang.Math.max;
import static java.lang.Math.min;

/**
 * Reader for CHRIS/Proba products.
 *
 * @author Norman Fomferra
 * @author Ralf Quast
 * @author Marco Zuehlke
 * @version $Revision$ $Date$
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
        dropoutCorrection = new DropoutCorrection(DropoutCorrection.Type.N4, true);

        return createProduct(inputFile);
    }

    @Override
    protected synchronized void readBandRasterDataImpl(int sourceOffsetX,
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

    private Product createProduct(File inputFile) {
        final String name = FileUtils.getFilenameWithoutExtension(inputFile);
        final String type = "CHRIS_M" + chrisFile.getGlobalAttribute(ChrisConstants.ATTR_NAME_CHRIS_MODE, 0);

        final Product product = new Product(name, type, sceneRasterWidth, sceneRasterHeight, this);
        product.setFileLocation(chrisFile.getFile());

        setStartAndEndTimes(product);
        addMetadataElements(product);
        addRciAndMaskBands(product);
        addFlagCodingsAndMasks(product);

        // due to mask refinement the preferred tile size has full size
        product.setPreferredTileSize(sceneRasterWidth, sceneRasterHeight);

        return product;
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

    private void setStartAndEndTimes(final Product product) {
        final String dateStr = chrisFile.getGlobalAttribute(ChrisConstants.ATTR_NAME_IMAGE_DATE, "2000-01-01");
        final String timeStr = chrisFile.getGlobalAttribute(ChrisConstants.ATTR_NAME_IMAGE_CENTRE_TIME, "00:00:00");

        try {
            final DateFormat dateFormat = ProductData.UTC.createDateFormat("yyyy-MM-dd HH:mm:ss");
            final Date date = dateFormat.parse(dateStr + " " + timeStr);
            final ProductData.UTC utc = ProductData.UTC.create(date, 0);

            product.setStartTime(utc);
            product.setEndTime(utc);
        } catch (ParseException ignored) {
            // ignore
        }
    }

    private void addMetadataElements(final Product product) {
        final MetadataElement mph = new MetadataElement(ChrisConstants.MPH_NAME);

        for (final String name : chrisFile.getGlobalAttributeNames()) {
            if (ChrisConstants.ATTR_NAME_KEY_TO_MASK.equals(name)) {
                continue;
            }
            final String globalAttribute = chrisFile.getGlobalAttribute(name);
            mph.addAttribute(new MetadataAttribute(name, ProductData.createInstance(globalAttribute), true));

            if (ChrisConstants.ATTR_NAME_SOLAR_ZENITH_ANGLE.equals(name)) {
                addSolarAzimuthAngleIfPossible(product, mph);
            }
        }

        mph.addAttribute(new MetadataAttribute(ChrisConstants.ATTR_NAME_NOISE_REDUCTION,
                                               ProductData.createInstance("None"), true));

        final MetadataElement bandInfo = createBandInfo();

        product.getMetadataRoot().addElement(mph);
        product.getMetadataRoot().addElement(bandInfo);
    }

    private MetadataElement createBandInfo() {
        final MetadataElement bandInfo = new MetadataElement(ChrisConstants.BAND_INFORMATION_NAME);
        for (int i = 0; i < chrisFile.getSpectralBandCount(); i++) {
            final String name = MessageFormat.format("radiance_{0}", i + 1);
            final MetadataElement element = new MetadataElement(name);

            MetadataAttribute attribute = new MetadataAttribute("Cut-on Wavelength", ProductData.TYPE_FLOAT32);
            attribute.getData().setElemFloat(chrisFile.getCutOnWavelength(i));
            attribute.setDescription("Cut-on wavelength");
            attribute.setUnit("nm");
            element.addAttribute(attribute);

            attribute = new MetadataAttribute("Cut-off Wavelength", ProductData.TYPE_FLOAT32);
            attribute.getData().setElemFloat(chrisFile.getCutOffWavelength(i));
            attribute.setDescription("Cut-off wavelength");
            attribute.setUnit("nm");
            element.addAttribute(attribute);

            attribute = new MetadataAttribute("Central Wavelength", ProductData.TYPE_FLOAT32);
            attribute.getData().setElemFloat(chrisFile.getWavelength(i));
            attribute.setDescription("Central wavelength");
            attribute.setUnit("nm");
            element.addAttribute(attribute);

            attribute = new MetadataAttribute("Bandwidth", ProductData.TYPE_FLOAT32);
            attribute.getData().setElemFloat(chrisFile.getBandwidth(i));
            attribute.setDescription("Cut-off minus cut-on wavelength");
            attribute.setUnit("nm");
            element.addAttribute(attribute);

            attribute = new MetadataAttribute("Gain Setting", ProductData.TYPE_INT32);
            attribute.getData().setElemInt(chrisFile.getGainSetting(i));
            attribute.setDescription("CHRIS analogue electronics gain setting");
            element.addAttribute(attribute);

            attribute = new MetadataAttribute("Gain Value", ProductData.TYPE_FLOAT32);
            attribute.getData().setElemFloat(chrisFile.getGainValue(i));
            attribute.setDescription("Relative analogue gain");
            element.addAttribute(attribute);

            attribute = new MetadataAttribute("Low Row", ProductData.TYPE_INT32);
            attribute.getData().setElemInt(chrisFile.getLowRow(i));
            attribute.setDescription("CCD row number for the cut-on wavelength");
            element.addAttribute(attribute);

            attribute = new MetadataAttribute("High Row", ProductData.TYPE_INT32);
            attribute.getData().setElemInt(chrisFile.getHighRow(i));
            attribute.setDescription("CCD row number for the cut-off wavelength");
            element.addAttribute(attribute);

            bandInfo.addElement(element);
        }
        return bandInfo;
    }

    private void addSolarAzimuthAngleIfPossible(final Product product, final MetadataElement element) {
        try {
            final Calendar calendar = product.getStartTime().getAsCalendar();
            final double lat = Double.parseDouble(chrisFile.getGlobalAttribute(ChrisConstants.ATTR_NAME_TARGET_LAT));
            final double lon = Double.parseDouble(chrisFile.getGlobalAttribute(ChrisConstants.ATTR_NAME_TARGET_LON));

            final double saa = SunPositionCalculator.calculate(calendar, lat, lon).getAzimuthAngle();
            final ProductData data = ProductData.createInstance(String.format("%05.2f", saa));

            element.addAttribute(new MetadataAttribute(ChrisConstants.ATTR_NAME_SOLAR_AZIMUTH_ANGLE, data, true));
        } catch (Exception e) {
            // ignore
        }
    }

    private void addRciAndMaskBands(final Product product) {
        final String units = chrisFile.getGlobalAttribute(ChrisConstants.ATTR_NAME_CALIBRATION_DATA_UNITS);

        for (int i = 0; i < spectralBandCount; ++i) {
            final String name = MessageFormat.format("radiance_{0}", i + 1);
            final Band band = product.addBand(name, ProductData.TYPE_INT32);

            band.setSpectralBandIndex(i);
            band.setSpectralWavelength(chrisFile.getWavelength(i));
            band.setSpectralBandwidth(chrisFile.getBandwidth(i));
            band.setUnit(units);
            band.setDescription(MessageFormat.format("Radiance for spectral band {0}", i + 1));
            band.setValidPixelExpression(MessageFormat.format("mask_{0} != {1}", i + 1, Flags.DROPOUT.getMask()));

            rciBands[i] = band;
        }
        for (int i = 0; i < spectralBandCount; ++i) {
            final String name = MessageFormat.format("mask_{0}", i + 1);
            final Band band = product.addBand(name, ProductData.TYPE_INT16);

            band.setSpectralBandIndex(i);
            band.setSpectralWavelength(chrisFile.getWavelength(i));
            band.setSpectralBandwidth(chrisFile.getBandwidth(i));
            band.setDescription(MessageFormat.format("Quality mask for spectral band {0}", i + 1));

            maskBands[i] = band;
        }
        product.setAutoGrouping("radiance:mask");
    }

    private void addFlagCodingsAndMasks(final Product product) {
        final FlagCoding flagCoding = new FlagCoding("CHRIS");

        for (final Flags flag : Flags.values()) {
            flagCoding.addFlag(flag.toString(), flag.getMask(), flag.getDescription());
        }
        product.getFlagCodingGroup().add(flagCoding);
        for (final Band band : maskBands) {
            band.setSampleCoding(flagCoding);
        }

        addSpectrumMask(product, Flags.DROPOUT, "spectrum_dropout",
                        "Spectrum contains a dropout pixel");
        addSpectrumMask(product, Flags.SATURATED, "spectrum_saturated",
                        "Spectrum contains a saturated pixel");
        addSpectrumMask(product, Flags.DROPOUT_CORRECTED, "spectrum_dropout_corrected",
                        "Spectrum contains a corrected dropout pixel");

        for (int i = 0; i < spectralBandCount; ++i) {
            for (final Flags flag : Flags.values()) {

                final String name = new StringBuilder(rciBands[i].getName()).append("_").append(flag).toString();
                final String expr = new StringBuilder(maskBands[i].getName()).append(".").append(flag).toString();
                product.addMask(name,
                                expr, flag.getDescription(),
                                flag.getColor(),
                                flag.getTransparency());
            }
        }
    }

    private void addSpectrumMask(Product product, Flags flag, String name, String description) {
        final StringBuilder expression = new StringBuilder();
        for (int i = 0; i < spectralBandCount; ++i) {
            if (i > 0) {
                expression.append(" || ");
            }
            expression.append(maskBands[i].getName()).append(".").append(flag);
        }
        product.addMask(name,
                        expression.toString(), description,
                        flag.getColor(),
                        flag.getTransparency());
    }

    private void readRciBandRasterData(int bandIndex,
                                       ProductData targetBuffer,
                                       int targetOffsetX,
                                       int targetOffsetY,
                                       int targetWidth,
                                       int targetHeight,
                                       ProgressMonitor pm) throws IOException {
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

        try {
            pm.beginTask(MessageFormat.format("Preparing radiance band {0}...", bandIndex + 1), bandCount + 4);
            final int[][] rciData = new int[bandCount][sceneRasterWidth * tileHeight];
            final short[][] maskData = new short[bandCount][sceneRasterWidth * tileHeight];

            for (int i = minBandIndex, j = 1; i <= maxBandIndex; ++i) {
                if (i != bandIndex) {
                    // mask refinement requires a full-width tile
                    readFullWidthTile(i, rciData[j], maskData[j], tileOffsetY, tileHeight);
                    ++j;
                } else {
                    // mask refinement requires a full-width tile
                    readFullWidthTile(i, rciData[0], maskData[0], tileOffsetY, tileHeight);
                }
                pm.worked(1);
            }
            dropoutCorrection.compute(rciData, maskData, sceneRasterWidth, tileHeight,
                                      new Rectangle(targetOffsetX, targetOffsetY - tileOffsetY, targetWidth,
                                                    targetHeight));
            pm.worked(3);

            for (int i = 0; i < targetHeight; ++i) {
                //noinspection SuspiciousSystemArraycopy
                System.arraycopy(rciData[0], targetOffsetX + (targetOffsetY - tileOffsetY + i) * sceneRasterWidth,
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
                                        ProgressMonitor pm) throws IOException {
        try {
            pm.beginTask(MessageFormat.format("Preparing mask band {0}...", bandIndex + 1), targetHeight);
            final int[] rciData = new int[sceneRasterWidth * targetHeight];
            final short[] maskData = new short[sceneRasterWidth * targetHeight];

            // mask refinement requires a full-width tile
            readFullWidthTile(bandIndex, rciData, maskData, targetOffsetY, targetHeight);

            for (int i = 0; i < targetHeight; ++i) {
                //noinspection SuspiciousSystemArraycopy
                System.arraycopy(maskData, targetOffsetX + i * sceneRasterWidth, targetBuffer.getElems(),
                                 i * targetWidth, targetWidth);
                pm.worked(1);
            }
        } finally {
            pm.done();
        }
    }

    private void readFullWidthTile(int bandIndex, int[] rciData, short[] maskData, int tileOffsetY, int tileHeight)
            throws IOException {
        final Band rciBand = rciBands[bandIndex];
        final Band maskBand = maskBands[bandIndex];

        if (maskBand.hasRasterData()) {
            //noinspection SuspiciousSystemArraycopy
            System.arraycopy(maskBand.getRasterData().getElems(), tileOffsetY * sceneRasterWidth,
                             maskData, 0, maskData.length);
            if (rciBand.hasRasterData()) {
                //noinspection SuspiciousSystemArraycopy
                System.arraycopy(rciBand.getRasterData().getElems(), tileOffsetY * sceneRasterWidth,
                                 rciData, 0, rciData.length);
            } else {
                chrisFile.readRciData(bandIndex, 0, tileOffsetY, 1, 1, sceneRasterWidth, tileHeight, rciData);
            }
        } else {
            chrisFile.readRciData(bandIndex, 0, tileOffsetY, 1, 1, sceneRasterWidth, tileHeight, rciData);
            if (chrisFile.hasMask()) {
                chrisFile.readMaskData(bandIndex, 0, tileOffsetY, 1, 1, sceneRasterWidth, tileHeight, maskData);
            }
            maskRefinement.refine(rciData, maskData, sceneRasterWidth);
        }
    }
}
