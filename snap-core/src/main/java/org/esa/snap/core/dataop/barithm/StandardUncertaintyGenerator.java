package org.esa.snap.core.dataop.barithm;

import com.bc.ceres.core.Assert;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.jexp.ParseException;
import org.esa.snap.core.jexp.Symbol;
import org.esa.snap.core.jexp.Term;
import org.esa.snap.core.jexp.WritableNamespace;
import org.esa.snap.core.jexp.impl.ParserImpl;
import org.esa.snap.core.jexp.impl.TermDecompiler;
import org.esa.snap.core.jexp.impl.TermFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.esa.snap.core.jexp.impl.TermFactory.*;

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
    public String generateUncertainty(Product product, String relation, String expression) throws ParseException, UnsupportedOperationException {
        WritableNamespace namespace = product.createBandArithmeticDefaultNamespace();
        ParserImpl parser = new ParserImpl(namespace);
        Term term = parser.parse(expression);
        RasterDataSymbol[] symbols = BandArithmetic.getRefRasterDataSymbols(term);
        HashMap<Symbol, Term> variables = new HashMap<>();

        for (RasterDataSymbol variable : symbols) {
            RasterDataNode[] uncertaintyRasters = variable.getRaster().getAncillaryVariables(relation);
            if (uncertaintyRasters.length > 0) {
                List<Term> symbolRefs = Stream.of(uncertaintyRasters)
                        .map(r -> ref(namespace.resolveSymbol(r.getName())))
                        .collect(Collectors.toList());

                variables.put(variable, symbolRefs.size() == 1 ? symbolRefs.get(0) : magnitude(symbolRefs));
            }
        }

        ArrayList<Term> uncertaintySum = new ArrayList<>();

        for (Symbol variable : variables.keySet()) {
            Term uncertainty = variables.get(variable);
            Term partialDerivative = derivative(term, variable);
            Term contrib = mul(partialDerivative, uncertainty);
            uncertaintySum.add(contrib);
        }

        if (maxOrder >= 2) {
            Term contrib = null;
            for (Symbol variable1 : variables.keySet()) {
                Term uncertainty1 = variables.get(variable1);
                Term partialDerivative1 = derivative(term, variable1);
                for (Symbol variable2 : variables.keySet()) {
                    Term uncertainty2 = variables.get(variable2);
                    Term partialDerivative2 = derivative(partialDerivative1, variable2);
                    Term singleContrib = mul(mul(partialDerivative2,
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
                Term uncertainty1 = variables.get(variable1);
                Term partialDerivative1 = derivative(term, variable1);
                for (Symbol variable2 : variables.keySet()) {
                    Term uncertainty2 = variables.get(variable2);
                    Term partialDerivative2 = derivative(partialDerivative1, variable2);
                    for (Symbol variable3 : variables.keySet()) {
                        Term uncertainty3 = variables.get(variable3);
                        Term partialDerivative3 = derivative(partialDerivative2, variable3);
                        Term singleContrib = mul(mul(mul(partialDerivative3,
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
        Term result;
        if (uncertaintySum.isEmpty()) {
            result = term.isConst() && Double.isNaN(term.evalD(null)) ? Term.ConstD.NAN : Term.ConstD.ZERO;
        } else {
            result = TermFactory.magnitude(uncertaintySum);
            result = simplify(result);
            result = optimize ? TermFactory.optimize(result) : result;
        }
        return new TermDecompiler().decompile(result);
    }


}
