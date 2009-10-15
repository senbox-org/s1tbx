package org.esa.beam.framework.datamodel;

import com.bc.ceres.binding.ValueContainer;
import com.bc.ceres.core.Assert;
import com.bc.ceres.glevel.MultiLevelImage;

import java.awt.image.RenderedImage;

/**
 * A {@code Mask} is used to mask image pixels of other raster data nodes.
 * <p/>
 * This is a preliminary API under construction for BEAM 4.7. Not intended for public use.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 * @since BEAM 4.7
 */
public class Mask extends Band {
    private final ImageType imageType;
    private volatile ValueContainer imageConfig;

    /**
     * Constructs a new mask.
     *
     * @param name        The new mask's name.
     * @param width       The new mask's raster width.
     * @param height      The new mask's raster height.
     * @param imageType   The new mask's image type.
     * @param imageConfig The new mask's image configuration.
     */
    public Mask(String name,
                int width, int height,
                ImageType imageType,
                ValueContainer imageConfig) {
        super(name, ProductData.TYPE_INT8, width, height);
        Assert.notNull(imageType, "type");
        this.imageType = imageType;
        this.imageConfig = imageConfig;
    }

    /**
     * @return The image type of this mask.
     */
    public ImageType getImageType() {
        return imageType;
    }

    /**
     * @return The image configuration of this mask.
     */
    public ValueContainer getImageConfig() {
        if (imageConfig == null) {
            synchronized (this) {
                if (imageConfig == null) {
                    imageConfig = imageType.createImageConfig();
                }
            }
        }
        return imageConfig;
    }

    /**
     * Calls {@link ImageType#createImage(Mask) createImage(this)} in this mask's image type.
     *
     * @return The mask's source image.
     *
     * @see #getImageType()
     */
    @Override
    protected RenderedImage createSourceImage() {
        final MultiLevelImage image = getImageType().createImage(this);
        // todo - check image type and size (nf, 10.2009)
        return image;
    }

    @Override
    public void acceptVisitor(ProductVisitor visitor) {
        visitor.visit(this);
    }

    /**
     * Specifies a factory for the {@link RasterDataNode#getSourceImage() source image} used by a {@link Mask}.
     */
    public static abstract class ImageType {

        /**
         * Creates the image.
         *
         * @param mask The mask which requests creation of its image.
         *
         * @return The image.
         */
        public abstract MultiLevelImage createImage(Mask mask);

        /**
         * Creates an (default) image configuration.
         *
         * @return The image configuration.
         */
        public abstract ValueContainer createImageConfig();
    }
}
