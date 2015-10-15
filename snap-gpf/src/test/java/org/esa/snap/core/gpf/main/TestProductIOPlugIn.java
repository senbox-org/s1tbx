package org.esa.snap.core.gpf.main;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.dataio.AbstractProductReader;
import org.esa.snap.core.dataio.AbstractProductWriter;
import org.esa.snap.core.dataio.DecodeQualification;
import org.esa.snap.core.dataio.EncodeQualification;
import org.esa.snap.core.dataio.ProductIOPlugInManager;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.dataio.ProductWriter;
import org.esa.snap.core.dataio.ProductWriterPlugIn;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.util.io.SnapFileFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * @author Norman Fomferra
 */
public class TestProductIOPlugIn implements ProductReaderPlugIn, ProductWriterPlugIn {

    public static final Class<Object> PARAMETER_TYPE = Object.class;
    public static final String FORMAT_NAME = "TESTDATA";
    public static final String FILE_EXT = ".testdata";
    public static final String FORMAT_DESCRIPTION = "Testdata format for unit-level testing";

    public static final TestProductIOPlugIn INSTANCE = new TestProductIOPlugIn();
    private final Map<Object, Product> sourceProducts = Collections.synchronizedMap(new HashMap<Object, Product>());
    private final Map<Object, Product> targetProducts = Collections.synchronizedMap(new HashMap<Object, Product>());

    static {
        ProductIOPlugInManager.getInstance().addReaderPlugIn(INSTANCE);
        ProductIOPlugInManager.getInstance().addWriterPlugIn(INSTANCE);
    }

    public Map<Object, Product> getSourceProducts() {
        return sourceProducts;
    }

    public Map<Object, Product> getTargetProducts() {
        return targetProducts;
    }

    public void clear() {
        sourceProducts.clear();
        targetProducts.clear();
    }

    @Override
    public EncodeQualification getEncodeQualification(Product product) {
        return EncodeQualification.FULL;
    }

    @Override
    public DecodeQualification getDecodeQualification(Object input) {
        return sourceProducts.containsKey(input) ? DecodeQualification.INTENDED : DecodeQualification.UNABLE;
    }

    @Override
    public Class[] getInputTypes() {
        return new Class[]{PARAMETER_TYPE};
    }

    @Override
    public Class[] getOutputTypes() {
        return new Class[]{PARAMETER_TYPE};
    }

    @Override
    public ProductReader createReaderInstance() {
        return new TestProductReader(this);
    }

    @Override
    public ProductWriter createWriterInstance() {
        return new TestProductWriter(this);
    }

    @Override
    public String[] getFormatNames() {
        return new String[]{FORMAT_NAME};
    }

    @Override
    public String[] getDefaultFileExtensions() {
        return new String[]{FILE_EXT};
    }

    @Override
    public String getDescription(Locale locale) {
        return FORMAT_DESCRIPTION;
    }

    @Override
    public SnapFileFilter getProductFileFilter() {
        return null;
    }

    public static class TestProductReader extends AbstractProductReader {

        public TestProductReader(TestProductIOPlugIn readerPlugIn) {
            super(readerPlugIn);
        }

        @Override
        public TestProductIOPlugIn getReaderPlugIn() {
            return (TestProductIOPlugIn) super.getReaderPlugIn();
        }

        @Override
        protected Product readProductNodesImpl() throws IOException {
            return getReaderPlugIn().getSourceProducts().get(getInput());
        }

        @Override
        protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY,
                                              int sourceWidth, int sourceHeight, int sourceStepX, int sourceStepY,
                                              Band destBand, int destOffsetX, int destOffsetY, int destWidth, int destHeight,
                                              ProductData destBuffer, ProgressMonitor pm) throws IOException {
        }
    }

    public static class TestProductWriter extends AbstractProductWriter {

        public TestProductWriter(TestProductIOPlugIn writerPlugIn) {
            super(writerPlugIn);
        }

        @Override
        public TestProductIOPlugIn getWriterPlugIn() {
            return (TestProductIOPlugIn) super.getWriterPlugIn();
        }

        @Override
        protected void writeProductNodesImpl() throws IOException {
            getWriterPlugIn().getTargetProducts().put(getOutput(), getSourceProduct());
        }

        @Override
        public void writeBandRasterData(Band sourceBand, int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight,
                                        ProductData sourceBuffer, ProgressMonitor pm) throws IOException {
        }

        @Override
        public void flush() throws IOException {
        }

        @Override
        public void close() throws IOException {
        }

        @Override
        public void deleteOutput() throws IOException {
            getWriterPlugIn().getTargetProducts().remove(getOutput());
        }
    }
}
