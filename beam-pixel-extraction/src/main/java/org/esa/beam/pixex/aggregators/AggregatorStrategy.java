package org.esa.beam.pixex.aggregators;

import org.esa.beam.pixex.calvalus.ma.AggregatedNumber;

public interface AggregatorStrategy {

    float[] getValues(AggregatedNumber aggregatedNumber);

    int getValueCount();

    String[] getSuffixes();

}
