package org.esa.beam.framework.ui.application.support;

import org.esa.beam.framework.ui.application.support.DefaultSelection;
import org.esa.beam.framework.ui.application.Selection;

import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeModel;

/**
 *
 */
public class TreeSelectionProvider extends AbstractSelectionProvider {

    private final TreeSelectionListener treeSelectionListener;
    private JTree tree;

    public TreeSelectionProvider(final JTree tree) {
        treeSelectionListener = new TreeSelectionHandler();
        this.tree = tree;
        this.tree.addTreeSelectionListener(treeSelectionListener);
    }

    public Selection getSelection() {
        TreePath[] treePaths = tree.getSelectionPaths();
        final Selection selection;
        if (treePaths != null) {
            Object[] elements = new Object[treePaths.length];
            for (int i = 0; i < treePaths.length; i++) {
                TreePath treePath = treePaths[i];
                elements[i] = treePath.getLastPathComponent();
            }
            selection = new DefaultSelection(elements);
        } else {
            selection = DefaultSelection.EMPTY;
        }
        return selection;
    }

    public void setSelection(Selection selection) {
        final Object[] elements = selection.getElements();
        if (elements.length == 0) {
            tree.clearSelection();
            return;
        }
        // todo
    }

    public JTree getTree() {
        return tree;
    }

    public void setTree(JTree tree) {
        if (tree != this.tree) {
            this.tree.removeTreeSelectionListener(treeSelectionListener);
            this.tree = tree;
            this.tree.addTreeSelectionListener(treeSelectionListener);
            fireSelectionChange(this.tree);
        }
    }

    private class TreeSelectionHandler implements TreeSelectionListener {
        public void valueChanged(TreeSelectionEvent e) {
            fireSelectionChange(tree);
        }
    }
}