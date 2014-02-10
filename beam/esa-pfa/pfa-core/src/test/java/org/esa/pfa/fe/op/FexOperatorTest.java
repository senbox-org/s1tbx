package org.esa.pfa.fe.op;

import com.bc.ceres.binding.PropertySet;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.GPF;
import org.esa.pfa.fe.op.out.PatchOutput;
import org.esa.pfa.fe.op.out.PatchWriter;
import org.esa.pfa.fe.op.out.PatchWriterFactory;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;

import static org.junit.Assert.*;

/**
 * @author Norman Fomferra
 */
public class FexOperatorTest {

    static {
        GPF.getDefaultInstance().getOperatorSpiRegistry().loadOperatorSpis();
    }

    @Test
    public void testOp() throws Exception {

        Product sourceProduct = new Product("P", "PT", 256, 260);
        sourceProduct.addBand("B1", "1");
        sourceProduct.addBand("B2", "2");

        MyPatchWriterFactory outputFactory = new MyPatchWriterFactory();
        assertEquals(null, outputFactory.featureOutput);

        FexOperator fexOperator = new MyFexOperator();
        fexOperator.setTargetPath("test");
        fexOperator.setOverwriteMode(true);
        fexOperator.setSkipProductOutput(true);
        fexOperator.setPatchWidth(100);
        fexOperator.setPatchHeight(100);
        fexOperator.setSourceProduct(sourceProduct);
        fexOperator.setPatchWriterFactory(outputFactory);
        Product targetProduct = fexOperator.getTargetProduct();

        assertEquals("test", outputFactory.getTargetPath());
        assertEquals(true, outputFactory.isOverwriteMode());
        assertEquals(false, outputFactory.getSkipFeatureOutput());
        assertEquals(true, outputFactory.getSkipProductOutput());
        assertEquals(false, outputFactory.getSkipQuicklookOutput());

        assertSame(targetProduct, sourceProduct);
        assertNotNull(outputFactory.featureOutput);
        assertTrue(outputFactory.featureOutput.initialized);
        assertTrue(outputFactory.featureOutput.closed);
        assertEquals(9, outputFactory.featureOutput.patchOutputs.size());

        assertNotNull(outputFactory.featureOutput.patchOutputs.get(0).patch.getPatchProduct());
        assertEquals(100, outputFactory.featureOutput.patchOutputs.get(0).patch.getPatchProduct().getSceneRasterWidth());
        assertEquals(100, outputFactory.featureOutput.patchOutputs.get(0).patch.getPatchProduct().getSceneRasterHeight());

        assertNotNull(outputFactory.featureOutput.patchOutputs.get(2).patch.getPatchProduct());
        assertEquals(56, outputFactory.featureOutput.patchOutputs.get(2).patch.getPatchProduct().getSceneRasterWidth());
        assertEquals(100, outputFactory.featureOutput.patchOutputs.get(2).patch.getPatchProduct().getSceneRasterHeight());

        assertNotNull(outputFactory.featureOutput.patchOutputs.get(6).patch.getPatchProduct());
        assertEquals(100, outputFactory.featureOutput.patchOutputs.get(6).patch.getPatchProduct().getSceneRasterWidth());
        assertEquals(60, outputFactory.featureOutput.patchOutputs.get(6).patch.getPatchProduct().getSceneRasterHeight());

        assertNotNull(outputFactory.featureOutput.patchOutputs.get(8).patch.getPatchProduct());
        assertEquals(56, outputFactory.featureOutput.patchOutputs.get(8).patch.getPatchProduct().getSceneRasterWidth());
        assertEquals(60, outputFactory.featureOutput.patchOutputs.get(8).patch.getPatchProduct().getSceneRasterHeight());
    }


    private static class MyFexOperator extends FexOperator {
        public static final FeatureType[] FEATURE_TYPES = new FeatureType[]{
                new FeatureType("f1", "d1", String.class),
                new FeatureType("f2", "d2", Double.class)
        };

        @Override
        protected FeatureType[] getFeatureTypes() {
            return FEATURE_TYPES;
        }

        @Override
        protected boolean processPatch(Patch patch, PatchOutput sink) throws IOException {
            Feature[] bibos = {
                    new Feature(FEATURE_TYPES[0], "bibo"),
                    new Feature(FEATURE_TYPES[1], 3.14),
            };
            sink.writePatch(patch, bibos);
            return true;
        }
    }


    public static class MyPatchWriterFactory extends PatchWriterFactory {

        MyPatchWriter featureOutput;
        String targetPath;

        @Override
        public PatchWriter createFeatureOutput(Product sourceProduct) {
            featureOutput = new MyPatchWriter();
            return featureOutput;
        }
    }

    private static class MyPatchWriter implements PatchWriter {
        ArrayList<MyPatchOutput> patchOutputs = new ArrayList<MyPatchOutput>();
        boolean initialized;
        boolean closed;

        @Override
        public void initialize(PropertySet configuration, Product sourceProduct, FeatureType... featureTypes) throws IOException {
            initialized = true;
        }

        @Override
        public void writePatch(Patch patch, Feature... features) throws IOException {
            MyPatchOutput patchOutput = new MyPatchOutput(patch, features);
            patchOutputs.add(patchOutput);
        }

        @Override
        public void close() {
            closed = true;
        }
    }

    private static class MyPatchOutput {
        Patch patch;
        Feature[] features;

        private MyPatchOutput(Patch patch, Feature[] features) {
            this.patch = patch;
            this.features = features;
        }
    }
}
