
package org.esa.s1tbx.teststacks;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.s1tbx.commons.test.ProcessorTest;
import org.esa.s1tbx.insar.gpf.coregistration.CreateStackOp;
import org.esa.s1tbx.io.productgroup.ProductGroupIO;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.GPF;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit test for CreateStackOp.
 */
public class TestProductGroupWriting extends ProcessorTest {

    private final static File f1 = new File("E:\\EO\\RS2\\ASMERS\\ManitobaFrame\\RS2_OK28321_PK282196_DK256363_FQ10W_20120613_001629_HH_VV_HV_VH_SLC.zip");
    private final static File f2 = new File("E:\\EO\\RS2\\ASMERS\\ManitobaFrame\\RS2_OK28321_PK282211_DK256378_FQ10W_20120707_001627_HH_VV_HV_VH_SLC.zip");
    private final static File f3 = new File("E:\\EO\\RS2\\ASMERS\\ManitobaFrame\\RS2_OK28321_PK282224_DK256391_FQ10W_20120731_001628_HH_VV_HV_VH_SLC.zip");
    private final static File f4 = new File("E:\\EO\\RS2\\ASMERS\\ManitobaFrame\\RS2_OK28551_PK284031_DK257628_FQ10W_20120824_001629_HH_VV_HV_VH_SLC.zip");
    private final static File f5 = new File("E:\\EO\\RS2\\ASMERS\\ManitobaFrame\\RS2_OK28551_PK284040_DK257637_FQ10W_20120917_001630_HH_VV_HV_VH_SLC.zip");

    private Product[] readProducts(final File folder) throws IOException {
        assertTrue(folder.isDirectory());
        File[] files = folder.listFiles();
        return readProducts(files);
    }

    private Product[] readProducts(final File[] files) throws IOException {
        final List<Product> productList = new ArrayList<>();
        if(files != null) {
            for(File file : files) {
                Product product = ProductIO.readProduct(file);
                if(product != null) {
                    productList.add(product);
                }
            }
        }
        return productList.toArray(new Product[0]);
    }

    private Product createStackProduct(final Product[] products) {
        CreateStackOp createStackOp = new CreateStackOp();
        int cnt = 0;
        for(Product product : products) {
            createStackOp.setSourceProduct("input"+cnt, product);
            ++cnt;
        }

        return createStackOp.getTargetProduct();
    }

    @Test
    public void testProductGroupWithProductGroupIO() throws IOException {
        final Product[] products = readProducts(new File[] {f1, f2});
        final Product outProduct = createStackProduct(products);

        File tmpFolder = createTmpFolder("group1");
        ProductGroupIO.writeProduct(outProduct, new File(tmpFolder,"group1.dim"), "BEAM-DIMAP", true);

        //tmpFolder.delete();
    }

    @Test
    public void testProductGroupWithProductGroupIO2() throws IOException {
        final Product[] products = readProducts(new File[] {f1, f2});
        final Product outProduct = createStackProduct(products);

        File tmpFolder = createTmpFolder("group2");

        ProductGroupIO.writeProduct(outProduct, new File(tmpFolder,"group2.dim"), "BEAM-DIMAP", true);

        //tmpFolder.delete();
    }

    @Test
    public void testProductGroupWithGPF() throws IOException {
        final Product[] products = readProducts(new File[] {f1, f2});
        final Product outProduct = createStackProduct(products);

        File tmpFolder = createTmpFolder("group3");

        GPF.writeProduct(outProduct, new File(tmpFolder,"group3.dim"), "BEAM-DIMAP", true, ProgressMonitor.NULL);

        //tmpFolder.delete();
    }

    @Test
    public void testProductGroupWithGPF2() throws IOException {
        final Product[] products = readProducts(new File[] {f1, f2});
        final Product outProduct = createStackProduct(products);

        File tmpFolder = createTmpFolder("group4");

        GPF.writeProduct(outProduct, new File(tmpFolder,"group4.dim"), "BEAM-DIMAP", true, ProgressMonitor.NULL);

        //tmpFolder.delete();
    }
}
