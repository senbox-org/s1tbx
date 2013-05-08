package org.esa.beam.opendap.ui;

import org.esa.beam.framework.gpf.ui.DefaultAppContext;
import org.esa.beam.opendap.datamodel.OpendapLeaf;
import org.junit.Assume;
import org.junit.Test;
import thredds.catalog.InvAccessImpl;
import thredds.catalog.InvCatalog;
import thredds.catalog.InvCatalogImpl;
import thredds.catalog.InvDataset;
import thredds.catalog.InvDatasetImpl;
import thredds.catalog.InvService;
import ucar.nc2.constants.FeatureType;

import javax.swing.ImageIcon;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import static org.junit.Assert.*;

public class CatalogTree_simpleDifferentTests {

    @Test
    public void testThatGetComponentGetsAWellDefinedJTreeComponent() {
        Assume.assumeTrue(!GraphicsEnvironment.isHeadless());
        final CatalogTree catalogTree = new CatalogTree(null, new DefaultAppContext(""), null);
        final Component component = catalogTree.getComponent();

        assertNotNull(component);
        assertEquals(true, component instanceof JTree);
        final JTree tree = (JTree) component;
        assertEquals(false, tree.isRootVisible());
        assertNotNull(tree.getModel());
        assertEquals(true, tree.getModel() instanceof DefaultTreeModel);

        final DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
        assertNotNull(model.getRoot());
        assertEquals(true, model.getRoot() instanceof DefaultMutableTreeNode);
        final DefaultMutableTreeNode rootNode = (DefaultMutableTreeNode) model.getRoot();
        assertNotNull(rootNode.getUserObject());
        assertEquals(true, rootNode.getUserObject() instanceof String);
        assertEquals("root", rootNode.getUserObject().toString());
    }

    @Test
    public void testThatAWellDefinedRootNodeIsCreated() {
        final DefaultMutableTreeNode rootNode = CatalogTreeUtils.createRootNode();
        assertNotNull(rootNode);

        final Object userObject = rootNode.getUserObject();
        assertNotNull(userObject);
        assertTrue(userObject instanceof String);
        assertEquals("root", userObject.toString());
    }

    @Test
    public void testThatCellRendererIsSet() {
        final JTree jTree = new JTree();
        final TreeCellRenderer renderer1 = jTree.getCellRenderer();
        assertNotNull(renderer1);
        assertEquals(true, renderer1 instanceof DefaultTreeCellRenderer);

        CatalogTreeUtils.addCellRenderer(jTree);

        final TreeCellRenderer renderer2 = jTree.getCellRenderer();
        assertNotNull(renderer2);
        assertEquals(true, renderer2 instanceof DefaultTreeCellRenderer);
        assertNotSame(renderer1, renderer2);
    }

    @Test
    public void testThatRendererRendersDifferentTypes() {
        final JTree jTree = new JTree();
        CatalogTreeUtils.addCellRenderer(jTree);
        final TreeCellRenderer dapCellRenderer = jTree.getCellRenderer();

        final OpendapLeaf opendapLeaf = new OpendapLeaf("This is A dap Node", new InvDataset(null, "") {
        });
        opendapLeaf.setDapAccess(true);
        final OpendapLeaf fileLeaf = new OpendapLeaf("This is A File Node", new InvDataset(null, "") {
        });
        fileLeaf.setFileAccess(true);
        final Object dapNode = new DefaultMutableTreeNode(opendapLeaf);
        final Object fileNode = new DefaultMutableTreeNode(fileLeaf);
        final Object noDapNode = new DefaultMutableTreeNode("otherNode");

        final Component component = dapCellRenderer.getTreeCellRendererComponent(jTree, noDapNode, false, false, true, 0, false);

        assertEquals(true, component instanceof DefaultTreeCellRenderer);
        final DefaultTreeCellRenderer tcr1 = (DefaultTreeCellRenderer) component;
        assertEquals("otherNode", tcr1.getText());
        assertEquals(true, tcr1.getIcon() instanceof ImageIcon);

        final Color foreground = tcr1.getForeground();
        final Color background = tcr1.getBackground();
        final Font font = tcr1.getFont();

        final Component component2 = dapCellRenderer.getTreeCellRendererComponent(jTree, dapNode, false, false, true, 0, false);

        assertSame(component, component2);

        assertEquals(true, component2 instanceof DefaultTreeCellRenderer);
        final DefaultTreeCellRenderer tcr2 = (DefaultTreeCellRenderer) component2;
        assertEquals("This is A dap Node", tcr2.getText());
        assertEquals(true, tcr2.getIcon() instanceof ImageIcon);
        final ImageIcon icon2 = (ImageIcon) tcr2.getIcon();
        // todo change the expected icon to a realistic icon
        assertEquals("/DRsProduct16.png", icon2.getDescription().substring(icon2.getDescription().lastIndexOf("/")));

        assertEquals(foreground, tcr2.getForeground());
        assertEquals(background, tcr2.getBackground());
        assertEquals(font, tcr2.getFont());


        final Component component3 = dapCellRenderer.getTreeCellRendererComponent(jTree, fileNode, false, false, true, 0, false);

        assertSame(component, component3);

        assertEquals(true, component3 instanceof DefaultTreeCellRenderer);
        final DefaultTreeCellRenderer tcr3 = (DefaultTreeCellRenderer) component3;
        assertEquals("This is A File Node", tcr3.getText());
        assertEquals(true, tcr3.getIcon() instanceof ImageIcon);
        final ImageIcon icon3 = (ImageIcon) tcr3.getIcon();
        // todo change the expected icon to a realistic icon
        assertEquals("/FRsProduct16.png", icon3.getDescription().substring(icon3.getDescription().lastIndexOf("/")));

        assertEquals(foreground, tcr3.getForeground());
        assertEquals(background, tcr3.getBackground());
        assertEquals(font, tcr3.getFont());
    }

    @Test
    public void testGetLeaves() throws Exception {
        Assume.assumeTrue(!GraphicsEnvironment.isHeadless());
        final CatalogTree catalogTree = new CatalogTree(null, new DefaultAppContext(""), null);
        List<InvDataset> datasets = new ArrayList<InvDataset>();
        InvCatalog catalog = new InvCatalogImpl("catalogName", "1.0", new URI("http://x.y"));
        final InvDataset rootDataset = createDataset(catalog, "first", "OPENDAP");
        rootDataset.getDatasets().add(createDataset(catalog, "second", "OPENDAP"));
        rootDataset.getDatasets().add(createDataset(catalog, "third", "OPENDAP"));

        datasets.add(rootDataset);
        catalogTree.setNewRootDatasets(datasets);

        OpendapLeaf[] leaves = catalogTree.getLeaves();
        Arrays.sort(leaves, new Comparator<OpendapLeaf>() {
            @Override
            public int compare(OpendapLeaf o1, OpendapLeaf o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        assertEquals(2, leaves.length);
        assertEquals("second", leaves[0].getName());
        assertEquals("third", leaves[1].getName());
    }

    private InvDataset createDataset(InvCatalog catalog, String datasetName, final String serviceName) {
        final InvDatasetImpl dapDataset =
                new InvDatasetImpl(null, datasetName, FeatureType.NONE, serviceName, "http://wherever.you.want.bc");
        dapDataset.setCatalog(catalog);
        final InvService dapService = new InvService(serviceName, serviceName, "irrelevant", "irrelevant", "irrelevant");
        dapDataset.addAccess(new InvAccessImpl(dapDataset, "http://y.z", dapService));
        dapDataset.finish();
        return dapDataset;
    }

}
