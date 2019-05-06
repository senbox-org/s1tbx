package org.esa.snap.core.gpf.common.resample;

import org.esa.snap.core.datamodel.RasterDataNode;

import javax.media.jai.RasterAccessor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Tonio Fincke
 */
public abstract class LongDataAggregator implements Aggregator {

    private LongDataAccessor accessor;
    protected RasterDataNode rasterDataNode;

    public void init(RasterDataNode rasterDataNode, RasterAccessor srcAccessor, RasterAccessor dstAccessor, double noDataValue) {
        this.accessor = DataAccessorFactory.createLongDataAccessor(srcAccessor, dstAccessor, noDataValue);
        this.rasterDataNode = rasterDataNode;
    }

    protected long getSrcData(int index) {
        return accessor.getSrcData(index);
    }

    protected void setDstData(int index, long value) {
        accessor.setDstData(index, value);
    }

    protected long getNoDataValue() {
        return accessor.getNoDataValue();
    }

    protected int getSrcOffset() { return accessor.getSrcOffset(); }

    public void dispose() {

    };

    static class Mean extends LongDataAggregator {

        @Override
        public void aggregate(int srcY0, int srcY1, int srcX0, int srcX1, int srcScanlineStride, double wx0, double wx1, double wy0, double wy1, int dstPos) {
            long vSum = 0;
            double wSum = 0.0;
            int srcIndexY = getSrcOffset() + srcY0 * srcScanlineStride;
            for (int srcY = srcY0; srcY <= srcY1; srcY++) {
                double wy = srcY == srcY0 ? wy0 : srcY == srcY1 ? wy1 : 1;
                for (int srcX = srcX0; srcX <= srcX1; srcX++) {
                    double wx = srcX == srcX0 ? wx0 : srcX == srcX1 ? wx1 : 1;
                    long v = getSrcData(srcIndexY + srcX);
                    if (v != getNoDataValue()) {
                        double w = wx * wy;
                        vSum += w * v;
                        wSum += w;
                    }
                }
                srcIndexY += srcScanlineStride;
            }
            if (wSum == 0.0) {
                setDstData(dstPos, getNoDataValue());
            } else {
                setDstData(dstPos, (long) (vSum / wSum));
            }
        }

    }

    static class Median extends LongDataAggregator {

        @Override
        public void aggregate(int srcY0, int srcY1, int srcX0, int srcX1, int srcScanlineStride, double wx0, double wx1, double wy0, double wy1, int dstPos) {
            List<Long> validValues = new ArrayList<>();
            int srcIndexY = getSrcOffset() + srcY0 * srcScanlineStride;
            for (int srcY = srcY0; srcY <= srcY1; srcY++) {
                for (int srcX = srcX0; srcX <= srcX1; srcX++) {
                    long v = getSrcData(srcIndexY + srcX);
                    if (v != getNoDataValue()) {
                        validValues.add(v);
                    }
                }
                srcIndexY += srcScanlineStride;
            }
            final int numValidValues = validValues.size();
            if (numValidValues == 0) {
                setDstData(dstPos, getNoDataValue());
            } else {
                Collections.sort(validValues);
                if (numValidValues % 2 == 1) {
                    setDstData(dstPos, validValues.get(numValidValues / 2));
                } else {
                    long median = (validValues.get(numValidValues / 2 - 1) + validValues.get(numValidValues / 2)) / 2;
                    setDstData(dstPos, median);
                }
            }
        }

    }

    static class Min extends LongDataAggregator {

        @Override
        public void aggregate(int srcY0, int srcY1, int srcX0, int srcX1, int srcScanlineStride, double wx0, double wx1, double wy0, double wy1, int dstPos) {
            long minValue = Long.MAX_VALUE;
            int n = 0;
            int srcIndexY = getSrcOffset() + srcY0 * srcScanlineStride;
            for (int srcY = srcY0; srcY <= srcY1; srcY++) {
                for (int srcX = srcX0; srcX <= srcX1; srcX++) {
                    long v = getSrcData(srcIndexY + srcX);
                    if (v != getNoDataValue() && v < minValue) {
                        minValue = v;
                        n++;
                    }
                }
                srcIndexY += srcScanlineStride;
            }
            if (n > 0) {
                setDstData(dstPos, minValue);
            } else {
                setDstData(dstPos, getNoDataValue());
            }
        }

    }

    static class Max extends LongDataAggregator {

        @Override
        public void aggregate(int srcY0, int srcY1, int srcX0, int srcX1, int srcScanlineStride, double wx0, double wx1, double wy0, double wy1, int dstPos) {
            long maxValue = Long.MIN_VALUE;
            int n = 0;
            int srcIndexY = getSrcOffset() + srcY0 * srcScanlineStride;
            for (int srcY = srcY0; srcY <= srcY1; srcY++) {
                for (int srcX = srcX0; srcX <= srcX1; srcX++) {
                    long v = getSrcData(srcIndexY + srcX);
                    if (v != getNoDataValue() && v > maxValue) {
                        maxValue = v;
                        n++;
                    }
                }
                srcIndexY += srcScanlineStride;
            }
            if (n > 0) {
                setDstData(dstPos, maxValue);
            } else {
                setDstData(dstPos, getNoDataValue());
            }
        }

    }

    static class First extends LongDataAggregator {

        @Override
        public void aggregate(int srcY0, int srcY1, int srcX0, int srcX1, int srcScanlineStride, double wx0, double wx1, double wy0, double wy1, int dstPos) {
            setDstData(dstPos, getSrcData(getSrcOffset() + srcY0 * srcScanlineStride + srcX0));
        }
    }

    static class FlagAnd extends LongDataAggregator {

        @Override
        public void aggregate(int srcY0, int srcY1, int srcX0, int srcX1, int srcScanlineStride, double wx0, double wx1, double wy0, double wy1, int dstPos) {
            long res = Long.MAX_VALUE;
            int n = 0;
            int srcIndexY = getSrcOffset() + srcY0 * srcScanlineStride;
            for (int srcY = srcY0; srcY <= srcY1; srcY++) {
                for (int srcX = srcX0; srcX <= srcX1; srcX++) {
                    long v = getSrcData(srcIndexY + srcX);
                    if (v != getNoDataValue()) {
                        res = res & v;
                        n++;
                    }
                }
                srcIndexY += srcScanlineStride;
            }
            res = n > 0 ? res : 0;
            setDstData(dstPos, res);
        }
    }

    static class FlagOr extends LongDataAggregator {

        @Override
        public void aggregate(int srcY0, int srcY1, int srcX0, int srcX1, int srcScanlineStride, double wx0, double wx1, double wy0, double wy1, int dstPos) {
            long res = 0;
            int srcIndexY = getSrcOffset() + srcY0 * srcScanlineStride;
            for (int srcY = srcY0; srcY <= srcY1; srcY++) {
                for (int srcX = srcX0; srcX <= srcX1; srcX++) {
                    long v = getSrcData(srcIndexY + srcX);
                    if (v != getNoDataValue()) {
                        res = res | v;
                    }
                }
                srcIndexY += srcScanlineStride;
            }
            setDstData(dstPos, res);
        }
    }

    static class FlagMedianAnd extends LongDataAggregator {

        @Override
        public void aggregate(int srcY0, int srcY1, int srcX0, int srcX1, int srcScanlineStride, double wx0, double wx1, double wy0, double wy1, int dstPos) {
            int n = 0;
            final int[] occurenceCounter = new int[63];
            int highestOccurence = 0;
            int srcIndexY = getSrcOffset() + srcY0 * srcScanlineStride;
            for (int srcY = srcY0; srcY <= srcY1; srcY++) {
                for (int srcX = srcX0; srcX <= srcX1; srcX++) {
                    long v = getSrcData(srcIndexY + srcX);
                    if (v != getNoDataValue()) {
                        int j = 0;
                        while (v > 0) {
                            long compare = v & 1;
                            if (compare != 0) {
                                occurenceCounter[j]++;
                            }
                            v >>= 1;
                            j++;
                        }
                        highestOccurence = Math.max(highestOccurence, j - 1);
                        n++;
                    }
                }
                srcIndexY += srcScanlineStride;
            }
            long res = 0;
            final float halfN = n / 2f;
            for (int i = highestOccurence; i >= 0; i--) {
                res <<= 1;
                if (occurenceCounter[i] > halfN) {
                    res++;
                }
            }
           setDstData(dstPos, res);
        }
    }

    static class FlagMedianOr extends LongDataAggregator {

        @Override
        public void aggregate(int srcY0, int srcY1, int srcX0, int srcX1, int srcScanlineStride, double wx0, double wx1, double wy0, double wy1, int dstPos) {
            int n = 0;
            final int[] occurenceCounter = new int[63];
            int highestOccurence = 0;
            int srcIndexY = getSrcOffset() + srcY0 * srcScanlineStride;
            for (int srcY = srcY0; srcY <= srcY1; srcY++) {
                for (int srcX = srcX0; srcX <= srcX1; srcX++) {
                    long v = getSrcData(srcIndexY + srcX);
                    if (v != getNoDataValue()) {
                        int j = 0;
                        while (v > 0) {
                            long compare = v & 1;
                            if (compare != 0) {
                                occurenceCounter[j]++;
                            }
                            v >>= 1;
                            j++;
                        }
                        highestOccurence = Math.max(highestOccurence, j - 1);
                        n++;
                    }
                }
                srcIndexY += srcScanlineStride;
            }
            long res = 0;
            final float halfN = n / 2f;
            for (int i = highestOccurence; i >= 0; i--) {
                res <<= 1;
                if (occurenceCounter[i] >= halfN) {
                    res++;
                }
            }
            setDstData(dstPos, res);
        }
    }

}
