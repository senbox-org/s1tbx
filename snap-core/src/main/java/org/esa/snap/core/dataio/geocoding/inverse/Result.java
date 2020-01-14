package org.esa.snap.core.dataio.geocoding.inverse;

class Result {

    int x;
    int y;
    double delta;

    Result() {
        delta = Double.MAX_VALUE;
    }

    final boolean update(final int x, final int y, final double delta) {
        final boolean b = delta < this.delta;
        if (b) {
            this.x = x;
            this.y = y;
            this.delta = delta;
        }
        return b;
    }
}
