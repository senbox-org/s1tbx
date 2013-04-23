package org.esa.beam.pixex.aggregators;

import org.esa.beam.pixex.calvalus.ma.Record;

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
    float[] getValues(Record record, int rasterIndex);

    /**
     * The number of values this strategy returns. Corresponds to the size of the return value of {@link org.esa.beam.pixex.aggregators.AggregatorStrategy#getValues(org.esa.beam.pixex.calvalus.ma.Record, int)}.
     *
     * @return The number of values.
     */
    int getValueCount();

    /**
     * The names of the statistical means this aggregator computes. Its size corresponds to the return value of {@link org.esa.beam.pixex.aggregators.AggregatorStrategy#getValueCount()}.
     *
     * @return The names of the statistical means this aggregator computes.
     */
    String[] getSuffixes();
}
