package org.esa.snap.core.gpf.common.resample;

import org.esa.snap.core.datamodel.RasterDataNode;

import javax.media.jai.RasterAccessor;
import javax.media.jai.RasterFormatTag;
import java.awt.Rectangle;
import java.awt.image.Raster;

/**
 * @author Tonio Fincke
 */
public abstract class DoubleDataInterpolator implements Interpolator {

    private DoubleDataAccessor accessor;
    protected RasterDataNode rasterDataNode;

    public void init(RasterDataNode rasterDataNode, RasterAccessor srcAccessor, RasterAccessor dstAccessor, double noDataValue) {
        this.rasterDataNode = rasterDataNode;
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

    public void dispose(){

    };


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
//                double srcYF = subPixelOffsetY + (scaleY * (dstY));
                int srcY = (int) srcYF;
                int srcIndexY = getSrcOffset() + srcY * getSrcScalineStride();
                double wy = srcYF - srcY;
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
                        setDstData(dstIndex, v00);
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
                        setDstData(dstIndex, v);
                    } else if (validityCounter == 1) {
                        if (v00Valid) {
                            setDstData(dstIndex, v00);
                        } else if (v01Valid) {
                            setDstData(dstIndex, v01);
                        } else if (v10Valid) {
                            setDstData(dstIndex, v10);
                        } else {
                            setDstData(dstIndex, v11);
                        }
                    } else if (validityCounter == 2) {
                        if (v00Valid && v01Valid) {
                            setDstData(dstIndex, (v00 * (1 - wx)) + (v01 * wx));
                        } else if (v10Valid && v11Valid) {
                            setDstData(dstIndex, (v10 * (1 - wx)) + (v11 * wx));
                        } else if (v00Valid && v10Valid) {
                            setDstData(dstIndex, (v00 * (1 - wy)) + (v10 * wy));
                        } else if (v01Valid && v11Valid) {
                            setDstData(dstIndex, (v01 * (1 - wy)) + (v11 * wy));
                        } else if (v00Valid && v11Valid) {
                            double ws = 1 / ((wx * wy) + ((1 - wx) * (1 - wy)));
                            double v = ((v00 * (1 - wx)) * (1 - wy)) + (v11 * wx * wy);
                            v *= ws;
                            setDstData(dstIndex, v);
                        } else {
                            double ws = 1 / (((1 - wx) * wy) + (wx * (1 - wy)));
                            double v = (v01 * wx * (1 - wy)) + (v10 * (1 - wx) * wy);
                            v *= ws;
                            setDstData(dstIndex, v);
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
                            setDstData(dstIndex, v);
                        } else if (!v01Valid) {
                            w00 += (0.5 * w01);
                            w11 += (0.5 * w01);
                            double v = v00 * w00 + v10 * w10 + v11 * w11;
                            setDstData(dstIndex, v);
                        } else if (!v10Valid) {
                            w00 += (0.5 * w10);
                            w11 += (0.5 * w10);
                            double v = v00 * w00 + v01 * w01 + v11 * w11;
                            setDstData(dstIndex, v);
                        } else {
                            w01 += (0.5 * w11);
                            w10 += (0.5 * w11);
                            double v = v00 * w00 + v01 * w01 + v10 * w10;
                            setDstData(dstIndex, v);
                        }
                    }
                }
                dstIndexY += getDstScalineStride();
            }
        }

        private boolean isValid(double v) {
            return !Double.isNaN(v) && ((Double.isNaN(getNoDataValue()) || Math.abs(v - getNoDataValue()) > 1e-8));
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
                    double[] rowMeans = new double[4];
                    for (int i = 0; i < 4; i++) {
                        rowMeans[i] =
                                (validRectangle[i][0] * (4 - 8 * (1 + wx) + 5 * Math.pow(1 + wx, 2) - Math.pow(1 + wx, 3))) +
                                        (validRectangle[i][1] * (1 - 2 * Math.pow(wx, 2) + Math.pow(wx, 3))) +
                                        (validRectangle[i][2] * (1 - 2 * Math.pow(1 - wx, 2) + Math.pow(1 - wx, 3))) +
                                        (validRectangle[i][3] * (4 - 8 * (2 - wx) + 5 * Math.pow(2 - wx, 2) - Math.pow(2 - wx, 3)));
                    }
                    double v =
                            (rowMeans[0] * (4 - 8 * (1 + wy) + 5 * Math.pow(1 + wy, 2) - Math.pow(1 + wy, 3))) +
                                    (rowMeans[1] * (1 - 2 * Math.pow(wy, 2) + Math.pow(wy, 3))) +
                                    (rowMeans[2] * (1 - 2 * Math.pow(1 - wy, 2) + Math.pow(1 - wy, 3))) +
                                    (rowMeans[3] * (4 - 8 * (2 - wy) + 5 * Math.pow(2 - wy, 2) - Math.pow(2 - wy, 3)));
                    if (Double.isNaN(v) || Math.abs(v - getNoDataValue()) < 1e-8) {
                        setDstData(dstIndexY + dstX, getNoDataValue());
                    } else {
                        setDstData(dstIndexY + dstX, v);
                    }
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
