package org.esa.nest.gpf;

import com.bc.ceres.core.ProgressMonitor;
import junit.framework.TestCase;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.esa.nest.util.TestUtils;

import java.io.File;

/**
 * Test performance of tie point grid geocoding
 */
public class TestTiePoints  extends TestCase {

    final File inputFile = new File("g:\\out\\ALOS-H1_1__A-ORBIT__ALPSRP076360690_DS.dim");

    private Product product1 = null;
    private Product product2 = null;

    @Override
    protected void setUp() throws Exception {
        TestUtils.initTestEnvironment();

        if(inputFile.exists()) {
            product1 = ProductIO.readProduct(inputFile);
            product2 = ProductIO.readProduct(inputFile);
        }
    }

    @Override
    protected void tearDown() throws Exception {

    }

    public void testGetPixelFloat() throws Exception {
        if(product1 == null) {
            TestUtils.skipTest(this);
            return;
        }
        TiePointGrid tpg = product1.getTiePointGridAt(0);
        int w = product1.getSceneRasterWidth();
        int h = product1.getSceneRasterHeight();

        float[] floats1 = new float[w*h];
        int i=0;
        for(int x=0; x < w; ++x) {
            for(int y=0; y < h; ++y) {
                floats1[i++] = tpg.getPixelFloat(x,y);
            }
        }

    }

    public void testGetPixelFloats() throws Exception {
        if(product2 == null) {
            TestUtils.skipTest(this);
            return;
        }
        TiePointGrid tpg = product2.getTiePointGridAt(0);
        int w = product2.getSceneRasterWidth();
        int h = product2.getSceneRasterHeight();

        float[] floats = new float[w*h];
        tpg.getPixels(0,0, w,h, floats, ProgressMonitor.NULL);
    }

    public void testCompareFloats() throws Exception {
        if(product1 == null) {
            TestUtils.skipTest(this);
            return;
        }
        TiePointGrid tpg = product1.getTiePointGridAt(0);
        int w = product1.getSceneRasterWidth();
        int h = product1.getSceneRasterHeight();

        float[] floats = new float[w*h];
        tpg.getPixels(0,0, w,h, floats, ProgressMonitor.NULL);

        int i=0;
        for(int y=0; y < h; ++y) {
            for(int x=0; x < w; ++x) {
                float f = tpg.getPixelFloat(x, y);
                System.out.println(f+"    "+ floats[y*w+x]);
            }
        }
    }
}
