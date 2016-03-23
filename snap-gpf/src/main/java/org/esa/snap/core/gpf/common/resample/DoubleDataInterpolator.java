package org.esa.snap.core.gpf.common.resample;

import javax.media.jai.RasterAccessor;
import java.awt.Rectangle;

/**
 * @author Tonio Fincke
 */
public abstract class DoubleDataInterpolator implements Interpolator {

    private DoubleDataAccessor accessor;

    public void init(RasterAccessor srcAccessor, RasterAccessor dstAccessor, double noDataValue) {
        this.accessor = DataAccessorFactory.createDoubleDataAccessor(srcAccessor, dstAccessor, noDataValue);
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

    int getSrcScalineStride() {
        return accessor.getSrcScalineStride();
    }

    int getDstScalineStride() {
        return accessor.getDstScalineStride();
    }

    int getSrcOffset() {
        return accessor.getSrcOffset();
    }

    int getDstOffset() {
        return accessor.getDstOffset();
    }

    static class NearestNeighbour extends DoubleDataInterpolator {

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

    static class Bilinear extends DoubleDataInterpolator {

        @Override
        public void interpolate(Rectangle destRect, Rectangle srcRect, double scaleX, double scaleY,
                                double offsetX, double offsetY) {
            final int srcH = (int) srcRect.getHeight();
            final int srcW = (int) srcRect.getWidth();
            int dstIndexY = getDstOffset();
            double subPixelOffsetY = offsetY - (int) offsetY;
            double subPixelOffsetX = offsetX - (int) offsetX;
            for (int dstY = 0; dstY < destRect.getHeight(); dstY++) {
                double srcYF = subPixelOffsetY + (scaleY * (dstY + 0.5)) - 0.5;
                int srcY = (int) srcYF;
                int srcIndexY = getSrcOffset() + srcY * getSrcScalineStride();
                double wy = srcYF - srcY;
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
                    double v = v0 + wy * (v1 - v0);
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

    static class CubicConvolution extends DoubleDataInterpolator {

        @Override
        public void interpolate(Rectangle destRect, Rectangle srcRect, double scaleX, double scaleY,
                                double offsetX, double offsetY) {
            final int srcH = (int) srcRect.getHeight();
            final int srcW = (int) srcRect.getWidth();
            int dstIndexY = getDstOffset();
            double subPixelOffsetY = offsetY - (int) offsetY;
            double subPixelOffsetX = offsetX - (int) offsetX;
            for (int dstY = 0; dstY < destRect.getHeight(); dstY++) {
                double srcYF = subPixelOffsetY + (scaleY * (dstY + 0.5)) - 0.5;
                int srcY = (int) srcYF;
                int srcIndexY = getSrcOffset() + srcY * getSrcScalineStride();
                double wy = srcYF - srcY;
                for (int dstX = 0; dstX < destRect.getWidth(); dstX++) {
                    double srcXF = subPixelOffsetX + (scaleX * (dstX + 0.5)) - 0.5;
                    int srcX = (int) srcXF;
                    double wx = srcXF - srcX;
                    final double[][] validRectangle = getValidRectangle(srcX, srcY, srcIndexY, srcW, srcH);

                    //being the only value we know to be included for sure
//                    double v11 = getSrcData(srcIndexY + srcX);
//
//                    double v10 = srcX - 1 >= 0 ? getSrcData(srcIndexY + srcX - 1) : v11;
//                    double v01 = srcY - 1 >= 0 ? getSrcData((srcIndexY - getSrcScalineStride()) + srcX) : v11;
//                    double v12 = srcX + 1 < srcW ? getSrcData(srcIndexY + srcX + 1) : v11;
//                    double v21 = srcY + 1 < srcH ? getSrcData((srcIndexY + getSrcScalineStride()) + srcX) : v11;

//                    boolean withinSrcH = srcY + 1 < srcH;
//                    boolean withinSrcW = srcX + 1 < srcW;
//                    double v01 = withinSrcW ? getSrcData(srcIndexY + srcX + 1) : v00;
//                    double v10 = withinSrcH ? getSrcData((srcIndexY + getSrcScalineStride()) + srcX) : v00;
//                    double v11 = withinSrcW && withinSrcH ? getSrcData((srcIndexY + getSrcScalineStride()) + srcX + 1) : v00;
//                    double v0 = v00 + wx * (v01 - v00);
//                    double v1 = v10 + wx * (v11 - v10);
//                    double v = v0 + wy * (v1 - v0);

//                    if (Double.isNaN(v) || Math.abs(v - getNoDataValue()) < 1e-8) {
//                        setDstData(dstIndexY + dstX, getNoDataValue());
//                    } else {
//                        setDstData(dstIndexY + dstX, v);
//                    }
                }
                dstIndexY += getDstScalineStride();
            }
        }

        double[][] getValidRectangle(int srcX, int srcY, int srcIndexY, int srcW, int srcH) {
            double[][] validRectangle = new double[4][4];
            validRectangle[1][1] = getSrcData(srcIndexY + srcX);

            final boolean notAtLeftBorder = srcX - 1 >= 0;
            final boolean notAtUpperBorder = srcY - 1 >= 0;
            final boolean notAtRightBorder = srcX + 1 < srcW;
            final boolean notAtRightBorder2 = srcX + 2 < srcW;
            final boolean notAtLowerBorder = srcY + 1 < srcH;
            final boolean notAtLowerBorder2 = srcY + 2 < srcH;
            validRectangle[0][1] = notAtLeftBorder ? getSrcData(srcIndexY, srcX, 0, 1) : validRectangle[1][1];
            validRectangle[1][0] = notAtUpperBorder ? getSrcData(srcIndexY, srcX, 1, 0) : validRectangle[1][1];
            validRectangle[2][1] = notAtRightBorder ? getSrcData(srcIndexY, srcX, 2, 1) : validRectangle[1][1];
            validRectangle[1][2] = notAtLowerBorder ? getSrcData(srcIndexY, srcX, 1, 2) : validRectangle[1][1];
            validRectangle[3][1] = notAtRightBorder2 ? getSrcData(srcIndexY, srcX, 3, 1) : validRectangle[2][1];
            validRectangle[1][3] = notAtLowerBorder2 ? getSrcData(srcIndexY, srcX, 1, 3) : validRectangle[1][2];

            if (notAtLeftBorder) {
                if (notAtUpperBorder) {
                    validRectangle[0][0] = getSrcData(srcIndexY, srcX, 0, 0);
                } else {
                    validRectangle[0][0] = validRectangle[0][1];
                }
            } else if (notAtUpperBorder) {
                validRectangle[0][0] = validRectangle[1][0];
            } else {
                validRectangle[0][0] = validRectangle[1][1];
            }
            if (notAtRightBorder) {
                if (notAtUpperBorder) {
                    validRectangle[2][0] = getSrcData(srcIndexY, srcX, 2, 0);
                } else {
                    validRectangle[2][0] = validRectangle[2][1];
                }
            } else if (notAtUpperBorder) {
                validRectangle[2][0] = validRectangle[1][0];
            } else {
                validRectangle[2][0] = validRectangle[1][1];
            }
            if (notAtRightBorder) {
                if (notAtLowerBorder) {
                    validRectangle[2][2] = getSrcData(srcIndexY, srcX, 2, 2);
                } else {
                    validRectangle[2][2] = validRectangle[2][1];
                }
            } else if (notAtLowerBorder) {
                validRectangle[2][2] = validRectangle[1][2];
            } else {
                validRectangle[2][2] = validRectangle[1][1];
            }
            if (notAtLeftBorder) {
                if (notAtLowerBorder) {
                    validRectangle[0][2] = getSrcData(srcIndexY, srcX, 0, 2);
                } else {
                    validRectangle[0][2] = validRectangle[0][1];
                }
            } else if (notAtLowerBorder) {
                validRectangle[0][2] = validRectangle[1][2];
            } else {
                validRectangle[0][2] = validRectangle[1][1];
            }
            if (notAtRightBorder2) {
                if (notAtUpperBorder) {
                    validRectangle[3][0] = getSrcData(srcIndexY, srcX, 3, 0);
                } else {
                    validRectangle[3][0] = validRectangle[3][1];
                }
            } else {
                validRectangle[3][0] = validRectangle[2][0];
            }
            if (notAtRightBorder2) {
                if (notAtLowerBorder) {
                    validRectangle[3][2] = getSrcData(srcIndexY, srcX, 3, 2);
                } else {
                    validRectangle[3][2] = validRectangle[3][1];
                }
            } else {
                validRectangle[3][2] = validRectangle[2][2];
            }
            if (notAtRightBorder) {
                if (notAtLowerBorder2) {
                    validRectangle[2][3] = getSrcData(srcIndexY, srcX, 2, 3);
                } else {
                    validRectangle[2][3] = validRectangle[2][2];
                }
            } else {
                validRectangle[2][3] = validRectangle[1][3];
            }
            if (notAtLeftBorder) {
                if (notAtLowerBorder2) {
                    validRectangle[0][3] = getSrcData(srcIndexY, srcX, 0, 3);
                } else {
                    validRectangle[0][3] = validRectangle[0][2];
                }
            } else {
                validRectangle[0][3] = validRectangle[1][3];
            }
            if (notAtRightBorder2) {
                if (notAtLowerBorder2) {
                    validRectangle[3][3] = getSrcData(srcIndexY, srcX, 3, 3);
                } else {
                    validRectangle[3][3] = validRectangle[3][2];
                }
            } else if (notAtLowerBorder2) {
                validRectangle[3][3] = validRectangle[2][3];
            } else {
                validRectangle[3][3] = validRectangle[2][2];
            }
            return validRectangle;
        }

        private double getSrcData(int srcIndexY, int srcX, int x, int y) {
            int yOffset = (y - 1) * getSrcScalineStride();
            int xOffset = srcX + (x - 1);
            return getSrcData(srcIndexY + yOffset + xOffset);
        }

    }

}
