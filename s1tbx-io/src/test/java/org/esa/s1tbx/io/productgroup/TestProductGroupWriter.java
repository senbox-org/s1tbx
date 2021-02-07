package org.esa.s1tbx.io.productgroup;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.s1tbx.commons.test.ProcessorTest;
import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.util.TestUtils;
import org.junit.Test;

import java.io.File;

public class TestProductGroupWriter extends ProcessorTest {

    @Test
    public void testWrite() throws Exception {
        Product product = createStackProduct();
        addMetadata(product);

        final File targetFolder = createTmpFolder("productgroups");
        ProductGroupIO.operatorWrite(product, targetFolder, "BEAM-DIMAP", ProgressMonitor.NULL);
    }


    private Product createStackProduct() {
        final int w = 10, h = 10;
        final Product product = TestUtils.createProduct("type", w, h);

        for(int i=1; i < 5; ++i) {
            TestUtils.createBand(product, "band" + i, w, h);
        }

        return product;
    }

    private void addMetadata(final Product product) {
        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(product);

        MetadataAttribute attribute = AbstractMetadata.addAbstractedAttribute(absRoot, AbstractMetadata.collocated_stack, ProductData.TYPE_INT8, "", "");
        attribute.getData().setElemInt(1);

    }
}
