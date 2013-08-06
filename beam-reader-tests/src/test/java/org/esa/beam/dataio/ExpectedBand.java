package org.esa.beam.dataio;


class ExpectedBand {

    private String name;
    private String description;
    private String geophysicalUnit;
    private String noDataValue;
    private String noDataValueUsed;
    private String spectralWavelength;
    private String spectralBandwidth;
    private ExpectedPixel[] expectedPixel;

    ExpectedBand() {
        expectedPixel = new ExpectedPixel[0];
    }

    String getName() {
        return name;
    }

    void setName(String name) {
        this.name = name;
    }

    String getDescription() {
        return description;
    }

    void setDescription(String description) {
        this.description = description;
    }

    String getGeophysicalUnit() {
        return geophysicalUnit;
    }

    void setGeophysicalUnit(String geophysicalUnit) {
        this.geophysicalUnit = geophysicalUnit;
    }

    String getNoDataValue() {
        return noDataValue;
    }

    void setNoDataValue(String noDataValue) {
        this.noDataValue = noDataValue;
    }

    String isNoDataValueUsed() {
        return noDataValueUsed;
    }

    void setNoDataValueUsed(String noDataValueUsed) {
        this.noDataValueUsed = noDataValueUsed;
    }

    String getSpectralWavelength() {
        return spectralWavelength;
    }

    void setSpectralWavelength(String spectralWavelength) {
        this.spectralWavelength = spectralWavelength;
    }

    String getSpectralBandwidth() {
        return spectralBandwidth;
    }

    void setSpectralBandwidth(String spectralBandwidth) {
        this.spectralBandwidth = spectralBandwidth;
    }

    ExpectedPixel[] getExpectedPixel() {
        return expectedPixel;
    }

    void setExpectedPixel(ExpectedPixel[] expectedPixel) {
        this.expectedPixel = expectedPixel;
    }
}
