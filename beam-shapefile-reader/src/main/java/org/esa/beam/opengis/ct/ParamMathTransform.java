package org.esa.beam.opengis.ct;

public class ParamMathTransform implements MathTransform {
    public String name;
    public Parameter[] parameters;

    public ParamMathTransform(String name, Parameter[] parameters) {
        this.name = name;
        this.parameters = parameters;
    }
}
