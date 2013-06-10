package org.esa.beam.opendap.ui;

import org.esa.beam.framework.gpf.ui.DefaultAppContext;
import org.esa.beam.opendap.HeadlessTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import thredds.catalog.InvAccessImpl;
import thredds.catalog.InvCatalogImpl;
import thredds.catalog.InvCatalogRef;
import thredds.catalog.InvDataset;
import thredds.catalog.InvDatasetImpl;
import thredds.catalog.InvService;
import ucar.nc2.constants.FeatureType;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

import static org.junit.Assert.*;

@RunWith(HeadlessTestRunner.class)
public class CatalogTree_appendToNodeTest {

    private ArrayList<InvDataset> datasets;
    private InvCatalogImpl catalog;
    private DefaultMutableTreeNode parentNode;

    @Before
    public void setUp() throws Exception {
        datasets = new ArrayList<InvDataset>();
        catalog = new InvCatalogImpl("catalogName", "1.0", new URI("http://x.y"));
        parentNode = new DefaultMutableTreeNode();
    }

    @Test
    public void testAppendDapNode() throws URISyntaxException {
        // preparation
        datasets.add(createDataset(catalog, "first", "OPENDAP"));

        // execution
        new CatalogTree(null, new DefaultAppContext(""), null).appendToNode(new JTree(), datasets, parentNode, true);

        // verification
        assertEquals(1, parentNode.getChildCount());
        assertEquals(true, parentNode.getChildAt(0).isLeaf());
        assertEquals(true, CatalogTreeUtils.isDapNode(parentNode.getChildAt(0)));
    }

    @Test
    public void testAppendThreeDapNodes() throws URISyntaxException {
        //preparation
        datasets.add(createDataset(catalog, "Name_1", "OPENDAP"));
        datasets.add(createDataset(catalog, "Name_2", "OPENDAP"));
        datasets.add(createDataset(catalog, "Name_3", "OPENDAP"));

        //execution
        new CatalogTree(null, new DefaultAppContext(""), null).appendToNode(new JTree(), datasets, parentNode, true);

        //verification
        assertEquals(3, parentNode.getChildCount());
        for (int i = 0; i < parentNode.getChildCount(); i++) {
            final DefaultMutableTreeNode childAt = (DefaultMutableTreeNode) parentNode.getChildAt(i);
            final String indexMessage = "Index = " + i;
            assertEquals(indexMessage, true, childAt.isLeaf());
            assertEquals(indexMessage, true, CatalogTreeUtils.isDapNode(childAt));
            assertEquals(indexMessage, "Name_" + (i + 1), childAt.getUserObject().toString());
        }
    }

    @Test
    public void testAppendFileNode() throws URISyntaxException {
        //preparation
        datasets.add(createDataset(catalog, "fileName", "file"));

        //execution
        new CatalogTree(null, new DefaultAppContext(""), null).appendToNode(new JTree(), datasets, parentNode, true);

        //verification
        assertEquals(1, parentNode.getChildCount());
        assertEquals(true, parentNode.getChildAt(0).isLeaf());
        assertEquals(false, CatalogTreeUtils.isDapNode(parentNode.getChildAt(0)));
    }

    @Test
    public void testAppendCatalogNode() throws URISyntaxException {
        //preparation
        datasets.add(createCatalogRefDataset());

        //execution
        new CatalogTree(null, new DefaultAppContext(""), null).appendToNode(new JTree(), datasets, parentNode, true);

        //verification
        assertEquals(1, parentNode.getChildCount());
        assertEquals(1, parentNode.getChildAt(0).getChildCount());
        assertEquals(true, parentNode.getChildAt(0).getChildAt(0).isLeaf());
        assertEquals(false, CatalogTreeUtils.isDapNode(parentNode.getChildAt(0)));
        assertEquals(false, CatalogTreeUtils.isCatalogReferenceNode(parentNode.getChildAt(0)));
        assertEquals(false, CatalogTreeUtils.isDapNode(parentNode.getChildAt(0).getChildAt(0)));
        assertEquals(true, CatalogTreeUtils.isCatalogReferenceNode(parentNode.getChildAt(0).getChildAt(0)));
    }

    @Test
    public void testAppendingVariousDatasets() {
        //preparation
        datasets.add(createDataset(catalog, "dapName", "OPENDAP"));
        datasets.add(createDataset(catalog, "fileName", "FILE"));
        datasets.add(createCatalogRefDataset());

        //execution
        new CatalogTree(null, new DefaultAppContext(""), null).appendToNode(new JTree(), datasets, parentNode, true);

        //verification
        assertEquals(3, parentNode.getChildCount());
        assertEquals(true, CatalogTreeUtils.isDapNode(parentNode.getChildAt(0)));
        assertEquals(false, CatalogTreeUtils.isDapNode(parentNode.getChildAt(1)));
        assertEquals(false, CatalogTreeUtils.isCatalogReferenceNode(parentNode.getChildAt(1)));
        assertEquals(true, CatalogTreeUtils.isCatalogReferenceNode(parentNode.getChildAt(2).getChildAt(0)));
    }

    private InvDatasetImpl createDataset(InvCatalogImpl catalog, String datasetName, final String serviceName) {
        final InvDatasetImpl dapDataset = new InvDatasetImpl(null, datasetName, FeatureType.NONE, serviceName, "http://wherever.you.want.bc");
        dapDataset.setCatalog(catalog);
        final InvService dapService = new InvService(serviceName, serviceName, "irrelevant", "irrelevant", "irrelevant");
        dapDataset.addAccess(new InvAccessImpl(dapDataset, "http://y.z", dapService));
        dapDataset.finish();
        return dapDataset;
    }

    private InvCatalogRef createCatalogRefDataset() {
        final InvCatalogRef catalogRef = new InvCatalogRef(null, "catalogName", "irrelevant");
        catalogRef.setCatalog(catalog);
        return catalogRef;
    }

}