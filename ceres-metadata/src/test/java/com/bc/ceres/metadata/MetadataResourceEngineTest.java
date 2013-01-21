/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package com.bc.ceres.metadata;

import com.bc.ceres.resource.Resource;
import org.apache.velocity.VelocityContext;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;

import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Map;

import static org.junit.Assert.*;

public class MetadataResourceEngineTest {

    private MetadataResourceEngine metadataResourceEngine;
    private SimpleFileSystem ioAccessor;

    @Before
    public void setUp() throws Exception {
        ioAccessor = Mockito.mock(SimpleFileSystem.class);
        metadataResourceEngine = new MetadataResourceEngine(ioAccessor);
    }

    @Test
    public void testCreation() throws Exception {
        VelocityContext velocityContext = metadataResourceEngine.getVelocityContext();
        assertNotNull(velocityContext);
        assertEquals(0, velocityContext.getKeys().length);
    }

    /**
     * A typical end-to-end example usage of the {@code MetadataResourceEngine}.
     */
    @Ignore
    public void useCaseOverview() throws Exception {
        metadataResourceEngine.readResource("metadata", "input/metadata.properties");
        metadataResourceEngine.readRelatedResource("source1", "input/MER_L1_1.N1"); // <-"input/MER_L1_1-report.xml", "input/MER_L1_1-meta.txt"
        metadataResourceEngine.readRelatedResource("source2", "input/MER_L1_2.N1");
        VelocityContext velocityContext = metadataResourceEngine.getVelocityContext();
        velocityContext.put("myKey", "value1");
        velocityContext.put("myOtherKey", "value2");
        metadataResourceEngine.writeRelatedResource("templates/report.xml.vm", "output/MER_L2_1.dim"); // ->"output/MER_L2_1-report.xml"
        metadataResourceEngine.writeRelatedResource("templates/report.txt.vm", "output/MER_L2_1.dim"); // ->"output/MER_L2_1-report.txt"
    }

    @Test
    public void testWriteTargetMetadata() throws Exception {
        Mockito.when(ioAccessor.createReader("templates/metadata.xml.vm")).thenReturn(
                new StringReader("I would say: ${var1} ${var2}"));
        StringWriter stringWriter = new StringWriter();
        Mockito.when(ioAccessor.createWriter("out/MER_L2-metadata.xml")).thenReturn(stringWriter);
        Mockito.when(ioAccessor.isFile("out/MER_L2.dim")).thenReturn(true);

        VelocityContext velocityContext = metadataResourceEngine.getVelocityContext();
        velocityContext.put("var1", "Hello");
        velocityContext.put("var2", "world");

        metadataResourceEngine.writeRelatedResource("templates/metadata.xml.vm", "out/MER_L2.dim");

        assertEquals("I would say: Hello world", stringWriter.toString());

        assertEquals(4, velocityContext.getKeys().length);
        assertEquals("metadata.xml.vm", velocityContext.get("templateName"));
        assertEquals("metadata.xml", velocityContext.get("templateBaseName"));
    }

    @Test
    public void testMetadataAsProperties() throws Exception {
        Mockito.when(ioAccessor.createReader("static.properties")).thenReturn(new StringReader("key = sdkfj"));

        VelocityContext velocityContext = metadataResourceEngine.getVelocityContext();
        metadataResourceEngine.readResource("props", "static.properties");
        assertEquals(1, velocityContext.getKeys().length);

        Object metadata = velocityContext.get("props");
        assertTrue(metadata instanceof Resource);
        Resource resource = (Resource) metadata;
        assertFalse(resource.isXml());
        assertEquals("key = sdkfj", resource.getContent());

        Map<String, String> map = resource.getMap();
        assertNotNull(map);
        assertEquals(1, map.size());
        assertEquals("sdkfj", map.get("key"));
    }

    @Test
    public void testMetadataAsPropertiesWithEvaluation() throws Exception {
        Reader reader = new StringReader("key = BEAM is ${state}");
        Mockito.when(ioAccessor.createReader("evaluation.data")).thenReturn(reader);

        VelocityContext velocityContext = metadataResourceEngine.getVelocityContext();
        velocityContext.put("state", "ok");
        metadataResourceEngine.readResource("foo", "evaluation.data");
        assertEquals(2, velocityContext.getKeys().length);

        Object metadata = velocityContext.get("foo");
        assertTrue(metadata instanceof Resource);
        Resource resource = (Resource) metadata;
        assertFalse(resource.isXml());
        assertEquals("key = BEAM is ok", resource.getContent());

        Map<String, String> map = resource.getMap();
        assertNotNull(map);
        assertEquals(1, map.size());
        assertEquals("BEAM is ok", map.get("key"));

        resource = resource.getOrigin();
        assertFalse(resource.isXml());
        assertEquals("key = BEAM is ${state}", resource.getContent());

        map = resource.getMap();
        assertNotNull(map);
        assertEquals(1, map.size());
        assertEquals("BEAM is ${state}", map.get("key"));

    }

    @Test
    public void testMetadataAsXML() throws Exception {
        Reader reader = new StringReader("<?xml>this is XML</xml>");
        Mockito.when(ioAccessor.createReader("static.xml")).thenReturn(reader);

        VelocityContext velocityContext = metadataResourceEngine.getVelocityContext();

        metadataResourceEngine.readResource("myxml", "static.xml");

        assertEquals(1, velocityContext.getKeys().length);

        assertNull(velocityContext.get("wrongKey"));

        Object metadata = velocityContext.get("myxml");
        assertTrue(metadata instanceof Resource);
        Resource resource = (Resource) metadata;
        assertTrue(resource.isXml());
        assertEquals("<?xml>this is XML</xml>", resource.getContent());

        Map<String, String> map = resource.getMap();
        assertNotNull(map);
        assertTrue(map.isEmpty());
    }

    @Test
    public void testSourceMetadata() throws Exception {
        Mockito.when(ioAccessor.createReader("input/MER_L1-report.xml")).thenReturn(
                new StringReader("<?xml>this is XML</xml>"));
        Mockito.when(ioAccessor.createReader("input/MER_L1-meta.txt")).thenReturn(
                new StringReader("key = BEAM is ${state}"));
        Mockito.when(ioAccessor.list("input")).thenReturn(
                new String[]{"MER_L1-report.xml", "MER_L1-meta.txt", "MER_L1.N1"});
        Mockito.when(ioAccessor.isFile(Matchers.anyString())).thenReturn(true);

        //execution
        metadataResourceEngine.readRelatedResource("source1", "input/MER_L1.N1");

        VelocityContext velocityContext = metadataResourceEngine.getVelocityContext();
        assertEquals(1, velocityContext.getKeys().length);
        assertNotNull(velocityContext.get("sourceMetadata"));

        Object object = velocityContext.get("sourceMetadata");
        assertTrue(object instanceof Map);
        Map map = (Map) object;
        assertEquals(1, map.size());
        assertTrue(map.containsKey("source1"));

        object = map.get("source1");
        assertNotNull(object);
        assertTrue(object instanceof Map);
        Map resourceMap = (Map) object;
        assertEquals(2, resourceMap.size());
        assertTrue(resourceMap.containsKey("report_xml"));
        assertTrue(resourceMap.containsKey("meta_txt"));

        object = resourceMap.get("report_xml");
        assertNotNull(object);
        assertTrue(object instanceof Resource);
        Resource resource = (Resource) object;
        assertTrue(resource.isXml());
        assertEquals("<?xml>this is XML</xml>", resource.getContent());
        Map<String, String> xmlMap = resource.getMap();
        assertNotNull(xmlMap);
        assertTrue(xmlMap.isEmpty());

        object = resourceMap.get("meta_txt");
        assertNotNull(object);
        assertTrue(object instanceof Resource);
        resource = (Resource) object;
        assertFalse(resource.isXml());
        assertEquals("key = BEAM is ${state}", resource.getContent()); // sourceMetadata NEVER gets evaluated
        Map<String, String> propertiesMap = resource.getMap();
        assertNotNull(propertiesMap);
        assertEquals(1, propertiesMap.size());
        assertEquals("BEAM is ${state}", propertiesMap.get("key"));
    }

    @Test
    public void testSourceMetadataWith2ProductsInSameDirectory() throws Exception {
        Mockito.when(ioAccessor.createReader("input/MER_L1-report.xml")).thenReturn(new StringReader("<?xml>hello</xml>"));
        Mockito.when(ioAccessor.createReader("input/MER_FRS_L1-meta.xml")).thenReturn(new StringReader("world"));
        Mockito.when(ioAccessor.list("input")).thenReturn(new String[]{"MER_L1-report.xml", "MER_L1.dim", "MER_L1.data", "MER_FRS_L1-meta.xml", "MER_FRS_L1.N1"});
        Mockito.when(ioAccessor.isFile(Matchers.endsWith(".data"))).thenReturn(false);
        Mockito.when(ioAccessor.isFile(Matchers.anyString())).thenReturn(true);

        //execution
        metadataResourceEngine.readRelatedResource("source1", "input/MER_L1.N1");
        metadataResourceEngine.readRelatedResource("source2", "input/MER_FRS_L1.N1");

        VelocityContext velocityContext = metadataResourceEngine.getVelocityContext();
        assertEquals(1, velocityContext.getKeys().length);

        assertNotNull(velocityContext.get("sourceMetadata"));

        Object object = velocityContext.get("sourceMetadata");
        assertTrue(object instanceof Map);
        Map map = (Map) object;
        assertEquals(2, map.size());
        assertTrue(map.containsKey("source1"));
        assertTrue(map.containsKey("source2"));

        object = map.get("source1");
        assertNotNull(object);
        assertTrue(object instanceof Map);
        Map sourceMap = (Map) object;
        assertEquals(1, sourceMap.size());

        object = sourceMap.get("report_xml");
        assertNotNull(object);
        assertTrue(object instanceof Resource);
        Resource resource = (Resource) object;
        assertTrue(resource.isXml());
        assertEquals("input/MER_L1-report.xml", resource.getPath());
        assertEquals("<?xml>hello</xml>", resource.getContent());

        object = map.get("source2");
        assertNotNull(object);
        assertTrue(object instanceof Map);
        sourceMap = (Map) object;
        assertEquals(1, sourceMap.size());

        object = sourceMap.get("meta_xml");
        assertNotNull(object);
        assertTrue(object instanceof Resource);
        resource = (Resource) object;
        assertFalse(resource.isXml());
        assertEquals("input/MER_FRS_L1-meta.xml", resource.getPath());
        assertEquals("world", resource.getContent());
    }

    @Test
    public void testSourceMetadataWith2Sources() throws Exception {
        Mockito.when(ioAccessor.createReader("input1/MER_L1-report.xml")).thenReturn(
                new StringReader("<?xml>this is XML</xml>"));
        Mockito.when(ioAccessor.createReader("input2/MER_FRS_L1-meta.xml")).thenReturn(
                new StringReader("<?xml>this is XML</xml> <tag>value</tag>"));
        Mockito.when(ioAccessor.list("input1")).thenReturn(new String[]{"MER_L1-report.xml", "MER_L1.N1"});
        Mockito.when(ioAccessor.list("input2")).thenReturn(new String[]{"MER_FRS_L1-meta.xml", "MER_FRS_L1.N1"});
        Mockito.when(ioAccessor.isFile(Matchers.anyString())).thenReturn(true);

        //execution
        metadataResourceEngine.readRelatedResource("source1", "input1/MER_L1.N1");
        metadataResourceEngine.readRelatedResource("source2", "input2/MER_FRS_L1.N1");

        VelocityContext velocityContext = metadataResourceEngine.getVelocityContext();
        assertEquals(1, velocityContext.getKeys().length);

        Object object = velocityContext.get("sourceMetadata");
        assertTrue(object instanceof Map);
        Map map = (Map) object;
        assertEquals(2, map.size());
        assertTrue(map.containsKey("source1"));
        assertTrue(map.containsKey("source2"));


        object = map.get("source1");
        assertTrue(object instanceof Map);
        Map sourceMap = (Map) object;
        assertEquals(1, sourceMap.size());
        assertTrue(sourceMap.containsKey("report_xml"));

        object = sourceMap.get("report_xml");
        assertNotNull(object);
        assertTrue(object instanceof Resource);
        Resource resource = (Resource) object;
        assertTrue(resource.isXml());
        assertEquals("input1/MER_L1-report.xml", resource.getPath());
        assertEquals("<?xml>this is XML</xml>", resource.getContent());

        object = map.get("source2");
        assertTrue(object instanceof Map);
        sourceMap = (Map) object;
        assertEquals(1, sourceMap.size());
        assertTrue(sourceMap.containsKey("meta_xml"));

        object = sourceMap.get("meta_xml");
        assertNotNull(object);
        assertTrue(object instanceof Resource);
        resource = (Resource) object;
        assertTrue(resource.isXml());
        assertEquals("input2/MER_FRS_L1-meta.xml", resource.getPath());
        assertEquals("<?xml>this is XML</xml> <tag>value</tag>", resource.getContent());
    }
}
