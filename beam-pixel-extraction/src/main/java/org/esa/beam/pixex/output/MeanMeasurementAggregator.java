package org.esa.beam.pixex.output;


import org.esa.beam.framework.datamodel.ProductData;

public class MeanMeasurementAggregator implements MeasurementAggregator {

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
                meanMeasurementValues[bandIndex] = isFloatingPointType ? meanMeasurementValues[bandIndex].floatValue() +
                                                                         allValues[pixelIndex][bandIndex].floatValue()
                                                                       : meanMeasurementValues[bandIndex].intValue() +
                                                                         allValues[pixelIndex][bandIndex].intValue();
            }
            meanMeasurementValues[bandIndex] = isFloatingPointType ? meanMeasurementValues[bandIndex].floatValue() / numPixels
                                                                   : meanMeasurementValues[bandIndex].intValue() / numPixels;
        }
        return meanMeasurementValues;
    }

}
