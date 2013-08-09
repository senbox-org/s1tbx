package org.esa.beam.dataio;

import com.fasterxml.jackson.annotation.JsonProperty;

class ExpectedPixel {
    @JsonProperty(required = true)
    private int x;
    @JsonProperty(required = true)
    private int y;
    @JsonProperty(required = true)
    private float value;

    ExpectedPixel() {
    }

    ExpectedPixel(int x, int y, float value) {
        this.x = x;
        this.y = y;
        this.value = value;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    float getValue() {
        return value;
    }

}
