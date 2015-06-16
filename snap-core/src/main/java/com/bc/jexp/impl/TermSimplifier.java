package com.bc.jexp.impl;

import com.bc.jexp.Term;
import com.bc.jexp.TermConverter;

/**
 * Builds simplified/optimized versions of a given term.
 *
 * @author Norman Fomferra
 */
public class TermSimplifier implements TermConverter {

    public Term simplify(Term term) {
        return term.accept(this);
    }

    public Term[] simplify(Term... terms) {
        Term[] simpTerms = terms.clone();
        for (int i = 0; i < simpTerms.length; i++) {
            simpTerms[i] = simplify(simpTerms[i]);
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

        term = new Term.Call(term.getFunction(), simplify(term.getArgs()));

        if (term.isConst()) {
            if (term.isB()) {
                return Term.ConstB.get(term.evalB(null));
            } else if (term.isI()) {
                return Term.ConstI.get(term.evalI(null));
            } else if (term.isD()) {
                double value = term.evalD(null);
                if (value == 0.0) {
                    return Term.ConstD.ZERO;
                } else if (value == 1.0) {
                    return Term.ConstD.ONE;
                } else if (value == 2.0) {
                    return Term.ConstD.TWO;
                } else {
                    double valueF = Math.floor(value);
                    if (value == valueF) {
                        return new Term.ConstD(value);
                    }
                    double valueR = Math.round(value * 100.0) / 100.0;
                    if (value == valueR) {
                        return new Term.ConstD(value);
                    }
                }
            }
        }

        if (term.getFunction() == Functions.SQRT) {
            Term arg1 = term.getArg();
            Term arg2 = Term.ConstD.HALF;
            return simpPow(arg1, arg2);
        } else if (term.getFunction() == Functions.SQR) {
            Term arg1 = term.getArg();
            Term arg2 = Term.ConstD.TWO;
            return simpPow(arg1, arg2);
        } else if (term.getFunction() == Functions.POW) {
            Term arg1 = term.getArg(0);
            Term arg2 = term.getArg(1);
            return simpPow(arg1, arg2);
        }

        return term;
    }

    private Term simpPow(Term arg1, Term arg2) {
        if (arg2.isConst()) {
            double v = arg2.evalD(null);
            if (v == 0.0) {
                return Term.ConstD.ONE;
            } else if (v == 1.0) {
                return arg1;
            } else if (v == -1.0) {
                return simplify(new Term.Div(Term.TYPE_D, Term.ConstD.ONE, arg1));
            }
        }

        if (arg1 instanceof Term.Call && ((Term.Call) arg1).getFunction() == Functions.POW) {
            Term.Call powCall = (Term.Call) arg1;
            return simplifyNestedPowButConsiderSign(powCall, powCall.getArg(0), powCall.getArg(1), arg2);
        }
        if (arg1 instanceof Term.Call && ((Term.Call) arg1).getFunction() == Functions.SQRT) {
            Term.Call sqrtCall = (Term.Call) arg1;
            return simplifyNestedPowButConsiderSign(sqrtCall, sqrtCall.getArg(), Term.ConstD.HALF, arg2);
        }
        if (arg1 instanceof Term.Call && ((Term.Call) arg1).getFunction() == Functions.SQR) {
            Term.Call sqrCall = (Term.Call) arg1;
            return simplifyNestedPowButConsiderSign(sqrCall, sqrCall.getArg(), Term.ConstD.TWO, arg2);
        }

        return pow(arg1, arg2);
    }

    private Term simplifyNestedPowButConsiderSign(Term.Call innerCall, Term base, Term exp1, Term exp2) {
        boolean even1 = isEvenIntConst(exp1);
        boolean even2 = isEvenIntConst(exp2);
        // Check to only simplify nested POW's if we don't have to fear a sign suppression
        if (even1 == even2) {
            return simplify(simpPow(base, new Term.Mul(Term.TYPE_D, exp1, exp2)));
        } else {
            return pow(innerCall, exp2);
        }
    }

    private Term pow(Term arg1, Term arg2) {
        // Can't further simplify
        if (arg2.isConst()) {
            double v = arg2.evalD(null);
            if (v == 0.5) {
                return new Term.Call(Functions.SQRT, arg1);
            } else if (v == 2.0) {
                return new Term.Call(Functions.SQR, arg1);
            }
        }
        return new Term.Call(Functions.POW, arg1, arg2);
    }


    private boolean isEvenIntConst(Term term) {
        if (term.isConst()) {
            double n = term.evalD(null);
            if (n != 0.0 && Math.floor(n) == n && n % 2.0 == 0.0) {
                return true;
            }
        }
        return false;
    }


    @Override
    public Term visit(Term.Cond term) {
        if (term.getArg(0).isConst()) {
            boolean value = term.getArg(0).evalB(null);
            return simplify(term.getArg(value ? 1 : 2));
        }
        Term arg1 = simplify(term.getArg(0));
        Term arg2 = simplify(term.getArg(1));
        Term arg3 = simplify(term.getArg(2));
        if (arg2.compare(arg3) == 0) {
            return arg2;
        }
        return new Term.Cond(term.getRetType(), arg1, arg2, arg3);
    }

    @Override
    public Term visit(Term.Assign term) {
        return new Term.Assign(term.getArg(0), simplify(term.getArg(1)));
    }

    @Override
    public Term visit(Term.NotB term) {
        if (term.isConst()) {
            return Term.ConstB.get(term.evalB(null));
        }
        return new Term.NotB(simplify(term.getArg()));
    }

    @Override
    public Term visit(Term.AndB term) {
        if (term.isConst()) {
            return Term.ConstB.get(term.evalB(null));
        }
        return new Term.AndB(simplify(term.getArg(0)), simplify(term.getArg(1)));
    }

    @Override
    public Term visit(Term.OrB term) {
        if (term.isConst()) {
            return Term.ConstB.get(term.evalB(null));
        }
        return new Term.OrB(simplify(term.getArg(0)), simplify(term.getArg(1)));
    }

    @Override
    public Term visit(Term.NotI term) {
        if (term.isConst()) {
            return Term.ConstI.get(term.evalI(null));
        }
        return new Term.NotI(simplify(term.getArg()));
    }

    @Override
    public Term visit(Term.XOrI term) {
        if (term.isConst()) {
            return Term.ConstI.get(term.evalI(null));
        }
        return new Term.XOrI(simplify(term.getArg(0)), simplify(term.getArg(1)));
    }

    @Override
    public Term visit(Term.AndI term) {
        if (term.isConst()) {
            return Term.ConstI.get(term.evalI(null));
        }
        return new Term.AndI(simplify(term.getArg(0)), simplify(term.getArg(1)));
    }

    @Override
    public Term visit(Term.OrI term) {
        if (term.isConst()) {
            return Term.ConstI.get(term.evalI(null));
        }
        return new Term.OrI(simplify(term.getArg(0)), simplify(term.getArg(1)));
    }

    @Override
    public Term visit(Term.Neg term) {
        if (term.isConst()) {
            if (term.isB() || term.isI()) {
                return Term.ConstI.get(term.evalI(null));
            } else if (term.isD()) {
                return Term.ConstD.get(term.evalD(null));
            }
        }
        Term arg = simplify(term.getArg(0));
        if (arg instanceof Term.Neg) {
            return ((Term.Neg) arg).getArg();
        }
        return new Term.Neg(term.getRetType(), arg);
    }

    @Override
    public Term visit(Term.Add term) {
        if (term.isConst()) {
            if (term.isB()) {
                return Term.ConstB.get(term.evalB(null));
            } else if (term.isI()) {
                return Term.ConstI.get(term.evalI(null));
            } else if (term.isD()) {
                return Term.ConstD.get(term.evalD(null));
            }
        }

        Term arg1 = simplify(term.getArg(0));
        Term arg2 = simplify(term.getArg(1));
        if (arg1.isConst() && arg1.evalD(null) == 0.0) {
            return arg2;
        } else if (arg2.isConst() && arg2.evalD(null) == 0.0) {
            return simplify(term.getArg(0));
        }
        int c = arg1.compare(arg2);
        if (c == 0) {
            if (term.isI()) {
                return simplify(new Term.Mul(term.getRetType(), Term.ConstI.TWO, arg2));
            } else {
                return simplify(new Term.Mul(term.getRetType(), Term.ConstD.TWO, arg2));
            }
        } else if (c < 0) {
            return new Term.Add(term.getRetType(), arg1, arg2);
        } else {
            return simplify(new Term.Add(term.getRetType(), arg2, arg1));
        }
    }

    @Override
    public Term visit(Term.Sub term) {
        if (term.isConst()) {
            if (term.isB()) {
                return Term.ConstB.get(term.evalB(null));
            } else if (term.isI()) {
                return Term.ConstI.get(term.evalI(null));
            } else if (term.isD()) {
                return Term.ConstD.get(term.evalD(null));
            }
        }

        Term arg1 = simplify(term.getArg(0));
        Term arg2 = simplify(term.getArg(1));
        if (arg1.isConst() && arg1.evalD(null) == 0.0) {
            return simplify(new Term.Neg(arg2.getRetType(), arg2));
        }
        if (arg2.isConst() && arg2.evalD(null) == 0.0) {
            return arg1;
        }
        int c = arg1.compare(arg2);
        if (c == 0) {
            if (term.isI()) {
                return Term.ConstI.ZERO;
            } else {
                return Term.ConstD.ZERO;
            }
        }
        return new Term.Sub(term.getRetType(), arg1, arg2);
    }

    @Override
    public Term visit(Term.Mul term) {
        if (term.isConst()) {
            if (term.isB()) {
                return Term.ConstB.get(term.evalB(null));
            } else if (term.isI()) {
                return Term.ConstI.get(term.evalI(null));
            } else if (term.isD()) {
                return Term.ConstD.get(term.evalD(null));
            }
        }
        Term arg1 = simplify(term.getArg(0));
        Term arg2 = simplify(term.getArg(1));
        if (arg1.isConst()) {
            if (arg1.isI() && arg1.evalI(null) == 0) {
                return Term.ConstI.ZERO;
            } else if (arg1.isD() && arg1.evalD(null) == 0.0) {
                return Term.ConstD.ZERO;
            } else if (arg1.evalD(null) == 1.0) {
                return arg2;
            }
        }
        if (arg2.isConst()) {
            if (arg2.isI() && arg2.evalI(null) == 0) {
                return Term.ConstI.ZERO;
            } else if (arg2.isD() && arg2.evalD(null) == 0.0) {
                return Term.ConstD.ZERO;
            } else if (arg2.evalD(null) == 1.0) {
                return arg1;
            }
        }
        int c = arg1.compare(arg2);
        if (c == 0) {
            return simplify(new Term.Call(Functions.SQR, arg1));
        } else if (c < 0) {
            return new Term.Mul(term.getRetType(), arg1, arg2);
        } else {
            return simplify(new Term.Mul(term.getRetType(), arg2, arg1));
        }
    }

    @Override
    public Term visit(Term.Div term) {

        if (term.getArg(1).isConst() && term.getArg(1).evalD(null) == 0.0) {
            return Term.ConstD.NAN;
        }

        if (term.isConst()) {
            if (term.isB()) {
                return Term.ConstB.get(term.evalB(null));
            } else if (term.isI()) {
                return Term.ConstI.get(term.evalI(null));
            } else if (term.isD()) {
                return Term.ConstD.get(term.evalD(null));
            }
        }

        Term arg1 = simplify(term.getArg(0));
        Term arg2 = simplify(term.getArg(1));
        if (arg1.isConst() && arg1.evalD(null) == 0.0) {
            return arg1.isI() ? Term.ConstI.ZERO : Term.ConstD.ZERO;
        }
        if (arg2.isConst() && arg2.evalD(null) == 1.0) {
            return arg1;
        }
        int compare = arg1.compare(arg2);
        if (compare == 0) {
            if (term.isI()) {
                return Term.ConstI.ONE;
            } else {
                return Term.ConstD.ONE;
            }
        }
        return new Term.Div(term.getRetType(), arg1, arg2);
    }

    @Override
    public Term visit(Term.Mod term) {
        Term arg1 = simplify(term.getArg(0));
        Term arg2 = simplify(term.getArg(1));
        return new Term.Mod(term.getRetType(), arg1, arg2);
    }

    @Override
    public Term visit(Term.EqB term) {
        if (term.isConst()) {
            return Term.ConstB.get(term.evalB(null));
        }
        Term arg1 = simplify(term.getArg(0));
        Term arg2 = simplify(term.getArg(1));
        int c = arg1.compare(arg2);
        if (c == 0) {
            return Term.ConstB.TRUE;
        }
        return new Term.EqB(arg1, arg2);
    }

    @Override
    public Term visit(Term.EqI term) {
        if (term.isConst()) {
            return Term.ConstB.get(term.evalB(null));
        }
        Term arg1 = simplify(term.getArg(0));
        Term arg2 = simplify(term.getArg(1));
        int c = arg1.compare(arg2);
        if (c == 0) {
            return Term.ConstB.TRUE;
        }
        return new Term.EqI(arg1, arg2);
    }

    @Override
    public Term visit(Term.EqD term) {
        if (term.isConst()) {
            return Term.ConstB.get(term.evalB(null));
        }
        Term arg1 = simplify(term.getArg(0));
        Term arg2 = simplify(term.getArg(1));
        int c = arg1.compare(arg2);
        if (c == 0) {
            return Term.ConstB.TRUE;
        }
        return new Term.EqD(arg1, arg2);
    }

    @Override
    public Term visit(Term.NEqB term) {
        if (term.isConst()) {
            return Term.ConstB.get(term.evalB(null));
        }
        Term arg1 = simplify(term.getArg(0));
        Term arg2 = simplify(term.getArg(1));
        int c = arg1.compare(arg2);
        if (c == 0) {
            return Term.ConstB.FALSE;
        }
        return new Term.NEqB(arg1, arg2);
    }

    @Override
    public Term visit(Term.NEqI term) {
        if (term.isConst()) {
            return Term.ConstB.get(term.evalB(null));
        }
        Term arg1 = simplify(term.getArg(0));
        Term arg2 = simplify(term.getArg(1));
        int c = arg1.compare(arg2);
        if (c == 0) {
            return Term.ConstB.FALSE;
        }
        return new Term.NEqI(arg1, arg2);
    }

    @Override
    public Term visit(Term.NEqD term) {
        if (term.isConst()) {
            return Term.ConstB.get(term.evalB(null));
        }
        Term arg1 = simplify(term.getArg(0));
        Term arg2 = simplify(term.getArg(1));
        int c = arg1.compare(arg2);
        if (c == 0) {
            return Term.ConstB.FALSE;
        }
        return new Term.NEqD(arg1, arg2);
    }

    @Override
    public Term visit(Term.LtI term) {
        if (term.isConst()) {
            return Term.ConstB.get(term.evalB(null));
        }
        Term arg1 = simplify(term.getArg(0));
        Term arg2 = simplify(term.getArg(1));
        int c = arg1.compare(arg2);
        if (c == 0) {
            return Term.ConstB.FALSE;
        }
        return new Term.LtI(arg1, arg2);
    }

    @Override
    public Term visit(Term.LtD term) {
        if (term.isConst()) {
            return Term.ConstB.get(term.evalB(null));
        }
        Term arg1 = simplify(term.getArg(0));
        Term arg2 = simplify(term.getArg(1));
        int c = arg1.compare(arg2);
        if (c == 0) {
            return Term.ConstB.FALSE;
        }
        return new Term.LtD(arg1, arg2);
    }


    @Override
    public Term visit(Term.LeI term) {
        if (term.isConst()) {
            return Term.ConstB.get(term.evalB(null));
        }
        Term arg1 = simplify(term.getArg(0));
        Term arg2 = simplify(term.getArg(1));
        int c = arg1.compare(arg2);
        if (c == 0) {
            return Term.ConstB.TRUE;
        }
        return new Term.LeI(arg1, arg2);
    }

    @Override
    public Term visit(Term.LeD term) {
        if (term.isConst()) {
            return Term.ConstB.get(term.evalB(null));
        }
        Term arg1 = simplify(term.getArg(0));
        Term arg2 = simplify(term.getArg(1));
        int c = arg1.compare(arg2);
        if (c == 0) {
            return Term.ConstB.TRUE;
        }
        return new Term.LeD(arg1, arg2);
    }

    @Override
    public Term visit(Term.GtI term) {
        if (term.isConst()) {
            return Term.ConstB.get(term.evalB(null));
        }
        Term arg1 = simplify(term.getArg(0));
        Term arg2 = simplify(term.getArg(1));
        int c = arg1.compare(arg2);
        if (c == 0) {
            return Term.ConstB.FALSE;
        }
        return new Term.GtI(arg1, arg2);
    }

    @Override
    public Term visit(Term.GtD term) {
        if (term.isConst()) {
            return Term.ConstB.get(term.evalB(null));
        }
        Term arg1 = simplify(term.getArg(0));
        Term arg2 = simplify(term.getArg(1));
        int c = arg1.compare(arg2);
        if (c == 0) {
            return Term.ConstB.FALSE;
        }
        return new Term.GtD(arg1, arg2);
    }

    @Override
    public Term visit(Term.GeI term) {
        if (term.isConst()) {
            return Term.ConstB.get(term.evalB(null));
        }
        Term arg1 = simplify(term.getArg(0));
        Term arg2 = simplify(term.getArg(1));
        int c = arg1.compare(arg2);
        if (c == 0) {
            return Term.ConstB.TRUE;
        }
        return new Term.GeI(arg1, arg2);
    }

    @Override
    public Term visit(Term.GeD term) {
        if (term.isConst()) {
            return Term.ConstB.get(term.evalB(null));
        }
        Term arg1 = simplify(term.getArg(0));
        Term arg2 = simplify(term.getArg(1));
        int c = arg1.compare(arg2);
        if (c == 0) {
            return Term.ConstB.TRUE;
        }
        return new Term.GeD(arg1, arg2);
    }
}
