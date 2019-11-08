package org.esa.s1tbx.commons.test;

import org.esa.snap.core.dataio.DecodeQualification;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.util.TestUtils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

public class ReaderTest {

    protected final ProductReaderPlugIn readerPlugIn;
    protected final ProductReader reader;
    protected boolean verifyTime = true;
    protected boolean verifyGeocoding = true;

    static {
        TestUtils.initTestEnvironment();
    }

    public ReaderTest(final ProductReaderPlugIn readerPlugIn) {
        this.readerPlugIn = readerPlugIn;
        this.reader = readerPlugIn.createReaderInstance();
    }

    @Deprecated
    protected Product testReader(final File inputFile) throws Exception {
        return testReader(inputFile, readerPlugIn);
    }

    @Deprecated
    protected Product testReader(final File inputFile, final ProductReaderPlugIn readerPlugIn) throws Exception {
        if(!inputFile.exists()){
            TestUtils.skipTest(this, inputFile +" not found");
            return null;
        }

        final DecodeQualification canRead = readerPlugIn.getDecodeQualification(inputFile);
        if(canRead != DecodeQualification.INTENDED) {
            throw new Exception("Reader not intended");
        }

        final ProductReader reader = readerPlugIn.createReaderInstance();
        final Product product = reader.readProductNodes(inputFile, null);
        if(product == null) {
            throw new Exception("Unable to read product");
        }

        TestUtils.verifyProduct(product, verifyTime, verifyGeocoding);
        validateMetadata(product);

        return product;
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

        TestUtils.verifyProduct(product, verifyTime, verifyGeocoding);
        validateMetadata(product);

        return product;
    }

    protected void validateMetadata(final Product trgProduct) throws Exception {
        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(trgProduct);
        final MetadataAttribute[] attribs = absRoot.getAttributes();

        for(MetadataAttribute attrib : attribs) {
            System.out.println(attrib.getName() +"= "+ attrib.getData().toString());
        }
    }
}
