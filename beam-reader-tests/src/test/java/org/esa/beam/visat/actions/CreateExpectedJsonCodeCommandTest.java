package org.esa.beam.visat.actions;

import org.esa.beam.HeadlessTestRunner;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.stubbing.OngoingStubbing;

import javax.media.jai.operator.ConstantDescriptor;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Random;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * @author Marco Peters
 */
@RunWith(HeadlessTestRunner.class)
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

        product = new Product("Hans Wurst", "T", 10, 20);
        product.setStartTime(ProductData.UTC.parse("23-AUG-1983 12:10:10"));
        product.setEndTime(ProductData.UTC.parse("23-AUG-1983 12:14:41"));
        final Band band1 = product.addBand("band_1", ProductData.TYPE_INT32);
        band1.setSourceImage(ConstantDescriptor.create(10.0f, 20.0f, new Integer[]{1}, null));
        band1.setDescription("description_1");
        band1.setUnit("abc");
        band1.setGeophysicalNoDataValue(1);
        band1.setNoDataValueUsed(true);
        final Band band2 = product.addBand("band_2", ProductData.TYPE_FLOAT32);
        band2.setDescription("description_2");
        band2.setUnit("m/w^2");
        band2.setSourceImage(ConstantDescriptor.create(10.0f, 20.0f, new Float[]{2.0f}, null));
        final MetadataElement metadataRoot = product.getMetadataRoot();
        final MetadataElement test1Element = new MetadataElement("test_1");
        final MetadataElement abc_1 = new MetadataElement("ABC");
        abc_1.addAttribute(new MetadataAttribute("Name" , ProductData.createInstance("ABC_1"), true));
        test1Element.addElement(abc_1);
        final MetadataElement abc_2 = new MetadataElement("ABC");
        abc_2.addAttribute(new MetadataAttribute("Name", ProductData.createInstance("ABC_2"), true));
        test1Element.addElement(abc_2);
        final MetadataElement abc_3 = new MetadataElement("ABC");
        abc_3.addAttribute(new MetadataAttribute("Name", ProductData.createInstance("ABC_3"), true));
        test1Element.addElement(abc_3);
        final MetadataElement abc_4 = new MetadataElement("ABC");
        abc_4.addAttribute(new MetadataAttribute("Name", ProductData.createInstance("ABC_4"), true));
        test1Element.addElement(abc_4);
        metadataRoot.addElement(test1Element);
        MetadataElement test2Element = new MetadataElement("test_2");
        test2Element.addAttribute(new MetadataAttribute("attrib", ProductData.createInstance("abc"), true));
        test2Element.addAttribute(new MetadataAttribute("attrib", ProductData.createInstance("def"), true));
        test2Element.addAttribute(new MetadataAttribute("attrib", ProductData.createInstance("ghi"), true));
        metadataRoot.addElement(test2Element);

    }

    @Test
    public void testCreatedJson() throws Exception {
        final CreateExpectedJsonCodeCommand jsonCodeCommand = new CreateExpectedJsonCodeCommand();
        final Random mock = createMockedRandom();
        final String actualJsonCode = jsonCodeCommand.createJsonCode(product, mock);
        assertEquals(EXPECTED_JSON_CODE, actualJsonCode);
    }

    @Test
    public void testFillClipboardWithJsonCode() throws Exception {
        final Clipboard clipboard = new Clipboard("testClipboard");
        final CreateExpectedJsonCodeCommand jsonCodeCommand = new CreateExpectedJsonCodeCommand(clipboard);
        jsonCodeCommand.fillClipboardWithJsonCode(product, createMockedRandom());

        assertTrue(clipboard.isDataFlavorAvailable(DataFlavor.stringFlavor));

        String actualJsonCode = (String) clipboard.getData(DataFlavor.stringFlavor);
        assertNotNull(actualJsonCode);
        assertEquals(EXPECTED_JSON_CODE, actualJsonCode);
    }

    private Random createMockedRandom() {
        final Random mock = Mockito.mock(Random.class);
        final OngoingStubbing<Float> ongoingStubbing = when(mock.nextFloat()).thenReturn(0.5f).thenReturn(0.3f).thenReturn(0.8f).thenReturn(0.4f);
        ongoingStubbing.thenReturn(0.0f).thenReturn(0.1f).thenReturn(0.8f).thenReturn(0.55f).thenReturn(0.2f).thenReturn(0.5f);
        return mock;
    }

}
