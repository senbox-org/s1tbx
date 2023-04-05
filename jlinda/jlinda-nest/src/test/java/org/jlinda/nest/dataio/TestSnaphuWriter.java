package org.jlinda.nest.dataio;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.dataio.dimap.DimapProductReaderPlugIn;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.dataio.envi.EnviProductReader;
import org.esa.snap.dataio.envi.EnviProductReaderPlugIn;
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
        writer.setOutputDir(new File("C:/tmp"));
        //writer.createSnaphuConfFile(stack);

        EnviProductReaderPlugIn enviProductReaderPlugIn = new EnviProductReaderPlugIn();
        ProductReader reader = enviProductReaderPlugIn.createReaderInstance();
        String [] productNames = new String[]{"UnwPhase_ifg_08Apr2017_02May2017.snaphu.hdr",
                "UnwPhase_ifg_20Apr2017_02May2017.snaphu.hdr",
                "UnwPhase_ifg_27Mar2017_02May2017.snaphu.hdr",
                "UnwPhase_ifg_27Mar2017_08Apr2017.snaphu.hdr"};

        String product2_name = "UnwPhase_ifg_20Apr2017_02May2017.snaphu.hdr";
        String product3_name = "UnwPhase_ifg_27Mar2017_02May2017.snaphu.hdr";
        String product4_name = "UnwPhase_ifg_27Mar2017_08Apr2017.snaphu.hdr";
        Product [] enviProducts = new Product[4];
        enviProducts[0] = reader.readProductNodes(new File("C:\\Users\\alex\\Documents\\data\\pyrateprocessing\\snaph\\subset_0_of_S1B_IW_SLC__1SSV_20170514T122459_20170514T122524_005594_009CC4_39A6_Orb_Stack_mmifg_deb", productNames[0]), null);
        for (int x = 1; x < 4; x++){
            File file = new File("C:\\Users\\alex\\Documents\\data\\pyrateprocessing\\snaph\\subset_0_of_S1B_IW_SLC__1SSV_20170514T122459_20170514T122524_005594_009CC4_39A6_Orb_Stack_mmifg_deb", productNames[x]);
            Product product = reader.readProductNodes(file, null);
            ProductUtils.copyBand(product.getBandAt(0).getName(), product, enviProducts[0], true);
            System.out.println(product.toString());
        }
        ProductIO.writeProduct(enviProducts[0], "C:/tmp/unwrapped", "BEAM-DIMAP");



        //Product product1 = reader.readProductNodes(new File("C:\\Users\\alex\\Documents\\data\\pyrateprocessing\\snaph\\subset_0_of_S1B_IW_SLC__1SSV_20170514T122459_20170514T122524_005594_009CC4_39A6_Orb_Stack_mmifg_deb\\UnwPhase_ifg_08Apr2017_02May2017.snaphu.hdr"), null);










    }
}
