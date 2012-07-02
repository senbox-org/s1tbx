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

import org.apache.velocity.VelocityContext;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Map;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

public class MetadataEngineTest {

    private MetadataEngine metadataEngine;
    private SimpleFileSystemMock ioAccessor;

    @Before
    public void setUp() throws Exception {
        ioAccessor = new SimpleFileSystemMock();
        metadataEngine = new MetadataEngine(ioAccessor);
    }

    @Test
    public void testCreation() throws Exception {
        VelocityContext velocityContext = metadataEngine.getVelocityContext();
        assertNotNull(velocityContext);
        assertEquals(0, velocityContext.getKeys().length);
    }

    /**
     * A typical end-to-end example usage of the {@code MetadataEngine}.
     */
    @Ignore
    public void useCaseOverview() throws Exception {
        metadataEngine.readMetadata("metadata", "input/metadata.properties", false);
        metadataEngine.readSourceMetadata("source1", "input/MER_L1_1.N1"); // <-"input/MER_L1_1-report.xml", "input/MER_L1_1-meta.txt"
        metadataEngine.readSourceMetadata("source2", "input/MER_L1_2.N1");
        VelocityContext velocityContext = metadataEngine.getVelocityContext();
        velocityContext.put("myKey", "value1");
        velocityContext.put("myOtherKey", "value2");
        metadataEngine.writeTargetMetadata("templates/report.xml.vm", "output/MER_L2_1.dim"); // ->"output/MER_L2_1-report.xml"
        metadataEngine.writeTargetMetadata("templates/report.txt.vm", "output/MER_L2_1.dim"); // ->"output/MER_L2_1-report.txt"
    }

    @Test
    public void testWriteTargetMetadata() throws Exception {
        ioAccessor.setReader("templates/metadata.xml.vm", new StringReader("I would say: ${var1} ${var2}"));
        StringWriter stringWriter = new StringWriter();
        ioAccessor.setWriter("out/MER_L2-metadata.xml", stringWriter);

        VelocityContext velocityContext = metadataEngine.getVelocityContext();
        velocityContext.put("var1", "Hello");
        velocityContext.put("var2", "world");

        metadataEngine.writeTargetMetadata("templates/metadata.xml.vm", "out/MER_L2.dim");

        assertEquals("I would say: Hello world", stringWriter.toString());

        assertEquals(4, velocityContext.getKeys().length);
        assertEquals("metadata.xml.vm", velocityContext.get("templateName"));
        assertEquals("metadata.xml", velocityContext.get("templateBaseName"));
    }

    @Test
    public void testMetadataAsProperties() throws Exception {
        ioAccessor.setReader("static.properties", new StringReader("key = sdkfj"));

        VelocityContext velocityContext = metadataEngine.getVelocityContext();
        metadataEngine.readMetadata("props", "static.properties", false);
        assertEquals(1, velocityContext.getKeys().length);

        Object metadata = velocityContext.get("props");
        assertTrue(metadata instanceof MetadataResource);
        MetadataResource resource = (MetadataResource) metadata;
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

        VelocityContext velocityContext = metadataEngine.getVelocityContext();
        velocityContext.put("state", "ok");
        metadataEngine.readMetadata("foo", "evaluation.data", true);
        assertEquals(2, velocityContext.getKeys().length);

        Object metadata = velocityContext.get("foo");
        assertTrue(metadata instanceof MetadataResource);
        MetadataResource resource = (MetadataResource) metadata;
        assertFalse(resource.isXml());
        assertEquals("key = BEAM is ok", resource.getContent());

        Map<String, String> map = resource.getMap();
        assertNotNull(map);
        assertEquals(1, map.size());
        assertEquals("BEAM is ok", map.get("key"));
    }

    @Test
    public void testMetadataAsPropertiesWithoutEvaluation() throws Exception {
        Reader reader = new StringReader("key = BEAM is ${state}");
        ioAccessor.setReader("evaluation.data", reader);

        VelocityContext velocityContext = metadataEngine.getVelocityContext();
        velocityContext.put("state", "ok");
        metadataEngine.readMetadata("foo", "evaluation.data", false);
        assertEquals(2, velocityContext.getKeys().length);

        Object metadata = velocityContext.get("foo");
        assertTrue(metadata instanceof MetadataResource);
        MetadataResource resource = (MetadataResource) metadata;
        assertFalse(resource.isXml());
        assertEquals("key = BEAM is ${state}", resource.getContent());

        Map<String, String> map = resource.getMap();
        assertNotNull(map);
        assertEquals(1, map.size());
        assertEquals("BEAM is ${state}", map.get("key"));
    }

    @Test
    public void testMetadataAsXML() throws Exception {
        Reader reader = new StringReader("<?xml>this is XML</xml>");
        ioAccessor.setReader("static.xml", reader);

        VelocityContext velocityContext = metadataEngine.getVelocityContext();

        metadataEngine.readMetadata("myxml", "static.xml", false);

        assertEquals(1, velocityContext.getKeys().length);

        assertNull(velocityContext.get("wrongKey"));

        Object metadata = velocityContext.get("myxml");
        assertTrue(metadata instanceof MetadataResource);
        MetadataResource resource = (MetadataResource) metadata;
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
        ioAccessor.setList("input", "MER_L1-report.xml", "MER_L1-meta.txt", "MER_L1.N1");

        VelocityContext velocityContext = metadataEngine.getVelocityContext();

        metadataEngine.readSourceMetadata("source1", "input/MER_L1.N1");

        assertEquals(1, velocityContext.getKeys().length);
        assertNotNull(velocityContext.get("source1"));

        Object object = velocityContext.get("source1");
        assertTrue(object instanceof Map);
        Map map = (Map) object;
        assertEquals(2, map.size());

        Object report = map.get("report.xml");
        assertNotNull(report);
        assertTrue(report instanceof MetadataResource);
        MetadataResource resource = (MetadataResource) report;
        assertTrue(resource.isXml());
        assertEquals("<?xml>this is XML</xml>", resource.getContent());
        Map<String, String> resourceMap = resource.getMap();
        assertNotNull(resourceMap);
        assertTrue(resourceMap.isEmpty());

        report = map.get("meta.txt");
        assertNotNull(report);
        assertTrue(report instanceof MetadataResource);
        resource = (MetadataResource) report;
        assertFalse(resource.isXml());
        assertEquals("key = BEAM is ${state}", resource.getContent()); // sourceMetadata NEVER gets evaluated
        resourceMap = resource.getMap();
        assertNotNull(resourceMap);
        assertEquals(1, resourceMap.size());
        assertEquals("BEAM is ${state}", resourceMap.get("key"));
    }

    @Test
    public void testSourceMetadataWith2ProductsInSameDirectory() throws Exception {
        ioAccessor.setReader("input/MER_L1-report.xml", new StringReader("<?xml>hello</xml>"));
        ioAccessor.setReader("input/MER_FRS_L1-meta.xml", new StringReader("world"));
        ioAccessor.setList("input", "MER_L1-report.xml", "MER_L1.dim", "MER_L1.data", "MER_FRS_L1-meta.xml", "MER_FRS_L1.N1");

        VelocityContext velocityContext = metadataEngine.getVelocityContext();

        metadataEngine.readSourceMetadata("source1", "input/MER_L1.N1");
        metadataEngine.readSourceMetadata("source2", "input/MER_FRS_L1.N1");

        assertEquals(2, velocityContext.getKeys().length);

        assertNotNull(velocityContext.get("source1"));
        Object object = velocityContext.get("source1");
        assertTrue(object instanceof Map);
        Map map = (Map) object;
        assertEquals(1, map.size());

        Object report = map.get("report.xml");
        assertNotNull(report);
        assertTrue(report instanceof MetadataResource);
        MetadataResource resource = (MetadataResource) report;
        assertTrue(resource.isXml());
        assertEquals("<?xml>hello</xml>", resource.getContent());

        assertNotNull(velocityContext.get("source2"));
        object = velocityContext.get("source2");
        assertTrue(object instanceof Map);
        Map map2 = (Map) object;
        assertEquals(1, map2.size());

        report = map2.get("meta.xml");
        assertNotNull(report);
        assertTrue(report instanceof MetadataResource);
        resource = (MetadataResource) report;
        assertFalse(resource.isXml());
        assertEquals("world", resource.getContent());
    }

    @Test
    public void testSourceMetadataWith2Sources() throws Exception {
        ioAccessor.setReader("input1/MER_L1-report.xml", new StringReader("<?xml>this is XML</xml>"));
        ioAccessor.setReader("input2/MER_FRS_L1-meta.xml", new StringReader("<?xml>this is XML</xml> <tag>value</tag>"));
        ioAccessor.setList("input1", "MER_L1-report.xml", "MER_L1.N1");
        ioAccessor.setList("input2", "MER_FRS_L1-meta.xml", "MER_FRS_L1.N1");

        VelocityContext velocityContext = metadataEngine.getVelocityContext();

        metadataEngine.readSourceMetadata("source1", "input1/MER_L1.N1");
        metadataEngine.readSourceMetadata("source2", "input2/MER_FRS_L1.N1");

        assertEquals(2, velocityContext.getKeys().length);

        assertNotNull(velocityContext.get("source1"));
        Object object = velocityContext.get("source1");
        assertTrue(object instanceof Map);

        assertNotNull(velocityContext.get("source2"));
        object = velocityContext.get("source2");
        assertTrue(object instanceof Map);
        Map map = (Map) object;
        assertEquals(1, map.size());

        Object report = map.get("meta.xml");
        assertNotNull(report);
        assertTrue(report instanceof MetadataResource);
        MetadataResource resource = (MetadataResource) report;
        assertTrue(resource.isXml());
        assertEquals("<?xml>this is XML</xml> <tag>value</tag>", resource.getContent());
        Map<String, String> resourceMap = resource.getMap();
        assertNotNull(resourceMap);
        assertTrue(resourceMap.isEmpty());
    }

    @Test
    public void testRemovefileExtension() throws Exception {
        assertEquals("foo", MetadataEngine.removeFileExtension("foo.txt"));
        assertEquals("foo", MetadataEngine.removeFileExtension("foo"));
        assertEquals("foo/bar", MetadataEngine.removeFileExtension("foo/bar.baz"));
        assertEquals("foo\\bar", MetadataEngine.removeFileExtension("foo\\bar.baz"));
    }

    @Test
    public void testBasename() throws Exception {
        assertEquals("foo.txt", MetadataEngine.getBasename("foo.txt"));

        assertEquals("foo.txt", MetadataEngine.getBasename("/foo.txt"));
        assertEquals("foo.txt", MetadataEngine.getBasename("./foo.txt"));
        assertEquals("foo.txt", MetadataEngine.getBasename("bar/foo.txt"));
        assertEquals("foo.txt", MetadataEngine.getBasename("/bar/foo.txt"));

        assertEquals("foo.txt", MetadataEngine.getBasename("\\foo.txt"));
        assertEquals("foo.txt", MetadataEngine.getBasename(".\\foo.txt"));
        assertEquals("foo.txt", MetadataEngine.getBasename("bar\\foo.txt"));
        assertEquals("foo.txt", MetadataEngine.getBasename("\\bar\\foo.txt"));
    }

    @Test
    public void testDirname() throws Exception {
        assertEquals("", MetadataEngine.getDirname("foo.txt"));

        assertEquals("", MetadataEngine.getDirname("/foo.txt"));
        assertEquals(".", MetadataEngine.getDirname("./foo.txt"));
        assertEquals("bar", MetadataEngine.getDirname("bar/foo.txt"));
        assertEquals("/bar", MetadataEngine.getDirname("/bar/foo.txt"));

        assertEquals("", MetadataEngine.getDirname("\\foo.txt"));
        assertEquals(".", MetadataEngine.getDirname(".\\foo.txt"));
        assertEquals("bar", MetadataEngine.getDirname("bar\\foo.txt"));
        assertEquals("C:/bar", MetadataEngine.getDirname("C:\\bar\\foo.txt"));
    }

}
