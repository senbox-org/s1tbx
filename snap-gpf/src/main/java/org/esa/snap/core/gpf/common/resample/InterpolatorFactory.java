package org.esa.snap.core.gpf.common.resample;

import java.awt.image.DataBuffer;

/**
 * @author Tonio Fincke
 */
public class InterpolatorFactory {

    public static Interpolator createInterpolator(InterpolationType type, int dataType) {
        if (dataType == DataBuffer.TYPE_FLOAT || dataType == DataBuffer.TYPE_DOUBLE) {
            switch (type) {
                case Nearest:
                    return new DoubleDataInterpolator.NearestNeighbour();
                case Bilinear:
                    return new DoubleDataInterpolator.Bilinear();
                case Cubic_Convolution:
                    return new DoubleDataInterpolator.CubicConvolution();
            }
        } else {
            switch (type) {
                case Nearest:
                    return new LongDataInterpolator.NearestNeighbour();
                case Bilinear:
                    return new LongDataInterpolator.Bilinear();
            }
        }
        throw new IllegalArgumentException("Interpolation method not supported");
    }

}
