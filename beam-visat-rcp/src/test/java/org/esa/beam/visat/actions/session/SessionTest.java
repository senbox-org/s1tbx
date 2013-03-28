/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.beam.visat.actions.session;

import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.core.CanceledException;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerTypeRegistry;
import junit.framework.TestCase;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.BitmaskDef;
import org.esa.beam.framework.datamodel.GcpDescriptor;
import org.esa.beam.framework.datamodel.PinDescriptor;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Placemark;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.ProductManager;
import org.esa.beam.framework.datamodel.VirtualBand;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.ui.application.ApplicationPage;
import org.esa.beam.framework.ui.product.ProductNodeView;
import org.esa.beam.framework.ui.product.ProductSceneImage;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.glayer.GraticuleLayer;
import org.esa.beam.glayer.MaskCollectionLayer;
import org.esa.beam.glayer.MaskLayerType;
import org.esa.beam.util.PropertyMap;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.Window;
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
        final RestoredSession restoredSession = originalSession.restore(appContext, sessionRoot, ProgressMonitor.NULL,
                                                                        solver);
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
        productY.getPinGroup().add(
                Placemark.createPointPlacemark(PinDescriptor.getInstance(), "Pin", "", "", new PixelPos(0, 0), null,
                                               null));
        productY.getGcpGroup().add(
                Placemark.createPointPlacemark(GcpDescriptor.getInstance(), "GCP", "", "", new PixelPos(0, 0), null,
                                               null));
        productY.addBitmaskDef(new BitmaskDef("M1", "descr", "D > 0.23", Color.RED, 0.3f));
        productY.addBitmaskDef(new BitmaskDef("M2", "descr", "C < 0.23", Color.BLUE, 0.3f));
        writeProduct(productX);
        writeProduct(productY);

        assertEquals(4, productY.getMaskGroup().getNodeCount()); // pin + GCPs + 2 extra masks = 4

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

        final GraticuleLayer graticuleLayer = new GraticuleLayer(bandD);
        graticuleLayer.setName("Graticule");
        graticuleLayer.setVisible(true);
        sceneViewD.getRootLayer().getChildren().add(graticuleLayer);

        final MaskCollectionLayer.Type mclType = LayerTypeRegistry.getLayerType(MaskCollectionLayer.Type.class);
        final MaskLayerType mlType = LayerTypeRegistry.getLayerType(MaskLayerType.class);
        final Layer maskCollectionLayer = mclType.createLayer(null, mclType.createLayerConfig(null));
        maskCollectionLayer.setName("Mask Collection");
        maskCollectionLayer.setVisible(true);
        maskCollectionLayer.getChildren().add(createMaskLayer(mlType, productY, "M1"));
        maskCollectionLayer.getChildren().add(createMaskLayer(mlType, productY, "M2"));
        sceneViewD.getRootLayer().getChildren().add(maskCollectionLayer);

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

    private static Layer createMaskLayer(MaskLayerType mlType, Product product, String name) {
        PropertySet layerConfig = mlType.createLayerConfig(null);
        layerConfig.setValue(MaskLayerType.PROPERTY_NAME_MASK, product.getMaskGroup().get(name));
        return mlType.createLayer(null, layerConfig);
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
