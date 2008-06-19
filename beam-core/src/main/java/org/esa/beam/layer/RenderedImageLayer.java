/*
 * $Id: RenderedImageLayer.java,v 1.1.1.1 2006/09/11 08:16:43 norman Exp $
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

package org.esa.beam.layer;

import com.bc.view.ViewModel;
import org.esa.beam.util.Debug;

import javax.media.jai.PlanarImage;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.*;

public class RenderedImageLayer extends StyledLayer {

    private RenderedImage image;
    private AffineTransform transform;

    public RenderedImageLayer(RenderedImage image) {
        this(image, null);
    }


    @Override
    public String getPropertyNamePrefix() {
        return "image";
    }

    public RenderedImageLayer(RenderedImage image, int x, int y) {
        this(image, AffineTransform.getTranslateInstance(x, y));
    }

    public RenderedImageLayer(RenderedImage image, AffineTransform transform) {
        this.image = image;
        this.transform = transform;
        updateBoundingBox();
    }

    public RenderedImage getImage() {
        return image;
    }

    public void setImage(RenderedImage image) {
        if (image != this.image) {
            this.image = image;
            updateBoundingBox();
            fireLayerChanged();
        }
    }

    public AffineTransform getTransform() {
        return transform;
    }

    public void setTransform(AffineTransform transform) {
        this.transform = transform;
        updateBoundingBox();
        fireLayerChanged();
    }


    private void updateBoundingBox() {
        final Rectangle imageRect = image != null ? new Rectangle(0, 0, image.getWidth(), image.getHeight()) : new Rectangle();
        if (transform != null) {
            setBoundingBox(transform.createTransformedShape(imageRect).getBounds2D());
        } else {
            setBoundingBox(imageRect);
        }
    }

    public void draw(Graphics2D g2d, ViewModel viewModel) {
        if (image == null) {
            return;
        }

        if (image instanceof BufferedImage) {
            g2d.drawRenderedImage(image, transform);
        } else {
            drawTiledImage(g2d);
        }
    }

    private void drawTiledImage(Graphics2D g2d) {
        // Get the clipping rectangle
        Rectangle clipBounds = g2d.getClipBounds();
        if (clipBounds == null) {
            clipBounds = new Rectangle(0, 0, image.getWidth(), image.getHeight());
        }

        // Determine the extent of the clipping region in tile coordinates.
        int txmin, txmax, tymin, tymax;
        int ti, tj;

        txmin = XtoTileX(clipBounds.x);
        txmin = Math.max(txmin, image.getMinTileX());
        txmin = Math.min(txmin, image.getMinTileX() + image.getNumXTiles() - 1);

        txmax = XtoTileX(clipBounds.x + clipBounds.width - 1);
        txmax = Math.max(txmax, image.getMinTileX());
        txmax = Math.min(txmax, image.getMinTileX() + image.getNumXTiles() - 1);

        tymin = YtoTileY(clipBounds.y);
        tymin = Math.max(tymin, image.getMinTileY());
        tymin = Math.min(tymin, image.getMinTileY() + image.getNumYTiles() - 1);

        tymax = YtoTileY(clipBounds.y + clipBounds.height - 1);
        tymax = Math.max(tymax, image.getMinTileY());
        tymax = Math.min(tymax, image.getMinTileY() + image.getNumYTiles() - 1);

        final ColorModel cm = image.getColorModel();
        final SampleModel sm = image.getSampleModel();

        // JAIJAIJAI
        if (Boolean.getBoolean("beam.imageTiling.enabled") &&
                Boolean.getBoolean("beam.imageTiling.debug")) {
            Debug.trace("Painting tiles:");
            Debug.trace("  X = " + txmin + " to " + txmax);
            Debug.trace("  Y = " + tymin + " to " + tymax);
        }

        // Loop over tiles within the clipping region
        for (tj = tymin; tj <= tymax; tj++) {
            for (ti = txmin; ti <= txmax; ti++) {
                int tx = TileXtoX(ti);
                int ty = TileYtoY(tj);
                Raster tile = image.getTile(ti, tj);
                if (tile != null) {
                    DataBuffer dataBuffer = tile.getDataBuffer();
                    WritableRaster wr = Raster.createWritableRaster(sm,
                                                                    dataBuffer,
                                                                    null);
                    BufferedImage bi = new BufferedImage(cm,
                                                         wr,
                                                         cm.isAlphaPremultiplied(),
                                                         null);
                    AffineTransform at = AffineTransform.getTranslateInstance(tx, ty);
                    if (transform != null) {
                        at.concatenate(transform);
                    }
                    g2d.drawRenderedImage(bi, at);

                    // JAIJAIJAI
                    if (Boolean.getBoolean("beam.imageTiling.enabled") &&
                            Boolean.getBoolean("beam.imageTiling.debug")) {
                        Debug.trace("Painting tile " + tx + "," + ty);
                        Color colorOld = g2d.getColor();
                        g2d.setColor(Color.RED);
                        g2d.drawRect(tx, ty, bi.getWidth(), bi.getHeight());
                        g2d.setColor(colorOld);
                    }
                }
            }
        }
    }

    private int XtoTileX(int x) {
        return (x - image.getTileGridXOffset()) / image.getTileWidth();
    }

    private int YtoTileY(int y) {
        return (y - image.getTileGridYOffset()) / image.getTileHeight();
    }

    private int TileXtoX(int tx) {
        return tx * image.getTileWidth() + image.getTileGridXOffset();
    }

    private int TileYtoY(int ty) {
        return ty * image.getTileHeight() + image.getTileGridYOffset();
    }

    @Override
    public void dispose() {
        if (image instanceof PlanarImage) {
            PlanarImage planarImage = (PlanarImage) image;
            planarImage.dispose();
        }
        image = null;
        transform = null;
        super.dispose();
    }
}
