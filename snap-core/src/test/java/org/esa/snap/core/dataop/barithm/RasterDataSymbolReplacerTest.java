package org.esa.snap.core.dataop.barithm;

import org.esa.snap.core.datamodel.FlagCoding;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.jexp.Term;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Norman
 */
public class RasterDataSymbolReplacerTest {

    @Test
    public void testApply() throws Exception {
        Product product = new Product("N", "T", 2, 2);
        product.addBand("B1", "1.5");
        product.addBand("B2", "0.5");
        product.addBand("B3", "1", ProductData.TYPE_INT8);
        FlagCoding fc = new FlagCoding("FC");
        fc.addFlag("INV", 0x01, "bad one");
        product.getFlagCodingGroup().add(fc);
        product.getBand("B3").setSampleCoding(fc);

        Term term1 = BandArithmetic.parseExpression("B3.INV ? 1 - B1 / B2 : 0", new Product[]{product}, 0);
        Term term2 = new RasterDataSymbolReplacer().apply(term1);

        assertEquals("Cond(B3.INV,Sub(1,Div(B1,B2)),0)", term1.toString());
        assertEquals(term1.toString(), term2.toString());

        RasterDataSymbol[] term1Symbols = BandArithmetic.getRefRasterDataSymbols(term1);
        RasterDataSymbol[] term2Symbols = BandArithmetic.getRefRasterDataSymbols(term2);
        assertEquals(3, term1Symbols.length);
        assertEquals(3, term2Symbols.length);
        for (int i = 0; i < term2Symbols.length; i++) {
            RasterDataSymbol actualSymbol = term2Symbols[i];
            RasterDataSymbol expectedSymbol = term1Symbols[i];
            String message = "term2Symbols[" + i + "] (" + actualSymbol.getName() + ")";
            assertNotSame(message, expectedSymbol, actualSymbol);
            assertEquals(message, expectedSymbol.getName(), actualSymbol.getName());
            assertEquals(message, expectedSymbol.getRetType(), actualSymbol.getRetType());
            assertSame(message, expectedSymbol.getRaster(), actualSymbol.getRaster());
            assertSame(message, expectedSymbol.getSource(), actualSymbol.getSource());
        }
    }
}
