package org.esa.beam.opengis.ct;

import org.esa.beam.opengis.ct.MathTransform;

public class ConcatMathTransform implements MathTransform {
    public MathTransform[] mts;

    public ConcatMathTransform(MathTransform[] mts) {
        this.mts = mts;
    }
}
