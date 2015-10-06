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

import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Placemark;
import org.esa.snap.core.datamodel.PlacemarkDescriptor;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductNodeGroup;
import org.esa.snap.core.datamodel.RasterDataNode;

import javax.media.jai.PlanarImage;
import javax.media.jai.RasterFactory;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
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
public class PlacemarkMaskOpImage extends SingleBandedOpImage {
    private static final byte FALSE = (byte) 0;
    private static final byte TRUE = (byte) 255;
    private final ColorModel colorModel;
    private final Product product;
    private final PlacemarkDescriptor placemarkDescriptor;
    private final int placemarkSize;

    public PlacemarkMaskOpImage(Product product,
                                PlacemarkDescriptor placemarkDescriptor,
                                int placemarkSize,
                                int width,
                                int height,
                                ResolutionLevel level) {
        super(DataBuffer.TYPE_BYTE,
              width,
              height,
              product.getPreferredTileSize(),
              null,
              level);
        this.product = product;
        this.placemarkDescriptor = placemarkDescriptor;
        this.placemarkSize = placemarkSize;
        this.colorModel = createColorModel(getSampleModel());
    }

    @Override
    protected void computeRect(PlanarImage[] sourceImages, WritableRaster tile, Rectangle destRect) {
        final BufferedImage image = new BufferedImage(colorModel, RasterFactory.createWritableRaster(tile.getSampleModel(), tile.getDataBuffer(), new Point(0, 0)), false, null);
        final Graphics2D graphics2D = image.createGraphics();
        graphics2D.translate(-tile.getMinX(), -tile.getMinY());
        graphics2D.setColor(Color.WHITE);

        ProductNodeGroup<Placemark> placemarkGroup = getPlacemarkGroup();
        Placemark[] placemarks = placemarkGroup.toArray(new Placemark[placemarkGroup.getNodeCount()]);
        for (Placemark placemark : placemarks) {
            final PixelPos pixelPos = placemark.getPixelPos();
            if (pixelPos != null) {
                final int x = (int) pixelPos.x - placemarkSize / 2;
                final int y = (int) pixelPos.y - placemarkSize / 2;
                graphics2D.fillRect(x, y, placemarkSize, placemarkSize);
            }
        }
        graphics2D.dispose();

        final byte[] data = ((DataBufferByte) tile.getDataBuffer()).getData();
        for (int i = 0; i < data.length; i++) {
            data[i] = (data[i] != 0) ? TRUE : FALSE;
        }
    }

    private ProductNodeGroup<Placemark> getPlacemarkGroup() {
        return placemarkDescriptor.getPlacemarkGroup(product);
    }
}
