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

package com.bc.layer.impl;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;

import javax.media.jai.PlanarImage;

import com.bc.layer.AbstractLayer;

/**
 * @author Norman Fomferra (norman.fomferra@brockmann-consult.de)
 */
public class RenderedImageLayer extends AbstractLayer {

    private static final BasicStroke BORDER_STROKE = new BasicStroke(1.0f);

    private RenderedImage _image;
    private AffineTransform _transform;

    public RenderedImageLayer(RenderedImage image) {
        this(image, null);
    }

    public RenderedImageLayer(RenderedImage image, int x, int y) {
        this(image, AffineTransform.getTranslateInstance(x, y));
    }

    public RenderedImageLayer(RenderedImage image, AffineTransform transform) {
        _image = image;
        _transform = transform;
        updateBoundingBox();
    }

    public RenderedImage getImage() {
        return _image;
    }

    public void setImage(RenderedImage image) {
        _image = image;
        updateBoundingBox();
        fireLayerChanged();
    }

    public AffineTransform getTransform() {
        return _transform;
    }

    public void setTransform(AffineTransform transform) {
        this._transform = transform;
        updateBoundingBox();
        fireLayerChanged();
    }

    public Color getBorderColor() {
        return (Color) getPropertyValue("style.borderColor");
    }

    public void setBorderColor(Color borderColor) {
        setPropertyValue("style.borderColor", borderColor);
    }

    private void updateBoundingBox() {
        final Rectangle imageRect = _image != null ? new Rectangle(0, 0, _image.getWidth(), _image.getHeight()) : new Rectangle();
        if (_transform != null) {
            setBoundingBox(_transform.createTransformedShape(imageRect).getBounds2D());
        } else {
            setBoundingBox(imageRect);
        }
    }

    public void draw(Graphics2D g2d) {
        if (_image == null) {
            return;
        }

        drawImageBorder(g2d);
        if (_image instanceof BufferedImage) {
            g2d.drawRenderedImage(_image, _transform);
        } else {
            drawTiledImage(g2d);
        }
    }

    private void drawTiledImage(Graphics2D g2d) {
        // Get the clipping rectangle
        Rectangle clipBounds = g2d.getClipBounds();
        if (clipBounds == null) {
            clipBounds = new Rectangle(0, 0, _image.getWidth(), _image.getHeight());
        }

        // Determine the extent of the clipping region in tile coordinates.
        int txmin, txmax, tymin, tymax;
        int ti, tj;

        txmin = XtoTileX(clipBounds.x);
        txmin = Math.max(txmin, _image.getMinTileX());
        txmin = Math.min(txmin, _image.getMinTileX() + _image.getNumXTiles() - 1);

        txmax = XtoTileX(clipBounds.x + clipBounds.width - 1);
        txmax = Math.max(txmax, _image.getMinTileX());
        txmax = Math.min(txmax, _image.getMinTileX() + _image.getNumXTiles() - 1);

        tymin = YtoTileY(clipBounds.y);
        tymin = Math.max(tymin, _image.getMinTileY());
        tymin = Math.min(tymin, _image.getMinTileY() + _image.getNumYTiles() - 1);

        tymax = YtoTileY(clipBounds.y + clipBounds.height - 1);
        tymax = Math.max(tymax, _image.getMinTileY());
        tymax = Math.min(tymax, _image.getMinTileY() + _image.getNumYTiles() - 1);

        final ColorModel cm = _image.getColorModel();
        final SampleModel sm = _image.getSampleModel();

        // Loop over tiles within the clipping region
        for (tj = tymin; tj <= tymax; tj++) {
            for (ti = txmin; ti <= txmax; ti++) {
                int tx = TileXtoX(ti);
                int ty = TileYtoY(tj);
                Raster tile = _image.getTile(ti, tj);
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
                    if (_transform != null) {
                        at.concatenate(_transform);
                    }
                    g2d.drawRenderedImage(bi, at);
                }
            }
        }
    }

    private void drawImageBorder(Graphics2D g2d) {
        final Color borderColor = getBorderColor();
        if (borderColor != null) {
            final Rectangle2D boundingBox = getBoundingBox();
            g2d.setColor(borderColor);
            g2d.setStroke(BORDER_STROKE);
            g2d.draw(boundingBox);
        }
    }

    private int XtoTileX(int x) {
        return (x - _image.getTileGridXOffset()) / _image.getTileWidth();
    }

    private int YtoTileY(int y) {
        return (y - _image.getTileGridYOffset()) / _image.getTileHeight();
    }

    private int TileXtoX(int tx) {
        return tx * _image.getTileWidth() + _image.getTileGridXOffset();
    }

    private int TileYtoY(int ty) {
        return ty * _image.getTileHeight() + _image.getTileGridYOffset();
    }

    public void dispose() {
        if (_image instanceof PlanarImage) {
            PlanarImage planarImage = (PlanarImage) _image;
            planarImage.dispose();
        }
        _image = null;
        _transform = null;
    }
}
