package org.esa.snap.statistics.output;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.text.ParseException;

import static org.junit.Assert.*;

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
    public void testWriteMetadata() throws ParseException {
        final Product[] sourceProducts = {new Product("MER_RR__2PBCMsomething", "type", 10, 10)};
        final ProductData.UTC startDate = ProductData.UTC.parse("2010-01-01", "yyyy-MM-dd");
        final ProductData.UTC endDate = ProductData.UTC.parse("2011-01-01", "yyyy-MM-dd");
        final String[] regionIds = {"bullerbue", "bielefeld"};

        metadataWriter.initialiseOutput(StatisticsOutputContext.create(sourceProducts, null, null, startDate, endDate, regionIds));

        assertEquals("# SNAP Statistics export\n" +
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
