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

package com.bc.ceres.grender.support;

import com.bc.ceres.grender.Viewport;
import org.junit.Test;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import static com.bc.ceres.glayer.Assert2D.assertEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.fail;

public class DefaultViewportTest {

    @Test
    public void testDefaultSettings() {
        final DefaultViewport viewport = new DefaultViewport(true);
        assertEquals(new AffineTransform(), viewport.getModelToViewTransform());
        assertEquals(new AffineTransform(), viewport.getViewToModelTransform());
        assertEquals(p(0.0, 0.0), getModelOffset(viewport));
        assertEquals(1.0, viewport.getZoomFactor(), 1e-10);
    }

    @Test
    public void testClone() {
        final DefaultViewport viewport = new DefaultViewport(new Rectangle(40, 50), true);
        viewport.setOrientation(0.3);
        viewport.setZoomFactor(1.2);
        viewport.setOffset(-4.0, 3.7);
        viewport.setModelYAxisDown(true);

        assertEquals(0.3, viewport.getOrientation(), 1e-10);
        assertEquals(-4.0, viewport.getOffsetX(), 1e-10);
        assertEquals(3.7, viewport.getOffsetY(), 1e-10);
        assertEquals(1.2, viewport.getZoomFactor(), 1e-10);
        assertEquals(true, viewport.isModelYAxisDown());

        Viewport viewportClone = viewport.clone();

        assertEquals(0.3, viewportClone.getOrientation(), 1e-10);
        assertEquals(-4.0, viewportClone.getOffsetX(), 1e-10);
        assertEquals(3.7, viewportClone.getOffsetY(), 1e-10);
        assertEquals(1.2, viewportClone.getZoomFactor(), 1e-10);
        assertEquals(true, viewportClone.isModelYAxisDown());
    }


    @Test
    public void testTransformsAreNotLife() {
        final DefaultViewport viewport = new DefaultViewport();

        final AffineTransform m2v = viewport.getModelToViewTransform();
        assertNotSame(m2v, viewport.getModelToViewTransform());

        final AffineTransform v2u = viewport.getViewToModelTransform();
        assertNotSame(v2u, viewport.getViewToModelTransform());

        viewport.moveViewDelta(150.0, -10.0);

        assertNotSame(m2v, viewport.getModelToViewTransform());
        assertNotSame(v2u, viewport.getViewToModelTransform());
    }

    @Test
    public void testInverse() {
        final DefaultViewport viewport = new DefaultViewport(true);

        final AffineTransform m2v = viewport.getModelToViewTransform();
        assertNotSame(m2v, viewport.getModelToViewTransform());

        final AffineTransform v2u = viewport.getViewToModelTransform();
        assertNotSame(v2u, viewport.getViewToModelTransform());

        viewport.moveViewDelta(150.0, -10.0);

        assertEquals(p(-150, 10), t(viewport.getViewToModelTransform(), p(0, 0)));
        assertEquals(p(0, 0), t(viewport.getModelToViewTransform(), p(-150, 10)));

        assertEquals(p(150, -10), t(viewport.getModelToViewTransform(), p(0, 0)));
        assertEquals(p(0.0, 0.0), t(viewport.getViewToModelTransform(), p(150, -10)));
    }

    @Test
    public void testMove() {
        final DefaultViewport viewport = new DefaultViewport(true);
        viewport.moveViewDelta(15.0, 10.0);
        assertEquals(p(-15.0, -10.0), getModelOffset(viewport));
        viewport.moveViewDelta(-15.0, -10.0);
        assertEquals(p(0.0, 0.0), getModelOffset(viewport));
    }

    @Test
    public void testZoomFactor() {
        final DefaultViewport viewport = new DefaultViewport(true);
        Point2D vc, uc;
        viewport.moveViewDelta(-10, -10);

        /////////////////////////////
        // view center 1

        viewport.setViewBounds(new Rectangle(0, 0, 30, 20));

        vc = p(15, 10);
        uc = t(viewport.getViewToModelTransform(), vc);

        viewport.setZoomFactor(0.5);

        assertEquals(0.5, viewport.getZoomFactor(), 1e-10);
        assertEquals(p(-5.0, 0.0), getModelOffset(viewport));
        assertEquals(uc, t(viewport.getViewToModelTransform(), vc));

        viewport.setZoomFactor(2.0);
        assertEquals(2.0, viewport.getZoomFactor(), 1e-10);
        assertEquals(p(17.5, 15.0), getModelOffset(viewport));
        assertEquals(uc, t(viewport.getViewToModelTransform(), vc));

        /////////////////////////////
        // view center 2

        viewport.setViewBounds(new Rectangle(0, 0, 100, 50));

        vc = p(50, 25);
        uc = t(viewport.getViewToModelTransform(), vc);

        viewport.setZoomFactor(1.0 / 1.2);

        assertEquals(1.0 / 1.2, viewport.getZoomFactor(), 1e-10);
        assertEquals(p(-17.5, -2.5), getModelOffset(viewport));
        assertEquals(uc, t(viewport.getViewToModelTransform(), vc));

        viewport.setZoomFactor(1.0 / 0.8);

        assertEquals(1.0 / 0.8, viewport.getZoomFactor(), 1e-10);
        assertEquals(p(2.5, 7.5), getModelOffset(viewport));
        assertEquals(uc, t(viewport.getViewToModelTransform(), vc));
    }

    @Test
    public void testRelativeZoomWithAffineTransform() {
        Point2D vc; // zoom center in view CS
        final Point2D v0 = p(0, 0);
        Point2D u0;

        AffineTransform v2u = new AffineTransform(); // view to model CS transformation
        v2u.translate(10, 10); // (10,10) are the model coordinates at (0,0) in view coordinates
        u0 = t(v2u, v0);

        assertEquals(1.0, v2u.getScaleX(), 1.0e-10);
        assertEquals(1.0, v2u.getScaleY(), 1.0e-10);
        assertEquals(p(10, 10), u0);

        /////////////////////////////
        // view center 1

        vc = p(15, 10);
        assertEquals(p(25.0, 20.0), t(v2u, vc));

        zoom(v2u, vc, 2.0);

        assertEquals(p(25.0, 20.0), t(v2u, vc));
        assertEquals(2.0, v2u.getScaleX(), 1.0e-10);
        assertEquals(2.0, v2u.getScaleY(), 1.0e-10);
        assertEquals(p(-5.0, 0.0), t(v2u, v0));

        zoom(v2u, vc, 0.5);

        assertEquals(p(25.0, 20.0), t(v2u, vc));
        assertEquals(0.5, v2u.getScaleX(), 1.0e-10);
        assertEquals(0.5, v2u.getScaleY(), 1.0e-10);
        assertEquals(p(17.5, 15.0), t(v2u, v0));

        /////////////////////////////
        // view center 2

        vc = p(50, 25);
        assertEquals(p(42.5, 27.5), t(v2u, vc));

        zoom(v2u, vc, 1.2);

        assertEquals(p(42.5, 27.5), t(v2u, vc));
        assertEquals(1.2, v2u.getScaleX(), 1.0e-10);
        assertEquals(1.2, v2u.getScaleY(), 1.0e-10);
        assertEquals(p(-17.5, -2.5), t(v2u, v0));

        zoom(v2u, vc, 0.8);

        assertEquals(p(42.5, 27.5), t(v2u, vc));
        assertEquals(0.8, v2u.getScaleX(), 1.0e-10);
        assertEquals(0.8, v2u.getScaleY(), 1.0e-10);
        assertEquals(p(2.5, 7.5), t(v2u, v0));

    }

    @Test
    public void testRelativeZoomWithViewport() {
        final DefaultViewport viewport = new DefaultViewport(new Rectangle(40, 50), true);
        viewport.moveViewDelta(10, 10);
        Point2D vc; // zoom center in view CS
        final Point2D v0 = p(0, 0);
        Point2D u0;

        viewport.setOffset(10, 10);
        AffineTransform v2m = viewport.getViewToModelTransform();
        u0 = t(v2m, v0);

        assertEquals(1.0, v2m.getScaleX(), 1.0e-10);
        assertEquals(1.0, v2m.getScaleY(), 1.0e-10);
        assertEquals(p(10, 10), u0);

        /////////////////////////////
        // view center 1

        vc = p(15, 10);
        assertEquals(p(25.0, 20.0), t(v2m, vc));

        viewport.setZoomFactor(0.5, vc);
        v2m = viewport.getViewToModelTransform();

        assertEquals(0.5, viewport.getZoomFactor(), 1.0e-10);
        assertEquals(p(25.0, 20.0), t(v2m, vc));
        assertEquals(2.0, v2m.getScaleX(), 1.0e-10);
        assertEquals(2.0, v2m.getScaleY(), 1.0e-10);
        assertEquals(p(-5.0, 0.0), t(v2m, v0));

        viewport.setZoomFactor(2.0, vc);
        v2m = viewport.getViewToModelTransform();

        assertEquals(2.0, viewport.getZoomFactor(), 1.0e-10);
        assertEquals(p(25.0, 20.0), t(v2m, vc));
        assertEquals(0.5, v2m.getScaleX(), 1.0e-10);
        assertEquals(0.5, v2m.getScaleY(), 1.0e-10);
        assertEquals(p(17.5, 15.0), t(v2m, v0));

        /////////////////////////////
        // view center 2

        vc = p(50, 25);
        assertEquals(p(42.5, 27.5), t(v2m, vc));

        viewport.setZoomFactor(1 / 1.2, vc);
        v2m = viewport.getViewToModelTransform();

        assertEquals(1 / 1.2, viewport.getZoomFactor(), 1.0e-10);
        assertEquals(p(42.5, 27.5), t(v2m, vc));
        assertEquals(1.2, v2m.getScaleX(), 1.0e-10);
        assertEquals(1.2, v2m.getScaleY(), 1.0e-10);
        assertEquals(p(-17.5, -2.5), t(v2m, v0));

        viewport.setZoomFactor(1 / 0.8, vc);
        v2m = viewport.getViewToModelTransform();

        assertEquals(1 / 0.8, viewport.getZoomFactor(), 1.0e-10);
        assertEquals(p(42.5, 27.5), t(v2m, vc));
        assertEquals(0.8, v2m.getScaleX(), 1.0e-10);
        assertEquals(0.8, v2m.getScaleY(), 1.0e-10);
        assertEquals(p(2.5, 7.5), t(v2m, v0));
    }

    @Test
    public void testZoomToModelPoint() {
        final DefaultViewport viewport = new DefaultViewport(new Rectangle(40, 50), true);
        final Point2D m0 = p(3, 3);
        final Point2D m1 = p(1, 1);
        Rectangle viewBounds = viewport.getViewBounds();

        viewport.setZoomFactor(2.0, 3.0, 3.0);
        AffineTransform m2v = viewport.getModelToViewTransform();

        assertEquals(2.0, viewport.getZoomFactor(), 1.0e-10);
        assertEquals(2.0, viewport.getZoomFactor(), 1.0e-10);
        assertEquals(2.0, m2v.getScaleX(), 1.0e-10);
        assertEquals(2.0, m2v.getScaleY(), 1.0e-10);
        assertEquals(p(20.0, 25.0), t(m2v, m0));
        assertEquals(p(16.0, 21.0), t(m2v, m1));
        Rectangle2D modelBounds = viewport.getViewToModelTransform().createTransformedShape(viewBounds).getBounds2D();
        double centerX = modelBounds.getCenterX();
        assertEquals(3.0, centerX, 1.0e-10);
        double centerY = modelBounds.getCenterY();
        assertEquals(3.0, centerY, 1.0e-10);
        assertEquals(new Rectangle2D.Double(-7.0, -9.5, 20.0, 25.0), modelBounds);

        viewport.setZoomFactor(2.0, 9.0, 9.0);
        m2v = viewport.getModelToViewTransform();

        assertEquals(2.0, viewport.getZoomFactor(), 1.0e-10);
        assertEquals(2.0, m2v.getScaleX(), 1.0e-10);
        assertEquals(2.0, m2v.getScaleY(), 1.0e-10);
        assertEquals(p(8.0, 13.0), t(m2v, m0));
        assertEquals(p(4.0, 9.0), t(m2v, m1));

        viewport.setZoomFactor(3.0, 3.0, 3.0);
        m2v = viewport.getModelToViewTransform();

        assertEquals(3.0, viewport.getZoomFactor(), 1.0e-10);
        assertEquals(3.0, m2v.getScaleX(), 1.0e-10);
        assertEquals(3.0, m2v.getScaleY(), 1.0e-10);
        assertEquals(p(20.0, 25.0), t(m2v, m0));
        assertEquals(p(14.0, 19.0), t(m2v, m1));
    }

    @Test
    public void testIllegalZoomFactor() {
        final DefaultViewport viewport = new DefaultViewport();

        try {
            viewport.setZoomFactor(0.0);
            fail("IAE expected");
        } catch (Exception e) {
            // ok
        }

        try {
            viewport.setZoomFactor(-4.0);
            fail("IAE expected");
        } catch (Exception e) {
            // ok
        }

        try {
            viewport.setZoomFactor(0.0, 3.0, 3.0);
            fail("IAE expected");
        } catch (IllegalArgumentException e) {
            // ok
        }

        try {
            viewport.setZoomFactor(-0.01, 3.0, 3.0);
            fail("IAE expected");
        } catch (IllegalArgumentException e) {
            // ok
        }

    }

    // V0 = {0,0}
    // U0 = T x V0
    // Uc = T x Vc
    // U0' = Uc - s'/s * (Uc - U0)
    // U0' = T' x V0
    // --> T x ((1 - s'/s) * Vc) = T' x V0
    //
    private static void zoom(AffineTransform t, Point2D vc, double s) {
        final double m00 = t.getScaleX();
        final double m10 = t.getShearY();
        final double m01 = t.getShearX();
        final double m11 = t.getScaleY();
        // todo - this code is correct only if sx and sy are the same! (rq)
        final double sx = Math.sqrt(m00 * m00 + m10 * m10);
        final double sy = Math.sqrt(m01 * m01 + m11 * m11);
        t.translate(vc.getX(), vc.getY());
        t.scale(s / sx, s / sy);
        t.translate(-vc.getX(), -vc.getY());
    }

    private static Point2D t(AffineTransform t, Point2D p) {
        return t.transform(p, null);
    }

    static Point2D p(double x, double y) {
        return new Point2D.Double(x, y);
    }

    public static Point2D getModelOffset(Viewport vp) {
        return vp.getViewToModelTransform().transform(new Point(0, 0), null);
    }
}
