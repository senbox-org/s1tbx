/*
 * $Id: RectangleIterator.java,v 1.1 2007/03/27 12:51:06 marcoz Exp $
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

import org.esa.beam.util.math.MathUtils;

import java.awt.*;
import java.util.Iterator;

/**
 * Created by marcoz.
 *
 * @author marcoz
 * @version $Revision: 1.1 $ $Date: 2007/03/27 12:51:06 $
 */
public class RectangleIterator implements Iterator<Rectangle> {

    private final Dimension requiredDimension;
    private final int sceneWidth;
    private final int sceneHeight;
    private int yOffset;

    public RectangleIterator(Dimension requiredDimension, int sceneWidth, int sceneHeight) {
        this.requiredDimension = requiredDimension;
        this.sceneWidth = sceneWidth;
        this.sceneHeight = sceneHeight;
        this.yOffset = 0;
    }

    public boolean hasNext() {
        return yOffset < sceneHeight;
    }

    public Rectangle next() {
        int height = requiredDimension.height;
        if (yOffset + height > sceneHeight) {
            height = sceneHeight - yOffset;
        }
        final Rectangle rectangle = new Rectangle(0, yOffset, sceneWidth, height);
        yOffset += height;
        return rectangle;
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }

    public int getNumRectangles() {
        return MathUtils.ceilInt((double) sceneHeight / (double) requiredDimension.height);
    }
}
