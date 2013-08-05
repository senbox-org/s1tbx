package org.esa.beam.dataio;


class ExpectedBand {

    private String name;
    private String description;
    private String geophysicalUnit;
    private String noDataValue;
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

    ExpectedPixel[] getExpectedPixel() {
        return expectedPixel;
    }

    String getNoDataValue() {
        return noDataValue;
    }

    void setNoDataValue(String noDataValue) {
        this.noDataValue = noDataValue;
    }

    void setExpectedPixel(ExpectedPixel[] expectedPixel) {
        this.expectedPixel = expectedPixel;
    }
}
