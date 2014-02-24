package org.esa.beam.dataio.envi;

import junit.framework.TestCase;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.datamodel.Product;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class EnviProductReaderAcceptanceTest extends TestCase {

    public void not_____testReadCorrectHeaderImgPair() throws IOException {
        final StringBuffer headerContent = new StringBuffer();
        headerContent.append("ENVI\n");
        headerContent.append("sensor type = Unknown\n");

        writeHeaderFile(headerContent);

        final byte[] imageContent = new byte[]{
                1, 2, 3, 4,
                5, 6, 7, 8,
                1, 2, 3, 4
        };
        writeImageFile(imageContent);

        final EnviProductReaderPlugIn plugIn = new EnviProductReaderPlugIn();
        final ProductReader productReader = plugIn.createReaderInstance();
        final Product product = productReader.readProductNodes(headerFile, null);
        // @todo 2 tb/** what exactly does this test check for???
    }

    public void testReadHeader_ImgMissing() throws IOException {
        StringBuffer headerContent = new StringBuffer();

        writeHeaderFile(headerContent);

        final EnviProductReaderPlugIn plugIn = new EnviProductReaderPlugIn();
        final ProductReader productReader = plugIn.createReaderInstance();
        try {
            final Product product = productReader.readProductNodes(headerFile, null);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException expected) {
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    /////// END OF PUBLIC
    ///////////////////////////////////////////////////////////////////////////

    private File headerFile;
    private File imageFile;

    protected void setUp() throws Exception {
        headerFile = null;
        imageFile = null;
    }

    protected void tearDown() throws Exception {
        if (headerFile != null) {
            if (!headerFile.delete()) {
                fail("unable to delete: " + headerFile.getCanonicalPath());
            }
        }
        if (imageFile != null) {
            if (!imageFile.delete()) {
                fail("unable to delete: " + imageFile.getCanonicalPath());
            }
        }
    }

    private void writeHeaderFile(StringBuffer headerContent) throws IOException {
        headerFile = new File("test_1.hdr");
        headerFile.createNewFile();

        final FileOutputStream headerOut = new FileOutputStream(headerFile);
        headerOut.write(headerContent.toString().getBytes());
        headerOut.flush();
        headerOut.close();
    }

    private void writeImageFile(byte[] content) throws IOException {
        imageFile = new File("test_1.img");
        imageFile.createNewFile();

        final FileOutputStream headerOut = new FileOutputStream(imageFile);
        headerOut.write(content);
        headerOut.flush();
        headerOut.close();
    }
}
