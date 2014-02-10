package org.esa.pfa.fe.op;

import org.junit.Assert;
import org.junit.Test;

import java.awt.*;

/**
 * @author Norman Fomferra
 */
public class HsbTest {
    @Test
    public void testThatHsbConversionIsSameAsFromAwt() {

        float[] hsb1 = Color.RGBtoHSB(25, 145, 93, null);
        Assert.assertEquals(0.4278f, hsb1[0], 1.e-4d);
        Assert.assertEquals(0.8276f, hsb1[1], 1.e-4d);
        Assert.assertEquals(0.5686f, hsb1[2], 1.e-4d);


        float[] hsb2 = myRGBtoHSB(25, 145, 93, null);
        Assert.assertEquals(0.4278f, hsb2[0], 1.e-4d);
        Assert.assertEquals(0.8276f, hsb2[1], 1.e-4d);
        Assert.assertEquals(0.5686f, hsb2[2], 1.e-4d);
    }


    public static float[] myRGBtoHSB(int r, int g, int b, float[] hsbvals) {
        return myRGBtoHSB(r / 255f, g / 255f, b / 255f, hsbvals);
    }

    public static float[] myRGBtoHSB(float r, float g, float b, float[] hsbvals) {
        float hue, saturation, brightness;
        if (hsbvals == null) {
            hsbvals = new float[3];
        }
        float cmax = (r > g) ? r : g;
        if (b > cmax) {
            cmax = b;
        }
        float cmin = (r < g) ? r : g;
        if (b < cmin) {
            cmin = b;
        }

        brightness = cmax;
        if (cmax != 0) {
            saturation = (cmax - cmin) / cmax;
        } else {
            saturation = 0;
        }
        if (saturation == 0) {
            hue = 0;
        } else {
            float rc = (cmax - r) / (cmax - cmin);
            float gc = (cmax - g) / (cmax - cmin);
            float bc = (cmax - b) / (cmax - cmin);
            if (r == cmax) {
                hue = bc - gc;
            } else if (g == cmax) {
                hue = 2.0f + rc - bc;
            } else {
                hue = 4.0f + gc - rc;
            }
            hue = hue / 6.0f;
            if (hue < 0) {
                hue = hue + 1.0f;
            }
        }
        hsbvals[0] = hue;
        hsbvals[1] = saturation;
        hsbvals[2] = brightness;
        return hsbvals;
    }
}
