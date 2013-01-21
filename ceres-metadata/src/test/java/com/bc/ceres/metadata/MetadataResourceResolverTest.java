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

import org.junit.Test;
import org.mockito.Matchers;

import java.util.SortedMap;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class MetadataResourceResolverTest {

    @Test
    public void testRemovefileExtension() throws Exception {
        SimpleFileSystem fileSystem = mock(SimpleFileSystem.class);
        MetadataResourceResolver metadataResourceResolver = new MetadataResourceResolver(fileSystem);

        when(fileSystem.isFile(Matchers.anyString())).thenReturn(true);
        // existing files
        assertEquals("foo", metadataResourceResolver.removeFileExtension("foo.txt"));
        assertEquals("foo", metadataResourceResolver.removeFileExtension("foo"));
        assertEquals("foo/bar", metadataResourceResolver.removeFileExtension("foo/bar.baz"));
        assertEquals("foo\\bar", metadataResourceResolver.removeFileExtension("foo\\bar.baz"));

        when(fileSystem.isFile(Matchers.anyString())).thenReturn(false );
        // not existing
        assertEquals("bar.foo", metadataResourceResolver.removeFileExtension("bar.foo")); // not existing file

        //directories
        assertEquals("\\root\\foo", metadataResourceResolver.removeFileExtension("\\root\\foo"));
        String directory = "C:\\Users\\bettina\\Software-Tests\\own-software\\ceres-metadata-0.13.2-SNAPSHOT\\data\\out";
        assertEquals(directory, metadataResourceResolver.removeFileExtension(directory));
        assertEquals("/root/foo/", metadataResourceResolver.removeFileExtension("/root/foo/"));
    }

    @Test
    public void testBasename() throws Exception {
        assertEquals("foo.txt", MetadataResourceResolver.getBasename("foo.txt"));

        assertEquals("foo.txt", MetadataResourceResolver.getBasename("/foo.txt"));
        assertEquals("foo.txt", MetadataResourceResolver.getBasename("./foo.txt"));
        assertEquals("foo.txt", MetadataResourceResolver.getBasename("bar/foo.txt"));
        assertEquals("foo.txt", MetadataResourceResolver.getBasename("/bar/foo.txt"));

        assertEquals("foo.txt", MetadataResourceResolver.getBasename("\\foo.txt"));
        assertEquals("foo.txt", MetadataResourceResolver.getBasename(".\\foo.txt"));
        assertEquals("foo.txt", MetadataResourceResolver.getBasename("bar\\foo.txt"));
        assertEquals("foo.txt", MetadataResourceResolver.getBasename("\\bar\\foo.txt"));
    }

    @Test
    public void testDirname() throws Exception {
        assertEquals("", MetadataResourceResolver.getDirname("foo.txt"));

        assertEquals("", MetadataResourceResolver.getDirname("/foo.txt"));
        assertEquals(".", MetadataResourceResolver.getDirname("./foo.txt"));
        assertEquals("bar", MetadataResourceResolver.getDirname("bar/foo.txt"));
        assertEquals("/bar", MetadataResourceResolver.getDirname("/bar/foo.txt"));

        assertEquals("", MetadataResourceResolver.getDirname("\\foo.txt"));
        assertEquals(".", MetadataResourceResolver.getDirname(".\\foo.txt"));
        assertEquals("bar", MetadataResourceResolver.getDirname("bar\\foo.txt"));
        assertEquals("C:/bar", MetadataResourceResolver.getDirname("C:\\bar\\foo.txt"));
    }

    @Test
    public void testGetSourceNames() throws Exception {
        SimpleFileSystem fileSystem = mock(SimpleFileSystem.class);
        MetadataResourceResolver resolver = new MetadataResourceResolver(fileSystem);

        when(fileSystem.isFile(anyString())).thenReturn(true);
        when(fileSystem.list(anyString())).thenReturn(new String[]{"file1", "product-file2.xml", "file3", "product-file4.properties"});
        SortedMap<String,String> sourceMetadataPaths= resolver.getSourceMetadataPaths("/bla/bli/product.dim");

        assertEquals(2, sourceMetadataPaths.size());
        assertEquals("/bla/bli/product-file2.xml", sourceMetadataPaths.get("file2.xml"));
        assertEquals("/bla/bli/product-file4.properties", sourceMetadataPaths.get("file4.properties"));
    }
}
