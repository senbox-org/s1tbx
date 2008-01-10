package org.esa.beam.framework.ui.application.support;

import org.esa.beam.framework.ui.application.Selection;

import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;

/**
 * A selection provider that wraps a {@link JTree}.
 * Elements contained in {@link Selection}s handled by this provider
 * represent currently selected tree node paths as {@code Object[]} values.
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
        final TreePath[] treePaths = tree.getSelectionPaths();
        final Selection selection;
        if (treePaths != null) {
            final Object[] elements = new Object[treePaths.length];
            for (int i = 0; i < treePaths.length; i++) {
                final Object[] path = treePaths[i].getPath();
                elements[i] = path;
            }
            selection = new DefaultSelection(elements);
        } else {
            selection = DefaultSelection.EMPTY;
        }
        return selection;
    }

    public void setSelection(Selection selection) {
        final Object[] elements = selection.getElements();
        if (elements.length > 0) {
            TreePath[] treePaths = new TreePath[elements.length];
            for (int i = 0; i < elements.length; i++) {
                final Object[] path = (Object[]) elements[i];
                treePaths[i] = new TreePath(path);
            }
            tree.setSelectionPaths(treePaths);
        } else {
            tree.clearSelection();
        }
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

    protected void handleTreeSelectionChanged(TreeSelectionEvent event) {
        fireSelectionChange(tree);
    }

    private class TreeSelectionHandler implements TreeSelectionListener {
        public void valueChanged(TreeSelectionEvent e) {
            handleTreeSelectionChanged(e);
        }
    }
}