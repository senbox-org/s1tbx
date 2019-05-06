package org.esa.snap.core.gpf.common.resample;

import org.esa.snap.core.datamodel.RasterDataNode;

import javax.media.jai.RasterAccessor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Tonio Fincke
 */
public abstract class DoubleDataAggregator implements Aggregator {

    private DoubleDataAccessor accessor;
    boolean noDataIsNaN;
    protected RasterDataNode rasterDataNode;

    public void init(RasterDataNode rasterDataNode, RasterAccessor srcAccessor, RasterAccessor dstAccessor, double noDataValue) {
        this.accessor = DataAccessorFactory.createDoubleDataAccessor(srcAccessor, dstAccessor, noDataValue);
        this.noDataIsNaN = Double.isNaN(noDataValue);
        this.rasterDataNode = rasterDataNode;
    }

    protected double getSrcData(int index) {
        return accessor.getSrcData(index);
    }

    protected void setDstData(int index, double value) {
        accessor.setDstData(index, value);
    }

    protected double getNoDataValue() {
        return accessor.getNoDataValue();
    }

    protected int getSrcOffset() {
        return accessor.getSrcOffset();
    }

    public void dispose() {

    };

    static class Mean extends DoubleDataAggregator {

        @Override
        public void aggregate(int srcY0, int srcY1, int srcX0, int srcX1, int srcScanlineStride, double wx0,
                              double wx1, double wy0, double wy1, int dstPos) {
            double vSum = 0.0;
            double wSum = 0.0;
            int srcIndexY = getSrcOffset() + srcY0 * srcScanlineStride;
            for (int srcY = srcY0; srcY <= srcY1; srcY++) {
                double wy = srcY == srcY0 ? wy0 : srcY == srcY1 ? wy1 : 1;
                for (int srcX = srcX0; srcX <= srcX1; srcX++) {
                    double wx = srcX == srcX0 ? wx0 : srcX == srcX1 ? wx1 : 1;
                    double v = getSrcData(srcIndexY + srcX);
                    if (!Double.isNaN(v) && (noDataIsNaN || Math.abs(v - getNoDataValue()) > 1e-8)) {
                        double w = wx * wy;
                        vSum += w * v;
                        wSum += w;
                    }
                }
                srcIndexY += srcScanlineStride;
            }
            if (Double.isNaN(vSum) || wSum == 0.0) {
                setDstData(dstPos, getNoDataValue());
            } else {
                setDstData(dstPos, vSum / wSum);
            }
        }

    }

    static class Median extends DoubleDataAggregator {

        @Override
        public void aggregate(int srcY0, int srcY1, int srcX0, int srcX1, int srcScanlineStride, double wx0, double wx1, double wy0, double wy1, int dstPos) {
            List<Double> validValues = new ArrayList<>();
            int srcIndexY = getSrcOffset() + srcY0 * srcScanlineStride;
            for (int srcY = srcY0; srcY <= srcY1; srcY++) {
                for (int srcX = srcX0; srcX <= srcX1; srcX++) {
                    double v = getSrcData(srcIndexY + srcX);
                    if (!Double.isNaN(v) && (noDataIsNaN || Math.abs(v - getNoDataValue()) > 1e-8)) {
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
                    double median = (validValues.get(numValidValues / 2 - 1) + validValues.get(numValidValues / 2)) / 2;
                    setDstData(dstPos, median);
                }
            }
        }
    }

    static class Min extends DoubleDataAggregator {

        @Override
        public void aggregate(int srcY0, int srcY1, int srcX0, int srcX1, int srcScanlineStride, double wx0, double wx1, double wy0, double wy1, int dstPos) {
            double minValue = Double.POSITIVE_INFINITY;
            int srcIndexY = getSrcOffset() + srcY0 * srcScanlineStride;
            for (int srcY = srcY0; srcY <= srcY1; srcY++) {
                for (int srcX = srcX0; srcX <= srcX1; srcX++) {
                    double v = getSrcData(srcIndexY + srcX);
                    if (!Double.isNaN(v) &&
                            (noDataIsNaN || (Math.abs(v - getNoDataValue()) > 1e-8)) && v < minValue) {
                        minValue = v;
                    }
                }
                srcIndexY += srcScanlineStride;
            }
            if (Double.isInfinite(minValue)) {
                minValue = getNoDataValue();
            }
            setDstData(dstPos, minValue);
        }
    }

    static class Max extends DoubleDataAggregator {

        @Override
        public void aggregate(int srcY0, int srcY1, int srcX0, int srcX1, int srcScanlineStride, double wx0, double wx1, double wy0, double wy1, int dstPos) {
            double maxValue = Double.NEGATIVE_INFINITY;
            int srcIndexY = getSrcOffset() + srcY0 * srcScanlineStride;
            for (int srcY = srcY0; srcY <= srcY1; srcY++) {
                for (int srcX = srcX0; srcX <= srcX1; srcX++) {
                    double v = getSrcData(srcIndexY + srcX);
                    if (!Double.isNaN(v) &&
                            (noDataIsNaN || (Math.abs(v - getNoDataValue()) > 1e-8)) && v > maxValue) {
                        maxValue = v;
                    }
                }
                srcIndexY += srcScanlineStride;
            }
            if (Double.isInfinite(maxValue)) {
                maxValue = getNoDataValue();
            }
            setDstData(dstPos, maxValue);
        }
    }

    static class First extends DoubleDataAggregator {

        @Override
        public void aggregate(int srcY0, int srcY1, int srcX0, int srcX1, int srcScanlineStride, double wx0, double wx1, double wy0, double wy1, int dstPos) {
            setDstData(dstPos, getSrcData(getSrcOffset() + srcY0 * srcScanlineStride + srcX0));
        }
    }
}
