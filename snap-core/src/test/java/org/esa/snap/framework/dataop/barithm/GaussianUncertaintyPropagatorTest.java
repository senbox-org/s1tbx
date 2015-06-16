package org.esa.snap.framework.dataop.barithm;

import com.bc.jexp.ParseException;
import com.bc.jexp.Term;
import org.junit.Test;

import static org.junit.Assert.assertEquals;


/**
 * Created by Norman on 13.06.2015.
 */
public class GaussianUncertaintyPropagatorTest extends UncertaintyPropagatorTest {


    @Test
    public void testGaussianUncertaintyPropagator() throws Exception {

        assertEquals("sqrt(sqr(B1_unc))", uncert("B1"));
        assertEquals("sqrt(sqr(B2_unc))", uncert("B2"));
        assertEquals("0.0", uncert("B3"));
        assertEquals("sqrt(B4_var)", uncert("B4"));

        assertEquals("0.0", uncert("false"));
        assertEquals("0.0", uncert("10"));
        assertEquals("0.0", uncert("20.1"));
        assertEquals("0.0", uncert("NaN")); // Improve me!

        assertEquals("sqrt(sqr(B1_unc))", uncert("-B1"));
        assertEquals("sqrt(sqr(B1_unc))", uncert("--B1"));
        assertEquals("sqrt(sqr(B1_unc))", uncert("---B1"));

        assertEquals("sqrt(Add(sqr(B1_unc),sqr(B2_unc)))", uncert("B1 + B2"));
        assertEquals("sqrt(sqr(B1_unc))", uncert("B1 + B3"));
        assertEquals("sqrt(Add(sqr(B1_unc),sqr(B2_unc)))", uncert("B1 + B2 + B3"));
        assertEquals("sqrt(Add(sqr(B1_unc),sqr(B2_unc)))", uncert("B2 + B3 + B1"));

        assertEquals("sqrt(Add(sqr(B1_unc),sqr(B2_unc)))", uncert("B1 - B2")); // Improve me!
        assertEquals("sqrt(Add(sqr(B1_unc),sqr(B2_unc)))", uncert("B2 - B1")); // Improve me!
        assertEquals("sqrt(sqr(B1_unc))", uncert("B1 - B3"));
        assertEquals("sqrt(sqr(B1_unc))", uncert("B3 - B1")); // Improve me!
        assertEquals("sqrt(Add(sqr(B1_unc),sqr(B2_unc)))", uncert("B1 - B2 - B3")); // Improve me!
        assertEquals("sqrt(Add(sqr(B1_unc),sqr(B2_unc)))", uncert("B2 - B3 - B1")); // Improve me!

        assertEquals("sqrt(Add(sqr(Mul(B1,B2_unc)),sqr(Mul(B1_unc,B2))))", uncert("B1 * B2"));
        assertEquals("sqrt(sqr(Mul(B1_unc,B3)))", uncert("B1 * B3"));
        assertEquals("sqrt(Add(sqr(Mul(B1_unc,Mul(B2,B3))),sqr(Mul(B2_unc,Mul(B1,B3)))))", uncert("B1 * B2 * B3"));
        assertEquals("sqrt(Add(sqr(Mul(B1_unc,Mul(B2,B3))),sqr(Mul(B2_unc,Mul(B1,B3)))))", uncert("B2 * B3 * B1"));

        assertEquals("sqrt(Add(sqr(Mul(B1_unc,Div(B2,sqr(B2)))),sqr(Mul(B2_unc,Div(Neg(B1),sqr(B2))))))", uncert("B1 / B2")); // Improve me
        assertEquals("sqrt(Add(sqr(Mul(B1_unc,Div(Neg(B2),sqr(B1)))),sqr(Mul(B2_unc,Div(B1,sqr(B1))))))", uncert("B2 / B1")); // Improve me
        assertEquals("sqrt(sqr(Mul(B1_unc,Div(B3,sqr(B3)))))", uncert("B1 / B3")); // Improve me
        assertEquals("sqrt(sqr(Mul(B1_unc,Div(Neg(B3),sqr(B1)))))", uncert("B3 / B1"));


        assertEquals("sqrt(sqr(B1_unc))", uncert("true ? B1 : B2"));
        assertEquals("sqrt(sqr(B2_unc))", uncert("!true ? B1 : B2"));
        assertEquals("sqrt(Add(sqr(Mul(B1_unc,Cond(GtD(sin(B3),0.5),1.0,0.0))),sqr(Mul(B2_unc,Cond(GtD(sin(B3),0.5),0.0,1.0)))))",
                uncert("sin(B3) > 0.5 ? B1 : B2"));

        assertEquals("0.0", uncert("sin(B3)"));
        assertEquals("sqrt(sqr(Mul(B1_unc,cos(B1))))", uncert("sin(B1)"));

        assertEquals("0.0", uncert("cos(B3)"));
        assertEquals("sqrt(sqr(Mul(B1_unc,Neg(sin(B1)))))", uncert("cos(B1)"));

        assertEquals("0.0", uncert("tan(B3)"));
        assertEquals("sqrt(sqr(Mul(B1_unc,Div(1.0,sqr(cos(B1))))))", uncert("tan(B1)"));

        assertEquals("0.0", uncert("sqrt(B3)"));
        assertEquals("sqrt(sqr(Mul(B2_unc,Div(1.0,Mul(2.0,sqrt(B2))))))", uncert("sqrt(B2)"));

        assertEquals("sqrt(Add(sqr(Mul(B1_unc,cos(B1))),sqr(Mul(B2_unc,Div(1.0,Mul(2.0,sqrt(B2)))))))",
                uncert("sin(B1) + sqrt(B2)"));
    }

    protected UncertaintyPropagator createUncertaintyPropagator() {
        return new GaussianUncertaintyPropagator(false);
    }
}
