package com.bc.ceres.compiler;


public class Band {
    String name;
    double[] samples;

    public Band(String name, double[] samples) {
        this.name = name;
        this.samples = samples;
    }

    public String getName() {
        return name;
    }

    public double[] getSamples() {
        return samples;
    }
}
