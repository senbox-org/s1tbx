package org.esa.beam.dataio;


import com.fasterxml.jackson.annotation.JsonProperty;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.util.StringUtils;

import java.util.Random;

class ExpectedBand {

    @JsonProperty(required = true)
    private String name;
    @JsonProperty
    private String description;
    @JsonProperty
    private String geophysicalUnit;
    @JsonProperty
    private String noDataValue;
    @JsonProperty
    private String noDataValueUsed;
    @JsonProperty
    private String spectralWavelength;
    @JsonProperty
    private String spectralBandwidth;
    @JsonProperty
    private ExpectedPixel[] expectedPixels;

    ExpectedBand() {
        expectedPixels = new ExpectedPixel[0];
    }

    public ExpectedBand(Band band, Random random) {
        this();
        this.name = band.getName();
        this.description = band.getDescription();
        this.geophysicalUnit = band.getUnit();
        this.noDataValue = String.valueOf(band.getGeophysicalNoDataValue());
        this.noDataValueUsed = String.valueOf(band.isNoDataValueUsed());
        this.spectralWavelength = String.valueOf(band.getSpectralWavelength());
        this.spectralBandwidth = String.valueOf(band.getSpectralBandwidth());
        this.expectedPixels = createExpectedPixels(band, random);
    }

    private ExpectedPixel[] createExpectedPixels(Band band, Random random) {
        final ExpectedPixel[] expectedPixels = new ExpectedPixel[2];
        for (int i = 0; i < expectedPixels.length; i++) {
            final int x = (int) (random.nextFloat() * band.getSceneRasterWidth());
            final int y = (int) (random.nextFloat() * band.getSceneRasterHeight());
            final float value = band.isPixelValid(x, y) ? band.getSampleFloat(x, y) : Float.NaN;
            expectedPixels[i] = new ExpectedPixel(x, y, value);
        }
        return expectedPixels;
    }

    String getName() {
        return name;
    }

    String getDescription() {
        return description;
    }

    boolean isDescriptionSet() {
        return StringUtils.isNotNullAndNotEmpty(description);
    }

    String getGeophysicalUnit() {
        return geophysicalUnit;
    }

    boolean isGeophysicalUnitSet() {
        return StringUtils.isNotNullAndNotEmpty(geophysicalUnit);
    }

    String getNoDataValue() {
        return noDataValue;
    }

    boolean isNoDataValueSet() {
        return StringUtils.isNotNullAndNotEmpty(noDataValue);
    }

    String isNoDataValueUsed() {
        return noDataValueUsed;
    }

    public boolean isNoDataValueUsedSet() {
        return StringUtils.isNotNullAndNotEmpty(noDataValueUsed);
    }


    String getSpectralWavelength() {
        return spectralWavelength;
    }

    boolean isSpectralWavelengthSet() {
        return StringUtils.isNotNullAndNotEmpty(spectralWavelength);
    }

    String getSpectralBandwidth() {
        return spectralBandwidth;
    }

    public boolean isSpectralBandWidthSet() {
        return StringUtils.isNotNullAndNotEmpty(spectralBandwidth);
    }

    ExpectedPixel[] getExpectedPixels() {
        return expectedPixels;
    }

}
