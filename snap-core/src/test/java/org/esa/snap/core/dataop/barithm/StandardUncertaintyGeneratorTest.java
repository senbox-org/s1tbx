package org.esa.snap.core.dataop.barithm;

import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Created by Norman on 13.06.2015.
 */
public class StandardUncertaintyGeneratorTest extends UncertaintyGeneratorTest {


    @Test
    public void testGaussianUncertaintyPropagator() throws Exception {

        assertEquals("sqrt(sq(B1_unc))", uncert("B1"));
        assertEquals("sqrt(sq(B2_unc))", uncert("B2"));
        assertEquals("0.0", uncert("B3"));

        assertEquals("0.0", uncert("false"));
        assertEquals("0.0", uncert("10"));
        assertEquals("0.0", uncert("20.1"));
        assertEquals("0.0", uncert("PI"));
        assertEquals("0.0", uncert("E"));
        assertEquals("NaN", uncert("NaN"));

        assertEquals("sqrt(sq(B1_unc))", uncert("-B1"));
        assertEquals("sqrt(sq(B1_unc))", uncert("--B1"));
        assertEquals("sqrt(sq(B1_unc))", uncert("---B1"));

        assertEquals("sqrt(sq(B1_unc) + sq(B2_unc))", uncert("B1 + B2"));
        assertEquals("sqrt(sq(B1_unc))", uncert("B1 + B3"));
        assertEquals("sqrt(sq(B1_unc) + sq(B2_unc))", uncert("B1 + B2 + B3"));
        assertEquals("sqrt(sq(B1_unc) + sq(B2_unc))", uncert("B2 + B3 + B1"));

        assertEquals("sqrt(sq(B1_unc) + sq(B2_unc))", uncert("B1 - B2"));
        assertEquals("sqrt(sq(B1_unc) + sq(B2_unc))", uncert("B2 - B1"));
        assertEquals("sqrt(sq(B1_unc))", uncert("B1 - B3"));
        assertEquals("sqrt(sq(B1_unc))", uncert("B3 - B1"));
        assertEquals("sqrt(sq(B1_unc) + sq(B2_unc))", uncert("B1 - B2 - B3"));
        assertEquals("sqrt(sq(B1_unc) + sq(B2_unc))", uncert("B2 - B3 - B1"));

        assertEquals("sqrt(sq(B1 * B2_unc) + sq(B1_unc * B2))", uncert("B1 * B2"));
        assertEquals("sqrt(sq(B1_unc * B3))", uncert("B1 * B3"));
        assertEquals("sqrt(sq(B1 * B3 * B2_unc) + sq(B2 * B3 * B1_unc))", uncert("B1 * B2 * B3"));
        assertEquals("sqrt(sq(B1 * B3 * B2_unc) + sq(B2 * B3 * B1_unc))", uncert("B2 * B3 * B1"));

        assertEquals("sqrt(sq(B1_unc * B2 / sq(B2)) + sq(B2_unc * -B1 / sq(B2)))", uncert("B1 / B2"));
        assertEquals("sqrt(sq(B1 * B2_unc / sq(B1)) + sq(B1_unc * -B2 / sq(B1)))", uncert("B2 / B1"));
        assertEquals("sqrt(sq(B1_unc * B3 / sq(B3)))", uncert("B1 / B3"));
        assertEquals("sqrt(sq(B1_unc * -B3 / sq(B1)))", uncert("B3 / B1"));


        assertEquals("sqrt(sq(B1_unc))", uncert("true ? B1 : B2"));
        assertEquals("sqrt(sq(B2_unc))", uncert("!true ? B1 : B2"));
        assertEquals("sqrt(sq(B1_unc * (sin(B3) > 0.5 ? 1.0 : 0.0)) + sq(B2_unc * (sin(B3) > 0.5 ? 0.0 : 1.0)))",
                     uncert("sin(B3) > 0.5 ? B1 : B2"));

        assertEquals("0.0", uncert("sin(B3)"));
        assertEquals("sqrt(sq(B1_unc * cos(B1)))", uncert("sin(B1)"));

        assertEquals("0.0", uncert("cos(B3)"));
        assertEquals("sqrt(sq(B1_unc * -sin(B1)))", uncert("cos(B1)"));

        assertEquals("0.0", uncert("tan(B3)"));
        assertEquals("sqrt(sq(B1_unc / sq(cos(B1))))", uncert("tan(B1)"));

        assertEquals("0.0", uncert("sqrt(B3)"));
        assertEquals("sqrt(sq(B2_unc / (2.0 * sqrt(B2))))", uncert("sqrt(B2)"));

        assertEquals("sqrt(sq(B2_unc / (2.0 * sqrt(B2))) + sq(B1_unc * cos(B1)))",
                     uncert("sin(B1) + sqrt(B2)"));
    }

    @Override
    protected UncertaintyGenerator createUncertaintyGenerator() {
        return new StandardUncertaintyGenerator(1, false);
    }
}
