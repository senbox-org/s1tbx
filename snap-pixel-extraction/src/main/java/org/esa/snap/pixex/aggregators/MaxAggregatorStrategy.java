package org.esa.snap.pixex.aggregators;

import org.esa.snap.pixex.calvalus.ma.AggregatedNumber;
import org.esa.snap.pixex.calvalus.ma.Record;

/**
 * {@inheritDoc}
 * <p>
 * Retrieves the maximum value for a record.
 */
public class MaxAggregatorStrategy extends AbstractAggregatorStrategy {

    /**
     * Returns the maximum value of the given record and the given raster index.
     *
     * @param record      The record containing the data for all rasters.
     * @param rasterIndex The raster the values shall be aggregated for.
     *
     * @return The maximum value.
     */
    @Override
    public Number[] getValues(Record record, int rasterIndex) {
        AggregatedNumber number = getAggregatedNumber(record, rasterIndex);
        return new Number[]{
                (float) number.max,
                number.nT
        };
    }

    @Override
    public String[] getSuffixes() {
        return new String[]{"max", NUM_PIXELS_SUFFIX};
    }

}
