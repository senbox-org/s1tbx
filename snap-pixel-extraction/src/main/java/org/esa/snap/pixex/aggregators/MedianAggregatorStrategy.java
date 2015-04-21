package org.esa.snap.pixex.aggregators;

import org.esa.snap.pixex.calvalus.ma.Record;

import java.util.Arrays;

/**
 * {@inheritDoc}
 * <p>
 * Retrieves the median value for a record.
 * If the record contains an even number of values, the mean of the left and the right median is taken.
 */
public class MedianAggregatorStrategy extends AbstractAggregatorStrategy {

    /**
     * Returns the median value for the given record and raster index.
     *
     * @param record      The record containing the data for all rasters.
     * @param rasterIndex The raster the values shall be aggregated for.
     * @return The median value.
     */
    @Override
    public Number[] getValues(Record record, int rasterIndex) {
        final float median = getMedian((Number[]) record.getAttributeValues()[rasterIndex]);
        return new Number[]{
                median,
                getAggregatedNumber(record, rasterIndex).nT
        };
    }

    @Override
    public String[] getSuffixes() {
        return new String[]{"median", NUM_PIXELS_SUFFIX};
    }

    float getMedian(Number[] bandValues) {
        if (bandValues == null || bandValues.length == 0) {
            return Float.NaN;
        }
        Number[] values = bandValues.clone();
        Arrays.sort(values);
        if (values.length % 2 == 1) {
            return values[values.length / 2].floatValue();
        }
        final float leftMedian = values[values.length / 2 - 1].floatValue();
        final float rightMedian = values[values.length / 2].floatValue();
        return (leftMedian + rightMedian) / 2;
    }
}
