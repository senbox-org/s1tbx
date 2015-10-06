package org.esa.snap.core.dataop.barithm;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.jexp.ParseException;
import org.esa.snap.core.jexp.Symbol;
import org.esa.snap.core.jexp.Term;
import org.esa.snap.core.jexp.TermTransformer;
import org.esa.snap.core.jexp.WritableNamespace;
import org.esa.snap.core.jexp.impl.Functions;
import org.esa.snap.core.jexp.impl.ParserImpl;
import org.esa.snap.core.jexp.impl.TermDecompiler;
import org.esa.snap.core.jexp.impl.TermSimplifier;

/**
 * @author Norman Fomferra
 * @since SNAP 2
 */
public class RangeUncertaintyGenerator implements UncertaintyGenerator {

    @Override
    public String generateUncertainty(Product product, String relation, String expression) throws ParseException, UnsupportedOperationException {
        WritableNamespace namespace = product.createBandArithmeticDefaultNamespace();
        ParserImpl parser = new ParserImpl(namespace);
        Term term = parser.parse(expression);
        Term result = new RangeUncertaintyTransformer(relation).apply(term);
        return new TermDecompiler().decompile(result);
    }

    private class RangeUncertaintyTransformer implements TermTransformer {
        private final TermSimplifier simplifier = new TermSimplifier();
        private final String relation;

        public RangeUncertaintyTransformer(String relation) {
            this.relation = relation;
        }

        @Override
        public Term apply(Term term) {
            return simplifier.apply(term.accept(this));
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
            return Double.isNaN(term.getValue()) ? Term.ConstD.NAN : Term.ConstD.ZERO;
        }

        @Override
        public Term visit(Term.ConstS term) {
            return Term.ConstD.ZERO;
        }

        @Override
        public Term visit(Term.Ref term) {
            Symbol symbol = term.getSymbol();
            if (symbol instanceof RasterDataSymbol) {
                RasterDataSymbol rds = (RasterDataSymbol) symbol;
                RasterDataNode raster = rds.getRaster();
                RasterDataNode uncertainty = raster.getAncillaryVariable(relation);
                if (uncertainty != null) {
                    return new Term.Ref(new RasterDataSymbol(uncertainty.getName(), uncertainty, RasterDataSymbol.GEOPHYSICAL));
                }
            }
            return Term.ConstD.ZERO;
        }

        @Override
        public Term visit(Term.Call term) {
            Term[] minArgs = getMinArgs(term.getArgs());
            Term[] maxArgs = getMaxArgs(term.getArgs());
            return maxDev(term,
                          new Term.Call(term.getFunction(), minArgs),
                          new Term.Call(term.getFunction(), maxArgs));
        }

        @Override
        public Term visit(Term.Cond term) {
            return new Term.Cond(Term.TYPE_D, term.getArg(0), apply(term.getArg(1)), apply(term.getArg(2)));
        }

        @Override
        public Term visit(Term.Neg term) {
            return apply(term.getArg());
        }

        @Override
        public Term visit(Term.Add term) {
            return addAbsUncertainies(term);
        }

        @Override
        public Term visit(Term.Sub term) {
            return addAbsUncertainies(term);
        }

        @Override
        public Term visit(Term.Mul term) {
            return addRelUncertainties(term);
        }

        @Override
        public Term visit(Term.Div term) {
            return addRelUncertainties(term);
        }

        @Override
        public Term visit(Term.Assign term) {
            return unsupportedOp(term);
        }

        @Override
        public Term visit(Term.NotB term) {
            return unsupportedOp(term);
        }

        @Override
        public Term visit(Term.AndB term) {
            return unsupportedOp(term);
        }

        @Override
        public Term visit(Term.OrB term) {
            return unsupportedOp(term);
        }

        @Override
        public Term visit(Term.NotI term) {
            return unsupportedOp(term);
        }

        @Override
        public Term visit(Term.XOrI term) {
            return unsupportedOp(term);
        }

        @Override
        public Term visit(Term.AndI term) {
            return unsupportedOp(term);
        }

        @Override
        public Term visit(Term.OrI term) {
            return unsupportedOp(term);
        }

        @Override
        public Term visit(Term.Mod term) {
            return unsupportedOp(term);
        }

        @Override
        public Term visit(Term.EqB term) {
            return unsupportedOp(term);
        }

        @Override
        public Term visit(Term.EqI term) {
            return unsupportedOp(term);
        }

        @Override
        public Term visit(Term.EqD term) {
            return unsupportedOp(term);
        }

        @Override
        public Term visit(Term.NEqB term) {
            return unsupportedOp(term);
        }

        @Override
        public Term visit(Term.NEqI term) {
            return unsupportedOp(term);
        }

        @Override
        public Term visit(Term.NEqD term) {
            return unsupportedOp(term);
        }

        @Override
        public Term visit(Term.LtD term) {
            return unsupportedOp(term);
        }

        @Override
        public Term visit(Term.LtI term) {
            return unsupportedOp(term);
        }

        @Override
        public Term visit(Term.LeI term) {
            return unsupportedOp(term);
        }

        @Override
        public Term visit(Term.LeD term) {
            return unsupportedOp(term);
        }

        @Override
        public Term visit(Term.GtI term) {
            return unsupportedOp(term);
        }

        @Override
        public Term visit(Term.GtD term) {
            return unsupportedOp(term);
        }

        @Override
        public Term visit(Term.GeI term) {
            return unsupportedOp(term);
        }

        @Override
        public Term visit(Term.GeD term) {
            return unsupportedOp(term);
        }

        private Term unsupportedOp(Term.Op term) {
            throw new UnsupportedOperationException("unsupported operation '" + term.getName() + "'");
        }

        // (c +- dc) = (a +- da) + (b +- db)
        // dc = da + db
        //
        private Term addAbsUncertainies(Term.Binary term) {
            Term uncert1 = apply(term.getArg(0));
            Term uncert2 = apply(term.getArg(1));
            return new Term.Add(Term.TYPE_D, uncert1, uncert2);
        }

        // (c +- dc) = (a +- da) * (b +- db)
        // dc/abs(c) = da/abs(a) + db/abs(b)
        //
        private Term addRelUncertainties(Term.Binary term) {
            Term arg1 = term.getArg(0);
            Term arg2 = term.getArg(1);
            Term uncert1 = apply(arg1);
            Term uncert2 = apply(arg2);
            return new Term.Mul(Term.TYPE_D,
                                new Term.Add(Term.TYPE_D,
                                             new Term.Div(Term.TYPE_D,
                                                          uncert1,
                                                          new Term.Call(Functions.ABS_D, arg1)),
                                             new Term.Div(Term.TYPE_D,
                                                          uncert2,
                                                          new Term.Call(Functions.ABS_D, arg2))),
                                new Term.Call(Functions.ABS_D, term));
        }

        // Rough deviation estimation:
        //
        // (c +- dc) = F(a +- da, b +- db)
        // dc =~ max(abs(F(a - da, b - db) - F(a, b)),
        //           abs(F(a + da, b + db) - F(a, b)))
        // where
        //    term := F(a, b)
        //    minTerm := F(a - da, b - db)
        //    maxTerm := F(a + da, b + db)
        //
        private Term maxDev(Term term, Term minTerm, Term maxTerm) {
            return new Term.Call(Functions.MAX_D,
                                 new Term.Call(Functions.ABS_D,
                                               new Term.Sub(Term.TYPE_D,
                                                            minTerm,
                                                            term)),
                                 new Term.Call(Functions.ABS_D,
                                               new Term.Sub(Term.TYPE_D,
                                                            maxTerm,
                                                            term)));
        }

        private Term[] getMinArgs(Term[] args) {
            Term[] minArgs = args.clone();
            for (int i = 0; i < args.length; i++) {
                Term arg = args[i];
                minArgs[i] = new Term.Sub(Term.TYPE_D, arg, apply(arg));
            }
            return minArgs;
        }

        private Term[] getMaxArgs(Term[] args) {
            Term[] maxArgs = args.clone();
            for (int i = 0; i < args.length; i++) {
                Term arg = args[i];
                maxArgs[i] = new Term.Add(Term.TYPE_D, arg, apply(arg));
            }
            return maxArgs;
        }
    }
}
