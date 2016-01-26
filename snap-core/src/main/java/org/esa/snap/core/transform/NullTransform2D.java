package org.esa.snap.core.transform;

import com.vividsolutions.jts.geom.Geometry;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.opengis.referencing.operation.TransformException;

import java.awt.geom.Point2D;

/**
 * A class to encapsulate the fact that no transformation is given.
 *
 * @author Tonio Fincke
 */
class NullTransform2D extends AbstractTransform2D {

    @Override
    public Point2D transform(Point2D ptSrc, Point2D ptDst) throws TransformException {
        throw new TransformException("No transformation given");
    }

    @Override
    public Geometry transform(Geometry geometry) throws TransformException {
        throw new TransformException("No transformation given");
    }

    @Override
    public MathTransform2D inverse() throws NoninvertibleTransformException {
        throw new NoninvertibleTransformException("Cannot invert transformation");
    }

    @Override
    public boolean equals(Object object) {
        return this == object;
    }

    @Override
    public int hashCode() {
        return 0;
    }
}
