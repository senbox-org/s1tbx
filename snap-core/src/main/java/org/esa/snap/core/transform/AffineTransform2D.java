package org.esa.snap.core.transform;

import java.awt.geom.AffineTransform;

/**
 * A wrapper class for an affine transform where a {@code MathTransform2D} is required.
 *
 * @author Tonio Fincke
 */
public class AffineTransform2D extends org.geotools.referencing.operation.transform.AffineTransform2D implements MathTransform2D {

    private final AffineTransform transform;
    private AffineTransform2D inverse;

    /**
     * Constructs a new affine transform with the same coefficient than the specified transform.
     */
    public AffineTransform2D(AffineTransform transform) {
        super(transform);
        this.transform = transform;
    }

    /**
     * Constructs a new {@code AffineTransform2D} from 6 values representing the 6 specifiable
     * entries of the 3&times;3 transformation matrix. Those values are given unchanged to the
     * {@link AffineTransform#AffineTransform(double, double, double, double, double, double) super
     * class constructor}.
     *
     * @since 2.5
     */
    public AffineTransform2D(double m00, double m10, double m01, double m11, double m02, double m12) {
        super(m00, m10, m01, m11, m02, m12);
        transform = new AffineTransform(m00, m10, m01, m11, m02, m12);
    }

    /**
     * Creates the inverse transform of this object.
     *
     * @throws org.opengis.referencing.operation.NoninvertibleTransformException if this transform can't be inverted.
     */
    @Override
    public MathTransform2D inverse() throws org.opengis.referencing.operation.NoninvertibleTransformException {
        if (inverse == null) {
            try {
                inverse = new AffineTransform2D(transform.createInverse());
            } catch (java.awt.geom.NoninvertibleTransformException e) {
                throw new org.opengis.referencing.operation.NoninvertibleTransformException(e.getMessage());
            }
        }
        return inverse;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof AffineTransform2D)) {
            return false;
        }
        return ((AffineTransform2D) object).transform.equals(transform);
    }
}
