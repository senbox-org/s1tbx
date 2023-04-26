package org.esa.s1tbx.sar.gpf;

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
        Product product = reader.readProductNodes(new File("C:\\Users\\alex\\Downloads\\gis\\subset_2_of_S1B_IW_SLC__1SSV_20161209T122500_20161209T122524_003319_005AA1_5E64_Orb_Stack_mmifg_flt.dim"), null);
        pyRateExportOp.setSourceProduct(product);
        pyRateExportOp.setParameter("processingLocation", "C:/tmp/pyrateProcessing" );
        pyRateExportOp.setParameter("snaphuInstallLocation", "C:/tmp/pyrateDownload");
        Product trgProduct = pyRateExportOp.getTargetProduct();
    }
}
