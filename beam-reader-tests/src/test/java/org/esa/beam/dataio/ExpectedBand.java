package org.esa.beam.dataio;


class ExpectedBand {

    private String name;
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

    ExpectedPixel[] getExpectedPixel() {
        return expectedPixel;
    }

    void setExpectedPixel(ExpectedPixel[] expectedPixel) {
        this.expectedPixel = expectedPixel;
    }
}
