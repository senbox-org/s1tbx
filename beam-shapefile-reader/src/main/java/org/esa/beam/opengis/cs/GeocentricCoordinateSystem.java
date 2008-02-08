package org.esa.beam.opengis.cs;

public class GeocentricCoordinateSystem implements CoordinateSystem {
    public String name;
    public HorizontalDatum datum;
    public PrimeMeridian primem;
    public Unit linearUnit;
    public AxisInfo axis1;
    public AxisInfo axis2;
    public AxisInfo axis3;
    public Authority authority;

    public GeocentricCoordinateSystem(String name, HorizontalDatum datum, PrimeMeridian primem, Unit linearUnit, AxisInfo axis1, AxisInfo axis2, AxisInfo axis3, Authority authority) {
        this.name = name;
        this.datum = datum;
        this.primem = primem;
        this.linearUnit = linearUnit;
        this.axis1 = axis1;
        this.axis2 = axis2;
        this.axis3 = axis3;
        this.authority = authority;
    }
}
