package org.esa.beam.opendap.ui;

import org.esa.beam.framework.gpf.ui.DefaultAppContext;
import org.esa.beam.opendap.HeadlessTestRunner;
import org.esa.beam.opendap.datamodel.CatalogNode;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import thredds.catalog.InvAccessImpl;
import thredds.catalog.InvCatalogImpl;
import thredds.catalog.InvCatalogRef;
import thredds.catalog.InvDatasetImpl;
import thredds.catalog.InvService;
import ucar.nc2.constants.FeatureType;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.net.URI;
import java.net.URISyntaxException;

import static org.junit.Assert.*;

@RunWith(HeadlessTestRunner.class)
public class CatalogTree_appendNodeUnitTest {

    private DefaultMutableTreeNode parentNode;

    @Before
    public void setUp() throws Exception {
        parentNode = new DefaultMutableTreeNode();
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testAppendAn_OPeNDAP_Node() throws URISyntaxException {
        // preparation
        final String serviceType = "OPENDAP";
        final InvDatasetImpl dapDataset = createDataset(new String[]{serviceType});

        // execution
        new CatalogTree(null, new DefaultAppContext(""), null).appendLeafNode(parentNode, getDefaultTreeModel(), dapDataset);

        // verification
        testThatChildIsOnlyDapNodeWithoutFileAccess(parentNode);
    }

    @Test
    public void testThatAppendDatasetWithFileAccessOnlyIsResolvedToNodeWithFileAccess() throws URISyntaxException {
        // preparation
        final String serviceName = "FILE";
        final InvDatasetImpl dapDataset = createDataset(new String[]{serviceName});

        // execution
        new CatalogTree(null, new DefaultAppContext(""), null).appendLeafNode(parentNode, getDefaultTreeModel(), dapDataset);

        // verification
        testThatChildIsNodeWithFileAccess(parentNode);
    }

    @Test
    public void testAppendDatasetWhichHasDapAccessAndAlsoFileAccess() throws URISyntaxException {
        // preparation
        final String dapServiceName = "OPENDAP";
        final String fileServiceName = "FILE";
        final InvDatasetImpl dapDataset = createDataset(new String[]{fileServiceName, dapServiceName});

        // execution
        new CatalogTree(null, new DefaultAppContext(""), null).appendLeafNode(parentNode, getDefaultTreeModel(), dapDataset);

        // verification
        testThatChildIsDapNodeWhichHasFileAccessToo(parentNode);
    }

    @Test
    public void testAppendCatalogNodeToParent() throws URISyntaxException {
        //preparation
        final URI whatever = null;
        final InvCatalogRef catalogReference = new InvCatalogRef(null, "catalogRefName", "http://a.b");
        catalogReference.setCatalog(new InvCatalogImpl("whatever", "1.0", whatever));

        //execution
        CatalogTreeUtils.appendCatalogNode(parentNode, getDefaultTreeModel(), catalogReference);

        //verification
        assertEquals(1, parentNode.getChildCount());
        assertEquals(1, parentNode.getChildAt(0).getChildCount());

        final DefaultMutableTreeNode child1 = (DefaultMutableTreeNode) parentNode.getChildAt(0);
        assertEquals(true, child1.getUserObject() instanceof String);
        assertEquals("catalogRefName", child1.getUserObject());

        final DefaultMutableTreeNode child2 = (DefaultMutableTreeNode) parentNode.getChildAt(0).getChildAt(0);
        assertEquals(true, child2.getUserObject() instanceof CatalogNode);
        final CatalogNode catalogNode = (CatalogNode) child2.getUserObject();
        assertEquals("http://a.b", catalogNode.getCatalogUri());
    }

    private void testThatChildIsNodeWithFileAccess(DefaultMutableTreeNode parentNode) {
        assertEquals(1, parentNode.getChildCount());
        assertEquals(true, parentNode.getChildAt(0).isLeaf());
        assertEquals(false, CatalogTreeUtils.isDapNode(parentNode.getChildAt(0)));
        assertEquals(true, CatalogTreeUtils.isFileNode(parentNode.getChildAt(0)));
    }

    private void testThatChildIsOnlyDapNodeWithoutFileAccess(DefaultMutableTreeNode parentNode) {
        testThatChildIsDapNode(parentNode);
        assertEquals(false, CatalogTreeUtils.isFileNode(parentNode.getChildAt(0)));
    }

    private void testThatChildIsDapNodeWhichHasFileAccessToo(DefaultMutableTreeNode parentNode) {
        testThatChildIsDapNode(parentNode);
        assertEquals(true, CatalogTreeUtils.isFileNode(parentNode.getChildAt(0)));
    }

    private void testThatChildIsDapNode(DefaultMutableTreeNode parentNode) {
        assertEquals(1, parentNode.getChildCount());
        assertEquals(true, parentNode.getChildAt(0).isLeaf());
        assertEquals(true, CatalogTreeUtils.isDapNode(parentNode.getChildAt(0)));
    }

    private InvDatasetImpl createDataset(String[] serviceTypeNames) throws URISyntaxException {
        final InvDatasetImpl dapDataset = new InvDatasetImpl(null, "datasetName", FeatureType.NONE, serviceTypeNames[0], "http://wherever.you.want.bc");

        final InvCatalogImpl catalog = new InvCatalogImpl("catalogName", "1.0", new URI("http://x.y"));
        dapDataset.setCatalog(catalog);

        for (String serviceName : serviceTypeNames) {
            final InvService dapService = new InvService(serviceName, serviceName, "irrelevant", "irrelevant", "irrelevant");
            final InvAccessImpl invAccess = new InvAccessImpl(dapDataset, "http://y.z", dapService);
            dapDataset.addAccess(invAccess);
        }

        dapDataset.finish();
        return dapDataset;
    }

    private DefaultTreeModel getDefaultTreeModel() {
        return (DefaultTreeModel) new JTree().getModel();
    }
}