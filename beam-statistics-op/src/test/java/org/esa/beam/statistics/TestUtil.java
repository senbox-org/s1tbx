package org.esa.beam.statistics;

import java.io.IOException;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Product;

public class TestUtil {

    public static Product getTestProduct() throws IOException {
        return ProductIO.readProduct(TestUtil.class.getResource("testProduct1.dim").getFile());
    }

}
