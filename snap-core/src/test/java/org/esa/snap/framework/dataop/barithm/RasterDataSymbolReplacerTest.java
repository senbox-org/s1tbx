package org.esa.snap.framework.dataop.barithm;

import com.bc.jexp.Term;
import org.esa.snap.framework.datamodel.Product;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

/**
 * @author Norman
 */
public class RasterDataSymbolReplacerTest {
    @Test
    public void testIt() throws Exception {
        Product product = new Product("N", "T", 2, 2);
        product.addBand("B1", "1.5");
        product.addBand("B2", "0.5");

        Term term1 = BandArithmetic.parseExpression("B1 + 1 / B2", new Product[] {product}, 0);
        Term term2 = new RasterDataSymbolReplacer().apply(term1);

        assertEquals("Add(B1,Div(1,B2))", term1.toString());
        assertEquals("Add(B1,Div(1,B2))", term2.toString());

        RasterDataSymbol[] term1Symbols = BandArithmetic.getRefRasterDataSymbols(term1);
        RasterDataSymbol[] term2Symbols = BandArithmetic.getRefRasterDataSymbols(term2);
        assertEquals(2, term1Symbols.length);
        assertEquals(2, term2Symbols.length);
        equalSymbols(term1Symbols[0], term2Symbols[0]);
        equalSymbols(term1Symbols[1], term2Symbols[1]);
    }

    private void equalSymbols(RasterDataSymbol symbol1, RasterDataSymbol symbol2) {
        assertNotSame(symbol1, symbol2);
        assertEquals(symbol1.getName(), symbol2.getName());
        assertEquals(symbol1.getRetType(), symbol2.getRetType());
        assertSame(symbol1.getRaster(), symbol2.getRaster());
        assertSame(symbol1.getSource(), symbol2.getSource());
    }

}
