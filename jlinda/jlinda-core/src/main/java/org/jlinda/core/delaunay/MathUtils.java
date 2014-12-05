package org.jlinda.core.delaunay;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * Math functions for computational geometry.
 * 
 */

public final class MathUtils {

   /**
    * Orientation of the p0-p1-p2 triangle. The function returns : <ul>
    * <li>-1 if p0-p1-p2 triangle is cw (or p2 on right of p0 - p1)</li>
    * <li>0 if p0-p1-p2 alignedtriangle is cw (or p2 on right of p0 - p1)</li>
    * <li>1 if p0-p1-p2 triangle is ccw (or p2 on left of p0 - p1)</li>
    * </ul>
    * Some versions of this function compare the length of p0-p1 and p0-p2
    * to return positive, negative or null in the colinear case.
    * This one just return 0 if p0, p1 and p2 are colinear.
    * @return a negative, null or positive integer depending on the position
    * p2 relative to p0 - p1
    */
    public static final int ccw (double p0x, double p0y,
                           double p1x, double p1y,
                           double p2x, double p2y) {
        double dx1dy2 = (p1x - p0x) * (p2y - p0y);
        double dy1dx2 = (p1y - p0y) * (p2x - p0x);
        if (dx1dy2 > dy1dx2) return 1;
        else if (dx1dy2 < dy1dx2) return -1;
        else {
            return 0;
          //if (dx1 * dx2 < 0 || dy1 * dy2 < 0) return -1;
          //else if (dx1*dx1 + dy1*dy1 >= dx2*dx2 + dy2*dy2) return 0;
          //else return 1;
        }
    }



   /**
    * Return a positive value if the point p4 lies inside the
    * circle passing through pa, pb, and pc; a negative value if
    * it lies outside; and zero if the four points are cocircular.
    * The points pa, pb, and pc must be in counterclockwise
    * order, or the sign of the result will be reversed.
    */
    public static double fastInCircle(final Coordinate p1,
                                      final Coordinate p2,
                                      final Coordinate p3,
                                      final Coordinate p4) {

        final double adx = p1.x-p4.x;
        final double ady = p1.y-p4.y;
        final double bdx = p2.x-p4.x;
        final double bdy = p2.y-p4.y;
        final double cdx = p3.x-p4.x;
        final double cdy = p3.y-p4.y;

        final double abdet = adx * bdy - bdx * ady;
        final double bcdet = bdx * cdy - cdx * bdy;
        final double cadet = cdx * ady - adx * cdy;
        final double alift = adx * adx + ady * ady;
        final double blift = bdx * bdx + bdy * bdy;
        final double clift = cdx * cdx + cdy * cdy;

        return alift * bcdet + blift * cadet + clift * abdet;
    }
}
