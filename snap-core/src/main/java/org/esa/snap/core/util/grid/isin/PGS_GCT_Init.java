package org.esa.snap.core.util.grid.isin;

class PGS_GCT_Init {

    static IsinForward forward(double[] projParam) {
        final double rMajor = projParam[0];
        final double radius = projParam[0];
        final double rMinor = projParam[1];
        final double falseEasting = projParam[6];
        final double falseNorthing = projParam[7];
        final double centerLong = projParam[4];
        final double dzone = projParam[8];
        final double djustify = projParam[10];

        final IsinForward isinForward = new IsinForward();
        isinForward.init(radius, centerLong, falseEasting, falseNorthing, dzone, djustify);
        return isinForward;
    }

    static void reverse(double[] projParam) {
        final double rMajor = projParam[0];
        final double radius = projParam[0];
        final double rMinor = projParam[1];
        final double falseEasting = projParam[6];
        final double falseNorthing = projParam[7];
        final double centerLong = projParam[4];
        final double dzone = projParam[8];
        final double djustify = projParam[10];
    }
}
