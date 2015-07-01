package org.esa.snap.framework.dataop.barithm;

import com.bc.jexp.ParseException;
import com.bc.jexp.Term;
import com.bc.jexp.impl.TermDecompiler;
import org.esa.snap.framework.datamodel.Band;
import org.esa.snap.framework.datamodel.Product;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;


/**
 * Created by Norman on 13.06.2015.
 */
public class MultiOrderStandardUncertaintyGeneratorTest {


    @Test
    public void test1stOrderUncertainty() throws Exception {
        assertEquals("0.0",
                     unc1("0"));
        assertEquals("0.0",
                     unc1("a"));
        assertEquals("sqrt(sqr(ux))",
                     unc1("x"));
        assertEquals("sqrt(sqr(2.0 * x * ux))",
                     unc1("sqr(x)")); // must further simplify me one day!
        assertEquals("sqrt(sqr(3.0 * sqr(x) * ux))",
                     unc1("pow(x, 3)")); // must further simplify me one day!
        assertEquals("sqrt(sqr(ux * cos(x)))",
                     unc1("sin(x)"));
        assertEquals("sqrt(sqr(ux * exp(x)))",
                     unc1("exp(x)"));
        assertEquals("sqrt(sqr(ux / x))",
                     unc1("log(x)"));
    }

    @Test
    public void test2ndOrderUncertainty() throws Exception {
        assertEquals("0.0",
                     unc2("0"));
        assertEquals("0.0",
                     unc2("a"));
        assertEquals("sqrt(sqr(ux) + sqr(sqr(ux) / 2.0))",
                     unc2("x"));
        assertEquals("sqrt(sqr(2.0 * x * 2.0 * x * ux * ux / 2.0) + sqr(2.0 * x * ux))",  // further simplify me one day!
                     unc2("sqr(x)"));
        assertEquals("sqrt(sqr(3.0 * sqr(x) * 3.0 * sqr(x) * ux * ux / 2.0) + sqr(3.0 * sqr(x) * ux))", // further simplify me one day!
                     unc2("pow(x, 3)"));
        assertEquals("sqrt(sqr(ux * sqr(cos(x)) * ux / 2.0) + sqr(ux * cos(x)))",
                     unc2("sin(x)"));
        assertEquals("sqrt(sqr(ux * exp(2.0 * x) * ux / 2.0) + sqr(ux * exp(x)))",
                     unc2("exp(x)"));
        assertEquals("sqrt(sqr(ux / x) + sqr(sqr(ux) / (2.0 * sqr(x))))",
                     unc2("log(x)"));
    }

    Product product;

    @Before
    public void setUp() throws Exception {
        product = new Product("N", "T", 10, 10);
        Band band1 = product.addBand("x", "X");
        Band band1Err = product.addBand("ux", "0.1 * X");
        band1.addAncillaryVariable(band1Err, "uncertainty");

        Band band2 = product.addBand("y", "Y");
        Band band2Err = product.addBand("uy", "0.1 * Y");
        band2.addAncillaryVariable(band2Err, "uncertainty");

        product.addBand("a", "1.2");
        product.addBand("b", "2.3");
    }

    protected String unc1(String expression) throws ParseException {
        return propagate(new StandardUncertaintyGenerator(1, false), expression);
    }

    protected String unc2(String expression) throws ParseException {
        return propagate(new StandardUncertaintyGenerator(2, false), expression);
    }

    protected String unc3(String expression) throws ParseException {
        return propagate(new StandardUncertaintyGenerator(3, false), expression);
    }

    private String propagate(StandardUncertaintyGenerator propagator, String expression) throws ParseException {
        Term term = propagator.generateUncertainty(getProduct(), expression);
        return new TermDecompiler().decompile(term);
    }

    public Product getProduct() {
        return product;
    }
}
