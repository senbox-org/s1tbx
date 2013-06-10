/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.accessors.DefaultPropertyAccessor;
import com.bc.ceres.binding.dom.DefaultDomElement;
import com.bc.ceres.binding.dom.DomElement;
import com.bc.ceres.binding.dom.DomElementXStreamConverter;
import com.bc.ceres.core.CanceledException;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerContext;
import com.bc.ceres.glayer.LayerType;
import com.bc.ceres.glayer.LayerTypeRegistry;
import com.bc.ceres.grender.Viewport;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import com.thoughtworks.xstream.converters.SingleValueConverterWrapper;
import com.thoughtworks.xstream.converters.basic.AbstractSingleValueConverter;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.ProductManager;
import org.esa.beam.framework.datamodel.RGBImageProfile;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.datamodel.VirtualBand;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.ui.product.ProductMetadataView;
import org.esa.beam.framework.ui.product.ProductNodeView;
import org.esa.beam.framework.ui.product.ProductSceneImage;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.glayer.MaskCollectionLayerType;
import org.esa.beam.util.PropertyMap;
import org.esa.beam.util.io.FileUtils;
import org.esa.beam.visat.actions.session.dom.SessionDomConverter;

import javax.swing.JComponent;
import javax.swing.RootPaneContainer;
import java.awt.Container;
import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/**
 * Data container used for storing/restoring BEAM sessions.
 *
 * @author Ralf Quast
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 * @since BEAM 4.6
 */
@XStreamAlias("session")
public class Session {

    public static String CURRENT_MODEL_VERSION = "1.0.0";

    String modelVersion;
    @XStreamAlias("products")
    ProductRef[] productRefs;
    @XStreamAlias("views")
    ViewRef[] viewRefs;

    /**
     * No-arg constructor required by XStream.
     */
    @SuppressWarnings("UnusedDeclaration")
    public Session() {
    }

    public Session(URI rootURI, Product[] products, ProductNodeView[] views) {
        modelVersion = CURRENT_MODEL_VERSION;

        productRefs = new ProductRef[products.length];
        for (int i = 0; i < products.length; i++) {
            Product product = products[i];
            URI relativeProductURI = getFileLocationURI(rootURI, product);
            productRefs[i] = new ProductRef(product.getRefNo(), relativeProductURI);
        }

        ProductManager productManager = new ProductManager();
        for (Product product : products) {
            productManager.addProduct(product);
        }

        viewRefs = new ViewRef[views.length];
        for (int i = 0; i < views.length; i++) {
            ProductNodeView view = views[i];
            ViewportDef viewportDef = null;
            LayerRef[] layerRefs = new LayerRef[0];
            if (view instanceof ProductSceneView) {
                ProductSceneView sceneView = (ProductSceneView) view;
                Viewport viewport = sceneView.getLayerCanvas().getViewport();
                viewportDef = new ViewportDef(viewport.isModelYAxisDown(),
                                              viewport.getOffsetX(),
                                              viewport.getOffsetY(),
                                              viewport.getZoomFactor(),
                                              viewport.getOrientation());
                List<Layer> layers = sceneView.getRootLayer().getChildren();
                layerRefs = getLayerRefs(layers, productManager);
            }

            Rectangle viewBounds = new Rectangle(0, 0, 200, 200);
            if (view instanceof JComponent) {
                viewBounds = getRootPaneContainer((JComponent) view).getBounds();
            }
            String productNodeName = null;
            String viewName = null;
            String expressionR = null;
            String expressionG = null;
            String expressionB = null;
            int productRefNo = 0;

            if (view instanceof ProductSceneView) {
                ProductSceneView psv = (ProductSceneView) view;
                if (psv.isRGB()) {
                    viewName = psv.getSceneName();

                    RasterDataNode[] rasters = psv.getRasters();
                    expressionR = getExpression(rasters[0]);
                    expressionG = getExpression(rasters[1]);
                    expressionB = getExpression(rasters[2]);

                    productRefNo = rasters[0].getProduct().getRefNo();
                } else {
                    productNodeName = view.getVisibleProductNode().getName();
                    productRefNo = view.getVisibleProductNode().getProduct().getRefNo();
                }
            } else if (view instanceof ProductMetadataView) {
                ProductMetadataView metadataView = (ProductMetadataView) view;
                MetadataElement metadataRoot = metadataView.getProduct().getMetadataRoot();
                MetadataElement metadataElement = metadataView.getMetadataElement();
                StringBuilder sb = new StringBuilder(metadataElement.getName());
                MetadataElement parent = metadataElement.getParentElement();
                while (parent != null && parent != metadataRoot) {
                    sb.append('|');
                    sb.append(parent.getName());
                    parent = parent.getParentElement();
                }
                productNodeName = sb.toString();
                productRefNo = view.getVisibleProductNode().getProduct().getRefNo();
                // todo - flag and index coding views (rq-20100618)
            }
            viewRefs[i] = new ViewRef(i,
                                      view.getClass().getName(),
                                      viewBounds,
                                      viewportDef,
                                      productRefNo,
                                      productNodeName,
                                      viewName,
                                      expressionR,
                                      expressionG,
                                      expressionB,
                                      layerRefs);
        }
    }

    // todo - code duplication in RgbImageLayerType.java (nf 10.2009)
    private static String getExpression(RasterDataNode raster) {
        Product product = raster.getProduct();
        if (product != null) {
            if (product.containsBand(raster.getName())) {
                return raster.getName();
            } else {
                if (raster instanceof VirtualBand) {
                    return ((VirtualBand) raster).getExpression();
                }
            }
        }
        return null;
    }

    private static URI getFileLocationURI(URI rootURI, Product product) {
        File file = product.getFileLocation();
        return FileUtils.getRelativeUri(rootURI, file);
    }

    private static LayerRef[] getLayerRefs(List<Layer> layers, ProductManager productManager) {
        ArrayList<LayerRef> layerRefs = new ArrayList<LayerRef>(layers.size());
        for (int i = 0; i < layers.size(); i++) {
            Layer layer = layers.get(i);
            if (isSerializableLayer(layer)) {
                PropertySet configuration = layer.getConfiguration();
                // todo - check - why create a configuration copy here?! (nf, 10.2009)
                PropertySet configurationCopy = getConfigurationCopy(configuration);
                SessionDomConverter domConverter = new SessionDomConverter(productManager);
                DomElement element = new DefaultDomElement("configuration");
                try {
                    domConverter.convertValueToDom(configurationCopy, element);
                } catch (ConversionException e) {
                    e.printStackTrace();
                }
                layerRefs.add(new LayerRef(layer, i, element,
                                           getLayerRefs(layer.getChildren(), productManager)));
            }
        }
        return layerRefs.toArray(new LayerRef[layerRefs.size()]);
    }

    private static boolean isSerializableLayer(Layer layer) {
        // todo - check, this could be solved in a generic way (nf, 10.2009)
        return !(layer.getLayerType() instanceof MaskCollectionLayerType);
    }


    private static PropertyContainer getConfigurationCopy(PropertySet propertyContainer) {
        PropertyContainer configuration = new PropertyContainer();

        for (Property model : propertyContainer.getProperties()) {
            PropertyDescriptor descriptor = new PropertyDescriptor(model.getDescriptor());
            DefaultPropertyAccessor valueAccessor = new DefaultPropertyAccessor();
            valueAccessor.setValue(model.getValue());
            configuration.addProperty(new Property(descriptor, valueAccessor));
        }

        return configuration;
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

    public RestoredSession restore(AppContext appContext, URI rootURI, ProgressMonitor pm,
                                   ProblemSolver problemSolver) throws
            CanceledException {
        try {
            pm.beginTask("Restoring session", 100);
            ArrayList<Exception> problems = new ArrayList<Exception>();
            ProductManager productManager = restoreProducts(rootURI, SubProgressMonitor.create(pm, 80),
                                                            problemSolver, problems);
            // Note: ProductManager is used for the SessionDomConverter
            ProductNodeView[] views = restoreViews(productManager, appContext.getPreferences(), SubProgressMonitor.create(pm, 20), problems
            );
            return new RestoredSession(productManager.getProducts(),
                                       views,
                                       problems.toArray(new Exception[problems.size()]));
        } finally {
            pm.done();
        }
    }

    ProductManager restoreProducts(URI rootURI, ProgressMonitor pm, ProblemSolver problemSolver,
                                   List<Exception> problems) throws CanceledException {
        ProductManager productManager = new ProductManager();
        try {
            pm.beginTask("Restoring products", productRefs.length);
            for (ProductRef productRef : productRefs) {
                try {
                    Product product;
                    File productFile = new File(rootURI.resolve(productRef.uri));
                    if (productFile.exists()) {
                        product = ProductIO.readProduct(productFile);
                    } else {
                        product = problemSolver.solveProductNotFound(productRef.refNo, productFile);
                        if (product == null) {
                            throw new IOException("Product [" + productRef.refNo + "] not found.");
                        }
                    }
                    product.setRefNo(productRef.refNo);
                    productManager.addProduct(product);
                } catch (IOException e) {
                    problems.add(e);
                } finally {
                    pm.worked(1);
                }
            }
        } finally {
            pm.done();
        }

        return productManager;
    }

    private ProductNodeView[] restoreViews(ProductManager productManager,
                                           PropertyMap applicationPreferences,
                                           ProgressMonitor pm,
                                           List<Exception> problems) {
        ArrayList<ProductNodeView> views = new ArrayList<ProductNodeView>();
        try {
            pm.beginTask("Restoring views", viewRefs.length);
            for (ViewRef viewRef : viewRefs) {
                try {
                    if (ProductSceneView.class.getName().equals(viewRef.type)) {
                        collectSceneView(viewRef, productManager, applicationPreferences, pm, problems, views);
                    } else if (ProductMetadataView.class.getName().equals(viewRef.type)) {
                        collectMetadataView(viewRef, productManager, views);
                        // todo - flag and index coding views (rq-20100618)
                    } else {
                        throw new Exception("Unknown view type: " + viewRef.type);
                    }
                } catch (Exception e) {
                    problems.add(e);
                } finally {
                    pm.worked(1);
                }
            }
        } finally {
            pm.done();
        }
        return views.toArray(new ProductNodeView[views.size()]);
    }

    private static void collectSceneView(ViewRef viewRef,
                                         ProductManager productManager,
                                         PropertyMap applicationPreferences,
                                         ProgressMonitor pm,
                                         List<Exception> problems,
                                         List<ProductNodeView> views) throws Exception {
        ProductSceneView view = createSceneView(viewRef, productManager, applicationPreferences, pm);
        views.add(view);
        for (int i = 0; i < viewRef.getLayerCount(); i++) {
            LayerRef ref = viewRef.getLayerRef(i);
            if (isBaseImageLayerRef(view, ref)) {
                // The BaseImageLayer is not restored by LayerRef, so we have to adjust
                // transparency and visibility  manually
                view.getBaseImageLayer().setTransparency(ref.transparency);
                view.getBaseImageLayer().setVisible(ref.visible);
            } else {
                try {
                    addLayerRef(view, view.getRootLayer(), ref, productManager);
                } catch (Exception e) {
                    problems.add(e);
                }
            }
        }
    }

    private static boolean isBaseImageLayerRef(ProductSceneView view, LayerRef ref) {
        return view.getBaseImageLayer().getId().equals(ref.id);
    }

    private static ProductSceneView createSceneView(ViewRef viewRef,
                                                    ProductManager productManager,
                                                    PropertyMap applicationPreferences,
                                                    ProgressMonitor pm) throws Exception {
        Product product = productManager.getProductByRefNo(viewRef.productRefNo);
        if (product == null) {
            throw new Exception("Unknown product reference number: " + viewRef.productRefNo);
        }
        ProductSceneImage sceneImage;
        if (viewRef.productNodeName != null) {
            RasterDataNode node = product.getRasterDataNode(viewRef.productNodeName);
            if (node != null) {
                sceneImage = new ProductSceneImage(node, applicationPreferences,
                                                   SubProgressMonitor.create(pm, 1));
            } else {
                throw new Exception("Unknown raster data source: " + viewRef.productNodeName);
            }
        } else {
            Band rBand = getRgbBand(product, viewRef.expressionR,
                                    RGBImageProfile.RGB_BAND_NAMES[0]);
            Band gBand = getRgbBand(product, viewRef.expressionG,
                                    RGBImageProfile.RGB_BAND_NAMES[1]);
            Band bBand = getRgbBand(product, viewRef.expressionB,
                                    RGBImageProfile.RGB_BAND_NAMES[2]);
            sceneImage = new ProductSceneImage(viewRef.viewName, rBand, gBand, bBand,
                                               applicationPreferences,
                                               SubProgressMonitor.create(pm, 1));
        }

        ProductSceneView view = new ProductSceneView(sceneImage);
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
        view.setLayerProperties(applicationPreferences);
        return view;
    }

    private static void collectMetadataView(ViewRef viewRef,
                                            ProductManager productManager,
                                            ArrayList<ProductNodeView> views) throws Exception {
        Product product = productManager.getProductByRefNo(viewRef.productRefNo);
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
                ProductMetadataView metadataView = new ProductMetadataView(element);
                Rectangle bounds = viewRef.bounds;
                if (bounds != null && !bounds.isEmpty()) {
                    metadataView.setBounds(bounds);
                }
                views.add(metadataView);
            }
        } else {
            throw new Exception("Unknown product reference number: " + viewRef.productRefNo);
        }
    }

    private static void addLayerRef(LayerContext layerContext,
                                    Layer parentLayer,
                                    LayerRef layerRef,
                                    ProductManager productManager) throws ConversionException, ValidationException {
        LayerType type = LayerTypeRegistry.getLayerType(layerRef.layerTypeName);
        if (type != null) {
            SessionDomConverter converter = new SessionDomConverter(productManager);
            PropertySet template = type.createLayerConfig(layerContext);
            converter.convertDomToValue(layerRef.configuration, template);
            Layer layer = type.createLayer(layerContext, template);
            layer.setId(layerRef.id);
            layer.setVisible(layerRef.visible);
            layer.setTransparency(layerRef.transparency);
            layer.setName(layerRef.name);
            parentLayer.getChildren().add(layerRef.zOrder, layer);
            for (LayerRef child : layerRef.children) {
                addLayerRef(layerContext, layer, child, productManager);
            }
        }
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

    private static Product getProductForRefNo(Product[] products, int refNo) {
        for (Product product : products) {
            if (product.getRefNo() == refNo) {
                return product;
            }
        }
        return null;
    }

    public static interface ProblemSolver {

        Product solveProductNotFound(int id, File file) throws CanceledException;
    }

    @XStreamAlias("product")
    public static class ProductRef {

        int refNo;
        @XStreamConverter(URIConverterWrapper.class)
        URI uri;

        /**
         * No-arg constructor required by XStream.
         */
        @SuppressWarnings("UnusedDeclaration")
        public ProductRef() {
        }

        public ProductRef(int refNo, URI uri) {
            this.refNo = refNo;
            this.uri = uri;
        }
    }

    @XStreamAlias("view")
    public static class ViewRef {

        int id;
        String type;
        Rectangle bounds;
        @XStreamAlias("viewport")
        ViewportDef viewportDef;

        int productRefNo;
        String productNodeName;
        String viewName;
        String expressionR;
        String expressionG;
        String expressionB;

        @XStreamAlias("layers")
        LayerRef[] layerRefs;

        /**
         * No-arg constructor required by XStream.
         */
        @SuppressWarnings("UnusedDeclaration")
        public ViewRef() {
        }

        public ViewRef(int id, String type, Rectangle bounds,
                       ViewportDef viewportDef, int productRefNo,
                       String productNodeName, String viewName, String expressionR, String expressionG,
                       String expressionB,
                       LayerRef[] layerRefs) {
            this.id = id;
            this.type = type;
            this.bounds = bounds;
            this.viewportDef = viewportDef;
            this.productRefNo = productRefNo;
            this.productNodeName = productNodeName;
            this.viewName = viewName;
            this.expressionR = expressionR;
            this.expressionG = expressionG;
            this.expressionB = expressionB;
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
        String layerTypeName;
        String id;
        String name;
        boolean visible;
        double transparency;
        int zOrder;
        @XStreamConverter(DomElementXStreamConverter.class)
        DomElement configuration;
        LayerRef[] children;

        /**
         * No-arg constructor required by XStream.
         */
        @SuppressWarnings("UnusedDeclaration")
        public LayerRef() {
        }

        public LayerRef(Layer layer, int zOrder, DomElement configuration, LayerRef[] children) {
            this.layerTypeName = layer.getLayerType().getName();
            this.id = layer.getId();
            this.name = layer.getName();
            this.visible = layer.isVisible();
            this.transparency = layer.getTransparency();
            this.zOrder = zOrder;
            this.configuration = configuration;
            this.children = children;
        }
    }

    @XStreamAlias("viewport")
    public static class ViewportDef {

        boolean modelYAxisDown;
        double offsetX;
        double offsetY;
        double zoomFactor;
        double orientation;

        /**
         * No-arg constructor required by XStream.
         */
        @SuppressWarnings("UnusedDeclaration")
        public ViewportDef() {
        }

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

    private static Band getRgbBand(Product product, String expression, String bandName) {
        Band band = null;
        if (expression != null && !expression.isEmpty()) {
            band = product.getBand(expression);
        }
        if (band == null) {
            if (expression == null || expression.isEmpty()) {
                expression = "0.0";
            }
            band = new Channel(bandName, product, expression);
        }

        return band;
    }

    private static class Channel extends VirtualBand {

        public Channel(String name, Product product, String expression) {
            super(name, ProductData.TYPE_FLOAT32, product.getSceneRasterWidth(), product.getSceneRasterHeight(),
                  expression);
            setOwner(product);
        }
    }

    public static class URIConverterWrapper extends SingleValueConverterWrapper {

        public URIConverterWrapper() {
            super(new URIConverter());
        }
    }

    public static class URIConverter extends AbstractSingleValueConverter {

        @Override
        public boolean canConvert(Class type) {
            return type.equals(URI.class);
        }

        @Override
        public Object fromString(String str) {
            try {
                return new URI(str);
            } catch (URISyntaxException e) {
                throw new com.thoughtworks.xstream.converters.ConversionException(e);
            }
        }
    }
}
