package com.bc.jexp.impl;

import com.bc.jexp.*;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Norman
 */
public class TermDerivatorTest {

    @Test
    public void testConst() throws Exception {
        assertEquals("0.0", derivative("0"));
        assertEquals("0.0", derivative("1"));
        assertEquals("0.0", derivative("true"));
    }

    @Test
    public void testRef() throws Exception {
        assertEquals("1.0", derivative("x"));
        assertEquals("0.0", derivative("PI"));
        assertEquals("0.0", derivative("NaN"));
        assertEquals("0.0", derivative("A"));
        assertEquals("0.0", derivative("B"));
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
        assertEquals("Add(sqr(x),Mul(x,Mul(2.0,x)))", derivative("x * x * x")); // improve me!
    }

    @Test
    public void testDiv() throws Exception {
        assertEquals("Div(-2.0,sqr(x))", derivative("2 / x"));
        assertEquals("Div(A,sqr(A))", derivative("x / A")); // improve me!
        assertEquals("0.0", derivative("x / x"));
        assertEquals("Div(-1.0,sqr(x))", derivative("x / x / x"));
    }

    @Test
    public void testCall() throws Exception {
        assertEquals("cos(x)", derivative("sin(x)"));
        assertEquals("Neg(sin(x))", derivative("cos(x)"));
        assertEquals("Div(1.0,sqr(cos(x)))", derivative("tan(x)"));
        assertEquals("Mul(2.0,x)", derivative("sqr(x)"));
        assertEquals("Div(1.0,Mul(2.0,sqrt(x)))", derivative("sqrt(x)"));
        assertEquals("Mul(3.0,sqr(x))", derivative("pow(x,3)"));
        assertEquals("Mul(4.0,pow(x,3.0))", derivative("pow(x,4)"));
        assertEquals("Mul(0.3,pow(x,-0.7))", derivative("pow(x,0.3)"));
        assertEquals("Mul(cos(x),Mul(0.5,pow(sin(x),-0.5)))", derivative("pow(sin(x),0.5)"));
    }

    @Test
    public void testCond() throws Exception {
        assertEquals("Cond(GtD(A,5),1.0,Mul(2.0,x))", derivative("A > 5 ? x : sqr(x)"));
        assertEquals("1.0", derivative("true ? x : sqr(x)"));
        assertEquals("Mul(2.0,x)", derivative("false ? x : sqr(x)"));
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
        return new TermDerivator(var).derivative(parser.parse(code)).toString();
    }
}
