package org.esa.beam.dataio.modis;

import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.framework.datamodel.Product;

import java.io.IOException;

// used to run acceptance tests on the MODIS reader stuff
public class ModisTestMain {

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            throw new IllegalStateException("must supply MODIS input filepath as argument");
        }

        final ModisProductReaderPlugIn plugIn = new ModisProductReaderPlugIn();
        final DecodeQualification decodeQualification = plugIn.getDecodeQualification(args[0]);
        System.out.println("decodeQualification = " + decodeQualification);
        if (decodeQualification == DecodeQualification.UNABLE) {
            return;
        }

        final ModisProductReader productReader = new ModisProductReader(plugIn);
        final Product product = productReader.readProductNodes(args[0], null);
    }

}
