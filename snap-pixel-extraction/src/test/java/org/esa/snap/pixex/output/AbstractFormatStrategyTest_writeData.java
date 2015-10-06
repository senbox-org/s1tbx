package org.esa.snap.pixex.output;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.measurement.Measurement;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.awt.Rectangle;
import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.Assert.*;


public class AbstractFormatStrategyTest_writeData {

    private AbstractFormatStrategy abstractFormatStrategy;
    private PrintWriter printWriter;
    private StringWriter stringWriter;

    @Before
    public void setUp() throws Exception {
        abstractFormatStrategy = new AbstractFormatStrategy(null, null, 1, true) {
            @Override
            public void writeHeader(PrintWriter writer, Product product) {
            }

            @Override
            public void writeMeasurements(Product product, PrintWriter writer, Measurement[] measurements) {
            }
        };
        stringWriter = new StringWriter();
        printWriter = new PrintWriter(stringWriter);
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testWriteValue_Double_NAN() {
        abstractFormatStrategy.writeValue(printWriter, Double.NaN);

        assertEquals("", stringWriter.toString());
    }

    @Test
    public void testWriteValue_Double_24Dot65() {
        abstractFormatStrategy.writeValue(printWriter, 24.65);

        assertEquals("24.65", stringWriter.toString());
    }

    @Test
    public void testWriteValue_Float_16Dot23() {
        abstractFormatStrategy.writeValue(printWriter, 16.23f);

        assertEquals("16.23", stringWriter.toString());
    }

    @Test
    public void testWriteValue_AnyObject_egRectangle() {
        abstractFormatStrategy.writeValue(printWriter, new Rectangle(3, 4, 5, 6));

        assertEquals("java.awt.Rectangle[x=3,y=4,width=5,height=6]", stringWriter.toString());
    }

    @Test
    public void testWriteValue_null() {
        abstractFormatStrategy.writeValue(printWriter, null);

        assertEquals("", stringWriter.toString());
    }
}
