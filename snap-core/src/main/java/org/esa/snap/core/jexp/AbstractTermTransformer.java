package org.esa.snap.core.jexp;

/**
 * A basic implementation of a term transformer which recursively clones operation and function call terms. For all
 * other term types (constants, symbol references), it simply returns the source terms.
 *
 * @author Norman Fomferra
 */
public abstract class AbstractTermTransformer implements TermTransformer {

    @Override
    public Term visit(Term.ConstB term) {
        return term;
    }

    @Override
    public Term visit(Term.ConstI term) {
        return term;
    }

    @Override
    public Term visit(Term.ConstD term) {
        return term;
    }

    @Override
    public Term visit(Term.ConstS term) {
        return term;
    }

    @Override
    public Term visit(Term.Ref term) {
        return term;
    }

    @Override
    public Term visit(Term.Call term) {
        Term[] args = term.getArgs();
        Term[] argClones = args.clone();
        for (int i = 0; i < argClones.length; i++) {
            argClones[i] = apply(args[i]);
        }
        return new Term.Call(term.getFunction(), argClones);
    }

    @Override
    public Term visit(Term.Cond term) {
        return new Term.Cond(term.getRetType(),
                             apply(term.getArg(0)),
                             apply(term.getArg(1)),
                             apply(term.getArg(2)));
    }

    @Override
    public Term visit(Term.Assign term) {
        return new Term.Assign(apply(term.getArg(0)),
                               apply(term.getArg(1)));
    }

    @Override
    public Term visit(Term.NotB term) {
        return new Term.NotB(apply(term.getArg()));
    }

    @Override
    public Term visit(Term.AndB term) {
        return new Term.AndB(apply(term.getArg(0)),
                             apply(term.getArg(1)));
    }

    @Override
    public Term visit(Term.OrB term) {
        return new Term.OrB(apply(term.getArg(0)),
                            apply(term.getArg(1)));
    }

    @Override
    public Term visit(Term.NotI term) {
        return new Term.NotI(apply(term.getArg()));
    }

    @Override
    public Term visit(Term.XOrI term) {
        return new Term.XOrI(apply(term.getArg(0)),
                             apply(term.getArg(1)));
    }

    @Override
    public Term visit(Term.AndI term) {
        return new Term.AndI(apply(term.getArg(0)),
                             apply(term.getArg(1)));
    }

    @Override
    public Term visit(Term.OrI term) {
        return new Term.OrI(apply(term.getArg(0)),
                            apply(term.getArg(1)));
    }

    @Override
    public Term visit(Term.Neg term) {
        return new Term.Neg(term.getRetType(),
                            apply(term.getArg()));
    }

    @Override
    public Term visit(Term.Add term) {
        return new Term.Add(term.getRetType(),
                            apply(term.getArg(0)),
                            apply(term.getArg(1)));
    }

    @Override
    public Term visit(Term.Sub term) {
        return new Term.Sub(term.getRetType(),
                            apply(term.getArg(0)),
                            apply(term.getArg(1)));
    }

    @Override
    public Term visit(Term.Mul term) {
        return new Term.Mul(term.getRetType(),
                            apply(term.getArg(0)),
                            apply(term.getArg(1)));
    }

    @Override
    public Term visit(Term.Div term) {
        return new Term.Div(term.getRetType(),
                            apply(term.getArg(0)),
                            apply(term.getArg(1)));
    }

    @Override
    public Term visit(Term.Mod term) {
        return new Term.Mod(term.getRetType(),
                            apply(term.getArg(0)),
                            apply(term.getArg(1)));
    }

    @Override
    public Term visit(Term.EqB term) {
        return new Term.EqB(apply(term.getArg(0)),
                            apply(term.getArg(1)));
    }

    @Override
    public Term visit(Term.EqI term) {
        return new Term.EqI(apply(term.getArg(0)),
                            apply(term.getArg(1)));
    }

    @Override
    public Term visit(Term.EqD term) {
        return new Term.EqD(apply(term.getArg(0)),
                            apply(term.getArg(1)));
    }

    @Override
    public Term visit(Term.NEqB term) {
        return new Term.NEqB(apply(term.getArg(0)),
                             apply(term.getArg(1)));
    }

    @Override
    public Term visit(Term.NEqI term) {
        return new Term.NEqI(apply(term.getArg(0)),
                             apply(term.getArg(1)));
    }

    @Override
    public Term visit(Term.NEqD term) {
        return new Term.NEqD(apply(term.getArg(0)),
                             apply(term.getArg(1)));
    }

    @Override
    public Term visit(Term.LtI term) {
        return new Term.LtI(apply(term.getArg(0)),
                            apply(term.getArg(1)));
    }

    @Override
    public Term visit(Term.LtD term) {
        return new Term.LtD(apply(term.getArg(0)),
                            apply(term.getArg(1)));
    }

    @Override
    public Term visit(Term.LeI term) {
        return new Term.LeI(apply(term.getArg(0)),
                            apply(term.getArg(1)));
    }

    @Override
    public Term visit(Term.LeD term) {
        return new Term.LeD(apply(term.getArg(0)),
                            apply(term.getArg(1)));
    }

    @Override
    public Term visit(Term.GtI term) {
        return new Term.GtI(apply(term.getArg(0)),
                            apply(term.getArg(1)));
    }

    @Override
    public Term visit(Term.GtD term) {
        return new Term.GtD(apply(term.getArg(0)),
                            apply(term.getArg(1)));
    }

    @Override
    public Term visit(Term.GeI term) {
        return new Term.GeI(apply(term.getArg(0)),
                            apply(term.getArg(1)));
    }

    @Override
    public Term visit(Term.GeD term) {
        return new Term.GeD(apply(term.getArg(0)),
                            apply(term.getArg(1)));
    }
}

