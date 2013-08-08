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

    public void setName(String name) {
        this.name = name;
    }

    public long getValue() {
        return value;
    }

    public void setValue(long value) {
        this.value = value;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
