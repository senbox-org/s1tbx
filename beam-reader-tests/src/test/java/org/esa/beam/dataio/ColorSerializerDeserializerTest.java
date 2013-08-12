package org.esa.beam.dataio;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.esa.beam.framework.datamodel.Mask;
import org.junit.BeforeClass;
import org.junit.Test;

import java.awt.Color;
import java.io.StringWriter;

import static org.junit.Assert.*;

/**
 * @author Marco Peters
 */
public class ColorSerializerDeserializerTest {

    private static final String JSON_CONTENT_RGB = "{\"name\":\"maskName\",\"type\":\"org.esa.beam.framework.datamodel.Mask$BandMathsType\",\"color\":\"0,255,255\",\"description\":\"describing the mask\"}";
    private static final ExpectedMask EXPECTED_MASK_RGB = new ExpectedMask("maskName", Mask.BandMathsType.class,
                                                                           Color.CYAN, "describing the mask");
    private static final String JSON_CONTENT_RGBA = "{\"name\":\"maskName\",\"type\":\"org.esa.beam.framework.datamodel.Mask$BandMathsType\",\"color\":\"101,25,0,120\",\"description\":\"describing the mask\"}";
    private static final ExpectedMask EXPECTED_MASK_RGBA = new ExpectedMask("maskName", Mask.BandMathsType.class,
                                                                            new Color(101, 25, 0, 120), "describing the mask");

    @BeforeClass
    public static void setUp() throws Exception {
    }

    @Test
    public void testSerializeRGB() throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        final StringWriter stringWriter = new StringWriter();
        mapper.writeValue(stringWriter, EXPECTED_MASK_RGB);

        assertEquals(JSON_CONTENT_RGB, stringWriter.toString());
    }


    @Test
    public void testDeserializeRGB() throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        final ExpectedMask actualExpectedMask = mapper.readValue(JSON_CONTENT_RGB, ExpectedMask.class);
        assertEquals(EXPECTED_MASK_RGB.getColor(), actualExpectedMask.getColor());
    }

    @Test
    public void testSerializeRGBA() throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        final StringWriter stringWriter = new StringWriter();
        mapper.writeValue(stringWriter, EXPECTED_MASK_RGBA);

        assertEquals(JSON_CONTENT_RGBA, stringWriter.toString());
    }


    @Test
    public void testDeserializeRGBA() throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        final ExpectedMask actualExpectedMask = mapper.readValue(JSON_CONTENT_RGBA, ExpectedMask.class);
        assertEquals(EXPECTED_MASK_RGBA.getColor(), actualExpectedMask.getColor());
    }

}
