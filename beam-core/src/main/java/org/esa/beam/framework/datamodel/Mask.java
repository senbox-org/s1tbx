package org.esa.beam.framework.datamodel;

import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.ValueContainer;
import com.bc.ceres.binding.ValueDescriptor;
import com.bc.ceres.binding.ValueModel;
import com.bc.ceres.binding.accessors.DefaultValueAccessor;
import com.bc.ceres.core.Assert;
import com.bc.ceres.glevel.MultiLevelImage;

import java.awt.Color;
import java.awt.image.RenderedImage;
import java.awt.image.DataBuffer;

import org.esa.beam.jai.ImageManager;

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
    private static final Color DEFAULT_COLOR = Color.RED;
    private static final float DEFAULT_TRANSPARENCY = 0.5f;

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
     * Constructs a new mask.
     *
     * @param name         The new mask's name.
     * @param width        The new mask's raster width.
     * @param height       The new mask's raster height.
     * @param imageType    The new mask's image type.
     * @param color        The new mask's image color.
     * @param transparency The new mask's image transparency.
     */
    public Mask(String name,
                int width, int height,
                ImageType imageType,
                Color color,
                float transparency) {
        super(name, ProductData.TYPE_INT8, width, height);
        Assert.notNull(imageType, "type");
        this.imageType = imageType;
        this.imageConfig = imageType.createImageConfig();
        setImageStyle(this.imageConfig, color, transparency);
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
        if (isMaskImageValid(image)) {
            throw new IllegalStateException("Invalid mask image.");
        }
        return image;
    }

    private boolean isMaskImageValid(MultiLevelImage image) {
        return image.getSampleModel().getDataType() != DataBuffer.TYPE_BYTE
                || image.getNumBands() != 1
                || image.getWidth() != getSceneRasterWidth()
                || image.getHeight() != getSceneRasterHeight();
    }

    @Override
    public void acceptVisitor(ProductVisitor visitor) {
        visitor.visit(this);
    }

    private static void setImageStyle(ValueContainer imageConfig, Color color, float transparency) {
        try {
            imageConfig.setValue("color", color);
            imageConfig.setValue("transparency", transparency);
        } catch (ValidationException e) {
            throw new IllegalStateException(e);
        }
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
         * Creates a prototype image configuration.
         *
         * @return The image configuration.
         */
        @SuppressWarnings({"MethodMayBeStatic"})
        public ValueContainer createImageConfig() {

            ValueDescriptor colorType = new ValueDescriptor("color", Color.class);
            colorType.setNotNull(true);
            colorType.setDefaultValue(DEFAULT_COLOR);

            ValueDescriptor transparencyType = new ValueDescriptor("transparency", Float.TYPE);
            transparencyType.setDefaultValue(DEFAULT_TRANSPARENCY);

            ValueContainer imageConfig = new ValueContainer();
            imageConfig.addModel(new ValueModel(colorType, new DefaultValueAccessor()));
            imageConfig.addModel(new ValueModel(transparencyType, new DefaultValueAccessor()));

            setImageStyle(imageConfig, DEFAULT_COLOR, DEFAULT_TRANSPARENCY);

            return imageConfig;
        }
    }

    /**
     * A mask image type which is based on band math.
     */
    public static class BandMathImageType extends ImageType {
        /**
         * Creates the image.
         *
         * @param mask The mask which requests creation of its image.
         *
         * @return The image.
         */
        @Override
        public MultiLevelImage createImage(Mask mask) {
            String expression = (String) mask.getImageConfig().getValue("expression");
            // todo - this is not the preferred way to create mask images (nf, 10.2009)
            return ImageManager.getInstance().getMaskImage(expression, mask.getProduct());
        }

        /**
         * Creates a prototype image configuration.
         *
         * @return The image configuration.
         */
        @Override
        public ValueContainer createImageConfig() {

            ValueDescriptor expressionDescriptor = new ValueDescriptor("expression", String.class);
            expressionDescriptor.setNotNull(true);
            expressionDescriptor.setNotEmpty(true);

            ValueContainer imageConfig = super.createImageConfig();
            imageConfig.addModel(new ValueModel(expressionDescriptor, new DefaultValueAccessor()));

            return imageConfig;
        }
    }
}
