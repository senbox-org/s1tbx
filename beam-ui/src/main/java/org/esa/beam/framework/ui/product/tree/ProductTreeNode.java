package org.esa.beam.framework.ui.product.tree;

import javax.swing.tree.TreeNode;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;

public abstract class ProductTreeNode implements TreeNode {
    private String name;
    private Object content;
    private ProductTreeNode parent;

    public ProductTreeNode(String name, Object content, ProductTreeNode parent) {
        this.name = name;
        this.content = content;
        this.parent = parent;
    }

    public String getName() {
        return name;
    }

    public Object getContent() {
        return content;
    }

    public ProductTreeNode getParent() {
        return parent;
    }

    public boolean isLeaf() {
        return getChildCount() == 0;
    }

    public boolean getAllowsChildren() {
        return true;
    }

    public Enumeration children() {
        ProductTreeNode[] nodes = new ProductTreeNode[getChildCount()];
        for (int i = 0; i < getChildCount(); i++) {
            nodes[i] = getChildAt(i);
        }
        return Collections.enumeration(Arrays.asList(nodes));
    }

    public int getIndex(TreeNode node) {
        return getIndex((ProductTreeNode) node);
    }

    @Override
    public String toString() {
        return getName();
    }

    public abstract ProductTreeNode getChildAt(int index);

    public abstract int getChildCount();

    protected abstract int getIndex(ProductTreeNode node);

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ProductTreeNode node = (ProductTreeNode) o;

        if (content != null ? !content.equals(node.content) : node.content != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return content != null ? content.hashCode() : 0;
    }
}
