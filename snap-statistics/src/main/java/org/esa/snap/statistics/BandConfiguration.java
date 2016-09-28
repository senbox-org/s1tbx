package org.esa.snap.statistics;

import org.esa.snap.core.gpf.annotations.Parameter;

public class BandConfiguration {

    @Parameter(description = "The name of the band in the source products. If empty, parameter 'expression' must be provided.")
    public String sourceBandName;

    @Parameter(description = "The band maths expression serving as input band. If empty, parameter 'sourceBandName' must be provided.")
    public String expression;

    @Parameter(description = "The band maths expression serving as criterion for whether to consider pixels for computation.")
    public String validPixelExpression;

    public BandConfiguration() {
        // used by DOM converter
    }
}
