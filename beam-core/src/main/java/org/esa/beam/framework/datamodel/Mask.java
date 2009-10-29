package org.esa.beam.framework.datamodel;

import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.accessors.DefaultPropertyAccessor;
import com.bc.ceres.core.Assert;
import com.bc.ceres.glevel.MultiLevelImage;
import org.esa.beam.jai.ImageManager;
import org.esa.beam.util.StringUtils;

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
    private volatile PropertyContainer imageConfig;

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
    public PropertyContainer getImageConfig() {
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

    public double getImageTransparency() {
        return (Double) imageConfig.getValue(ImageType.PROPERTY_NAME_TRANSPARENCY);
    }

    public void setImageTransparency(double transparency) {
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

    private static void setImageStyle(PropertyContainer imageConfig, Color color, double transparency) {
        imageConfig.setValue(ImageType.PROPERTY_NAME_COLOR, color);
        imageConfig.setValue(ImageType.PROPERTY_NAME_TRANSPARENCY, transparency);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateExpression(final String oldExternalName, final String newExternalName) {
        getImageType().handleRename(this, oldExternalName, newExternalName);
        super.updateExpression(oldExternalName, newExternalName);
    }

    /**
     * Specifies a factory for the {@link RasterDataNode#getSourceImage() source image} used by a {@link Mask}.
     */
    public static abstract class ImageType {
        public static final String PROPERTY_NAME_COLOR = "color";
        public static final String PROPERTY_NAME_TRANSPARENCY = "transparency";
        private static final Color DEFAULT_COLOR = Color.RED;
        private static final double DEFAULT_TRANSPARENCY = 0.5;
        private final String name;

        protected ImageType(String name) {
            this.name = name;
        }

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
        public PropertyContainer createImageConfig() {

            PropertyDescriptor colorType = new PropertyDescriptor(PROPERTY_NAME_COLOR, Color.class);
            colorType.setNotNull(true);
            colorType.setDefaultValue(DEFAULT_COLOR);

            PropertyDescriptor transparencyType = new PropertyDescriptor(PROPERTY_NAME_TRANSPARENCY, Double.TYPE);
            transparencyType.setDefaultValue(DEFAULT_TRANSPARENCY);

            PropertyContainer imageConfig = new PropertyContainer();
            imageConfig.addProperty(new Property(colorType, new DefaultPropertyAccessor()));
            imageConfig.addProperty(new Property(transparencyType, new DefaultPropertyAccessor()));

            setImageStyle(imageConfig, DEFAULT_COLOR, DEFAULT_TRANSPARENCY);

            return imageConfig;
        }

        public void handleRename(Mask mask, String oldExternalName, String newExternalName) {
        }

        public  String getName() {
            return name;
        }
    }

    /**
     * A mask image type which is based on band math.
     */
    public static class BandMathType extends ImageType {
        public static final String PROPERTY_NAME_EXPRESSION = "expression";

        public BandMathType() {
            super("Band math");
        }

        /**
         * Creates the image.
         *
         * @param mask The mask which requests creation of its image.
         *
         * @return The image.
         */
        @Override
        public MultiLevelImage createImage(Mask mask) {
            String expression = getExpression(mask);
            // todo - this is not the preferred way to create mask images (nf, 10.2009)
            return ImageManager.getInstance().getMaskImage(expression, mask.getProduct());
        }

        /**
         * Creates a prototype image configuration.
         *
         * @return The image configuration.
         */
        @Override
        public PropertyContainer createImageConfig() {

            PropertyDescriptor expressionDescriptor = new PropertyDescriptor(PROPERTY_NAME_EXPRESSION, String.class);
            expressionDescriptor.setNotNull(true);
            expressionDescriptor.setNotEmpty(true);

            PropertyContainer imageConfig = super.createImageConfig();
            imageConfig.addProperty(new Property(expressionDescriptor, new DefaultPropertyAccessor()));

            return imageConfig;
        }

        @Override
        public void handleRename(Mask mask, String oldExternalName, String newExternalName) {
            String oldExpression = getExpression(mask);
            final String newExpression = StringUtils.replaceWord(oldExpression, oldExternalName, newExternalName);
            setExpression(mask, newExpression);

            super.handleRename(mask, oldExternalName, newExternalName);
        }

        public static String getExpression(Mask mask) {
            return (String) mask.getImageConfig().getValue(PROPERTY_NAME_EXPRESSION);
        }

        public static void setExpression(Mask mask, String expression) {
            mask.getImageConfig().setValue(PROPERTY_NAME_EXPRESSION, expression);
        }
    }

    /**
     * A mask image type which is based on band math.
     */
    public static class GeometryType extends ImageType {
        public static final String PROPERTY_NAME_VECTOR_DATA = "vectorData";

        public GeometryType() {
            super("Geometry");
        }

        /**
         * Creates the image.
         *
         * @param mask The mask which requests creation of its image.
         *
         * @return The image.
         */
        @Override
        public MultiLevelImage createImage(Mask mask) {
            // todo
            return null;
        }

        /**
         * Creates a prototype image configuration.
         *
         * @return The image configuration.
         */
        @Override
        public PropertyContainer createImageConfig() {

            PropertyDescriptor vectorDataDescriptor = new PropertyDescriptor(PROPERTY_NAME_VECTOR_DATA, VectorData.class);
            vectorDataDescriptor.setNotNull(true);

            PropertyContainer imageConfig = super.createImageConfig();
            imageConfig.addProperty(new Property(vectorDataDescriptor, new DefaultPropertyAccessor()));

            return imageConfig;
        }

        public static VectorData getVectorData(Mask mask) {
            return (VectorData) mask.getImageConfig().getValue(PROPERTY_NAME_VECTOR_DATA);
        }

        public static void setVectorData(Mask mask, VectorData vectorData) {
            mask.getImageConfig().setValue(PROPERTY_NAME_VECTOR_DATA, vectorData);
        }
    }
}
