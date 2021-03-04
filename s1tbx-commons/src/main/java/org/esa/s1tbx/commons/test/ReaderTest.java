package org.esa.s1tbx.commons.test;

import org.esa.snap.core.dataio.DecodeQualification;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.engine_utilities.util.TestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ReaderTest {

    protected final ProductReaderPlugIn readerPlugIn;
    protected final ProductReader reader;
    protected boolean verifyTime = true;
    protected boolean verifyGeoCoding = true;

    static {
        TestUtils.initTestEnvironment();
    }

    public ReaderTest(final ProductReaderPlugIn readerPlugIn) {
        this.readerPlugIn = readerPlugIn;
        this.reader = readerPlugIn.createReaderInstance();
    }

    protected void close() throws IOException {
        reader.close();
    }

    protected Product testReader(final Path inputPath) throws Exception {
        return testReader(inputPath, readerPlugIn);
    }

    protected Product testReader(final Path inputPath, final ProductReaderPlugIn readerPlugIn) throws Exception {
        if(!Files.exists(inputPath)){
            TestUtils.skipTest(this, inputPath +" not found");
            return null;
        }

        final DecodeQualification canRead = readerPlugIn.getDecodeQualification(inputPath);
        if(canRead != DecodeQualification.INTENDED) {
            throw new Exception("Reader not intended");
        }

        final ProductReader reader = readerPlugIn.createReaderInstance();
        final Product product = reader.readProductNodes(inputPath, null);
        if(product == null) {
            throw new Exception("Unable to read product");
        }

        return product;
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
            String expectedBandNames = "";
            for(String bandName : trgProduct.getBandNames()) {
                if(!expectedBandNames.isEmpty())
                    expectedBandNames += ", ";
                expectedBandNames += bandName;
            }
            String actualBandNames = "";
            for(String bandName : bandNames) {
                if(!actualBandNames.isEmpty())
                    actualBandNames += ", ";
                actualBandNames += bandName;
            }
            throw new Exception("Expecting "+bandNames.length + " bands "+actualBandNames+" but found "+ bands.length +" "+ expectedBandNames);
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
