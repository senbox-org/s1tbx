package org.esa.beam.dataio.rtp;

import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("band")
class BandDescriptor {
    private String name;
    private String dataType;
    private String description;
    private double scalingOffset;
    private double scalingFactor;
    private String expression;

    BandDescriptor() {
    }

    BandDescriptor(String name, String dataType, double scalingOffset, double scalingFactor, String expression, String description) {
        this.name = name;
        this.dataType = dataType;
        this.description = description;
        this.scalingOffset = scalingOffset;
        this.scalingFactor = scalingFactor;
        this.expression = expression;
    }

    public String getName() {
        return name;
    }

    public String getDataType() {
        return dataType;
    }

    public String getDescription() {
        return description;
    }

    public double getScalingOffset() {
        return scalingOffset;
    }

    public double getScalingFactor() {
        return scalingFactor;
    }

    public String getExpression() {
        return expression;
    }
}
