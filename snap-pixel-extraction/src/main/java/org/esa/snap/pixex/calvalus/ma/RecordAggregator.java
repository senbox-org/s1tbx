package org.esa.snap.pixex.calvalus.ma;

import java.lang.reflect.Array;

/**
 * Converts even int[] into floating point values, not at least to symbolize NaN
 * for values that have been masked out.
 */
// todo Copied from Calvalus: Move to BEAM core!?

public class RecordAggregator implements RecordTransformer {

    private final int maskAttributeIndex;
    private final double filteredMeanCoeff;

    public RecordAggregator(int maskAttributeIndex, double filteredMeanCoeff) {
        this.maskAttributeIndex = maskAttributeIndex;
        this.filteredMeanCoeff = filteredMeanCoeff;
    }

    @Override
    public Record transform(Record record) {
        final Object[] attributeValues = record.getAttributeValues();
        int length = getCommonArrayValueLength(attributeValues);
        if (length == -1) {
            // If there are not primitive int[] or float[] values in attributeValues, we are done
            return record;
        }
        final int[] maskValues = getMaskValues(attributeValues);
        final Object[] aggregatedValues = new Object[attributeValues.length];
        for (int valueIndex = 0; valueIndex < aggregatedValues.length; valueIndex++) {
            Object attributeValue = attributeValues[valueIndex];
            if (attributeValue != null && attributeValue == maskValues) {
                attributeValue = aggregate(maskValues, null);
            } else if (attributeValue instanceof float[]) {
                attributeValue = aggregate((float[]) attributeValue, maskValues);
            } else if (attributeValue instanceof int[]) {
                attributeValue = aggregate((int[]) attributeValue, maskValues);
            }
            aggregatedValues[valueIndex] = attributeValue;
        }
        return new DefaultRecord(record.getLocation(), record.getTime(), aggregatedValues);
    }

    private int[] getMaskValues(Object[] attributeValues) {
        if (maskAttributeIndex != -1) {
            return (int[]) attributeValues[maskAttributeIndex];
        } else {
            return null;
        }
    }

    private Number aggregate(int[] values, int[] maskValues) {
        if (values.length == 1) {
            // Case: macroPixelSize = 1
            return isGoodPixel(maskValues, 0) ? values[0] : null;
        }
        float[] floats = new float[values.length];
        for (int i = 0; i < floats.length; i++) {
            floats[i] = values[i];
        }
        return aggregate(floats, maskValues);
    }

    private Number aggregate(float[] values, int[] maskValues) {
        if (values.length == 1) {
            // Case: macroPixelSize = 1
            return isGoodPixel(maskValues, 0) ? values[0] : Float.NaN;
        }

        // Step 1: Compute min, max, mean

        double sum = 0;
        double min = +Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;

        int numGoodPixels = 0;
        int numTotalPixels = 0;

        for (int i = 0; i < values.length; i++) {
            final float value = values[i];
            if (!Float.isNaN(value)) {
                numTotalPixels++;
                if (isGoodPixel(maskValues, i)) {
                    numGoodPixels++;
                    sum += value;
                    min = Math.min(min, value);
                    max = Math.max(max, value);
                }
            }
        }
        final double mean = numGoodPixels > 0 ? sum / numGoodPixels : Double.NaN;

        // Step 2: Compute sigma

        double sumSigma = 0;
        for (int i = 0; i < values.length; i++) {
            final float value = values[i];
            if (!Float.isNaN(value) && isGoodPixel(maskValues, i)) {
                sumSigma += (mean - value) * (mean - value);
            }
        }
        final double sigma = numGoodPixels > 1 ? Math.sqrt(sumSigma / (numGoodPixels - 1)) : 0.0;

        // If we don't want to filter or we can't, then we are done.
        if (filteredMeanCoeff <= 0.0 || Math.abs(sigma) < 1E-10) {
            return new AggregatedNumber(numGoodPixels, numTotalPixels, 0, min, max, mean, sigma, values);
        }

        // Step 3: Compute filteredMin, filteredMax, filteredMean

        final double lowerBound = mean - filteredMeanCoeff * sigma;
        final double upperBound = mean + filteredMeanCoeff * sigma;

        numGoodPixels = 0;
        int numFilteredPixels = 0;

        double filteredSum = 0;
        double filteredMin = +Double.MAX_VALUE;
        double filteredMax = -Double.MAX_VALUE;

        for (int i = 0; i < values.length; i++) {
            final float value = values[i];
            if (!Float.isNaN(value) && isGoodPixel(maskValues, i)) {
                if (value >= lowerBound && value <= upperBound) {
                    filteredSum += value;
                    filteredMin = Math.min(filteredMin, value);
                    filteredMax = Math.max(filteredMax, value);
                    numGoodPixels++;
                } else {
                    numFilteredPixels++;
                }
            }
        }
        final double filteredMean = numGoodPixels > 0 ? filteredSum / numGoodPixels : Double.NaN;

        // Step 4: Compute filteredSigma
        double filteredSumSigma = 0;
        for (int i = 0; i < values.length; i++) {
            final float value = values[i];
            if (!Float.isNaN(value) && isGoodPixel(maskValues, i)) {
                if (value > lowerBound && value < upperBound) {
                    filteredSumSigma += (filteredMean - value) * (filteredMean - value);
                }
            }
        }
        final double filteredSigma = numGoodPixels > 1 ? Math.sqrt(filteredSumSigma / (numGoodPixels - 1)) : 0.0;

        // Done!
        return new AggregatedNumber(numGoodPixels, numTotalPixels, numFilteredPixels,
                                    filteredMin, filteredMax, filteredMean, filteredSigma, values);
    }

    private static boolean isGoodPixel(int[] maskValues, int i) {
        return maskValues == null || maskValues[i] != 0;
    }

    private static int getCommonArrayValueLength(Object[] attributeValues) {
        int commonLength = -1;
        for (Object attributeValue : attributeValues) {
            if (attributeValue != null && attributeValue.getClass().isArray()) {
                int length = Array.getLength(attributeValue);
                if (commonLength >= 0 && length != commonLength) {
                    throw new IllegalArgumentException(
                            "Record with varying array lengths detected. Expected " + commonLength + ", found " + length);
                }
                commonLength = length;
                if (!(attributeValue instanceof int[] || attributeValue instanceof float[])) {
                    throw new IllegalArgumentException(
                            "Records with array values can only be of type int[] or float[].");
                }
            }
        }
        if (commonLength == 0) {
            throw new IllegalArgumentException("Record with zero-length arrays.");
        }
        return commonLength;
    }

}
