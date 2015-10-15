package org.esa.snap.core.dataop.barithm;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.jexp.ParseException;
import org.junit.Before;


/**
 * Created by Norman on 13.06.2015.
 */
public abstract class UncertaintyGeneratorTest {
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
        band1.addAncillaryVariable(band1Err, "uncertainty");

        Band band2 = product.addBand("B2", "Y");
        Band band2Err = product.addBand("B2_unc", "0.1 * Y");
        band2.addAncillaryVariable(band2Err, "uncertainty");


        product.addBand("B3", "X+Y");

        Band band4 = product.addBand("B4", "X*Y");
        Band band4Err = product.addBand("B4_var", "0.1 * X * X");
        band4.addAncillaryVariable(band4Err, "variance");

    }

    protected String uncert(String expression) throws ParseException {
        return createUncertaintyGenerator().generateUncertainty(getProduct(), "uncertainty", expression);
    }

    protected abstract UncertaintyGenerator createUncertaintyGenerator();


    public Product getProduct() {
        return product;
    }
}
