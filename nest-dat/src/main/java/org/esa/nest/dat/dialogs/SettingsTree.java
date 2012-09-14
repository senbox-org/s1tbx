/*
 * Copyright (C) 2012 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.nest.dat.dialogs;

import org.esa.beam.framework.ui.PopupMenuFactory;
import org.esa.beam.framework.ui.PopupMenuHandler;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.nest.util.Settings;
import org.jdom.Attribute;
import org.jdom.Element;

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.Enumeration;
import java.util.List;

/**
 * A tree-view component for Settings
 */
class SettingsTree extends JTree implements PopupMenuFactory, ActionListener {

    private Object menuContext;
    private DefaultMutableTreeNode selectedNode;
    private TreePath selectedPath;

    /**
     * Constructs a new Tree.
     */
    public SettingsTree() {
        this(false);
    }

    /**
     * Constructs a new Tree with the given selection mode.
     *
     * @param multipleSelect whether or not the tree is multiple selection capable
     */
    public SettingsTree(final boolean multipleSelect) {

        getSelectionModel().setSelectionMode(multipleSelect
                ? TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION
                : TreeSelectionModel.SINGLE_TREE_SELECTION);
        setCellRenderer(new PTCellRenderer());
        setRootVisible(true);
        setShowsRootHandles(true);
        setToggleClickCount(2);
        setExpandsSelectedPaths(true);
        setScrollsOnExpand(true);
        setAutoscrolls(true);
        putClientProperty("JTree.lineStyle", "Angled");
        ToolTipManager.sharedInstance().registerComponent(this);

        final PopupMenuHandler popupMenuHandler = new PopupMenuHandler(this);
        addMouseListener(popupMenuHandler);
        addKeyListener(popupMenuHandler);
    }

    public JPopupMenu createPopupMenu(final Component component) {
        return null;
    }

    public JPopupMenu createPopupMenu(MouseEvent event) {
        selectedPath = getPathForLocation(event.getX(), event.getY());
        if (selectedPath != null) {
            setSelectionPath(selectedPath);
            selectedNode = (DefaultMutableTreeNode) getLastSelectedPathComponent();
            if (selectedNode != null) {
                Object context = selectedNode.getUserObject();
                if (context != null) {
                    return createPopup(context);
                }
            }
        }
        return null;
    }

    JPopupMenu createPopup(final Object context) {

        final JPopupMenu popup = new JPopupMenu();
        menuContext = context;

        
        return popup;
    }

    private JMenuItem createMenuItem(final JPopupMenu popup, final String text) {
        final JMenuItem menuItem;
        menuItem = new JMenuItem(text);
        menuItem.addActionListener(this);
        popup.add(menuItem);
        return menuItem;
    }

    private static void addSeparator(JPopupMenu popup) {
        if (popup.getComponentCount() > 0) {
            popup.addSeparator();
        }
    }

    /**
     * Invoked when an action occurs.
     */
    public void actionPerformed(ActionEvent e) {

        final DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) selectedNode.getParent();


    }

    void expandAll() {
        final TreeNode root = (TreeNode) getModel().getRoot();
        expandAll(this, new TreePath(root), true);
    }

    /**
     * If expand is true, expands all nodes in the tree.
     * Otherwise, collapses all nodes in the tree.
     *
     * @param tree the tree
     * @param parent the parent path
     * @param expand or collapse
     */
    private static void expandAll(JTree tree, TreePath parent, boolean expand) {
        // Traverse children
        final TreeNode node = (TreeNode) parent.getLastPathComponent();
        if (node.getChildCount() >= 0) {
            for (Enumeration e = node.children(); e.hasMoreElements();) {
                final TreeNode n = (TreeNode) e.nextElement();
                final TreePath path = parent.pathByAddingChild(n);
                expandAll(tree, path, expand);
            }
        }

        // Expansion or collapse must be done bottom-up
        if (expand) {
            tree.expandPath(parent);
        } else {
            tree.collapsePath(parent);
        }
    }


    public void populateTree(Element elem, DefaultMutableTreeNode treeNode) {
        populateNode(elem, treeNode);
        setModel(new DefaultTreeModel(treeNode));
        expandAll();
    }

    private static void populateNode(Element elem, DefaultMutableTreeNode treeNode) {

        final List children = elem.getContent();
        for (Object aChild : children) {
            if (aChild instanceof Element) {
                final Element child = (Element) aChild;
                final DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(child);
                treeNode.add(newNode);

                final List grandChildren = child.getChildren();
                if(!grandChildren.isEmpty()) {
                    populateNode(child, newNode);
                }
            }
        }
    }

    /**
     * Selects the specified object in this tree's model. If the given object has no representation in the tree, the
     * current selection will not be changed.
     *
     * @param toSelect the object whose representation in the tree will be selected.
     */
    public void select(Object toSelect) {
        final TreePath path = findTreePathFor(toSelect);
        if (path != null) {
            setSelectionPath(path);
        }
    }

    private TreePath findTreePathFor(final Object o) {
        final DefaultMutableTreeNode rootNode = (DefaultMutableTreeNode) getModel().getRoot();
        final Enumeration enumeration = rootNode.depthFirstEnumeration();
        while (enumeration.hasMoreElements()) {
            final DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) enumeration.nextElement();
            if (treeNode.getUserObject() == o) {
                return new TreePath(treeNode.getPath());
            }
        }
        return null;
    }

    private static class PTCellRenderer extends DefaultTreeCellRenderer {

        private final ImageIcon prefIcon;

        public PTCellRenderer() {
            prefIcon = UIUtils.loadImageIcon("icons/Properties16.gif");
        }

        @Override
        public Component getTreeCellRendererComponent(JTree tree,
                                                      Object value,
                                                      boolean sel,
                                                      boolean expanded,
                                                      boolean leaf,
                                                      int row,
                                                      boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

            final DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) value;
            final Object userObject = treeNode.getUserObject();

            if (userObject instanceof Element) {
                final Element elem = (Element) userObject;
                final Attribute label = elem.getAttribute(Settings.LABEL);
                if (label != null) {
                    this.setText(label.getValue());
                } else {
                    this.setText(elem.getName());
                }

             /*   final Attribute elemValue = elem.getAttribute(Settings.VALUE);
                if(elemValue != null) {
                    this.setIcon(prefIcon);
                    this.setToolTipText(elemValue.getValue());     
                } else {
                    this.setToolTipText(elem.getName());
                }      */
            }

            return this;
        }
    }

}