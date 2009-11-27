package org.esa.beam.dataio.dimap.spi;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Mask;
import org.esa.beam.framework.datamodel.ProductData;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

/**
 * @author Marco Peters
 * @version $ Revision $ Date $
 * @since BEAM 4.7
 */
public class RangeTypePersistableSpiTest {
    private RangeTypePersistableSpi _persistableSpi;

    @Before
    public void setUp() {
        _persistableSpi = new RangeTypePersistableSpi();
    }

    @After
    public void tearDown() throws Exception {
        _persistableSpi = null;
    }

    @Test
    public void testCanDecode_GoodElement() throws JDOMException, IOException {
        final InputStream resourceStream = getClass().getResourceAsStream("RangeMask.xml");
        final Document document = new SAXBuilder().build(resourceStream);

        assertTrue(_persistableSpi.canDecode(document.getRootElement()));
    }

    @Test
    public void testCanDecode_NotDecodeableElement() {

        final Element element = new Element("SomeWhat");

        assertFalse(_persistableSpi.canDecode(element));
    }

    @Test
    public void testCanPersist() {
        final Mask mask = new Mask("b", 2, 2, new Mask.RangeType());

        assertTrue(_persistableSpi.canPersist(mask));

        assertFalse(_persistableSpi.canPersist(new ArrayList()));
        assertFalse(_persistableSpi.canPersist(new Object()));
        assertFalse(_persistableSpi.canPersist(new Band("b", ProductData.TYPE_INT8, 2, 2)));
    }

    @Test
    public void testCreatePersistable() {
        assertNotNull(_persistableSpi.createPersistable());
    }


}
