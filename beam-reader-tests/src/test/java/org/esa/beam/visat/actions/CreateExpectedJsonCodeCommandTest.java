package org.esa.beam.visat.actions;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.media.jai.operator.ConstantDescriptor;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import static org.junit.Assert.*;

/**
 * @author Marco Peters
 */
public class CreateExpectedJsonCodeCommandTest {

    private static final String JSON_CODE_RESOURCE = "EXPECTED_JSON_CODE.json";
    private static final String LF = System.getProperty("line.separator");
    private static String EXPECTED_JSON_CODE;
    private static Product product;


    @BeforeClass
    public static void setUp() throws Exception {
        final InputStream resourceAsStream = CreateExpectedJsonCodeCommandTest.class.getResourceAsStream(JSON_CODE_RESOURCE);

        final InputStreamReader reader = new InputStreamReader(resourceAsStream);
        BufferedReader r = new BufferedReader(reader);
        String line = r.readLine();
        final StringBuilder sb = new StringBuilder();
        while (line != null) {
            sb.append(line);
            line = r.readLine();
            if (line != null) {
                sb.append(LF);
            }
        }
        EXPECTED_JSON_CODE = sb.toString();

        product = new Product("Hans Wurst", "T", 12, 13);
        product.setStartTime(ProductData.UTC.parse("23-AUG-1983 12:10:10"));
        product.setEndTime(ProductData.UTC.parse("23-AUG-1983 12:14:41"));
        final Band band1 = product.addBand("band_1", ProductData.TYPE_INT32);
        band1.setSourceImage(ConstantDescriptor.create(12.0f, 13.0f, new Integer[]{1}, null));
        band1.setDescription("description_1");
        band1.setUnit("abc");
        band1.setGeophysicalNoDataValue(1);
        band1.setNoDataValueUsed(true);
        final Band band2 = product.addBand("band_2", ProductData.TYPE_FLOAT32);
        band2.setDescription("description_2");
        band2.setUnit("m/w^2");
        band2.setSourceImage(ConstantDescriptor.create(12.0f, 13.0f, new Float[]{2.0f}, null));

    }

    @Test
    public void testCreatedJson() throws Exception {
        final CreateExpectedJsonCodeCommand jsonCodeCommand = new CreateExpectedJsonCodeCommand();

        final String actualJsonCode = jsonCodeCommand.createJsonCode(product);
        assertEquals(EXPECTED_JSON_CODE, actualJsonCode);
    }

    @Test
    public void testFillClipboardWithJsonCode() throws Exception {
        final Clipboard clipboard = new Clipboard("testClipboard");
        final CreateExpectedJsonCodeCommand jsonCodeCommand = new CreateExpectedJsonCodeCommand(clipboard);
        jsonCodeCommand.fillClipboardWithJsonCode(product);

        assertTrue(clipboard.isDataFlavorAvailable(DataFlavor.stringFlavor));

        String actualJsonCode = (String) clipboard.getData(DataFlavor.stringFlavor);
        assertNotNull(actualJsonCode);
        assertEquals(EXPECTED_JSON_CODE, actualJsonCode);
    }
}
