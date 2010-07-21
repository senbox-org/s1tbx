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

package com.bc.ceres.swing.selection.support;

import com.bc.ceres.swing.selection.Selection;
import com.bc.ceres.swing.selection.SelectionChangeEvent;
import com.bc.ceres.swing.selection.SelectionChangeListener;
import junit.framework.TestCase;

import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;


public class SwingSelectionProviderTest extends TestCase {

    private static final Object[] LIST_DATA = new Object[]{
            "Sauerkraut", "Zwiebeln", "Äpfel", "Wacholderbeeren"
    };

    private static final TreeNode[] TREE_DATA;

    private static final Object[][] TABLE_DATA = new Object[][]{
            {4, 'A', "Sauerkraut"},
            {1, 'B', "Zwiebeln"},
            {6, 'C', "Äpfel"},
            {2, 'D', "Wacholderbeeren"}
    };
    private static final Object[][] TABLE_CNAMES = new Object[][]{
            {"C1", "C2", "C3"}
    };
    private static final DefaultMutableTreeNode TREE_GEMUESE = new DefaultMutableTreeNode("Gemüse");

    private static final DefaultMutableTreeNode TREE_OBST = new DefaultMutableTreeNode("Obst");

    private static final DefaultMutableTreeNode LEAF_SAUERKRAUT = new DefaultMutableTreeNode("Sauerkraut");

    private static final DefaultMutableTreeNode LEAF_ZWIEBELN = new DefaultMutableTreeNode("Zwiebeln");

    private static final DefaultMutableTreeNode LEAF_AEPFEL = new DefaultMutableTreeNode("Äpfel");

    private static final DefaultMutableTreeNode LEAF_BEEREN = new DefaultMutableTreeNode("Wacholderbeeren");

    static {

        TREE_GEMUESE.add(LEAF_SAUERKRAUT);
        TREE_GEMUESE.add(LEAF_ZWIEBELN);

        TREE_OBST.add(LEAF_AEPFEL);
        TREE_OBST.add(LEAF_BEEREN);

        TREE_DATA = new TreeNode[]{
                TREE_GEMUESE,
                TREE_OBST,
        };
    }

    public void testListSelectionProvider() {
        final JList list = new JList(LIST_DATA);
        final ListSelectionContext selectionContext = new ListSelectionContext(list);
        assertSame(list, selectionContext.getList());
        assertSame(DefaultSelection.EMPTY, selectionContext.getSelection());
        testListSelectionProvider(selectionContext);

        final JList otherList = new JList(LIST_DATA);
        otherList.setSelectedIndices(new int[]{2, 0, 3});
        selectionContext.setList(otherList);
        assertSame(otherList, selectionContext.getList());
        assertEquals(new DefaultSelection<Object>("Sauerkraut", "Äpfel", "Wacholderbeeren"),
                     selectionContext.getSelection());
        testListSelectionProvider(selectionContext);
    }

    public void testTreeSelectionProvider() {
        final JTree tree = new JTree(TREE_DATA);
        final TreeSelectionContext selectionContext = new TreeSelectionContext(tree);
        assertSame(tree, selectionContext.getTree());
        assertSame(DefaultSelection.EMPTY, selectionContext.getSelection());
        testTreeSelectionProvider(selectionContext);

        final JTree otherTree = new JTree(TREE_DATA);
        otherTree.setSelectionPaths(new TreePath[]{
                new TreePath(new Object[]{TREE_GEMUESE, LEAF_SAUERKRAUT}),
                new TreePath(new Object[]{TREE_OBST, LEAF_AEPFEL}),
                new TreePath(new Object[]{TREE_OBST, LEAF_BEEREN}),
        });
        selectionContext.setTree(otherTree);
        assertSame(otherTree, selectionContext.getTree());
//        assertEquals(new DefaultSelection(new Object[]{"Sauerkraut", "Äpfel", "Wacholderbeeren"}),
//                     selectionContext.getSelection());
        testTreeSelectionProvider(selectionContext);
    }

    public void testTableSelectionProvider() {
        final JTable table = new JTable(TABLE_DATA, TABLE_CNAMES);
        final TableSelectionContext selectionContext = new TableSelectionContext(table);
        assertSame(table, selectionContext.getTable());
        assertSame(DefaultSelection.EMPTY, selectionContext.getSelection());
        testTableSelectionProvider(selectionContext);

        final JTable otherTable = new JTable(TABLE_DATA, TABLE_CNAMES);
        otherTable.setRowSelectionInterval(2, 2);
        otherTable.addRowSelectionInterval(0, 0);
        otherTable.addRowSelectionInterval(3, 3);
        selectionContext.setTable(otherTable);
        assertSame(otherTable, selectionContext.getTable());
        assertEquals(new DefaultSelection<Object>(new Object[]{0, 2, 3}),
                     selectionContext.getSelection());
        testTableSelectionProvider(selectionContext);
    }

    private static void testListSelectionProvider(ListSelectionContext selectionContext) {
        final SelectionChangeHandler listener = new SelectionChangeHandler();
        selectionContext.addSelectionChangeListener(listener);

        Selection selection = new DefaultSelection<String>("Zwiebeln");
        selectionContext.setSelection(selection);
        assertNotSame(selection, selectionContext.getSelection());
        assertEquals(selection, selectionContext.getSelection());
        assertEquals("1;", listener.callSeq);

        selection = new DefaultSelection<String>("Zwiebeln", "Äpfel");
        selectionContext.setSelection(selection);
        assertNotSame(selection, selectionContext.getSelection());
        assertEquals(selection, selectionContext.getSelection());
        assertEquals("1;2;", listener.callSeq);

        selection = DefaultSelection.EMPTY;
        selectionContext.setSelection(selection);
        assertSame(selection, selectionContext.getSelection());
        assertEquals(selection, selectionContext.getSelection());
        assertEquals("1;2;0;", listener.callSeq);

        selection = new DefaultSelection<Object>(LIST_DATA);
        selectionContext.setSelection(selection);
        assertNotSame(selection, selectionContext.getSelection());
        assertEquals(selection, selectionContext.getSelection());
        assertEquals("1;2;0;4;", listener.callSeq);
    }

    private static void testTreeSelectionProvider(TreeSelectionContext selectionContext) {
        final SelectionChangeHandler listener = new SelectionChangeHandler();
        selectionContext.addSelectionChangeListener(listener);

        Selection selection = new DefaultSelection<TreePath>(new TreePath(new Object[]{TREE_GEMUESE, LEAF_ZWIEBELN}));
        selectionContext.setSelection(selection);
        assertNotSame(selection, selectionContext.getSelection());
        //assertEquals(selection, selectionContext.getSelection());
        assertEquals("1;", listener.callSeq);

        selection = new DefaultSelection<TreePath>(
                new TreePath(new Object[]{TREE_OBST, LEAF_AEPFEL}),
                new TreePath(new Object[]{TREE_GEMUESE, LEAF_ZWIEBELN}));
        selectionContext.setSelection(selection);
        assertNotSame(selection, selectionContext.getSelection());
        //assertEquals(selection, selectionContext.getSelection());
        assertEquals("1;2;", listener.callSeq);

        selection = DefaultSelection.EMPTY;
        selectionContext.setSelection(selection);
        assertSame(selection, selectionContext.getSelection());
        assertEquals(selection, selectionContext.getSelection());
        assertEquals("1;2;0;", listener.callSeq);

        selection = new DefaultSelection<TreePath>(
                new TreePath(new Object[]{TREE_OBST, LEAF_AEPFEL}),
                new TreePath(new Object[]{TREE_OBST, LEAF_BEEREN}),
                new TreePath(new Object[]{TREE_GEMUESE, LEAF_SAUERKRAUT}),
                new TreePath(new Object[]{TREE_GEMUESE, LEAF_ZWIEBELN}));
        selectionContext.setSelection(selection);
        assertNotSame(selection, selectionContext.getSelection());
        //assertEquals(selection, selectionContext.getSelection());
        assertEquals("1;2;0;4;", listener.callSeq);
    }

    private static void testTableSelectionProvider(TableSelectionContext selectionContext) {
        final SelectionChangeHandler listener = new SelectionChangeHandler();
        selectionContext.addSelectionChangeListener(listener);

        Selection selection = new DefaultSelection<Integer>(1);
        selectionContext.setSelection(selection);
        assertNotSame(selection, selectionContext.getSelection());
        assertEquals(selection, selectionContext.getSelection());
        assertEquals("1;", listener.callSeq);

        selection = new DefaultSelection<Integer>(1, 2);
        selectionContext.setSelection(selection);
        assertNotSame(selection, selectionContext.getSelection());
        assertEquals(selection, selectionContext.getSelection());
        assertEquals("1;2;", listener.callSeq);

        selection = DefaultSelection.EMPTY;
        selectionContext.setSelection(selection);
        assertSame(selection, selectionContext.getSelection());
        assertEquals(selection, selectionContext.getSelection());
        assertEquals("1;2;0;", listener.callSeq);

        selection = new DefaultSelection<Integer>(0, 1, 2, 3);
        selectionContext.setSelection(selection);
        assertNotSame(selection, selectionContext.getSelection());
        assertEquals(selection, selectionContext.getSelection());
        assertEquals("1;2;0;4;", listener.callSeq);
    }

    private static class SelectionChangeHandler implements SelectionChangeListener {

        private String callSeq = "";

        @Override
        public void selectionChanged(SelectionChangeEvent event) {
            callSeq += event.getSelection().getSelectedValues().length + ";";
        }

        @Override
        public void selectionContextChanged(SelectionChangeEvent event) {
        }
    }
}
