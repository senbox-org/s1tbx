package org.esa.beam.pixex.output;

public interface MeasurementAggregator {

    public Number[] aggregateMeasuresForBands(Number[][] allValues, int numPixels, int numBands, int[] datatypes);

}
