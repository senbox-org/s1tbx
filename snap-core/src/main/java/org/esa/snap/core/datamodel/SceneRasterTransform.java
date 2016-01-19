package org.esa.snap.core.datamodel;

import org.esa.snap.core.transform.MathTransform2D;

/**
 * Allows a {@link RasterDataNode}’s native raster data to be spatially transformed from its
 * model CRS into the product’s scene CRS and vice versa. It comprises a forward and inverse 2D coordinate
 * Transformation. It is legal to have {@code SceneRasterTransform} instances whose either forward or inverse
 * pixel coordinate Transformations are not available.
 *
 * @author Norman Fomferra
 * @see RasterDataNode#getSceneRasterTransform()
 * @see RasterDataNode#setSceneRasterTransform(SceneRasterTransform)
 */
public interface SceneRasterTransform {

    /**
     * The identity transform.
     */
    SceneRasterTransform IDENTITY = new SceneRasterTransform() {

        @Override
        public MathTransform2D getForward() {
            return MathTransform2D.IDENTITY;
        }

        @Override
        public MathTransform2D getInverse() {
            return MathTransform2D.IDENTITY;
        }
    };

    /**
     * The forward transformation which transforms raster pixel coordinates into product scene raster pixel coordinates.
     *
     * @return the forward pixel coordinate transformation, or {@code null} if no such exists
     */
    MathTransform2D getForward();

    /**
     * The inverse transformation which transforms product scene raster pixel coordinates into raster pixel coordinates.
     *
     * @return the inverse pixel coordinate transformation, or {@code null} if no such exists
     */
    MathTransform2D getInverse();
}
