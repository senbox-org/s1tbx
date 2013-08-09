package org.esa.beam.dataio;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Marco Peters
 */
public class ExpectedFlag {
    @JsonProperty(required = true)
    private String name;
    @JsonProperty(required = true)
    private long value;
    @JsonProperty
    private String description;

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
