package org.jlinda.core.delaunay;

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
    public static final double fastInCircle(double p1x, double p1y,
                                            double p2x, double p2y,
                                            double p3x, double p3y,
                                            double p4x, double p4y) {
        double adx, ady, bdx, bdy, cdx, cdy;
        double abdet, bcdet, cadet;
        double alift, blift, clift;

        adx = p1x-p4x;
        ady = p1y-p4y;
        bdx = p2x-p4x;
        bdy = p2y-p4y;
        cdx = p3x-p4x;
        cdy = p3y-p4y;

        abdet = adx * bdy - bdx * ady;
        bcdet = bdx * cdy - cdx * bdy;
        cadet = cdx * ady - adx * cdy;
        alift = adx * adx + ady * ady;
        blift = bdx * bdx + bdy * bdy;
        clift = cdx * cdx + cdy * cdy;

        return alift * bcdet + blift * cadet + clift * abdet;
    }
}
