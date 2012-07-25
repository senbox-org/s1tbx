package org.esa.beam.pixex.aggregators;


import org.esa.beam.framework.datamodel.ProductData;

public class MeanAggregator implements Aggregator {

    @Override
    public Number[] aggregateMeasuresForBands(Number[][] allValues, int numPixels, int numBands, int[] dataTypes) {
        Number[] meanMeasurementValues = new Number[numBands];

        for (int bandIndex = 0; bandIndex < numBands; bandIndex++) {
            final boolean isFloatingPointType = ProductData.isFloatingPointType(dataTypes[bandIndex]);
            if (isFloatingPointType) {
                meanMeasurementValues[bandIndex] = 0.0;
            } else {
                meanMeasurementValues[bandIndex] = 0;
            }
            for (int pixelIndex = 0; pixelIndex < allValues.length; pixelIndex++) {
                //don't replace if-expression with ?-operator!
                if (isFloatingPointType) {
                    meanMeasurementValues[bandIndex] = meanMeasurementValues[bandIndex].floatValue() +
                                                       allValues[pixelIndex][bandIndex].floatValue();
                } else {
                    meanMeasurementValues[bandIndex] = meanMeasurementValues[bandIndex].intValue() +
                                                       allValues[pixelIndex][bandIndex].intValue();
                }
            }
            //don't replace if-expression with ?-operator!
            if (isFloatingPointType) {
                meanMeasurementValues[bandIndex] = meanMeasurementValues[bandIndex].floatValue() / numPixels;
            } else {
                meanMeasurementValues[bandIndex] = meanMeasurementValues[bandIndex].intValue() / numPixels;
            }
        }
        return meanMeasurementValues;
    }

}
