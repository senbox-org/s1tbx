package org.esa.beam.opengis.ct;

public class PassthroughMathTransform implements MathTransform {
    public int value;
    public MathTransform mt;

    public PassthroughMathTransform(int value, MathTransform mt) {
        this.value = value;
        this.mt = mt;
    }
}
