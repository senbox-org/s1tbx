package org.esa.snap.framework.dataop.barithm;

import com.bc.jexp.*;
import com.bc.jexp.impl.Functions;
import com.bc.jexp.impl.ParserImpl;
import com.bc.jexp.impl.TermSimplifier;
import org.esa.snap.framework.datamodel.Product;
import org.esa.snap.framework.datamodel.RasterDataNode;

/**
 * @author Norman Fomferra
 * @since SNAP 2
 */
public class RangeUncertaintyPropagator implements UncertaintyPropagator, TermConverter {

    TermSimplifier simplifier = new TermSimplifier();

    @Override
    public Term propagateUncertainties(Product product, String expression) throws ParseException {
        WritableNamespace namespace = product.createBandArithmeticDefaultNamespace();
        ParserImpl parser = new ParserImpl(namespace);
        Term term = parser.parse(expression);
        return uncertainty(term);
    }

    public Term uncertainty(Term term) {
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
        Symbol symbol = term.getSymbol();
        if (symbol instanceof RasterDataSymbol) {
            RasterDataSymbol rds = (RasterDataSymbol) symbol;
            RasterDataNode raster = rds.getRaster();
            RasterDataNode uncertainty = raster.getAncillaryBand("uncertainty");
            if (uncertainty != null) {
                return new Term.Ref(new RasterDataSymbol(uncertainty.getName(), uncertainty, RasterDataSymbol.GEOPHYSICAL));
            }

        }
        return Term.ConstD.ZERO;
    }

    @Override
    public Term visit(Term.Call term) {
        Term[] args = term.getArgs();
        Term[] minArgs = args.clone();
        Term[] maxArgs = args.clone();
        for (int i = 0; i < args.length; i++) {
            Term arg = args[i];
            minArgs[i] = new Term.Sub(Term.TYPE_D, arg, uncertainty(arg));
            maxArgs[i] = new Term.Add(Term.TYPE_D, arg, uncertainty(arg));
        }
        return new Term.Call(Functions.ABS_D,
                new Term.Sub(Term.TYPE_D,
                        new Term.Call(term.getFunction(), minArgs),
                        new Term.Call(term.getFunction(), maxArgs)));
    }

    @Override
    public Term visit(Term.Cond term) {
        return new Term.Cond(Term.TYPE_D, term.getArg(0), uncertainty(term.getArg(1)), uncertainty(term.getArg(2)));
    }

    @Override
    public Term visit(Term.Assign term) {
        // todo - test + implement me
        throw new UnsupportedOperationException();
    }

    @Override
    public Term visit(Term.NotB term) {
        // todo - test + implement me
        throw new UnsupportedOperationException();
    }

    @Override
    public Term visit(Term.AndB term) {
        // todo - test + implement me
        throw new UnsupportedOperationException();
    }

    @Override
    public Term visit(Term.OrB term) {
        // todo - test + implement me
        throw new UnsupportedOperationException();
    }

    @Override
    public Term visit(Term.NotI term) {
        // todo - test + implement me
        throw new UnsupportedOperationException();
    }

    @Override
    public Term visit(Term.XOrI term) {
        // todo - test + implement me
        throw new UnsupportedOperationException();

    }

    @Override
    public Term visit(Term.AndI term) {
        // todo - test + implement me
        throw new UnsupportedOperationException();
    }

    @Override
    public Term visit(Term.OrI term) {
        // todo - test + implement me
        throw new UnsupportedOperationException();
    }

    @Override
    public Term visit(Term.Neg term) {
        return uncertainty(term.getArg());
    }

    @Override
    public Term visit(Term.Add term) {
        return addAbsUncertain(term);
    }

    @Override
    public Term visit(Term.Sub term) {
        return addAbsUncertain(term);
    }

    @Override
    public Term visit(Term.Mul term) {
        return addRelUncertain(term);
    }

    @Override
    public Term visit(Term.Div term) {
        return addRelUncertain(term);
    }

    private Term addAbsUncertain(Term.Binary term) {
        Term uncert1 = uncertainty(term.getArg(0));
        Term uncert2 = uncertainty(term.getArg(1));
        return new Term.Add(Term.TYPE_D, uncert1, uncert2);
    }

    private Term addRelUncertain(Term.Binary term) {
        Term arg1 = term.getArg(0);
        Term arg2 = term.getArg(1);
        Term uncert1 = uncertainty(arg1);
        Term uncert2 = uncertainty(arg2);
        return new Term.Add(Term.TYPE_D,
                new Term.Div(Term.TYPE_D, uncert1, new Term.Call(Functions.ABS_D, arg1)),
                new Term.Div(Term.TYPE_D, uncert2, new Term.Call(Functions.ABS_D, arg2)));
    }

    @Override
    public Term visit(Term.Mod term) {
        // todo - test + implement me
        throw new UnsupportedOperationException();
    }

    @Override
    public Term visit(Term.EqB term) {
        // todo - test + implement me
        throw new UnsupportedOperationException();
    }

    @Override
    public Term visit(Term.EqI term) {
        // todo - test + implement me
        throw new UnsupportedOperationException();
    }

    @Override
    public Term visit(Term.EqD term) {
        // todo - test + implement me
        throw new UnsupportedOperationException();
    }

    @Override
    public Term visit(Term.NEqB term) {
        // todo - test + implement me
        throw new UnsupportedOperationException();
    }

    @Override
    public Term visit(Term.NEqI term) {
        // todo - test + implement me
        throw new UnsupportedOperationException();
    }

    @Override
    public Term visit(Term.NEqD term) {
        // todo - test + implement me
        throw new UnsupportedOperationException();
    }

    @Override
    public Term visit(Term.LtD term) {
        // todo - test + implement me
        throw new UnsupportedOperationException();
    }

    @Override
    public Term visit(Term.LtI term) {
        // todo - test + implement me
        throw new UnsupportedOperationException();
    }

    @Override
    public Term visit(Term.LeI term) {
        // todo - test + implement me
        throw new UnsupportedOperationException();
    }

    @Override
    public Term visit(Term.LeD term) {
        // todo - test + implement me
        throw new UnsupportedOperationException();
    }

    @Override
    public Term visit(Term.GtI term) {
        // todo - test + implement me
        throw new UnsupportedOperationException();
    }

    @Override
    public Term visit(Term.GtD term) {
        // todo - test + implement me
        throw new UnsupportedOperationException();
    }

    @Override
    public Term visit(Term.GeI term) {
        // todo - test + implement me
        throw new UnsupportedOperationException();
    }

    @Override
    public Term visit(Term.GeD term) {
        // todo - test + implement me
        throw new UnsupportedOperationException();
    }
}
