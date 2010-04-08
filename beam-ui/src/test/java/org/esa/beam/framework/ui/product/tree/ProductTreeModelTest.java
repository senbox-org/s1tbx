package org.esa.beam.framework.ui.product.tree;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.PinDescriptor;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Placemark;
import org.esa.beam.framework.datamodel.PlainFeatureFactory;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.ProductManager;
import org.esa.beam.framework.datamodel.VectorDataNode;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class ProductTreeModelTest {

    private ProductManager productManager;
    private ProductTreeModel treeModel;


    @Before
    public void setupTest() {
        productManager = new ProductManager();
        treeModel = new ProductTreeModel(productManager);
    }

    @Test
    public void testEmptyManager() {
        assertSame(productManager, treeModel.getProductManager());
        final ProductManagerNode treeRoot = (ProductManagerNode) treeModel.getRoot();
        assertSame(productManager, treeRoot.getContent());
        assertTrue("Root is not a leaf", treeModel.isLeaf(treeRoot));
        assertEquals(0, treeModel.getChildCount(treeRoot));
    }

    @Test
    public void testAddingRemovingProducts() {
        final Object treeRoot = treeModel.getRoot();
        final Product product1 = createDummyProduct("x1");
        productManager.addProduct(product1);

        assertEquals(1, treeModel.getChildCount(treeRoot));
        final ProductNode child1 = (ProductNode) treeModel.getChild(treeRoot, 0);
        assertSame(product1, child1.getProduct());

        final Product product2 = createDummyProduct("x2");
        productManager.addProduct(product2);

        assertEquals(2, treeModel.getChildCount(treeRoot));
        final ProductNode child2 = (ProductNode) treeModel.getChild(treeRoot, 1);
        assertSame(product2, child2.getProduct());

        productManager.removeProduct(product1);

        assertEquals(1, treeModel.getChildCount(treeRoot));
        final ProductNode child3 = (ProductNode) treeModel.getChild(treeRoot, 0);
        assertSame(product2, child3.getProduct());

    }

    @Test
    public void testAddingRemovingProductNodeGroups() {
        final Product product = createDummyProduct("x1");
        productManager.addProduct(product);
        ProductTreeNode rootNode = treeModel.getRoot();
        ProductNode productNode = (ProductNode) treeModel.getChild(rootNode, 0);
        assertEquals(0, treeModel.getChildCount(productNode));

        product.addBand("b1", ProductData.TYPE_INT8);
        assertEquals(1, treeModel.getChildCount(productNode));
        product.addBand("b2", ProductData.TYPE_INT8);
        assertEquals(1, treeModel.getChildCount(productNode));

        final Placemark pin1 = createDummyPin("p1");
        product.getPinGroup().add(pin1);
        assertEquals(2, treeModel.getChildCount(productNode));
        final VectorDataNode vec1 = new VectorDataNode("v1", PlainFeatureFactory.createDefaultFeatureType());
        product.getVectorDataGroup().add(vec1);
        assertEquals(2, treeModel.getChildCount(productNode));
        product.getPinGroup().remove(pin1);
        assertEquals(2, treeModel.getChildCount(productNode));
        product.getVectorDataGroup().remove(vec1);
        assertEquals(1, treeModel.getChildCount(productNode));
    }

    @Test
    public void testAddingRemovingProductNodes() {
        final Product product = createDummyProduct("x1");
        productManager.addProduct(product);
        ProductTreeNode rootNode = treeModel.getRoot();
        ProductNode productNode = (ProductNode) treeModel.getChild(rootNode, 0);

        assertEquals(0, treeModel.getChildCount(productNode));

        final Band band1 = product.addBand("b1", ProductData.TYPE_INT8);
        assertEquals(1, treeModel.getChildCount(productNode));
        final Object bandGroup = treeModel.getChild(productNode, 0);
        assertNotNull(bandGroup);
        assertEquals(1, treeModel.getChildCount(bandGroup));
        assertSame(band1, ((ProductNodeNode)treeModel.getChild(bandGroup, 0)).getProductNode());

        product.addBand("b2", ProductData.TYPE_INT8);
        assertEquals(2, treeModel.getChildCount(bandGroup));
        product.removeBand(product.getBand("b2"));
        product.removeBand(product.getBand("b1"));
        assertEquals(0, treeModel.getChildCount(productNode));
    }
    

    private Placemark createDummyPin(String name) {
        return new Placemark(name, "", "",
                             new PixelPos(0.5f, 0.5f), null,
                             PinDescriptor.INSTANCE, null);
    }

    private Product createDummyProduct(String name) {
        return new Product(name, "t", 1, 1);
    }

}
