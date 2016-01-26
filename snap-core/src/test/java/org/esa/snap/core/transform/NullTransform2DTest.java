package org.esa.snap.core.transform;

import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import org.junit.Before;
import org.junit.Test;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.opengis.referencing.operation.TransformException;

import java.awt.geom.Point2D;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Tonio Fincke
 */
public class NullTransform2DTest {

    private NullTransform2D nullTransform;

    @Before
    public void setUp() {
        nullTransform = new NullTransform2D();
    }

    @Test
    public void testTransform_Point2D() throws Exception {
        try {
            nullTransform.transform(new Point2D.Double(0, 0), new Point2D.Double(0, 0));
            fail("Exception expected");
        } catch (TransformException te) {
            assertEquals("No transformation given", te.getMessage());
        }
    }

    @Test
    public void testTransform_Geometry() throws Exception {
        try {
            nullTransform.transform(new Point(null, new GeometryFactory()));
            fail("Exception expected");
        } catch (TransformException te) {
            assertEquals("No transformation given", te.getMessage());
        }
    }

    @Test
    public void testInverse() throws Exception {
        try {
            nullTransform.inverse();
            fail("Exception expected");
        } catch (NoninvertibleTransformException nte) {
            assertEquals("Cannot invert transformation", nte.getMessage());
        }
    }
}