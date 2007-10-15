package org.esa.beam.framework.ui.io;

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
        model = new TargetProductSelectorModel(true);
    }

    public void testSetGetProductName() {
        model.setProductName("Obelix");
        assertEquals("Obelix", model.getProductName());
    }

    public void testGetFileName() {
        model.setProductName("Obelix");
        assertEquals("Obelix", model.getProductName());
        assertEquals("Obelix.dim", model.getFileName());

        // other format
        model.setFormatName(writerPlugIn.getFormatNames()[0]);
        assertEquals("Obelix.x", model.getFileName());

        model.setProductName("Idefix.dim");
        assertEquals("Idefix.dim.x", model.getFileName());

        model.setProductName("Idefix.x");
        assertEquals("Idefix.x", model.getFileName());
    }

    public void testSetGetDirectory() {
        final File directory = new File("Gallien");
        model.setDirectory(directory);
        assertEquals(directory, model.getDirectory());

        model.setProductName("Obelix");
        assertEquals(new File("Gallien", "Obelix.dim"), model.getFile());

        // other format
        model.setFormatName(writerPlugIn.getFormatNames()[0]);
        assertEquals(new File("Gallien", "Obelix.x"), model.getFile());
    }

    public void testSelections() {
        assertTrue(model.isSaveToFileSelected());
        assertFalse(model.isOpenInVisatSelected());

        model.setOpenInVisatSelected(false);
        assertFalse(model.isOpenInVisatSelected());
        assertTrue(model.isSaveToFileSelected());

        model.setSaveToFileSelected(false);
        assertFalse(model.isSaveToFileSelected());
        assertTrue(model.isOpenInVisatSelected());
        model.setOpenInVisatSelected(false);
        assertFalse(model.isOpenInVisatSelected());
        assertTrue(model.isSaveToFileSelected());
    }

    private class DummyTestProductWriter extends AbstractProductWriter {

        public DummyTestProductWriter(ProductWriterPlugIn writerPlugIn) {
            super(writerPlugIn);
        }

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
