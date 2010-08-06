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
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.util.ProductUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

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

    @Parameter(defaultValue = "true",
               label = "Perform radiance to reflectance conversion",
               description = "Whether to perform radiance to reflectance conversion or not.")
    private boolean doRad2Refl;

    @SourceProduct(alias = "source", label = "Name", description = "The source product.")
    private Product sourceProduct;

    @TargetProduct(description = "The target product.")
    private Product targetProduct;

    private static final String ELEM_NAME_MPH = "MPH";
    private EqualizationLUT equalizationLUT;
    private long date;


    @Override
    public void initialize() throws OperatorException {
        try {
            equalizationLUT = new EqualizationLUT(getReprocessingVersion());
        } catch (IOException e) {
            throw new OperatorException("Not able to create LUT.", e);
        }
        // compute julian date
        final Calendar calendar = sourceProduct.getStartTime().getAsCalendar();
        long productJulianDate = (long) JulianDate.julianDate(calendar.get(Calendar.YEAR),
                                                              calendar.get(Calendar.MONTH),
                                                              calendar.get(Calendar.DAY_OF_MONTH));
        date = productJulianDate - (long) JulianDate.julianDate(2002, 4, 1);

        // create the target product
        final int rasterWidth = sourceProduct.getSceneRasterWidth();
        final int rasterHeight = sourceProduct.getSceneRasterHeight();
        targetProduct = new Product(String.format("%s_Equalized", sourceProduct.getName()),
                                    String.format("%s_EQ", sourceProduct.getProductType()),
                                    rasterWidth, rasterHeight);
        ProductUtils.copyMetadata(sourceProduct, targetProduct);
        ProductUtils.copyTiePointGrids(sourceProduct, targetProduct);

        String[] spectralBandNames = getSpectralBandNames(sourceProduct);
        for (String spectralBandName : spectralBandNames) {
            final Band sourceBand = sourceProduct.getBand(spectralBandName);
            final Band targetBand = targetProduct.addBand(spectralBandName, ProductData.TYPE_FLOAT32);
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
        final Tile bandSourceTile = getSourceTile(sourceProduct.getRasterDataNode(targetBand.getName()),
                                                  targetTile.getRectangle(),
                                                  ProgressMonitor.NULL);
        final Tile detectorSourceTile = getSourceTile(sourceProduct.getRasterDataNode("detector_index"),
                                                      targetTile.getRectangle(),
                                                      ProgressMonitor.NULL);
        final int bandIndex = targetBand.getSpectralBandIndex();

        for (Tile.Pos pos : targetTile) {
            if (doSmile) {
                // todo Smile correct value
            }
            if (doRad2Refl) {
                // todo convert to reflectances
            }

            final int detectorIndex = detectorSourceTile.getSampleInt(pos.x, pos.y);
            if (detectorIndex != -1) {
                final double[] coefficients = equalizationLUT.getCoefficients(bandIndex, detectorIndex);
                double sampleFactor = coefficients[0] +
                                      coefficients[1] * date +
                                      coefficients[2] * date * date;
                double sample = bandSourceTile.getSampleFloat(pos.x, pos.y) * sampleFactor;
                targetTile.setSample(pos.x, pos.y, sample);
            }
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
