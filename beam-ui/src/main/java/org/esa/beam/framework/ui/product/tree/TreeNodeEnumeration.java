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

package org.esa.beam.framework.ui.product.tree;

import javax.swing.tree.TreeNode;
import java.util.Enumeration;
import java.util.NoSuchElementException;

// NOTE: this code is taken from DefaultMutableTreeNode

final class TreeNodeEnumeration {

    public static final Enumeration<TreeNode> EMPTY_ENUMERATION = new EmptyEnumeration();

    private TreeNodeEnumeration() {

    }

    public static final class Postorder implements Enumeration<TreeNode> {

        protected TreeNode root;
        protected Enumeration<TreeNode> children;
        protected Enumeration<TreeNode> subtree;

        public Postorder(TreeNode rootNode) {
            super();
            root = rootNode;
            children = root.children();
            subtree = EMPTY_ENUMERATION;
        }

        public boolean hasMoreElements() {
            return root != null;
        }

        public TreeNode nextElement() {
            TreeNode retval;

            if (subtree.hasMoreElements()) {
                retval = subtree.nextElement();
            } else if (children.hasMoreElements()) {
                subtree = new Postorder(
                        (TreeNode) children.nextElement());
                retval = subtree.nextElement();
            } else {
                retval = root;
                root = null;
            }

            return retval;
        }

    }

    private static class EmptyEnumeration implements Enumeration<TreeNode> {
        public boolean hasMoreElements() {
            return false;
        }

        public TreeNode nextElement() {
            throw new NoSuchElementException("No more elements");
        }
    }
}
