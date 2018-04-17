package org.esa.s1tbx.commons.test;

import org.esa.snap.core.dataio.DecodeQualification;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.util.TestUtils;
import org.junit.Assert;

import java.io.File;

public class ReaderTest {

    private final ProductReaderPlugIn readerPlugIn;
    private final ProductReader reader;

    static {
        TestUtils.initTestEnvironment();
    }

    public ReaderTest(final ProductReaderPlugIn readerPlugIn) {
        this.readerPlugIn = readerPlugIn;
        this.reader = readerPlugIn.createReaderInstance();
    }

    protected void testReader(final File inputFile) throws Exception {
        if(!inputFile.exists()){
            TestUtils.skipTest(this, inputFile +" not found");
            return;
        }

        final DecodeQualification canRead = readerPlugIn.getDecodeQualification(inputFile);
        Assert.assertTrue(canRead == DecodeQualification.INTENDED);

        final Product product = reader.readProductNodes(inputFile, null);
        Assert.assertTrue(product != null);

        TestUtils.verifyProduct(product, true, true);
        validateMetadata(product);
    }

    protected void validateMetadata(final Product trgProduct) throws Exception {
        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(trgProduct);
        final MetadataAttribute[] attribs = absRoot.getAttributes();

        for(MetadataAttribute attrib : attribs) {
            System.out.println(attrib.getName() +"= "+ attrib.getData().toString());
        }
    }
}
