package org.esa.beam.visat.actions.session;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import com.bc.ceres.grender.Viewport;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.ui.product.ProductNodeView;
import org.esa.beam.framework.ui.product.ProductSceneImage;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.util.PropertyMap;

import javax.swing.JComponent;
import javax.swing.RootPaneContainer;
import java.awt.Container;
import java.awt.Rectangle;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * todo - add API doc
 *
 * @author Ralf Quast
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 * @since BEAM 4.6
 */
@XStreamAlias("session")
public class Session {

    public static final String CURRENT_MODEL_VERSION = "1.0.0";

    final String modelVersion;
    @XStreamAlias("products")
    final ProductRef[] productRefs;
    @XStreamAlias("views")
    final ViewRef[] viewRefs;

    public Session(Product[] products, ProductNodeView[] views) {
        modelVersion = CURRENT_MODEL_VERSION;

        productRefs = new ProductRef[products.length];
        for (int i = 0; i < products.length; i++) {
            Product product = products[i];
            productRefs[i] = new ProductRef(product.getRefNo(), product.getFileLocation());
        }

        viewRefs = new ViewRef[views.length];
        for (int i = 0; i < views.length; i++) {
            ProductNodeView view = views[i];
            ViewportDef viewportDef = null;
            if (view instanceof ProductSceneView) {
                final ProductSceneView sceneView = (ProductSceneView) view;
                final Viewport viewport = sceneView.getLayerCanvas().getViewport();
                viewportDef = new ViewportDef(viewport.isModelYAxisDown(),
                                              viewport.getOffsetX(),
                                              viewport.getOffsetY(),
                                              viewport.getZoomFactor(),
                                              viewport.getOrientation());
            }

            Rectangle viewBounds = new Rectangle(0, 0, 200, 200);
            if (view instanceof JComponent) {
                viewBounds = getRootPaneContainer((JComponent) view).getBounds();
            }

            viewRefs[i] = new ViewRef(i,
                                      view.getClass().getName(),
                                      viewBounds,
                                      viewportDef,
                                      view.getVisibleProductNode().getProduct().getRefNo(),
                                      view.getVisibleProductNode().getName());


        }
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public int getProductCount() {
        return productRefs.length;
    }

    public ProductRef getProductRef(int index) {
        return productRefs[index];
    }

    public int getViewCount() {
        return viewRefs.length;
    }

    public ViewRef getViewRef(int index) {
        return viewRefs[index];
    }

    public RestoredSession restore(ProgressMonitor pm) {
        try {
            pm.beginTask("Restoring session", 100);
            final ArrayList<Exception> problems = new ArrayList<Exception>();
            final Product[] products = restoreProducts(SubProgressMonitor.create(pm, 80), problems);
            final ProductNodeView[] views = restoreViews(products, SubProgressMonitor.create(pm, 20), problems);
            return new RestoredSession(products, views, problems.toArray(new Exception[problems.size()]));
        } finally {
            pm.done();
        }
    }

    Product[] restoreProducts(ProgressMonitor pm, List<Exception> problems) {
        ArrayList<Product> products = new ArrayList<Product>();
        try {
            pm.beginTask("Restoring products", productRefs.length);
            for (ProductRef productRef : productRefs) {
                try {
                    final Product product = ProductIO.readProduct(productRef.file, null);
                    products.add(product);
                    product.setRefNo(productRef.id);
                } catch (Exception e) {
                    problems.add(e);
                }
                pm.worked(1);
            }
        } finally {
            pm.done();
        }
        return products.toArray(new Product[products.size()]);
    }

    ProductNodeView[] restoreViews(Product[] restoredProducts, ProgressMonitor pm, List<Exception> problems) {
        ArrayList<ProductNodeView> views = new ArrayList<ProductNodeView>();
        try {
            pm.beginTask("Restoring views", viewRefs.length);
            for (ViewRef viewRef : viewRefs) {
                try {
                    if (ProductSceneView.class.getName().equals(viewRef.type)) {
                        Product product = getProductForRefNo(restoredProducts, viewRef.productId);
                        if (product != null) {
                            RasterDataNode node = product.getRasterDataNode(viewRef.productNodeName);
                            if (node != null) {
                                final ProductSceneView view = new ProductSceneView(new ProductSceneImage(node, new PropertyMap(), SubProgressMonitor.create(pm, 1)));
                                Rectangle bounds = viewRef.bounds;
                                if (bounds != null && !bounds.isEmpty()) {
                                    view.setBounds(bounds);
                                }
                                ViewportDef viewportDef = viewRef.viewportDef;
                                if (viewportDef != null) {
                                    Viewport viewport = view.getLayerCanvas().getViewport();
                                    viewport.setModelYAxisDown(viewportDef.modelYAxisDown);
                                    viewport.setZoomFactor(viewportDef.zoomFactor);
                                    viewport.setOrientation(viewportDef.orientation);
                                    viewport.setOffset(viewportDef.offsetX, viewportDef.offsetY);
                                }
                                views.add(view);
                            } else {
                                throw new Exception("Unknown raster data source: " + viewRef.productNodeName);
                            }
                        } else {
                            throw new Exception("Unknown product reference number: " + viewRef.productId);
                        }
                    } else {
                        throw new Exception("Unknown view type: " + viewRef.type);
                    }
                } catch (Exception e) {
                    problems.add(e);
                }
                pm.worked(1);
            }
        } finally {
            pm.done();
        }
        return views.toArray(new ProductNodeView[views.size()]);
    }

    public static Container getRootPaneContainer(JComponent component) {
        Container parent = component;
        Container lastParent;
        do {
            if (parent instanceof RootPaneContainer) {
                return parent;
            }
            lastParent = parent;
            parent = lastParent.getParent();
        } while (parent != null);
        return lastParent;
    }

    private Product getProductForRefNo(Product[] products, int refNo) {
        for (Product product : products) {
            if (product.getRefNo() == refNo) {
                return product;
            }
        }
        return null;
    }

    @XStreamAlias("product")
    public static class ProductRef {
        final int id;
        final File file;

        public ProductRef(int id, File file) {
            this.id = id;
            this.file = file;
        }
    }

    @XStreamAlias("view")
    public static class ViewRef {

        final int id;
        final String type;
        final Rectangle bounds;
        @XStreamAlias("viewport")
        final ViewportDef viewportDef;

        final int productId;
        final String productNodeName;


        public ViewRef(int id, String type, Rectangle bounds,
                       ViewportDef viewportDef, int productId,
                       String productNodeName) {
            this.id = id;
            this.type = type;
            this.bounds = bounds;
            this.viewportDef = viewportDef;
            this.productId = productId;
            this.productNodeName = productNodeName;
        }

        public int getLayerCount() {
            return 0;  // todo - impl. (nf)
        }

        public LayerRef getLayerRef(int index) {
            return null;  // todo - impl. (nf)
        }
    }

    @XStreamAlias("layer")
    public static class LayerRef {

        final String id;
        final String name;
        final boolean visible;
        final LayerConfiguration configuration;

        public LayerRef(String id, String name, boolean visible, LayerConfiguration configuration) {
            this.id = id;
            this.name = name;
            this.visible = visible;
            this.configuration = configuration;
        }
    }

    @XStreamAlias("configuration")
    public static class LayerConfiguration {
    }

    @XStreamAlias("viewport")
    public static class ViewportDef {
        final boolean modelYAxisDown;
        final double offsetX;
        final double offsetY;
        final double zoomFactor;
        final double orientation;

        public ViewportDef(boolean modelYAxisDown,
                           double offsetX,
                           double offsetY,
                           double zoomFactor,
                           double orientation) {
            this.modelYAxisDown = modelYAxisDown;
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            this.zoomFactor = zoomFactor;
            this.orientation = orientation;
        }
    }
}
