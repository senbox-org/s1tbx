package org.esa.beam.dataio;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Marco Peters
 */
class ExpectedSample {
    @JsonProperty(required = true)
    private String name;
    @JsonProperty(required = true)
    private long value;
    @JsonProperty
    private String description;


    ExpectedSample() {
    }

    public ExpectedSample(String name, long value, String description) {
        this();
        this.name = name;
        this.value = value;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public long getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }

}
