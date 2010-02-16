package org.esa.beam.processor.cloud.internal;

import com.bc.ceres.core.ProgressMonitor;
import junit.framework.TestCase;
import org.esa.beam.framework.dataio.IllegalFileFormatException;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.dataio.ProductSubsetDef;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;

import java.io.IOException;

public class ProcessingNodeTest extends TestCase {

    private SourceProductReader sourceProductReader;
    private TargetProcessingNode targetProcessingNode;
    private Product sourceProduct;
    private Product targetProduct;

    @Override
    protected void setUp() throws Exception {
        sourceProductReader = new SourceProductReader();
        sourceProduct = sourceProductReader.readProductNodes(null, null);
        targetProcessingNode = new TargetProcessingNode();
        targetProduct = targetProcessingNode.readProductNodes(sourceProduct, null);
    }

    public void testSetUp() throws IOException {
        assertNotNull(sourceProduct.getBand("A"));
        assertNotNull(sourceProduct.getBand("B"));

        assertNotNull(targetProduct.getBand("U"));
        assertNotNull(targetProduct.getBand("V"));
        assertNotNull(targetProduct.getBand("W"));

        assertSame(sourceProductReader, sourceProduct.getProductReader());
        assertSame(targetProcessingNode, targetProduct.getProductReader());
        assertNull(targetProcessingNode.getFrameData("U"));
        assertNull(targetProcessingNode.getFrameData("V"));
        assertNull(targetProcessingNode.getFrameData("W"));
    }

    public void testThatDataIsProcessedCorrectly() throws IOException {
        final int[] pixelData = new int[4];
        //
        // A = x
        // B = y
        //
        // U = A
        // V = B
        // W = A + B
        //
        targetProduct.getBand("U").readPixels(0, 0, 4, 1, pixelData, ProgressMonitor.NULL);
        assertEquals(0, pixelData[0]);
        assertEquals(1, pixelData[1]);
        assertEquals(2, pixelData[2]);
        assertEquals(3, pixelData[3]);
        targetProduct.getBand("V").readPixels(0, 0, 4, 1, pixelData, ProgressMonitor.NULL);
        assertEquals(0, pixelData[0]);
        assertEquals(0, pixelData[1]);
        assertEquals(0, pixelData[2]);
        assertEquals(0, pixelData[3]);
        targetProduct.getBand("W").readPixels(0, 0, 4, 1, pixelData, ProgressMonitor.NULL);
        assertEquals(0, pixelData[0]);
        assertEquals(1, pixelData[1]);
        assertEquals(2, pixelData[2]);
        assertEquals(3, pixelData[3]);

        targetProduct.getBand("U").readPixels(0, 2, 4, 1, pixelData, ProgressMonitor.NULL);
        assertEquals(0, pixelData[0]);
        assertEquals(1, pixelData[1]);
        assertEquals(2, pixelData[2]);
        assertEquals(3, pixelData[3]);
        targetProduct.getBand("V").readPixels(0, 2, 4, 1, pixelData, ProgressMonitor.NULL);
        assertEquals(2, pixelData[0]);
        assertEquals(2, pixelData[1]);
        assertEquals(2, pixelData[2]);
        assertEquals(2, pixelData[3]);
        targetProduct.getBand("W").readPixels(0, 2, 4, 1, pixelData, ProgressMonitor.NULL);
        assertEquals(2, pixelData[0]);
        assertEquals(3, pixelData[1]);
        assertEquals(4, pixelData[2]);
        assertEquals(5, pixelData[3]);

        targetProduct.getBand("U").readPixels(3, 0, 1, 4, pixelData, ProgressMonitor.NULL);
        assertEquals(3, pixelData[0]);
        assertEquals(3, pixelData[1]);
        assertEquals(3, pixelData[2]);
        assertEquals(3, pixelData[3]);
        targetProduct.getBand("V").readPixels(3, 0, 1, 4, pixelData, ProgressMonitor.NULL);
        assertEquals(0, pixelData[0]);
        assertEquals(1, pixelData[1]);
        assertEquals(2, pixelData[2]);
        assertEquals(3, pixelData[3]);
        targetProduct.getBand("W").readPixels(3, 0, 1, 4, pixelData, ProgressMonitor.NULL);
        assertEquals(3, pixelData[0]);
        assertEquals(4, pixelData[1]);
        assertEquals(5, pixelData[2]);
        assertEquals(6, pixelData[3]);
    }

    private static class TargetProcessingNode extends ProcessingNode {

        StringBuffer callBuffer = new StringBuffer();

        public TargetProcessingNode() {
            super(null);
        }

        @Override
        protected Product createTargetProductImpl() {
            final Product product = new Product("T", "T", getSourceProduct().getSceneRasterWidth(),
                                                getSourceProduct().getSceneRasterHeight(), this);
            product.addBand("U", ProductData.TYPE_FLOAT32);
            product.getBand("U").setDescription("U=A");
            product.addBand("V", ProductData.TYPE_FLOAT32);
            product.getBand("V").setDescription("V=B");
            product.addBand("W", ProductData.TYPE_FLOAT32);
            product.getBand("W").setDescription("W=A+B");
            return product;
        }

        String getCallString() {
            return callBuffer.toString();
        }

        @Override
        protected void processFrame(int frameX, int frameY, int frameW, int frameH, ProgressMonitor pm) throws
                                                                                                        IOException {
            callBuffer.append("processFrame(");
            callBuffer.append(frameX);
            callBuffer.append(",");
            callBuffer.append(frameY);
            callBuffer.append(",");
            callBuffer.append(frameW);
            callBuffer.append(",");
            callBuffer.append(frameH);
            callBuffer.append(");");

            final ProductData uData = getFrameData("U");
            final ProductData vData = getFrameData("V");
            final ProductData wData = getFrameData("W");

            final Band aBand = getSourceProduct().getBand("A");
            final Band bBand = getSourceProduct().getBand("B");
            final ProductData aData = aBand.createCompatibleProductData(frameW * frameH);
            final ProductData bData = bBand.createCompatibleProductData(frameW * frameH);

            aBand.readRasterData(frameX, frameY, frameW, frameH, aData, pm);
            bBand.readRasterData(frameX, frameY, frameW, frameH, bData, pm);

            for (int y = 0; y < frameH; y++) {
                for (int x = 0; x < frameW; x++) {
                    final int index = y * frameW + x;
                    uData.setElemDoubleAt(index, aData.getElemDoubleAt(index));
                    vData.setElemDoubleAt(index, bData.getElemDoubleAt(index));
                    wData.setElemDoubleAt(index, aData.getElemDoubleAt(index) + bData.getElemDoubleAt(index));
                }
            }
        }
    }

    private static class SourceProductReader implements ProductReader {

        public ProductReaderPlugIn getReaderPlugIn() {
            return null;
        }

        public Object getInput() {
            return null;
        }

        public ProductSubsetDef getSubsetDef() {
            return null;
        }

        public Product readProductNodes(Object input, ProductSubsetDef subsetDef) throws IOException,
                                                                                         IllegalFileFormatException {
            final Product product = new Product("S", "S", 4, 4, this);
            product.addBand("A", ProductData.TYPE_FLOAT32);
            product.addBand("B", ProductData.TYPE_FLOAT32);
            return product;
        }

        public void readBandRasterData(Band destBand, int destOffsetX, int destOffsetY, int destWidth, int destHeight,
                                       ProductData destBuffer, ProgressMonitor pm) throws IOException {
            pm.beginTask("Reading raster data", destHeight);
            try {
                for (int y = 0; y < destHeight; y++) {
                    for (int x = 0; x < destWidth; x++) {
                        final int sourceX = destOffsetX + x;
                        final int sourceY = destOffsetY + y;
                        final int index = y * destWidth + x;
                        if (destBand.getName().equals("A")) {
                            destBuffer.setElemDoubleAt(index, sourceX);
                        } else if (destBand.getName().equals("B")) {
                            destBuffer.setElemDoubleAt(index, sourceY);
                        }
                    }
                    pm.worked(1);
                }
            } finally {
                pm.done();
            }
        }

        public void close() throws IOException {
        }
    }
}
