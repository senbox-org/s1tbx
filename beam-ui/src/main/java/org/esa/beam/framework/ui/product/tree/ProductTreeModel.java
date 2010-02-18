/*
 * $Id: $
 *
 * Copyright (C) 2010 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.framework.ui.product.tree;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductManager;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.datamodel.ProductNodeEvent;
import org.esa.beam.framework.datamodel.ProductNodeListener;
import org.esa.beam.framework.datamodel.ProductNodeListenerAdapter;
import org.esa.beam.framework.datamodel.VectorDataNode;

import javax.swing.event.EventListenerList;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;


public class ProductTreeModel implements TreeModel {

    private final ProductManagerNode rootNode;
    private ProductNodeListener productNodeListener;
    private ProductManager productManager;
    private final EventListenerList treeModelListeners;

    public ProductTreeModel(final ProductManager manager) {
        this.productManager = manager;
        rootNode = new ProductManagerNode(productManager);
        productNodeListener = new ProductListener();
        productManager.addListener(new ProductManagerListener());
        treeModelListeners = new EventListenerList();
    }

    public ProductManager getProductManager() {
        return productManager;
    }

    @Override
    public ProductTreeNode getChild(Object parent, int index) {
        return ((ProductTreeNode) parent).getChildAt(index);
    }

    @Override
    public int getChildCount(Object parent) {
        return ((ProductTreeNode) parent).getChildCount();
    }


    @Override
    public int getIndexOfChild(Object parent, Object child) {
        return ((ProductTreeNode) parent).getIndex((ProductTreeNode) child);
    }

    @Override
    public void addTreeModelListener(TreeModelListener l) {
        treeModelListeners.add(TreeModelListener.class, l);
    }

    @Override
    public void removeTreeModelListener(TreeModelListener l) {
        treeModelListeners.remove(TreeModelListener.class, l);
    }

    @Override
    public ProductTreeNode getRoot() {
        return rootNode;
    }

    @Override
    public boolean isLeaf(Object node) {
        return ((ProductTreeNode) node).getChildCount() == 0;
    }

    @Override
    public void valueForPathChanged(TreePath path, Object newValue) {
        fireTreeNodeChanged(path);
    }

    protected void fireTreeNodeInserted(TreePath treePath) {
        TreeModelEvent event = new TreeModelEvent(this, treePath);
        TreeModelListener[] listeners = treeModelListeners.getListeners(TreeModelListener.class);
        for (TreeModelListener treeModelListener : listeners) {
            treeModelListener.treeNodesInserted(event);
        }
    }

    protected void fireTreeNodeRemoved(TreePath treePath) {
        TreeModelEvent event = new TreeModelEvent(this, treePath);
        TreeModelListener[] listeners = treeModelListeners.getListeners(TreeModelListener.class);
        for (TreeModelListener treeModelListener : listeners) {
            treeModelListener.treeNodesRemoved(event);
        }
    }

    protected void fireTreeNodeChanged(TreePath treePath) {
        TreeModelEvent event = new TreeModelEvent(this, treePath);
        TreeModelListener[] listeners = treeModelListeners.getListeners(TreeModelListener.class);
        for (TreeModelListener treeModelListener : listeners) {
            treeModelListener.treeNodesChanged(event);
        }
    }


    public TreePath getTreePath(Object nodeContent) {
        Enumeration enumeration = new TreeNodeEnumeration.Postorder(getRoot());
        while (enumeration.hasMoreElements()) {
            ProductTreeNode childNode = (ProductTreeNode) enumeration.nextElement();
            if (childNode.getContent() == nodeContent) {
                ProductTreeNode actualNode = childNode;
                List<ProductTreeNode> pathList = new ArrayList<ProductTreeNode>();
                while (actualNode != null) {
                    pathList.add(actualNode);
                    actualNode = actualNode.getParent();
                }
                Collections.reverse(pathList);
                return new TreePath(pathList.toArray());
            }
        }
        return null;
    }

    private class ProductManagerListener implements ProductManager.Listener {

        @Override
        public void productAdded(ProductManager.Event event) {
            Product product = event.getProduct();
            fireTreeNodeInserted(getTreePath(product));
            product.addProductNodeListener(productNodeListener);
        }

        @Override
        public void productRemoved(ProductManager.Event event) {
            Product product = event.getProduct();
            product.removeProductNodeListener(productNodeListener);
            fireTreeNodeRemoved(getTreePath(getProductManager()));
        }
    }

    private class ProductListener extends ProductNodeListenerAdapter {
        @Override
        public void nodeChanged(ProductNodeEvent event) {
            if (event.getSourceNode() instanceof VectorDataNode) {
                VectorDataNode vectorDataNode = (VectorDataNode) event.getSourceNode();
                if(vectorDataNode.isInternalNode()) {
                    TreePath path = getTreePath(event.getSourceNode());
                    if (path != null) {
                    fireTreeNodeChanged(path);
                    }
                }
            }
        }

        @Override
        public void nodeDataChanged(ProductNodeEvent event) {
            TreePath path = getTreePath(event.getSourceNode());
            if (path != null) {
                fireTreeNodeChanged(path);
            }
        }

        @Override
        public void nodeAdded(ProductNodeEvent event) {
            ProductNode content = event.getSourceNode();
            TreePath path = getTreePath(content);
            if (path != null) {
                fireTreeNodeInserted(path);
            }
        }

        @Override
        public void nodeRemoved(ProductNodeEvent event) {
            fireTreeNodeRemoved(getTreePath(event.getSourceNode().getProduct()));
        }
    }
}
