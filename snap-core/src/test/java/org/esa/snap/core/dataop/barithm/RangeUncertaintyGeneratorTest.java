package org.esa.snap.core.dataop.barithm;

import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Created by Norman on 13.06.2015.
 */
public class RangeUncertaintyGeneratorTest extends UncertaintyGeneratorTest {

    @Test
    public void testRangeUncertaintyPropagator() throws Exception {

        assertEquals("B1_unc", uncert("B1"));
        assertEquals("B2_unc", uncert("B2"));
        assertEquals("0.0", uncert("B3"));

        assertEquals("0.0", uncert("false"));
        assertEquals("0.0", uncert("10"));
        assertEquals("0.0", uncert("20.1"));
        assertEquals("0.0", uncert("PI"));
        assertEquals("0.0", uncert("E"));
        assertEquals("NaN", uncert("NaN"));

        assertEquals("B1_unc", uncert("-B1"));
        assertEquals("B1_unc", uncert("--B1"));
        assertEquals("B1_unc", uncert("---B1"));

        assertEquals("B1_unc + B2_unc", uncert("B1 + B2"));
        assertEquals("B1_unc", uncert("B1 + B3"));
        assertEquals("B1_unc + B2_unc", uncert("B1 + B2 + B3"));
        assertEquals("B1_unc + B2_unc", uncert("B2 + B3 + B1"));

        assertEquals("B1_unc + B2_unc", uncert("B1 - B2"));
        assertEquals("B1_unc + B2_unc", uncert("B2 - B1"));
        assertEquals("B1_unc", uncert("B1 - B3"));
        assertEquals("B1_unc", uncert("B3 - B1"));
        assertEquals("B1_unc + B2_unc", uncert("B1 - B2 - B3"));
        assertEquals("B1_unc + B2_unc", uncert("B2 - B3 - B1"));

        assertEquals("abs(B1 * B2) * (B1_unc / abs(B1) + B2_unc / abs(B2))",
                     uncert("B1 * B2"));
        assertEquals("B1_unc * abs(B1 * B3) / abs(B1)",
                     uncert("B1 * B3"));
        assertEquals("abs(B1 * B2) * abs(B1 * B2 * B3) * (B1_unc / abs(B1) + B2_unc / abs(B2)) / abs(B1 * B2)",
                     uncert("B1 * B2 * B3"));
        assertEquals("abs(B2 * B3 * B1) * (B1_unc / abs(B1) + B2_unc * abs(B2 * B3) / (abs(B2) * abs(B2 * B3)))",
                     uncert("B2 * B3 * B1"));

        assertEquals("abs(B1 / B2) * (B1_unc / abs(B1) + B2_unc / abs(B2))", uncert("B1 / B2"));
        assertEquals("abs(B2 / B1) * (B1_unc / abs(B1) + B2_unc / abs(B2))", uncert("B2 / B1"));
        assertEquals("B1_unc * abs(B1 / B3) / abs(B1)", uncert("B1 / B3"));
        assertEquals("B1_unc * abs(B3 / B1) / abs(B1)", uncert("B3 / B1"));

        assertEquals("B1_unc", uncert("true ? B1 : B2"));
        assertEquals("B2_unc", uncert("!true ? B1 : B2"));
        assertEquals("sin(B3) > 0.5 ? B1_unc : B2_unc", uncert("sin(B3) > 0.5 ? B1 : B2"));

        assertEquals("0.0", uncert("sin(B3)"));
        assertEquals("max(abs(sin(B1 - B1_unc) - sin(B1)), abs(sin(B1 + B1_unc) - sin(B1)))", uncert("sin(B1)"));
        assertEquals("max(abs(sqrt(B1 - B1_unc) - sqrt(B1)), abs(sqrt(B1 + B1_unc) - sqrt(B1)))", uncert("sqrt(B1)"));
    }

    protected UncertaintyGenerator createUncertaintyGenerator() {
        return new RangeUncertaintyGenerator();
    }
}
