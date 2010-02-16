package org.esa.beam.dataio.geotiff.internal;

/**
 * Created by IntelliJ IDEA.
 * User: marco
 * Date: 08.02.2005
 * Time: 12:52:22
 */

import junit.framework.TestCase;
import org.esa.beam.framework.datamodel.ProductData;

import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class TiffValueTest extends TestCase {

    private TiffValue _tiffValue;
    private ImageOutputStream _stream;

    @Override
    public void setUp() throws Exception {
        _tiffValue = createTiffValueInstance();
        _stream = new MemoryCacheImageOutputStream(new ByteArrayOutputStream());
    }

    @Override
    public void tearDown() throws Exception {
    }

    public void testWrite() throws IOException {
        final int[] expected = new int[]{3, 6, 128973, 8};
        _tiffValue.setData(ProductData.createInstance(expected));

        _tiffValue.write(_stream);

        assertEquals(16, _stream.length());
        _stream.seek(0);
        for (int i = 0; i < expected.length; i++) {
            assertEquals("failure at index " + i, expected[i], _stream.readInt());
        }
    }

    public void testWrite_ProductDataThrowsIOException() {
        _tiffValue.setData(new ProductDataDummy());

        try {
            _tiffValue.write(_stream);
            fail("IOException expected");
        } catch (IOException expected) {

        } catch (Exception notExpected) {
            fail("IOException expected");
        }
    }

    public void testWrite_ThrowsIllegalStateExceptionBecauseDataWasNotSetToValue() {
        _tiffValue.setData(null);

        try {
            _tiffValue.write(_stream);
            fail("IllegalStateException expected");
        } catch (IOException notExpected) {
            fail("IllegalStateException expected");
        } catch (IllegalStateException expected) {

        } catch (Exception notExpected) {
            fail("IllegalStateException expected");
        }
    }

    private TiffValue createTiffValueInstance() {
        return new TiffValue() {

        };
    }

    private class ProductDataDummy extends ProductData.Byte {

        public ProductDataDummy() {
            super(5);
        }

        @Override
        public void writeTo(final ImageOutputStream output) throws IOException {
            throw new IOException("dummy exception");
        }
    }
}