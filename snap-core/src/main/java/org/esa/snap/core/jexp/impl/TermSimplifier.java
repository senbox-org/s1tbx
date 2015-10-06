package org.esa.snap.core.jexp.impl;

import org.esa.snap.core.jexp.Term;
import org.esa.snap.core.jexp.TermTransformer;

import static org.esa.snap.core.jexp.Term.ConstD.*;

/**
 * Builds simplified versions of a given term.
 *
 * @author Norman Fomferra
 */
public class TermSimplifier implements TermTransformer {

    @Override
    public Term apply(Term term) {
        return term.accept(this);
    }

    public Term[] apply(Term... terms) {
        Term[] simpTerms = terms.clone();
        for (int i = 0; i < simpTerms.length; i++) {
            simpTerms[i] = apply(simpTerms[i]);
        }
        return simpTerms;
    }

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
        Term c = tryEvalToConst(term);
        if (c != null) {
            return c;
        }
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

        term = new Term.Call(term.getFunction(), apply(term.getArgs()));

        Term c = tryEvalToConst(term);
        if (c != null) {
            return c;
        }

        if (term.getFunction() == Functions.SQRT) {
            Term arg1 = term.getArg();
            Term arg2 = Term.ConstD.HALF;
            return simpPow(arg1, arg2);
        } else if (term.getFunction() == Functions.SQ) {
            Term arg1 = term.getArg();
            Term arg2 = Term.ConstD.TWO;
            return simpPow(arg1, arg2);
        } else if (term.getFunction() == Functions.POW) {
            Term arg1 = term.getArg(0);
            Term arg2 = term.getArg(1);
            return simpPow(arg1, arg2);
        } else if (term.getFunction() == Functions.EXP) {
            Term arg = term.getArg();
            if (arg.isConst()) {
                double v = arg.evalD(null);
                if (v == 0.0) {
                    return Term.ConstD.ONE;
                } else if (v == 1.0) {
                    return new Term.Ref(Symbols.E);
                }
            } else if (arg instanceof Term.Call) {
                Term.Call callArg = (Term.Call) arg;
                if (callArg.getFunction() == Functions.LOG) {
                    return callArg.getArg();
                }
            }
        } else if (term.getFunction() == Functions.LOG) {
            Term arg = term.getArg();
            if (arg.isConst()) {
                double v = arg.evalD(null);
                if (v == 0.0) {
                    return Term.ConstD.ONE;
                } else if (v == Math.E) {
                    return new Term.Ref(Symbols.E);
                }
            } else if (arg instanceof Term.Call) {
                Term.Call callArg = (Term.Call) arg;
                if (callArg.getFunction() == Functions.EXP) {
                    return callArg.getArg();
                }
            }
        } else if (term.getFunction() == Functions.ABS_I || term.getFunction() == Functions.ABS_D) {
            Term arg = term.getArg();
            if (arg instanceof Term.Neg) {
                Term.Neg negTerm = (Term.Neg) arg;
                return apply(new Term.Call(term.getFunction(), negTerm.getArg()));
            }
        }

        return term;
    }

    private Term tryEvalToConst(Term term) {
        if (term.isConst()) {
            if (term.isB()) {
                return TermFactory.c(term.evalB(null));
            } else if (term.isI()) {
                return TermFactory.c(term.evalI(null));
            } else if (term.isD()) {
                double value = term.evalD(null);
                Term.ConstD result = Term.ConstD.lookup(value);
                if (result == Term.ConstD.PI) {
                    return new Term.Ref(Symbols.PI);
                }
                if (result == Term.ConstD.E) {
                    return new Term.Ref(Symbols.E);
                }
                if (result != null) {
                    return result;
                }
                double valueF = Math.floor(value);
                if (eq(value, valueF)) {
                    return TermFactory.c(valueF);
                }
                double valueR = Math.round(value * 100.0) / 100.0;
                if (eq(value, valueR)) {
                    return TermFactory.c(valueR);
                }
            }
        }
        return null;
    }

    private Term simpPow(Term base, Term exp) {
        if (base.isConst()) {
            double nBase = base.evalD(null);
            if (eq(nBase, 0.0)) {
                return Term.ConstD.ZERO;
            } else if (eq(nBase, 1.0)) {
                return Term.ConstD.ONE;
            } else if (eq(nBase, Math.E)) {
                return apply(new Term.Call(Functions.EXP, exp));
            }
        }
        if (exp.isConst()) {
            double nExp = exp.evalD(null);
            if (eq(nExp, 0.0)) {
                return Term.ConstD.ONE;
            } else if (eq(nExp, 1.0)) {
                return base;
            } else if (eq(nExp, -1.0)) {
                return apply(new Term.Div(Term.TYPE_D, Term.ConstD.ONE, base));
            }
        }

        if (base instanceof Term.Call) {
            Term.Call baseCall = (Term.Call) base;
            if (baseCall.getFunction() == Functions.POW) {
                return simplifyNestedPowButConsiderSign(baseCall, baseCall.getArg(0), baseCall.getArg(1), exp);
            } else if (baseCall.getFunction() == Functions.SQRT) {
                return simplifyNestedPowButConsiderSign(baseCall, baseCall.getArg(), Term.ConstD.HALF, exp);
            } else if (baseCall.getFunction() == Functions.SQ) {
                return simplifyNestedPowButConsiderSign(baseCall, baseCall.getArg(), Term.ConstD.TWO, exp);
            } else if (baseCall.getFunction() == Functions.EXP) {
                return apply(new Term.Call(Functions.EXP, new Term.Mul(Term.TYPE_D, baseCall.getArg(), exp)));
            }
        } else if (base instanceof Term.Neg) {
            Term.Neg negOp = (Term.Neg) base;
            if (isNoneZeroEvenInt(exp)) {
                return simpPow(negOp.getArg(), exp);
            }
        }

        return pow(base, exp);
    }

    private Term simplifyNestedPowButConsiderSign(Term.Call innerCall, Term base, Term exp1, Term exp2) {
        // Check to only simplify nested POW's if we don't have to fear a sign suppression
        boolean noneZeroEvenInt1 = isNoneZeroEvenInt(exp1);
        boolean noneZeroEvenInt2 = isNoneZeroEvenInt(exp2);
        if (!noneZeroEvenInt1 || noneZeroEvenInt2) {
            return apply(simpPow(base, new Term.Mul(Term.TYPE_D, exp1, exp2)));
        } else {
            return pow(innerCall, exp2);
        }
    }

    private Term pow(Term arg1, Term arg2) {
        // Can't further simplify
        if (arg2.isConst()) {
            double v = arg2.evalD(null);
            if (eq(v, 0.5)) {
                return new Term.Call(Functions.SQRT, arg1);
            } else if (eq(v, 2.0)) {
                return new Term.Call(Functions.SQ, arg1);
            }
        }
        return new Term.Call(Functions.POW, arg1, arg2);
    }

    private boolean isNoneZeroEvenInt(Term term) {
        if (term.isConst()) {
            double v = term.evalD(null);
            if (!eq(v, 0.0) && isEvenInt(v)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isEvenInt(double v) {
        double f = v - 2.0 * Math.floor(v / 2.0);
        return eq(f, 0.0);
    }

    @Override
    public Term visit(Term.Cond term) {
        if (term.getArg(0).isConst()) {
            boolean value = term.getArg(0).evalB(null);
            return apply(term.getArg(value ? 1 : 2));
        }
        Term arg1 = apply(term.getArg(0));
        Term arg2 = apply(term.getArg(1));
        Term arg3 = apply(term.getArg(2));
        if (arg2.compare(arg3) == 0) {
            return arg2;
        }
        return new Term.Cond(term.getRetType(), arg1, arg2, arg3);
    }

    @Override
    public Term visit(Term.Assign term) {
        return new Term.Assign(term.getArg(0), apply(term.getArg(1)));
    }

    @Override
    public Term visit(Term.NotB term) {
        if (term.isConst()) {
            return TermFactory.c(term.evalB(null));
        }
        return new Term.NotB(apply(term.getArg()));
    }

    @Override
    public Term visit(Term.AndB term) {
        if (term.isConst()) {
            return TermFactory.c(term.evalB(null));
        }
        return new Term.AndB(apply(term.getArg(0)), apply(term.getArg(1)));
    }

    @Override
    public Term visit(Term.OrB term) {
        if (term.isConst()) {
            return TermFactory.c(term.evalB(null));
        }
        return new Term.OrB(apply(term.getArg(0)), apply(term.getArg(1)));
    }

    @Override
    public Term visit(Term.NotI term) {
        if (term.isConst()) {
            return TermFactory.c(term.evalI(null));
        }
        return new Term.NotI(apply(term.getArg()));
    }

    @Override
    public Term visit(Term.XOrI term) {
        if (term.isConst()) {
            return TermFactory.c(term.evalI(null));
        }
        return new Term.XOrI(apply(term.getArg(0)), apply(term.getArg(1)));
    }

    @Override
    public Term visit(Term.AndI term) {
        if (term.isConst()) {
            return TermFactory.c(term.evalI(null));
        }
        return new Term.AndI(apply(term.getArg(0)), apply(term.getArg(1)));
    }

    @Override
    public Term visit(Term.OrI term) {
        if (term.isConst()) {
            return TermFactory.c(term.evalI(null));
        }
        return new Term.OrI(apply(term.getArg(0)), apply(term.getArg(1)));
    }

    @Override
    public Term visit(Term.Neg term) {
        Term c = tryEvalToConst(term);
        if (c != null) {
            // -C1 --> C2
            return c;
        }

        Term arg = apply(term.getArg(0));
        if (arg instanceof Term.Neg) {
            // --X --> X
            Term.Neg neg = (Term.Neg) arg;
            return neg.getArg();
        }
        if (arg instanceof Term.Mul) {
            // -(X * Y) --> -X * Y
            Term arg11 = ((Term.Mul) arg).getArg(0);
            Term arg12 = ((Term.Mul) arg).getArg(1);
            return apply(new Term.Mul(new Term.Neg(term.getRetType(), arg11), arg12));
        }
        if (arg instanceof Term.Div) {
            // -(X / Y) --> -X / Y
            Term arg11 = ((Term.Div) arg).getArg(0);
            Term arg12 = ((Term.Div) arg).getArg(1);
            return apply(new Term.Div(new Term.Neg(term.getRetType(), arg11), arg12));
        }


        return new Term.Neg(term.getRetType(), arg);
    }

    @Override
    public Term visit(Term.Add term) {
        Term c = tryEvalToConst(term);
        if (c != null) {
            // C1 + C2 --> C3
            return c;
        }

        Term arg1 = apply(term.getArg(0));
        Term arg2 = apply(term.getArg(1));
        if (arg1.isConst() && eq(arg1.evalD(null), 0.0)) {
            // X + 0 --> X
            return arg2;
        } else if (arg2.isConst() && eq(arg2.evalD(null), 0.0)) {
            // 0 + X --> X
            return apply(term.getArg(0));
        }

        if (arg2 instanceof Term.Add) {
            // X + (Y + Z) --> (X + Y) + Z
            Term arg21 = ((Term.Add) arg2).getArg(0);
            Term arg22 = ((Term.Add) arg2).getArg(1);
            return apply(new Term.Add(term.getRetType(), new Term.Add(term.getRetType(), arg1, arg21), arg22));
        }

        if (arg1 instanceof Term.Add) {
            Term arg11 = ((Term.Add) arg1).getArg(0);
            Term arg12 = ((Term.Add) arg1).getArg(1);
            if (arg11.isConst() && arg2.isConst()) {
                // (C1 + Y) + C2 --> (C1 + C2) + Y
                return apply(new Term.Add(term.getRetType(), new Term.Add(term.getRetType(), arg11, arg2), arg12));
            }
            int comp = arg11.compare(arg2);
            if (comp == 0) {
                // (X + Y) + X --> 2 * X + Y
                return apply(new Term.Add(term.getRetType(), new Term.Mul(term.getRetType(), Term.ConstD.TWO, arg2), arg12));
            }
            comp = arg12.compare(arg2);
            if (comp == 0) {
                // (X + Y) + Y --> 2 * Y + X
                return apply(new Term.Add(term.getRetType(), new Term.Mul(term.getRetType(), Term.ConstD.TWO, arg2), arg11));
            }
        }

        if (arg1 instanceof Term.Neg) {
            Term arg11 = ((Term.Neg) arg1).getArg();
            // -X + Y --> Y - X
            return apply(new Term.Sub(term.getRetType(), arg2, arg11));
        }
        if (arg2 instanceof Term.Neg) {
            Term arg12 = ((Term.Neg) arg2).getArg();
            // X + -Y --> X - Y
            return apply(new Term.Sub(term.getRetType(), arg1, arg12));
        }

        int comp = arg1.compare(arg2);
        if (comp == 0) {
            // X + X --> 2 * X
            if (term.isI()) {
                return apply(new Term.Mul(term.getRetType(), Term.ConstI.TWO, arg2));
            } else {
                return apply(new Term.Mul(term.getRetType(), Term.ConstD.TWO, arg2));
            }
        } else if (comp > 0 && !(arg1 instanceof Term.Add)) {
            // Y + X --> X + Y
            return apply(new Term.Add(term.getRetType(), arg2, arg1));
        }

        // No further simplifications
        return new Term.Add(term.getRetType(), arg1, arg2);
    }

    @Override
    public Term visit(Term.Sub term) {
        Term c = tryEvalToConst(term);
        if (c != null) {
            // C1 - C2 --> C3
            return c;
        }

        Term arg1 = apply(term.getArg(0));
        Term arg2 = apply(term.getArg(1));
        if (arg1.isConst() && eq(arg1.evalD(null), 0.0)) {
            // 0 - X --> -X
            return apply(new Term.Neg(arg2.getRetType(), arg2));
        }
        if (arg2.isConst() && eq(arg2.evalD(null), 0.0)) {
            // X - 0 --> X
            return arg1;
        }

        if (arg1 instanceof Term.Neg) {
            // -X - Y --> -(X + Y)
            Term arg11 = ((Term.Neg) arg1).getArg();
            return apply(new Term.Neg(term.getRetType(), new Term.Add(term.getRetType(), arg11, arg2)));
        }
        if (arg2 instanceof Term.Neg) {
            // X - -Y --> X + Y
            Term arg21 = ((Term.Neg) arg2).getArg();
            return apply(new Term.Add(term.getRetType(), arg1, arg21));
        }

        int comp = arg1.compare(arg2);
        if (comp == 0) {
            // X - X --> 0
            return term.isI() ? Term.ConstI.ZERO : Term.ConstD.ZERO;
        }
        // No further simplifications
        return new Term.Sub(term.getRetType(), arg1, arg2);
    }

    @Override
    public Term visit(Term.Mul term) {
        Term c = tryEvalToConst(term);
        if (c != null) {
            // C1 * C2 --> C3
            return c;
        }

        Term arg1 = apply(term.getArg(0));
        Term arg2 = apply(term.getArg(1));
        if (arg1.isConst()) {
            double v = arg1.evalD(null);
            if (eq(v, 0.0)) {
                // 0 * X --> 0
                return term.isI() ? Term.ConstI.ZERO : Term.ConstD.ZERO;
            } else if (eq(v, 1.0)) {
                // 1 * X --> X
                return arg2;
            } else if (eq(v, -1.0)) {
                // -1 * X --> -X
                return apply(new Term.Neg(arg2.getRetType(), arg2));
            }
        }
        if (arg2.isConst()) {
            double v = arg2.evalD(null);
            if (eq(v, 0.0)) {
                // 0 * X --> 0
                return term.isI() ? Term.ConstI.ZERO : Term.ConstD.ZERO;
            } else if (eq(v, 1.0)) {
                // 1 * X --> X
                return arg1;
            } else if (eq(v, -1.0)) {
                // -1 * X --> -X
                return apply(new Term.Neg(arg1.getRetType(), arg1));
            }
        }

        if (arg1 instanceof Term.Neg && arg2 instanceof Term.Neg) {
            // -X * -Y --> X * Y
            Term arg11 = ((Term.Neg) arg1).getArg();
            Term arg21 = ((Term.Neg) arg2).getArg();
            return apply(new Term.Mul(term.getRetType(), arg11, arg21));
        }

        if (arg1 instanceof Term.Const && arg2 instanceof Term.Neg) {
            // C * -X --> -C * X
            Term arg21 = ((Term.Neg) arg2).getArg();
            return apply(new Term.Mul(term.getRetType(), new Term.Neg(arg1), arg21));
        }

        if (arg2 instanceof Term.Mul) {
            // X * (Y * Z) --> (X * Y) * Z
            Term arg21 = ((Term.Mul) arg2).getArg(0);
            Term arg22 = ((Term.Mul) arg2).getArg(1);
            return apply(new Term.Mul(term.getRetType(), new Term.Mul(term.getRetType(), arg1, arg21), arg22));
        }

        if (arg2 instanceof Term.Div) {
            // X * (Y / Z) --> (X * Y) / Z
            Term arg21 = ((Term.Div) arg2).getArg(0);
            Term arg22 = ((Term.Div) arg2).getArg(1);
            return apply(new Term.Div(term.getRetType(), new Term.Mul(term.getRetType(), arg1, arg21), arg22));
        }

        int comp = arg1.compare(arg2);
        if (comp == 0) {
            // X * X = sq(X)
            return apply(new Term.Call(Functions.SQ, arg1));
        } else if (comp > 0 && !(arg1 instanceof Term.Mul)) {
            // Y * X --> X * Y
            return apply(new Term.Mul(term.getRetType(), arg2, arg1));
        }

        // No further simplifications
        return new Term.Mul(term.getRetType(), arg1, arg2);
    }

    @Override
    public Term visit(Term.Div term) {

        if (term.getArg(1).isConst() && eq(term.getArg(1).evalD(null), 0.0)) {
            return Term.ConstD.NAN;
        }

        Term c = tryEvalToConst(term);
        if (c != null) {
            // C1 / C2 --> C3
            return c;
        }

        Term arg1 = apply(term.getArg(0));
        Term arg2 = apply(term.getArg(1));
        if (arg1.isConst() && eq(arg1.evalD(null), 0.0)) {
            // 0 / X --> 0
            return arg1.isI() ? Term.ConstI.ZERO : Term.ConstD.ZERO;
        }
        if (arg2.isConst() && eq(arg2.evalD(null), 1.0)) {
            // X / 1 --> X
            return arg1;
        }

        if (arg1 instanceof Term.Neg && arg2 instanceof Term.Neg) {
            // -X / -Y --> X / Y
            Term arg11 = ((Term.Neg) arg1).getArg();
            Term arg21 = ((Term.Neg) arg2).getArg();
            return apply(new Term.Div(term.getRetType(), arg11, arg21));
        }

        if (arg1 instanceof Term.Const && arg2 instanceof Term.Neg) {
            // C / -X --> -C / X
            Term arg21 = ((Term.Neg) arg2).getArg();
            return apply(new Term.Div(term.getRetType(), new Term.Neg(arg1), arg21));
        }

        if (arg1 instanceof Term.Div) {
            // (X / Y) / Z  --> X / (Y * Z)
            Term arg11 = ((Term.Div) arg1).getArg(0);
            Term arg12 = ((Term.Div) arg1).getArg(1);
            return apply(new Term.Div(term.getRetType(), arg11, new Term.Mul(term.getRetType(), arg12, arg2)));
        }

        if (arg2 instanceof Term.Div) {
            // X / (Y / Z)  --> X * Z / Y
            Term arg21 = ((Term.Div) arg2).getArg(0);
            Term arg22 = ((Term.Div) arg2).getArg(1);
            return apply(new Term.Div(term.getRetType(), new Term.Mul(term.getRetType(), arg1, arg22), arg21));
        }

        int comp = arg1.compare(arg2);
        if (comp == 0) {
            // X / X --> 1
            return term.isI() ? Term.ConstI.ONE : Term.ConstD.ONE;
        }
        return new Term.Div(term.getRetType(), arg1, arg2);
    }

    @Override
    public Term visit(Term.Mod term) {
        Term c = tryEvalToConst(term);
        if (c != null) {
            // C1 % C2 --> C3
            return c;
        }

        Term arg1 = apply(term.getArg(0));
        Term arg2 = apply(term.getArg(1));
        int comp = arg1.compare(arg2);
        if (comp == 0) {
            // X % X --> 0
            return term.isI() ? Term.ConstI.ZERO : Term.ConstD.ZERO;
        }
        return new Term.Mod(arg1, arg2);
    }

    @Override
    public Term visit(Term.EqB term) {
        if (term.isConst()) {
            return TermFactory.c(term.evalB(null));
        }
        Term arg1 = apply(term.getArg(0));
        Term arg2 = apply(term.getArg(1));
        int c = arg1.compare(arg2);
        if (c == 0) {
            return Term.ConstB.TRUE;
        }
        return new Term.EqB(arg1, arg2);
    }

    @Override
    public Term visit(Term.EqI term) {
        if (term.isConst()) {
            return TermFactory.c(term.evalB(null));
        }
        Term arg1 = apply(term.getArg(0));
        Term arg2 = apply(term.getArg(1));
        int c = arg1.compare(arg2);
        if (c == 0) {
            return Term.ConstB.TRUE;
        }
        return new Term.EqI(arg1, arg2);
    }

    @Override
    public Term visit(Term.EqD term) {
        if (term.isConst()) {
            return TermFactory.c(term.evalB(null));
        }
        Term arg1 = apply(term.getArg(0));
        Term arg2 = apply(term.getArg(1));
        int c = arg1.compare(arg2);
        if (c == 0) {
            return Term.ConstB.TRUE;
        }
        return new Term.EqD(arg1, arg2);
    }

    @Override
    public Term visit(Term.NEqB term) {
        if (term.isConst()) {
            return TermFactory.c(term.evalB(null));
        }
        Term arg1 = apply(term.getArg(0));
        Term arg2 = apply(term.getArg(1));
        int c = arg1.compare(arg2);
        if (c == 0) {
            return Term.ConstB.FALSE;
        }
        return new Term.NEqB(arg1, arg2);
    }

    @Override
    public Term visit(Term.NEqI term) {
        if (term.isConst()) {
            return TermFactory.c(term.evalB(null));
        }
        Term arg1 = apply(term.getArg(0));
        Term arg2 = apply(term.getArg(1));
        int c = arg1.compare(arg2);
        if (c == 0) {
            return Term.ConstB.FALSE;
        }
        return new Term.NEqI(arg1, arg2);
    }

    @Override
    public Term visit(Term.NEqD term) {
        if (term.isConst()) {
            return TermFactory.c(term.evalB(null));
        }
        Term arg1 = apply(term.getArg(0));
        Term arg2 = apply(term.getArg(1));
        int c = arg1.compare(arg2);
        if (c == 0) {
            return Term.ConstB.FALSE;
        }
        return new Term.NEqD(arg1, arg2);
    }

    @Override
    public Term visit(Term.LtI term) {
        if (term.isConst()) {
            return TermFactory.c(term.evalB(null));
        }
        Term arg1 = apply(term.getArg(0));
        Term arg2 = apply(term.getArg(1));
        int c = arg1.compare(arg2);
        if (c == 0) {
            return Term.ConstB.FALSE;
        }
        return new Term.LtI(arg1, arg2);
    }

    @Override
    public Term visit(Term.LtD term) {
        if (term.isConst()) {
            return TermFactory.c(term.evalB(null));
        }
        Term arg1 = apply(term.getArg(0));
        Term arg2 = apply(term.getArg(1));
        int c = arg1.compare(arg2);
        if (c == 0) {
            return Term.ConstB.FALSE;
        }
        return new Term.LtD(arg1, arg2);
    }


    @Override
    public Term visit(Term.LeI term) {
        if (term.isConst()) {
            return TermFactory.c(term.evalB(null));
        }
        Term arg1 = apply(term.getArg(0));
        Term arg2 = apply(term.getArg(1));
        int c = arg1.compare(arg2);
        if (c == 0) {
            return Term.ConstB.TRUE;
        }
        return new Term.LeI(arg1, arg2);
    }

    @Override
    public Term visit(Term.LeD term) {
        if (term.isConst()) {
            return TermFactory.c(term.evalB(null));
        }
        Term arg1 = apply(term.getArg(0));
        Term arg2 = apply(term.getArg(1));
        int c = arg1.compare(arg2);
        if (c == 0) {
            return Term.ConstB.TRUE;
        }
        return new Term.LeD(arg1, arg2);
    }

    @Override
    public Term visit(Term.GtI term) {
        if (term.isConst()) {
            return TermFactory.c(term.evalB(null));
        }
        Term arg1 = apply(term.getArg(0));
        Term arg2 = apply(term.getArg(1));
        int c = arg1.compare(arg2);
        if (c == 0) {
            return Term.ConstB.FALSE;
        }
        return new Term.GtI(arg1, arg2);
    }

    @Override
    public Term visit(Term.GtD term) {
        if (term.isConst()) {
            return TermFactory.c(term.evalB(null));
        }
        Term arg1 = apply(term.getArg(0));
        Term arg2 = apply(term.getArg(1));
        int c = arg1.compare(arg2);
        if (c == 0) {
            return Term.ConstB.FALSE;
        }
        return new Term.GtD(arg1, arg2);
    }

    @Override
    public Term visit(Term.GeI term) {
        if (term.isConst()) {
            return TermFactory.c(term.evalB(null));
        }
        Term arg1 = apply(term.getArg(0));
        Term arg2 = apply(term.getArg(1));
        int c = arg1.compare(arg2);
        if (c == 0) {
            return Term.ConstB.TRUE;
        }
        return new Term.GeI(arg1, arg2);
    }

    @Override
    public Term visit(Term.GeD term) {
        if (term.isConst()) {
            return TermFactory.c(term.evalB(null));
        }
        Term arg1 = apply(term.getArg(0));
        Term arg2 = apply(term.getArg(1));
        int c = arg1.compare(arg2);
        if (c == 0) {
            return Term.ConstB.TRUE;
        }
        return new Term.GeD(arg1, arg2);
    }
}
