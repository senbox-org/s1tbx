package org.esa.beam.opengis.cs;

// <unit> = UNIT["<name>", <conversion factor> {,<authority>}]
public class Unit {
    public String name;
    public double conversionFactor;
    public Authority authority;

    public Unit(String name, double conversionFactor, Authority authority) {
        this.name = name;
        this.conversionFactor = conversionFactor;
        this.authority = authority;
    }
}
