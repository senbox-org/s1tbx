package org.esa.beam.dataio;


import com.fasterxml.jackson.annotation.JsonProperty;

class ExpectedContent {
    @JsonProperty(required = true)
    private String id;
    @JsonProperty
    private Integer sceneWidth;
    @JsonProperty
    private Integer sceneHeight;
    @JsonProperty
    private String startTime;
    @JsonProperty
    private String endTime;
    @JsonProperty
    private ExpectedGeoCoding geoCoding;
    @JsonProperty
    private ExpectedFlagCoding[] flagCodings;
    @JsonProperty
    private ExpectedBand[] bands;

    ExpectedContent() {
        bands = new ExpectedBand[0];
        flagCodings = new ExpectedFlagCoding[0];
    }

    String getId() {
        return id;
    }

    int getSceneWidth() {
        return sceneWidth;
    }

    public boolean isSceneWidthSet() {
        return sceneWidth != null;
    }

    int getSceneHeight() {
        return sceneHeight;
    }

    public boolean isSceneHeightSet() {
        return sceneHeight != null;
    }

    String getStartTime() {
        return startTime;
    }

    boolean isStartTimeSet() {
        return startTime != null;
    }

    String getEndTime() {
        return endTime;
    }

    boolean isEndTimeSet() {
        return endTime != null;
    }

    ExpectedGeoCoding getGeoCoding() {
        return geoCoding;
    }

    boolean isGeoCodingSet(){
     return geoCoding != null;
    }

    ExpectedFlagCoding[] getFlagCodings() {
        return flagCodings;
    }

    ExpectedBand[] getBands() {
        return bands;
    }

}
