package org.esa.beam.pixex.aggregators;

import org.esa.beam.pixex.calvalus.ma.AggregatedNumber;
import org.esa.beam.pixex.calvalus.ma.Record;

/**
 * {@inheritDoc}
 * <p/>
 * Retrieves the minimum value for a record.
 */
public class MinAggregatorStrategy extends AbstractAggregatorStrategy {

    /**
     * Returns the minimum value for the given record and raster index.
     *
     * @param record      The record containing the data for all rasters.
     * @param rasterIndex The raster the values shall be aggregated for.
     * @return The minimum value.
     */
    @Override
    public float[] getValues(Record record, int rasterIndex) {
        AggregatedNumber number = getAggregatedNumber(record, rasterIndex);
        return new float[]{(float) number.min};
    }

    @Override
    public String[] getSuffixes() {
        return new String[]{"min"};
    }

}
