package org.esa.beam.statistics;

import org.esa.beam.framework.gpf.annotations.Parameter;

public class BandConfiguration {

    @Parameter(description = "The name of the band in the source products. If empty, parameter 'expression' must " +
            "be provided.")
    String sourceBandName;

    @Parameter(description =
                       "The band maths expression serving as input band. If empty, parameter 'sourceBandName'" +
                               "must be provided.")
    String expression;

    @Parameter(description = "The band maths expression serving as criterion for whether to consider pixels for " +
            "computation.")
    String validPixelExpression;

    public BandConfiguration() {
        // used by DOM converter
    }
}
