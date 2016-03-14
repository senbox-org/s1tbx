package org.esa.snap.core.gpf.common.resample;

import javax.media.jai.RasterAccessor;
import java.awt.Rectangle;

/**
 * @author Tonio Fincke
 */
public abstract class LongDataInterpolator implements Interpolator {

    private LongDataAccessor accessor;

    public void init(RasterAccessor srcAccessor, RasterAccessor dstAccessor, double noDataValue) {
        this.accessor = DataAccessorFactory.createLongDataAccessor(srcAccessor, dstAccessor, noDataValue);
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

    int getSrcScalineStride(){
        return accessor.getSrcScalineStride();
    }

    int getDstScalineStride() {
        return accessor.getDstScalineStride();
    }

    int getSrcOffset() { return accessor.getSrcOffset(); }

    int getDstOffset() { return accessor.getDstOffset(); }

    static class NearestNeighbour extends LongDataInterpolator {

        @Override
        public void interpolate(Rectangle destRect, Rectangle srcRect, double scaleX, double scaleY,
                                double offsetX, double offsetY) {
            int dstIndexY = getDstOffset();
            double subPixelOffsetY = offsetY - (int) offsetY;
            double subPixelOffsetX = offsetX - (int) offsetX;
            for (int dstY = 0; dstY < destRect.getHeight(); dstY++) {
                int srcY = (int) (subPixelOffsetY + scaleY * dstY);
                int srcIndexY = getSrcOffset() + srcY * getSrcScalineStride();
                boolean yValid = srcY < srcRect.getHeight();
                for (int dstX = 0; dstX < destRect.getWidth(); dstX++) {
                    int srcX = (int) (subPixelOffsetX + scaleX * dstX);
                    if (yValid && srcX < srcRect.getWidth()) {
                        setDstData(dstIndexY + dstX, getSrcData(srcIndexY + srcX));
                    } else {
                        setDstData(dstIndexY + dstX, getNoDataValue());
                    }
                }
                dstIndexY += getDstScalineStride();
            }
        }
    }

    static class Bilinear extends LongDataInterpolator {

        @Override
        public void interpolate(Rectangle destRect, Rectangle srcRect, double scaleX, double scaleY, double offsetX, double offsetY) {
            final int srcH = (int) srcRect.getHeight();
            final int srcW = (int) srcRect.getWidth();
            int dstIndexY = getDstOffset();
            double subPixelOffsetY = offsetY - (int) offsetY;
            double subPixelOffsetX = offsetX - (int) offsetX;
            for (int dstY = 0; dstY < destRect.getHeight(); dstY++) {
                double srcYF = subPixelOffsetY + (scaleY * (dstY + 0.5)) - 0.5;
                int srcY = (int) srcYF;
                double wy = srcYF - srcY;
                int srcIndexY = getSrcOffset() + srcY * getSrcScalineStride();
                for (int dstX = 0; dstX < destRect.getWidth(); dstX++) {
                    double srcXF = subPixelOffsetX + (scaleX * (dstX + 0.5)) - 0.5;
                    int srcX = (int) srcXF;
                    double wx = srcXF - srcX;
                    boolean withinSrcH = srcY + 1 < srcH;
                    boolean withinSrcW = srcX + 1 < srcW;
                    double v00 = getSrcData(srcIndexY + srcX);
                    double v01 = withinSrcW ? getSrcData(srcIndexY + srcX + 1) : v00;
                    double v10 = withinSrcH ? getSrcData((srcIndexY + getSrcScalineStride()) + srcX) : v00;
                    double v11 = withinSrcW && withinSrcH ? getSrcData((srcIndexY + getSrcScalineStride()) + srcX + 1) : v00;
                    double v0 = v00 + wx * (v01 - v00);
                    double v1 = v10 + wx * (v11 - v10);
                    long v = (long) (v0 + wy * (v1 - v0));
                    if (Double.isNaN(v) || Math.abs(v - getNoDataValue()) < 1e-8) {
                        setDstData(dstIndexY + dstX, getNoDataValue());
                    } else {
                        setDstData(dstIndexY + dstX, v);
                    }
                }
                dstIndexY += getDstScalineStride();
            }
        }
    }

}
