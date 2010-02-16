package com.bc.ceres.swing.update;

class CaselessKey implements Comparable {

    private final String key;
    private final int hashCode;

    public CaselessKey(String key) {
        this.key = key;
        hashCode = key.toLowerCase().hashCode();
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        return key.equalsIgnoreCase(obj.toString());
    }

    @Override
    public String toString() {
        return key;
    }

    public int compareTo(Object obj) {
        if (obj == null) {
            return 1;
        }
        return key.compareToIgnoreCase(obj.toString());
    }
}
