package org.esa.snap.core.jexp.impl;

import org.esa.snap.core.jexp.Function;
import org.esa.snap.core.jexp.Symbol;
import org.esa.snap.core.jexp.Term;
import org.esa.snap.core.jexp.TermTransformer;

import static org.esa.snap.core.jexp.impl.TermFactory.*;

/**
 * Computes the derivative of a term.
 * The result of this operation is always a term of type {@link Term#TYPE_D}.
 *
 * @author Norman Fomferra
 * @since SNAP 2
 */
public class TermDerivator implements TermTransformer {
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
    @Override
    public Term apply(Term term) {
        //return term.accept(this);
        Term derivative = term.accept(this);
        return simplifier.apply(derivative);
    }

    @Override
    public Term visit(Term.ConstB term) {
        return c(0.0);
    }

    @Override
    public Term visit(Term.ConstI term) {
        return c(0.0);
    }

    @Override
    public Term visit(Term.ConstD term) {
        double v = term.getValue();
        return c(Double.isNaN(v) ? v : 0.0);
    }

    @Override
    public Term visit(Term.ConstS term) {
        return c(0.0);
    }

    @Override
    public Term visit(Term.Ref term) {
        if (term.getSymbol() == variable) {
            return c(1.0);
        }
        return c(0.0);
    }

    @Override
    public Term visit(Term.Call term) {
        if (is(term, Functions.SQ)) {
            return mul(mul(c(2.0), term.getArg()),
                       apply(term.getArg()));
        } else if (is(term, Functions.SQRT)) {
            return mul(div(c(1.0), mul(c(2.0), sqrt(term.getArg()))),
                       apply(term.getArg()));
        } else if (is(term, Functions.POW)) {
            if (term.getArg(1).isConst()) {
                double v = term.getArg(1).evalD(null);
                return mul(mul(c(v), TermFactory.pow(term.getArg(), c(v - 1.0))),
                           apply(term.getArg(0)));
            }
        } else if (is(term, Functions.EXP)) {
            return mul(term,
                       apply(term.getArg()));
        } else if (is(term, Functions.LOG)) {
            return mul(div(c(1.0), term.getArg()),
                       apply(term.getArg()));
        } else if (is(term, Functions.SIN)) {
            return mul(TermFactory.cos(term.getArg()),
                       apply(term.getArg()));
        } else if (is(term, Functions.COS)) {
            return mul(neg(TermFactory.sin(term.getArg())),
                       apply(term.getArg()));
        } else if (is(term, Functions.TAN)) {
            return mul(div(c(1.0), sq(TermFactory.cos(term.getArg()))),
                       apply(term.getArg()));
        }
        // add other functions from Functions class here...
        return unsupported(term);
    }

    private static boolean is(Term.Call term, Function f) {
        return term.getFunction() == f;
    }

    @Override
    public Term visit(Term.Cond term) {
        return TermFactory.cond(term.getArg(0),
                                apply(term.getArg(1)),
                                apply(term.getArg(2)));
    }

    @Override
    public Term visit(Term.Neg term) {
        return neg(apply(term.getArg()));
    }

    @Override
    public Term visit(Term.Add term) {
        return add(apply(term.getArg(0)),
                   apply(term.getArg(1)));
    }

    @Override
    public Term visit(Term.Sub term) {
        return sub(apply(term.getArg(0)),
                   apply(term.getArg(1)));
    }

    @Override
    public Term visit(Term.Mul term) {
        Term t1 = term.getArg(0);
        Term t2 = term.getArg(1);
        return add(mul(t1, apply(t2)),
                   mul(apply(t1), t2));
    }

    @Override
    public Term visit(Term.Div term) {
        Term t1 = term.getArg(0);
        Term t2 = term.getArg(1);
        return div(sub(mul(apply(t1), t2),
                       mul(t1, apply(t2))),
                   sq(t2));
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
