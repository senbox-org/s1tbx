package org.jlinda.core;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;

/**
 * User: pmar@ppolabs.com
 * Date: 2/18/11
 * Time: 5:41 PM
 */
public class PointTest {

    private double[] array = {1, 2, 3};
    private Point X = new Point(array);
    private Point Y = new Point(4,5,6);
    private Point Z = new Point(Y);
    private Point testPoint;
    private Point refPoint;
    private double testDouble;
    private double refDouble;
    private double delta = 10e-6;

    @Test
    public void testMin() throws Exception {
        testPoint = Z.min(X);
        refPoint = new Point(3, 3, 3);
        assertEquals(refPoint, testPoint);
    }

    @Test
    public void testPlus() throws Exception {
        testPoint = Z.plus(X);
        refPoint = new Point(5, 7, 9);
        assertEquals(refPoint, testPoint);
    }

    @Test
    public void testMult() throws Exception {
        testPoint = X.mult(Z);
        refPoint = new Point(4, 10, 18);
        assertEquals(refPoint, testPoint);
    }

    @Test
    public void testDiv() throws Exception {
        testPoint = X.div(Z);
        refPoint = new Point(0.25, 0.4, 0.5);
        assertEquals(refPoint, testPoint);
    }

    @Test
    public void testIn() throws Exception {
        testDouble = X.in(Z);
        refDouble = 32;
        assertEquals(refDouble, testDouble);
    }

    @Test
    public void testMultByScalar() throws Exception {
        testPoint = X.multByScalar(4.25);
        refPoint = new Point(4.25, 8.5, 12.75);
        assertEquals(refPoint, testPoint);
    }

    @Test
    public void testDivByScalar() throws Exception {
        testPoint = X.divByScalar(0.25);
        refPoint = new Point(4, 8, 12);
        assertEquals(refPoint, testPoint);
    }

    @Test
    public void testNegative() throws Exception {
        testPoint = X.negative();
        refPoint = X.multByScalar(-1);
        assertEquals(refPoint, testPoint);
    }

    @Test
    public void testNorm2() throws Exception {
        testDouble = X.norm2();
        refDouble = 14;
        assertEquals(refDouble, testDouble);
    }

    @Test
    public void testNorm() throws Exception {
        testDouble = (float)X.norm();
        refDouble = 3.741657387;
        assertEquals(refDouble, testDouble, delta);
    }

    @Test
    public void testNormalize() throws Exception {
        testPoint = X.normalize();
        refPoint = new Point(0.26726124, 0.53452248, 0.80178372);
        assertEquals(refPoint.x, testPoint.x, delta);
        assertEquals(refPoint.y, testPoint.y, delta);
        assertEquals(refPoint.z, testPoint.z, delta);
    }

    @Test
    public void testDistance() throws Exception {
        testDouble = X.distance(Z);
        refDouble = 5.19615242270663;
        assertEquals(refDouble, testDouble, delta);
    }

    @Test
    public void testScale() throws Exception {
        testPoint = X.scale(5);
        refPoint = new Point(5, 10, 15);
        assertEquals(testPoint, refPoint);
    }

    @Test
    public void testOut() throws Exception {
        testPoint = X.out(Z);
        refPoint = new Point(-3, 6, -3);
        assertEquals(testPoint, refPoint);
    }


    @Test
    public void testAngle() throws Exception {
        testDouble = X.angle(Z);
        refDouble = 0.225726;
        assertEquals(refDouble,testDouble,delta);

    }

}
