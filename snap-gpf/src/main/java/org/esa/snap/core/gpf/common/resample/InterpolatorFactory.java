package org.esa.snap.core.gpf.common.resample;

import org.esa.snap.core.datamodel.ProductData;

/**
 * @author Tonio Fincke
 */
public class InterpolatorFactory {

    public static Interpolator createInterpolator(InterpolationType type, int dataType) {
        if (dataType == ProductData.TYPE_FLOAT32 || dataType == ProductData.TYPE_FLOAT64) {
            switch (type) {
                case Nearest:
                    return new DoubleDataInterpolator.NearestNeighbour();
                case Bilinear:
                    return new DoubleDataInterpolator.Bilinear();
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
