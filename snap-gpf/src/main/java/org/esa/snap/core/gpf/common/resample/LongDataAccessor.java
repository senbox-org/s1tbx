package org.esa.snap.core.gpf.common.resample;

import javax.media.jai.RasterAccessor;

/**
 * @author Tonio Fincke
 */
public abstract class LongDataAccessor implements DataAccessor {

    abstract long getSrcData(int index);

    abstract void setDstData(int index, long value);

    abstract long getNoDataValue();

    static class Byte extends LongDataAccessor {

        private final long noDataValue;
        private final byte[] dstArray;
        private final byte[] srcArray;
        private final int srcScanlineStride;
        private final int dstScanlineStride;
        private final int srcOffset;
        private final int dstOffset;

        Byte(RasterAccessor srcAccessor, RasterAccessor dstAccessor, double noDataValue) {
            srcArray = srcAccessor.getByteDataArray(0);
            dstArray = dstAccessor.getByteDataArray(0);
            srcScanlineStride = srcAccessor.getScanlineStride();
            dstScanlineStride = dstAccessor.getScanlineStride();
            srcOffset = srcAccessor.getBandOffset(0);
            dstOffset = dstAccessor.getBandOffset(0);
            this.noDataValue = (long) noDataValue;
        }

        @Override
        long getSrcData(int index) {
            return srcArray[index];
        }

        @Override
        void setDstData(int index, long value) {
            if (value < 0) {
                value = 0;
            } else if (value > 255) {
                value = 255;
            }
            dstArray[index] = (byte) value;
        }

        @Override
        long getNoDataValue() {
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

    static class Short extends LongDataAccessor {

        private final long noDataValue;
        private final short[] dstArray;
        private final short[] srcArray;
        private final int srcScanlineStride;
        private final int dstScanlineStride;
        private final int srcOffset;
        private final int dstOffset;

        Short(RasterAccessor srcAccessor, RasterAccessor dstAccessor, double noDataValue) {
            srcArray = srcAccessor.getShortDataArray(0);
            dstArray = dstAccessor.getShortDataArray(0);
            srcScanlineStride = srcAccessor.getScanlineStride();
            dstScanlineStride = dstAccessor.getScanlineStride();
            srcOffset = srcAccessor.getBandOffset(0);
            dstOffset = dstAccessor.getBandOffset(0);
            this.noDataValue = (long) noDataValue;
        }

        @Override
        long getSrcData(int index) {
            return srcArray[index];
        }

        @Override
        void setDstData(int index, long value) {
            if (value < java.lang.Short.MIN_VALUE) {
                value = java.lang.Short.MIN_VALUE;
            } else if (value > java.lang.Short.MAX_VALUE) {
                value = java.lang.Short.MAX_VALUE;
            }
            dstArray[index] = (short) value;
        }

        @Override
        long getNoDataValue() {
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

    static class UShort extends LongDataAccessor {

        private final long noDataValue;
        private final short[] dstArray;
        private final short[] srcArray;
        private final int srcScanlineStride;
        private final int dstScanlineStride;
        private final int srcOffset;
        private final int dstOffset;

        UShort(RasterAccessor srcAccessor, RasterAccessor dstAccessor, double noDataValue) {
            srcArray = srcAccessor.getShortDataArray(0);
            dstArray = dstAccessor.getShortDataArray(0);
            srcScanlineStride = srcAccessor.getScanlineStride();
            dstScanlineStride = dstAccessor.getScanlineStride();
            srcOffset = srcAccessor.getBandOffset(0);
            dstOffset = dstAccessor.getBandOffset(0);
            this.noDataValue = (long) noDataValue;
        }

        @Override
        long getSrcData(int index) {
            return srcArray[index]  & 0xffff;
        }

        @Override
        void setDstData(int index, long value) {
            if (value < 0) {
                value = 0;
            } else if (value > 0xffff) {
                value = 0xffff;
            }
            dstArray[index] = (short) value;
        }

        @Override
        long getNoDataValue() {
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

    static class Int extends LongDataAccessor {

        private final long noDataValue;
        private final int[] dstArray;
        private final int[] srcArray;
        private final int srcScanlineStride;
        private final int dstScanlineStride;
        private final int srcOffset;
        private final int dstOffset;

        Int(RasterAccessor srcAccessor, RasterAccessor dstAccessor, double noDataValue) {
            srcArray = srcAccessor.getIntDataArray(0);
            dstArray = dstAccessor.getIntDataArray(0);
            srcScanlineStride = srcAccessor.getScanlineStride();
            dstScanlineStride = dstAccessor.getScanlineStride();
            srcOffset = srcAccessor.getBandOffset(0);
            dstOffset = dstAccessor.getBandOffset(0);
            this.noDataValue = (long) noDataValue;
        }

        @Override
        long getSrcData(int index) {
            return srcArray[index];
        }

        @Override
        void setDstData(int index, long value) {
            if (value < java.lang.Integer.MIN_VALUE) {
                value = java.lang.Integer.MIN_VALUE;
            } else if (value > java.lang.Integer.MAX_VALUE) {
                value = java.lang.Integer.MAX_VALUE;
            }
            dstArray[index] = (int) value;
        }

        @Override
        long getNoDataValue() {
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
