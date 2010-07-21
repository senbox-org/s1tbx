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
package org.esa.beam.framework.ui;

import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.glayer.CollectionLayer;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerContext;
import com.bc.ceres.glayer.LayerType;
import com.bc.ceres.glayer.LayerTypeRegistry;
import com.bc.ceres.grender.support.BufferedImageRendering;
import org.geotools.referencing.crs.DefaultGeographicCRS;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

/**
 * This utility class is responsible for loading the world map image.
 *
 * @author Marco Peters
 * @version $Revision$ $Date$
 */
public class WorldMapImageLoader {

    private static BufferedImage worldMapImage;
    private static boolean highResolution = false;
    private static final Dimension HI_RES_DIMENSION = new Dimension(4320, 2160);
    private static final Dimension LOW_RES_DIMENSION = new Dimension(2160, 1080);

    private WorldMapImageLoader() {
    }

    /**
     * Reads the world map image from disk if not yet loaded, otherwise
     * it is just returning the image.
     * <p/>
     * If the world map image cannot be read an image with an error message is returned.
     *
     * @param highRes specifies if the high-resolution image shall be returned,
     *
     * @return the world map image
     */
    public static BufferedImage getWorldMapImage(boolean highRes) {
        if (worldMapImage == null || isHighRes() != highRes) {
            setHighRes(highRes);

            LayerType layerType = LayerTypeRegistry.getLayerType("org.esa.beam.worldmap.BlueMarbleLayerType");
            if (layerType == null) {
                worldMapImage = createErrorImage();
            } else {
                final CollectionLayer rootLayer = new CollectionLayer();
                Layer worldMapLayer = layerType.createLayer(new WorldMapLayerContext(rootLayer), new PropertyContainer());
                Dimension dimension = highRes ? HI_RES_DIMENSION : LOW_RES_DIMENSION;
                final BufferedImageRendering biRendering = new BufferedImageRendering(dimension.width,
                                                                                      dimension.height);
                biRendering.getViewport().setModelYAxisDown(false);
                biRendering.getViewport().zoom(worldMapLayer.getModelBounds());
                worldMapLayer.render(biRendering);
                worldMapImage = biRendering.getImage();
            }
        }
        return worldMapImage;
    }

    private static BufferedImage createErrorImage() {
        final BufferedImage errorImage = new BufferedImage(LOW_RES_DIMENSION.width, LOW_RES_DIMENSION.height,
                                                           BufferedImage.TYPE_3BYTE_BGR);
        final Graphics graphics = errorImage.getGraphics();
        graphics.setColor(Color.WHITE);
        final Font font = graphics.getFont().deriveFont(75.0f);
        if (graphics instanceof Graphics2D) {
            final Graphics2D g2d = (Graphics2D) graphics;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        }
        graphics.setFont(font);
        final String msg = "Failed to load worldmap image.";
        final Rectangle2D stringBounds = graphics.getFontMetrics().getStringBounds(msg, graphics);
        final int xPos = (int) (errorImage.getWidth() * 0.5f - stringBounds.getWidth() * 0.5f);
        final int yPos = (int) (errorImage.getWidth() * 0.1f);
        graphics.drawString(msg, xPos, yPos);
        return errorImage;
    }

    private static boolean isHighRes() {
        return highResolution;
    }

    private static void setHighRes(boolean highRes) {
        highResolution = highRes;
    }

    private static class WorldMapLayerContext implements LayerContext {

        private final Layer rootLayer;

        private WorldMapLayerContext(Layer rootLayer) {
            this.rootLayer = rootLayer;
        }

        @Override
        public Object getCoordinateReferenceSystem() {
            return DefaultGeographicCRS.WGS84;
        }

        @Override
        public Layer getRootLayer() {
            return rootLayer;
        }
    }

}
