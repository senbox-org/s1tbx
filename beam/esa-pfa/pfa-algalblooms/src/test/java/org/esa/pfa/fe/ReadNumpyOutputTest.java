package org.esa.pfa.fe;

import org.junit.Test;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;

import static org.junit.Assert.assertEquals;

/**
 * On Windows:
 *
 * <pre>
 * >>> import numpy
 * >>> a = numpy.array(range(10), dtype=numpy.float32)
 * >>> a = a.byteswap()
 * >>> f = open('test_f32.bin', 'wb')
 * >>> a.tofile(f)
 * >>> f.close()
 * </pre>
 *
 * @author Norman Fomferra
 */
public class ReadNumpyOutputTest {

    @Test
    public void testNumpyOutput() throws Exception {

        URL resource = getClass().getResource("/test_f32.bin");
        File file = new File(resource.toURI());
        long length = file.length();
        assertEquals(40, length);

        InputStream is = new FileInputStream(file);
        DataInputStream dis = new DataInputStream(is);

        for (int i = 0; i < 10; i++) {
            assertEquals("i = " + i, (float) i, dis.readFloat(), 1e-5F);
        }
    }
}
