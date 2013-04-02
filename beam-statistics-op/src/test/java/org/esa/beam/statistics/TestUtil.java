package org.esa.beam.statistics;

import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.util.Guardian;

import java.io.File;
import java.io.IOException;

public class TestUtil {

    public static Product getTestProduct() throws IOException {
        return ProductIO.readProduct(TestUtil.class.getResource("testProduct1.dim").getFile());
    }

    public static void deleteTreeOnExit(File tree) {
        Guardian.assertNotNull("tree", tree);

        File[] files = tree.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteTreeOnExit(file);
                } else {
                    file.deleteOnExit();
                }
            }
        }
        tree.deleteOnExit();
    }
}
