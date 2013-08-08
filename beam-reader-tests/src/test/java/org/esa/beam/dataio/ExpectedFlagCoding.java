package org.esa.beam.dataio;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Marco Peters
 */
public class ExpectedFlagCoding {
    @JsonProperty
    private String name;
    @JsonProperty
    private ExpectedFlag[] flags;

    public String getName() {
        return name;
    }

    public ExpectedFlag[] getFlags() {
        return flags;
    }

}
