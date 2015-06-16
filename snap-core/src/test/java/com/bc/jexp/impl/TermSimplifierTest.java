package com.bc.jexp.impl;

import com.bc.jexp.EvalEnv;
import com.bc.jexp.EvalException;
import com.bc.jexp.ParseException;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Norman
 */
public class TermSimplifierTest {
    @Test
    public void testConst() throws Exception {
        assertEquals("0", simplify("0"));
        assertEquals("1", simplify("1"));
        assertEquals("10.3", simplify("10.3"));
        assertEquals("true", simplify("true"));
        assertEquals("PI", simplify("PI"));
    }

    @Test
    public void testSymbol() throws Exception {
        assertEquals("A", simplify("A"));
        assertEquals("B", simplify("B"));
    }

    @Test
    public void testCall() throws Exception {

        assertEquals("0.0", simplify("sin(0)"));
        assertEquals("sin(PI)", simplify("sin(PI)"));
        assertEquals("sin(A)", simplify("sin(A)"));

        assertEquals("0.0", simplify("sqrt(0)"));
        assertEquals("0.0", simplify("sqrt(0.0)"));
        assertEquals("sqrt(A)", simplify("sqrt(A)"));
        assertEquals("sqrt(A)", simplify("sqrt(A + 0)"));
        assertEquals("pow(A,0.25)", simplify("sqrt(sqrt(A))"));
        assertEquals("sqrt(sqr(A))", simplify("sqrt(sqr(A))")); // don't suppress sign!
        assertEquals("sqrt(sqr(A))", simplify("sqrt(pow(A,2))")); // don't suppress sign!
        assertEquals("sqrt(sqr(A))", simplify("sqrt(sqr(A))")); // don't suppress sign!
        assertEquals("sqrt(pow(A,4))", simplify("sqrt(pow(A, 4))")); // don't suppress sign!
        assertEquals("pow(A,2.5)", simplify("sqrt(pow(A, 5))"));

        assertEquals("0.0", simplify("sqr(0)"));
        assertEquals("1.0", simplify("sqr(1)"));
        assertEquals("4.0", simplify("sqr(2)"));
        assertEquals("sqr(A)", simplify("sqr(-A)"));
        assertEquals("sqr(sqrt(A))", simplify("sqr(sqrt(A))"));  // don't suppress sign!
        assertEquals("sqr(pow(A,3))", simplify("sqr(pow(A,3))")); // don't suppress sign!
        assertEquals("sqr(sqrt(A))", simplify("sqr(pow(A,0.5))")); // don't suppress sign!

        assertEquals("1.0", simplify("pow(A,0)"));
        assertEquals("A", simplify("pow(A,1)"));
        assertEquals("0.0", simplify("pow(0,A)"));
        assertEquals("1.0", simplify("pow(1,A)"));
        assertEquals("exp(A)", simplify("pow(E,A)"));
        assertEquals("A", simplify("pow(E,log(A))"));
        assertEquals("pow(A,sqr(A))", simplify("pow(A,sqr(A))"));
        assertEquals("pow(sqr(A),A)", simplify("pow(sqr(A),A)")); // don't suppress sign!
        assertEquals("sqr(sqrt(A))", simplify("pow(pow(A,0.5),2)")); // don't suppress sign!
        assertEquals("sqrt(sqr(A))", simplify("pow(pow(A,2),0.5)")); // don't suppress sign!
        assertEquals("sqrt(sqr(A))", simplify("pow(sqr(A),0.5)")); // don't suppress sign!
        assertEquals("exp(Mul(3,A))", simplify("pow(exp(A),3)"));
        assertEquals("pow(A,Mul(0.5,A))", simplify("pow(sqrt(A),A)"));


        assertEquals("3", simplify("min(3,4)"));
        assertEquals("3.0", simplify("min(3,4.0)"));

        assertEquals("4", simplify("max(3,4)"));
        assertEquals("4.0", simplify("max(3,4.0)"));

        assertEquals("5.0", simplify("abs(5.0)"));
        assertEquals("5", simplify("abs(-5)"));
        assertEquals("abs(A)", simplify("abs(-A)"));

        assertEquals("1.0", simplify("exp(0)"));
        assertEquals("E", simplify("exp(1)"));
        assertEquals("3.2", simplify("exp(log(3.2))"));
        assertEquals("A", simplify("exp(log(A))"));

        assertEquals("0.0", simplify("log(1)"));
        assertEquals("1.0", simplify("log(E)"));
        assertEquals("2.3", simplify("log(exp(2.3))"));
        assertEquals("A", simplify("log(exp(A))"));

    }

    @Test
    public void testCond() throws Exception {
        assertEquals("A", simplify("true ? A : B"));
        assertEquals("A", simplify("!false ? A : B"));
        assertEquals("Add(1,A)", simplify("true ? A + 1 : B - 2"));
        assertEquals("B", simplify("false ? A : B"));
        assertEquals("B", simplify("!true ? A : B"));
        assertEquals("Sub(B,2)", simplify("false ? A + 1 : B - 2"));
        assertEquals("Cond(GtD(sin(A),2),Add(2,A),Add(1,A))", simplify("sin(A) > 2 ? A + 2 : A + 1"));
        assertEquals("Add(2,A)", simplify("sin(A) > 2 ? A + 2 : A + 2"));

    }

    @Test
    public void testNeg() throws Exception {
        assertEquals("-3", simplify("-3"));
        assertEquals("3", simplify("--3"));
        assertEquals("Neg(A)", simplify("-A"));
        assertEquals("A", simplify("--A"));
        assertEquals("Neg(A)", simplify("---A"));
        assertEquals("Neg(A)", simplify("-(A + 0)"));
    }

    @Test
    public void testAdd() throws Exception {
        assertEquals("3", simplify("1 + 2"));
        assertEquals("Add(A,B)", simplify("A + B"));
        assertEquals("Add(A,B)", simplify("B + A"));
        assertEquals("A", simplify("A + 0"));
        assertEquals("Add(1,A)", simplify("A + 1"));
        assertEquals("Add(1,A)", simplify("1 + A"));
        assertEquals("A", simplify("0 + A"));
        assertEquals("Add(1,A)", simplify("1 + A"));
        assertEquals("A", simplify("A + (B * 0)"));
        assertEquals("Add(A,B)", simplify("A + (B * 1)"));
        assertEquals("Add(A,B)", simplify("(A * 1) + B"));
        assertEquals("Mul(2.0,A)", simplify("A + A"));
        assertEquals("Mul(2.0,Add(2,A))", simplify("(A + 2) + (2 + A)"));
        assertEquals("Mul(2,B)", simplify("B + B"));
        assertEquals("Mul(2,Add(2,B))", simplify("(B + 2) + (2 + B)"));
    }

    @Test
    public void testSub() throws Exception {
        assertEquals("-1", simplify("1 - 2"));
        assertEquals("Sub(A,B)", simplify("A - B"));
        assertEquals("A", simplify("A - 0"));
        assertEquals("Sub(A,1)", simplify("A - 1"));
        assertEquals("Neg(A)", simplify("0 - A"));
        assertEquals("A", simplify("A - (B * 0)"));
        assertEquals("Sub(A,B)", simplify("A - (B * 1)"));
        assertEquals("Neg(B)", simplify("(A * 0) - B"));
        assertEquals("Sub(A,B)", simplify("(A * 1) - B"));
        assertEquals("0.0", simplify("A - A"));
        assertEquals("0.0", simplify("(A + 2) - (2 + A)"));
        assertEquals("0", simplify("B - B"));
        assertEquals("0", simplify("(B + 2) - (2 + B)"));
    }

    @Test
    public void testMul() throws Exception {
        assertEquals("12", simplify("3 * 4"));
        assertEquals("2.5", simplify("5 * 0.5"));
        assertEquals("Mul(A,B)", simplify("A * B"));
        assertEquals("Mul(A,B)", simplify("B * A"));
        assertEquals("0", simplify("A * 0"));
        assertEquals("A", simplify("A * 1"));
        assertEquals("Mul(2,A)", simplify("A * 2"));
        assertEquals("0", simplify("0 * A"));
        assertEquals("A", simplify("1 * A"));
        assertEquals("Mul(A,B)", simplify("A * (B + 0)"));
        assertEquals("Mul(A,B)", simplify("(A + 0) * B"));
        //assertEquals("pow(A, 2)", simplify("A * A"));
    }

    @Test
    public void testDiv() throws Exception {
        assertEquals("3", simplify("12 / 4"));
        assertEquals("5.0", simplify("2.5 / 0.5"));
        assertEquals("Div(A,B)", simplify("A / B"));
        assertEquals("Div(B,A)", simplify("B / A"));
        assertEquals("NaN", simplify("0 / 0"));
        assertEquals("NaN", simplify("1 / 0"));
        assertEquals("NaN", simplify("A / 0"));
        assertEquals("NaN", simplify("(A + 1) / 0"));
        assertEquals("A", simplify("A / 1"));
        assertEquals("0", simplify("0 / A"));
        assertEquals("Div(A,B)", simplify("A / (B + 0)"));
        assertEquals("Div(A,B)", simplify("(A + 0) / B"));
        assertEquals("1.0", simplify("A / A"));
        assertEquals("1.0", simplify("(A + 1) / (A + 1)"));
        assertEquals("1", simplify("B / B"));
        assertEquals("1", simplify("(B + 1) / (B + 1)"));
    }

    @Test
    public void testEq() throws Exception {
        assertEquals("true", simplify("true == true"));
        assertEquals("true", simplify("3 == 3"));
        assertEquals("true", simplify("3.2 == 3.2"));
        assertEquals("true", simplify("A == A"));
        assertEquals("true", simplify("(A + 2) == (2 + A)"));

        assertEquals("false", simplify("true == false"));
        assertEquals("false", simplify("3 == 2"));
        assertEquals("false", simplify("3.2 == 3.1"));
        assertEquals("EqD(A,B)", simplify("A == B"));
        assertEquals("EqD(A,1)", simplify("A == 1"));
        assertEquals("EqD(Add(2,A),Add(1,A))", simplify("(A + 2) == (1 + A)"));
    }

    @Test
    public void testNEq() throws Exception {
        assertEquals("false", simplify("true != true"));
        assertEquals("false", simplify("3 != 3"));
        assertEquals("false", simplify("3.2 != 3.2"));
        assertEquals("false", simplify("A != A"));
        assertEquals("false", simplify("(A + 2) != (2 + A)"));

        assertEquals("true", simplify("true != false"));
        assertEquals("true", simplify("3 != 2"));
        assertEquals("true", simplify("3.2 != 3.1"));
        assertEquals("NEqD(A,B)", simplify("A != B"));
        assertEquals("NEqD(A,1)", simplify("A != 1"));
        assertEquals("NEqD(Add(2,A),Add(1,A))", simplify("(A + 2) != (1 + A)"));
    }

    private ParserImpl parser;

    @Before
    public void setUp() throws Exception {
        DefaultNamespace namespace = new DefaultNamespace();
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
        });
        parser = new ParserImpl(namespace);
    }

    private String simplify(String code) throws ParseException {
        return new TermSimplifier().simplify(parser.parse(code)).toString();
    }
}
