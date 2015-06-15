package org.esa.snap.framework.dataop.barithm;

import org.junit.Test;

import static org.junit.Assert.assertEquals;


/**
 * Created by Norman on 13.06.2015.
 */
public class RangeUncertaintyPropagatorTest extends UncertaintyPropagatorTest {

    @Test
    public void testRangeUncertaintyPropagator() throws Exception {

        assertEquals("B1_unc", uncert("B1"));
        assertEquals("B2_unc", uncert("B2"));
        assertEquals("0.0", uncert("B3"));
        assertEquals("sqrt(B4_var)", uncert("B4"));

        assertEquals("0.0", uncert("false"));
        assertEquals("0.0", uncert("10"));
        assertEquals("0.0", uncert("20.1"));
        //assertEquals("NaN", uncert(product, "NaN"));

        assertEquals("B1_unc", uncert("-B1"));
        assertEquals("B1_unc", uncert("--B1"));
        assertEquals("B1_unc", uncert("---B1"));

        assertEquals("Add(B1_unc,B2_unc)", uncert("B1 + B2"));
        assertEquals("B1_unc", uncert("B1 + B3"));
        assertEquals("Add(B1_unc,B2_unc)", uncert("B1 + B2 + B3"));
        assertEquals("Add(B1_unc,B2_unc)", uncert("B2 + B3 + B1"));

        assertEquals("Add(B1_unc,B2_unc)", uncert("B1 - B2"));
        assertEquals("Add(B1_unc,B2_unc)", uncert("B2 - B1"));
        assertEquals("B1_unc", uncert("B1 - B3"));
        assertEquals("B1_unc", uncert("B3 - B1"));
        assertEquals("Add(B1_unc,B2_unc)", uncert("B1 - B2 - B3"));
        assertEquals("Add(B1_unc,B2_unc)", uncert("B2 - B3 - B1"));

        assertEquals("Add(Div(B1_unc,abs(B1)),Div(B2_unc,abs(B2)))", uncert("B1 * B2"));
        assertEquals("Div(B1_unc,abs(B1))", uncert("B1 * B3"));
        assertEquals("Div(Add(Div(B1_unc,abs(B1)),Div(B2_unc,abs(B2))),abs(Mul(B1,B2)))", uncert( "B1 * B2 * B3"));
        assertEquals("Add(Div(B1_unc,abs(B1)),Div(Div(B2_unc,abs(B2)),abs(Mul(B2,B3))))", uncert("B2 * B3 * B1"));

        assertEquals("Add(Div(B1_unc,abs(B1)),Div(B2_unc,abs(B2)))", uncert("B1 / B2"));
        assertEquals("Add(Div(B1_unc,abs(B1)),Div(B2_unc,abs(B2)))", uncert("B2 / B1"));
        assertEquals("Div(B1_unc,abs(B1))", uncert("B1 / B3"));
        assertEquals("Div(B1_unc,abs(B1))", uncert("B3 / B1"));

        assertEquals("B1_unc", uncert("true ? B1 : B2"));
        assertEquals("B2_unc", uncert("!true ? B1 : B2"));
        assertEquals("Cond(GtD(sin(B3),0.5),B1_unc,B2_unc)", uncert("sin(B3) > 0.5 ? B1 : B2"));

        assertEquals("0.0", uncert("sin(B3)"));
        assertEquals("Mul(0.5,abs(Sub(sin(Sub(B1,B1_unc)),sin(Add(B1,B1_unc)))))", uncert("sin(B1)"));
        assertEquals("Mul(0.5,abs(Sub(sqrt(Sub(B1,B1_unc)),sqrt(Add(B1,B1_unc)))))", uncert("sqrt(B1)"));
    }

    protected UncertaintyPropagator createUncertaintyPropagator() {
        return new RangeUncertaintyPropagator();
    }
}
