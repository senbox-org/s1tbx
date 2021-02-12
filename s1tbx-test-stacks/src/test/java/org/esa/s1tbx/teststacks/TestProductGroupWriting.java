
package org.esa.s1tbx.teststacks;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.s1tbx.commons.test.ProcessorTest;
import org.esa.s1tbx.insar.gpf.coregistration.CreateStackOp;
import org.esa.s1tbx.io.productgroup.support.ProductGroupIO;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.util.io.FileUtils;
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

    private List<Product> readProducts(final File folder) throws IOException {
        assertTrue(folder.isDirectory());
        File[] files = folder.listFiles();
        return readProducts(files);
    }

    private List<Product> readProducts(final File[] files) throws IOException {
        final List<Product> productList = new ArrayList<>();
        if(files != null) {
            for(File file : files) {
                Product product = ProductIO.readProduct(file);
                if(product != null) {
                    productList.add(product);
                }
            }
        }
        return productList;
    }

    private void closeProducts(final List<Product> products) {
        for(Product product : products) {
            if(product != null)
                product.dispose();
        }
    }

    private Product createStackProduct(final List<Product> products) {
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
        final List<Product> products = readProducts(new File[] {f1, f2, f3});
        final Product outProduct = createStackProduct(products);

        File tmpFolder = createTmpFolder("group1");
        File productFile = new File(tmpFolder,"productIO1_group1.dim");
        ProductGroupIO.writeProduct(outProduct, productFile, "BEAM-DIMAP", true);

        closeProducts(products);
        outProduct.dispose();
        assertTrue(FileUtils.deleteTree(tmpFolder));
    }

    @Test
    public void testProductGroupWithGPF() throws IOException {
        final List<Product> inputProducts1 = readProducts(new File[] {f1, f2});
        final Product outProduct1 = createStackProduct(inputProducts1);

        File tmpFolder = createTmpFolder("group3");
        File stackProductFile = new File(tmpFolder,"gpf1_group3");
        stackProductFile = ProductGroupIO.operatorWrite(outProduct1, stackProductFile, "BEAM-DIMAP", ProgressMonitor.NULL);

        closeProducts(inputProducts1);
        outProduct1.dispose();

        Product stackProduct = ProductIO.readProduct(stackProductFile);
        final List<Product> inputProducts2 = readProducts(new File[] {f3});
        final List<Product> stackProductList = new ArrayList<>();
        stackProductList.add(stackProduct);
        stackProductList.addAll(inputProducts2);

        final Product outProduct2 = createStackProduct(stackProductList);

        //GPF.writeProduct(outProduct2, stackProductFile2, "BEAM-DIMAP", true, ProgressMonitor.NULL);
        ProductGroupIO.operatorWrite(outProduct2, stackProductFile, "BEAM-DIMAP", ProgressMonitor.NULL);

        closeProducts(inputProducts2);
        outProduct2.dispose();
        //assertTrue(FileUtils.deleteTree(tmpFolder));
    }
}
