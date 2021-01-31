package org.esa.s1tbx.commons.test;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.engine_utilities.util.TestUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class ProcessorTest {

    static {
        TestUtils.initTestEnvironment();
    }

    protected File createTmpFolder(final String folderName) throws IOException {
        //File folder = Files.createTempDirectory(folderName).toFile();
        File folder = new File("c:\\out\\" + folderName);
        folder.mkdirs();
        return folder;
    }

    protected void validateProduct(final Product product) throws Exception {
        final ProductValidator productValidator = new ProductValidator(product);
        productValidator.validate();
    }

    protected void validateProduct(final Product product, final ProductValidator.ValidationOptions options) throws Exception {
        final ProductValidator productValidator = new ProductValidator(product, options);
        productValidator.validate();
    }

    protected void validateMetadata(final Product product) throws Exception {
        final MetadataValidator metadataValidator = new MetadataValidator(product);
        metadataValidator.validate();
    }

    protected void validateMetadata(final Product product, final MetadataValidator.ValidationOptions options) throws Exception {
        final MetadataValidator metadataValidator = new MetadataValidator(product, options);
        metadataValidator.validate();
    }

    protected void validateBands(final Product trgProduct, final String[] bandNames) throws Exception {
        final Band[] bands = trgProduct.getBands();
        if(bandNames.length != bands.length) {
            String msg = "";
            for(String bandName : bandNames) {
                if(!msg.isEmpty())
                    msg += ", ";
                msg += bandName;
            }
            throw new Exception("Expecting "+bandNames.length + " bands but found "+ bands.length +" "+ msg);
        }
        for(String bandName : bandNames) {
            Band band = trgProduct.getBand(bandName);
            if(band == null) {
                throw new Exception("Band "+ bandName +" not found");
            }
            if(band.getUnit() == null) {
                throw new Exception("Band "+ bandName +" is missing a unit");
            }
            if(!band.isNoDataValueUsed()) {
                throw new Exception("Band "+ bandName +" is not using a nodata value");
            }
        }
    }
}
