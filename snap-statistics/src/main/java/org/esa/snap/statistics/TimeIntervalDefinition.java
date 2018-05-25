package org.esa.snap.statistics;

import org.esa.snap.core.gpf.annotations.Parameter;

public class TimeIntervalDefinition {

    @Parameter(description = "The amount of temporal units of which the interval consists.")
    public int amount;

    @Parameter(description = "The unit in which the amount is given. Must be one of the following: " +
            "days, weeks, months, years.", valueSet = {"days", "weeks", "months", "years"})
    public String unit;

    public TimeIntervalDefinition() {
        // used by DOM converter
    }
}
