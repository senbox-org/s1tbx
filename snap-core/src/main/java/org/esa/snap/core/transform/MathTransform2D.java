package org.esa.snap.core.transform;

import com.vividsolutions.jts.geom.Geometry;
import org.geotools.geometry.jts.JTS;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.opengis.referencing.operation.TransformException;

/**
 * An extension of the {@code org.opengis.referencing.operation.MathTransform2D} interface
 * which adds a new transformation method for JTS {@code Geometry} objects.
 *
 * @author Tonio Fincke
 * @author Norman Fomferra
 */
public interface MathTransform2D extends org.opengis.referencing.operation.MathTransform2D {

    /**
     * The identity transform.
     */
    MathTransform2D IDENTITY = new IdentityTransform2D();

    /**
     * Gets the dimension of input points.
     *
     * @return The dimension of input points.
     */
    default int getSourceDimensions() {
        return 2;
    }

    /**
     * Gets the dimension of output points.
     *
     * @return The dimension of output points.
     */
    default int getTargetDimensions() {
        return 2;
    }

    /**
     * Transforms the specified {@code geometry}.
     *
     * @param geometry the geometry to be transformed.
     * @return the transformed geometry.
     * @throws TransformException if the geometry can't be transformed.
     */
    default Geometry transform(Geometry geometry) throws TransformException {
        return JTS.transform(geometry, this);
    }

    /**
     * Creates the inverse transform of this object.
     *
     * @return The inverse transform.
     * @throws NoninvertibleTransformException if the transform can't be inversed.
     */
    MathTransform2D inverse() throws NoninvertibleTransformException;
}
