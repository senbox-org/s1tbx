package org.esa.beam.framework.gpf.jpy;


import org.esa.beam.framework.gpf.main.GPT;

/**
 * @author Norman Fomferra
 */
public class PyOperatorTest {
    public static void main(String[] args) throws Exception {
        GPT.main("PyOp",
                 "-q", "4",
                 "-e",
                 "-PpythonModulePath=C:\\Users\\Norman\\JavaProjects\\beam\\beam\\beam-python\\src\\main\\resources\\examples",
                 "-PpythonModuleName=ndvi_op",
                 "-PpythonClassName=MerisNdviTileComputer",
                 "C:\\Users\\Norman\\JavaProjects\\jpy\\beampy\\MER_RR__1P.N1");
    }
}
