package org.esa.snap.csv.dataio.writer;

import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Product;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

/**
 * @author Thomas Storm
 */
public class PixelCsvWriterTest {
    
    @Test
    @Ignore
    public void testWriteCsv() throws Exception {
        final StringBuilder actual = new StringBuilder();
        final PixelCsvWriter csvWriter = new PixelCsvWriter(new WriteStrategy() {
            @Override
            public void writeCsv(String fullOutput) throws IOException {
                actual.append(fullOutput);
            }
        }, new CsvWriterBuilder.BeamOutputFormat());

        final Product testProduct = new Product("testProduct", "someType", 10, 10);
        final PixelPos[] pixelPositions = new PixelPos[2];
        pixelPositions[0] = new PixelPos();
        pixelPositions[1] = new PixelPos();
        csvWriter.writeCsv(testProduct, pixelPositions);
        
        final StringBuilder expected = new StringBuilder();
        expected.append("featureId\tattr_1:string\tattr_2:float");
        expected.append("\n");
        expected.append("0\tval_1\t10.0");
        expected.append("\n");
        expected.append("1\tval_2\t20.0");
        expected.append("\n");
        assertEquals(expected.toString(), actual.toString());
    }

    @Test
    public void testIsValidInput() throws Exception {
        final PixelCsvWriter csvWriter = new PixelCsvWriter(null, null);
        final Product testProduct = new Product("testProduct", "someType", 10, 10);
        final PixelPos[] pixelPositions = new PixelPos[10];

        assertFalse(csvWriter.isValidInput());
        assertFalse(csvWriter.isValidInput("", "", ""));
        assertFalse(csvWriter.isValidInput(testProduct, new PixelPos(), new PixelPos()));
        assertTrue(csvWriter.isValidInput(testProduct, pixelPositions));
    }
}
