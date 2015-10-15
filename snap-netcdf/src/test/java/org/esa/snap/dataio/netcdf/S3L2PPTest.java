package org.esa.snap.dataio.netcdf;

import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.dataio.netcdf.metadata.profiles.cf.CfNetCdfReaderPlugIn;
import org.esa.snap.dataio.netcdf.util.NetcdfFileOpener;
import org.junit.Test;
import ucar.nc2.NetcdfFile;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

/**
 * @author Norman Fomferra
 */
public class S3L2PPTest {
    @Test
    public void testSYN() throws Exception {
        String path = System.getProperty("s3l2pp.synpath");
        if (path == null) {
            return;
        }

        File[] ncFiles = new File(path).listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".nc");
            }
        });

        for (File ncFile : ncFiles) {
            //ncdump(ncFile);

            try {
                final ProductReader reader = new CfNetCdfReaderPlugIn().createReaderInstance();
                final Product product = reader.readProductNodes(ncFile.getPath(), null);
                Band[] bands = product.getBands();
                System.out.println("product = " + product.getName());
                for (int i = 0; i < bands.length; i++) {
                    Band band = bands[i];
                    System.out.println("bands[" + i + "] = " + band.getName());
                }
            } catch (IOException e) {
                System.err.println("error: " + e.getMessage());
            }
        }
    }

    private static void ncdump(File ncFile) throws IOException {
        NetcdfFile netcdfFile = NetcdfFileOpener.open("file:" + ncFile.getPath());
        System.out.println(netcdfFile);
        netcdfFile.close();
    }
}
