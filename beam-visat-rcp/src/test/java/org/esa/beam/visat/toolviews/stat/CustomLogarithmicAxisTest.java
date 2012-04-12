package org.esa.beam.visat.toolviews.stat;

import org.jfree.chart.axis.NumberTick;
import org.jfree.ui.RectangleEdge;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Date: 11.04.12
 * Time: 13:18
 *
 * @author olafd
 */
public class CustomLogarithmicAxisTest {

    @Test
    public void testRefreshTicks() throws Exception {
        CustomLogarithmicAxis testAxis = new CustomLogarithmicAxis("testAxis");

        // axis range includes 3 power of 10 values: 1, 10, 100
        testAxis.setRange(0.3, 300.0);
        List ticks = testAxis.refreshTicks(RectangleEdge.TOP, CustomLogarithmicAxis.HORIZONTAL);
        assertNotNull(ticks);
        assertEquals(28, ticks.size());
        // we expect labels at the power of 10 values only
        assertEquals("", ((NumberTick) ticks.get(0)).getText());
        assertEquals("", ((NumberTick) ticks.get(1)).getText());
        assertEquals("1", ((NumberTick) ticks.get(7)).getText());
        assertEquals("", ((NumberTick) ticks.get(9)).getText());
        assertEquals("10", ((NumberTick) ticks.get(16)).getText());
        assertEquals("", ((NumberTick) ticks.get(18)).getText());
        assertEquals("100", ((NumberTick) ticks.get(25)).getText());
        assertEquals("", ((NumberTick) ticks.get(26)).getText());

        // axis range includes 2 power of 10 values: 1, 10
        testAxis.setRange(0.3, 30.0);
        ticks = testAxis.refreshTicks(RectangleEdge.TOP, CustomLogarithmicAxis.HORIZONTAL);
        assertNotNull(ticks);
        assertEquals(19, ticks.size());
        // we expect labels at the power of 10 values only
        assertEquals("", ((NumberTick) ticks.get(0)).getText());
        assertEquals("", ((NumberTick) ticks.get(1)).getText());
        assertEquals("1", ((NumberTick) ticks.get(7)).getText());
        assertEquals("", ((NumberTick) ticks.get(9)).getText());
        assertEquals("10", ((NumberTick) ticks.get(16)).getText());
        assertEquals("", ((NumberTick) ticks.get(18)).getText());

        // axis range includes 1 power of 10 values: 1
        testAxis.setRange(0.3, 3.0);
        ticks = testAxis.refreshTicks(RectangleEdge.TOP, CustomLogarithmicAxis.HORIZONTAL);
        assertNotNull(ticks);
        assertEquals(10, ticks.size());
        // we expect labels at every tick!
        // todo: clarify why these tests work locally, but not on buildserver (who expects "0.3", "0.4", ...)
//        assertEquals("0,3", ((NumberTick) ticks.get(0)).getText());
//        assertEquals("0,4", ((NumberTick) ticks.get(1)).getText());
//        assertEquals("0,5", ((NumberTick) ticks.get(2)).getText());
//        assertEquals("0,6", ((NumberTick) ticks.get(3)).getText());
//        assertEquals("0,7", ((NumberTick) ticks.get(4)).getText());
//        assertEquals("0,8", ((NumberTick) ticks.get(5)).getText());
//        assertEquals("0,9", ((NumberTick) ticks.get(6)).getText());
        assertEquals(0.3, ((NumberTick) ticks.get(0)).getValue(), 1.E-6);
        assertEquals(0.4, ((NumberTick) ticks.get(1)).getValue(), 1.E-6);
        assertEquals(0.5, ((NumberTick) ticks.get(2)).getValue(), 1.E-6);
        assertEquals(0.6, ((NumberTick) ticks.get(3)).getValue(), 1.E-6);
        assertEquals(0.7, ((NumberTick) ticks.get(4)).getValue(), 1.E-6);
        assertEquals(0.8, ((NumberTick) ticks.get(5)).getValue(), 1.E-6);
        assertEquals(0.9, ((NumberTick) ticks.get(6)).getValue(), 1.E-6);
        assertEquals("1", ((NumberTick) ticks.get(7)).getText());
        assertEquals("2", ((NumberTick) ticks.get(8)).getText());
        assertEquals("3", ((NumberTick) ticks.get(9)).getText());

        // axis range includes no power of 10 values
        testAxis.setRange(0.3, 0.8);
        ticks = testAxis.refreshTicks(RectangleEdge.TOP, CustomLogarithmicAxis.HORIZONTAL);
        assertNotNull(ticks);
        assertEquals(6, ticks.size());
        // we expect labels at every tick!
        // todo: clarify why these tests work locally, but not on buildserver (who expects "0.3", "0.4", ...)
//        assertEquals("0,3", ((NumberTick) ticks.get(0)).getText());
//        assertEquals("0,4", ((NumberTick) ticks.get(1)).getText());
//        assertEquals("0,5", ((NumberTick) ticks.get(2)).getText());
//        assertEquals("0,6", ((NumberTick) ticks.get(3)).getText());
//        assertEquals("0,7", ((NumberTick) ticks.get(4)).getText());
//        assertEquals("0,8", ((NumberTick) ticks.get(5)).getText());
        assertEquals(0.3, ((NumberTick) ticks.get(0)).getValue(), 1.E-6);
        assertEquals(0.4, ((NumberTick) ticks.get(1)).getValue(), 1.E-6);
        assertEquals(0.5, ((NumberTick) ticks.get(2)).getValue(), 1.E-6);
        assertEquals(0.6, ((NumberTick) ticks.get(3)).getValue(), 1.E-6);
        assertEquals(0.7, ((NumberTick) ticks.get(4)).getValue(), 1.E-6);
        assertEquals(0.8, ((NumberTick) ticks.get(5)).getValue(), 1.E-6);
    }

    @Test
    public void testRefreshTicksForNegativeAxisRange() throws Exception {
        CustomLogarithmicAxis testAxis = new CustomLogarithmicAxis("testAxis");
        testAxis.setRange(-300, -3);
        List ticks = testAxis.refreshTicks(RectangleEdge.TOP, CustomLogarithmicAxis.HORIZONTAL);
        assertNotNull(ticks);
        assertEquals(19, ticks.size());
        // we expect labels at the power of 10 values only
        assertEquals("", ((NumberTick) ticks.get(0)).getText());
        assertEquals("", ((NumberTick) ticks.get(1)).getText());
        assertEquals("-10", ((NumberTick) ticks.get(7)).getText());
        assertEquals("", ((NumberTick) ticks.get(9)).getText());
        assertEquals("-100", ((NumberTick) ticks.get(16)).getText());
        assertEquals("", ((NumberTick) ticks.get(18)).getText());
    }

    @Test
    public void testRefreshTicksForInvalidAxisRange() throws Exception {
        CustomLogarithmicAxis testAxis = new CustomLogarithmicAxis("testAxis");
        // axis range maximum/minimum switched
        try {
            testAxis.setRange(30, 3);
        } catch (IllegalArgumentException expected) {
            assertEquals("Range(double, double): require lower (30.0) <= upper (3.0).", expected.getMessage());
        }

        // axis range includes zero
        testAxis.setRange(-30, 30);
        List ticks = testAxis.refreshTicks(RectangleEdge.TOP, CustomLogarithmicAxis.HORIZONTAL);
        assertNotNull(ticks);
        assertEquals(0, ticks.size());
    }

    @Test
    public void testRefreshTicksVerticalAxisOrientation() throws Exception {
        CustomLogarithmicAxis testAxis = new CustomLogarithmicAxis("testAxis");
        testAxis.setRange(-300, -3);
        List ticks = testAxis.refreshTicks(RectangleEdge.LEFT, CustomLogarithmicAxis.VERTICAL);
        assertNotNull(ticks);
        assertEquals(19, ticks.size());
        assertEquals("", ((NumberTick) ticks.get(0)).getText());
        assertEquals("", ((NumberTick) ticks.get(1)).getText());
        assertEquals("-10", ((NumberTick) ticks.get(7)).getText());
        assertEquals("", ((NumberTick) ticks.get(9)).getText());
        assertEquals("-100", ((NumberTick) ticks.get(16)).getText());
        assertEquals("", ((NumberTick) ticks.get(18)).getText());
    }
}
