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

import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

public class MetadataResourceEngineTest {

    private MetadataResourceEngine metadataResourceEngine;
    private SimpleFileSystemMock ioAccessor;

    @Before
    public void setUp() throws Exception {
        ioAccessor = new SimpleFileSystemMock();
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
        ioAccessor.setReader("templates/metadata.xml.vm", new StringReader("I would say: ${var1} ${var2}"));
        StringWriter stringWriter = new StringWriter();
        ioAccessor.setWriter("out/MER_L2-metadata.xml", stringWriter);

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
        ioAccessor.setReader("static.properties", new StringReader("key = sdkfj"));

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
        ioAccessor.setReader("evaluation.data", reader);

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
        ioAccessor.setReader("static.xml", reader);

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
        ioAccessor.setReader("input/MER_L1-report.xml", new StringReader("<?xml>this is XML</xml>"));
        ioAccessor.setReader("input/MER_L1-meta.txt", new StringReader("key = BEAM is ${state}"));
        ioAccessor.setDirectoryList("input", "MER_L1-report.xml", "MER_L1-meta.txt", "MER_L1.N1");

        VelocityContext velocityContext = metadataResourceEngine.getVelocityContext();

        metadataResourceEngine.readRelatedResource("source1", "input/MER_L1.N1");

        assertEquals(2, velocityContext.getKeys().length);
        assertNotNull(velocityContext.get("source1"));
        assertNotNull(velocityContext.get("sourceIDs"));

        Object object = velocityContext.get("source1");
        assertTrue(object instanceof Map);
        Map map = (Map) object;
        assertEquals(2, map.size());
        assertTrue(map.containsKey("report.xml"));
        assertTrue(map.containsKey("meta.txt"));

        object = map.get("report.xml");
        assertNotNull(object);
        assertTrue(object instanceof Resource);
        Resource resource = (Resource) object;
        assertTrue(resource.isXml());
        assertEquals("<?xml>this is XML</xml>", resource.getContent());
        Map<String, String> resourceMap = resource.getMap();
        assertNotNull(resourceMap);
        assertTrue(resourceMap.isEmpty());

        object = map.get("meta.txt");
        assertNotNull(object);
        assertTrue(object instanceof Resource);
        resource = (Resource) object;
        assertFalse(resource.isXml());
        assertEquals("key = BEAM is ${state}", resource.getContent()); // sourceMetadata NEVER gets evaluated
        resourceMap = resource.getMap();
        assertNotNull(resourceMap);
        assertEquals(1, resourceMap.size());
        assertEquals("BEAM is ${state}", resourceMap.get("key"));

        object = velocityContext.get("sourceIDs");
        assertTrue(object instanceof List);
        List list = (List) object;
        assertEquals(1, list.size());
        assertTrue(list.contains("source1"));
    }

    @Test
    public void testSourceMetadataWith2ProductsInSameDirectory() throws Exception {
        ioAccessor.setReader("input/MER_L1-report.xml", new StringReader("<?xml>hello</xml>"));
        ioAccessor.setReader("input/MER_FRS_L1-meta.xml", new StringReader("world"));
        ioAccessor.setDirectoryList("input", "MER_L1-report.xml", "MER_L1.dim", "MER_L1.data", "MER_FRS_L1-meta.xml", "MER_FRS_L1.N1");

        VelocityContext velocityContext = metadataResourceEngine.getVelocityContext();

        metadataResourceEngine.readRelatedResource("source1", "input/MER_L1.N1");
        metadataResourceEngine.readRelatedResource("source2", "input/MER_FRS_L1.N1");

        assertEquals(3, velocityContext.getKeys().length);

        assertNotNull(velocityContext.get("source1"));
        assertNotNull(velocityContext.get("source2"));
        assertNotNull(velocityContext.get("sourceIDs"));

        Object object = velocityContext.get("source1");
        assertTrue(object instanceof Map);
        Map map = (Map) object;
        assertEquals(1, map.size());
        assertTrue(map.containsKey("report.xml"));

        object = map.get("report.xml");
        assertNotNull(object);
        assertTrue(object instanceof Resource);
        Resource resource = (Resource) object;
        assertTrue(resource.isXml());
        assertEquals("input/MER_L1-report.xml", resource.getPath());
        assertEquals("<?xml>hello</xml>", resource.getContent());

        object = velocityContext.get("source2");
        assertTrue(object instanceof Map);
        map = (Map) object;
        assertEquals(1, map.size());
        assertTrue(map.containsKey("meta.xml"));

        object = map.get("meta.xml");
        assertNotNull(object);
        assertTrue(object instanceof Resource);
        resource = (Resource) object;
        assertFalse(resource.isXml());
        assertEquals("input/MER_FRS_L1-meta.xml", resource.getPath());
        assertEquals("world", resource.getContent());

        object = velocityContext.get("sourceIDs");
        assertTrue(object instanceof List);
        List list = (List) object;
        assertEquals(2, list.size());
        assertTrue(list.contains("source1"));
        assertTrue(list.contains("source2"));
    }

    @Test
    public void testSourceMetadataWith2Sources() throws Exception {
        ioAccessor.setReader("input1/MER_L1-report.xml", new StringReader("<?xml>this is XML</xml>"));
        ioAccessor.setReader("input2/MER_FRS_L1-meta.xml", new StringReader("<?xml>this is XML</xml> <tag>value</tag>"));
        ioAccessor.setDirectoryList("input1", "MER_L1-report.xml", "MER_L1.N1");
        ioAccessor.setDirectoryList("input2", "MER_FRS_L1-meta.xml", "MER_FRS_L1.N1");

        VelocityContext velocityContext = metadataResourceEngine.getVelocityContext();

        metadataResourceEngine.readRelatedResource("source1", "input1/MER_L1.N1");
        metadataResourceEngine.readRelatedResource("source2", "input2/MER_FRS_L1.N1");

        assertEquals(3, velocityContext.getKeys().length);

        assertNotNull(velocityContext.get("source1"));
        assertNotNull(velocityContext.get("source2"));
        assertNotNull(velocityContext.get("sourceIDs"));

        Object object = velocityContext.get("source1");
        assertTrue(object instanceof Map);
        Map map = (Map) object;
        assertEquals(1, map.size());
        assertTrue(map.containsKey("report.xml"));

        object = map.get("report.xml");
        assertNotNull(object);
        assertTrue(object instanceof Resource);
        Resource resource = (Resource) object;
        assertTrue(resource.isXml());
        assertEquals("input1/MER_L1-report.xml", resource.getPath());
        assertEquals("<?xml>this is XML</xml>", resource.getContent());

        object = velocityContext.get("source2");
        assertTrue(object instanceof Map);
        map = (Map) object;
        assertEquals(1, map.size());
        assertTrue(map.containsKey("meta.xml"));

        object = map.get("meta.xml");
        assertNotNull(object);
        assertTrue(object instanceof Resource);
        resource = (Resource) object;
        assertTrue(resource.isXml());
        assertEquals("input2/MER_FRS_L1-meta.xml", resource.getPath());
        assertEquals("<?xml>this is XML</xml> <tag>value</tag>", resource.getContent());

        object = velocityContext.get("sourceIDs");
        assertTrue(object instanceof List);
        List list = (List) object;
        assertEquals(2, list.size());
        assertTrue(list.contains("source1"));
        assertTrue(list.contains("source2"));
    }



}
