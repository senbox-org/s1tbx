package org.esa.beam.opendap.ui;

import org.esa.beam.framework.gpf.ui.DefaultAppContext;
import org.esa.beam.opendap.HeadlessTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import thredds.catalog.InvAccessImpl;
import thredds.catalog.InvCatalogImpl;
import thredds.catalog.InvDataset;
import thredds.catalog.InvDatasetImpl;
import thredds.catalog.InvService;
import ucar.nc2.constants.FeatureType;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(HeadlessTestRunner.class)
public class CatalogTree_setNewRootDatasetsTest {

    private List<InvDataset> datasets;
    private InvCatalogImpl catalog;
    private CatalogTree catalogTree;

    @Before
    public void setUp() throws Exception {
        datasets = new ArrayList<InvDataset>();
        catalog = new InvCatalogImpl("catalogName", "1.0", new URI("http://x.y"));
        InvDatasetImpl dapDataset = createDataset(catalog, "first", "OPENDAP");
        datasets.add(dapDataset);
        catalogTree = new CatalogTree(null, new DefaultAppContext(""), null);
    }

    @Test
    public void testAddingDapDataset() {
        //execution
        catalogTree.setNewRootDatasets(datasets);

        //verification
        assertEquals(true, ((JTree) catalogTree.getComponent()).getModel().getRoot() instanceof DefaultMutableTreeNode);
        final DefaultMutableTreeNode root = (DefaultMutableTreeNode) ((JTree) catalogTree.getComponent()).getModel().getRoot();
        assertEquals(1, root.getChildCount());
        assertEquals(true, CatalogTreeUtils.isDapNode(root.getChildAt(0)));
        assertEquals("first", ((DefaultMutableTreeNode) root.getChildAt(0)).getUserObject().toString());
    }

    @Test
    public void testAddingDatasetWithDAPAccessAndOneWithFileAccessOnly_FileAccessOnlyResolvesToNodeWithFileAccess() {
        //preparation
        final InvDatasetImpl fileDataset = createDataset(catalog, "second", "file");
        datasets.add(fileDataset);

        //execution
        catalogTree.setNewRootDatasets(datasets);

        //verification
        final DefaultMutableTreeNode root = (DefaultMutableTreeNode) ((JTree) catalogTree.getComponent()).getModel().getRoot();
        assertEquals(2, root.getChildCount());
        assertEquals(true, CatalogTreeUtils.isDapNode(root.getChildAt(0)));
        assertEquals(false, CatalogTreeUtils.isFileNode(root.getChildAt(0)));
        assertEquals(false, CatalogTreeUtils.isDapNode(root.getChildAt(1)));
        assertEquals(true, CatalogTreeUtils.isFileNode(root.getChildAt(1)));
    }

    @Test
    public void testWhetherRootNodeHasBeenExchanged() {
        //preparation
        final InvDatasetImpl fileDataset = createDataset(catalog, "second", "file");
        final ArrayList<InvDataset> otherDatasets = new ArrayList<InvDataset>();
        otherDatasets.add(fileDataset);

        //execution
        catalogTree.setNewRootDatasets(datasets);

        //verification
        final DefaultMutableTreeNode root = (DefaultMutableTreeNode) ((JTree) catalogTree.getComponent()).getModel().getRoot();
        catalogTree.setNewRootDatasets(otherDatasets);
        final DefaultMutableTreeNode otherRoot = (DefaultMutableTreeNode) ((JTree) catalogTree.getComponent()).getModel().getRoot();
        assertEquals(false, root.equals(otherRoot));
    }

    @Test
    public void testThatPreviousDatasetsHaveBeenRemoved() {
        //preparation
        catalogTree.setNewRootDatasets(datasets);
        final DefaultMutableTreeNode previousRootNode = (DefaultMutableTreeNode) ((JTree) catalogTree.getComponent()).getModel().getRoot();

        final InvDatasetImpl fileDataset = createDataset(catalog, "second", "file");
        final ArrayList<InvDataset> otherDatasets = new ArrayList<InvDataset>();
        otherDatasets.add(fileDataset);

        //execution
        catalogTree.setNewRootDatasets(otherDatasets);

        //verification
        final DefaultMutableTreeNode newRootNode = (DefaultMutableTreeNode) ((JTree) catalogTree.getComponent()).getModel().getRoot();
        assertNotSame(previousRootNode, newRootNode);
    }

    private InvDatasetImpl createDataset(InvCatalogImpl catalog, String datasetName, final String serviceName) {
        final InvDatasetImpl dapDataset =
                new InvDatasetImpl(null, datasetName, FeatureType.NONE, serviceName, "http://wherever.you.want.bc");
        dapDataset.setCatalog(catalog);
        final InvService dapService = new InvService(serviceName, serviceName, "irrelevant", "irrelevant", "irrelevant");
        dapDataset.addAccess(new InvAccessImpl(dapDataset, "http://y.z", dapService));
        dapDataset.finish();
        return dapDataset;
    }

}