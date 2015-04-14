package org.esa.snap.pixex.aggregators;

import org.esa.snap.pixex.calvalus.ma.Record;

/**
 * The aggregator strategies allow to aggregate band values into statistical values.
 *
 * @author Tonio Fincke
 * @author Thomas Storm
 */
public interface AggregatorStrategy {

    /**
     * Returns the value set for the given record and the given raster index.
     *
     * @param record      The record containing the data for all rasters.
     * @param rasterIndex The raster the values shall be aggregated for.
     * @return The aggregated values.
     */
    Number[] getValues(Record record, int rasterIndex);

    /**
     * The number of values this strategy returns. Corresponds to the size of the return value of {@link org.esa.snap.pixex.aggregators.AggregatorStrategy#getValues(org.esa.snap.pixex.calvalus.ma.Record, int)}.
     *
     * @return The number of values.
     */
    int getValueCount();

    /**
     * The names of the statistical means this aggregator computes. Its size corresponds to the return value of {@link org.esa.snap.pixex.aggregators.AggregatorStrategy#getValueCount()}.
     *
     * @return The names of the statistical means this aggregator computes.
     */
    String[] getSuffixes();
}
