package org.esa.beam.dataio.modis;

import org.esa.beam.framework.dataio.DecodeQualification;

// used to run acceptance tests on the MODIS reader stuff
public class ModisTestMain {

    public static void main(String[] args) {
        if (args.length != 1) {
            throw new IllegalStateException("must supply MODIS input filepath as argument");
        }

        final ModisProductReaderPlugIn plugIn = new ModisProductReaderPlugIn();
        final DecodeQualification decodeQualification = plugIn.getDecodeQualification(args[0]);
        System.out.println("decodeQualification = " + decodeQualification);

    }

}
