package org.esa.beam.framework.ui.application.support;

import junit.framework.TestCase;
import com.thoughtworks.xstream.XStream;

public class DefaultApplicationDescriptorTest extends TestCase {
    private static final String XML =
            "<applicationDescriptor>\n" +
            "  <applicationId>DatMain</applicationId>\n" +
            "  <displayName>DAT</displayName>\n" +
            "  <frameIcon>/org/esa/nest/dat/images/frame-icon.png</frameIcon>\n" +
            "  <image>/org/esa/nest/dat/images/about.jpg</image>\n" +
            "  <excludedActions>\n" +
            "      <id>a1</id>\n" +
            "      <id>a2</id>\n" +
            "  </excludedActions>\n" +
            "  <excludedToolViews>\n" +
            "      <id>tv1</id>\n" +
            "      <id>tv2</id>\n" +
            "      <id>tv3</id>\n" +
            "  </excludedToolViews>\n" +
            "</applicationDescriptor>";

    public void testUnmarshalling() {

        final XStream xStream = new XStream();
        xStream.autodetectAnnotations(true);
        xStream.alias("applicationDescriptor", DefaultApplicationDescriptor.class);
        Object o = null;
        try {
           o = xStream.fromXML(XML);
        } catch (Exception e) {
            fail(e.getMessage());
            e.printStackTrace();
        }
        assertNotNull(o);
        assertSame(DefaultApplicationDescriptor.class, o.getClass());
        DefaultApplicationDescriptor defaultApplicationDescriptor = (DefaultApplicationDescriptor) o;

        assertEquals("DatMain", defaultApplicationDescriptor.getApplicationId());
        assertEquals("DAT", defaultApplicationDescriptor.getDisplayName());
        assertEquals("/org/esa/nest/dat/images/frame-icon.png", defaultApplicationDescriptor.getFrameIconPath());
        assertEquals("/org/esa/nest/dat/images/about.jpg", defaultApplicationDescriptor.getImagePath());


        assertNotNull(defaultApplicationDescriptor.getExcludedActions());
        assertEquals(2, defaultApplicationDescriptor.getExcludedActions().length);
        assertEquals("a1", defaultApplicationDescriptor.getExcludedActions()[0]);
        assertEquals("a2", defaultApplicationDescriptor.getExcludedActions()[1]);

        assertNotNull(defaultApplicationDescriptor.getExcludedToolViews());
        assertEquals(3, defaultApplicationDescriptor.getExcludedToolViews().length);
        assertEquals("tv1", defaultApplicationDescriptor.getExcludedToolViews()[0]);
        assertEquals("tv2", defaultApplicationDescriptor.getExcludedToolViews()[1]);
        assertEquals("tv3", defaultApplicationDescriptor.getExcludedToolViews()[2]);
    }
}
