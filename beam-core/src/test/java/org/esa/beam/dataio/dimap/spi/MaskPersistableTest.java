package org.esa.beam.dataio.dimap.spi;

import com.bc.ceres.glevel.MultiLevelImage;
import static org.esa.beam.dataio.dimap.DimapProductConstants.*;
import org.esa.beam.framework.datamodel.Mask;
import org.esa.beam.framework.datamodel.Product;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import static org.junit.Assert.*;
import org.junit.Test;

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;

public class MaskPersistableTest {

    @Test
    public void createXmlFromObject() {
        final Mask.RangeType rangeType = new Mask.RangeType();
        final Mask mask = new Mask("myRange", 10, 10, rangeType);
        mask.setDescription("Carefully defined range");
        mask.setImageColor(new Color(0, 255, 0, 128));
        mask.setImageTransparency(0.78);

        final RangeTypePersistable persistable = new RangeTypePersistable();
        final Element element = persistable.createXmlFromObject(mask);
        assertNotNull(element);
        assertEquals(TAG_MASK, element.getName());
        assertEquals(Mask.RangeType.TYPE_NAME, getAttributeString(element, ATTRIB_TYPE));

        final Element name = element.getChild(TAG_NAME);
        assertEquals("myRange", getAttributeString(name, ATTRIB_VALUE));

        final Element description = element.getChild(TAG_DESCRIPTION);
        assertEquals("Carefully defined range", getAttributeString(description, ATTRIB_VALUE));

        final Element color = element.getChild(TAG_COLOR);
        assertEquals(0, getAttributeInt(color, ATTRIB_RED));
        assertEquals(255, getAttributeInt(color, ATTRIB_GREEN));
        assertEquals(0, getAttributeInt(color, ATTRIB_BLUE));
        assertEquals(128, getAttributeInt(color, ATTRIB_ALPHA));

        final Element transparency = element.getChild(TAG_TRANSPARENCY);
        assertEquals(0.78, getAttributeDouble(transparency, ATTRIB_VALUE), 0.0);
    }

    @Test
    public void testXmlCreationWithoutMaskDescription() {
        final Mask mask = new Mask("name", 10, 10, new TestImageType());
        mask.setDescription(null);
        final DimapPersistable persistable = new TestMaskPersistable();
        final Element element = persistable.createXmlFromObject(mask);

        final Element description = element.getChild(TAG_DESCRIPTION);
        assertNotNull(description);
        assertTrue(description.getAttribute(ATTRIB_VALUE).getValue().isEmpty());
    }

    @Test
    public void testMaskCreation() throws IOException, JDOMException {
        final DimapPersistable persistable = new TestMaskPersistable();
        final InputStream resourceStream = getClass().getResourceAsStream("TestMask.xml");
        final Document document = new SAXBuilder().build(resourceStream);
        final Product product = new Product("P", "T", 10, 10);
        final Mask maskFromXml = (Mask) persistable.createObjectFromXml(document.getRootElement(), product);

        assertNotNull(maskFromXml);
        assertEquals(TestImageType.class, maskFromXml.getImageType().getClass());
        assertEquals(10, maskFromXml.getSceneRasterWidth());
        assertEquals(10, maskFromXml.getSceneRasterHeight());
        assertEquals("Bibo", maskFromXml.getName());
        assertEquals("A big yellow bird is in the pixel.", maskFromXml.getDescription());
        assertEquals(0.7, maskFromXml.getImageTransparency(), 0.0);
        assertEquals(new Color(17, 11, 67), maskFromXml.getImageColor());
    }


    private int getAttributeInt(Element element, String attribName) {
        return Integer.parseInt(element.getAttribute(attribName).getValue());
    }

    private double getAttributeDouble(Element element, String attribName) {
        return Double.parseDouble(element.getAttribute(attribName).getValue());
    }

    private String getAttributeString(Element element, String attribName) {
        return element.getAttribute(attribName).getValue();
    }

    private class TestMaskPersistable extends MaskPersistable {

        @Override
        protected Mask.ImageType createImageType() {
            return new TestImageType();
        }

        @Override
        protected void configureMask(Mask mask, Element element) {
        }

        @Override
        protected void configureElement(Element root, Mask mask) {
        }
    }

    private static class TestImageType extends Mask.ImageType {

        public TestImageType() {
            super("type");
        }

        @Override
            public MultiLevelImage createImage(Mask mask) {
            return null;
        }
    }
}
