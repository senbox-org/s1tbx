package org.esa.beam.dataio.modis;

import ncsa.hdf.hdflib.HDFException;
import org.esa.beam.framework.dataio.DecodeQualification;

import java.io.IOException;

// used to run acceptance tests on the MODIS reader stuff
public class ModisTestMain {

    public static void main(String[] args) throws IOException, HDFException {
        if (args.length != 1) {
            throw new IllegalStateException("must supply MODIS input filepath as argument");
        }

        final ModisProductReaderPlugIn plugIn = new ModisProductReaderPlugIn();
        final DecodeQualification decodeQualification = plugIn.getDecodeQualification(args[0]);
        System.out.println("decodeQualification = " + decodeQualification);
        if (decodeQualification == DecodeQualification.UNABLE) {
            return;
        }

//        final ModisProductReader productReader = new ModisProductReader(plugIn);
//        final Product product = productReader.readProductNodes(args[0], null);
//
//        if (product!= null) {
//            product.dispose();
//        }
    }

}
