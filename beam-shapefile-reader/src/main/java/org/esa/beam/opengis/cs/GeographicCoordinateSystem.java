package org.esa.beam.opengis.cs;

public class GeographicCoordinateSystem extends HorizontalCoordinateSystem {

    public String name;
    public PrimeMeridian primem;
    public Unit angularUnit;
    public AxisInfo axis1;
    public AxisInfo axis2;
    public Authority authority;

    public GeographicCoordinateSystem(String name, HorizontalDatum datum, PrimeMeridian primem, Unit angularUnit, AxisInfo axis1, AxisInfo axis2, Authority authority) {
        super(datum);
        this.name = name;
        this.datum = datum;
        this.primem = primem;
        this.angularUnit = angularUnit;
        this.axis1 = axis1;
        this.axis2 = axis2;
        this.authority = authority;
    }
}
