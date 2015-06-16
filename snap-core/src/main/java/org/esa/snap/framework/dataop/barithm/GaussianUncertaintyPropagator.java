package org.esa.snap.framework.dataop.barithm;

import com.bc.jexp.ParseException;
import com.bc.jexp.Symbol;
import com.bc.jexp.Term;
import com.bc.jexp.TermConverter;
import com.bc.jexp.WritableNamespace;
import com.bc.jexp.impl.Functions;
import com.bc.jexp.impl.ParserImpl;
import com.bc.jexp.impl.TermDerivator;
import com.bc.jexp.impl.TermSimplifier;
import org.esa.snap.framework.datamodel.Product;
import org.esa.snap.framework.datamodel.RasterDataNode;

import java.util.ArrayList;

/**
 * @author Norman Fomferra
 * @since SNAP 2
 */
public class GaussianUncertaintyPropagator implements UncertaintyPropagator {

    private final boolean optimize;

    public GaussianUncertaintyPropagator() {
        this(false);
    }

    public GaussianUncertaintyPropagator(boolean optimize) {
        this.optimize = optimize;
    }

    @Override
    public Term propagateUncertainties(Product product, String expression) throws ParseException, UnsupportedOperationException {
        WritableNamespace namespace = product.createBandArithmeticDefaultNamespace();
        ParserImpl parser = new ParserImpl(namespace);
        Term term = parser.parse(expression);
        RasterDataSymbol[] symbols = BandArithmetic.getRefRasterDataSymbols(term);
        ArrayList<Term> terms = new ArrayList<>();
        for (RasterDataSymbol symbol : symbols) {
            RasterDataNode uncertaintyRaster = symbol.getRaster().getAncillaryBand("uncertainty");
            RasterDataNode varianceRaster = symbol.getRaster().getAncillaryBand("variance");
            if (uncertaintyRaster != null) {
                Term partialDerivative = new TermDerivator(symbol).derivative(term);
                Symbol uncertaintySymbol = namespace.resolveSymbol(uncertaintyRaster.getName());
                Term sqrTerm = new Term.Call(Functions.SQR,
                                             new Term.Mul(Term.TYPE_D,
                                                          partialDerivative,
                                                          new Term.Ref(uncertaintySymbol)));
                terms.add(sqrTerm);
            } else if (varianceRaster != null) {
                Term partialDerivative = new TermDerivator(symbol).derivative(term);
                Symbol varianceSymbol = namespace.resolveSymbol(varianceRaster.getName());
                Term sqrTerm = new Term.Mul(Term.TYPE_D,
                                            new Term.Call(Functions.SQR, partialDerivative),
                                            new Term.Ref(varianceSymbol));
                terms.add(sqrTerm);
            }
        }
        if (terms.isEmpty()) {
            return term.isConst() && Double.isNaN(term.evalD(null)) ? Term.ConstD.NAN : Term.ConstD.ZERO;
        }
        Term result;
        if (terms.size() == 1) {
            result = terms.get(0);
        } else {
            result = null;
            for (Term arg : terms) {
                if (result == null) {
                    result = arg;
                } else {
                    result = new Term.Add(Term.TYPE_D, result, arg);
                }
            }
        }
        result = new Term.Call(Functions.SQRT, result);
        Term simplifiedResult = new TermSimplifier().simplify(result);
        return optimize ? new Optimizer().optimize(simplifiedResult) : simplifiedResult;
    }


    private static class Optimizer implements TermConverter {

        private Term optimize(Term term) {
            return term.accept(this);
        }

        private Term[] optimize(Term[] terms) {
            Term[] clone = terms.clone();
            for (int i = 0; i < clone.length; i++) {
                clone[i] = optimize(terms[i]);
            }
            return clone;
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
            if (term.getFunction() == Functions.SQRT) {
                Term arg = term.getArg();
                if (arg instanceof Term.Call) {
                    Term.Call innerCall = (Term.Call) arg;
                    if (innerCall.getFunction() == Functions.SQR) {
                        Term innerArg = innerCall.getArg();
                        return new Term.Call(Functions.ABS_D, optimize(innerArg));
                    }
                }
            }
            return new Term.Call(term.getFunction(), optimize(term.getArgs()));
        }

        @Override
        public Term visit(Term.Cond term) {
            return new Term.Cond(term.getRetType(), term.getArg(0), optimize(term.getArg(1)), optimize(term.getArg(2)));
        }

        @Override
        public Term visit(Term.Assign term) {
            return term;
        }

        @Override
        public Term visit(Term.NotB term) {
            return term;
        }

        @Override
        public Term visit(Term.AndB term) {
            return term;
        }

        @Override
        public Term visit(Term.OrB term) {
            return term;
        }

        @Override
        public Term visit(Term.NotI term) {
            return term;
        }

        @Override
        public Term visit(Term.XOrI term) {
            return term;
        }

        @Override
        public Term visit(Term.AndI term) {
            return term;
        }

        @Override
        public Term visit(Term.OrI term) {
            return term;
        }

        @Override
        public Term visit(Term.Neg term) {
            return new Term.Neg(term.getRetType(), optimize(term.getArg()));
        }

        @Override
        public Term visit(Term.Add term) {
            return new Term.Add(term.getRetType(), optimize(term.getArg(0)), optimize(term.getArg(1)));
        }

        @Override
        public Term visit(Term.Sub term) {
            return new Term.Sub(term.getRetType(), optimize(term.getArg(0)), optimize(term.getArg(1)));
        }

        @Override
        public Term visit(Term.Mul term) {
            return new Term.Mul(term.getRetType(), optimize(term.getArg(0)), optimize(term.getArg(1)));
        }

        @Override
        public Term visit(Term.Div term) {
            return new Term.Div(term.getRetType(), optimize(term.getArg(0)), optimize(term.getArg(1)));
        }

        @Override
        public Term visit(Term.Mod term) {
            return new Term.Mod(term.getRetType(), optimize(term.getArg(0)), optimize(term.getArg(1)));
        }

        @Override
        public Term visit(Term.EqB term) {
            return term;
        }

        @Override
        public Term visit(Term.EqI term) {
            return term;
        }

        @Override
        public Term visit(Term.EqD term) {
            return term;
        }

        @Override
        public Term visit(Term.NEqB term) {
            return term;
        }

        @Override
        public Term visit(Term.NEqI term) {
            return term;
        }

        @Override
        public Term visit(Term.NEqD term) {
            return term;
        }

        @Override
        public Term visit(Term.LtI term) {
            return term;
        }

        @Override
        public Term visit(Term.LtD term) {
            return term;
        }

        @Override
        public Term visit(Term.LeI term) {
            return term;
        }

        @Override
        public Term visit(Term.LeD term) {
            return term;
        }

        @Override
        public Term visit(Term.GtI term) {
            return term;
        }

        @Override
        public Term visit(Term.GtD term) {
            return term;
        }

        @Override
        public Term visit(Term.GeI term) {
            return term;
        }

        @Override
        public Term visit(Term.GeD term) {
            return term;
        }
    }
}
