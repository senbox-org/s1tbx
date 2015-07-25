package org.esa.snap.framework.dataop.barithm;

import com.bc.jexp.AbstractTermTransformer;
import com.bc.jexp.Symbol;
import com.bc.jexp.Term;

/**
 * An implementation of a term transformer which replaces all occurrences of {@link RasterDataSymbol}s
 * by the ones created by the {@link #createReplacement(RasterDataSymbol)} method.
 *
 * @author Norman Fomferra
 */
public class RasterDataSymbolReplacer extends AbstractTermTransformer {

    @Override
    public Term visit(Term.Ref term) {
        if (term.getSymbol() instanceof RasterDataSymbol) {
            return new Term.Ref(createReplacement((RasterDataSymbol) term.getSymbol()));
        } else {
            return super.visit(term);
        }
    }

    protected Symbol createReplacement(RasterDataSymbol symbol) {
        return symbol.clone();
    }
}