package org.esa.snap.framework.dataop.barithm;

import com.bc.jexp.ParseException;
import com.bc.jexp.Term;
import com.bc.jexp.impl.TermDecompiler;
import org.esa.snap.framework.datamodel.Band;
import org.esa.snap.framework.datamodel.Product;
import org.junit.Before;


/**
 * Created by Norman on 13.06.2015.
 */
public abstract class UncertaintyPropagatorTest {
    /*
     * Band B1 has uncertainty B1_unc
     * Band B2 has uncertainty B2_unc
     * Band B3 does not have any uncertainty info
     * Band B4 has variance B4_var
     */
    private Product product;

    @Before
    public void setUp() throws Exception {
        product = new Product("N", "T", 10, 10);
        Band band1 = product.addBand("B1", "X");
        Band band1Err = product.addBand("B1_unc", "0.1 * X");
        band1.setAncillaryBand("uncertainty", band1Err);

        Band band2 = product.addBand("B2", "Y");
        Band band2Err = product.addBand("B2_unc", "0.1 * Y");
        band2.setAncillaryBand("uncertainty", band2Err);


        product.addBand("B3", "X+Y");

        Band band4 = product.addBand("B4", "X*Y");
        Band band4Err = product.addBand("B4_var", "0.1 * X * X");
        band4.setAncillaryBand("variance", band4Err);

    }

    protected String uncert(String expression) throws ParseException {
        Term term = createUncertaintyPropagator().propagateUncertainties(getProduct(), expression);
        return new TermDecompiler().decompile(term);
    }

    protected abstract UncertaintyPropagator createUncertaintyPropagator();


    public Product getProduct() {
        return product;
    }
}
