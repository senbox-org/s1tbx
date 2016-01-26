package org.esa.snap.core.transform;

import com.vividsolutions.jts.geom.Geometry;
import org.geotools.referencing.operation.transform.IdentityTransform;
import org.opengis.referencing.operation.TransformException;

import java.awt.geom.Point2D;

/**
 * The identity transform. The data are only copied without any transformation.
 *
 * @author Tonio Fincke
 * @author Norman Fomferra
 */
class IdentityTransform2D extends IdentityTransform implements MathTransform2D {

    public IdentityTransform2D() {
        super(2);
    }

    /**
     * Transforms the specified {@code geometry}.
     *
     * @param geometry the geometry to be transformed.
     * @return the transformed geometry.
     * @throws TransformException if the geometry can't be transformed.
     */
    @Override
    public Geometry transform(Geometry geometry) throws TransformException {
        return (Geometry) geometry.clone();
    }

    /**
     * Copies the location of {@code ptSrc} to {@code ptDst}.
     *
     * @param  ptSrc the coordinate point to be transformed.
     * @param  ptDst the coordinate point that stores the location of {@code ptSrc},
     *         or {@code null} if a new point should be created.
     * @return the coordinate point after copying the location of {@code ptSrc} to
     *         {@code ptDst} or to a new point if {@code ptDst} was null.
     */
    @Override
    public Point2D transform(Point2D ptSrc, Point2D ptDst) throws TransformException {
        if(ptDst != null) {
            ptDst.setLocation(ptSrc);
        } else {
            ptDst = new Point2D.Double(ptSrc.getX(), ptSrc.getY());
        }
        return ptDst;
    }

    /**
     * Returns the inverse transform of this object, which
     * is this transform itself
     */
    @Override
    public MathTransform2D inverse() {
        return this;
    }
}