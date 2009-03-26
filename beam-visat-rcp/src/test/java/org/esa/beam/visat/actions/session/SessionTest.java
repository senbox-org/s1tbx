package org.esa.beam.visat.actions.session;

import com.bc.ceres.core.ProgressMonitor;
import com.thoughtworks.xstream.XStream;
import junit.framework.TestCase;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.ProductManager;
import org.esa.beam.framework.datamodel.VirtualBand;
import org.esa.beam.framework.ui.product.ProductNodeView;
import org.esa.beam.framework.ui.product.ProductSceneImage;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.util.PropertyMap;

import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class SessionTest extends TestCase {

    public void testSaveProductsOnly() throws IOException {
        final ProductManager productManager = new ProductManager();

        final Product product1 = createProduct("Ralf", "Quast", 17, 11);
        final Product product2 = createProduct("Norman", "Fomferra", 23, 19);
        productManager.addProduct(product1);
        productManager.addProduct(product2);

        final Session sessionOriginal = new Session(productManager.getProducts(), new ProductNodeView[0]);
        assertEquals(2, sessionOriginal.getProductCount());
        final Session.ProductRef productRefOriginal1 = sessionOriginal.getProductRef(0);
        assertEquals(product1.getRefNo(), productRefOriginal1.refNo);
        assertEquals(product1.getFileLocation(), productRefOriginal1.fileLocation);
        final Session.ProductRef productRefOriginal2 = sessionOriginal.getProductRef(1);
        assertEquals(product2.getRefNo(), productRefOriginal2.refNo);
        assertEquals(product2.getFileLocation(), productRefOriginal2.fileLocation);

        XStream xStream = new XStream();
        xStream.autodetectAnnotations(true);

        final String xml = xStream.toXML(sessionOriginal);
        System.out.println("xml = " + xml);
        final Object object = xStream.fromXML(xml);

        assertTrue(object instanceof Session);
        Session sessionRestored = (Session) object;
        assertEquals(Session.CURRENT_MODEL_VERSION, sessionRestored.getModelVersion());

        assertEquals(sessionOriginal.getProductCount(), sessionRestored.getProductCount());
        final Session.ProductRef productRefRestored1 = sessionRestored.getProductRef(0);
        assertEquals(productRefOriginal1.refNo, productRefRestored1.refNo);
        assertEquals(productRefOriginal1.fileLocation, productRefRestored1.fileLocation);
        final Session.ProductRef productRefRestored2 = sessionRestored.getProductRef(1);
        assertEquals(productRefOriginal2.refNo, productRefRestored2.refNo);
        assertEquals(productRefOriginal2.fileLocation, productRefRestored2.fileLocation);

        final Product[] restoredProducts = sessionRestored.restoreProducts(ProgressMonitor.NULL, new ArrayList<IOException>());
        assertNotNull(restoredProducts);
        assertEquals(2, restoredProducts.length);

        assertEquals(product1.getRefNo(), restoredProducts[0].getRefNo());
        assertEquals(product1.getFileLocation(), restoredProducts[0].getFileLocation());
        assertEquals(product2.getRefNo(), restoredProducts[1].getRefNo());
        assertEquals(product2.getFileLocation(), restoredProducts[1].getFileLocation());
    }

    public void testSaveSceneViews() throws IOException {

        final ProductManager productManager = new ProductManager();

        final Product product1 = createProduct("Ralf", "Quast", 17, 11);
        final Product product2 = createProduct("Norman", "Fomferra", 23, 19);
        final VirtualBand band1 = new VirtualBand("RQ", ProductData.TYPE_INT32, 17, 11, "0.23");
        product1.addBand(band1);
        productManager.addProduct(product1);
        productManager.addProduct(product2);

        final ProductSceneView sceneView1 = new ProductSceneView(new ProductSceneImage(band1, new PropertyMap(), ProgressMonitor.NULL));
        sceneView1.setGraticuleOverlayEnabled(true);
        sceneView1.setBounds(new Rectangle(100, 110, 120, 130));
        final ProductNodeView[] views = new ProductNodeView[]{
                sceneView1,
        };

        final Session sessionOriginal = new Session(productManager.getProducts(), views);
        assertEquals(Session.CURRENT_MODEL_VERSION, sessionOriginal.getModelVersion());
        assertEquals(1, sessionOriginal.getViewCount());
        assertEquals(sceneView1.getClass().getName(), sessionOriginal.getViewRef(0).type);
        assertEquals(sceneView1.getBounds(), sessionOriginal.getViewRef(0).bounds);
        assertEquals(product1.getRefNo(), sessionOriginal.getViewRef(0).productRefNo);
        assertEquals(band1.getName(), sessionOriginal.getViewRef(0).nodeName);

        XStream xStream = new XStream();
        xStream.autodetectAnnotations(true);

        final String xml = xStream.toXML(sessionOriginal);
        System.out.println("xml = " + xml);
        final Object object = xStream.fromXML(xml);
        assertTrue(object instanceof Session);
        Session sessionRestored = (Session) object;
        assertEquals(Session.CURRENT_MODEL_VERSION, sessionRestored.getModelVersion());

        final Product[] restoredProducts = sessionRestored.restoreProducts(ProgressMonitor.NULL, new ArrayList<IOException>());
        final ProductNodeView[] restoredViews = sessionRestored.restoreViews(restoredProducts, ProgressMonitor.NULL);
        assertNotNull(restoredViews);
// todo - continue work (nf)        
//        assertEquals(1, restoredViews.length);
//
//        assertTrue(restoredViews[0].getVisibleProductNode() instanceof VirtualBand);
//        assertSame(restoredProducts[0], restoredViews[0].getVisibleProductNode().getProduct());
//        assertEquals(sceneView1.getVisibleProductNode().getName(), restoredViews[0].getVisibleProductNode().getName());
//        assertEquals(sceneView1.getBounds(), restoredViews[0].getBounds());
    }

    private Product createProduct(String name, String type, int w, int h) throws IOException {
        final Product product = new Product(name, type, w, h);
        final File file = new File("testdata/out/DIMAP/" + name + ".dim");
        product.setFileLocation(file);

        if (!file.exists()) {
            ProductIO.writeProduct(product, file, ProductIO.DEFAULT_FORMAT_NAME, false);
        }

        return product;
    }

}
