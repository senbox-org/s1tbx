package org.esa.beam.framework.gpf.ui;

import com.bc.ceres.core.ProgressMonitor;
import junit.framework.TestCase;
import org.esa.beam.framework.dataio.AbstractProductWriter;
import org.esa.beam.framework.dataio.ProductIOPlugInManager;
import org.esa.beam.framework.dataio.ProductWriter;
import org.esa.beam.framework.dataio.ProductWriterPlugIn;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.util.io.BeamFileFilter;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

/**
 * Tests for class {@link TargetProductSelectorModel}.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public class TargetProductSelectorModelTest extends TestCase {

    private TargetProductSelectorModel model;
    private DummyTestProductWriterPlugIn writerPlugIn;


    @Override
    protected void setUp() throws Exception {
        writerPlugIn = new DummyTestProductWriterPlugIn();
        ProductIOPlugInManager.getInstance().addWriterPlugIn(writerPlugIn);
        model = new TargetProductSelectorModel();
    }

    public void testSetGetProductName() {
        model.setProductName("Obelix");
        assertEquals("Obelix", model.getProductName());
    }

    public void testSetGetInvalidProductName() {
        assertNull(model.getProductName());
        try {
            model.setProductName("Obel/x");
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().startsWith("The product name 'Obel/x' is not valid"));
        }
        assertNull(model.getProductName());
    }
    
    public void testSetGetFormatName() {
        model.setFormatName("Majestix");
        assertEquals("Majestix", model.getFormatName());
    }

    public void testGetFileName() {
        model.setProductName("Obelix");
        assertEquals("Obelix", model.getProductName());
        assertEquals("Obelix.dim", model.getProductFileName());

        // other format
        model.setFormatName(writerPlugIn.getFormatNames()[0]);
        assertEquals("Obelix.x", model.getProductFileName());

        model.setProductName("Idefix.dim");
        assertEquals("Idefix.dim.x", model.getProductFileName());

        model.setProductName("Idefix.x");
        assertEquals("Idefix.x", model.getProductFileName());
    }

    public void testSetGetDirectory() {
        final File directory = new File("Gallien");
        model.setProductDir(directory);
        assertEquals(directory, model.getProductDir());

        model.setProductName("Obelix");
        assertEquals(new File("Gallien", "Obelix.dim"), model.getProductFile());

        // other format
        model.setFormatName(writerPlugIn.getFormatNames()[0]);
        assertEquals(new File("Gallien", "Obelix.x"), model.getProductFile());
    }

    public void testSelections() {
        assertTrue(model.isSaveToFileSelected());
        assertTrue(model.isOpenInAppSelected());

        model.setOpenInAppSelected(false);
        assertFalse(model.isOpenInAppSelected());
        assertTrue(model.isSaveToFileSelected());

        model.setSaveToFileSelected(false);
        assertFalse(model.isSaveToFileSelected());
        assertTrue(model.isOpenInAppSelected());
        model.setOpenInAppSelected(false);
        assertFalse(model.isOpenInAppSelected());
        assertTrue(model.isSaveToFileSelected());
    }

    private class DummyTestProductWriter extends AbstractProductWriter {

        public DummyTestProductWriter(ProductWriterPlugIn writerPlugIn) {
            super(writerPlugIn);
        }

        @Override
        protected void writeProductNodesImpl() throws IOException {

        }

        public void writeBandRasterData(Band sourceBand, int sourceOffsetX, int sourceOffsetY, int sourceWidth,
                                        int sourceHeight, ProductData sourceBuffer, ProgressMonitor pm) throws
                IOException {

        }

        public void flush() throws IOException {

        }

        public void close() throws IOException {

        }

        public void deleteOutput() throws IOException {

        }
    }

    private class DummyTestProductWriterPlugIn implements ProductWriterPlugIn {

        public Class[] getOutputTypes() {
            return new Class[]{String.class};
        }

        public ProductWriter createWriterInstance() {
            return new DummyTestProductWriter(this);
        }

        public String[] getFormatNames() {
            return new String[]{"Dummy"};
        }

        public String[] getDefaultFileExtensions() {
            return new String[]{".x"};
        }

        public String getDescription(Locale locale) {
            return "";
        }

        public BeamFileFilter getProductFileFilter() {
            return new BeamFileFilter();
        }
    }
}
