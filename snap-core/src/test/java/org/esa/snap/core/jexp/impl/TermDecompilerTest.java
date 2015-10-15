package org.esa.snap.core.jexp.impl;

import org.esa.snap.core.jexp.EvalEnv;
import org.esa.snap.core.jexp.EvalException;
import org.esa.snap.core.jexp.ParseException;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Norman
 */
public class TermDecompilerTest {

    @Test
    public void testAssign() throws Exception {
        //assertEquals("v = 3", decompile("v=3"));
    }

    @Test
    public void testCond() throws Exception {
        assertEquals("C ? A : B", decompile("C?A:B"));
        assertEquals("C ? A + 1 : B - 1", decompile("C? A+1 : B-1"));
        assertEquals("C ? A + 1 : true ? 1 : 2", decompile("C? A+1 : true ? 1:2"));
        assertEquals("C ? true ? 1 : 2 : B - 1", decompile("C? true ? 1:2 : B-1"));
        assertEquals("(C ? true : false) ? true ? 1 : 2 : B - 1", decompile("(C?true:false)? true ? 1:2 : B-1"));
    }

    @Test
    public void testOrB() throws Exception {
        assertEquals("C || true", decompile(" C|| true"));
        assertEquals("C || true || false", decompile(" C|| true||false"));
        assertEquals("C || true || false", decompile(" C|| (true||false)"));
        assertEquals("C || true || false", decompile(" (C|| true)||false"));
    }

    @Test
    public void testAndB() throws Exception {
        assertEquals("C && true", decompile(" C&& true"));
        assertEquals("C && true && false", decompile(" C&& true&&false"));
        assertEquals("C && true && false", decompile(" C&& (true&&false)"));
        assertEquals("C && true && false", decompile(" (C&& true)&&false"));
    }

    @Test
    public void testXOrI() throws Exception {
        assertEquals("B ^ 2", decompile(" B^ 2"));
        assertEquals("B ^ 2 ^ 4", decompile(" B^ 2^4"));
        assertEquals("B ^ 2 ^ 4", decompile(" B^ (2^4)"));
        assertEquals("B ^ 2 ^ 4", decompile(" (B^ 2)^4"));
    }
    @Test
    public void testAndI() throws Exception {
        assertEquals("B & 2", decompile(" B& 2"));
        assertEquals("B & 2 & 4", decompile(" B& 2&4"));
        assertEquals("B & 2 & 4", decompile(" B& (2&4)"));
        assertEquals("B & 2 & 4", decompile(" (B& 2)&4"));
    }

    @Test
    public void testOrI() throws Exception {
        assertEquals("B | 2", decompile(" B| 2"));
        assertEquals("B | 2 | 4", decompile(" B| 2|4"));
        assertEquals("B | 2 | 4", decompile(" B| (2|4)"));
        assertEquals("B | 2 | 4", decompile(" (B| 2)|4"));
    }

    @Test
    public void testAdd() throws Exception {
        assertEquals("A + 2", decompile(" A +2"));
        assertEquals("A + 2 + B", decompile(" A +2 +B"));
        assertEquals("A + 2 + B", decompile("( A +2) +B"));
        assertEquals("A + 2 + B", decompile(" A +(2 +B)"));
        assertEquals("A + 2 + B + 1", decompile(" A +(2 +(B +1))"));
        assertEquals("A + 2 + B + 1", decompile("((A +2) +  B) +1"));
    }

    @Test
    public void testSub() throws Exception {
        assertEquals("A - 2", decompile(" A -2"));
        assertEquals("A - 2 - B", decompile(" A -2 -B"));
        assertEquals("A - 2 - B", decompile("( A -2) -B"));
        assertEquals("A - 2 - B - 1", decompile("(( A -2)-B) -1"));
        assertEquals("A - (2 - B)", decompile(" A -(2 -B)"));
        assertEquals("A - (2 - B)", decompile(" A -(2 -B)"));
        assertEquals("A - 2 - (B - 1)", decompile(" A -2 -(B -1)"));
        assertEquals("A - 2 - (B - 1)", decompile("( A -2) -(B -1)"));
        assertEquals("A - (2 - (B - 1))", decompile(" A -(2 -(B -1))"));
    }

    @Test
    public void testMul() throws Exception {
        assertEquals("A * 2", decompile(" A *2"));
        assertEquals("A * 2 * B", decompile(" A *2 *B"));
        assertEquals("A * 2 * B", decompile("( A *2) *B"));
        assertEquals("A * 2 * B", decompile(" A *(2 *B)"));
        assertEquals("A * 2 * B * 1", decompile(" A *(2 *(B *1))"));
        assertEquals("A * 2 * B * 1", decompile("((A *2) *  B) *1"));
    }

    @Test
    public void testDiv() throws Exception {
        assertEquals("A / 2", decompile(" A /2"));
        assertEquals("A / 2 / B", decompile(" A /2 /B"));
        assertEquals("A / 2 / B", decompile("( A /2) /B"));
        assertEquals("A / 2 / B / 1", decompile("(( A /2)/B) /1"));
        assertEquals("A / (2 / B)", decompile(" A /(2 /B)"));
        assertEquals("A / (2 / B)", decompile(" A /(2 /B)"));
        assertEquals("A / 2 / (B / 1)", decompile(" A /2 /(B /1)"));
        assertEquals("A / 2 / (B / 1)", decompile("( A /2) /(B /1)"));
        assertEquals("A / (2 / (B / 1))", decompile(" A /(2 /(B /1))"));
    }

    @Test
    public void testMod() throws Exception {
        assertEquals("A % 2", decompile(" A %2"));
        assertEquals("A % 2 % B", decompile(" A %2 %B"));
        assertEquals("A % 2 % B", decompile("( A %2) %B"));
        assertEquals("A % 2 % B % 1", decompile("(( A %2)%B) %1"));
        assertEquals("A % (2 % B)", decompile(" A %(2 %B)"));
        assertEquals("A % (2 % B)", decompile(" A %(2 %B)"));
        assertEquals("A % 2 % (B % 1)", decompile(" A %2 %(B %1)"));
        assertEquals("A % 2 % (B % 1)", decompile("( A %2) %(B %1)"));
        assertEquals("A % (2 % (B % 1))", decompile(" A %(2 %(B %1))"));
    }

    @Test
    public void testComp() throws Exception {

        assertEquals("A == 2", decompile("A==2"));
        assertEquals("A == 2 == false", decompile("A==2==false"));
        assertEquals("A == 2 == false", decompile("A==(2==false)"));
        assertEquals("A == 2 == false", decompile("(A==2)==false"));

        assertEquals("A != 2", decompile("A!=2"));
        assertEquals("A != 2 != false", decompile("A!=2!=false"));
        assertEquals("A != 2 != false", decompile("A!=(2!=false)"));
        assertEquals("A != 2 != false", decompile("(A!=2)!=false"));

        assertEquals("A >= 2", decompile("A>=2"));
        assertEquals("A >= 2 == false", decompile("A>=2==false"));

        assertEquals("A <= 2", decompile("A<=2"));
        assertEquals("A <= 2 == false", decompile("A<=2==false"));

        assertEquals("A < 2", decompile("A<2"));
        assertEquals("A < 2 == false", decompile("A<2==false"));

        assertEquals("A > 2", decompile("A>2"));
        assertEquals("A > 2 == false", decompile("A>2==false"));
    }

    @Test
    public void testUnary() throws Exception {
        assertEquals("-A", decompile("-A"));
        assertEquals("--A", decompile("--A"));
        assertEquals("--A", decompile("-(-A)"));

        assertEquals("~A", decompile("~A"));
        assertEquals("~~A", decompile("~~A"));
        assertEquals("~~A", decompile("~(~A)"));

        assertEquals("!C", decompile("!C"));
        assertEquals("!!C", decompile("!!C"));
        assertEquals("!!C", decompile("!(!C)"));

        assertEquals("!(~A > -1)", decompile("!(~A > -1)"));
    }

    @Test
    public void testCall() throws Exception {
        assertEquals("sin(x)", decompile("sin(x)"));
        assertEquals("min(2, x)", decompile("min( 2,  x)"));
        assertEquals("max(2, x + 1.5)", decompile("max( 2,  x + 1.5)"));
    }

    @Test
    public void testSymbol() throws Exception {
        assertEquals("x", decompile("x"));
        assertEquals("A", decompile("A"));
        assertEquals("PI", decompile("PI"));
        assertEquals("E", decompile("E"));
    }

    @Test
    public void testConst() throws Exception {
        assertEquals("0", decompile("0"));
        assertEquals("1", decompile("1"));
        assertEquals("10.3", decompile("10.3"));
        assertEquals("true", decompile("true"));
        assertEquals("NaN", decompile("NaN"));
    }

    @Test
    public void testMixed() throws Exception {
        assertEquals("A * (B + A) * B / (x * B) / (-7 - x + A - x)",
                     decompile("A*(B+A)*B/(x*B)/(-7-x+A-x)"));

        assertEquals("C && (B >= 4 || A == 2 * B) ? (x - 1) / (x + 2) : pow(2, -x - (A - 1))",
                     decompile("C&&(B>=4||A==2*B)?(x-1)/(x+2):pow(2, -x-(A-1))"));
    }

    private ParserImpl parser;

    @Before
    public void setUp() throws Exception {
        DefaultNamespace namespace = new DefaultNamespace();
        namespace.registerSymbol(new AbstractSymbol.D("x") {
            @Override
            public double evalD(EvalEnv env) throws EvalException {
                return Math.random();
            }
        });
        namespace.registerSymbol(new AbstractSymbol.I("A") {
            @Override
            public int evalI(EvalEnv env) throws EvalException {
                return (int) (100 * Math.random());
            }
        });
        namespace.registerSymbol(new AbstractSymbol.I("B") {
            @Override
            public int evalI(EvalEnv env) throws EvalException {
                return (int) (200 * Math.random());
            }
        });
        namespace.registerSymbol(new AbstractSymbol.B("C") {

            @Override
            public boolean evalB(EvalEnv env) throws EvalException {
                return Math.random() > 0.5;
            }
        });
        parser = new ParserImpl(namespace);
    }

    private String decompile(String code) throws ParseException {
        return new TermDecompiler().decompile(parser.parse(code));
    }

}
