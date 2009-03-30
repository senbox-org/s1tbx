package org.esa.beam.visat.actions.session;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glayer.Layer;
import junit.framework.TestCase;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.VirtualBand;
import org.esa.beam.framework.ui.product.ProductNodeView;
import org.esa.beam.framework.ui.product.ProductSceneImage;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.framework.draw.Figure;
import org.esa.beam.framework.draw.ShapeFigure;
import org.esa.beam.util.PropertyMap;
import org.esa.beam.glayer.FigureLayer;
import org.esa.beam.glayer.GraticuleLayer;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

public class SessionTest extends TestCase {
    public void testConstruction() throws IOException {
        try {
            new Session(null, new ProductNodeView[0]);
            fail();
        } catch (NullPointerException e) {
            // ok
        }
        try {
            new Session(new Product[0], null);
            fail();
        } catch (NullPointerException e) {
            // ok
        }
        try {
            new Session(null, null);
            fail();
        } catch (NullPointerException e) {
            // ok
        }

        final Session session = new Session(new Product[0], new ProductNodeView[0]);
        assertEquals(0, session.getProductCount());
        assertEquals(0, session.getViewCount());
    }

    public void testRestore() throws IOException {

        final SessionData sessionData = createSessionData();
        final Product[] originalProducts = sessionData.getProducts();
        final ProductNodeView[] originalViews = sessionData.getViews();

        assertEquals(2, originalProducts.length);
        assertEquals(4, originalViews.length);

        final Session originalSession = new Session(originalProducts, originalViews);
        final RestoredSession restoredSession = originalSession.restore(ProgressMonitor.NULL);
        checkProblems(restoredSession.getProblems());
        final Product[] restoredProducts = restoredSession.getProducts();
        assertNotNull(restoredProducts);
        final ProductNodeView[] restoredViews = restoredSession.getViews();
        assertNotNull(restoredViews);

        assertEquals(2, originalProducts.length);
        assertEquals(2, restoredProducts.length);

        assertEquals(4, originalViews.length);
        assertEquals(4, restoredViews.length);

        assertEquals(originalProducts[0].getRefNo(), restoredProducts[0].getRefNo());
        assertEquals(originalProducts[1].getRefNo(), restoredProducts[1].getRefNo());

        assertEquals(originalProducts[0].getFileLocation(), restoredProducts[0].getFileLocation());
        assertEquals(originalProducts[1].getFileLocation(), restoredProducts[1].getFileLocation());

        assertEquals(originalViews[0].getBounds(), restoredViews[0].getBounds());
        assertEquals(originalViews[1].getBounds(), restoredViews[1].getBounds());
        assertEquals(originalViews[2].getBounds(), restoredViews[2].getBounds());
        assertEquals(originalViews[3].getBounds(), restoredViews[3].getBounds());

        assertEquals(originalViews[0].getVisibleProductNode().getName(), restoredViews[0].getVisibleProductNode().getName());
        assertEquals(originalViews[1].getVisibleProductNode().getName(), restoredViews[1].getVisibleProductNode().getName());
        assertEquals(originalViews[2].getVisibleProductNode().getName(), restoredViews[2].getVisibleProductNode().getName());
        assertEquals(originalViews[3].getVisibleProductNode().getName(), restoredViews[3].getVisibleProductNode().getName());

        assertTrue(restoredViews[0] instanceof ProductSceneView);
        assertTrue(restoredViews[1] instanceof ProductSceneView);
        assertTrue(restoredViews[2] instanceof ProductSceneView);
        assertTrue(restoredViews[3] instanceof ProductSceneView);

        assertTrue(restoredViews[0].getVisibleProductNode() instanceof VirtualBand);
        assertTrue(restoredViews[1].getVisibleProductNode() instanceof VirtualBand);
        assertTrue(restoredViews[2].getVisibleProductNode() instanceof VirtualBand);
        assertTrue(restoredViews[3].getVisibleProductNode() instanceof VirtualBand);

        assertSame(restoredProducts[0], restoredViews[0].getVisibleProductNode().getProduct());
        assertSame(restoredProducts[1], restoredViews[1].getVisibleProductNode().getProduct());
        assertSame(restoredProducts[0], restoredViews[2].getVisibleProductNode().getProduct());
        assertSame(restoredProducts[1], restoredViews[3].getVisibleProductNode().getProduct());

    }

    static void checkProblems(Exception[] problems) {
        if (problems.length > 0) {
            for (Exception exception : problems) {
                exception.printStackTrace();
            }
            fail("Problems detected while restoring session!");
        }
    }

    static Product createProduct(int refNo, String name, String type, int w, int h) {
        final Product product = new Product(name, type, w, h);
        product.setRefNo(refNo);
        final File file = new File("testdata/out/DIMAP/" + name + ".dim");
        product.setFileLocation(file);
        return product;
    }

    static void writeProduct(Product product) throws IOException {
        ProductIO.writeProduct(product, product.getFileLocation(), ProductIO.DEFAULT_FORMAT_NAME, false);
    }

    static Session createTestSession() throws IOException {
        final SessionData sessionData = createSessionData();
        return new Session(sessionData.getProducts(), sessionData.getViews());
    }

    static SessionData createSessionData() throws IOException {
        final Product productX = createProduct(11, "X", "XT", 16, 16);
        final Product productY = createProduct(15, "Y", "YT", 16, 16);
        final VirtualBand bandA = new VirtualBand("A", ProductData.TYPE_INT32, 16, 16, "0.23");
        final VirtualBand bandB = new VirtualBand("B", ProductData.TYPE_INT32, 16, 16, "0.23");
        productX.addBand(bandA);
        productX.addBand(bandB);
        final VirtualBand bandC = new VirtualBand("C", ProductData.TYPE_INT32, 16, 16, "0.23");
        final VirtualBand bandD = new VirtualBand("D", ProductData.TYPE_INT32, 16, 16, "0.23");
        productY.addBand(bandC);
        productY.addBand(bandD);
        writeProduct(productX);
        writeProduct(productY);

        final ProductSceneView sceneViewA = new ProductSceneView(new ProductSceneImage(bandA, new PropertyMap(), ProgressMonitor.NULL));
        sceneViewA.setBounds(new Rectangle(0, 0, 200, 100));
        final ProductSceneView sceneViewB = new ProductSceneView(new ProductSceneImage(bandB, new PropertyMap(), ProgressMonitor.NULL));
        sceneViewB.setBounds(new Rectangle(0, 100, 200, 100));
        final ProductSceneView sceneViewC = new ProductSceneView(new ProductSceneImage(bandC, new PropertyMap(), ProgressMonitor.NULL));
        sceneViewC.setBounds(new Rectangle(200, 0, 200, 100));
        final ProductSceneView sceneViewD = new ProductSceneView(new ProductSceneImage(bandD, new PropertyMap(), ProgressMonitor.NULL));
        sceneViewD.setBounds(new Rectangle(200, 100, 200, 100));

        // todo - add more layers (nf)
        GraticuleLayer graticuleLayer = new GraticuleLayer(bandD.getProduct(), bandD, new AffineTransform());
        graticuleLayer.setName("Graticule"); // todo - place in GraticuleLayer constructor (nf)
        graticuleLayer.setVisible(true);
        sceneViewD.getRootLayer().getChildren().add(graticuleLayer);

        return new SessionData(
                new Product[]{
                        productX,
                        productY,
                },
                new ProductNodeView[]{
                        sceneViewA,
                        sceneViewC,
                        sceneViewB,
                        sceneViewD
                });
    }

    static class SessionData {

        private Product[] products;

        private ProductNodeView[] views;

        private SessionData(Product[] products, ProductNodeView[] views) {
            this.products = products;
            this.views = views;
        }

        public Product[] getProducts() {
            return products;
        }

        public ProductNodeView[] getViews() {
            return views;
        }
    }
}
