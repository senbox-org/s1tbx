package org.jlinda.core;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.awt.*;

/**
 * User: pmar@ppolabs.com
 * Date: 2/18/11
 * Time: 7:00 PM
 */
public class WindowTest {

    private final static Window win_EXPECTED = new Window(11, 21, 103, 114);
    private final static Rectangle rect = new Rectangle(103, 11, 11, 10);
    private static Window win_ACTUAL;


    @BeforeClass
    public static void setupTestData() {

        win_ACTUAL = new Window(win_EXPECTED);

    }


    @Test
    public void testConstructor() throws Exception {

        Assert.assertEquals(win_EXPECTED.linelo, win_ACTUAL.linelo);
        Assert.assertEquals(win_EXPECTED.linehi, win_ACTUAL.linehi);
        Assert.assertEquals(win_EXPECTED.pixlo, win_ACTUAL.pixlo);
        Assert.assertEquals(win_EXPECTED.pixhi, win_ACTUAL.pixhi);

    }

    @Test
    public void testConstructorRectangle() throws Exception {
        Window win_rectangle_EXPECTED = new Window(rect);
        Assert.assertEquals(win_EXPECTED, win_rectangle_EXPECTED);
    }

    @Test
    public void testSetWindow() throws Exception {

        Assert.assertEquals(win_EXPECTED.linelo, win_ACTUAL.linelo);
        Assert.assertEquals(win_EXPECTED.linehi, win_ACTUAL.linehi);
        Assert.assertEquals(win_EXPECTED.pixlo, win_ACTUAL.pixlo);
        Assert.assertEquals(win_EXPECTED.pixhi, win_ACTUAL.pixhi);

    }

    @Test
    public void testCompareTo() throws Exception {
        Assert.assertEquals(0, win_ACTUAL.compareTo(win_EXPECTED));
    }

    @Test
    public void testClone() throws Exception {
        Window win_ACTUAL_CLONE = (Window) win_EXPECTED.clone();
        Assert.assertEquals(win_EXPECTED, win_ACTUAL_CLONE);
    }

    @Test
    public void testLines() throws Exception {
        Assert.assertEquals(win_EXPECTED.lines(), win_ACTUAL.lines());
        Assert.assertEquals(win_EXPECTED.lines(), Window.lines(win_ACTUAL));
    }

    @Test
    public void testPixels() throws Exception {
        Assert.assertEquals(win_EXPECTED.pixels(), win_ACTUAL.pixels());
        Assert.assertEquals(win_EXPECTED.pixels(), Window.pixels(win_ACTUAL));
    }

}
