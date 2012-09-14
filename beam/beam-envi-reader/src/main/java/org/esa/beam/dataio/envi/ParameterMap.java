package org.esa.beam.dataio.envi;

class ParameterMap {

    ParameterMap(int targetSize, int[] indices) {
        this.targetSize = targetSize;
        this.indices = indices;
    }

    double[] transform(double[] sourceValues) {
        final double[] result = new double[targetSize];
        for (int i = 0; i < sourceValues.length; i++) {
            result[indices[i]] = sourceValues[i];

        }
        return result;
    }

    ///////////////////////////////////////////////////////////////////////////
    /////// END OF PUBLIC
    ///////////////////////////////////////////////////////////////////////////

    private int targetSize;
    private int[] indices;
}
