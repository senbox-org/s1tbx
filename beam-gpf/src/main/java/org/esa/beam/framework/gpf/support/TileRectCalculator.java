/*
 * $Id: TileRectCalculator.java,v 1.1 2007/03/27 12:51:05 marcoz Exp $
 *
 * Copyright (C) 2007 by Brockmann Consult (info@brockmann-consult.de)
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
package org.esa.beam.framework.gpf.support;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.util.math.MathUtils;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.geom.Point2D;

/**
 * Created by marcoz.
 *
 * @author marcoz
 * @version $Revision: 1.1 $ $Date: 2007/03/27 12:51:05 $
 */
public class TileRectCalculator {

    private final Rectangle sceneRectangle;
    private final int widthExtend;
    private final int heightExtend;

    public TileRectCalculator(Product product, int widthExtend, int heightExtend) {
        final int sceneWidth = product.getSceneRasterWidth();
        final int sceneHeight = product.getSceneRasterHeight();
        this.sceneRectangle = new Rectangle(sceneWidth, sceneHeight);
        this.widthExtend = widthExtend;
        this.heightExtend = heightExtend;
    }

    public TileRectCalculator(Rectangle sceneRectangle, int widthExtend, int heightExtend) {
        this.sceneRectangle = sceneRectangle;
        this.widthExtend = widthExtend;
        this.heightExtend = heightExtend;
    }

    public Rectangle computeSourceRectangle(Rectangle targetRectangle) {
        Rectangle requestedRectangle = new Rectangle(targetRectangle);
        requestedRectangle.grow(widthExtend, heightExtend);
        return requestedRectangle.intersection(sceneRectangle);
    }

    public Dimension computeMaxSourceSize(Dimension maxTileSize) {
        final int width = maxTileSize.width + 2 * widthExtend;
        final int height = maxTileSize.height + 2 * heightExtend;
        final Dimension dim = new Dimension(width, height);
        return dim;
    }

    public static int convertToIndex(int x, int y, Rectangle rectangle) {
        final int ix = x - rectangle.x;
        final int iy = y - rectangle.y;
        final int index = iy * rectangle.width + ix;
        return index;
    }

    public static int convertToIndex(Point2D pixelPos, Rectangle tileRectangle) {
        final int pixelX = MathUtils.floorInt(pixelPos.getX());
        final int pixelY = MathUtils.floorInt(pixelPos.getY());
        return convertToIndex(pixelX, pixelY, tileRectangle);
    }
}
