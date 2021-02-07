package org.esa.s1tbx.io.productgroup;

import org.esa.s1tbx.commons.test.ProcessorTest;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Product;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertNotNull;


public class TestProductGroupReader extends ProcessorTest {

    @Test
    public void testWrite() throws Exception {
        final File file = new File("C:\\out\\productgroups\\product_group.json");

        Product product = ProductIO.readProduct(file);
        assertNotNull(product);
    }

}
