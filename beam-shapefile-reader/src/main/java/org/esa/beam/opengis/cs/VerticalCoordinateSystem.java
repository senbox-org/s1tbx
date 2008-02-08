package org.esa.beam.opengis.cs;

public class VerticalCoordinateSystem implements CoordinateSystem {
    public String name;
    public VerticalDatum verticalDatum;
    public Unit linearUnit;
    public AxisInfo axisInfo;
    public Authority authority;

    public VerticalCoordinateSystem(String name, VerticalDatum verticalDatum, Unit linearUnit, AxisInfo axisInfo, Authority authority) {
        this.name = name;
        this.verticalDatum = verticalDatum;
        this.linearUnit = linearUnit;
        this.axisInfo = axisInfo;
        this.authority = authority;
    }
}
