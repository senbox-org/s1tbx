package org.esa.beam.visat.actions.session;

import com.bc.ceres.binding.ClassFieldDescriptorFactory;
import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.Converter;
import com.bc.ceres.binding.ConverterRegistry;
import com.bc.ceres.binding.ValueContainer;
import com.bc.ceres.binding.ValueDescriptor;
import com.bc.ceres.binding.dom.DefaultDomConverter;
import com.bc.ceres.binding.dom.DefaultDomElement;
import com.bc.ceres.binding.dom.DomConverter;
import com.bc.ceres.binding.dom.DomElement;
import com.bc.ceres.binding.dom.DomElementXStreamConverter;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerContext;
import com.bc.ceres.grender.Viewport;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamConverter;

import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.datamodel.VirtualBand;
import org.esa.beam.framework.ui.product.ProductMetadataView;
import org.esa.beam.framework.ui.product.ProductNodeView;
import org.esa.beam.framework.ui.product.ProductSceneImage;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.util.PropertyMap;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.awt.Container;
import java.awt.Rectangle;
import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.RootPaneContainer;

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

        registerConverters();

        viewRefs = new ViewRef[views.length];
        for (int i = 0; i < views.length; i++) {
            ProductNodeView view = views[i];
            ViewportDef viewportDef = null;
            LayerRef[] layerRefs = new LayerRef[0];
            if (view instanceof ProductSceneView) {
                final ProductSceneView sceneView = (ProductSceneView) view;
                final Viewport viewport = sceneView.getLayerCanvas().getViewport();
                viewportDef = new ViewportDef(viewport.isModelYAxisDown(),
                                              viewport.getOffsetX(),
                                              viewport.getOffsetY(),
                                              viewport.getZoomFactor(),
                                              viewport.getOrientation());
                final List<Layer> layers = sceneView.getRootLayer().getChildren();

                final LayerContext layerContext = new LayerContext() {
                    @Override
                    public CoordinateReferenceSystem getCoordinateReferenceSystem() {
                        return sceneView.getRaster().getProduct().getGeoCoding().getModelCRS();
                    }

                    @Override
                    public Layer getRootLayer() {
                        return sceneView.getRootLayer();
                    }
                };
                layerRefs = getLayerRefs(layerContext, layers);
            }

            Rectangle viewBounds = new Rectangle(0, 0, 200, 200);
            if (view instanceof JComponent) {
                viewBounds = getRootPaneContainer((JComponent) view).getBounds();
            }
            String productNodeName = "";
            if (view instanceof ProductSceneView) {
                productNodeName = view.getVisibleProductNode().getName();
            } else if (view instanceof ProductMetadataView) {
                ProductMetadataView metadataView = (ProductMetadataView) view;
                MetadataElement metadataRoot = metadataView.getProduct().getMetadataRoot();
                MetadataElement metadataElement = metadataView.getMetadataElement();
                StringBuilder sb = new StringBuilder(metadataElement.getName());
                ProductNode owner = metadataElement.getOwner();
                while(owner != metadataRoot) {
                    sb.append('|');
                    sb.append(owner.getName());
                    owner = owner.getOwner();
                }
                productNodeName = sb.toString();
            }
            viewRefs[i] = new ViewRef(i,
                                      view.getClass().getName(),
                                      viewBounds,
                                      viewportDef,
                                      view.getVisibleProductNode().getProduct().getRefNo(),
                                      productNodeName,
                                      layerRefs);
        }
    }

    private static LayerRef[] getLayerRefs(LayerContext layerContext, List<Layer> layers) {
        final LayerRef[] layerRefs = new LayerRef[layers.size()];
        for (int i = 0; i < layers.size(); i++) {
            Layer layer = layers.get(i);
            final ValueContainer configuration = getConfiguration(layerContext, layer);
            final ClassFieldDescriptorFactory factory = new ClassFieldDescriptorFactory() {
                @Override
                public ValueDescriptor createValueDescriptor(Field field) {
                    return new ValueDescriptor(field.getName(), field.getType());
                }
            };
            final DomConverter dc = new DefaultDomConverter(ValueContainer.class, factory) {
                @Override
                protected ValueContainer getValueContainer(Object value) {
                    if (value instanceof ValueContainer) {
                        return (ValueContainer) value;
                    }
                    return super.getValueContainer(value);
                }
            };
            final DefaultDomElement element = new DefaultDomElement("configuration");
            dc.convertValueToDom(configuration, element);
            layerRefs[i] = new LayerRef(layer.getLayerType().getName(),
                                        layer.getId(),
                                        layer.getName(),
                                        layer.isVisible(),
                                        element,
                                        getLayerRefs(layerContext, layer.getChildren()));
        }
        return layerRefs;
    }

    private static ValueContainer getConfiguration(LayerContext ctx, Layer layer) {
        return layer.getLayerType().getConfigurationCopy(ctx, layer);
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

    public RestoredSession restore(ProgressMonitor pm, ProblemSolver problemSolver) {
        try {
            pm.beginTask("Restoring session", 100);
            final ArrayList<Exception> problems = new ArrayList<Exception>();
            final Product[] products = restoreProducts(SubProgressMonitor.create(pm, 80), problemSolver, problems);
            final ProductNodeView[] views = restoreViews(products, SubProgressMonitor.create(pm, 20), problems);
            return new RestoredSession(products, views, problems.toArray(new Exception[problems.size()]));
        } finally {
            pm.done();
        }
    }

    Product[] restoreProducts(ProgressMonitor pm, ProblemSolver problemSolver, List<Exception> problems) {
        final ArrayList<Product> products = new ArrayList<Product>();
        try {
            pm.beginTask("Restoring products", productRefs.length);
            for (ProductRef productRef : productRefs) {
                try {
                    final Product product;
                    if (productRef.file.exists()) {
                        product = ProductIO.readProduct(productRef.file, null);
                    } else {
                        product = problemSolver.solveProductNotFound(productRef.file);
                        if (product == null) {
                            throw new Exception("Product [" + productRef.id + "] not found.");
                        }
                    }
                    products.add(product);
                    product.setRefNo(productRef.id);
                } catch (Exception e) {
                    problems.add(e);
                } finally {
                    pm.worked(1);
                }
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
                                final ProductSceneView view = new ProductSceneView(
                                        new ProductSceneImage(node, new PropertyMap(),
                                                              SubProgressMonitor.create(pm, 1)));
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
                    } else if (ProductMetadataView.class.getName().equals(viewRef.type)) {
                        Product product = getProductForRefNo(restoredProducts, viewRef.productId);
                        if (product != null) {
                            String[] productNodeNames = viewRef.productNodeName.split("\\|");
                            MetadataElement element = product.getMetadataRoot();
                            for (int i = productNodeNames.length - 1; i >= 0; i--) {
                                if (element == null) {
                                    break;
                                }
                                element = element.getElement(productNodeNames[i]);
                            }
                            if (element != null) {
                                ProductMetadataView view = new ProductMetadataView(element);
                                Rectangle bounds = viewRef.bounds;
                                if (bounds != null && !bounds.isEmpty()) {
                                    view.setBounds(bounds);
                                }
                                views.add(view);
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

    public static interface ProblemSolver {
        Product solveProductNotFound(File file);
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
        @XStreamAlias("layers")
        final LayerRef[] layerRefs;


        public ViewRef(int id, String type, Rectangle bounds,
                       ViewportDef viewportDef, int productId,
                       String productNodeName, LayerRef[] layerRefs) {
            this.id = id;
            this.type = type;
            this.bounds = bounds;
            this.viewportDef = viewportDef;
            this.productId = productId;
            this.productNodeName = productNodeName;
            this.layerRefs = layerRefs;
        }

        public int getLayerCount() {
            return layerRefs.length;
        }

        public LayerRef getLayerRef(int index) {
            return layerRefs[index];
        }
    }

    @XStreamAlias("layer")
    public static class LayerRef {

        @XStreamAlias("type")
        final String typeName;
        final String id;
        final String name;
        final boolean visible;
        @XStreamConverter(DomElementXStreamConverter.class)
        final DomElement configuration;
        final LayerRef[] children;
        
        public LayerRef(String typeName, String id, String name, boolean visible, DomElement configuration,
                        LayerRef[] children) {
            this.typeName = typeName;
            this.id = id;
            this.name = name;
            this.visible = visible;
            this.configuration = configuration;
            this.children = children;
        }
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

    // TODO: implement converters - without converters dom converter recurrs infinitely
    private static void registerConverters() {
        ConverterRegistry.getInstance().setConverter(RasterDataNode.class, new Converter<RasterDataNode>(){
            @Override
            public Class<? extends RasterDataNode> getValueType() {
                return RasterDataNode.class;
            }

            @Override
            public RasterDataNode parse(String text) throws ConversionException {
                return new VirtualBand(text, ProductData.TYPE_INT32, 10, 10);
            }

            @Override
            public String format(RasterDataNode value) {
                return value.getName();
            }
        });
    }
}
