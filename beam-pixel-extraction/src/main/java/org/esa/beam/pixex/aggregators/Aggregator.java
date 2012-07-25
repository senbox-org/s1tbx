package org.esa.beam.pixex.aggregators;

public interface Aggregator {

    public Number[] aggregateMeasuresForBands(Number[][] allValues, int numPixels, int numBands, int[] datatypes);

}
