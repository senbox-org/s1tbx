package org.esa.beam.statistics.tools;

class GeometryID implements Comparable<GeometryID> {

    public final String name;

    public GeometryID(String name) {
        this.name = name;
    }

    @Override
    public int compareTo(GeometryID o) {
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
        GeometryID that = (GeometryID) o;
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
