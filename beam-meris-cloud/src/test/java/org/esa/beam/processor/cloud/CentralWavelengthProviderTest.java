package org.esa.beam.processor.cloud;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import junit.framework.TestCase;

/**
 * Created by IntelliJ IDEA.
 * User: marcoz
 * Date: 07.09.2005
 * Time: 11:13:08
 * To change this template use File | Settings | File Templates.
 */
public class CentralWavelengthProviderTest extends TestCase {

    private static float DIFF = 0.000001f;
    private CentralWavelengthProvider provider;

    @Override
	public void setUp() {
        provider = new CentralWavelengthProvider();
    }

    public void testInit() {
        final float[] cwRR = provider.getCentralWavelength("MER_RR");
        assertEquals("925 members", 925, cwRR.length);
        assertEquals("value 0", 0f, cwRR[0], DIFF);
        assertEquals("value 1", 0f, cwRR[1], DIFF);
        assertEquals("value 924", 0f, cwRR[924], DIFF);
    }

    public void testIllegalProductType() {
        try {
            provider.getCentralWavelength("MER_TT");
            fail("Exception expected.");
        } catch (IllegalArgumentException e) {
        }
    }

    public void testReadCW() throws IOException {
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        for (int i = 0; i < 925; i++) {
            final String line = String.valueOf(i) + "\n";
            outputStream.write(line.getBytes());
        }
        final byte[] outArray = outputStream.toByteArray();
        final InputStream inputStream = new ByteArrayInputStream(outArray);
        provider.readCW(inputStream);

        final float[] cwRR = provider.getCentralWavelength("MER_RR");
        assertEquals("925 members", 925, cwRR.length);
        assertEquals("value 0", 0f, cwRR[0], DIFF);
        assertEquals("value 1", 1f, cwRR[1], DIFF);
        assertEquals("value 2", 2f, cwRR[2], DIFF);
        assertEquals("value 924", 924f, cwRR[924], DIFF);
    }

    public void testFr() throws IOException {
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        float value = 1.5f;
        for (int i = 0; i < 925; i++) {
            final String line = String.valueOf(value) + "\n";
            outputStream.write(line.getBytes());
            value += 4;
        }
        final byte[] outArray = outputStream.toByteArray();
        final InputStream inputStream = new ByteArrayInputStream(outArray);
        provider.readCW(inputStream);
        final float[] cwFR = provider.getCentralWavelength("MER_FR");

        assertEquals("3700 members", 3700, cwFR.length);
        for (int i = 0; i < 3700; i++) {
            assertEquals("value " + i, i, cwFR[i], DIFF);
        }
    }

    public void testFrDiscontinuiti() throws IOException {
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        for (int camera = 0; camera < 5; camera++) {
            float value = 1.5f;
            for (int i = 0; i < 185; i++) {
                final String line = String.valueOf(value) + "\n";
                outputStream.write(line.getBytes());
                value += 4;
            }
        }
        final byte[] outArray = outputStream.toByteArray();
        final InputStream inputStream = new ByteArrayInputStream(outArray);
        provider.readCW(inputStream);
        final float[] cwFR = provider.getCentralWavelength("MER_FR");

        assertEquals("3700 members", 3700, cwFR.length);
        int index=0;
        for (int camera = 0; camera < 5; camera++) {
            for (int value = 0; value < 740; value++) {
                assertEquals("value " + index, value, cwFR[index], DIFF);
                index++;
            }
        }
    }
}
