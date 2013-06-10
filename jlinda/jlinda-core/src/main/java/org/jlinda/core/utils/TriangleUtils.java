package org.jlinda.core.utils;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import org.apache.log4j.Logger;
import org.jlinda.core.Window;
import org.jlinda.core.delaunay.FastDelaunayTriangulator;
import org.jlinda.core.delaunay.Triangle;
import org.jlinda.core.delaunay.TriangulationException;

import java.util.ArrayList;
import java.util.List;

public class TriangleUtils {

    static Logger logger = Logger.getLogger(TriangleUtils.class.getName());

    public static double[][] gridDataLinear(final double[][] x_in, final double[][] y_in, final double[][] z_in,
                                            final Window window, final double xyRatio, final int xScale,
                                            final int yScale, final double nodata, final int offset) throws Exception {

/*
        // number of elements
        final int x_in_dim = x_in.length * x_in[0].length;
        final int z_in_dim = z_in.length * z_in[0].length;

        //// How many groups of z value should be interpolated
        // TODO: work out "multiple levels" for interpolation
        if ((z_in_dim % x_in_dim) != 0) {
            logger.warn("The input of the DEM buffer and z is not the same...");
            throw new IllegalArgumentException();
        } else {
            int zLoops = z_in.length / x_in.length;
        }

        logger.trace("DelaunayTriangulator with " + x_in_dim + " points");
*/

        final FastDelaunayTriangulator FDT = triangulate(x_in, y_in, z_in, xyRatio);
        return interpolate(xyRatio, window, xScale, yScale, offset, nodata, FDT);

    }

    private static FastDelaunayTriangulator triangulate(double[][] x_in, double[][] y_in, double[][] z_in,
                                                        double xyRatio) throws Exception {

        //// organize input data
        long t0 = System.currentTimeMillis();
        List<Geometry> list = new ArrayList<Geometry>();
        GeometryFactory gf = new GeometryFactory();
        for (int i = 0; i < x_in.length; i++) {
            for (int j = 0; j < x_in[0].length; j++) {
                list.add(gf.createPoint(new Coordinate(x_in[i][j], y_in[i][j] * xyRatio, z_in[i][j])));
            }
        }
        long t1 = System.currentTimeMillis();
        logger.info("Input set constructed in " + (0.001 * (t1 - t0)) + " sec");

        //// triangulate input data
        long t2 = System.currentTimeMillis();
        FastDelaunayTriangulator FDT = new FastDelaunayTriangulator();
        try {
            FDT.triangulate(list.iterator());
        } catch (TriangulationException te) {
            te.printStackTrace();
        }
        long t3 = System.currentTimeMillis();
        logger.info("Data set triangulated in " + (0.001 * (t3 - t2)) + " sec");
        return FDT;
    }

    private static double[][] interpolate(double xyRatio, final Window tileWindow,
                                          final double xScale, final double yScale,
                                          final double offset, final double nodata,
                                          FastDelaunayTriangulator FDT) {

        final int zLoops = 1;
        final double x_min = tileWindow.linelo;
        final double y_min = tileWindow.pixlo;

        int i, j; // counters
        long i_min, i_max, j_min, j_max; // minimas/maximas
        double xp, yp;
        double xkj, ykj, xlj, ylj;
        double f; // function
        double zj, zk, zl, zkj, zlj;

        // containers
        int zLoop; // z-level - hardcoded!
        double[] a = new double[zLoops];
        double[] b = new double[zLoops];
        double[] c = new double[zLoops];
        // containers for xy coordinates of Triangles: p1-p2-p3-p1
        double[] vx = new double[4];
        double[] vy = new double[4];

        // declare demRadarCode_phase
        double[][] griddedData = new double[(int) tileWindow.lines()][(int) tileWindow.pixels()];
        final int nx = griddedData.length / zLoops;
        final int ny = griddedData[0].length;

        //// interpolate: loop over triangles
        long t4 = System.currentTimeMillis();
        for (Triangle triangle : FDT.triangles) {

            // store triangle coordinates in local variables
            vx[0] = vx[3] = triangle.getA().x;
            vy[0] = vy[3] = triangle.getA().y / xyRatio;

            vx[1] = triangle.getB().x;
            vy[1] = triangle.getB().y / xyRatio;

            vx[2] = triangle.getC().x;
            vy[2] = triangle.getC().y / xyRatio;

            // check whether something is no-data
            if (vx[0] == nodata || vx[1] == nodata || vx[2] == nodata) {
                continue;
            }
            if (vy[0] == nodata || vy[1] == nodata || vy[2] == nodata) {
                continue;
            }

            /* Compute grid indices the current triangle may cover.*/

            xp = Math.min(Math.min(vx[0], vx[1]), vx[2]);
            i_min = coordToIndex(xp, x_min, xScale, offset);

            xp = Math.max(Math.max(vx[0], vx[1]), vx[2]);
            i_max = coordToIndex(xp, x_min, xScale, offset);

            yp = Math.min(Math.min(vy[0], vy[1]), vy[2]);
            j_min = coordToIndex(yp, y_min, yScale, offset);

            yp = Math.max(Math.max(vy[0], vy[1]), vy[2]);
            j_max = coordToIndex(yp, y_min, yScale, offset);

            /* Adjustments for triangles outside -R region. */
            /* Triangle to the left or right. */
            if ((i_max < 0) || (i_min >= nx)) {
                continue;
            }
            /* Triangle Above or below */
            if ((j_max < 0) || (j_min >= ny)) {
                continue;
            }
            /* Triangle covers boundary, left or right. */
            if (i_min < 0) {
                i_min = 0;
            }
            if (i_max >= nx) {
                i_max = nx - 1;
            }
            /* Triangle covers boundary, top or bottom. */
            if (j_min < 0) {
                j_min = 0;
            }
            if (j_max >= ny) {
                j_max = ny - 1;
            }

            /* Find equation for the plane as z = ax + by + c */
            xkj = vx[1] - vx[0];
            ykj = vy[1] - vy[0];
            xlj = vx[2] - vx[0];
            ylj = vy[2] - vy[0];

            f = 1.0 / (xkj * ylj - ykj * xlj);

            for (zLoop = 0; zLoop < zLoops; zLoop++) {
                zj = triangle.getA().z;
                zk = triangle.getB().z;
                zl = triangle.getC().z;
                zkj = zk - zj;
                zlj = zl - zj;
                a[zLoop] = -f * (ykj * zlj - zkj * ylj);
                b[zLoop] = -f * (zkj * xlj - xkj * zlj);
                c[zLoop] = -a[zLoop] * vx[1] - b[zLoop] * vy[1] + zk;
            }

            for (i = (int) i_min; i <= i_max; i++) {

                xp = indexToCoord(i, x_min, xScale, offset);

                for (j = (int) j_min; j <= j_max; j++) {

                    yp = indexToCoord(j, y_min, yScale, offset);

                    if (!pointInTriangle(vx, vy, xp, yp))
                        continue; /* Outside */

                    for (zLoop = 0; zLoop < zLoops; zLoop++) {
                        griddedData[i][j] = a[zLoop] * xp + b[zLoop] * yp + c[zLoop];
                    }
                }
            }
        }
        long t5 = System.currentTimeMillis();
        logger.info("Data set interpolated in " + (0.001 * (t5 - t4)) + " sec");

        return griddedData;
    }

    private static boolean pointInTriangle(double[] xt, double[] yt, double x, double y) {
        int iRet0 = ((xt[2] - xt[0]) * (y - yt[0])) > ((x - xt[0]) * (yt[2] - yt[0])) ? 1 : -1;
        int iRet1 = ((xt[0] - xt[1]) * (y - yt[1])) > ((x - xt[1]) * (yt[0] - yt[1])) ? 1 : -1;
        int iRet2 = ((xt[1] - xt[2]) * (y - yt[2])) > ((x - xt[2]) * (yt[1] - yt[2])) ? 1 : -1;

        return (iRet0 > 0 && iRet1 > 0 && iRet2 > 0) || (iRet0 < 0 && iRet1 < 0 && iRet2 < 0);
    }

    private static long coordToIndex(final double coord, final double coord0, final double deltaCoord, final double offset) {
        return irint((((coord - coord0) / (deltaCoord)) - offset));
    }

    private static double indexToCoord(final long idx, final double coord0, final double deltaCoord, final double offset) {
        return (coord0 + idx * deltaCoord + offset);
    }

    private static long irint(final double coord) {
        return ((long) rint(coord));
    }

    private static double rint(final double coord) {
        return Math.floor(coord + 0.5);
    }

}
