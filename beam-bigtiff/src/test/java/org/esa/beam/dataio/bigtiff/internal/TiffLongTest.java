package org.esa.beam.dataio.bigtiff.internal;


import org.junit.Test;

import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class TiffLongTest {

    @Test
    public void testConstructAndGetData() {
        final TiffLong tiffLong = new TiffLong(12345678910L);

        assertEquals(12345678910L, tiffLong.getValue());
    }

    @Test
    public void testGetSizeInBytes() {
        final TiffLong tiffLong = new TiffLong(13L);

        assertEquals(8, tiffLong.getSizeInBytes());
    }

    @Test
    public void testWrite() throws IOException {
        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        final MemoryCacheImageOutputStream stream = new MemoryCacheImageOutputStream(byteArrayOutputStream);

        final TiffLong tiffLong = new TiffLong(23345L);
        tiffLong.write(stream);

        stream.close();

        assertEquals(8, byteArrayOutputStream.toString().length());
    }
}
