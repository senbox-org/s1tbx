package org.esa.beam.visat.toolviews.layermanager;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

/**
 * <p>Transfer handler implementation that supports to move a selected tree
 * node within a <code>JTree</code>.</p>
 */
class NodeMoveTransferHandler extends TransferHandler {

    private TreePath[] movedPaths;

    /**
     * create a transferable that contains all paths that are currently selected in
     * a given tree
     *
     * @return all selected paths in the given tree
     *         (or null if the given component is not a tree)
     * @see javax.swing.TransferHandler#createTransferable(javax.swing.JComponent)
     */
    @Override
    protected Transferable createTransferable(JComponent c) {
        Transferable t = null;

        if (c instanceof JTree) {
            final JTree tree = (JTree) c;

            movedPaths = tree.getSelectionPaths();
            t = new GenericTransferable(movedPaths);
        }

        return t;
    }

    @Override
    public boolean importData(TransferSupport support) {
        if (support.getComponent() instanceof JTree) {
            final JTree tree = (JTree) support.getComponent();
            final DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
            final Point dropPoint = support.getDropLocation().getDropPoint();
            final TreePath currentPath = tree.getClosestPathForLocation(dropPoint.x, dropPoint.y);

            // System.out.println("currentPath = " + currentPath);

            if (currentPath != null) {
                addNodes(currentPath, model);
                tree.setSelectionPath(tree.getClosestPathForLocation(dropPoint.x, dropPoint.y));

                return true;
            }
        }

        return false;
    }

    @Override
    public boolean canImport(TransferSupport support) {
        final Component component = support.getComponent();

        if (component instanceof JTree) {
            final JTree tree = (JTree) component;
            final Point dropPoint = support.getDropLocation().getDropPoint();
            final TreePath targetPath = tree.getClosestPathForLocation(dropPoint.x, dropPoint.y);
            final MutableTreeNode targetNode = (MutableTreeNode) targetPath.getLastPathComponent();

            // todo - can used the transferable from the support object.
            // todo - this returns just a proxy object which can't be used to get the nodes
            for (final TreePath movedPath : movedPaths) {
                final MutableTreeNode movedNode = (MutableTreeNode) movedPath.getLastPathComponent();
                if (!movedNode.getParent().equals(targetNode.getParent())) {
                    return false;
                }
            }

            return true;
        }

        return false;
    }

    /**
     * add a number of given nodes
     *
     * @param currentPath the tree path currently selected
     * @param model       tree model containing the nodes
     */
    private void addNodes(TreePath currentPath, DefaultTreeModel model) {
        final MutableTreeNode targetNode = (MutableTreeNode) currentPath.getLastPathComponent();

        for (final TreePath movedPath : movedPaths) {
            final MutableTreeNode movedNode = (MutableTreeNode) movedPath.getLastPathComponent();

            if (!movedNode.equals(targetNode)) {
                // todo - implement swapping of nodes (rq)
                model.removeNodeFromParent(movedNode);
                final MutableTreeNode parent = (MutableTreeNode) targetNode.getParent();
                final int index = parent.getIndex(targetNode);
                model.insertNodeInto(movedNode, parent, index);
            }
        }
    }

    /**
     * Returns the type of transfer actions supported by the source.
     * This transfer handler supports moving of tree nodes so it returns MOVE.
     *
     * @return TransferHandler.MOVE
     */
    @Override
    public int getSourceActions(JComponent c) {
        return MOVE;
    }

    /**
     * GenericTransferable.java
     * <p/>
     * <p>This transferable takes an object as data that is to be transferred. It
     * uses DataFlavor.stringFlavor, which is supported by all objects. This transferable
     * can be used in cases where a special handling in terms of which data flavors are
     * acceptable or which data is transported do not matter.</p>
     *
     * @author Ulrich Hilger
     * @author Light Development
     * @author <a href="http://www.lightdev.com">http://www.lightdev.com</a>
     * @author <a href="mailto:info@lightdev.com">info@lightdev.com</a>
     * @author published under the terms and conditions of the
     *         GNU General Public License,
     *         for details see file gpl.txt in the distribution
     *         package of this software
     * @version 1, 30.07.2005
     */
    private static class GenericTransferable implements Transferable {

        /**
         * the data this transferable transports
         */
        private Object data;


        /**
         * construct a transferabe with a given object to transfer
         *
         * @param data the data object to transfer
         */
        public GenericTransferable(Object data) {
            this.data = data;
        }

        /**
         * get the data flavors supported by this object
         *
         * @return an array of supported data flavors
         */
        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[0];
        }

        /**
         * determine whether or not a given data flavor is supported by this transferable
         *
         * @return true, if the given data flavor is supported
         */
        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return false;
        }

        /**
         * get the data this transferable transports
         *
         * @return the data transported by this transferable
         */
        @Override
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
            return data;
        }
    }
}
