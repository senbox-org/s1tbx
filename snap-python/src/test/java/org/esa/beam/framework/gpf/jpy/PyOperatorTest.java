package org.esa.beam.framework.gpf.jpy;


import org.esa.beam.framework.gpf.main.GPT;
import org.junit.Ignore;
import org.junit.Test;

/**
 * @author Norman Fomferra
 */
public class PyOperatorTest {
    private final static String MODULE_PATH = "C:\\Users\\Norman\\JavaProjects\\senbox\\snap-engine\\snap-python\\target\\classes\\beampy-examples";

    static {
        System.setProperty("snap.snappy.ext", MODULE_PATH);
    }



    @Ignore
    public static void main(String[] args) throws Exception {
        GPT.main("py_ndvi_op",
                 "-q", "4",
                 "-e",
                 "-PlowerName=radiance_13",
                 "-PupperName=radiance_7",
                 "-Ssource=C:\\Users\\Norman\\EOData\\MER_FRS_1PNMAP20070709_111419_000001722059_00395_28004_0001.N1");
    }

    @Test
    public void testOp() throws Exception {
        PyOperator operator = new PyOperator();
        operator.setPythonModulePath(MODULE_PATH);
        operator.setPythonModuleName("ndvi_op");
        operator.setPythonClassName("NdviOp");

    }
}
