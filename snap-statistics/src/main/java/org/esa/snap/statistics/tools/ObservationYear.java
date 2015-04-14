package org.esa.snap.statistics.tools;

public class ObservationYear implements Comparable<ObservationYear> {

    public final Integer year;

    public ObservationYear(int value) {
        year = value;
    }

    @Override
    public int compareTo(ObservationYear o) {
        return year.compareTo(o.year);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ObservationYear && year.equals(((ObservationYear) o).year);
    }

    @Override
    public int hashCode() {
        return year.hashCode();
    }

    @Override
    public String toString() {
        return year.toString();
    }
}
