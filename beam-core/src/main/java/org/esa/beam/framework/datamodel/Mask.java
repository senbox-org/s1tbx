package org.esa.beam.framework.datamodel;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.Validator;
import com.bc.ceres.binding.accessors.DefaultPropertyAccessor;
import com.bc.ceres.core.Assert;
import com.bc.ceres.glevel.MultiLevelImage;
import com.bc.ceres.glevel.MultiLevelSource;
import com.bc.ceres.glevel.support.AbstractMultiLevelSource;
import com.bc.ceres.glevel.support.DefaultMultiLevelImage;
import com.bc.jexp.impl.Tokenizer;
import org.esa.beam.jai.ImageManager;
import org.esa.beam.jai.ResolutionLevel;
import org.esa.beam.jai.VectorDataMaskOpImage;
import org.esa.beam.jai.VirtualBandOpImage;
import org.esa.beam.util.StringUtils;

import javax.media.jai.KernelJAI;
import javax.media.jai.operator.DilateDescriptor;
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
    private final PropertyContainer imageConfig;


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
                if (isSourceImageSet()) {
                    getSourceImage().reset();
                }
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
    public abstract static class ImageType {

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

        public String getName() {
            return name;
        }
    }

    /**
     * A mask image type which is based on band math.
     */
    public static class BandMathType extends ImageType {

        public static final String TYPE_NAME = "Math";
        public static final String PROPERTY_NAME_EXPRESSION = "expression";

        public BandMathType() {
            super(TYPE_NAME);
        }

        /**
         * Creates the image.
         *
         * @param mask The mask which requests creation of its image.
         *
         * @return The image.
         */
        @Override
        public MultiLevelImage createImage(final Mask mask) {
            MultiLevelSource mls = new AbstractMultiLevelSource(ImageManager.createMultiLevelModel(mask)) {
                @Override
                public RenderedImage createImage(int level) {
                    return VirtualBandOpImage.createMask(getExpression(mask),
                                                         mask.getProduct(),
                                                         ResolutionLevel.create(getModel(), level));
                }
            };
            return new DefaultMultiLevelImage(mls);
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
            final Property property = new Property(expressionDescriptor, new DefaultPropertyAccessor());
            imageConfig.addProperty(property);

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

        private static void setExpression(Mask mask, String expression) {
            mask.getImageConfig().setValue(PROPERTY_NAME_EXPRESSION, expression);
        }
    }

    /**
     * A mask image type which is based on vector data.
     */
    public static class VectorDataType extends ImageType {

        public static final String PROPERTY_NAME_VECTOR_DATA = "vectorData";

        public VectorDataType() {
            super("Vector");
        }

        /**
         * Creates the image.
         *
         * @param mask The mask which requests creation of its image.
         *
         * @return The image.
         */
        @Override
        public MultiLevelImage createImage(final Mask mask) {
            MultiLevelSource mls = new AbstractMultiLevelSource(ImageManager.createMultiLevelModel(mask)) {
                @Override
                public RenderedImage createImage(int level) {
                    VectorDataMaskOpImage opImage = new VectorDataMaskOpImage(getVectorData(mask),
                                                                              ResolutionLevel.create(getModel(),
                                                                                                     level));
                    return DilateDescriptor.create(opImage, new KernelJAI(3, 3, new float[]{
                            1, 0, 1,
                            0, 1, 0,
                            1, 0, 1,
                    }), null);
                }
            };
            return new DefaultMultiLevelImage(mls);
        }

        /**
         * Creates a prototype image configuration.
         *
         * @return The image configuration.
         */
        @Override
        public PropertyContainer createImageConfig() {

            PropertyDescriptor vectorDataDescriptor = new PropertyDescriptor(PROPERTY_NAME_VECTOR_DATA,
                                                                             VectorData.class);
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

    public static class RangeType extends ImageType {

        public static final String TYPE_NAME = "Range";

        public static final String PROPERTY_NAME_MINIMUM = "minimum";
        public static final String PROPERTY_NAME_MAXIMUM = "maximum";
        public static final String PROPERTY_NAME_RASTER = "rasterName";

        public RangeType() {
            super(TYPE_NAME);
        }

        @Override
        public MultiLevelImage createImage(final Mask mask) {
            MultiLevelSource mls = new AbstractMultiLevelSource(ImageManager.createMultiLevelModel(mask)) {
                @Override
                public RenderedImage createImage(int level) {
                    final String expression = getExpression(mask);

                    return VirtualBandOpImage.createMask(expression,
                                                         mask.getProduct(),
                                                         ResolutionLevel.create(getModel(), level));
                }
            };
            return new DefaultMultiLevelImage(mls);
        }

        @Override
        public PropertyContainer createImageConfig() {
            PropertyDescriptor minimumDescriptor = new PropertyDescriptor(PROPERTY_NAME_MINIMUM, Double.class);
            minimumDescriptor.setNotNull(true);
            minimumDescriptor.setNotEmpty(true);
            PropertyDescriptor maximumDescriptor = new PropertyDescriptor(PROPERTY_NAME_MAXIMUM, Double.class);
            maximumDescriptor.setNotNull(true);
            maximumDescriptor.setNotEmpty(true);
            PropertyDescriptor rasterDescriptor = new PropertyDescriptor(PROPERTY_NAME_RASTER, String.class);
            rasterDescriptor.setNotNull(true);
            rasterDescriptor.setNotEmpty(true);
            rasterDescriptor.setValidator(new Validator() {
                @Override
                public void validateValue(Property property, Object value) throws ValidationException {
                    final String rasterName = String.valueOf(value);
                    if (!Tokenizer.isExternalName(rasterName)) {
                        throw new ValidationException(String.format("'%s' is not an external name.", rasterName));
                    }
                }
            });

            PropertyContainer imageConfig = super.createImageConfig();
            imageConfig.addProperty(new Property(minimumDescriptor, new DefaultPropertyAccessor()));
            imageConfig.addProperty(new Property(maximumDescriptor, new DefaultPropertyAccessor()));
            imageConfig.addProperty(new Property(rasterDescriptor, new DefaultPropertyAccessor()));

            return imageConfig;
        }

        @Override
        public void handleRename(Mask mask, String oldExternalName, String newExternalName) {
            final Property rasterProperty = mask.getImageConfig().getProperty(PROPERTY_NAME_RASTER);
            if (rasterProperty.getValue().equals(oldExternalName)) {
                try {
                    rasterProperty.setValue(newExternalName);
                } catch (ValidationException e) {
                    throw new IllegalStateException(e);
                }
            }

            super.handleRename(mask, oldExternalName, newExternalName);
        }

        public static String getRasterName(Mask mask) {
            return (String) mask.getImageConfig().getValue(PROPERTY_NAME_RASTER);
        }

        public static Double getMinimum(Mask mask) {
            return (Double) mask.getImageConfig().getValue(PROPERTY_NAME_MINIMUM);
        }

        public static Double getMaximum(Mask mask) {
            return (Double) mask.getImageConfig().getValue(PROPERTY_NAME_MAXIMUM);
        }

        private static String getExpression(Mask mask) {
            final Double min = getMinimum(mask);
            final Double max = getMaximum(mask);
            final String rasterName = getRasterName(mask);

            return rasterName + " >= " + min + " && " + rasterName + " <= " + max;
        }
    }
}
