package com.bc.ceres.glevel;

import com.bc.ceres.glevel.support.NullLayerImage;

import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.RenderedImage;

/**
 * An image comprising multiple resolution levels.
 */
public interface LayerImage {
    LayerImage NULL = NullLayerImage.INSTANCE;

    /**
     * Gets the maximum number of resolution levels.
     *
     * @return the number of resolution levels
     */
    int getLevelCount();

    /**
     * Computes the resolution level from the given scaling factor. The scaling factor is the number of units in
     * the model CS per unit in the view CS.
     *
     * @param scale the model-to-view scaling factor.
     * @return the resolution level.
     */
    int computeLevel(double scale);

    /**
     * Computes the scaling factor from the given resolution level. The scaling factor is the number of units in
     * the model CS per unit in the view CS.
     * 
     * @param level The resolution level.
     * @return the model-to-view scaling factor.
     */
    double computeScale(int level);
    
    /**
     * Gets the image for the given level.
     *
     * @param level the resolution level.
     * @return the image for the given level.
     * @see #reset()
     */
    RenderedImage getLevelImage(int level);

    /**
     * States an attempt to regenerate images at all levels and remove all cached tiles.
     * After calling this method, {@link #getLevelImage(int)} should return a newly created image the first
     * time it is called.
     * <p/>
     * This method is particularly useful if properties have changed that affect the appearance of the
     * returned images at all levels, e.g. after a new color palette has been assigned or the
     * contrast range has changed.
     */
    void reset();

    /**
     * Gets a copy (non-life object) of the affine transformation from image to model coordinates for the given level.
     *
     * @param level the resolution level
     * @return the affine transformation from image to model coordinates.
     */
    AffineTransform getImageToModelTransform(int level);

    /**
     * Gets a copy (non-life object) of the affine transformation from model to image coordinates for the given level.
     *
     * @param level the resolution level
     * @return the affine transformation from model to image coordinates.
     */
    AffineTransform getModelToImageTransform(int level);

    /**
     * Returns the bounding box in model coordinates.
     *
     * @return the bounding box.
     */
    Rectangle2D getModelBounds();


}
