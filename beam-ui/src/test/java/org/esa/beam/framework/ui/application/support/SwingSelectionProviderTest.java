package org.esa.beam.framework.ui.application.support;

import junit.framework.TestCase;
import org.esa.beam.framework.ui.application.SelectionChangeEvent;
import org.esa.beam.framework.ui.application.SelectionChangeListener;

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
        final ListSelectionProvider selectionProvider = new ListSelectionProvider(list);
        assertSame(list, selectionProvider.getList());
        assertSame(DefaultSelection.EMPTY, selectionProvider.getSelection());
        testListSelectionProvider(selectionProvider);

        final JList otherList = new JList(LIST_DATA);
        otherList.setSelectedIndices(new int[]{2, 0, 3});
        selectionProvider.setList(otherList);
        assertSame(otherList, selectionProvider.getList());
        assertEquals(new DefaultSelection(new Object[]{"Sauerkraut", "Äpfel", "Wacholderbeeren"}),
                     selectionProvider.getSelection());
        testListSelectionProvider(selectionProvider);
    }

    public void testTreeSelectionProvider() {
        final JTree tree = new JTree(TREE_DATA);
        final TreeSelectionProvider selectionProvider = new TreeSelectionProvider(tree);
        assertSame(tree, selectionProvider.getTree());
        assertSame(DefaultSelection.EMPTY, selectionProvider.getSelection());
        testTreeSelectionProvider(selectionProvider);

        final JTree otherTree = new JTree(TREE_DATA);
        otherTree.setSelectionPaths(new TreePath[]{
                new TreePath(new Object[]{TREE_GEMUESE, LEAF_SAUERKRAUT}),
                new TreePath(new Object[]{TREE_OBST, LEAF_AEPFEL}),
                new TreePath(new Object[]{TREE_OBST, LEAF_BEEREN}),
        });
        selectionProvider.setTree(otherTree);
        assertSame(otherTree, selectionProvider.getTree());
//        assertEquals(new DefaultSelection(new Object[]{"Sauerkraut", "Äpfel", "Wacholderbeeren"}),
//                     selectionProvider.getSelection());
        testTreeSelectionProvider(selectionProvider);
    }

    public void testTableSelectionProvider() {
        final JTable table = new JTable(TABLE_DATA, TABLE_CNAMES);
        final TableSelectionProvider selectionProvider = new TableSelectionProvider(table);
        assertSame(table, selectionProvider.getTable());
        assertSame(DefaultSelection.EMPTY, selectionProvider.getSelection());
        testTableSelectionProvider(selectionProvider);

        final JTable otherTable = new JTable(TABLE_DATA, TABLE_CNAMES);
        otherTable.setRowSelectionInterval(2, 2);
        otherTable.addRowSelectionInterval(0, 0);
        otherTable.addRowSelectionInterval(3, 3);
        selectionProvider.setTable(otherTable);
        assertSame(otherTable, selectionProvider.getTable());
        assertEquals(new DefaultSelection(new Object[]{0, 2, 3}),
                     selectionProvider.getSelection());
        testTableSelectionProvider(selectionProvider);
    }

    private static void testListSelectionProvider(ListSelectionProvider selectionProvider) {
        final SelectionChangeHandler listener = new SelectionChangeHandler();
        selectionProvider.addSelectionChangeListener(listener);

        DefaultSelection selection = new DefaultSelection(new Object[]{"Zwiebeln"});
        selectionProvider.setSelection(selection);
        assertNotSame(selection, selectionProvider.getSelection());
        assertEquals(selection, selectionProvider.getSelection());
        assertEquals("1;", listener.callSeq);

        selection = new DefaultSelection(new Object[]{"Zwiebeln", "Äpfel"});
        selectionProvider.setSelection(selection);
        assertNotSame(selection, selectionProvider.getSelection());
        assertEquals(selection, selectionProvider.getSelection());
        assertEquals("1;2;", listener.callSeq);

        selection = DefaultSelection.EMPTY;
        selectionProvider.setSelection(selection);
        assertSame(selection, selectionProvider.getSelection());
        assertEquals(selection, selectionProvider.getSelection());
        assertEquals("1;2;0;", listener.callSeq);

        selection = new DefaultSelection(LIST_DATA);
        selectionProvider.setSelection(selection);
        assertNotSame(selection, selectionProvider.getSelection());
        assertEquals(selection, selectionProvider.getSelection());
        assertEquals("1;2;0;4;", listener.callSeq);
    }

    private static void testTreeSelectionProvider(TreeSelectionProvider selectionProvider) {
        final SelectionChangeHandler listener = new SelectionChangeHandler();
        selectionProvider.addSelectionChangeListener(listener);

        DefaultSelection selection = new DefaultSelection(new Object[]{
                new Object[]{TREE_GEMUESE, LEAF_ZWIEBELN}});
        selectionProvider.setSelection(selection);
        assertNotSame(selection, selectionProvider.getSelection());
        //assertEquals(selection, selectionProvider.getSelection());
        assertEquals("1;", listener.callSeq);

        selection = new DefaultSelection(new Object[]{
                new Object[]{TREE_OBST, LEAF_AEPFEL},
                new Object[]{TREE_GEMUESE, LEAF_ZWIEBELN}});
        selectionProvider.setSelection(selection);
        assertNotSame(selection, selectionProvider.getSelection());
        //assertEquals(selection, selectionProvider.getSelection());
        assertEquals("1;2;", listener.callSeq);

        selection = DefaultSelection.EMPTY;
        selectionProvider.setSelection(selection);
        assertSame(selection, selectionProvider.getSelection());
        assertEquals(selection, selectionProvider.getSelection());
        assertEquals("1;2;0;", listener.callSeq);

        selection = new DefaultSelection(new Object[]{
                new Object[]{TREE_OBST, LEAF_AEPFEL},
                new Object[]{TREE_OBST, LEAF_BEEREN},
                new Object[]{TREE_GEMUESE, LEAF_SAUERKRAUT},
                new Object[]{TREE_GEMUESE, LEAF_ZWIEBELN}});
        selectionProvider.setSelection(selection);
        assertNotSame(selection, selectionProvider.getSelection());
        //assertEquals(selection, selectionProvider.getSelection());
        assertEquals("1;2;0;4;", listener.callSeq);
    }

    private static void testTableSelectionProvider(TableSelectionProvider selectionProvider) {
        final SelectionChangeHandler listener = new SelectionChangeHandler();
        selectionProvider.addSelectionChangeListener(listener);

        DefaultSelection selection = new DefaultSelection(new Object[]{1});
        selectionProvider.setSelection(selection);
        assertNotSame(selection, selectionProvider.getSelection());
        assertEquals(selection, selectionProvider.getSelection());
        assertEquals("1;", listener.callSeq);

        selection = new DefaultSelection(new Object[]{1, 2});
        selectionProvider.setSelection(selection);
        assertNotSame(selection, selectionProvider.getSelection());
        assertEquals(selection, selectionProvider.getSelection());
        assertEquals("1;2;", listener.callSeq);

        selection = DefaultSelection.EMPTY;
        selectionProvider.setSelection(selection);
        assertSame(selection, selectionProvider.getSelection());
        assertEquals(selection, selectionProvider.getSelection());
        assertEquals("1;2;0;", listener.callSeq);

        selection = new DefaultSelection(new Object[]{0, 1, 2, 3});
        selectionProvider.setSelection(selection);
        assertNotSame(selection, selectionProvider.getSelection());
        assertEquals(selection, selectionProvider.getSelection());
        assertEquals("1;2;0;4;", listener.callSeq);
    }

    private static class SelectionChangeHandler implements SelectionChangeListener {
        String callSeq = "";

        public void selectionChanged(SelectionChangeEvent event) {
            callSeq += event.getSelection().getElements().length + ";";
        }
    }
}
