package org.esa.snap.core.transform;

import com.vividsolutions.jts.geom.Geometry;
import org.geotools.referencing.operation.transform.IdentityTransform;
import org.opengis.referencing.operation.TransformException;

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
     * Returns the inverse transform of this object, which
     * is this transform itself
     */
    @Override
    public MathTransform2D inverse() {
        return this;
    }
}