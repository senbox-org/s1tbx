package org.esa.snap.core.datamodel;

import org.esa.snap.core.transform.MathTransform2D;

/**
 * Provides various transformations from local {@link RasterDataNode} coordinate reference systems
 * to a {@link Product}'s scene coordinate reference systems.
 *
 * @author Tonio Fincke
 * @author Norman Fomferra
 */
public interface SceneTransformProvider {

    /**
     * Gets the transformation that transforms from local {@link RasterDataNode} model coordinates
     * to the {@link Product}'s scene coordinates.
     *
     * @return The model-to-scene transformation, or {@code null} if no such exists.
     * @see Product#getSceneCRS()
     * @see RasterDataNode#getImageToModelTransform()
     */
    MathTransform2D getModelToSceneTransform();

    /**
     * Gets the transformation that transforms from the {@link Product}'s scene coordinates
     * to the local {@link RasterDataNode} model coordinates.
     *
     * @return The model-to-scene transformation, or {@code null} if no such exists.
     * @see Product#getSceneCRS()
     * @see RasterDataNode#getImageToModelTransform()
     */
    MathTransform2D getSceneToModelTransform();

    // Later?
    //MathTransform2D getImageToSceneTransform();
    //MathTransform2D getSceneToImageTransform();
}
