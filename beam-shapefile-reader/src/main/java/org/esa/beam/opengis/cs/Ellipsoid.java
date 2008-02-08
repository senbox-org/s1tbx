package org.esa.beam.opengis.cs;

public class Ellipsoid {
    public String name;
    public double semiMajorAxis;
    public double inverseFlattening;
    public Authority authority;

    public Ellipsoid(String name, double semiMajorAxis, double inverseFlattening, Authority authority) {
        this.name = name;
        this.semiMajorAxis = semiMajorAxis;
        this.inverseFlattening = inverseFlattening;
        this.authority = authority;
    }
}
