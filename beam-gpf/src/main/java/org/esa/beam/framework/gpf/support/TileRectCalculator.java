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

import java.awt.Rectangle;

import org.esa.beam.framework.datamodel.Product;

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

    public Rectangle computeSourceRectangle(Rectangle targetRectangle) {
        Rectangle requestedRectangle = new Rectangle(targetRectangle);
        requestedRectangle.grow(widthExtend, heightExtend);
        return requestedRectangle.intersection(sceneRectangle);
    }

    public static int convertToIndex(int x, int y, Rectangle rectangle) {
        final int ix = x - rectangle.x;
        final int iy = y - rectangle.y;
        final int index = iy * rectangle.width + ix;
        return index;
    }
}
