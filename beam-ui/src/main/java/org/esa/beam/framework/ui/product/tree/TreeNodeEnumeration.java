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
