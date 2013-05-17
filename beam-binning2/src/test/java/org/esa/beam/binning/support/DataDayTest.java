package org.esa.beam.binning.support;

import org.esa.beam.framework.datamodel.GeoPos;
import org.junit.Test;

/**
 * @author Norman Fomferra
 */
public class DataDayTest {



    public static class SwathElement {
        final GeoPos s;
        final GeoPos c;
        final GeoPos e;

        public SwathElement(GeoPos s, GeoPos c, GeoPos e) {
            this.s = s;
            this.c = c;
            this.e = e;
        }
    }

    public static class Orbit {
        int no;
        SwathElement[] swathElements;

        public Orbit(int no, SwathElement... swathElements) {
            this.no = no;
            this.swathElements = swathElements;
        }
    }


    @Test
    public void testOrbitGen() throws Exception {

        int numScans = 100;
        double x1 = -3;
        double x2 = +3;

        final double A = 2;
        final double B = 0.9;
        final double C = 0.25;
        final double U = 0.0;

        System.out.printf("%s\t%s\t%s\t%s\t%s\n", "i", "x", "s", "c", "e");
        for (int i = 0; i < numScans; i++) {
            double w = i / (numScans - 1.0);
            double x = x1 + w * (x2 - x1);

            double s = sigmoid(x, A, B, -C);
            double c = sigmoid(x, A, B, 0.0);
            double e = sigmoid(x, A, B, +C);

            s += U * (s + 1) * (s - 1);
            e -= U * (e + 1) * (e - 1);

            System.out.printf("%s\t%s\t%s\t%s\t%s\n", i, x, s, c, e);
        }

    }

    public static double sigmoid(double x, double a, double b, double c) {
        return b * (x - c) / Math.sqrt(1 + a * (x - c)*(x - c));
    }
}
