package org.esa.s1tbx.insar.gpf;

import org.esa.s1tbx.commons.test.ProcessorTest;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.dataio.dimap.DimapProductReaderPlugIn;
import org.esa.snap.core.datamodel.Product;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

public class TestPyRateExportOp extends ProcessorTest  {

    @Test
    public void testPyRateExportOp() throws IOException {
        PyRateExportOp pyRateExportOp = new PyRateExportOp();
        DimapProductReaderPlugIn productReaderPlugIn = new DimapProductReaderPlugIn();
        ProductReader reader = productReaderPlugIn.createReaderInstance();
        Product product = reader.readProductNodes(new File("C:\\Users\\alex\\Documents\\data\\pyrateprocessing\\subset_0_of_S1B_IW_SLC__1SSV_20170514T122459_20170514T122524_005594_009CC4_39A6_Orb_Stack_mmifg_deb.dim"), null);
        pyRateExportOp.setSourceProduct(product);
        pyRateExportOp.setParameter("snaphuProcessingLocation", "C:/tmp/pyrateProcessing" );
        pyRateExportOp.setParameter("snaphuInstallLocation", "C:/tmp/pyrateDownload");
        Product trgProduct = pyRateExportOp.getTargetProduct();
    }
}
