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
public class TermOptimizerTest extends TermSimplifierTest {

    @Test
    public void testSqrt() throws Exception {
        assertEquals("0.0", simplify("sqrt(0)"));
        assertEquals("0.0", simplify("sqrt(0.0)"));
        assertEquals("sqrt(A)", simplify("sqrt(A)"));
        assertEquals("sqrt(A)", simplify("sqrt(A + 0)"));
        assertEquals("pow(A,0.25)", simplify("sqrt(sqrt(A))"));
        assertEquals("abs(A)", simplify("sqrt(sqr(A))")); // don't suppress sign!
        assertEquals("abs(A)", simplify("sqrt(pow(A,2))")); // don't suppress sign!
        assertEquals("pow(A,0.25)", simplify("sqrt(sqrt(A))")); // don't suppress sign!
        assertEquals("sqrt(pow(A,4))", simplify("sqrt(pow(A, 4))")); // don't suppress sign!
        assertEquals("pow(A,2.5)", simplify("sqrt(pow(A, 5))"));
    }

    @Test
    public void testPow() throws Exception {
        assertEquals("1.0", simplify("pow(A,0)"));
        assertEquals("A", simplify("pow(A,1)"));
        assertEquals("0.0", simplify("pow(0,A)"));
        assertEquals("1.0", simplify("pow(1,A)"));
        assertEquals("exp(A)", simplify("pow(E,A)"));
        assertEquals("A", simplify("pow(E,log(A))"));
        assertEquals("pow(A,sqr(A))", simplify("pow(A,sqr(A))"));
        assertEquals("pow(sqr(A),A)", simplify("pow(sqr(A),A)")); // don't suppress sign!
        assertEquals("sqr(sqrt(A))", simplify("pow(pow(A,0.5),2)")); // don't suppress sign!
        assertEquals("abs(A)", simplify("pow(pow(A,2),0.5)")); // don't suppress sign!
        assertEquals("abs(A)", simplify("pow(sqr(A),0.5)")); // don't suppress sign!
        assertEquals("exp(Mul(3,A))", simplify("pow(exp(A),3)"));
        assertEquals("pow(A,Mul(0.5,A))", simplify("pow(sqrt(A),A)"));
    }

    @Override
    protected TermSimplifier createSimplifier() {
        return new TermOptimizer();
    }
}
