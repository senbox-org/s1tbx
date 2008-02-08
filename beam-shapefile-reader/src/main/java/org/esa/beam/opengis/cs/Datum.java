package org.esa.beam.opengis.cs;

public class Datum {
    public String name;
    public int datumType;
    public Authority authority;

    public Datum(String name, int datumType, Authority authority) {
        this.name = name;
        this.datumType = datumType;
        this.authority = authority;
    }
}
