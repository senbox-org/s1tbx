package org.esa.snap.core.jexp.impl;

import com.bc.ceres.core.Assert;
import org.esa.snap.core.jexp.Symbol;
import org.esa.snap.core.jexp.Term;

import java.util.List;

/**
 * Helper class making term construction easy and readable. Includes common mathematical functions.
 *
 * @author Norman Fomferra
 */
public class TermFactory {

    /**
     * @param v A constant value.
     * @return Term representing the constant.
     */
    public static Term c(boolean v) {
        return v ? Term.ConstB.TRUE : Term.ConstB.FALSE;
    }

    /**
     * @param v A constant value.
     * @return Term representing the constant.
     */
    public static Term c(int v) {
        Term t = Term.ConstI.lookup(v);
        if (t != null) {
            return t;
        }
        return new Term.ConstI(v);
    }

    /**
     * @param v A constant value.
     * @return Term representing the constant.
     */
    public static Term c(double v) {
        Term t = Term.ConstD.lookup(v);
        if (t != null) {
            return t;
        }
        return new Term.ConstD(v);
    }

    /**
     * @param t A term.
     * @param x The variable.
     * @return The derivative of the term t at the variable x.
     * @see TermDerivator
     */
    public static Term derivative(Term t, Symbol x) {
        return new TermDerivator(x).apply(t);
    }

    /**
     * @param t A term.
     * @param x The variable.
     * @param n The order of the derivative
     * @return The derivative of the term t at the variable x.
     * @see TermDerivator
     */
    public static Term derivative(Term t, Symbol x, int n) {
        Assert.argument(n >= 0);
        if (n == 0) {
            return t;
        }
        for (int i = 1; i <= n; i++) {
            t = derivative(t, x);
        }
        return t;
    }

    /**
     * @param t A term.
     * @return An simplified version of term t.
     * @see TermSimplifier
     */
    public static Term simplify(Term t) {
        return new TermSimplifier().apply(t);
    }

    /**
     * @param t A term.
     * @return An optimized version of term t.
     * @see TermOptimizer
     */
    public static Term optimize(Term t) {
        return new TermOptimizer().apply(t);
    }

    /**
     * @param vector The argument.
     * @return The magnitude of the argument.
     */
    public static Term magnitude(List<Term> vector) {
        Term result = null;
        for (Term arg : vector) {
            if (result == null) {
                result = sq(arg);
            } else {
                result = add(result, sq(arg));
            }
        }
        return sqrt(result);
    }

    /**
     * @param t The argument.
     * @return The square root of the argument.
     */
    public static Term sqrt(Term t) {
        return new Term.Call(Functions.SQRT, t);
    }

    /**
     * @param s The argument.
     * @return A reference to the argument.
     */
    public static Term ref(Symbol s) {
        return new Term.Ref(s);
    }

    /**
     * @param t The argument.
     * @return The square of argument.
     */
    public static Term sq(Term t) {
        return new Term.Call(Functions.SQ, t);
    }

    /**
     * @param t The argument.
     * @return The negation of argument.
     */
    public static Term neg(Term t) {
        return new Term.Neg(t);
    }

    /**
     * @param t1 The 1st argument.
     * @param t2 The 2nd argument.
     * @return The sum of the two arguments.
     */
    public static Term add(Term t1, Term t2) {
        return new Term.Add(t1, t2);
    }

    /**
     * @param t1 The 1st argument.
     * @param t2 The 2nd argument.
     * @return The difference of the two arguments.
     */
    public static Term sub(Term t1, Term t2) {
        return new Term.Sub(t1, t2);
    }

    /**
     * @param t1 The 1st argument.
     * @param t2 The 2nd argument.
     * @return The product of the two arguments.
     */
    public static Term mul(Term t1, Term t2) {
        return new Term.Mul(t1, t2);
    }

    /**
     * @param t1 The 1st argument.
     * @param t2 The 2nd argument.
     * @return The quotient of the two arguments.
     */
    public static Term div(Term t1, Term t2) {
        return new Term.Div(t1, t2);
    }

    /**
     * @param t1 The 1st argument.
     * @param t2 The 2nd argument.
     * @return The modulo of the two arguments.
     */
    public static Term mod(Term t1, Term t2) {
        return new Term.Mod(t1, t2);
    }

    /**
     * @param t The argument.
     * @return The absolute value of the argument.
     */
    public static Term abs(Term t) {
        return new Term.Call(Functions.ABS_D, t);
    }

    /**
     * @param t The argument.
     * @return The sine of the argument.
     */
    public static Term sin(Term t) {
        return new Term.Call(Functions.SIN, t);
    }

    /**
     * @param t The argument.
     * @return The cosine of the argument.
     */
    public static Term cos(Term t) {
        return new Term.Call(Functions.COS, t);
    }

    /**
     * @param t The argument.
     * @return The tangens of the argument.
     */
    public static Term tan(Term t) {
        return new Term.Call(Functions.TAN, t);
    }

    /**
     * @param t1 The 1st argument.
     * @param t2 The 2nd argument.
     * @return The power of the 1st argument to the 2nd.
     */
    public static Term pow(Term t1, Term t2) {
        return new Term.Call(Functions.POW, t1, t2);
    }

    /**
     * @param t1 The 'if' condition.
     * @param t2 The 'then' clause.
     * @param t3 The 'else' clause.
     * @return A conditional expression.
     */
    public static Term cond(Term t1, Term t2, Term t3) {
        int type1 = t2.getRetType();
        int type2 = t3.getRetType();
        int type = inferType(type1, type2);
        return new Term.Cond(type, t1, t2, t3);
    }

    private static int inferType(int type1, int type2) {
        int type;
        if (type1 == type2) {
            type = type1;
        } else if (type1 == Term.TYPE_D || type2 == Term.TYPE_D) {
            type = Term.TYPE_D;
        } else if (type1 == Term.TYPE_I || type2 == Term.TYPE_I) {
            type = Term.TYPE_I;
        } else if (type1 == Term.TYPE_B || type2 == Term.TYPE_B) {
            type = Term.TYPE_B;
        } else if (type1 == Term.TYPE_S || type2 == Term.TYPE_S) {
            type = Term.TYPE_S;
        } else {
            type = Term.TYPE_D;
        }
        return type;
    }



}
