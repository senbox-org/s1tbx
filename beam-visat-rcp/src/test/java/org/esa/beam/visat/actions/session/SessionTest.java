package org.esa.beam.visat.actions.session;

import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.core.CanceledException;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glayer.LayerTypeRegistry;
import junit.framework.TestCase;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.BitmaskDef;
import org.esa.beam.framework.datamodel.BitmaskOverlayInfo;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.ProductManager;
import org.esa.beam.framework.datamodel.VirtualBand;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.ui.application.ApplicationPage;
import org.esa.beam.framework.ui.product.ProductNodeView;
import org.esa.beam.framework.ui.product.ProductSceneImage;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.glayer.BitmaskCollectionLayer;
import org.esa.beam.glayer.GraticuleLayer;
import org.esa.beam.util.PropertyMap;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.geom.AffineTransform;
import java.io.File;
import java.io.IOException;
import java.net.URI;

public class SessionTest extends TestCase {

    public void testConstruction() {
        try {
            new Session(null, null, new ProductNodeView[0]);
            fail();
        } catch (NullPointerException e) {
            // ok
        }
        try {
            new Session(null, new Product[0], null);
            fail();
        } catch (NullPointerException e) {
            // ok
        }
        try {
            new Session(null, null, null);
            fail();
        } catch (NullPointerException e) {
            // ok
        }

        final Session session = new Session(URI.create("foo"), new Product[0], new ProductNodeView[0]);
        assertEquals(0, session.getProductCount());
        assertEquals(0, session.getViewCount());
    }

    public void testRestore() throws IOException, CanceledException, ValidationException {
        final SessionData sessionData = createSessionData();
        final Product[] originalProducts = sessionData.getProducts();
        final ProductNodeView[] originalViews = sessionData.getViews();

        assertEquals(2, originalProducts.length);
        assertEquals(4, originalViews.length);

        URI sessionRoot = createSessionRootURI();
        final Session originalSession = new Session(sessionRoot, originalProducts, originalViews);
        final Session.ProblemSolver solver = new Session.ProblemSolver() {
            @Override
            public Product solveProductNotFound(int id, File file) {
                return null;
            }
        };
        AppContext appContext = new MyAppContext(originalProducts);
        final RestoredSession restoredSession = originalSession.restore(appContext, sessionRoot, ProgressMonitor.NULL, solver);
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

        assertEquals(originalProducts[0].getFileLocation().toURI(), restoredProducts[0].getFileLocation().toURI());
        assertEquals(originalProducts[1].getFileLocation().toURI(), restoredProducts[1].getFileLocation().toURI());

        assertEquals(originalViews[0].getBounds(), restoredViews[0].getBounds());
        assertEquals(originalViews[1].getBounds(), restoredViews[1].getBounds());
        assertEquals(originalViews[2].getBounds(), restoredViews[2].getBounds());
        assertEquals(originalViews[3].getBounds(), restoredViews[3].getBounds());

        assertEquals(originalViews[0].getVisibleProductNode().getName(),
                     restoredViews[0].getVisibleProductNode().getName());
        assertEquals(originalViews[1].getVisibleProductNode().getName(),
                     restoredViews[1].getVisibleProductNode().getName());
        assertEquals(originalViews[2].getVisibleProductNode().getName(),
                     restoredViews[2].getVisibleProductNode().getName());
        assertEquals(originalViews[3].getVisibleProductNode().getName(),
                     restoredViews[3].getVisibleProductNode().getName());

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

    public void testRestoreAfterMove() throws IOException, CanceledException, ValidationException {
        final SessionData sessionData = createSessionData();
        final Product[] originalProducts = sessionData.getProducts();
        final ProductNodeView[] originalViews = sessionData.getViews();

        assertEquals(2, originalProducts.length);
        assertEquals(4, originalViews.length);

        URI sessionRoot = createSessionRootURI();
        URI movedSesissionRoot = new File("testdata/moved/here/").toURI();
        final Session originalSession = new Session(sessionRoot, originalProducts, originalViews);
        final AppContext appContext = new MyAppContext(originalProducts);
        final RestoredSession restoredSession = originalSession.restore(appContext,
                                                                        movedSesissionRoot, ProgressMonitor.NULL,
                                                                        new Session.ProblemSolver() {
                                                                            @Override
                                                                            public Product solveProductNotFound(int id,
                                                                                                                File file) {
                                                                                for (Product product : originalProducts) {
                                                                                    if (product.getRefNo() == id) {
                                                                                        return product;
                                                                                    }
                                                                                }
                                                                                return null;
                                                                            }
                                                                        });
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

        assertEquals(originalProducts[0].getFileLocation().toURI(), restoredProducts[0].getFileLocation().toURI());
        assertEquals(originalProducts[1].getFileLocation().toURI(), restoredProducts[1].getFileLocation().toURI());

        assertEquals(originalViews[0].getBounds(), restoredViews[0].getBounds());
        assertEquals(originalViews[1].getBounds(), restoredViews[1].getBounds());
        assertEquals(originalViews[2].getBounds(), restoredViews[2].getBounds());
        assertEquals(originalViews[3].getBounds(), restoredViews[3].getBounds());

        assertEquals(originalViews[0].getVisibleProductNode().getName(),
                     restoredViews[0].getVisibleProductNode().getName());
        assertEquals(originalViews[1].getVisibleProductNode().getName(),
                     restoredViews[1].getVisibleProductNode().getName());
        assertEquals(originalViews[2].getVisibleProductNode().getName(),
                     restoredViews[2].getVisibleProductNode().getName());
        assertEquals(originalViews[3].getVisibleProductNode().getName(),
                     restoredViews[3].getVisibleProductNode().getName());

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
        final File file = new File(new File(createSessionRootURI()), "out/DIMAP/" + name + ".dim");
        product.setFileLocation(file);
        return product;
    }

    static void writeProduct(Product product) throws IOException {
        ProductIO.writeProduct(product, product.getFileLocation(), ProductIO.DEFAULT_FORMAT_NAME, false);
    }

    static Session createTestSession() throws IOException, ValidationException {
        final SessionData sessionData = createSessionData();
        return new Session(createSessionRootURI(), sessionData.getProducts(), sessionData.getViews());
    }

    static URI createSessionRootURI() {
        return new File("testdata").toURI();
    }

    static SessionData createSessionData() throws IOException, ValidationException {
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

        productY.addBitmaskDef(new BitmaskDef("D_eq_23", "descr", "D > 0.23", Color.RED, 0.3f));
        productY.addBitmaskDef(new BitmaskDef("C_lt_23", "descr", "C < 0.23", Color.BLUE, 0.3f));
        final BitmaskOverlayInfo overlayInfo = new BitmaskOverlayInfo();
        final BitmaskDef[] defs = productY.getBitmaskDefs();
        for (BitmaskDef def : defs) {
            overlayInfo.addBitmaskDef(def);
        }
        bandD.setBitmaskOverlayInfo(overlayInfo);

        final ProductSceneView sceneViewA = new ProductSceneView(
                new ProductSceneImage(bandA, new PropertyMap(), ProgressMonitor.NULL));
        sceneViewA.setBounds(new Rectangle(0, 0, 200, 100));
        final ProductSceneView sceneViewB = new ProductSceneView(
                new ProductSceneImage(bandB, new PropertyMap(), ProgressMonitor.NULL));
        sceneViewB.setBounds(new Rectangle(0, 100, 200, 100));
        final ProductSceneView sceneViewC = new ProductSceneView(
                new ProductSceneImage(bandC, new PropertyMap(), ProgressMonitor.NULL));
        sceneViewC.setBounds(new Rectangle(200, 0, 200, 100));
        final ProductSceneView sceneViewD = new ProductSceneView(
                new ProductSceneImage(bandD, new PropertyMap(), ProgressMonitor.NULL));
        sceneViewD.setBounds(new Rectangle(200, 100, 200, 100));

        // todo - add more layers (nf)
        GraticuleLayer graticuleLayer = new GraticuleLayer(bandD);
        graticuleLayer.setName("Graticule"); // todo - place in GraticuleLayer constructor (nf)
        graticuleLayer.setVisible(true);
        sceneViewD.getRootLayer().getChildren().add(graticuleLayer);

        final BitmaskCollectionLayer.Type type = LayerTypeRegistry.getLayerType(BitmaskCollectionLayer.Type.class);
        final PropertySet template = type.createLayerConfig(sceneViewD);
        template.setValue(BitmaskCollectionLayer.Type.PROPERTY_NAME_RASTER, bandD);
        BitmaskCollectionLayer bitmaskCollectionLayer = new BitmaskCollectionLayer(type, template);
        bitmaskCollectionLayer.setName("Bitmask Collection");
        bitmaskCollectionLayer.setVisible(true);
        sceneViewD.getRootLayer().getChildren().add(bitmaskCollectionLayer);

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

    private static class MyAppContext implements AppContext {
        private ProductManager productManager;
        private PropertyMap propertyMap;

        private MyAppContext(Product[] originalProducts) {

            propertyMap = new PropertyMap();
            productManager = new ProductManager();
            for (Product originalProduct : originalProducts) {
                productManager.addProduct(originalProduct);
            }
        }

        @Override
        public String getApplicationName() {
            return "Hainz";
        }

        @Override
        public Window getApplicationWindow() {
            return null;
        }

        @Override
        public ApplicationPage getApplicationPage() {
            return null;
        }

        @Override
        public Product getSelectedProduct() {
            return null;
        }

        @Override
        public void handleError(Throwable e) {
        }

        @Override
        public void handleError(String message, Throwable e) {
        }

        @Override
        public PropertyMap getPreferences() {
            return propertyMap;
        }

        @Override
        public ProductManager getProductManager() {
            return productManager;
        }

        @Override
        public ProductSceneView getSelectedProductSceneView() {
            return null;
        }
    }
}
