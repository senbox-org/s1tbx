package org.esa.snap.core.jexp.impl;

import org.esa.snap.core.jexp.EvalEnv;
import org.esa.snap.core.jexp.EvalException;
import org.esa.snap.core.jexp.ParseException;
import org.esa.snap.core.jexp.Parser;
import org.esa.snap.core.jexp.Symbol;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Norman
 */
public class TermDerivatorTest {

    @Test
    public void testConst() throws Exception {
        assertEquals("0.0", derivative("0"));
        assertEquals("0.0", derivative("1"));
        assertEquals("0.0", derivative("1.0"));
        assertEquals("0.0", derivative("true"));
        assertEquals("NaN", derivative("NaN"));
    }

    @Test
    public void testRef() throws Exception {
        assertEquals("1.0", derivative("x"));
        assertEquals("0.0", derivative("PI"));
        assertEquals("0.0", derivative("A"));
        assertEquals("0.0", derivative("B"));
    }

    @Test
    public void testNeg() throws Exception {
        assertEquals("-1.0", derivative("-x"));
        assertEquals("1.0", derivative("--x"));
        assertEquals("-1.0", derivative("---x"));
        assertEquals("Mul(-2.0,x)", derivative("-x * x"));
        assertEquals("-1.0", derivative("-(x + 1)"));
    }

    @Test
    public void testAdd() throws Exception {
        assertEquals("1.0", derivative("1 + x"));
        assertEquals("1.0", derivative("x + A"));
        assertEquals("2.0", derivative("x + x"));
        assertEquals("3.0", derivative("x + x + x"));
    }

    @Test
    public void testSub() throws Exception {
        assertEquals("-1.0", derivative("1 - x"));
        assertEquals("1.0", derivative("x - A"));
        assertEquals("0.0", derivative("x - x"));
        assertEquals("-1.0", derivative("x - x - x"));
    }

    @Test
    public void testMul() throws Exception {
        assertEquals("2.0", derivative("2 * x"));
        assertEquals("A", derivative("x * A"));
        assertEquals("Mul(2.0,x)", derivative("x * x"));
        assertEquals("Add(sq(x),Mul(Mul(2.0,x),x))", derivative("x * x * x")); // further simplify me!
    }

    @Test
    public void testDiv() throws Exception {
        assertEquals("Div(-2.0,sq(x))", derivative("2 / x"));
        assertEquals("Div(A,sq(A))", derivative("x / A")); // improve me!
        assertEquals("0.0", derivative("x / x"));
        assertEquals("Div(-1.0,sq(x))", derivative("x / x / x"));
    }

    @Test
    public void testCall() throws Exception {
        assertEquals("cos(x)", derivative("sin(x)"));

        assertEquals("Neg(sin(x))", derivative("cos(x)"));

        assertEquals("Div(1.0,sq(cos(x)))", derivative("tan(x)"));

        assertEquals("Mul(2.0,x)", derivative("sq(x)"));

        assertEquals("Div(1.0,Mul(2.0,sqrt(x)))", derivative("sqrt(x)"));

        assertEquals("exp(x)", derivative("exp(x)"));
        assertEquals("Mul(2.0,exp(Mul(2,x)))", derivative("exp(2 * x)"));
        assertEquals("Mul(Mul(2.0,exp(sq(x))),x)", derivative("exp(x * x)"));
        assertEquals("Mul(exp(x),exp(exp(x)))", derivative("exp(exp(x))"));
        assertEquals("1.0", derivative("exp(log(x))"));

        assertEquals("Div(1.0,x)", derivative("log(x)"));
        assertEquals("Div(2.0,Mul(2,x))", derivative("log(2 * x)")); // further simplify me to "Div(1.0,x)"
        assertEquals("Div(Mul(2.0,x),sq(x))", derivative("log(x * x)"));  // further simplify me to "Div(2.0,x)"
        assertEquals("Div(1.0,Mul(x,log(x)))", derivative("log(log(x))"));
        assertEquals("1.0", derivative("log(exp(x))"));

        assertEquals("Mul(3.0,sq(x))", derivative("pow(x,3)"));
        assertEquals("Mul(4.0,pow(x,3.0))", derivative("pow(x,4)"));
        assertEquals("Mul(0.3,pow(x,-0.7))", derivative("pow(x,0.3)"));
        assertEquals("Mul(Mul(0.5,pow(sin(x),-0.5)),cos(x))", derivative("pow(sin(x),0.5)"));
    }

    @Test
    public void testCond() throws Exception {
        assertEquals("Cond(GtD(A,5),1.0,Mul(2.0,x))", derivative("A > 5 ? x : sq(x)"));
        assertEquals("1.0", derivative("true ? x : sq(x)"));
        assertEquals("Mul(2.0,x)", derivative("false ? x : sq(x)"));
    }

    private Parser parser;
    private Symbol var;

    @Before
    public void setUp() throws Exception {
        var = new AbstractSymbol.D("x") {
            @Override
            public double evalD(EvalEnv env) throws EvalException {
                return Math.random();
            }
        };

        DefaultNamespace namespace = new DefaultNamespace();
        namespace.registerSymbol(var);
        namespace.registerSymbol(new AbstractSymbol.D("A") {
            @Override
            public double evalD(EvalEnv env) throws EvalException {
                return Math.random();
            }
        });
        namespace.registerSymbol(new AbstractSymbol.I("B") {
            @Override
            public int evalI(EvalEnv env) throws EvalException {
                return (int) (100 * Math.random());
            }

            @Override
            public boolean isConst() {
                return true;
            }
        });
        parser = new ParserImpl(namespace);
    }

    private String derivative(String code) throws ParseException {
        return new TermDerivator(var).apply(parser.parse(code)).toString();
    }
}
