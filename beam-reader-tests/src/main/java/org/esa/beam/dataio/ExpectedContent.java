package org.esa.beam.dataio;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.ProductNodeGroup;
import org.esa.beam.framework.datamodel.SampleCoding;

import java.util.Random;

public class ExpectedContent {
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
    private ExpectedSampleCoding[] flagCodings;
    @JsonProperty
    private ExpectedSampleCoding[] indexCodings;
    @JsonProperty
    private ExpectedBand[] bands;

    ExpectedContent() {
        bands = new ExpectedBand[0];
        flagCodings = new ExpectedSampleCoding[0];
        indexCodings = new ExpectedSampleCoding[0];
    }

    public ExpectedContent(Product product, Random random) {
        this();
        this.id = product.getName();
        this.sceneWidth = product.getSceneRasterWidth();
        this.sceneHeight = product.getSceneRasterHeight();
        final ProductData.UTC endTime = product.getEndTime();
        if (endTime != null) {
            this.endTime = endTime.format();
        }
        final ProductData.UTC startTime = product.getStartTime();
        if (startTime != null) {
            this.startTime = startTime.format();
        }
        if (product.getGeoCoding() != null) {
            this.geoCoding = new ExpectedGeoCoding(product, random);
        }
        this.flagCodings = createExpectedSampleCodings(product.getFlagCodingGroup());
        this.indexCodings = createExpectedSampleCodings(product.getIndexCodingGroup());

        this.bands = createExpectedBands(product, random);
    }

    private ExpectedBand[] createExpectedBands(Product product, Random random) {
        final ExpectedBand[] expectedBands = new ExpectedBand[product.getNumBands()];
        for (int i = 0; i < expectedBands.length; i++) {
            expectedBands[i] = new ExpectedBand(product.getBandAt(i), random);
        }
        return expectedBands;
    }

    private ExpectedSampleCoding[] createExpectedSampleCodings(ProductNodeGroup<? extends SampleCoding> sampleCodingGroup) {
        ExpectedSampleCoding[] sampleCodings = new ExpectedSampleCoding[sampleCodingGroup.getNodeCount()];
        for (int i = 0; i < sampleCodings.length; i++) {
            final SampleCoding sampleCoding = sampleCodingGroup.get(i);
            sampleCodings[i] = new ExpectedSampleCoding(sampleCoding);
        }
        return sampleCodings;
    }

    String getId() {
        return id;
    }

    int getSceneWidth() {
        return sceneWidth;
    }

    @JsonIgnoreProperties
    public boolean isSceneWidthSet() {
        return sceneWidth != null;
    }

    int getSceneHeight() {
        return sceneHeight;
    }

    @JsonIgnoreProperties()
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

    @JsonIgnoreProperties
    boolean isGeoCodingSet(){
     return geoCoding != null;
    }

    ExpectedSampleCoding[] getFlagCodings() {
        return flagCodings;
    }

    ExpectedSampleCoding[] getIndexCodings() {
        return indexCodings;
    }

    ExpectedBand[] getBands() {
        return bands;
    }

}
