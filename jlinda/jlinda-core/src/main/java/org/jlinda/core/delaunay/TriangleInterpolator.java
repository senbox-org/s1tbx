package org.jlinda.core.delaunay;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.jlinda.core.Window;

import java.util.ArrayList;
import java.util.List;

public class TriangleInterpolator {

    public static class ZData {
        public final double[] z_1d_in;
        public final double[][] z_in;
        public final double[][] z_out;
        public double a, b, c;

        public ZData(final double[][] z_in, final double[][] z_out) {
            this.z_1d_in = null;
            this.z_in = z_in;
            this.z_out = z_out;
        }

        public ZData(final double[] z_in, final double[][] z_out) {
            this.z_1d_in = z_in;
            this.z_in = null;
            this.z_out = z_out;
        }
    }

    public static void gridDataLinear(final double[][] x_in, final double[][] y_in, final ZData[] zList,
                                      final Window window, final double xyRatio, final int xScale,
                                      final int yScale, final double invalidIndex, final int offset) throws Exception {

        final FastDelaunayTriangulator FDT = triangulate(x_in, y_in, xyRatio, invalidIndex);
        if(FDT != null) {
            interpolate(xyRatio, window, xScale, yScale, offset, invalidIndex, FDT, zList);
        }
    }

    public static void gridDataLinear(final double[] x_in, final double[] y_in, final ZData[] zList,
                                      final Window window, final double xyRatio, final int xScale,
                                      final int yScale, final double invalidIndex, final int offset) throws Exception {

        final FastDelaunayTriangulator FDT = triangulate(x_in, y_in, xyRatio, invalidIndex);
        if(FDT != null) {
            interpolate(xyRatio, window, xScale, yScale, offset, invalidIndex, FDT, zList);
        }
    }

    public static FastDelaunayTriangulator triangulate(final double[][] x_in, final double[][] y_in,
                                                       final double xyRatio, final double invalidIndex)
            throws Exception {

        //// organize input data
        //long t0 = System.currentTimeMillis();
        final List<Geometry> list = new ArrayList<>();
        final GeometryFactory gf = new GeometryFactory();
        for (int i = 0; i < x_in.length; i++) {
            for (int j = 0; j < x_in[0].length; j++) {
                if (x_in[i][j] == invalidIndex || y_in[i][j] == invalidIndex) {
                    continue;
                }
                list.add(gf.createPoint(new Coordinate(x_in[i][j], y_in[i][j] * xyRatio, i*x_in[0].length + j)));
            }
        }
        //long t1 = System.currentTimeMillis();
        //SystemUtils.LOG.info("Input set constructed in " + (0.001 * (t1 - t0)) + " sec");

        if (list.size() < 3) {
            return null;
        }

        //// triangulate input data
        //long t2 = System.currentTimeMillis();
        FastDelaunayTriangulator FDT = new FastDelaunayTriangulator();
        try {
            FDT.triangulate(list.iterator());
        } catch (TriangulationException te) {
            te.printStackTrace();
        }
        //long t3 = System.currentTimeMillis();
        //SystemUtils.LOG.info("Data set triangulated in " + (0.001 * (t3 - t2)) + " sec");
        return FDT;
    }

    public static FastDelaunayTriangulator triangulate(final double[] x_in, final double[] y_in,
                                                       final double xyRatio, final double invalidIndex)
            throws Exception {

        java.util.List<Geometry> list = new ArrayList<>();
        GeometryFactory gf = new GeometryFactory();
        for (int i = 0; i < x_in.length; i++) {
            if (x_in[i] == invalidIndex || y_in[i] == invalidIndex) {
                continue;
            }
            list.add(gf.createPoint(new Coordinate(x_in[i], y_in[i] * xyRatio, i)));
        }

        if (list.size() < 3) {
            return null;
        }

        FastDelaunayTriangulator FDT = new FastDelaunayTriangulator();
        try {
            FDT.triangulate(list.iterator());
        } catch (TriangulationException te) {
            te.printStackTrace();
        }

        return FDT;
    }

    public static void interpolate(final double xyRatio, final Window tileWindow,
                                   final double xScale, final double yScale,
                                   final double offset, final double invalidIndex,
                                   final FastDelaunayTriangulator FDT, final ZData[] zList) {

        final double x_min = tileWindow.linelo;
        final double y_min = tileWindow.pixlo;

        long i_min, i_max, j_min, j_max; // minimas/maximas
        double xp, yp;
        double xkj, ykj, xlj, ylj;
        double f; // function

        // containers for xy coordinates of Triangles: p1-p2-p3-p1
        final double[] vx = new double[4];
        final double[] vy = new double[4];
        final double[] vz = new double[3];

        final int nx = (int) tileWindow.lines();
        final int ny = (int) tileWindow.pixels();

        //// interpolate: loop over triangles
        //long t4 = System.currentTimeMillis();
        for (Triangle triangle : FDT.triangles) {

            // store triangle coordinates in local variables
            vx[0] = vx[3] = triangle.getA().x;
            vy[0] = vy[3] = triangle.getA().y / xyRatio;

            vx[1] = triangle.getB().x;
            vy[1] = triangle.getB().y / xyRatio;

            vx[2] = triangle.getC().x;
            vy[2] = triangle.getC().y / xyRatio;

            // skip invalid indices
            if (vx[0] == invalidIndex || vx[1] == invalidIndex || vx[2] == invalidIndex ||
                    vy[0] == invalidIndex || vy[1] == invalidIndex || vy[2] == invalidIndex) {
                continue;
            }

            // Compute grid indices the current triangle may cover
            xp = Math.min(Math.min(vx[0], vx[1]), vx[2]);
            i_min = coordToIndex(xp, x_min, xScale, offset);

            xp = Math.max(Math.max(vx[0], vx[1]), vx[2]);
            i_max = coordToIndex(xp, x_min, xScale, offset);

            yp = Math.min(Math.min(vy[0], vy[1]), vy[2]);
            j_min = coordToIndex(yp, y_min, yScale, offset);

            yp = Math.max(Math.max(vy[0], vy[1]), vy[2]);
            j_max = coordToIndex(yp, y_min, yScale, offset);

            // skip triangle that is above or below the region
            if ((i_max < 0) || (i_min >= nx)) {
                continue;
            }

            // skip triangle that is on the left or right of the region
            if ((j_max < 0) || (j_min >= ny)) {
                continue;
            }

            // triangle covers the upper or lower boundary
            if (i_min < 0) {
                i_min = 0;
            }

            if (i_max >= nx) {
                i_max = nx - 1;
            }

            // triangle covers left or right boundary
            if (j_min < 0) {
                j_min = 0;
            }

            if (j_max >= ny) {
                j_max = ny - 1;
            }

            // compute plane defined by the three vertices of the triangle: z = ax + by + c
            xkj = vx[1] - vx[0];
            ykj = vy[1] - vy[0];
            xlj = vx[2] - vx[0];
            ylj = vy[2] - vy[0];

            f = 1.0 / (xkj * ylj - ykj * xlj);

            vz[0] = triangle.getA().z;
            vz[1] = triangle.getB().z;
            vz[2] = triangle.getC().z;

            for(ZData data : zList) {
                getABC(vx, vy, vz, data, f, xkj, ykj, xlj, ylj);
            }

            final PointInTriangle pointInTriangle = new PointInTriangle(vx, vy);

            for (int i = (int)i_min; i <= i_max; i++) {
                xp = x_min + i * xScale + offset;
                for (int j = (int)j_min; j <= j_max; j++) {
                    yp = y_min + j * yScale + offset;

                    if(!pointInTriangle.test(xp, yp)) {
                        continue;
                    }

                    for(ZData d : zList) {
                        d.z_out[i][j] = d.a * xp + d.b * yp + d.c;
                    }
                }
            }
        }
        //long t5 = System.currentTimeMillis();
        //SystemUtils.LOG.info("Data set interpolated in " + (0.001 * (t5 - t4)) + " sec");
    }

    private static void getABC(
            final double[] vx, final double[] vy, final double[] vz, final ZData data,
            final double f, final double  xkj, final double ykj, final double xlj, final double ylj) {

        double zj, zk, zl;
        if (data.z_1d_in != null) {
            zj = data.z_1d_in[(int)vz[0]];
            zk = data.z_1d_in[(int)vz[1]];
            zl = data.z_1d_in[(int)vz[2]];
        } else {
            final int i0 = (int)(vz[0]/data.z_in[0].length);
            final int j0 = (int)(vz[0] - i0*data.z_in[0].length);
            zj = data.z_in[i0][j0];

            final int i1 = (int)(vz[1]/data.z_in[1].length);
            final int j1 = (int)(vz[1] - i1*data.z_in[1].length);
            zk = data.z_in[i1][j1];

            final int i2 = (int)(vz[2]/data.z_in[2].length);
            final int j2 = (int)(vz[2] - i2*data.z_in[2].length);
            zl = data.z_in[i2][j2];
        }

        final double zkj = zk - zj;
        final double zlj = zl - zj;

        data.a = -f * (ykj * zlj - zkj * ylj);
        data.b = -f * (zkj * xlj - xkj * zlj);
        data.c = -data.a * vx[1] - data.b * vy[1] + zk;
    }

    public static long coordToIndex(final double coord, final double coord0, final double deltaCoord, final double offset) {
        return (long) Math.floor((((coord - coord0) / (deltaCoord)) - offset) + 0.5);
    }

    private static class PointInTriangle {
        private final double[] xt, yt;
        private final double xtd0, xtd1, xtd2, ytd0, ytd1, ytd2;

        public PointInTriangle(double[] xt, double[] yt) {
            this.xt = xt;
            this.yt = yt;
            xtd0 = xt[2] - xt[0];
            xtd1 = xt[0] - xt[1];
            xtd2 = xt[1] - xt[2];
            ytd0 = yt[2] - yt[0];
            ytd1 = yt[0] - yt[1];
            ytd2 = yt[1] - yt[2];
        }

        public boolean test(double x, double y) {
            int iRet0 = (xtd0 * (y - yt[0])) > ((x - xt[0]) * ytd0) ? 1 : -1;
            int iRet1 = (xtd1 * (y - yt[1])) > ((x - xt[1]) * ytd1) ? 1 : -1;
            int iRet2 = (xtd2 * (y - yt[2])) > ((x - xt[2]) * ytd2) ? 1 : -1;

            return (iRet0 > 0 && iRet1 > 0 && iRet2 > 0) || (iRet0 < 0 && iRet1 < 0 && iRet2 < 0);
        }
    }
}
