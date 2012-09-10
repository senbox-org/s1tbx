package org.esa.beam.statistics;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.text.ParseException;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.junit.*;

import static org.junit.Assert.assertEquals;

public class MetadataWriterTest {

    private ByteArrayOutputStream outputStream;
    private MetadataWriter metadataWriter;

    @Before
    public void setUp() throws Exception {
        outputStream = new ByteArrayOutputStream();
        metadataWriter = new MetadataWriter(new PrintStream(outputStream));
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testSomething() throws ParseException {
        final Product[] sourceProducts = {new Product("MER_RR__2PBCMsomething", "type", 10, 10)};
        final ProductData.UTC startDate = ProductData.UTC.parse("2010-01-01", "yyyy-MM-dd");
        final ProductData.UTC endDate = ProductData.UTC.parse("2011-01-01", "yyyy-MM-dd");
        final String[] regionIds = {"bullerbue", "bielefeld"};

        metadataWriter.writeMetadata(sourceProducts, startDate, endDate, regionIds);

        assertEquals("# BEAM Statistics export\n" +
                     "#\n" +
                     "# Products:\n" +
                     "#              MER_RR__2PBCMsomething\n" +
                     "#\n" +
                     "# Start Date: 01-JAN-2010 00:00:00.000000\n" +
                     "#\n" +
                     "# End Date: 01-JAN-2011 00:00:00.000000\n" +
                     "#\n" +
                     "# Regions:\n" +
                     "#              bullerbue\n" +
                     "#              bielefeld\n"
                , outputStream.toString());

    }
}
