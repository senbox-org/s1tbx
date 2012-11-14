package org.esa.beam.dataio.pgx;

import org.junit.Test;

import java.nio.ByteOrder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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
}
