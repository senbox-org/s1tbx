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

package com.bc.ceres.swing;

import junit.framework.TestCase;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import java.awt.event.MouseListener;

public class TreeCellExtenderTest extends TestCase {

    public void testActiveState() {
        JTree tree = new JTree();
        TreeCellExtender extender = new TreeCellExtender(tree);
        assertSame(tree, extender.getTree());
        assertEquals(false, extender.isActive());

        MouseListener[] listeners = tree.getMouseListeners();
        assertNotNull(listeners);
        int oldLength = listeners.length;

        extender.setActive(true);
        assertEquals(true, extender.isActive());

        listeners = tree.getMouseListeners();
        int newLength = listeners.length;
        assertEquals(oldLength + 1, newLength);


    }

    public void testEquip() {
        JTree tree = new JTree();
        TreeCellExtender extender = TreeCellExtender.equip(tree);
        assertNotNull(extender);
        assertSame(tree, extender.getTree());
        assertEquals(true, extender.isActive());
    }

    public static void main(String[] args) {
        final JFrame frame = new JFrame(TreeCellExtender.class.getSimpleName());

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        final JTree tree = new JTree(getDefaultTreeModel());
        TreeCellExtender.equip(tree);

        frame.getContentPane().add(new JScrollPane(tree));
        frame.setSize(200, 200);
        frame.setVisible(true);
    }

    /**
     * Creates and returns a sample <code>TreeModel</code>.
     * Used primarily for beanbuilders to show something interesting.
     *
     * @return the default <code>TreeModel</code>
     */
    protected static TreeModel getDefaultTreeModel() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("JTree");
        DefaultMutableTreeNode parent;

        parent = new DefaultMutableTreeNode("colors colors colors colors colors");
        root.add(parent);
        parent.add(new DefaultMutableTreeNode("blue"));
        parent.add(new DefaultMutableTreeNode("violet"));
        parent.add(new DefaultMutableTreeNode("red"));
        parent.add(new DefaultMutableTreeNode("yellow"));

        parent = new DefaultMutableTreeNode("sports sports sports sports sports sports");
        root.add(parent);
        parent.add(new DefaultMutableTreeNode("basketball"));
        parent.add(new DefaultMutableTreeNode("soccer"));
        parent.add(new DefaultMutableTreeNode("football football football football football football"));
        parent.add(new DefaultMutableTreeNode("hockey"));

        parent = new DefaultMutableTreeNode("food");
        root.add(parent);
        parent.add(new DefaultMutableTreeNode("hot dogs"));
        parent.add(new DefaultMutableTreeNode("pizza"));
        parent.add(new DefaultMutableTreeNode("ravioli ravioli ravioli ravioli ravioli ravioli ravioli ravioli ravioli ravioli ravioli ravioli"));
        parent.add(new DefaultMutableTreeNode("bananas"));
        return new DefaultTreeModel(root);
    }

}
