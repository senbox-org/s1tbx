package com.bc.jexp.impl;

import com.bc.jexp.*;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Norman
 */
public class TermDecompilerTest {
    @Test
    public void testConst() throws Exception {
        assertEquals("0", decompile("0"));
        assertEquals("1", decompile("1"));
        assertEquals("10.3", decompile("10.3"));
        assertEquals("true", decompile("true"));
        assertEquals("NaN", decompile("NaN"));
    }

    @Test
    public void testSymbol() throws Exception {
        assertEquals("x", decompile("x"));
        assertEquals("A", decompile("A"));
        assertEquals("PI", decompile("PI"));
        assertEquals("E", decompile("E"));
    }

    @Test
    public void testCall() throws Exception {
        assertEquals("rand()", decompile("rand()"));
    }

    @Test
    public void testCond() throws Exception {
    }

    @Test
    public void testNeg() throws Exception {
    }

    @Test
    public void testAdd() throws Exception {
    }

    @Test
    public void testSub() throws Exception {
    }

    @Test
    public void testMul() throws Exception {
    }

    @Test
    public void testDiv() throws Exception {
    }

    @Test
    public void testEq() throws Exception {
    }

    @Test
    public void testNEq() throws Exception {
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
        parser = new ParserImpl(namespace);
    }

    private String decompile(String code) throws ParseException {
        return new TermDecompiler().decompile(parser.parse(code));
    }

    public static class TermDecompiler implements TermVisitor<String> {

        public String  decompile(Term term) {
            return term.accept(this);
        }

        @Override
        public String visit(Term.ConstB term) {
            return term.toString();
        }

        @Override
        public String visit(Term.ConstI term) {
            return term.toString();
        }

        @Override
        public String visit(Term.ConstD term) {
            return term.toString();
        }

        @Override
        public String visit(Term.ConstS term) {
            return term.toString();
        }

        @Override
        public String visit(Term.Ref term) {
            return term.getSymbol().getName();
        }

        @Override
        public String visit(Term.Call term) {
            return term.toString();
        }

        @Override
        public String visit(Term.Cond term) {
            return null;
        }

        @Override
        public String visit(Term.Assign term) {
            return null;
        }

        @Override
        public String visit(Term.NotB term) {
            return null;
        }

        @Override
        public String visit(Term.AndB term) {
            return null;
        }

        @Override
        public String visit(Term.OrB term) {
            return null;
        }

        @Override
        public String visit(Term.NotI term) {
            return null;
        }

        @Override
        public String visit(Term.XOrI term) {
            return null;
        }

        @Override
        public String visit(Term.AndI term) {
            return null;
        }

        @Override
        public String visit(Term.OrI term) {
            return null;
        }

        @Override
        public String visit(Term.Neg term) {
            return null;
        }

        @Override
        public String visit(Term.Add term) {
            return null;
        }

        @Override
        public String visit(Term.Sub term) {
            return null;
        }

        @Override
        public String visit(Term.Mul term) {
            return null;
        }

        @Override
        public String visit(Term.Div term) {
            return null;
        }

        @Override
        public String visit(Term.Mod term) {
            return null;
        }

        @Override
        public String visit(Term.EqB term) {
            return null;
        }

        @Override
        public String visit(Term.EqI term) {
            return null;
        }

        @Override
        public String visit(Term.EqD term) {
            return null;
        }

        @Override
        public String visit(Term.NEqB term) {
            return null;
        }

        @Override
        public String visit(Term.NEqI term) {
            return null;
        }

        @Override
        public String visit(Term.NEqD term) {
            return null;
        }

        @Override
        public String visit(Term.LtI term) {
            return null;
        }

        @Override
        public String visit(Term.LtD term) {
            return null;
        }

        @Override
        public String visit(Term.LeI term) {
            return null;
        }

        @Override
        public String visit(Term.LeD term) {
            return null;
        }

        @Override
        public String visit(Term.GtI term) {
            return null;
        }

        @Override
        public String visit(Term.GtD term) {
            return null;
        }

        @Override
        public String visit(Term.GeI term) {
            return null;
        }

        @Override
        public String visit(Term.GeD term) {
            return null;
        }
    }
}
