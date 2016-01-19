package org.esa.snap.core.datamodel;

import org.esa.snap.core.transform.AbstractTransform2D;
import org.esa.snap.core.transform.MathTransform2D;
import org.junit.Before;
import org.junit.Test;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.opengis.referencing.operation.TransformException;

import java.awt.geom.Point2D;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class RasterDataNode_SceneTransformProviderTest {

    private Band anyRDN;
    private MathTransform2D forward;
    private MathTransform2D inverse;

    @Before
    public void setUp() throws Exception {
        anyRDN = new Band("band", ProductData.TYPE_INT8, 1, 1);
        forward = new Forward();
        inverse = new Inverse();
    }

    @Test
    public void testSetSceneToModelTransform_isInitiallyNull() {
        assertNull(anyRDN.getSceneToModelTransform());
    }

    @Test
    public void testSetModelToSceneTransform_isInitiallyNull() {
        assertNull(anyRDN.getModelToSceneTransform());
    }

    @Test
    public void testSetSceneToModelTransform_noInverse() {
        final AbstractTransform2D sceneToModelTransform = new TestTransformWithoutInverse();
        anyRDN.setSceneToModelTransform(sceneToModelTransform);

        assertSame(sceneToModelTransform, anyRDN.getSceneToModelTransform());
        assertNull(anyRDN.getModelToSceneTransform());
    }

    @Test
    public void testSetSceneToModelTransform_withInverse() {
        anyRDN.setSceneToModelTransform(forward);

        assertSame(forward, anyRDN.getSceneToModelTransform());
        assertSame(inverse, anyRDN.getModelToSceneTransform());
    }

    @Test
    public void testSetModelToSceneTransform_noInverse() {
        final AbstractTransform2D modelToSceneTransform = new TestTransformWithoutInverse();
        anyRDN.setModelToSceneTransform(modelToSceneTransform);

        assertSame(modelToSceneTransform, anyRDN.getModelToSceneTransform());
        assertNull(anyRDN.getSceneToModelTransform());
    }

    @Test
    public void testSetModelToSceneTransform_withInverse() {
        anyRDN.setModelToSceneTransform(forward);

        assertSame(forward, anyRDN.getModelToSceneTransform());
        assertSame(inverse, anyRDN.getSceneToModelTransform());
    }

    private class TestTransformWithoutInverse extends AbstractTransform2D {

        @Override
        public Point2D transform(Point2D ptSrc, Point2D ptDst) throws TransformException {
            final double x = ptSrc.getX();
            final double y = ptSrc.getY();
            ptDst.setLocation(x + (x % 1.2), y + 1 - (y % 3));
            return ptDst;
        }

        @Override
        public MathTransform2D inverse() throws NoninvertibleTransformException {
            return null;
        }

    }

    private class Forward extends AbstractTransform2D {

        @Override
        public Point2D transform(Point2D ptSrc, Point2D ptDst) throws TransformException {
            final double x = ptSrc.getX();
            final double y = ptSrc.getY();
            ptDst.setLocation(x + 1, y - 1);
            return ptDst;
        }

        @Override
        public MathTransform2D inverse() throws NoninvertibleTransformException {
            return inverse;
        }

    }

    private class Inverse extends AbstractTransform2D {

        @Override
        public Point2D transform(Point2D ptSrc, Point2D ptDst) throws TransformException {
            final double x = ptSrc.getX();
            final double y = ptSrc.getY();
            ptDst.setLocation(x + 1, y - 1);
            return ptDst;
        }

        @Override
        public MathTransform2D inverse() throws NoninvertibleTransformException {
            return forward;
        }

    }

}