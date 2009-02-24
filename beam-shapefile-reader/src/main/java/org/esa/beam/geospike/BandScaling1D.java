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
package org.esa.beam.geospike;

import org.esa.beam.framework.datamodel.Band;
import org.geotools.geometry.DirectPosition1D;
import org.geotools.referencing.operation.matrix.Matrix1;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.operation.MathTransform1D;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.opengis.referencing.operation.TransformException;

/**
 * Abstract utility class for defining a {@link org.geotools.coverage.Category}
 * with a sample-to-geophysical transform for scaled {@link Band}s.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.6
 */
abstract class BandScaling1D implements MathTransform1D {
    private final Band band;

    protected BandScaling1D(Band band) {
        this.band = band;
    }

    protected final Band getBand() {
        return band;
    }

    @Override
    public abstract double transform(double value) throws TransformException;

    @Override
    public abstract double derivative(double value) throws TransformException;

    @Override
    public abstract MathTransform1D inverse() throws NoninvertibleTransformException;

    @Override
    public final int getSourceDimensions() {
        return 1;
    }

    @Override
    public final int getTargetDimensions() {
        return 1;
    }

    @Override
    public final DirectPosition transform(DirectPosition ptSrc, DirectPosition ptDst) throws MismatchedDimensionException, TransformException {
        if (ptDst == null) {
            ptDst = new DirectPosition1D();
        }
        if (ptSrc.getDimension() != getSourceDimensions()) {
            throw new MismatchedDimensionException("ptSrc.getDimension() != getSourceDimensions()");
        }
        if (ptDst.getDimension() != getTargetDimensions()) {
            throw new MismatchedDimensionException("ptDst.getDimension() != getTargetDimensions()");
        }
        ptDst.setOrdinate(0, transform(ptSrc.getOrdinate(0)));

        return ptDst;
    }

    @Override
    public final void transform(double[] srcPts, int srcOff, double[] dstPts, int dstOff, int numPts) throws TransformException {
        for (int i = 0; i < numPts; i++) {
            dstPts[dstOff + i] = transform(srcPts[srcOff + i]);
        }
    }

    @Override
    public final void transform(float[] srcPts, int srcOff, float[] dstPts, int dstOff, int numPts) throws TransformException {
        for (int i = 0; i < numPts; i++) {
            dstPts[dstOff + i] = (float) transform(srcPts[srcOff + i]);
        }
    }

    @Override
    public final Matrix derivative(DirectPosition point) throws MismatchedDimensionException, TransformException {
        if (point.getDimension() != getSourceDimensions()) {
            throw new MismatchedDimensionException("point.getDimension() != getSourceDimensions()");
        }

        return new Matrix1(derivative(point.getOrdinate(0)));
    }

    @Override
    public final boolean isIdentity() {
        return !band.isScalingApplied();
    }

    @Override
    public String toWKT() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }
}
