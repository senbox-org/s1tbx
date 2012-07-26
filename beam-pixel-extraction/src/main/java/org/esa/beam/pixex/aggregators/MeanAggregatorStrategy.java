package org.esa.beam.pixex.aggregators;


import org.esa.beam.pixex.calvalus.ma.AggregatedNumber;

public class MeanAggregatorStrategy implements AggregatorStrategy {

    @Override
    public float[] getValues(AggregatedNumber aggregatedNumber) {
        float[] values = new float[2];
        values[0] = (float) aggregatedNumber.mean;
        values[1] = (float) aggregatedNumber.sigma;
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
