package org.esa.beam.opengis.cs;

public class ProjectedCoordinateSystem extends HorizontalCoordinateSystem {
    public String name;
    public GeographicCoordinateSystem geogCs;
    public Projection projection;
    public Unit linearUnit;
    public AxisInfo[] axes;
    public Authority authority;

    public ProjectedCoordinateSystem(String name, GeographicCoordinateSystem geogCs, Projection projection, Unit linearUnit, AxisInfo axis1, AxisInfo axis2, Authority authority) {
        super(geogCs.datum);
        this.name = name;
        this.geogCs = geogCs;
        this.projection = projection;
        this.linearUnit = linearUnit;
        this.axes = new AxisInfo[] {axis1, axis2};
        this.authority = authority;
    }

}
