package org.esa.beam.dataio;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.SampleCoding;

/**
 * @author Marco Peters
 */
class ExpectedSampleCoding {
    @JsonProperty(required = true)
    private String name;
    @JsonProperty
    private ExpectedSample[] samples;


    ExpectedSampleCoding() {

    }

    ExpectedSampleCoding(SampleCoding sampleCoding) {
        this();
        this.name = sampleCoding.getName();

        samples = new ExpectedSample[sampleCoding.getNumAttributes()];
        for (int i = 0; i < samples.length; i++) {
            final MetadataAttribute sampleAttribute = sampleCoding.getAttributeAt(i);
            samples[i] = new ExpectedSample(sampleAttribute.getName(),
                                            sampleAttribute.getData().getElemUInt(),
                                            sampleAttribute.getDescription());
        }

    }

    public String getName() {
        return name;
    }

    public ExpectedSample[] getSamples() {
        return samples;
    }

}
