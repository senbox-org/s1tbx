package org.esa.beam.opengis.ct;

import org.esa.beam.opengis.ct.MathTransform;

public class InverseMathTransform implements MathTransform {
    public MathTransform mt;

    public InverseMathTransform(MathTransform mt) {
        this.mt = mt;
    }
}
