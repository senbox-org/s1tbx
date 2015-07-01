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
import java.util.HashMap;

import static com.bc.jexp.impl.TermFactory.add;
import static com.bc.jexp.impl.TermFactory.c;
import static com.bc.jexp.impl.TermFactory.derivative;
import static com.bc.jexp.impl.TermFactory.div;
import static com.bc.jexp.impl.TermFactory.mul;
import static com.bc.jexp.impl.TermFactory.ref;
import static com.bc.jexp.impl.TermFactory.simplify;

/**
 * Implementation of an {@link UncertaintyGenerator} which generates the Standard Combined Uncertainty
 * according to JCGM 100:2008 (GUM 1995), Chapter 5.
 *
 * @author Norman Fomferra
 * @since SNAP 2
 */
public class StandardUncertaintyGenerator implements UncertaintyGenerator {

    private final boolean optimize;
    private final int maxOrder;

    public StandardUncertaintyGenerator() {
        this(1, false);
    }

    public StandardUncertaintyGenerator(int order, boolean optimize) {
        Assert.argument(order >= 1 && order <= 3, "order >= 1 && order <= 3");
        this.optimize = optimize;
        this.maxOrder = order;
    }

    @Override
    public Term generateUncertainty(Product product, String expression) throws ParseException, UnsupportedOperationException {
        WritableNamespace namespace = product.createBandArithmeticDefaultNamespace();
        ParserImpl parser = new ParserImpl(namespace);
        Term term = parser.parse(expression);
        RasterDataSymbol[] symbols = BandArithmetic.getRefRasterDataSymbols(term);
        HashMap<Symbol, Symbol> variables = new HashMap<>();

        for (RasterDataSymbol variable : symbols) {
            RasterDataNode uncertaintyRaster = variable.getRaster().getAncillaryVariable("uncertainty");
            if (uncertaintyRaster != null) {
                Symbol uncertainty = namespace.resolveSymbol(uncertaintyRaster.getName());
                Assert.notNull(uncertainty, "uncertainty");
                variables.put(variable, uncertainty);
            }
        }

        ArrayList<Term> uncertaintySum = new ArrayList<>();

        for (Symbol variable : variables.keySet()) {
            Symbol uncertaintySymbol = variables.get(variable);
            Term uncertainty = ref(uncertaintySymbol);
            Term partialDerivative = derivative(term, variable);
            Term contrib = mul(partialDerivative, uncertainty);
            uncertaintySum.add(contrib);
        }

        if (maxOrder >= 2) {
            Term contrib = null;
            for (Symbol variable1 : variables.keySet()) {
                Symbol uncertaintySymbol1 = variables.get(variable1);
                Term uncertainty1 = ref(uncertaintySymbol1);
                Term partialDerivative1 = derivative(term, variable1);
                for (Symbol variable2 : variables.keySet()) {
                    Symbol uncertaintySymbol2 = variables.get(variable2);
                    Term uncertainty2 = ref(uncertaintySymbol2);
                    Term partialDerivative2 = derivative(term, variable2);
                    Term singleContrib = mul(mul(mul(partialDerivative1,
                                                     partialDerivative2),
                                                 uncertainty1),
                                             uncertainty2);
                    contrib = contrib != null ? add(contrib, singleContrib) : singleContrib;
                }
            }
            if (contrib != null) {
                uncertaintySum.add(div(contrib, c(2.0)));
            }
        }

        if (maxOrder == 3) {
            Term contrib = null;
            for (Symbol variable1 : variables.keySet()) {
                Symbol uncertaintySymbol1 = variables.get(variable1);
                Term uncertainty1 = ref(uncertaintySymbol1);
                Term partialDerivative1 = derivative(term, variable1);
                for (Symbol variable2 : variables.keySet()) {
                    Symbol uncertaintySymbol2 = variables.get(variable2);
                    Term uncertainty2 = ref(uncertaintySymbol2);
                    Term partialDerivative2 = derivative(term, variable2);
                    for (Symbol variable3 : variables.keySet()) {
                        Symbol uncertaintySymbol3 = variables.get(variable3);
                        Term uncertainty3 = ref(uncertaintySymbol3);
                        Term partialDerivative3 = derivative(term, variable3);
                        Term singleContrib = mul(mul(mul(mul(mul(partialDerivative1,
                                                                 partialDerivative2),
                                                             partialDerivative3),
                                                         uncertainty1),
                                                     uncertainty2),
                                                 uncertainty3);
                        contrib = contrib != null ? add(contrib, singleContrib) : singleContrib;
                    }
                }
            }
            if (contrib != null) {
                uncertaintySum.add(div(contrib, c(6.0)));
            }
        }

        if (uncertaintySum.isEmpty()) {
            return term.isConst() && Double.isNaN(term.evalD(null)) ? Term.ConstD.NAN : Term.ConstD.ZERO;
        }
        Term result = TermFactory.magnitude(uncertaintySum);
        Term simplifiedResult = simplify(result);
        return optimize ? TermFactory.optimize(simplifiedResult) : simplifiedResult;
    }


}
