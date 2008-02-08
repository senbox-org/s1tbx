package org.esa.beam.opengis.cs;

public abstract class HorizontalCoordinateSystem implements CoordinateSystem {
    public HorizontalDatum datum;

    protected HorizontalCoordinateSystem(HorizontalDatum datum) {
        this.datum = datum;
    }
}
