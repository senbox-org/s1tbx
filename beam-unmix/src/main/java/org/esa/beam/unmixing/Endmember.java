package org.esa.beam.unmixing;

import com.bc.ceres.core.Assert;

public class Endmember {
    private final String name;
    private final double[] wavelengths;
    private final double[] radiations;


    public Endmember(String name, double[] wavelengths, double[] radiations) {
        Assert.notNull(name, "name");
        if (wavelengths.length != radiations.length) {
            throw new IllegalArgumentException("wavelengths.length != radiations.length");
        }
        this.name = name;
        this.wavelengths = wavelengths;
        this.radiations = radiations;
    }

    public String getName() {
        return name;
    }

    public int getSize() {
        return wavelengths.length;
    }

    public double getWavelength(int i) {
        return wavelengths[i];
    }

    public double[] getWavelengths() {
        return wavelengths.clone();
    }

    public double getRadiation(int i) {
        return radiations[i];
    }

    public double[] getRadiations() {
        return radiations.clone();
    }

    @Override
    public String toString() {
        return getName();
    }
}
