/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.core.util;

import junit.framework.Assert;
import junit.framework.TestCase;

public class TreeNodeTest extends TestCase {

    public void testProxy() {
        TreeNode<String> n = new TreeNode<String>("x");
        Assert.assertEquals("x", n.getId());
        Assert.assertEquals(null, n.getParent());
        Assert.assertEquals(null, n.getContent());
        assertNotNull(n.getChildren());
        Assert.assertEquals(0, n.getChildren().length);
    }

    public void testGetChildren() {
        TreeNode<String> root = new TreeNode<String>("");

        TreeNode<String>[] children = root.getChildren();
        assertNotNull(children);
        assertEquals(0, children.length);

        root.createChild("foo/bar");
        root.createChild("foo/baz");
        root.createChild("foo/boo");
        children = root.getChildren();
        assertNotNull(children);
        assertEquals(1, children.length);

        children = root.getChild("foo").getChildren();
        assertNotNull(children);
        assertEquals(3, children.length);
    }

    public void testGetAndCreateChild() {
        TreeNode<String> root = new TreeNode<String>("");
        TreeNode<String> subChild = root.createChild("foo/bar/grunt");

        TreeNode<String> foo = root.getChild("foo");
        assertNotNull(foo);
        Assert.assertEquals("foo", foo.getId());

        TreeNode<String> bar = foo.getChild("bar");
        assertNotNull(bar);
        Assert.assertEquals("bar", bar.getId());

        TreeNode<String> grunt = bar.getChild("grunt");
        assertNotNull(grunt);
        Assert.assertEquals("grunt", grunt.getId());
        assertSame(grunt, subChild);

        TreeNode<String> subChild2 = root.createChild("foo/bar/baz");
        TreeNode<String> baz = bar.getChild("baz");
        assertNotNull(baz);
        Assert.assertEquals("baz", baz.getId());
        assertSame(baz, subChild2);

        assertSame(baz, root.getChild("foo/bar/baz"));
        assertSame(grunt, root.getChild("foo/bar/grunt"));
        assertSame(bar, root.getChild("foo/bar"));
        assertSame(foo, root.getChild("foo"));

        assertSame(root, root.getChild(""));
        assertSame(baz, baz.getChild(""));
        assertSame(grunt, grunt.getChild(""));
        assertSame(bar, bar.getChild(""));
        assertSame(foo, foo.getChild(""));

        assertSame(root, root.getChild("."));
        assertSame(baz, baz.getChild("."));
        assertSame(grunt, grunt.getChild("."));
        assertSame(bar, bar.getChild("."));
        assertSame(foo, foo.getChild("."));

        assertSame(null, root.getChild(".."));
        assertSame(bar, baz.getChild(".."));
        assertSame(bar, grunt.getChild(".."));
        assertSame(foo, bar.getChild(".."));
        assertSame(root, foo.getChild(".."));
        assertSame(baz, root.getChild("foo/bar/grunt/../baz"));

        assertSame(null, root.getChild("foo/bar/pippo"));
        assertSame(null, root.getChild("foo/pippo/grunt"));
        assertSame(null, root.getChild("pippo"));
    }


    public void testRoot() {
        TreeNode<String> root = new TreeNode<String>("");
        TreeNode<String> grunt = root.createChild("foo/bar/grunt");
        assertSame(root, grunt.getRoot());
    }

    public void testAbsolutePaths() {
        TreeNode<String> root = new TreeNode<String>("");
        TreeNode<String> foo = root.createChild("foo");
        TreeNode<String> bar = foo.createChild("bar");
        TreeNode<String> grunt = bar.createChild("grunt");

        assertSame(grunt, grunt.getChild("/foo/bar/grunt"));
        assertSame(grunt, foo.getChild("/foo/bar/grunt"));
        assertSame(grunt, bar.getChild("/foo/bar/grunt"));
        assertSame(grunt, grunt.getChild("/foo/bar/grunt"));

        TreeNode<String> baz = grunt.createChild("/foo/bar/baz");
        assertSame(baz, grunt.getChild("/foo/bar/baz"));
        assertSame(baz, foo.getChild("/foo/bar/baz"));
        assertSame(baz, bar.getChild("/foo/bar/baz"));
        assertSame(baz, grunt.getChild("/foo/bar/baz"));
        assertSame(baz, baz.getChild("/foo/bar/baz"));
    }
}
