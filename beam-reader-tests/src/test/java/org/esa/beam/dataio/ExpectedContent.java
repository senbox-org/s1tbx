package org.esa.beam.dataio;


class ExpectedContent {
    private String id;
    private ExpectedBand[] bands;

    ExpectedContent() {
        bands = new ExpectedBand[0];
    }

    String getId() {
        return id;
    }

    void setId(String id) {
        this.id = id;
    }

    ExpectedBand[] getBands() {
        return bands;
    }

    void setBands(ExpectedBand[] bands) {
        this.bands = bands;
    }
}
