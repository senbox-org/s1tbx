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
package org.esa.beam.dataio.avhrr;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.dataio.avhrr.noaa.KlmAvhrrFile;
import org.esa.beam.framework.dataio.AbstractProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.FlagCoding;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.TiePointGeoCoding;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.esa.beam.framework.datamodel.VirtualBand;
import org.esa.beam.framework.dataop.maptransf.Datum;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

/**
 * A reader for AVHRR/3 Level-1b data products.
 *
 * @see <a href="http://www2.ncdc.noaa.gov/doc/klm/">NOAA KLM User's Guide</a>
 */
public class AvhrrReader extends AbstractProductReader implements AvhrrConstants {

    protected Product product;

    protected AvhrrFile avhrrFile;

    protected Map<Band, BandReader> bandReaders = new HashMap<Band, BandReader>();

    public AvhrrReader(ProductReaderPlugIn avhrrReaderPlugIn) {
        super(avhrrReaderPlugIn);
    }

    /**
     * Provides an implementation of the <code>readProductNodes</code>
     * interface method. Clients implementing this method can be sure that the
     * input object and eventually the subset information has already been set.
     * <p/>
     * <p/>
     * This method is called as a last step in the
     * <code>readProductNodes(input, subsetInfo)</code> method.
     *
     * @throws java.io.IOException if an I/O error occurs
     */
    @Override
    protected Product readProductNodesImpl() throws IOException {
        final File dataFile = AvhrrReaderPlugIn.getInputFile(getInput());

        try {
            avhrrFile = new KlmAvhrrFile(dataFile);
            avhrrFile.readHeader();
            createProduct();
            product.setFileLocation(dataFile);
        } catch (IOException e) {
            e.printStackTrace();
            try {
                close();
            } catch (IOException ignored) {
            }
            throw e;
        }

        return product;
    }

    @Override
    protected void readBandRasterDataImpl(int sourceOffsetX,
                                          int sourceOffsetY,
                                          int sourceWidth,
                                          int sourceHeight,
                                          int sourceStepX,
                                          int sourceStepY,
                                          final Band destBand,
                                          int destOffsetX,
                                          int destOffsetY,
                                          int destWidth,
                                          int destHeight,
                                          final ProductData destBuffer,
                                          final ProgressMonitor pm) throws IOException {

        final BandReader bandReader = bandReaders.get(destBand);
        if (bandReader == null) {
            throw new IllegalStateException("no band reader available");
        }

        bandReader.readBandRasterData(sourceOffsetX, sourceOffsetY, sourceWidth, sourceHeight,
                                      sourceStepX, sourceStepY, destBuffer, pm);
    }

    /**
     * Closes the access to all currently opened resources such as file input
     * streams and all resources of this children directly owned by this reader.
     * Its primary use is to allow the garbage collector to perform a vanilla
     * job. <p/>
     * <p/>
     * This method should be called only if it is for sure that this object
     * instance will never be used again. The results of referencing an instance
     * of this class after a call to <code>close()</code> are undefined. <p/>
     * <p/>
     * Overrides of this method should always call <code>super.close();</code>
     * after disposing this instance.
     *
     * @throws java.io.IOException if an I/O error occurs
     */
    @Override
    public void close() throws IOException {
        super.close();
        avhrrFile.dispose();
        avhrrFile = null;
        product = null;
    }

    @SuppressWarnings("unchecked")
    protected void createProduct() throws IOException {
        product = new Product(avhrrFile.getProductName(), PRODUCT_TYPE,
                              avhrrFile.getProductWidth(), avhrrFile.getProductHeight(), this);

        product.setDescription(PRODUCT_DESCRIPTION);
        final int channel3ab = avhrrFile.getChannel3abState();

        // ////////////////////////////////////////////////////////////////
        // Create the visual radiance and IR radiance bands

        product.addBand(createVisibleRadianceBand(CH_1));
        product.addBand(createVisibleRadianceBand(CH_2));
        if (channel3ab == CH_3A) {
            product.addBand(createVisibleRadianceBand(CH_3A));
            product.addBand(createZeroFilledBand(CH_3B,
                                                 RADIANCE_BAND_NAME_PREFIX));
        } else if (channel3ab == CH_3B) {
            product.addBand(createZeroFilledBand(CH_3A,
                                                 RADIANCE_BAND_NAME_PREFIX));
            product.addBand(createIrRadianceBand(CH_3B));
        } else {
            product.addBand(createVisibleRadianceBand(CH_3A));
            product.addBand(createIrRadianceBand(CH_3B));
        }
        product.addBand(createIrRadianceBand(CH_4));
        product.addBand(createIrRadianceBand(CH_5));

        // ////////////////////////////////////////////////////////////////
        // Create the visual reflectance and IR temperature bands

        product.addBand(createReflectanceFactorBand(CH_1));
        product.addBand(createReflectanceFactorBand(CH_2));
        if (channel3ab == CH_3A) {
            product.addBand(createReflectanceFactorBand(CH_3A));
            product.addBand(createZeroFilledBand(CH_3B,
                                                 TEMPERATURE_BAND_NAME_PREFIX));
        } else if (channel3ab == CH_3B) {
            product.addBand(createZeroFilledBand(CH_3A,
                                                 REFLECTANCE_BAND_NAME_PREFIX));
            product.addBand(createIrTemperatureBand(CH_3B));
        } else {
            product.addBand(createReflectanceFactorBand(CH_3A));
            product.addBand(createIrTemperatureBand(CH_3B));
        }
        product.addBand(createIrTemperatureBand(CH_4));
        product.addBand(createIrTemperatureBand(CH_5));

        // ////////////////////////////////////////////////////////////////
        // Create the inverted IR temperature bands

        addFlagCodingAndBitmaskDef();
        addCloudBand();
        product.setStartTime(avhrrFile.getStartDate());
        product.setEndTime(avhrrFile.getEndDate());
        avhrrFile.addMetaData(product.getMetadataRoot());

        addTiePointGrids();
    }

    protected Band createVisibleRadianceBand(int channel) throws IOException {
        BandReader bandReader = avhrrFile.createVisibleRadianceBandReader(channel);
        return createBand(bandReader, channel);
    }

    protected Band createIrRadianceBand(int channel) throws IOException {
        BandReader bandReader = avhrrFile.createIrRadianceBandReader(channel);
        return createBand(bandReader, channel);
    }

    protected Band createIrTemperatureBand(int channel) throws IOException {
        BandReader bandReader = avhrrFile.createIrTemperatureBandReader(channel);
        return createBand(bandReader, channel);
    }

    protected Band createReflectanceFactorBand(int channel) {
        BandReader bandReader = avhrrFile.createReflectanceFactorBandReader(channel);
        return createBand(bandReader, channel);
    }

    protected Band createBand(BandReader bandReader, int channel) {
        final Band band = new Band(bandReader.getBandName(), bandReader
                .getDataType(), avhrrFile.getProductWidth(), avhrrFile
                                           .getProductHeight());
        band.setScalingFactor(bandReader.getScalingFactor());
        band.setUnit(bandReader.getBandUnit());
        band.setDescription(bandReader.getBandDescription());
        band.setSpectralBandIndex(channel);
        band.setSpectralBandwidth(CH_BANDWIDTHS[channel]);
        band.setSpectralWavelength(CH_WAVELENGTHS[channel]);
        band.setValidPixelExpression(CH_VALID_MASK_EXPRESSIONS[channel]);
        band.setNoDataValue(NO_DATA_VALUE);
        band.setNoDataValueUsed(true);
        bandReaders.put(band, bandReader);
        return band;
    }

    protected Band createZeroFilledBand(int channel, String namePrefix) {
        final String name = namePrefix + CH_STRINGS[channel];
        final VirtualBand band = new VirtualBand(name,
                                                 ProductData.TYPE_FLOAT32, avhrrFile.getProductWidth(),
                                                 avhrrFile.getProductHeight(), "0");
        band.setUnit("-");
        band.setDescription("Zero-filled placeholder for " + name
                            + ", no data available");
        band.setSpectralBandIndex(channel);
        band.setSpectralBandwidth(CH_BANDWIDTHS[channel]);
        band.setSpectralWavelength(CH_WAVELENGTHS[channel]);
        band.setValidPixelExpression(null);
        band.setNoDataValueUsed(true);
        band.setNoDataValue(0.0);
        return band;
    }

    protected void addFlagCodingAndBitmaskDef() {
        BandReader bandReader = avhrrFile.createFlagBandReader();

        Band flagsBand = new Band(bandReader.getBandName(), bandReader
                .getDataType(), avhrrFile.getProductWidth(), avhrrFile
                                          .getProductHeight());

        FlagCoding fc = new FlagCoding(bandReader.getBandName());
        fc.setDescription("Flag coding for AVHRR data quality");

        addFlagAndBitmaskDef(fc, FLAG_QS, FLAG_QS_DESC, 0);
        addFlagAndBitmaskDef(fc, FLAG_SCANLINE, FLAG_SCANLINE_DESC, 1);
        addFlagAndBitmaskDef(fc, FLAG_3B, FLAG_SCANLINE_DESC, 2);
        addFlagAndBitmaskDef(fc, FLAG_4, FLAG_SCANLINE_DESC, 3);
        addFlagAndBitmaskDef(fc, FLAG_5, FLAG_SCANLINE_DESC, 4);
        addFlagAndBitmaskDef(fc, FLAG_SYNC, FLAG_SYNC_DESC, 5);

        flagsBand.setSampleCoding(fc);
        product.getFlagCodingGroup().add(fc);
        product.addBand(flagsBand);

        bandReaders.put(flagsBand, bandReader);
    }

    protected void addCloudBand() {
        if (avhrrFile.hasCloudBand()) {
            BandReader cloudReader = avhrrFile.createCloudBandReader();
            Band cloudMaskBand = new Band(cloudReader.getBandName(),
                                          cloudReader.getDataType(), avhrrFile.getProductWidth(),
                                          avhrrFile.getProductHeight());

            final String cloudBandName = cloudReader.getBandName();
            product.addMask("clear", cloudBandName + "==0", "", Color.LIGHT_GRAY, 0.4);
            product.addMask("probably_clear", cloudBandName + "==1", "", Color.YELLOW, 0.4);
            product.addMask("probably_cloudy", cloudBandName + "==2", "", Color.ORANGE, 0.4);
            product.addMask("cloudy", cloudBandName + "==3", "", Color.RED, 0.4);

            product.addBand(cloudMaskBand);
            bandReaders.put(cloudMaskBand, cloudReader);
        }
    }

    protected void addFlagAndBitmaskDef(FlagCoding fc, String flagName,
                                        String flagDesc, int shift) {
        final double rf1 = 0.0;
        final double gf1 = 1.0;
        final double bf1 = 0.5;
        final double a = 2.0 * Math.PI * (shift / 6.0);

        final float r = (float) (0.5 + 0.5 * Math.sin(a + rf1 * Math.PI));
        final float g = (float) (0.5 + 0.5 * Math.sin(a + gf1 * Math.PI));
        final float b = (float) (0.5 + 0.5 * Math.sin(a + bf1 * Math.PI));

        final Color color = new Color(r, g, b);

        fc.addFlag(flagName, 1 << shift, flagDesc);
        product.addMask(flagName, fc.getName() + "." + flagName, flagDesc, color, 0.4);
    }

    protected void addTiePointGrids() throws IOException {
        int tpSubsampling = avhrrFile.getTiePointSubsampling();
        final int gridHeight = avhrrFile.getProductHeight() / tpSubsampling + 1;

        String[] tiePointNames = avhrrFile.getTiePointNames();
        float[][] tiePointData = avhrrFile.getTiePointData();

        final int numGrids = tiePointNames.length;
        TiePointGrid grid[] = new TiePointGrid[numGrids];

        for (int i = 0; i < grid.length; i++) {
            grid[i] = createTiePointGrid(tiePointNames[i], TP_GRID_WIDTH,
                                         gridHeight, TP_OFFSET_X, TP_OFFSET_Y, tpSubsampling, tpSubsampling,
                                         tiePointData[i]);
            grid[i].setUnit(UNIT_DEG);
            product.addTiePointGrid(grid[i]);
        }

        GeoCoding geoCoding = new TiePointGeoCoding(grid[numGrids - 2],
                                                    grid[numGrids - 1], Datum.WGS_72);
        product.setGeoCoding(geoCoding);
        }

    public static String format(String pattern, String arg) {
        return new MessageFormat(pattern).format(new Object[]{arg});
    }
}