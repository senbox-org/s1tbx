package org.esa.beam.dataio;


class ExpectedContent {
    private String id;
    private Integer sceneWidth;
    private Integer sceneHeight;
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

    int getSceneWidth() {
        return sceneWidth;
    }

    void setSceneWidth(int sceneWidth) {
        this.sceneWidth = sceneWidth;
    }

    public boolean isSceneWidthSet() {
        return sceneWidth != null;
    }

    int getSceneHeight() {
        return sceneHeight;
    }

    void setSceneHeight(int sceneHeight) {
        this.sceneHeight = sceneHeight;
    }

    public boolean isSceneHeightSet() {
        return sceneHeight != null;
    }

    ExpectedBand[] getBands() {
        return bands;
    }

    void setBands(ExpectedBand[] bands) {
        this.bands = bands;
    }

}
