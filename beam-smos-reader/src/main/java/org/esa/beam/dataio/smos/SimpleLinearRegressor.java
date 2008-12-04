package org.esa.beam.dataio.smos;

import java.awt.geom.Point2D;

/**
 * Class performing the function of calculating simple linear regressions. See
 * Mendenhall & Sincich (1995, Statistics for Engineering and the Sciences).
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.6
 */
public class SimpleLinearRegressor {

    final PointFilter pointFilter;

    private int count;
    private double sx;

    private double sy;
    private double sxx;

    private double sxy;

    public SimpleLinearRegressor() {
        this(PointFilter.NULL);
    }

    public SimpleLinearRegressor(PointFilter pointFilter) {
        if (pointFilter == null) {
            pointFilter = PointFilter.NULL;
        }
        this.pointFilter = pointFilter;
    }

    /**
     * Adds a point (x, y) to the regression, if accepted.
     *
     * @param x the x-coordinate of the point to be added.
     * @param y the y-coordinate of the point to be added.
     *
     * @return {@code true} if the point was accepted and added to the regression,
     *         otherwise {@code false}.
     */
    public boolean add(double x, double y) {
        final boolean accepted = pointFilter.accept(x, y);

        if (accepted) {
            sx += x;
            sy += y;
            sxx += x * x;
            sxy += x * y;
            ++count;
        }

        return accepted;
    }

    /**
     * Returns the number of valid ({@code x[i]}, {@code y[i]}) pairs.
     *
     * @return the number of valid pairs.
     */
    public final int getPointCount() {
        return count;
    }

    /**
     * Returns the regression for the current points.
     *
     * @return a point (x, y) where x is the slope of the regression line,
     *         and y is the intercept with the y-axis.
     */
    public final Point2D getRegression() {
        final double a = (count * sxy - sx * sy) / (count * sxx - sx * sx);
        final double b = (sy - a * sx) / count;

        return new Point2D.Double(a, b);
    }
}
