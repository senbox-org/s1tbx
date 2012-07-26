package org.esa.beam.pixex.aggregators;

import org.esa.beam.pixex.calvalus.ma.AggregatedNumber;

public class MinAggregatorStrategy implements AggregatorStrategy {

    @Override
    public float[] getValues(AggregatedNumber aggregatedNumber) {
        return new float[]{(float) aggregatedNumber.min};
    }

    @Override
    public int getValueCount() {
        return 1;
    }

    @Override
    public String[] getSuffixes() {
        return new String[]{"min"};
    }

}
