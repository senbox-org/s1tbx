package org.esa.snap.core.gpf.common.resample;

import org.esa.snap.core.datamodel.RasterDataNode;

import javax.media.jai.RasterAccessor;
import java.awt.Rectangle;

/**
 * @author Tonio Fincke
 */
public abstract class LongDataInterpolator implements Interpolator {

    private LongDataAccessor accessor;
    protected RasterDataNode rasterDataNode;

    public void init(RasterDataNode rasterDataNode, RasterAccessor srcAccessor, RasterAccessor dstAccessor, double noDataValue) {
        this.rasterDataNode = rasterDataNode;
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

    public void dispose(){

    };

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
//                double srcYF = subPixelOffsetY + (scaleY * (dstY));
                int srcY = (int) srcYF;
                double wy = srcYF - srcY;
                int srcIndexY = getSrcOffset() + srcY * getSrcScalineStride();
                for (int dstX = 0; dstX < destRect.getWidth(); dstX++) {
                    double srcXF = subPixelOffsetX + (scaleX * (dstX + 0.5)) - 0.5;
//                    double srcXF = subPixelOffsetX + (scaleX * (dstX));
                    int srcX = (int) srcXF;
                    double wx = srcXF - srcX;
                    boolean withinSrcH = srcY + 1 < srcH;
                    boolean withinSrcW = srcX + 1 < srcW;
                    double v00 = getSrcData(srcIndexY + srcX);
                    final int dstIndex = dstIndexY + dstX;
                    if (!withinSrcW && !withinSrcH) {
                        setDstData(dstIndex, (long) v00);
                        continue;
                    }
                    double v01 = withinSrcW ? getSrcData(srcIndexY + srcX + 1) : getNoDataValue();
                    double v10 = withinSrcH ? getSrcData((srcIndexY + getSrcScalineStride()) + srcX) : getNoDataValue();
                    double v11 = withinSrcW && withinSrcH ? getSrcData((srcIndexY + getSrcScalineStride()) + srcX + 1) : getNoDataValue();
                    final boolean v00Valid = isValid(v00);
                    final boolean v01Valid = isValid(v01);
                    final boolean v10Valid = isValid(v10);
                    final boolean v11Valid = isValid(v11);
                    int validityCounter = 0;
                    validityCounter = v00Valid ? validityCounter + 1 : validityCounter;
                    validityCounter = v01Valid ? validityCounter + 1 : validityCounter;
                    validityCounter = v10Valid ? validityCounter + 1 : validityCounter;
                    validityCounter = v11Valid ? validityCounter + 1 : validityCounter;
                    if (validityCounter == 0) {
                        setDstData(dstIndex, getNoDataValue());
                    } else if (validityCounter == 4) {
                        double v0 = v00 + wx * (v01 - v00);
                        double v1 = v10 + wx * (v11 - v10);
                        double v = v0 + wy * (v1 - v0);
                        setDstData(dstIndex, (long) v);
                    } else if (validityCounter == 1) {
                        if (v00Valid) {
                            setDstData(dstIndex, (long) v00);
                        } else if (v01Valid) {
                            setDstData(dstIndex, (long) v01);
                        } else if (v10Valid) {
                            setDstData(dstIndex, (long) v10);
                        } else {
                            setDstData(dstIndex, (long) v11);
                        }
                    } else if (validityCounter == 2) {
                        if (v00Valid && v01Valid) {
                            setDstData(dstIndex, (long) ((v00 * (1 - wx)) + (v01 * wx)));
                        } else if (v10Valid && v11Valid) {
                            setDstData(dstIndex, (long) ((v10 * (1 - wx)) + (v11 * wx)));
                        } else if (v00Valid && v10Valid) {
                            setDstData(dstIndex, (long) ((v00 * (1 - wy)) + (v10 * wy)));
                        } else if (v01Valid && v11Valid) {
                            setDstData(dstIndex, (long) ((v01 * (1 - wy)) + (v11 * wy)));
                        } else if (v00Valid && v11Valid) {
                            double ws = 1 / ((wx * wy) + ((1 - wx) * (1 - wy)));
                            double v = ((v00 * (1 - wx)) * (1 - wy)) + (v11 * wx * wy);
                            v *= ws;
                            setDstData(dstIndex, (long) v);
                        } else {
                            double ws = 1 / (((1 - wx) * wy) + (wx * (1 - wy)));
                            double v = (v01 * wx * (1 - wy)) + (v10 * (1 - wx) * wy);
                            v *= ws;
                            setDstData(dstIndex, (long) v);
                        }
                    } else { //validityCounter == 3
                        double w00 = (1 - wx) * (1 - wy);
                        double w01 = wx * (1 - wy);
                        double w10 = (1 - wx) * wy;
                        double w11 = wx * wy;
                        if (!v00Valid) {
                            w01 += (0.5 * w00);
                            w10 += (0.5 * w00);
                            double v = v01 * w01 + v10 * w10 + v11 * w11;
                            setDstData(dstIndex, (long) v);
                        } else if (!v01Valid) {
                            w00 += (0.5 * w01);
                            w11 += (0.5 * w01);
                            double v = v00 * w00 + v10 * w10 + v11 * w11;
                            setDstData(dstIndex, (long) v);
                        } else if (!v10Valid) {
                            w00 += (0.5 * w10);
                            w11 += (0.5 * w10);
                            double v = v00 * w00 + v01 * w01 + v11 * w11;
                            setDstData(dstIndex, (long) v);
                        } else {
                            w01 += (0.5 * w11);
                            w10 += (0.5 * w11);
                            double v = v00 * w00 + v01 * w01 + v10 * w10;
                            setDstData(dstIndex, (long) v);
                        }
                    }
                }
                dstIndexY += getDstScalineStride();
            }
        }

        private boolean isValid(double v) {
            return !Double.isNaN(v) && Math.abs(v - getNoDataValue()) > 1e-8;
        }

    }

}
