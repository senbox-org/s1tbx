package org.esa.snap.core.gpf.common.resample;

import javax.media.jai.RasterAccessor;

/**
 * @author Tonio Fincke
 */
abstract class DoubleDataAccessor implements DataAccessor {

    abstract double getSrcData(int index);

    abstract void setDstData(int index, double value);

    abstract double getNoDataValue();

    static class Double extends DoubleDataAccessor {

        private final double noDataValue;
        private final double[] dstArray;
        private final double[] srcArray;

        Double(RasterAccessor srcAccessor, RasterAccessor dstAccessor, double noDataValue) {
            srcArray = srcAccessor.getDoubleDataArray(0);
            dstArray = dstAccessor.getDoubleDataArray(0);
            this.noDataValue = noDataValue;
        }

        @Override
        double getSrcData(int index) {
            return srcArray[index];
        }

        @Override
        void setDstData(int index, double value) {
            dstArray[index] = value;
        }

        @Override
        double getNoDataValue() {
            return noDataValue;
        }
    }

    static class Float extends DoubleDataAccessor {

        private final double noDataValue;
        private final float[] dstArray;
        private final float[] srcArray;

        Float(RasterAccessor srcAccessor, RasterAccessor dstAccessor, double noDataValue) {
            srcArray = srcAccessor.getFloatDataArray(0);
            dstArray = dstAccessor.getFloatDataArray(0);
            this.noDataValue = noDataValue;
        }

        @Override
        double getSrcData(int index) {
            return srcArray[index];
        }

        @Override
        void setDstData(int index, double value) {
            dstArray[index] = (float) value;
        }

        @Override
        double getNoDataValue() {
            return noDataValue;
        }
    }

}
