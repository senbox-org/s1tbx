package org.esa.snap.framework.dataop.barithm;

import com.bc.ceres.core.Assert;
import com.bc.jexp.ParseException;
import com.bc.jexp.Symbol;
import com.bc.jexp.Term;
import com.bc.jexp.WritableNamespace;
import com.bc.jexp.impl.ParserImpl;
import com.bc.jexp.impl.TermFactory;
import org.esa.snap.framework.datamodel.Product;
import org.esa.snap.framework.datamodel.RasterDataNode;

import java.util.ArrayList;

import static com.bc.jexp.impl.TermFactory.add;
import static com.bc.jexp.impl.TermFactory.c;
import static com.bc.jexp.impl.TermFactory.derivative;
import static com.bc.jexp.impl.TermFactory.div;
import static com.bc.jexp.impl.TermFactory.mul;
import static com.bc.jexp.impl.TermFactory.pow;
import static com.bc.jexp.impl.TermFactory.ref;
import static com.bc.jexp.impl.TermFactory.simplify;
import static com.bc.jexp.impl.TermFactory.sqrt;

/**
 * @author Norman Fomferra
 * @since SNAP 2
 */
public class GaussianUncertaintyPropagator implements UncertaintyPropagator {

    private final boolean optimize;
    private final int maxOrder;

    public GaussianUncertaintyPropagator() {
        this(1, false);
    }

    public GaussianUncertaintyPropagator(int maxOrder, boolean optimize) {
        Assert.argument(maxOrder >= 1, "maxOrder >= 1");
        this.optimize = optimize;
        this.maxOrder = maxOrder;
    }

    @Override
    public Term propagateUncertainties(Product product, String expression) throws ParseException, UnsupportedOperationException {
        WritableNamespace namespace = product.createBandArithmeticDefaultNamespace();
        ParserImpl parser = new ParserImpl(namespace);
        Term term = parser.parse(expression);
        RasterDataSymbol[] symbols = BandArithmetic.getRefRasterDataSymbols(term);
        ArrayList<Term> uncertaintyContribTerms = new ArrayList<>();
        for (RasterDataSymbol symbol : symbols) {
            Term uncertainty = null;
            RasterDataNode uncertaintyRaster = symbol.getRaster().getAncillaryBand("uncertainty");
            RasterDataNode varianceRaster = symbol.getRaster().getAncillaryBand("variance");
            if (uncertaintyRaster != null) {
                Symbol uncertaintySymbol = namespace.resolveSymbol(uncertaintyRaster.getName());
                uncertainty = ref(uncertaintySymbol);
            } else if (varianceRaster != null) {
                Symbol varianceSymbol = namespace.resolveSymbol(varianceRaster.getName());
                uncertainty = sqrt(ref(varianceSymbol));
            }
            if (uncertainty != null) {
                Term partialDerivative = derivative(term, symbol);
                Term contrib = mul(partialDerivative, uncertainty);
                double f = 1.0;
                for (int order = 2; order <= maxOrder; order++) {
                    f *= order;
                    partialDerivative = derivative(partialDerivative, symbol);
                    contrib = add(contrib, mul(div(c(1.0), c(f)), mul(partialDerivative, pow(uncertainty, c(order)))));
                }
                uncertaintyContribTerms.add(contrib);
            }
        }
        if (uncertaintyContribTerms.isEmpty()) {
            return term.isConst() && Double.isNaN(term.evalD(null)) ? Term.ConstD.NAN : Term.ConstD.ZERO;
        }
        Term result = TermFactory.magnitude(uncertaintyContribTerms);
        Term simplifiedResult = simplify(result);
        return optimize ? TermFactory.optimize(simplifiedResult) : simplifiedResult;
    }


}
