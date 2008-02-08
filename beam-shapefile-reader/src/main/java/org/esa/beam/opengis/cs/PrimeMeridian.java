package org.esa.beam.opengis.cs;

public class PrimeMeridian {
    public String name;
    public double longitude;
    public Authority authority;

    public PrimeMeridian(String name, double longitude, Authority authority) {
        this.name = name;
        this.longitude = longitude;
        this.authority = authority;
    }
}
