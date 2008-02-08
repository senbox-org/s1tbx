package org.esa.beam.opengis.cs;

public class HorizontalDatum {
    public String name;
    public Ellipsoid ellipsoid;
    public WGS84ConversionInfo toWgs84;
    public Authority authority;

    public HorizontalDatum(String name, Ellipsoid ellipsoid, WGS84ConversionInfo toWgs84, Authority authority) {
        this.name = name;
        this.ellipsoid = ellipsoid;
        this.toWgs84 = toWgs84;
        this.authority = authority;
    }
}
