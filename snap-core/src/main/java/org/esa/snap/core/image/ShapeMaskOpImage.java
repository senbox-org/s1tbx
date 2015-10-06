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

package org.esa.snap.core.image;

import org.esa.snap.core.datamodel.RasterDataNode;

import javax.media.jai.PlanarImage;
import javax.media.jai.RasterFactory;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.WritableRaster;

/**
 * Creates a mask image for a given {@link RasterDataNode}.
 * The resulting image will have a single-band, interleaved sample model
 * with sample values 255 or 0.
 */
public class ShapeMaskOpImage extends SingleBandedOpImage {
    private static final byte FALSE = (byte) 0;
    private static final byte TRUE = (byte) 255;
    private final Shape shape;
    private final ColorModel colorModel;

    public ShapeMaskOpImage(Shape shape, int width, int height, ResolutionLevel level) {
        super(DataBuffer.TYPE_BYTE, width, height, null, null, level);
        this.shape = AffineTransform.getScaleInstance(1.0 / getScale(), 1.0 / getScale()).createTransformedShape(shape);
        this.colorModel = PlanarImage.createColorModel(getSampleModel());
    }

    public ShapeMaskOpImage(Shape shape, int width, int height, Dimension tileSize, ResolutionLevel level) {
        super(DataBuffer.TYPE_BYTE, width, height, tileSize, null, level);
        this.shape = AffineTransform.getScaleInstance(1.0 / getScale(), 1.0 / getScale()).createTransformedShape(shape);
        this.colorModel = PlanarImage.createColorModel(getSampleModel());
    }

    @Override
    protected void computeRect(PlanarImage[] sourceImages, WritableRaster tile, Rectangle destRect) {
        WritableRaster wrappedTile = RasterFactory.createWritableRaster(tile.getSampleModel(),
                                                                        tile.getDataBuffer(),
                                                                        new Point(0, 0));
        final BufferedImage image = new BufferedImage(colorModel, wrappedTile, false, null);
        final Graphics2D graphics2D = image.createGraphics();
        graphics2D.translate(-tile.getMinX(), -tile.getMinY());
        graphics2D.setColor(Color.WHITE);
        graphics2D.fill(shape);
        graphics2D.dispose();

        final byte[] data = ((DataBufferByte) tile.getDataBuffer()).getData();
        for (int i = 0; i < data.length; i++) {
            data[i] = (data[i] != 0) ? TRUE : FALSE;
        }
    }
}
