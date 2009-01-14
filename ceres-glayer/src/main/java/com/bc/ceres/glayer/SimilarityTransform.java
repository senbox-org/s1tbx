/* 
 * Copyright (C) 2002-2008 by Brockmann Consult
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package com.bc.ceres.glayer;

import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;

/**
 * Similarity transform.
 * <p/>
 * A similarity transform is an {@link AffineTransform} which is
 * composed of scalings, rotations and translations only, so the
 * shape of geometric objects is preserved.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public class SimilarityTransform extends AffineTransform {

    public SimilarityTransform() {
        super();
    }

    public SimilarityTransform(AffineTransform Tx) {
        super(Tx);
        assertSimilarityTransform(Tx);
    }

    public SimilarityTransform(float m00, float m10, float m01, float m11, float m02, float m12) {
        super(m00, m10, m01, m11, m02, m12);
        assertSimilarityTransform(m00, m10, m01, m11);
    }

    public SimilarityTransform(float[] flatmatrix) {
        super(flatmatrix);
        assertSimilarityTransform(this);
    }

    public SimilarityTransform(double m00, double m10, double m01, double m11, double m02, double m12) {
        super(m00, m10, m01, m11, m02, m12);
        assertSimilarityTransform(m00, m10, m01, m11);
    }

    public SimilarityTransform(double[] flatmatrix) {
        super(flatmatrix);
        assertSimilarityTransform(this);
    }

    @Override
    public final void shear(double shx, double shy) {
        if (shx != 0.0 || shy != 0.0) {
            throw new IllegalArgumentException("shx != 0.0 || shy != 0.0");
        }
        super.shear(shx, shy);
    }

    @Override
    public final void setTransform(AffineTransform Tx) {
        assertSimilarityTransform(Tx);
        super.setTransform(Tx);
    }

    public final void setTransform(SimilarityTransform Tx) {
        super.setTransform(Tx);
    }

    public final double getScale() {
        return Math.sqrt(getDeterminant());
    }

    public final double getRotationAngle() {
        return Math.asin(getShearY());
    }

    @Override
    public final void scale(double sx, double sy) {
        if (sx != sy) {
            throw new IllegalArgumentException("sx != sy");
        }
        super.scale(sx, sy);
    }

    public final void scale(double s) {
        super.scale(s, s);
    }

    @Override
    public final void setToScale(double sx, double sy) {
        if (sx != sy || sx <= 0.0) {
            throw new IllegalArgumentException("sx != sy || sy <= 0.0");
        }
        super.setToScale(sx, sy);
    }

    public final void setToScale(double s) {
        if (s <= 0.0) {
            throw new IllegalArgumentException("s <= 0.0");
        }
        super.setToScale(s, s);
    }

    @Override
    public final void setToShear(double shx, double shy) {
        if (shx != 0.0 || shy != 0.0) {
            throw new IllegalArgumentException("shx != 0.0 || shy != 0.0");
        }
        super.setToShear(shx, shy);
    }

    @Override
    public final void setTransform(double m00, double m10, double m01, double m11, double m02, double m12) {
        assertSimilarityTransform(m00, m10, m01, m11);
        super.setTransform(m00, m10, m01, m11, m02, m12);
    }

    @Override
    public final void concatenate(AffineTransform Tx) {
        assertSimilarityTransform(Tx);
        super.concatenate(Tx);
    }

    public final void concatenate(SimilarityTransform Tx) {
        super.concatenate(Tx);
    }

    @Override
    public final void preConcatenate(AffineTransform Tx) {
        assertSimilarityTransform(Tx);
        super.preConcatenate(Tx);
    }

    public final void preConcatenate(SimilarityTransform Tx) {
        super.preConcatenate(Tx);
    }

    @Override
    public final SimilarityTransform createInverse() throws NoninvertibleTransformException {
        return (SimilarityTransform) super.createInverse();
    }

    /**
     * Tests whether an affine transform is a similarity transform.
     *
     * @param affineTransform the affine transform.
     *
     * @return {@code true}, if the the affine transform is a similarity transform.
     */
    public static boolean isSimilarityTransform(AffineTransform affineTransform) {
        return isSimilarityTransform(affineTransform.getScaleX(), affineTransform.getShearY(),
                affineTransform.getShearX(), affineTransform.getScaleY());
    }

    private static boolean isSimilarityTransform(double m00, double m10, double m01, double m11) {
        return m00 == m11 && m10 == -m01; // todo - further constraints
    }

    private static void assertSimilarityTransform(AffineTransform affineTransform) {
        if (!isSimilarityTransform(affineTransform)) {
            throw new IllegalArgumentException("affine transform is not a similarity transform");
        }
    }

    private static void assertSimilarityTransform(double m00, double m10, double m01, double m11) {
        if (!isSimilarityTransform(m00, m10, m01, m11)) {
            throw new IllegalArgumentException("parameters do not represent a similarity transform");
        }
    }
}
