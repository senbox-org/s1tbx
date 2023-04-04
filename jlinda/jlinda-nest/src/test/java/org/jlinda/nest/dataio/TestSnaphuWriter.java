package org.jlinda.nest.dataio;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.dataio.dimap.DimapProductReaderPlugIn;
import org.esa.snap.core.datamodel.Product;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

public class TestSnaphuWriter {



    @Test
    public void testStuff() throws IOException {
        SnaphuExportOp snaphuExportOp = new SnaphuExportOp();
        DimapProductReaderPlugIn dimapProductReaderPlugIn = new DimapProductReaderPlugIn();
        ProductReader dimapReader = dimapProductReaderPlugIn.createReaderInstance();
        Product stack = dimapReader.readProductNodes(new File("C:\\Users\\alex\\Documents\\data\\pyrateprocessing\\S1B_IW_SLC__1SSV_20170514T122459_20170514T122524_005594_009CC4_39A6_Orb_Stack_mmifg_deb.dim"), null);

        System.out.println("This is a test");
        snaphuExportOp.setSourceProduct(stack);
        snaphuExportOp.setParameter("source", stack);
        //
        snaphuExportOp.setParameter("targetFolder", "C:/tmp");

        snaphuExportOp.initialize();



        snaphuExportOp.getTargetProduct();
        snaphuExportOp.execute(ProgressMonitor.NULL);

        SnaphuWriterPlugIn writerPlugIn = new SnaphuWriterPlugIn();
        SnaphuWriter writer = new SnaphuWriter(writerPlugIn);
        writer.createSnaphuConfFile(stack);





    }
}
