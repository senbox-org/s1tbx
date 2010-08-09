/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import org.esa.beam.dataio.envisat.EnvisatConstants;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.gpf.operators.standard.BandMathsOp;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.util.math.RsMathUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import static org.esa.beam.dataio.envisat.EnvisatConstants.*;

@OperatorMetadata(alias = "Equalize",
                  description = "Performs removal of detector-to-detector systematic " +
                                "radiometric differences in MERIS L1b data products.",
                  authors = "Marc Bouvet (ESTEC), Marco Peters (Brockmann Consult)",
                  copyright = "(c) 2010 by Brockmann Consult",
                  version = "1.0")
public class EqualizationOp extends Operator {

    @Parameter(defaultValue = "true",
               label = "Perform Smile correction",
               description = "Whether to perform Smile correction or not.")
    private boolean doSmile;

    @SourceProduct(alias = "source", label = "Name", description = "The source product.")
    private Product sourceProduct;

    @TargetProduct(description = "The target product.")
    private Product targetProduct;

    private static final String ELEM_NAME_MPH = "MPH";
    private EqualizationLUT equalizationLUT;
    private long date;
    private SmileAlgorithm smileAlgorithm;
    private Band landMaskBand;
    private Band validBand;
    private HashMap<String, String> bandNameMap;


    @Override
    public void initialize() throws OperatorException {
        Guardian.assertTrue("Source product must contain band '" + MERIS_DETECTOR_INDEX_DS_NAME + "'.",
                            sourceProduct.containsBand(MERIS_DETECTOR_INDEX_DS_NAME));
        Guardian.assertTrue("Source product must contain band '" + MERIS_L1B_FLAGS_DS_NAME + "'.",
                            sourceProduct.containsBand(MERIS_L1B_FLAGS_DS_NAME));
        Guardian.assertTrue("Source product must contain tie-point grid '" + MERIS_SUN_ZENITH_DS_NAME + "'.",
                            sourceProduct.containsTiePointGrid(MERIS_SUN_ZENITH_DS_NAME));
        final ProductData.UTC startTime = sourceProduct.getStartTime();
        Guardian.assertNotNull("Source product must have a start time", startTime);

        try {
            equalizationLUT = new EqualizationLUT(getReprocessingVersion());
        } catch (IOException e) {
            throw new OperatorException("Not able to create LUT.", e);
        }
        // compute julian date
        final Calendar calendar = startTime.getAsCalendar();
        long productJulianDate = (long) JulianDate.julianDate(calendar.get(Calendar.YEAR),
                                                              calendar.get(Calendar.MONTH),
                                                              calendar.get(Calendar.DAY_OF_MONTH));
        date = productJulianDate - (long) JulianDate.julianDate(2002, 4, 1);

        try {
            smileAlgorithm = new SmileAlgorithm(sourceProduct.getProductType());
        } catch (IOException e) {
            throw new OperatorException("Not able to initialise SMILE algorithm.", e);
        }

        BandMathsOp landOceanMathsOp = BandMathsOp.createBooleanExpressionBand("l1_flags.LAND_OCEAN", sourceProduct);
        landMaskBand = landOceanMathsOp.getTargetProduct().getBandAt(0);
        BandMathsOp validMathsOp = BandMathsOp.createBooleanExpressionBand("not l1_flags.INVALID", sourceProduct);
        validBand = validMathsOp.getTargetProduct().getBandAt(0);


        // create the target product
        final int rasterWidth = sourceProduct.getSceneRasterWidth();
        final int rasterHeight = sourceProduct.getSceneRasterHeight();
        targetProduct = new Product(String.format("%s_Equalized", sourceProduct.getName()),
                                    String.format("%s_EQ", sourceProduct.getProductType()),
                                    rasterWidth, rasterHeight);
        ProductUtils.copyMetadata(sourceProduct, targetProduct);
        ProductUtils.copyTiePointGrids(sourceProduct, targetProduct);

        bandNameMap = new HashMap<String, String>();
        String[] sourceSpectralBandNames = getSpectralBandNames(sourceProduct);
        for (String spectralBandName : sourceSpectralBandNames) {
            final Band sourceBand = sourceProduct.getBand(spectralBandName);
            final String targetBandName = "reflec_" + (sourceBand.getSpectralBandIndex() + 1);
            final Band targetBand = targetProduct.addBand(targetBandName,
                                                          ProductData.TYPE_FLOAT32);
            bandNameMap.put(targetBandName, spectralBandName);
            targetBand.setDescription(sourceBand.getDescription());
            targetBand.setUnit(sourceBand.getUnit());
            targetBand.setValidPixelExpression(sourceBand.getValidPixelExpression());
            ProductUtils.copySpectralBandProperties(sourceBand, targetBand);
        }

        copyBand("detector_index");
        ProductUtils.copyFlagBands(sourceProduct, targetProduct);
        targetProduct.getBand("l1_flags").setSourceImage(sourceProduct.getBand("l1_flags").getSourceImage());
        ProductUtils.copyGeoCoding(sourceProduct, targetProduct);
        targetProduct.setStartTime(sourceProduct.getStartTime());
        targetProduct.setEndTime(sourceProduct.getEndTime());

    }

    @Override
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        pm.beginTask("Performing equalization...", 7);
        try {
            final Band sourceBand = sourceProduct.getBand(bandNameMap.get(targetBand.getName()));
            final Tile sourceBandTile = getSourceTile(sourceBand, targetTile.getRectangle(),
                                                      new SubProgressMonitor(pm, 1));
            final RasterDataNode detectorNode = sourceProduct.getRasterDataNode(
                    MERIS_DETECTOR_INDEX_DS_NAME);
            final Tile detectorSourceTile = getSourceTile(detectorNode, targetTile.getRectangle(),
                                                          new SubProgressMonitor(pm, 1));
            final RasterDataNode sunZenithGrid = sourceProduct.getRasterDataNode(MERIS_SUN_ZENITH_DS_NAME);
            final Tile sunZenithTile = getSourceTile(sunZenithGrid, targetTile.getRectangle(),
                                                     new SubProgressMonitor(pm, 1));
            Tile validMaskTile = getSourceTile(validBand, targetTile.getRectangle(), new SubProgressMonitor(pm, 1));

            Tile[] radianceTiles = new Tile[0];
            Tile landMaskTile = null;
            if (doSmile) {
                final int[] requiredBandIndices = smileAlgorithm.computeRequiredBandIndexes(
                        targetBand.getSpectralBandIndex());
                radianceTiles = new Tile[EnvisatConstants.MERIS_L1B_NUM_SPECTRAL_BANDS];
                for (int requiredBandIndex : requiredBandIndices) {
                    radianceTiles[requiredBandIndex] = getSourceTile(sourceProduct.getBandAt(requiredBandIndex),
                                                                     targetTile.getRectangle(),
                                                                     new SubProgressMonitor(pm, 1));
                }

                landMaskTile = getSourceTile(landMaskBand, targetTile.getRectangle(), new SubProgressMonitor(pm, 1));
            }

            for (int y = targetTile.getMinY(); y <= targetTile.getMaxY(); y++) {
                checkForCancellation(pm);
                for (int x = targetTile.getMinX(); x <= targetTile.getMaxX(); x++) {

                    final int detectorIndex = detectorSourceTile.getSampleInt(x, y);
                    if (detectorIndex != -1) {
                        double sourceSample = sourceBandTile.getSampleDouble(x, y);
                        if (doSmile && validMaskTile.getSampleBoolean(x, y)) {
                            sourceSample = smileAlgorithm.correct(x, y, targetBand.getSpectralBandIndex(),
                                                                  detectorIndex, radianceTiles,
                                                                  landMaskTile.getSampleBoolean(x, y));
                        }
                        final double sunZenithSample = sunZenithTile.getSampleDouble(x, y);
                        final double sourceReflectance = RsMathUtils.radianceToReflectance((float)sourceSample,
                                                                                           (float)sunZenithSample,
                                                                                           sourceBand.getSolarFlux());

                        final double[] coefficients = equalizationLUT.getCoefficients(targetBand.getSpectralBandIndex(),
                                                                                      detectorIndex);
                        double cEq = coefficients[0] +
                                     coefficients[1] * date +
                                     coefficients[2] * date * date;
                        double sample = sourceReflectance / cEq;
                        targetTile.setSample(x, y, sample);
                    }
                }
            }
            pm.worked(1);
        } finally {
            pm.done();
        }
    }

    static int parseReprocessingVersion(String processorName, float processorVersion) {
        if ("MERIS".equalsIgnoreCase(processorName)) {
            if (processorVersion >= 5.02f && processorVersion <= 5.05f) {
                return 2;
            }
        }
        if ("MEGS-PC".equalsIgnoreCase(processorName)) {
            if (processorVersion == 8.0f) { // todo (mp,ts): Also allow 8.x ?
                return 3;
            } else { //noinspection ConstantConditions
                if (processorVersion == 7.4f || processorVersion == 7.41f) {
                    return 2;
                }
            }
        }

        throw new OperatorException(String.format("Unknown reprocessing version %s.", processorVersion));
    }

    private int getReprocessingVersion() {
        final MetadataElement mphElement = sourceProduct.getMetadataRoot().getElement(ELEM_NAME_MPH);
        if (mphElement != null) {
            final String softwareVer = mphElement.getAttributeString("SOFTWARE_VER");
            if (softwareVer != null) {
                final String[] strings = softwareVer.split("/");
                final String processorName = strings[0];
                final String processorVersion = strings[1];
                final float version = Float.parseFloat(processorVersion);
                return parseReprocessingVersion(processorName, version);
            } else {
                throw new OperatorException(String.format("Not able to detect reprocessing version.\n%s",
                                                          "Metadata attribute 'MPH/SOFTWARE_VER' not found."));
            }
        }
        throw new OperatorException(
                String.format("Not able to detect reprocessing version.\n%s", "Metadata element 'MPH' not found."));
    }

    private void copyBand(String sourceBandName) {
        final Band destBand = ProductUtils.copyBand(sourceBandName, sourceProduct, targetProduct);
        Band srcBand = sourceProduct.getBand(sourceBandName);
        destBand.setSourceImage(srcBand.getSourceImage());
    }

    private String[] getSpectralBandNames(Product sourceProduct) {
        final Band[] bands = sourceProduct.getBands();
        final List<String> spectralBandNames = new ArrayList<String>(bands.length);
        for (Band band : bands) {
            if (band.getSpectralBandIndex() != -1) {
                spectralBandNames.add(band.getName());
            }
        }
        return spectralBandNames.toArray(new String[spectralBandNames.size()]);
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(EqualizationOp.class);
        }

    }
}
