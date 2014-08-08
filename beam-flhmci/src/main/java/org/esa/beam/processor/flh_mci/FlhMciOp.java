/*
 * Copyright (C) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.beam.processor.flh_mci;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.Validator;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.pointop.PixelOperator;
import org.esa.beam.framework.gpf.pointop.ProductConfigurer;
import org.esa.beam.framework.gpf.pointop.Sample;
import org.esa.beam.framework.gpf.pointop.SampleConfigurer;
import org.esa.beam.framework.gpf.pointop.WritableSample;
import org.esa.beam.framework.ui.BooleanExpressionConverter;
import org.esa.beam.jai.ResolutionLevel;
import org.esa.beam.jai.VirtualBandOpImage;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.util.StringUtils;

import javax.media.jai.OpImage;
import java.awt.image.Raster;

/**
 * An operator for computing fluorescence line height (FLH) or maximum chlorophyll index (MCI).
 *
 * @author Tom Block
 * @author Ralf Quast
 */
@OperatorMetadata(alias = "FlhMci", authors = "Tom Block, Ralf Quast", copyright = "Brockmann Consult GmbH",
                  category = "Optical Processing/Thematic Water Processing",
                  version = "2.0",
                  description = "Computes fluorescence line height (FLH) or maximum chlorophyll index (MCI).")
public class FlhMciOp extends PixelOperator {

    @SourceProduct(alias = "source", label = "Source product", description="The source product.")
    private Product sourceProduct;

    @Parameter(description = "The name for the lower wavelength band defining the baseline",
               rasterDataNodeType = Band.class)
    private String lowerBaselineBandName;
    @Parameter(description = "The name of the upper wavelength band defining the baseline",
               rasterDataNodeType = Band.class)
    private String upperBaselineBandName;
    @Parameter(description = " The name of the signal band, i.e. the band for which the baseline height is calculated",
               rasterDataNodeType = Band.class)
    private String signalBandName;
    @Parameter(description = "The name of the line height band in the target product",
               validator = NodeNameValidator.class)
    private String lineHeightBandName;
    @Parameter(description = "Activates or deactivates calculating the slope parameter",
               defaultValue = "true",
               label = "Generate slope parameter")
    private boolean slope;
    @Parameter(description = "The name of the slope band in the target product",
               validator = NodeNameValidator.class)
    private String slopeBandName;
    @Parameter(description = "A ROI-mask expression used to identify pixels of interest",
               converter = BooleanExpressionConverter.class)
    private String maskExpression;
    @Parameter(description = "The cloud correction factor used during calculation",
               defaultValue = "1.005")
    private float cloudCorrectionFactor;
    @Parameter(defaultValue = "NaN",
               label = "Invalid FLH/MCI value",
               description = "Value used to fill invalid FLH/MCI pixels")
    private float invalidFlhMciValue;

    private transient BaselineAlgorithm algorithm;
    private transient OpImage maskOpImage;

    private transient int currentPixel = 0;

    @Override
    protected void computePixel(int x, int y, Sample[] sourceSamples, WritableSample[] targetSamples) {
        checkCancellation();

        final float signal = sourceSamples[0].getFloat();
        final float lower = sourceSamples[1].getFloat();
        final float upper = sourceSamples[2].getFloat();

        if (isMasked(maskOpImage, x, y)) {
            targetSamples[0].set(algorithm.computeLineHeight(lower, upper, signal));
            if (slope) {
                targetSamples[1].set(algorithm.computeSlope(lower, upper));
            }
        } else {
            targetSamples[0].set(invalidFlhMciValue);
            if (slope) {
                targetSamples[1].set(invalidFlhMciValue);
            }
        }
    }

    private void checkCancellation() {
        if (currentPixel % 1000 == 0) {
            checkForCancellation();
            currentPixel = 0;
        }
        currentPixel++;
    }

    @Override
    protected void configureSourceSamples(SampleConfigurer sampleConfigurer) throws OperatorException {
        sampleConfigurer.defineSample(0, signalBandName);
        sampleConfigurer.defineSample(1, lowerBaselineBandName);
        sampleConfigurer.defineSample(2, upperBaselineBandName);
    }

    @Override
    protected void configureTargetSamples(SampleConfigurer sampleConfigurer) throws OperatorException {
        sampleConfigurer.defineSample(0, lineHeightBandName);
        if (slope) {
            sampleConfigurer.defineSample(1, slopeBandName);
        }
    }

    @Override
    protected void configureTargetProduct(ProductConfigurer productConfigurer) {
        super.configureTargetProduct(productConfigurer);

        final String validPixelExpression = createValidMaskExpression();

        final Band lineHeightBand = productConfigurer.addBand(lineHeightBandName, ProductData.TYPE_FLOAT32);

        final Band signalBand = sourceProduct.getBand(signalBandName);
        lineHeightBand.setUnit(signalBand.getUnit());
        lineHeightBand.setDescription("Line height band");
        lineHeightBand.setValidPixelExpression(validPixelExpression);
        lineHeightBand.setNoDataValueUsed(true);
        lineHeightBand.setNoDataValue(invalidFlhMciValue);

        ProductUtils.copySpectralBandProperties(signalBand, lineHeightBand);

        if (slope) {
            final Band slopeBand = productConfigurer.addBand(slopeBandName, ProductData.TYPE_FLOAT32);
            slopeBand.setUnit(signalBand.getUnit() + " nm-1");
            slopeBand.setDescription("Baseline slope band");
            slopeBand.setNoDataValueUsed(true);
            slopeBand.setNoDataValue(Double.NaN);
            slopeBand.setValidPixelExpression(validPixelExpression);
        }

        ProductUtils.copyFlagBands(sourceProduct, productConfigurer.getTargetProduct(), true);
    }

    private String createValidMaskExpression() {
        if (maskExpression != null && !maskExpression.trim().isEmpty()) {
            return maskExpression;
        }
        final String expression;
        if (sourceProduct.getBand("l1_flags") != null && sourceProduct.getFlagCodingGroup().get("l1_flags") != null) {
            expression = "!l1_flags.INVALID && !l1_flags.LAND_OCEAN";
        } else if (sourceProduct.getBand("l2_flags") != null && sourceProduct.getFlagCodingGroup().get("l2_flags") != null) {
            expression = "l2_flags.WATER && !l2_flags.CLOUD";
        } else {
            expression = null;
        }
        return expression;
    }

    @Override
    public void dispose() {
        super.dispose();
        if (maskOpImage != null) {
            maskOpImage.dispose();
        }
    }

    @Override
    protected void prepareInputs() throws OperatorException {
        super.prepareInputs();
        validateParameters();

        final float lambda1 = getWavelength(lowerBaselineBandName);
        final float lambda2 = getWavelength(signalBandName);
        final float lambda3 = getWavelength(upperBaselineBandName);

        algorithm = new BaselineAlgorithm();
        algorithm.setWavelengths(lambda1, lambda3, lambda2);
        algorithm.setCloudCorrectionFactor(cloudCorrectionFactor);

        if (maskExpression != null && !maskExpression.isEmpty()) {
            maskOpImage = VirtualBandOpImage.createMask(maskExpression, sourceProduct, ResolutionLevel.MAXRES);
        }
    }

    private void validateParameters() throws OperatorException {
        assertParameterBandNameValid(lowerBaselineBandName, "lowerBaselineBandName");
        assertParameterBandNameValid(signalBandName, "signalBandName");
        assertParameterBandNameValid(upperBaselineBandName, "upperBaselineBandName");
        if (slope) {
            assertParameterBandNameValid(slopeBandName, "slopeBandName");
        }
    }

    private void assertParameterBandNameValid(String parameterValue, String parameterName) {
        if (StringUtils.isNullOrEmpty(parameterValue)) {
            throw new OperatorException(String.format("Parameter '%s' not specified", parameterName));
        }
    }

    private float getWavelength(String bandName) {
        final Band band = sourceProduct.getBand(bandName);
        final float wavelength = band.getSpectralWavelength();
        if (wavelength == 0.0f) {
            throw new OperatorException(
                    "The band '" + band.getName() + "' is not a spectral band.\nPlease select a spectral band for processing.");
        }
        return wavelength;
    }

    private static boolean isMasked(OpImage maskOpImage, int x, int y) {
        if (maskOpImage == null) {
            return true;
        }
        final int tileX = maskOpImage.XToTileX(x);
        final int tileY = maskOpImage.YToTileY(y);
        final Raster tile = maskOpImage.getTile(tileX, tileY);

        return tile.getSample(x, y, 0) != 0;
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(FlhMciOp.class);
        }
    }

    public static class NodeNameValidator implements Validator {

        @Override
        public void validateValue(Property property, Object value) throws ValidationException {
            ProductNode.isValidNodeName(value.toString());
        }
    }
}
