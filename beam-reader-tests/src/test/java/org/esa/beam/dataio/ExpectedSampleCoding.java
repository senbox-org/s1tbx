package org.esa.beam.dataio;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Marco Peters
 */
public class ExpectedSampleCoding {
    @JsonProperty(required = true)
    private String name;
    @JsonProperty
    private ExpectedSample[] samples;

    public String getName() {
        return name;
    }

    public ExpectedSample[] getSamples() {
        return samples;
    }

}
