package org.esa.beam.opengis.cs;

import org.esa.beam.opengis.ct.MathTransform;

public class FittedCoordinateSystem implements CoordinateSystem {
    public String name;
    public MathTransform toBase;
    public CoordinateSystem baseCs;

    public FittedCoordinateSystem(String name, MathTransform toBase, CoordinateSystem baseCs) {
        this.name = name;
        this.toBase = toBase;
        this.baseCs = baseCs;
    }
}
