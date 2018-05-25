package org.esa.snap.statistics;

import org.esa.snap.core.gpf.annotations.Parameter;

public class BandConfiguration {

    @Parameter(description = "The name of the band in the source products. If empty, parameter 'expression' must be provided.")
    public String sourceBandName;

    @Parameter(description = "The band maths expression serving as input band. If empty, parameter 'sourceBandName' must be provided.")
    public String expression;

    @Parameter(description = "The band maths expression serving as criterion for whether to consider pixels for computation.")
    public String validPixelExpression;

    @Parameter(description = "If true, from bands with integer values will be treated as categorical variables and measures will be" +
            "retrieved accordingly. (counts per integer value, names of two most frequent integer values). " +
            "If the band is an index band, class names will be extracted from it.", defaultValue = "false")
    public boolean retrieveCategoricalStatistics;

    public BandConfiguration() {
        // used by DOM converter
    }
}
