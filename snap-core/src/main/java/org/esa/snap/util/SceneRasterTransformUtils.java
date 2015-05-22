package org.esa.snap.util;

import org.esa.snap.framework.datamodel.PixelPos;
import org.esa.snap.framework.datamodel.SceneRasterTransform;
import org.esa.snap.framework.datamodel.SceneRasterTransformException;
import org.opengis.referencing.operation.MathTransform2D;
import org.opengis.referencing.operation.TransformException;

import java.awt.Shape;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;

/**
 * @author Tonio Fincke
 */
public class SceneRasterTransformUtils {

    public static Shape transformShapeToProductCoordinates(Shape shape, SceneRasterTransform sceneRasterTransform) throws SceneRasterTransformException {
        if (sceneRasterTransform == SceneRasterTransform.IDENTITY) {
            return shape;
        }
        final MathTransform2D forward = sceneRasterTransform.getForward();
        if (forward == null) {
            throw new SceneRasterTransformException("Cannot transform: No inverse transformation provided");
        }
        return transformShape(shape, forward);
    }

    public static Shape transformShapeToRasterCoordinates(Shape shape, SceneRasterTransform sceneRasterTransform) throws SceneRasterTransformException {
        if (sceneRasterTransform == SceneRasterTransform.IDENTITY) {
            return shape;
        }
        final MathTransform2D inverse = sceneRasterTransform.getInverse();
        if (inverse == null) {
            throw new SceneRasterTransformException("Cannot inverse: No inverse transformation provided");
        }
        return transformShape(shape, inverse);
    }

    private static Shape transformShape(Shape shape, MathTransform2D transformation) throws SceneRasterTransformException {
        try {
            if (shape instanceof Path2D.Double) {
                Path2D.Double origPath = (Path2D.Double) shape;
                Path2D.Double newPath = new Path2D.Double(origPath.getWindingRule());
                final PathIterator origPathIterator = origPath.getPathIterator(null);
                while (!origPathIterator.isDone()) {
                    double[] coords = new double[6];
                    final int segmentType = origPathIterator.currentSegment(coords);
                    if (segmentType == PathIterator.SEG_MOVETO) {
                        PixelPos moveTo = new PixelPos();
                        transformation.transform(new PixelPos(coords[0], coords[1]), moveTo);
                        newPath.moveTo(moveTo.getX(), moveTo.getY());
                    } else if (segmentType == PathIterator.SEG_LINETO) {
                        PixelPos lineTo = new PixelPos();
                        transformation.transform(new PixelPos(coords[0], coords[1]), lineTo);
                        newPath.lineTo(lineTo.getX(), lineTo.getY());
                    } else if (segmentType == PathIterator.SEG_QUADTO) {
                        PixelPos quadTo1 = new PixelPos();
                        PixelPos quadTo2 = new PixelPos();
                        transformation.transform(new PixelPos(coords[0], coords[1]), quadTo1);
                        transformation.transform(new PixelPos(coords[2], coords[3]), quadTo2);
                        newPath.quadTo(quadTo1.getX(), quadTo1.getY(), quadTo2.getX(), quadTo2.getY());
                    } else if (segmentType == PathIterator.SEG_CUBICTO) {
                        PixelPos cubicTo1 = new PixelPos();
                        PixelPos cubicTo2 = new PixelPos();
                        PixelPos cubicTo3 = new PixelPos();
                        transformation.transform(new PixelPos(coords[0], coords[1]), cubicTo1);
                        transformation.transform(new PixelPos(coords[2], coords[3]), cubicTo2);
                        transformation.transform(new PixelPos(coords[4], coords[5]), cubicTo3);
                        newPath.curveTo(cubicTo1.getX(), cubicTo1.getY(), cubicTo2.getX(), cubicTo2.getY(),
                                        cubicTo3.getX(), cubicTo3.getY());
                    } else if (segmentType == PathIterator.SEG_CLOSE) {
                        newPath.closePath();
                    }
                    origPathIterator.next();
                }
                return newPath;
            } else {
                return transformation.createTransformedShape(shape);
            }
        } catch (TransformException e) {
            throw new SceneRasterTransformException(e.getMessage(), e);
        }
    }

}
