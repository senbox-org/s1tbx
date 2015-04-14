package org.esa.snap.statistics.tools;

public class ParameterName implements Comparable<ParameterName> {

    public final String name;

    public ParameterName(String name) {
        this.name = name;
    }

    @Override
    public int compareTo(ParameterName o) {
        return name.compareTo(o.name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ParameterName that = (ParameterName) o;
        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return name;
    }
}
