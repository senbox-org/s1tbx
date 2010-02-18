package org.esa.beam.framework.ui.product.tree;

import javax.swing.tree.TreeNode;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;

public abstract class ProductTreeNode implements TreeNode {
    private String name;
    private Object content;
    private ProductTreeNode parent;

    protected ProductTreeNode(String name, Object content, ProductTreeNode parent) {
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

    @Override
    public ProductTreeNode getParent() {
        return parent;
    }

    @Override
    public boolean isLeaf() {
        return getChildCount() == 0;
    }

    @Override
    public boolean getAllowsChildren() {
        return true;
    }

    @Override
    public Enumeration children() {
        ProductTreeNode[] nodes = new ProductTreeNode[getChildCount()];
        for (int i = 0; i < getChildCount(); i++) {
            nodes[i] = getChildAt(i);
        }
        return Collections.enumeration(Arrays.asList(nodes));
    }

    @Override
    public int getIndex(TreeNode node) {
        return getIndex((ProductTreeNode) node);
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public abstract ProductTreeNode getChildAt(int index);

    @Override
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
