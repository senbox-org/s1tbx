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

package com.bc.ceres.resource;


import org.apache.velocity.VelocityContext;
import org.junit.Test;

import static org.junit.Assert.*;


public class ResourceEngineTest {

    @Test
    public void testResourceEngine() throws Exception {
        ResourceEngine engine = new ResourceEngine();

        engine.processAndAddResource("a", new StringResource("simple", "foo bar baz"));
        VelocityContext velocityContext = engine.getVelocityContext();
        velocityContext.put("v", "direct");
        engine.processAndAddResource("b", new StringResource("variables", "process ${a.content} in ${v} mode"));
        engine.processAndAddResource("c", new StringResource("recursiveVariables", "do ${b.content} and ${a.content}"));

        Resource resultResource = engine.getResource("b");
        assertEquals("process foo bar baz in direct mode", resultResource.getContent());

        Resource resultC = engine.getResource("c");
        assertEquals("do process foo bar baz in direct mode and foo bar baz", resultC.getContent());
    }

    @Test
    public void testGetVelocityContext() throws Exception {
        ResourceEngine engine = new ResourceEngine();
        VelocityContext velocityContext = engine.getVelocityContext();
        assertNotNull(velocityContext);
        assertEquals(0, velocityContext.getKeys().length);
    }

    @Test
    public void testThatResourcesAreInVelocityContext() throws Exception {
        ResourceEngine engine = new ResourceEngine();
        engine.processAndAddResource("a", new StringResource("apath", "foo bar baz"));
        VelocityContext velocityContext = engine.getVelocityContext();
        assertEquals(1, velocityContext.getKeys().length);
        Object a = velocityContext.get("a");
        assertNotNull(a);
        assertTrue(a instanceof Resource);
        Resource aResource = (Resource) a;
        assertEquals("apath", aResource.getPath());
        assertEquals("foo bar baz", aResource.getContent());
        assertEquals("foo bar baz", aResource.toString());
        assertFalse(aResource.isXml());
    }

    @Test
    public void testProcessAndGetResource() throws Exception {
        ResourceEngine engine = new ResourceEngine();
        StringResource a = new StringResource("apath", "foo bar baz");
        StringResource b = new StringResource("bpath", "process ${a.content}");

        engine.processAndAddResource("a", a);
        engine.processAndAddResource("b", b);

        Resource aResource = engine.getResource("a");
        assertNotNull(aResource);
        assertEquals("apath", aResource.getPath());
        assertEquals("foo bar baz", aResource.getContent());
        assertEquals("foo bar baz", aResource.toString());
        assertNull(aResource.getOrigin());

        Resource bResource = engine.getResource("b");
        assertNotNull(bResource);
        assertEquals("bpath", bResource.getPath());
        assertEquals("process foo bar baz", bResource.getContent());
        assertEquals("process foo bar baz", bResource.toString());
        Resource bOrigin = bResource.getOrigin();
        assertNotNull(bOrigin);
        assertEquals("bpath", bOrigin.getPath());
        assertEquals("process ${a.content}", bOrigin.getContent());
        assertEquals("process ${a.content}", bOrigin.toString());
    }

    @Test
    public void testVelocityContextContainsResults() throws Exception {
        ResourceEngine engine = new ResourceEngine();
        StringResource a = new StringResource("apath", "foo bar baz");
        StringResource b = new StringResource("bpath", "process ${a.content}");

        engine.processAndAddResource("a", a);
        engine.processAndAddResource("b", b);

        VelocityContext velocityContext = engine.getVelocityContext();
        assertEquals(2, velocityContext.getKeys().length);
        Object obj = velocityContext.get("b");
        assertNotNull(obj);
        assertTrue(obj instanceof Resource);
        Resource bResource = (Resource) obj;
        assertEquals("bpath", bResource.getPath());
        assertEquals("process foo bar baz", bResource.getContent());
        assertEquals("process foo bar baz", bResource.toString());
    }
}
