package org.esa.beam.pixex.aggregators;


import org.esa.beam.pixex.calvalus.ma.AggregatedNumber;
import org.esa.beam.pixex.calvalus.ma.Record;

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
    public float[] getValues(Record record, int rasterIndex) {
        AggregatedNumber number = getAggregatedNumber(record, rasterIndex);
        float[] values = new float[2];
        values[0] = (float) number.mean;
        values[1] = (float) number.sigma;
        return values;
    }

    @Override
    public int getValueCount() {
        return 2;
    }

    @Override
    public String[] getSuffixes() {
        return new String[]{"mean", "sigma"};
    }
}
