package org.esa.beam.opengis.cs;

public class CompoundCoordinateSystem implements CoordinateSystem {
    public String name;
    public CoordinateSystem headCs;
    public CoordinateSystem tailCs;
    public Authority authority;

    public CompoundCoordinateSystem(String name, CoordinateSystem headCs, CoordinateSystem tailCs, Authority authority) {
        this.name = name;
        this.headCs = headCs;
        this.tailCs = tailCs;
        this.authority = authority;
    }
}
