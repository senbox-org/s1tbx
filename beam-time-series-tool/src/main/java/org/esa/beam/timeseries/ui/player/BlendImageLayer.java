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

package org.esa.beam.timeseries.ui.player;

import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.ceres.glevel.MultiLevelSource;
import com.bc.ceres.glevel.support.DefaultMultiLevelSource;
import com.bc.ceres.grender.Rendering;
import com.bc.ceres.grender.Viewport;
import org.esa.beam.glevel.BandImageMultiLevelSource;

import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.RenderedImage;

class BlendImageLayer extends ImageLayer {

    private ImageLayer baseLayer;
    private ImageLayer blendLayer;
    private float blendFactor;

    BlendImageLayer(MultiLevelSource baseMultiLevelSource, MultiLevelSource blendMultiLevelSource) {
        super(DefaultMultiLevelSource.NULL);
        blendFactor = 0.0f;
        baseLayer = new ImageLayer(baseMultiLevelSource);
        blendLayer = new ImageLayer(blendMultiLevelSource);
    }

    void setBlendFactor(float factor) {
        blendFactor = factor;
    }

    @Override
    public MultiLevelSource getMultiLevelSource() {
        return getBaseMultiLevelSource();
    }

    public BandImageMultiLevelSource getBaseMultiLevelSource() {
        return (BandImageMultiLevelSource) baseLayer.getMultiLevelSource();
    }

    public BandImageMultiLevelSource getBlendMultiLevelSource() {
        return (BandImageMultiLevelSource) blendLayer.getMultiLevelSource();
    }

    @Override
    public AffineTransform getImageToModelTransform(int level) {
        return baseLayer.getImageToModelTransform(level);
    }

    @Override
    public AffineTransform getModelToImageTransform(int level) {
        return baseLayer.getModelToImageTransform(level);
    }

    /**
     * Returns the image of the base layer
     */
    @Override
    public RenderedImage getImage(int level) {
        return baseLayer.getImage(level);
    }

    @Override
    public int getLevel(Viewport vp) {
        return getLevel(getBaseMultiLevelSource().getModel(), vp);
    }

    @Override
    protected Rectangle2D getLayerModelBounds() {
        return getBaseMultiLevelSource().getModel().getModelBounds();
    }


    @Override
    protected void renderLayer(Rendering rendering) {
        final Graphics2D graphics = rendering.getGraphics();
        final Composite oldComposite = graphics.getComposite();
        try {
            final float layerAlpha = 1 - (float) getTransparency();
            final float baseLayerAlpha = (1 - blendFactor) * layerAlpha;
            graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, baseLayerAlpha));
            if (baseLayerAlpha != 0) {
                baseLayer.render(rendering);
            }
            final float blendLayerAlpha = blendFactor * layerAlpha;
            graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, blendLayerAlpha));
            if (blendLayerAlpha != 0) {
                blendLayer.render(rendering);
            }
        } finally {
            graphics.setComposite(oldComposite);
        }
    }

    @Override
    public void regenerate() {
        baseLayer.regenerate();
        blendLayer.regenerate();
    }

    @Override
    protected synchronized void disposeLayer() {
        super.disposeLayer();
        baseLayer.dispose();
        blendLayer.dispose();
    }

    public void swap(BandImageMultiLevelSource multiLevelSource, boolean forward) {
        if (forward) {
            baseLayer = blendLayer;
            blendLayer = new ImageLayer(multiLevelSource);
        } else {
            blendLayer = baseLayer;
            baseLayer = new ImageLayer(multiLevelSource);
        }
    }

}

