/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.beam.framework.datamodel;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.Validator;
import com.bc.ceres.binding.accessors.DefaultPropertyAccessor;
import com.bc.ceres.core.Assert;
import com.bc.ceres.glevel.MultiLevelImage;
import com.bc.ceres.glevel.MultiLevelModel;
import com.bc.ceres.glevel.MultiLevelSource;
import com.bc.ceres.glevel.support.AbstractMultiLevelSource;
import com.bc.jexp.ParseException;
import com.bc.jexp.impl.Tokenizer;
import org.esa.beam.dataio.dimap.DimapProductConstants;
import org.esa.beam.dataio.dimap.DimapProductHelpers;
import org.esa.beam.framework.dataop.barithm.BandArithmetic;
import org.esa.beam.jai.ImageManager;
import org.esa.beam.jai.ResolutionLevel;
import org.esa.beam.jai.VirtualBandOpImage;
import org.esa.beam.util.Debug;
import org.esa.beam.util.StringUtils;
import org.jdom.Element;

import java.awt.Color;
import java.awt.Shape;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Map;

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
        super(name, ProductData.TYPE_UINT8, width, height);
        Assert.notNull(imageType, "imageType");
        this.imageType = imageType;
        this.imageConfigListener = new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (isSourceImageSet()) {
                    getSourceImage().reset();
                }
                fireProductNodeChanged(evt.getPropertyName(), evt.getOldValue(), evt.getNewValue());
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
        if (isMaskImageInvalid(image)) {
            throw new IllegalStateException("Invalid mask image.");
        }
        return image;
    }

    private boolean isMaskImageInvalid(MultiLevelImage image) {
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

    @Override
    public Shape getValidShape() {
        return getSourceImage().getImageShape(0);
    }

    /**
     * Specifies a factory for the {@link RasterDataNode#getSourceImage() source image} used by a {@link Mask}.
     */
    public abstract static class ImageType {

        public static final String PROPERTY_NAME_COLOR = "color";
        public static final String PROPERTY_NAME_TRANSPARENCY = "transparency";
        public static final Color DEFAULT_COLOR = Color.RED;
        public static final double DEFAULT_TRANSPARENCY = 0.5;
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

        public Mask transferMask(Mask mask, Product product) {
            return null;
        }

        public boolean canTransferMask(Mask mask, Product product) {
            return false;
        }

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
    public static class BandMathsType extends ImageType {

        public static final String TYPE_NAME = "Maths";
        public static final String PROPERTY_NAME_EXPRESSION = "expression";

        public static final BandMathsType INSTANCE = new BandMathsType();

        private BandMathsType() {
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
            final MultiLevelModel multiLevelModel = ImageManager.getMultiLevelModel(mask);
            final MultiLevelSource multiLevelSource = new AbstractMultiLevelSource(multiLevelModel) {
                @Override
                public RenderedImage createImage(int level) {
                    return VirtualBandOpImage.createMask(getExpression(mask),
                                                         mask.getProduct(),
                                                         ResolutionLevel.create(getModel(), level));
                }
            };
            return new VirtualBandMultiLevelImage(multiLevelSource, getExpression(mask), mask.getProduct()) {
                @Override
                public void reset() {
                    super.reset();
                    mask.fireProductNodeDataChanged();
                }
            };
        }

        @Override
        public boolean canTransferMask(Mask mask, Product product) {
            final String expression = getExpression(mask);
            if (StringUtils.isNullOrEmpty(expression)) {
                return false;
            }
            try {
                if (mask.getProduct() != null) {
                    for (RasterDataNode raster : BandArithmetic.getRefRasters(expression, mask.getProduct())) {
                        if (raster instanceof Mask) {
                            if (!product.getMaskGroup().contains(raster.getName())) {
                                Mask refMask = (Mask) raster;
                                if (!canTransferMask(refMask, product)) {
                                    return false;
                                }
                            }
                        } else {
                            if (!product.containsRasterDataNode(raster.getName())) {
                                return false;
                            }
                        }
                    }
                } else { // the mask has not been added to a product yet
                    BandArithmetic.getRefRasters(expression, product);
                }
            } catch (ParseException e) {
                return false;
            }
            return true;
        }

        @Override
        public Mask transferMask(Mask mask, Product product) {
            if (canTransferMask(mask, product)) {
                String expression = getExpression(mask);
                final Map<Mask, Mask> translationMap = transferReferredMasks(expression, mask.getProduct(), product);
                expression = translateExpression(translationMap, expression);
                final String originalMaskName = mask.getName();
                final String maskName = getAvailableMaskName(originalMaskName, product.getMaskGroup());
                return product.addMask(maskName, expression, mask.getDescription(),
                                       mask.getImageColor(), mask.getImageTransparency());
            }

            return null;
        }

        private static Map<Mask, Mask> transferReferredMasks(String expression, Product sourceProduct,
                                                             Product targetProduct) {
            final Map<Mask, Mask> translationMap = new HashMap<Mask, Mask>();
            final RasterDataNode[] rasters;
            try {
                rasters = BandArithmetic.getRefRasters(expression, sourceProduct);
            } catch (ParseException e) {
                return translationMap;
            }
            for (RasterDataNode raster : rasters) {
                if (raster instanceof Mask && !targetProduct.getMaskGroup().contains(raster.getName())) {
                    Mask refMask = (Mask) raster;
                    Mask newMask = refMask.getImageType().transferMask(refMask, targetProduct);
                    translationMap.put(refMask, newMask);
                }
            }
            return translationMap;
        }

        private static String translateExpression(Map<Mask, Mask> translationMap, String expression) {
            for (Map.Entry<Mask, Mask> entry : translationMap.entrySet()) {
                String srcName = entry.getKey().getName();
                String targetName = entry.getValue().getName();
                expression = StringUtils.replaceWord(expression, srcName, targetName);
            }
            return expression;
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

        public static void setExpression(Mask mask, String expression) {
            mask.getImageConfig().setValue(PROPERTY_NAME_EXPRESSION, expression);
        }

        public static String getExpression(Mask mask) {
            return (String) mask.getImageConfig().getValue(PROPERTY_NAME_EXPRESSION);
        }

        public static Mask create(String name, String description, int width, int height,
                                  String expression, Color color, double transparency) {
            final Mask mask = new Mask(name, width, height, BandMathsType.INSTANCE);
            if (description != null) {
                mask.setDescription(description);
            }
            mask.setImageColor(color);
            mask.setImageTransparency(transparency);
            BandMathsType.setExpression(mask, expression);
            return mask;
        }

        /**
         * Used to read in Mask from legacy "BitmaskDef" format
         * @param element A DOM element
         * @param width The width of the mask
         * @param height The height of the mask
         * @return a mask
         */
        public static Mask createFromBitmaskDef(Element element, int width, int height) {
            final String name = element.getAttributeValue(DimapProductConstants.ATTRIB_NAME);
            String description = null;
            Element descElem = element.getChild(DimapProductConstants.TAG_BITMASK_DESCRIPTION);
            if (descElem != null) {
                description = descElem.getAttributeValue(DimapProductConstants.ATTRIB_VALUE).trim();
            }
            final String expression = element.getChild(DimapProductConstants.TAG_BITMASK_EXPRESSION).getAttributeValue(
                    DimapProductConstants.ATTRIB_VALUE).trim();
            final Color color = DimapProductHelpers.createColor(element.getChild(DimapProductConstants.TAG_BITMASK_COLOR));
            final String value = element.getChild(DimapProductConstants.TAG_BITMASK_TRANSPARENCY).getAttributeValue(
                    DimapProductConstants.ATTRIB_VALUE);
            float transparency = 0.5F;
            try {
                transparency = Float.parseFloat(value);
            } catch (NumberFormatException e) {
                Debug.trace(e);
            }
            return BandMathsType.create(name, description, width, height, expression, color, transparency);
        }

    }

    /**
     * A mask image type which is based on vector data.
     */
    public static class VectorDataType extends ImageType {

        public static final String TYPE_NAME = "Geometry";
        public static final String PROPERTY_NAME_VECTOR_DATA = "vectorData";

        public static final VectorDataType INSTANCE = new VectorDataType();

        private VectorDataType() {
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
            return VectorDataMultiLevelImage.createMask(getVectorData(mask), mask);
        }

        /**
         * Creates a prototype image configuration.
         *
         * @return The image configuration.
         */
        @Override
        public PropertyContainer createImageConfig() {

            PropertyDescriptor vectorDataDescriptor = new PropertyDescriptor(PROPERTY_NAME_VECTOR_DATA,
                                                                             VectorDataNode.class);
            vectorDataDescriptor.setNotNull(true);

            PropertyContainer imageConfig = super.createImageConfig();
            imageConfig.addProperty(new Property(vectorDataDescriptor, new DefaultPropertyAccessor()));

            return imageConfig;
        }

        public static VectorDataNode getVectorData(Mask mask) {
            return (VectorDataNode) mask.getImageConfig().getValue(PROPERTY_NAME_VECTOR_DATA);
        }

        public static void setVectorData(Mask mask, VectorDataNode vectorDataNode) {
            mask.getImageConfig().setValue(PROPERTY_NAME_VECTOR_DATA, vectorDataNode);
        }
    }

    public static class RangeType extends ImageType {

        public static final String TYPE_NAME = "Range";

        public static final String PROPERTY_NAME_MINIMUM = "minimum";
        public static final String PROPERTY_NAME_MAXIMUM = "maximum";
        public static final String PROPERTY_NAME_RASTER = "rasterName";

        public static final RangeType INSTANCE = new RangeType();

        private RangeType() {
            super(TYPE_NAME);
        }

        @Override
        public MultiLevelImage createImage(final Mask mask) {
            final MultiLevelModel multiLevelModel = ImageManager.getMultiLevelModel(mask);
            final MultiLevelSource multiLevelSource = new AbstractMultiLevelSource(multiLevelModel) {
                @Override
                public RenderedImage createImage(int level) {
                    return VirtualBandOpImage.createMask(getExpression(mask),
                                                         mask.getProduct(),
                                                         ResolutionLevel.create(getModel(), level));
                }
            };
            return new VirtualBandMultiLevelImage(multiLevelSource, getExpression(mask), mask.getProduct()) {
                @Override
                public void reset() {
                    super.reset();
                    mask.fireProductNodeDataChanged();
                }
            };
        }

        @Override
        public boolean canTransferMask(Mask mask, Product product) {
            final String rasterName = getRasterName(mask);
            return !StringUtils.isNullOrEmpty(rasterName) && product.containsRasterDataNode(rasterName);
        }

        @Override
        public Mask transferMask(Mask mask, Product product) {
            if (canTransferMask(mask, product)) {
                final String originalMaskName = mask.getName();
                final String maskName = getAvailableMaskName(originalMaskName, product.getMaskGroup());
                final int w = product.getSceneRasterWidth();
                final int h = product.getSceneRasterHeight();
                final Mask newMask = new Mask(maskName, w, h, this);
                newMask.setDescription(mask.getDescription());
                setImageStyle(newMask.getImageConfig(), mask.getImageColor(), mask.getImageTransparency());
                setRasterName(newMask, getRasterName(mask));
                setMinimum(newMask, getMinimum(mask));
                setMaximum(newMask, getMaximum(mask));
                product.getMaskGroup().add(newMask);
                return newMask;
            }

            return null;
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

        public static void setRasterName(Mask mask, String rasterName) {
            mask.getImageConfig().setValue(PROPERTY_NAME_RASTER, rasterName);
        }

        public static String getRasterName(Mask mask) {
            return (String) mask.getImageConfig().getValue(PROPERTY_NAME_RASTER);
        }

        public static void setMinimum(Mask mask, double minimum) {
            mask.getImageConfig().setValue(PROPERTY_NAME_MINIMUM, minimum);
        }

        public static Double getMinimum(Mask mask) {
            return (Double) mask.getImageConfig().getValue(PROPERTY_NAME_MINIMUM);
        }

        public static void setMaximum(Mask mask, double maximum) {
            mask.getImageConfig().setValue(PROPERTY_NAME_MAXIMUM, maximum);
        }

        public static Double getMaximum(Mask mask) {
            return (Double) mask.getImageConfig().getValue(PROPERTY_NAME_MAXIMUM);
        }

        public static String getExpression(Mask mask) {
            final Double min = getMinimum(mask);
            final Double max = getMaximum(mask);
            final String rasterName = getRasterName(mask);

            return rasterName + " >= " + min + " && " + rasterName + " <= " + max;
        }
    }

    private static String getAvailableMaskName(String name, ProductNodeGroup<Mask> maskGroup) {
        int index = 1;
        String foundName = name;
        while (maskGroup.contains(foundName)) {
            foundName = name + "_" + index;
        }
        return foundName;
    }
}
