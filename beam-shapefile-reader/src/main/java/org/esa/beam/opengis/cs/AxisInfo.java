package org.esa.beam.opengis.cs;

public class AxisInfo {
    public String name;
    public Orientation orientation;

    public AxisInfo(String name, Orientation orientation) {
        this.name = name;
        this.orientation = orientation;
    }

    public enum Orientation {
        NORTH, SOUTH, EAST, WEST, UP, DOWN, OTHER;
    }
}
