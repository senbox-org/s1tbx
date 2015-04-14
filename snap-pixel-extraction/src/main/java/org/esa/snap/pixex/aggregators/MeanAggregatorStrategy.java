package org.esa.snap.pixex.aggregators;


import org.esa.snap.pixex.calvalus.ma.AggregatedNumber;
import org.esa.snap.pixex.calvalus.ma.Record;

/**
 * {@inheritDoc}
 * Retrieves the mean and the sigma values for a record.
 */
public class MeanAggregatorStrategy extends AbstractAggregatorStrategy {

    /**
     * Returns the mean and the sigma values for the given record and raster index.
     *
     * @param record      The record containing the data for all rasters.
     * @param rasterIndex The raster the values shall be aggregated for.
     * @return The mean and the sigma values.
     */
    @Override
    public Number[] getValues(Record record, int rasterIndex) {
        AggregatedNumber number = getAggregatedNumber(record, rasterIndex);
        Number[] values = new Number[3];
        values[0] = (float) number.mean;
        values[1] = (float) number.sigma;
        values[2] = number.nT;
        return values;
    }

    @Override
    public String[] getSuffixes() {
        return new String[]{"mean", "sigma", NUM_PIXELS_SUFFIX};
    }
}
