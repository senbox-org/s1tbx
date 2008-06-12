/*
 * $Id: WorldMapImageLoader.java,v 1.6 2007/04/20 11:44:24 marcoz Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.framework.ui;

import org.esa.beam.util.Debug;
import org.esa.beam.util.ImageUtils;

import javax.imageio.ImageIO;
import javax.swing.JOptionPane;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

/**
 * This utility class is responsible for loading the world map image.
 *
 * @author Marco Peters
 * @version $Revision$ $Date$
 */
public class WorldMapImageLoader {

    private static BufferedImage worldMapImage;
    private static boolean highResolution = false;
    private static final String WORLDMAP_JPG_HIGH = "WorldmapHighRes.jpg";
    private static final String WORLDMAP_JPG_LOW = "WorldmapLowRes.jpg";

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
            final String imageFileName = highRes ? WORLDMAP_JPG_HIGH : WORLDMAP_JPG_LOW;
            worldMapImage = loadImage(imageFileName);
        }
        return worldMapImage;
    }

    private static BufferedImage loadImage(final String fileName) {
        String resourcePath = "/auxdata/worldmap/" + fileName;
        InputStream stream = WorldMapImageLoader.class.getResourceAsStream(resourcePath);
        if (stream == null) {
            throw new IllegalStateException("resource not found: " + resourcePath);
        }
        BufferedImage loadedImage;
        try {
            final BufferedImage bi = ImageIO.read(stream);
            if (bi.getType() != BufferedImage.TYPE_3BYTE_BGR) {
                Debug.trace("Must convert world map image to 3-byte BGR...");
                loadedImage = ImageUtils.convertImage(bi, BufferedImage.TYPE_3BYTE_BGR);
                Debug.trace("World image converted");
            } else {
                loadedImage = bi;
                Debug.trace("World map image is already 3-byte BGR");
            }
        } catch (IOException e) {
            Debug.trace(e);
            JOptionPane.showMessageDialog(null, "Failed to load world map image '" + resourcePath
                                                + "':\n" + e.getMessage(), "Loading World Map Image",
                                                                           JOptionPane.ERROR_MESSAGE);
            loadedImage = createErrorImage();
        } finally {
            try {
                stream.close();
            } catch (IOException e) {
                // ok
            }
        }
        return loadedImage;
    }

    private static BufferedImage createErrorImage() {
        final BufferedImage errorImage = new BufferedImage(4000, 2000, BufferedImage.TYPE_3BYTE_BGR);
        final Graphics graphics = errorImage.getGraphics();
        graphics.setColor(Color.WHITE);
        final Font font = graphics.getFont().deriveFont(150.0f);
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
}
