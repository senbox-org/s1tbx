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
        private final int srcScanlineStride;
        private final int dstScanlineStride;
        private final int srcOffset;
        private final int dstOffset;

        Double(RasterAccessor srcAccessor, RasterAccessor dstAccessor, double noDataValue) {
            srcArray = srcAccessor.getDoubleDataArray(0);
            dstArray = dstAccessor.getDoubleDataArray(0);
            srcScanlineStride = srcAccessor.getScanlineStride();
            dstScanlineStride = dstAccessor.getScanlineStride();
            srcOffset = srcAccessor.getBandOffset(0);
            dstOffset = dstAccessor.getBandOffset(0);
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

        @Override
        public int getSrcScalineStride() {
            return srcScanlineStride;
        }

        @Override
        public int getDstScalineStride() {
            return dstScanlineStride;
        }

        @Override
        public int getSrcOffset() {
            return srcOffset;
        }

        @Override
        public int getDstOffset() {
            return dstOffset;
        }
    }

    static class Float extends DoubleDataAccessor {

        private final double noDataValue;
        private final float[] dstArray;
        private final float[] srcArray;
        private final int srcScanlineStride;
        private final int dstScanlineStride;
        private final int srcOffset;
        private final int dstOffset;

        Float(RasterAccessor srcAccessor, RasterAccessor dstAccessor, double noDataValue) {
            srcArray = srcAccessor.getFloatDataArray(0);
            dstArray = dstAccessor.getFloatDataArray(0);
            srcScanlineStride = srcAccessor.getScanlineStride();
            dstScanlineStride = dstAccessor.getScanlineStride();
            srcOffset = srcAccessor.getBandOffset(0);
            dstOffset = dstAccessor.getBandOffset(0);
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

        @Override
        public int getSrcScalineStride() {
            return srcScanlineStride;
        }

        @Override
        public int getDstScalineStride() {
            return dstScanlineStride;
        }

        @Override
        public int getSrcOffset() {
            return srcOffset;
        }

        @Override
        public int getDstOffset() {
            return dstOffset;
        }
    }

}
