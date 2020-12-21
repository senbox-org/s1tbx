package org.esa.s1tbx.commons.test;

import org.esa.snap.core.dataio.DecodeQualification;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.engine_utilities.util.TestUtils;

import java.nio.file.Files;
import java.nio.file.Path;

public class ReaderTest extends ProcessorTest {

    protected final ProductReaderPlugIn readerPlugIn;
    protected final ProductReader reader;
    protected boolean verifyTime = true;
    protected boolean verifyGeoCoding = true;

    public ReaderTest(final ProductReaderPlugIn readerPlugIn) {
        this.readerPlugIn = readerPlugIn;
        this.reader = readerPlugIn.createReaderInstance();
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
}
