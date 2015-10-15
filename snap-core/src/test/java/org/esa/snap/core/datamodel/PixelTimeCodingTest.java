package org.esa.snap.core.datamodel;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * Created by Sabine on 21.08.2015.
 */
public class PixelTimeCodingTest {

    @Test
    public void testCSVProductReaderBehavior() throws Exception {
        // 2x2 = 4 pixel but only three doubles applied.
        // this needed by CSVProductReader because the given samples can be less then rectangles pixel count
        double[] onlyThreeDoubles = {3.0, 4.0, 5.0};
        TimeCoding timeCoding = new PixelTimeCoding(onlyThreeDoubles, 2, 2);

        assertEquals("3.0", "" + getMjd(0.5, 0.5, timeCoding));
        assertEquals("4.0", "" + getMjd(1.5, 0.5, timeCoding));
        assertEquals("5.0", "" + getMjd(0.5, 1.5, timeCoding));
        assertEquals("NaN", "" + getMjd(1.5, 1.5, timeCoding));
    }

    @Test
    public void testImplementsTimeCoding() {
        TimeCoding timeCoding = new PixelTimeCoding(new double[]{3.0, 4.0, 5.0}, 2, 2);

        assertThat(timeCoding, is(TimeCoding.class));
    }

    @Test
    public void testCornerOn2x2Coding() {
        TimeCoding timeCoding = new PixelTimeCoding(new double[]{3, 6, 2, 4}, 2, 2);

        assertEquals("3.0", "" + getMjd(0.0, 0.0, timeCoding));
        assertEquals("6.0", "" + getMjd(2.0, 0.0, timeCoding));
        assertEquals("2.0", "" + getMjd(0.0, 2.0, timeCoding));
        assertEquals("4.0", "" + getMjd(2.0, 2.0, timeCoding));

        assertEquals("NaN", "" + getMjd(0.0, -0.1, timeCoding));
        assertEquals("NaN", "" + getMjd(0.0, 2.1, timeCoding));
        assertEquals("NaN", "" + getMjd(2.0, -0.1, timeCoding));
        assertEquals("NaN", "" + getMjd(2.0, 2.1, timeCoding));
        assertEquals("NaN", "" + getMjd(-0.1, 0.0, timeCoding));
        assertEquals("NaN", "" + getMjd(2.1, 0.0, timeCoding));
        assertEquals("NaN", "" + getMjd(-0.1, 2.0, timeCoding));
        assertEquals("NaN", "" + getMjd(2.1, 2.0, timeCoding));
    }

    private double getMjd(double x, double y, TimeCoding timeCoding) {
        return timeCoding.getMJD(new PixelPos(x, y));
    }
}