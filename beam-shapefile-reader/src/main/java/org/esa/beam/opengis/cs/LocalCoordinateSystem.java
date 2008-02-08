package org.esa.beam.opengis.cs;

public class LocalCoordinateSystem implements CoordinateSystem {
    public String name;
    public LocalDatum localDatum;
    public Unit unit;
    public AxisInfo[] axes;
    public Authority authority;

    public LocalCoordinateSystem(String name, LocalDatum localDatum, Unit unit, AxisInfo[] axes, Authority authority) {
        this.name = name;
        this.localDatum = localDatum;
        this.unit = unit;
        this.axes = axes;
        this.authority = authority;
    }
}
