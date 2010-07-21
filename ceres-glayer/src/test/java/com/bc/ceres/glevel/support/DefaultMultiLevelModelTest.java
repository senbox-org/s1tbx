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

package com.bc.ceres.glevel.support;

import junit.framework.TestCase;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

public class DefaultMultiLevelModelTest extends TestCase {

    final double TX0 = -50;
    final double TY0 = 150;
    final double S0 = 10;
    final int W = 800;
    final int H = 600;

    final double[] ES = {1, 2, 4, 8};

    public void testAllProperties() {
        AffineTransform i2m0 = new AffineTransform();
        i2m0.translate(TX0, TY0);
        i2m0.scale(S0, S0);

        DefaultMultiLevelModel model = new DefaultMultiLevelModel(4, i2m0, W, H);
        assertEquals(4, model.getLevelCount());
        assertEquals(new Rectangle2D.Double(TX0, TY0, S0 * W, S0 * H), model.getModelBounds());
        assertEquals(i2m0, model.getImageToModelTransform(0));

        testLevel(model, 0);
        testLevel(model, 1);
        testLevel(model, 2);
        testLevel(model, 3);
    }

    private void testLevel(DefaultMultiLevelModel model, int level) {
        AffineTransform i2m = model.getImageToModelTransform(level);
        double scale = model.getScale(level);
        String msg = "at level " + level + ", ";
        assertEquals(msg, ES[level], scale, 1e-10);
        assertEquals(msg, level, model.getLevel(scale));
        assertEquals(msg, new Point2D.Double(TX0, TY0),
                     i2m.transform(new Point2D.Double(0, 0), null));
        assertEquals(msg, new Point2D.Double(S0 * scale + TX0, S0 * scale + TY0),
                     i2m.transform(new Point2D.Double(1, 1), null));
    }
}
