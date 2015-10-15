package org.esa.snap.core.jexp.impl;

import org.esa.snap.core.jexp.Term;

/**
 * A specialisation of the {@link TermSimplifier} which performs optimisations on a term that are guaranteed
 * to perform faster and are numerically equivalent to the original term. However, the optimisation may change the
 * term's structure in an unwanted way. For example, the term {@code sqrt(sq(t))} can be simplified to {@code abs(t)}
 * which is numerically equivalent but is not differentiable anymore.
 * <p/>
 * Note: The only optimisation currently implemented on top of simplifications performed by
 * the {@link TermSimplifier} is {@code sqrt(sq(t)) --> abs(t)}.
 * Other optimisations may be added in the future.
 *
 * @author Norman Fomferra
 */
public class TermOptimizer extends  TermSimplifier {

    @Override
    public Term visit(Term.Call term) {

        Term t = super.visit(term);
        if (t instanceof Term.Call) {
            term = (Term.Call) t;
        } else {
            return t;
        }

        if (term.getFunction() == Functions.SQRT) {
            Term arg = term.getArg();
            if (arg instanceof Term.Call) {
                Term.Call innerCall = (Term.Call) arg;
                if (innerCall.getFunction() == Functions.SQ) {
                    Term innerArg = innerCall.getArg();
                    return apply(new Term.Call(Functions.ABS_D, innerArg));
                }
            }
        }

        return term;
    }
}
