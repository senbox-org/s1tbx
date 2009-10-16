package org.esa.beam.framework.datamodel;

import com.bc.ceres.binding.ValueContainer;
import com.bc.ceres.binding.ValueDescriptor;
import com.bc.ceres.binding.ValueModel;
import com.bc.ceres.binding.accessors.DefaultValueAccessor;
import com.bc.ceres.core.Assert;
import com.bc.ceres.glevel.MultiLevelImage;
import org.esa.beam.jai.ImageManager;

import java.awt.Color;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

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
    private final PropertyChangeListener imageConfigListener;
    private volatile ValueContainer imageConfig;

    /**
     * Constructs a new mask.
     *
     * @param name      The new mask's name.
     * @param width     The new mask's raster width.
     * @param height    The new mask's raster height.
     * @param imageType The new mask's image type.
     */
    public Mask(String name, int width, int height, ImageType imageType) {
        super(name, ProductData.TYPE_INT8, width, height);
        Assert.notNull(imageType, "imageType");
        this.imageType = imageType;
        this.imageConfigListener = new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                fireProductNodeChanged(evt.getPropertyName(), evt.getOldValue());
            }
        };
        this.imageConfig = imageType.createImageConfig();
        this.imageConfig.addPropertyChangeListener(imageConfigListener);
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

    public Color getImageColor() {
        return (Color) imageConfig.getValue(ImageType.PROPERTY_NAME_COLOR);
    }

    public void setImageColor(Color color) {
        imageConfig.setValue(ImageType.PROPERTY_NAME_COLOR, color);
    }

    public float getImageTransparency() {
        return (Float) imageConfig.getValue(ImageType.PROPERTY_NAME_TRANSPARENCY);
    }

    public void setImageTransparency(float transparency) {
        imageConfig.setValue(ImageType.PROPERTY_NAME_TRANSPARENCY, transparency);
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

    @Override
    public void dispose() {
        imageConfig.removePropertyChangeListener(imageConfigListener);
        super.dispose();
    }

    private static void setImageStyle(ValueContainer imageConfig, Color color, float transparency) {
        imageConfig.setValue(ImageType.PROPERTY_NAME_COLOR, color);
        imageConfig.setValue(ImageType.PROPERTY_NAME_TRANSPARENCY, transparency);
    }

    /**
     * Specifies a factory for the {@link RasterDataNode#getSourceImage() source image} used by a {@link Mask}.
     */
    public static abstract class ImageType {
        public static final String PROPERTY_NAME_COLOR = "color";
        public static final String PROPERTY_NAME_TRANSPARENCY = "transparency";
        private static final Color DEFAULT_COLOR = Color.RED;
        private static final float DEFAULT_TRANSPARENCY = 0.5f;

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

            ValueDescriptor colorType = new ValueDescriptor(PROPERTY_NAME_COLOR, Color.class);
            colorType.setNotNull(true);
            colorType.setDefaultValue(DEFAULT_COLOR);

            ValueDescriptor transparencyType = new ValueDescriptor(PROPERTY_NAME_TRANSPARENCY, Float.TYPE);
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
        public static final String PROPERTY_NAME_EXPRESSION = "expression";

        /**
         * Creates the image.
         *
         * @param mask The mask which requests creation of its image.
         *
         * @return The image.
         */
        @Override
        public MultiLevelImage createImage(Mask mask) {
            String expression = (String) mask.getImageConfig().getValue(PROPERTY_NAME_EXPRESSION);
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

            ValueDescriptor expressionDescriptor = new ValueDescriptor(PROPERTY_NAME_EXPRESSION, String.class);
            expressionDescriptor.setNotNull(true);
            expressionDescriptor.setNotEmpty(true);

            ValueContainer imageConfig = super.createImageConfig();
            imageConfig.addModel(new ValueModel(expressionDescriptor, new DefaultValueAccessor()));

            return imageConfig;
        }
    }
}
