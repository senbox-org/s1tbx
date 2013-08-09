package org.esa.beam.dataio;


import com.fasterxml.jackson.annotation.JsonProperty;
import org.esa.beam.util.StringUtils;

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
    private ExpectedPixel[] expectedPixel;

    ExpectedBand() {
        expectedPixel = new ExpectedPixel[0];
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

    ExpectedPixel[] getExpectedPixel() {
        return expectedPixel;
    }

}
