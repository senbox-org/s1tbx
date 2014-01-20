package org.esa.beam.framework.gpf.jpy;


import org.esa.beam.framework.gpf.main.GPT;

/**
 * @author Norman Fomferra
 */
public class PyOperatorTest {

    public static void main(String[] args) throws Exception {
        GPT.main("PyOp", "-PpythonModuleName=beampy_op", "-PtileComputerClassName=BiboTileComputer", "-e", "C:\\Users\\Norman\\JavaProjects\\jpy\\beampy\\MER_RR__1P.N1");
    }

}
