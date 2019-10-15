package org.esa.s1tbx.benchmark;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.common.SubsetOp;

import java.awt.*;
import java.io.File;
import java.io.IOException;

public class TestBenchmarks {

    protected final static File grdFile = new File("E:\\TestData\\s1tbx\\SAR\\S1\\AWS\\S1A_IW_GRDH_1SDV_20180719T002854_20180719T002919_022856_027A78_042A\\manifest.safe");
    protected final static File qpFile = new File("E:\\TestData\\s1tbx\\SAR\\RS2\\RS2_OK2084_PK24911_DK25857_FQ14_20080802_225909_HH_VV_HV_VH_SLC\\product.xml");

    protected final static File outputFolder = new File("e:\\out");
    protected final static Rectangle rect = new Rectangle(1000, 1000, 3000, 3000);

    protected Product subset(final File file, final Rectangle rect) throws IOException {
        final Product srcProduct = ProductIO.readProduct(file);
        SubsetOp op = new SubsetOp();
        op.setSourceProduct(srcProduct);
        op.setCopyMetadata(true);
        op.setRegion(rect);
        return op.getTargetProduct();
    }

    protected void write(final Product trgProduct) throws IOException {
        ProductIO.writeProduct(trgProduct, new File(outputFolder, trgProduct.getName()), "BEAM-DIMAP", false);
    }

    protected void writeGPF(final Product trgProduct) {
        GPF.writeProduct(trgProduct, new File(outputFolder, trgProduct.getName()), "BEAM-DIMAP", false, ProgressMonitor.NULL);
    }
}
