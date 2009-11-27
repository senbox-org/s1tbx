package org.esa.beam.dataio.dimap.spi;

import static org.esa.beam.dataio.dimap.DimapProductConstants.*;
import org.esa.beam.framework.datamodel.Mask;
import org.esa.beam.framework.datamodel.Product;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;

public class BandMathMaskPersistableTest {

    private Mask mask;

    @Before
    public void setup() {
        mask = new Mask("Bibo", 10, 10, new Mask.BandMathType());
        mask.setImageColor(new Color(17, 11, 67));
        mask.setImageTransparency(0.7);
        mask.setDescription("A big yellow bird is in the pixel.");
        mask.getImageConfig().setValue(Mask.BandMathType.PROPERTY_NAME_EXPRESSION, "false");

        final Product product = new Product("P", "T", 10, 10);
        product.getMaskGroup().add(mask);
    }

    @Test
    public void testXmlCreation() {
        final DimapPersistable persistable = new BandMathMaskPersistable();

        final Element element = persistable.createXmlFromObject(mask);

        assertNotNull(element);
        assertEquals(TAG_MASK, element.getName());

        final Attribute type = element.getAttribute(ATTRIB_TYPE);
        assertNotNull(type);
        assertEquals("Math", type.getValue());

        final Element name = element.getChild(TAG_NAME);
        assertNotNull(name);
        assertEquals("Bibo", name.getAttribute("value").getValue());

        final Element description = element.getChild(TAG_DESCRIPTION);
        assertNotNull(description);
        assertEquals("A big yellow bird is in the pixel.", description.getAttribute(ATTRIB_VALUE).getValue());

        final Element color = element.getChild(TAG_COLOR);
        assertNotNull(color);
        final Attribute red = color.getAttribute(ATTRIB_RED);
        assertNotNull(red);
        assertEquals(17, Integer.parseInt(red.getValue()));
        final Attribute green = color.getAttribute(ATTRIB_GREEN);
        assertNotNull(green);
        assertEquals(11, Integer.parseInt(green.getValue()));
        final Attribute blue = color.getAttribute(ATTRIB_BLUE);
        assertNotNull(blue);
        assertEquals(67, Integer.parseInt(blue.getValue()));
        final Attribute alpha = color.getAttribute(ATTRIB_ALPHA);
        assertNotNull(alpha);
        assertEquals(255, Integer.parseInt(alpha.getValue()));

        final Element transparency = element.getChild(TAG_TRANSPARENCY);
        assertNotNull(transparency);
        assertEquals(0.7, Double.parseDouble(transparency.getAttribute(ATTRIB_VALUE).getValue()), 0.0);


        final Element expression = element.getChild(TAG_EXPRESSION);
        assertNotNull(expression);
        assertEquals("false", expression.getAttribute(ATTRIB_VALUE).getValue());
    }


    @Test
    public void testXmlCreationWithoutMaskDescription() {
        mask.setDescription(null);
        final DimapPersistable persistable = new BandMathMaskPersistable();
        final Element element = persistable.createXmlFromObject(mask);

        final Element description = element.getChild(TAG_DESCRIPTION);
        assertNotNull(description);
        assertTrue(description.getAttribute(ATTRIB_VALUE).getValue().isEmpty());
    }

    @Test
    public void testMaskCreation() throws IOException, JDOMException {
        final DimapPersistable persistable = new BandMathMaskPersistable();
        final InputStream resourceStream = getClass().getResourceAsStream("BandMathMask.xml");
        final Document document = new SAXBuilder().build(resourceStream);
        final Mask maskFromXml = (Mask) persistable.createObjectFromXml(document.getRootElement(), mask.getProduct());

        assertNotNull(maskFromXml);
        assertEquals(Mask.BandMathType.class, maskFromXml.getImageType().getClass());
        assertEquals("Bibo", maskFromXml.getName());
        assertEquals("A big yellow bird is in the pixel.", maskFromXml.getDescription());
        assertEquals(0.7, maskFromXml.getImageTransparency(), 0.0);
        assertEquals(new Color(17, 11, 67), maskFromXml.getImageColor());


        assertEquals("false", maskFromXml.getImageConfig().getValue(Mask.BandMathType.PROPERTY_NAME_EXPRESSION));
    }


}
