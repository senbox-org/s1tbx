package org.esa.beam.opengis.pt;

public class Envelope {
    public CoordinatePoint minCP;
    public CoordinatePoint maxCP;

    public Envelope(CoordinatePoint minCP, CoordinatePoint maxCP) {
        this.minCP = minCP;
        this.maxCP = maxCP;
    }
}
