package org.esa.snap.dataio.pgx;

import org.junit.Test;

import javax.imageio.stream.MemoryCacheImageInputStream;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteOrder;

import static org.junit.Assert.*;

/**
 * @author Norman Fomferra
 */
public class PgxProductReaderTest {

    @Test
    public void testHeaderParsing() {
        final PgxProductReader.Header header = PgxProductReader.parseHeaderLine("PG ML + 16 10960 4096");
        assertNotNull(header);
        assertEquals(ByteOrder.BIG_ENDIAN, header.byteOrder);
        assertEquals(false, header.signed);
        assertEquals(16, header.bitDepth);
        assertEquals(10960, header.width);
        assertEquals(4096, header.height);
    }

    @Test
    public void testHeaderParsingFromStream() throws IOException {
        String content = "PG ML + 16 10960 4096\nfurther, unneeded content";
        BufferedInputStream stream = new BufferedInputStream(new ByteArrayInputStream(content.getBytes()));
        final PgxProductReader.Header header = PgxProductReader.readHeader(new MemoryCacheImageInputStream(stream));
        assertNotNull(header);
        assertEquals(ByteOrder.BIG_ENDIAN, header.byteOrder);
        assertEquals(false, header.signed);
        assertEquals(16, header.bitDepth);
        assertEquals(10960, header.width);
        assertEquals(4096, header.height);
    }

    @Test
    public void testHeaderParsing_WrongHeader() {
        assertNull(PgxProductReader.parseHeaderLine("gernuigh589z589nerjkghrepgöniofh89348"));
    }
}
