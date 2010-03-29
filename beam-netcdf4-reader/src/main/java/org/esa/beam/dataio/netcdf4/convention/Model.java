package org.esa.beam.dataio.netcdf4.convention;

import org.esa.beam.dataio.netcdf4.Nc4ReaderParameters;
import org.esa.beam.framework.datamodel.Product;
import ucar.nc2.NetcdfFileWriteable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class Model {

    private InitialisationPart initialisationPart;
    private final List<ModelPart> modelParts = new ArrayList<ModelPart>();

    public Product readProduct(final String productName, Nc4ReaderParameters rp) throws IOException {
        final Product product = initialisationPart.readProductBody(productName, rp);
        if (modelParts != null) {
            for (ModelPart modelPart : modelParts) {
                modelPart.read(product, rp);
            }
        }
        return product;
    }

    public void writeProduct(final NetcdfFileWriteable writeable, final Product p) throws IOException {
        final HeaderDataWriter hdw = new HDW();
        initialisationPart.writeProductBody(writeable, p);
        for (ModelPart modelPart : modelParts) {
            modelPart.write(p, writeable, hdw);
        }
        writeable.create();
        hdw.writeHeaderData();
    }

    public void addModelPart(ModelPart modelPart) {
        this.modelParts.add(modelPart);
    }

    public void setInitialisationPart(InitialisationPart initPart) {
        this.initialisationPart = initPart;
    }

    public static class HDW implements HeaderDataWriter {

        private LinkedList<HeaderDataJob> jobList;

        @Override
        public void registerHeaderDataJob(HeaderDataJob job) {
            if (job != null) {
                if (jobList == null) {
                    jobList = new LinkedList<HeaderDataJob>();
                }
                jobList.add(job);
            }
        }

        @Override
        public void writeHeaderData() throws IOException {
            if (jobList != null) {
                for (HeaderDataJob job : jobList) {
                    job.go();
                }
                jobList = null;
            }
        }
    }
}
