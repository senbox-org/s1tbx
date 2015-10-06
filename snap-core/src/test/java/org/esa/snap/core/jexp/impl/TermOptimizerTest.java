package org.esa.snap.core.jexp.impl;

import org.junit.Test;

import static org.junit.Assert.*;

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
        assertEquals("abs(A)", simplify("sqrt(sq(A))")); // don't suppress sign!
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
        assertEquals("pow(A,sq(A))", simplify("pow(A,sq(A))"));
        assertEquals("pow(sq(A),A)", simplify("pow(sq(A),A)")); // don't suppress sign!
        assertEquals("A", simplify("pow(pow(A,0.5),2)")); // don't suppress sign!
        assertEquals("abs(A)", simplify("pow(pow(A,2),0.5)")); // don't suppress sign!
        assertEquals("abs(A)", simplify("pow(sq(A),0.5)")); // don't suppress sign!
        assertEquals("exp(Mul(3,A))", simplify("pow(exp(A),3)"));
        assertEquals("pow(A,Mul(0.5,A))", simplify("pow(sqrt(A),A)"));
    }

    @Override
    protected TermSimplifier createSimplifier() {
        return new TermOptimizer();
    }
}
