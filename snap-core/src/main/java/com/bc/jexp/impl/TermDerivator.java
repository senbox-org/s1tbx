package com.bc.jexp.impl;

import com.bc.jexp.Symbol;
import com.bc.jexp.Term;
import com.bc.jexp.TermConverter;

/**
 * Computes the derivative of a term.
 *
 * @author Norman Fomferra
 */
public class TermDerivator implements TermConverter {
    private final TermSimplifier simplifier;
    private final Symbol variable;

    public TermDerivator(Symbol variable) {
        this(variable, new TermSimplifier());
    }

    public TermDerivator(Symbol variable, TermSimplifier simplifier) {
        this.variable = variable;
        this.simplifier = simplifier;
    }

    /**
     * Computes the derivative of a term.
     *
     * @param term The term.
     * @return The derivative.
     * @throws UnsupportedOperationException if the derivative could not be computed
     */
    public Term derivative(Term term) {
        //return term.accept(this);
        return simplifier.simplify(term.accept(this));
    }

    @Override
    public Term visit(Term.ConstB term) {
        return Term.ConstD.ZERO;
    }

    @Override
    public Term visit(Term.ConstI term) {
        return Term.ConstD.ZERO;
    }

    @Override
    public Term visit(Term.ConstD term) {
        return Term.ConstD.ZERO;
    }

    @Override
    public Term visit(Term.ConstS term) {
        return Term.ConstD.ZERO;
    }

    @Override
    public Term visit(Term.Ref term) {
        if (term.getSymbol() == variable) {
            return Term.ConstD.ONE;
        }
        return Term.ConstD.ZERO;
    }

    @Override
    public Term visit(Term.Call term) {
        if (term.getFunction() == Functions.SQR) {
            return new Term.Mul(Term.TYPE_D,
                                new Term.Mul(Term.TYPE_D, Term.ConstD.TWO, term.getArg()),
                                derivative(term.getArg()));
        } else if (term.getFunction() == Functions.SQRT) {
            return new Term.Mul(Term.TYPE_D,
                                new Term.Div(Term.TYPE_D,
                                             Term.ConstD.ONE,
                                             new Term.Mul(Term.TYPE_D,
                                                          Term.ConstD.TWO,
                                                          new Term.Call(Functions.SQRT, term.getArg()))),
                                derivative(term.getArg()));
        } else if (term.getFunction() == Functions.POW) {
            if (term.getArg(1).isConst()) {
                double v = term.getArg(1).evalD(null);
                return new Term.Mul(Term.TYPE_D,
                                    new Term.Mul(Term.TYPE_D,
                                                 Term.ConstD.get(v),
                                                 new Term.Call(Functions.POW,
                                                               term.getArg(),
                                                               Term.ConstD.get(v - 1))),
                                    derivative(term.getArg(0)));
            }
        } else if (term.getFunction() == Functions.EXP) {
            return new Term.Mul(Term.TYPE_D,
                                term,
                                derivative(term.getArg()));
        } else if (term.getFunction() == Functions.LOG) {
            return new Term.Mul(Term.TYPE_D,
                                new Term.Div(Term.TYPE_D, Term.ConstD.ONE, term.getArg()),
                                derivative(term.getArg()));
        } else if (term.getFunction() == Functions.SIN) {
            return new Term.Mul(Term.TYPE_D,
                                new Term.Call(Functions.COS, term.getArg()),
                                derivative(term.getArg()));
        } else if (term.getFunction() == Functions.COS) {
            return new Term.Mul(Term.TYPE_D,
                                new Term.Neg(Term.TYPE_D, new Term.Call(Functions.SIN, term.getArg())),
                                derivative(term.getArg()));
        } else if (term.getFunction() == Functions.TAN) {
            return new Term.Mul(Term.TYPE_D,
                                new Term.Div(Term.TYPE_D,
                                             Term.ConstD.ONE,
                                             new Term.Call(Functions.SQR, new Term.Call(Functions.COS, term.getArg()))),
                                derivative(term.getArg()));
        }
        // add other functions from Functions class here...
        return unsupported(term);
    }

    @Override
    public Term visit(Term.Cond term) {
        return new Term.Cond(Term.TYPE_D, term.getArg(0), derivative(term.getArg(1)), derivative(term.getArg(2)));
    }

    @Override
    public Term visit(Term.Neg term) {
        return new Term.Neg(term.getRetType(), derivative(term.getArg()));
    }

    @Override
    public Term visit(Term.Add term) {
        return new Term.Add(term.getRetType(), derivative(term.getArg(0)), derivative(term.getArg(1)));
    }

    @Override
    public Term visit(Term.Sub term) {
        return new Term.Sub(term.getRetType(), derivative(term.getArg(0)), derivative(term.getArg(1)));
    }

    @Override
    public Term visit(Term.Mul term) {
        Term arg1 = term.getArg(0);
        Term arg2 = term.getArg(1);
        return new Term.Add(Term.TYPE_D,
                            new Term.Mul(Term.TYPE_D, arg1, derivative(arg2)),
                            new Term.Mul(Term.TYPE_D, derivative(arg1), arg2));
    }

    @Override
    public Term visit(Term.Div term) {
        Term arg1 = term.getArg(0);
        Term arg2 = term.getArg(1);
        return new Term.Div(Term.TYPE_D,
                            new Term.Sub(Term.TYPE_D,
                                         new Term.Mul(Term.TYPE_D, derivative(arg1), arg2),
                                         new Term.Mul(Term.TYPE_D, arg1, derivative(arg2))),
                            new Term.Call(Functions.SQR, arg2));
    }

    @Override
    public Term visit(Term.Assign term) {
        return unsupported(term);
    }

    @Override
    public Term visit(Term.NotB term) {
        return unsupported(term);
    }

    @Override
    public Term visit(Term.AndB term) {
        return unsupported(term);
    }

    @Override
    public Term visit(Term.OrB term) {
        return unsupported(term);
    }

    @Override
    public Term visit(Term.NotI term) {
        return unsupported(term);
    }

    @Override
    public Term visit(Term.XOrI term) {
        return unsupported(term);
    }

    @Override
    public Term visit(Term.AndI term) {
        return unsupported(term);
    }

    @Override
    public Term visit(Term.OrI term) {
        return unsupported(term);
    }

    @Override
    public Term visit(Term.Mod term) {
        return unsupported(term);
    }

    @Override
    public Term visit(Term.EqB term) {
        return unsupported(term);
    }

    @Override
    public Term visit(Term.EqI term) {
        return unsupported(term);
    }

    @Override
    public Term visit(Term.EqD term) {
        return unsupported(term);
    }

    @Override
    public Term visit(Term.NEqB term) {
        return unsupported(term);
    }

    @Override
    public Term visit(Term.NEqI term) {
        return unsupported(term);
    }

    @Override
    public Term visit(Term.NEqD term) {
        return unsupported(term);
    }

    @Override
    public Term visit(Term.LtI term) {
        return unsupported(term);
    }

    @Override
    public Term visit(Term.LtD term) {
        return unsupported(term);
    }

    @Override
    public Term visit(Term.LeI term) {
        return unsupported(term);
    }

    @Override
    public Term visit(Term.LeD term) {
        return unsupported(term);
    }

    @Override
    public Term visit(Term.GtI term) {
        return unsupported(term);
    }

    @Override
    public Term visit(Term.GtD term) {
        return unsupported(term);
    }

    @Override
    public Term visit(Term.GeI term) {
        return unsupported(term);
    }

    @Override
    public Term visit(Term.GeD term) {
        return unsupported(term);
    }

    private Term unsupported(Term.Op term) {
        throw new UnsupportedOperationException(String.format("derivative of operator '%s'", term.getName()));
    }

    private Term unsupported(Term.Call term) {
        throw new UnsupportedOperationException(String.format("derivative of function '%s'", term.getFunction().getName()));
    }
}
