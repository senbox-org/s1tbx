/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package com.bc.ceres.glayer.support;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.core.Assert;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerContext;
import com.bc.ceres.glayer.LayerType;
import com.bc.ceres.glayer.LayerTypeRegistry;
import com.bc.ceres.glevel.MultiLevelModel;
import com.bc.ceres.glevel.MultiLevelRenderer;
import com.bc.ceres.glevel.MultiLevelSource;
import com.bc.ceres.glevel.support.ConcurrentMultiLevelRenderer;
import com.bc.ceres.glevel.support.DefaultMultiLevelModel;
import com.bc.ceres.glevel.support.DefaultMultiLevelRenderer;
import com.bc.ceres.glevel.support.DefaultMultiLevelSource;
import com.bc.ceres.grender.InteractiveRendering;
import com.bc.ceres.grender.Rendering;
import com.bc.ceres.grender.Viewport;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.RenderedImage;

/**
 * A multi-resolution capable image layer.
 *
 * @author Norman Fomferra
 */
public class ImageLayer extends Layer {

    private static final Type LAYER_TYPE = LayerTypeRegistry.getLayerType(Type.class);

    public static final String PROPERTY_NAME_MULTI_LEVEL_SOURCE = "multiLevelSource";
    public static final String PROPERTY_NAME_BORDER_SHOWN = "borderShown";
    public static final String PROPERTY_NAME_BORDER_WIDTH = "borderWidth";
    public static final String PROPERTY_NAME_BORDER_COLOR = "borderColor";

    public static final boolean DEFAULT_BORDER_SHOWN = false;
    public static final double DEFAULT_BORDER_WIDTH = 1.0;
    public static final Color DEFAULT_BORDER_COLOR = new Color(204, 204, 255);

    /**
     * @deprecated since BEAM 4.7, no replacement; kept for compatibility of sessions
     */
    @Deprecated
    private static final String PROPERTY_NAME_IMAGE_TO_MODEL_TRANSFORM = "imageToModelTransform";

    private MultiLevelSource multiLevelSource;

    private MultiLevelRenderer multiLevelRenderer;
    private boolean debug;

    /**
     * Constructs a single-resolution-level image layer.
     *
     * @param image the image
     */
    public ImageLayer(RenderedImage image) {
        this(image, new AffineTransform(), 1);
    }

    /**
     * Constructs a multi-resolution-level image layer.
     *
     * @param image                 the image
     * @param imageToModelTransform the transformation from image to model CS
     * @param levelCount            the number of resolution levels
     */
    public ImageLayer(RenderedImage image, AffineTransform imageToModelTransform, int levelCount) {
        this(new DefaultMultiLevelSource(image,
                                         new DefaultMultiLevelModel(levelCount, imageToModelTransform,
                                                                    DefaultMultiLevelModel.getModelBounds(
                                                                            imageToModelTransform, image))
        ));
    }

    /**
     * Constructs a multi-resolution-level image layer.
     *
     * @param multiLevelSource the multi-resolution-level image
     */
    public ImageLayer(MultiLevelSource multiLevelSource) {
        this(LAYER_TYPE, multiLevelSource,  initConfiguration(LAYER_TYPE.createLayerConfig(null), multiLevelSource));
    }

    public ImageLayer(Type layerType, MultiLevelSource multiLevelSource, PropertySet configuration) {
        super(layerType, configuration);
        Assert.notNull(multiLevelSource);
        this.multiLevelSource = multiLevelSource;
        setName("Image Layer");
    }

    @Override
    public void regenerate() {
        clearCaches();
        fireLayerDataChanged(getModelBounds());
    }

    public RenderedImage getImage() {
        return getImage(0);
    }

    public MultiLevelSource getMultiLevelSource() {
        return multiLevelSource;
    }

    public void setMultiLevelSource(MultiLevelSource multiLevelSource) {
        Assert.notNull(multiLevelSource);
        if (multiLevelSource != this.multiLevelSource) {
            final Rectangle2D region;
            final Rectangle2D oldBounds = this.multiLevelSource.getModel().getModelBounds();
            final Rectangle2D newBounds = multiLevelSource.getModel().getModelBounds();
            if (oldBounds == null) {
                region = newBounds;
            } else if (newBounds == null) {
                region = oldBounds;
            } else {
                region = oldBounds.createUnion(newBounds);
            }
            clearCaches();
            this.multiLevelSource = multiLevelSource;
            multiLevelRenderer = null;
            fireLayerDataChanged(region);
        }
    }

    public AffineTransform getImageToModelTransform() {
        return getImageToModelTransform(0);
    }

    public AffineTransform getModelToImageTransform() {
        return getModelToImageTransform(0);
    }

    public RenderedImage getImage(int level) {
        return multiLevelSource.getImage(level);
    }

    public AffineTransform getImageToModelTransform(int level) {
        return multiLevelSource.getModel().getImageToModelTransform(level);
    }

    public AffineTransform getModelToImageTransform(int level) {
        return multiLevelSource.getModel().getModelToImageTransform(level);
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public int getLevel(Viewport vp) {
        return getLevel(multiLevelSource.getModel(), vp);
    }

    public static int getLevel(MultiLevelModel model, Viewport vp) {
        final AffineTransform i2m = model.getImageToModelTransform(0);
        final double i2mScale = Math.sqrt(Math.abs(i2m.getDeterminant()));
        final double m2vScale = 1.0 / vp.getZoomFactor();
        final double scale = m2vScale / i2mScale;
        return model.getLevel(scale);
    }

    @Override
    protected Rectangle2D getLayerModelBounds() {
        return multiLevelSource.getModel().getModelBounds();
    }

    @Override
    protected void renderLayer(Rendering rendering) {
        if (multiLevelSource == MultiLevelSource.NULL) {
            return;
        }
        final Viewport vp = rendering.getViewport();
        final int level = getLevel(vp);
        final MultiLevelRenderer renderer = getRenderer(rendering);
        renderer.renderImage(rendering, multiLevelSource, level);

        if (isBorderShown()) {
            renderImageBorder(rendering, level);
        }
    }

    private void renderImageBorder(Rendering rendering, int level) {
        final Graphics2D graphics2D = rendering.getGraphics();
        final Viewport viewport = rendering.getViewport();

        final Object oldAntialiasing = graphics2D.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        final Paint oldPaint = graphics2D.getPaint();
        final Stroke oldStroke = graphics2D.getStroke();

        try {
            AffineTransform i2m = multiLevelSource.getModel().getImageToModelTransform(level);
            AffineTransform m2v = viewport.getModelToViewTransform();
            RenderedImage image = multiLevelSource.getImage(level);
            final Shape modelShape = i2m.createTransformedShape(
                    new Rectangle(image.getMinX(), image.getMinY(), image.getWidth(), image.getHeight()));
            Shape viewShape = m2v.createTransformedShape(modelShape);

            graphics2D.setStroke(new BasicStroke((float) Math.max(0.0, getBorderWidth())));
            graphics2D.setColor(getBorderColor());
            graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics2D.draw(viewShape);
        } finally {
            graphics2D.setPaint(oldPaint);
            graphics2D.setStroke(oldStroke);
            graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAntialiasing);
        }
    }

    @Override
    protected synchronized void disposeLayer() {
        resetRenderer();
        if (multiLevelSource != null) {
            multiLevelSource.reset();
            multiLevelSource = null;
        }
    }

    private synchronized MultiLevelRenderer getRenderer(Rendering rendering) {
        if (rendering instanceof InteractiveRendering) {
            if (multiLevelRenderer == null) {
                multiLevelRenderer = new ConcurrentMultiLevelRenderer();
                // multiLevelRenderer = new DefaultMultiLevelRenderer();
            }
            return multiLevelRenderer;
        } else {
            return new DefaultMultiLevelRenderer();
        }
    }

    private void resetRenderer() {
        if (multiLevelRenderer != null) {
            multiLevelRenderer.reset();
            multiLevelRenderer = null;
        }
    }

    private void clearCaches() {
        resetRenderer();
        multiLevelSource.reset();
    }

    public boolean isBorderShown() {
        return getConfigurationProperty(PROPERTY_NAME_BORDER_SHOWN, DEFAULT_BORDER_SHOWN);
    }

    public double getBorderWidth() {
        return getConfigurationProperty(PROPERTY_NAME_BORDER_WIDTH, DEFAULT_BORDER_WIDTH);
    }

    public Color getBorderColor() {
        return getConfigurationProperty(PROPERTY_NAME_BORDER_COLOR, DEFAULT_BORDER_COLOR);
    }

    private static PropertySet initConfiguration(PropertySet configuration, MultiLevelSource multiLevelSource) {
        configuration.setValue(PROPERTY_NAME_MULTI_LEVEL_SOURCE, multiLevelSource);
        return configuration;
    }

    public static class Type extends LayerType {

        private static final String TYPE_NAME = "ImageLayerType";
        private static final String[] ALIASES = {"com.bc.ceres.glayer.support.ImageLayer$Type"};

        @Override
        public String getName() {
            return TYPE_NAME;
        }
        
        @Override
        public String[] getAliases() {
            return ALIASES;
        }
        
        @Override
        public boolean isValidFor(LayerContext ctx) {
            return true;
        }

        @Override
        public Layer createLayer(LayerContext ctx, PropertySet configuration) {
            MultiLevelSource multiLevelSource = (MultiLevelSource) configuration.getValue(
                    ImageLayer.PROPERTY_NAME_MULTI_LEVEL_SOURCE);
            return new ImageLayer(this, multiLevelSource, configuration);
        }

        @Override
        public PropertySet createLayerConfig(LayerContext ctx) {
            final PropertyContainer template = new PropertyContainer();

            addMultiLevelSourceModel(template);
            addImageToModelTransformModel(template);

            template.addProperty(Property.create(ImageLayer.PROPERTY_NAME_BORDER_SHOWN, Boolean.class, ImageLayer.DEFAULT_BORDER_SHOWN, true));
            template.addProperty(Property.create(ImageLayer.PROPERTY_NAME_BORDER_COLOR, Color.class, ImageLayer.DEFAULT_BORDER_COLOR, true));
            template.addProperty(Property.create(ImageLayer.PROPERTY_NAME_BORDER_WIDTH, Double.class, ImageLayer.DEFAULT_BORDER_WIDTH, true));

            return template;
        }

        private static Property addImageToModelTransformModel(PropertyContainer configuration) {
            Property property = configuration.getProperty(PROPERTY_NAME_IMAGE_TO_MODEL_TRANSFORM);
            if (property == null) {
                 property = Property.create(PROPERTY_NAME_IMAGE_TO_MODEL_TRANSFORM, AffineTransform.class);
                configuration.addProperty(property);
            }
            property.getDescriptor().setTransient(true);
            return property;
         }


        private static Property addMultiLevelSourceModel(PropertyContainer configuration) {
            if (configuration.getProperty(PROPERTY_NAME_MULTI_LEVEL_SOURCE) == null) {
                configuration.addProperty(Property.create(PROPERTY_NAME_MULTI_LEVEL_SOURCE, MultiLevelSource.class));
            }
            configuration.getDescriptor(PROPERTY_NAME_MULTI_LEVEL_SOURCE).setTransient(true);

            return configuration.getProperty(PROPERTY_NAME_MULTI_LEVEL_SOURCE);
        }
    }
}