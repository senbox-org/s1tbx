package org.esa.beam.pixex.aggregators;

import org.esa.beam.pixex.calvalus.ma.AggregatedNumber;

public class MedianAggregatorStrategy implements AggregatorStrategy {

    @Override
    public float[] getValues(AggregatedNumber aggregatedNumber) {
        return new float[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public int getValueCount() {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String[] getSuffixes() {
        return new String[]{"median"};
    }
}
