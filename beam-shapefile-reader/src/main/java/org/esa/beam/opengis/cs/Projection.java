package org.esa.beam.opengis.cs;

import org.esa.beam.opengis.ct.Parameter;

public class Projection {
    public String name;
    public Authority authority;
    public Parameter[] parameters;

    public Projection(String name, Authority authority) {
        this.name = name;
        this.authority = authority;
    }
}
