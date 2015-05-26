package org.esa.snap.util;

import org.esa.snap.framework.datamodel.PixelPos;
import org.esa.snap.framework.datamodel.Product;
import org.esa.snap.framework.datamodel.RasterDataNode;
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

    /**
     * Converts a {@link PixelPos} from raster coordinates of a {@link RasterDataNode}
     * to raster coordinates of the {@link Product}
     *
     * @param rasterDataNode The {@link RasterDataNode} from which to transform
     * @param orig The {@link PixelPos} in raster coordinates of a {@link RasterDataNode}
     * @return A {@link PixelPos} in raster coordinates of the {@link Product}
     * @throws SceneRasterTransformException When the transformation could not be applied
     */
    public static PixelPos transformToProductRaster(RasterDataNode rasterDataNode, PixelPos orig) throws SceneRasterTransformException {
        final SceneRasterTransform sceneRasterTransform = rasterDataNode.getSceneRasterTransform();
        if(sceneRasterTransform == SceneRasterTransform.IDENTITY) {
            return orig;
        }
        final PixelPos target = new PixelPos();
        final MathTransform2D forward = sceneRasterTransform.getForward();
        if(forward == null) {
            throw new SceneRasterTransformException("Cannot transform: No forward transformation provided");
        }
        try {
            forward.transform(orig, target);
        } catch (TransformException e) {
            throw new SceneRasterTransformException(e.getMessage(), e);
        }
        return target;
    }

    /**
     * Converts a {@link PixelPos} from raster coordinates of the {@link Product}
     * to raster coordinates of a {@link RasterDataNode}
     *
     * @param rasterDataNode The {@link RasterDataNode} to which to transform
     * @param orig The {@link PixelPos} in raster coordinates of the {@link Product}
     * @return A {@link PixelPos} in raster coordinates of the {@link RasterDataNode}
     * @throws SceneRasterTransformException When the transformation could not be applied
     */
    public static PixelPos transformToRasterDataNodeRaster(RasterDataNode rasterDataNode, PixelPos orig) throws SceneRasterTransformException {
        final SceneRasterTransform sceneRasterTransform = rasterDataNode.getSceneRasterTransform();
        if(sceneRasterTransform == SceneRasterTransform.IDENTITY) {
            return orig;
        }
        final PixelPos target = new PixelPos();
        final MathTransform2D inverse = sceneRasterTransform.getInverse();
        if(inverse == null) {
            throw new SceneRasterTransformException("Cannot transform: No inverse transformation provided");
        }
        try {
            inverse.transform(orig, target);
        } catch (TransformException e) {
            throw new SceneRasterTransformException(e.getMessage(), e);
        }
        return target;
    }

    /**
     * Converts a {@link PixelPos} from coordinates of one {@link RasterDataNode} raster
     * to coordinates of another {@link RasterDataNode} raster
     *
     * @param from The {@link RasterDataNode} from which to transform
     * @param to The {@link RasterDataNode} to which to transform
     * @param orig The {@link PixelPos} in raster coords of the from {@link RasterDataNode}
     * @return A {@link PixelPos} in raster coords of the to {@link RasterDataNode}
     * @throws SceneRasterTransformException When the transformation could not be applied
     */
    public static PixelPos transformFromToRasterDataNodeRaster(RasterDataNode from, RasterDataNode to, PixelPos orig) throws SceneRasterTransformException {
        return transformToRasterDataNodeRaster(to, transformToProductRaster(from, orig));
    }

    /**
     * Converts a {@link Shape} from {@link RasterDataNode} raster coordinates to {@link Product} raster coordinates
     *
     * @param rasterDataNode The {@link RasterDataNode} from which to transform
     * @param orig The {@link Shape} in {@link RasterDataNode} raster coordinates
     * @return A {@link Shape} in {@link Product} raster coordinates
     * @throws SceneRasterTransformException When the transformation could not be applied
     */
    public static Shape transformToProductRaster(RasterDataNode rasterDataNode, Shape orig) throws SceneRasterTransformException {
        return transformShapeToProductCoordinates(orig, rasterDataNode.getSceneRasterTransform());
    }

    /**
     * Converts a {@link Shape} from {@link Product} raster coordinates to {@link RasterDataNode} raster coordinates
     *
     * @param rasterDataNode The {@link RasterDataNode} to which to transform
     * @param orig The {@link Shape} in {@link Product} raster coordinates
     * @return A {@link Shape} in {@link RasterDataNode} raster coordinates
     * @throws SceneRasterTransformException When the transformation could not be applied
     */
    public static Shape transformToRasterDataNodeRaster(RasterDataNode rasterDataNode, Shape orig) throws SceneRasterTransformException {
        return transformShapeToRasterCoordinates(orig, rasterDataNode.getSceneRasterTransform());
    }

    /**
     * Converts a {@link Shape} from coordinates of one {@link RasterDataNode} raster to coordinates of another {@link RasterDataNode} raster
     *
     * @param from The {@link RasterDataNode} from which to transform
     * @param to The {@link RasterDataNode} to which to transform
     * @param orig The {@link Shape} in raster coordinates of the from {@link RasterDataNode}
     * @return A {@link Shape} in raster coordinates of the to {@link RasterDataNode}
     * @throws SceneRasterTransformException When the transformation could not be applied
     */
    public static Shape transformFromToRasterDataNodeRaster(RasterDataNode from, RasterDataNode to, Shape orig) throws SceneRasterTransformException {
        return transformToRasterDataNodeRaster(to, transformToProductRaster(from, orig));
    }

    /**
     * Converts a {@link Shape} from {@link RasterDataNode} raster coordinates to {@link Product} raster coordinates
     *
     * @param sceneRasterTransform The {@link SceneRasterTransform} to be used to transform
     * @param shape The {@link Shape} in {@link RasterDataNode} raster coordinates
     * @return A {@link Shape} in {@link Product} raster coordinates
     * @throws SceneRasterTransformException when the transformation could not be applied
     */
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

    /**
     * Converts a {@link Shape} from {@link Product} raster coordinates to {@link RasterDataNode} raster coordinates
     *
     * @param sceneRasterTransform The {@link SceneRasterTransform} to be used to transform
     * @param shape The {@link Shape} in {@link Product} raster coordinates
     * @return A {@link Shape} in {@link RasterDataNode} raster coordinates
     * @throws SceneRasterTransformException when the transformation could not be applied
     */
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
