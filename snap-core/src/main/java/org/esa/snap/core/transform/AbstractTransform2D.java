package org.esa.snap.core.transform;

import org.geotools.referencing.operation.transform.AbstractMathTransform;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.opengis.referencing.operation.TransformException;

import java.awt.geom.Point2D;

/**
 * An abstract transform using {@code org.geotools.referencing.operation.transform.AbstractMathTransform}
 * that can be used to implement {@code MathTransform2D}.
 *
 * @author Tonio Fincke
 */
public abstract class AbstractTransform2D extends AbstractMathTransform implements MathTransform2D {

    @Override
    public void transform(double[] srcPts, int srcOff, double[] dstPts, int dstOff, int numPts) throws TransformException {
        final Point2D.Double source = new Point2D.Double();
        final Point2D.Double target = new Point2D.Double();
        boolean throwTransformException = false;
        String message = null;
        for (int i = 0; i < numPts; i++) {
            final int srcXIndex = srcOff + (2 * i);
            final int srcYIndex = srcOff + (2 * i) + 1;
            final int dstXIndex = dstOff + (2 * i);
            final int dstYIndex = dstOff + (2 * i) + 1;
            source.setLocation(srcPts[srcXIndex], srcPts[srcYIndex]);
            try {
                transform(source, target);
                dstPts[dstXIndex] = target.getX();
                dstPts[dstYIndex] = target.getY();
            } catch (TransformException te) {
                throwTransformException = true;
                message = te.getMessage();
                dstPts[dstXIndex] = Double.NaN;
                dstPts[dstYIndex] = Double.NaN;
            }
        }
        if (throwTransformException) {
            TransformException transformException;
            if (message != null) {
                transformException = new TransformException("Could not transform point: " + message);
            } else {
                transformException = new TransformException("Could not transform point");
            }
            transformException.setLastCompletedTransform(this);
            throw transformException;
        }
    }

    @Override
    public abstract Point2D transform(Point2D ptSrc, Point2D ptDst) throws TransformException;

    @Override
    public int getSourceDimensions() {
        return 2;
    }

    @Override
    public int getTargetDimensions() {
        return 2;
    }

    @Override
    public abstract MathTransform2D inverse() throws NoninvertibleTransformException;

    @Override
    public abstract boolean equals(Object object);

    @Override
    public abstract int hashCode();
}
