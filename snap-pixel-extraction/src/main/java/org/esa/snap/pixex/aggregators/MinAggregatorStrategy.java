package org.esa.snap.pixex.aggregators;

import org.esa.snap.pixex.calvalus.ma.AggregatedNumber;
import org.esa.snap.pixex.calvalus.ma.Record;

/**
 * {@inheritDoc}
 * <p>
 * Retrieves the minimum value for a record.
 */
public class MinAggregatorStrategy extends AbstractAggregatorStrategy {

    /**
     * Returns the minimum value for the given record and raster index.
     *
     * @param record      The record containing the data for all rasters.
     * @param rasterIndex The raster the values shall be aggregated for.
     *
     * @return The minimum value.
     */
    @Override
    public Number[] getValues(Record record, int rasterIndex) {
        AggregatedNumber number = getAggregatedNumber(record, rasterIndex);
        return new Number[]{
                (float) number.min,
                number.nT
        };
    }

    @Override
    public String[] getSuffixes() {
        return new String[]{"min", NUM_PIXELS_SUFFIX};
    }

}
