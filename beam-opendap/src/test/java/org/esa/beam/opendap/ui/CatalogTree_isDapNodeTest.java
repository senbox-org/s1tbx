package org.esa.beam.opendap.ui;

import org.esa.beam.opendap.datamodel.OpendapLeaf;
import org.junit.*;
import thredds.catalog.InvDataset;

import javax.swing.tree.DefaultMutableTreeNode;

import static org.junit.Assert.*;

public class CatalogTree_isDapNodeTest {

    @Test
    public void testThatNullIsResolvedToFalse() {
        final Object noDapNode = null;
        assertEquals(false, CatalogTree.isDapNode(noDapNode));
    }

    @Test
    public void testThatUserObjectWhichIsNoOpendapLeafIsResolvedToFalse() {
        final Integer userObject = 4;
        final DefaultMutableTreeNode noDapNode = new DefaultMutableTreeNode(userObject);
        assertEquals(false, CatalogTree.isDapNode(noDapNode));
    }

    @Test
    public void testThatOpendapLeafWhichHasNoDapServiceSetIsResolvedToFalse() {
        final OpendapLeaf userObject = new OpendapLeaf("name", new InvDataset(null, "") {
                });
        userObject.setDapAccess(false);
        final DefaultMutableTreeNode noDapNode = new DefaultMutableTreeNode(userObject);
        assertEquals(false, CatalogTree.isDapNode(noDapNode));
    }

    @Test
    public void testThatOpendapLeafWhichHasDapServiceSetIsResolvedToTrue() {
        final OpendapLeaf userObject = new OpendapLeaf("name", new InvDataset(null, "") {
                });
        userObject.setDapAccess(true);
        final DefaultMutableTreeNode notADapNode = new DefaultMutableTreeNode(userObject);
        assertEquals(true, CatalogTree.isDapNode(notADapNode));
    }
}
