package org.esa.beam.unmixing;

import com.bc.ceres.core.Assert;
import org.esa.beam.framework.gpf.annotations.Parameter;

public class Endmember {
    @Parameter(pattern = "[a-zA-Z_0-9]*")
    private String name;
    @Parameter
    private double[] wavelengths;
    @Parameter
    private double[] radiations;

    public Endmember() {
    }

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
