package org.esa.beam.framework.ui.product;

import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.core.Assert;
import com.bc.ceres.glayer.CollectionLayer;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerFilter;
import com.bc.ceres.glayer.support.LayerUtils;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.datamodel.ProductNodeEvent;
import org.esa.beam.framework.datamodel.ProductNodeGroup;
import org.esa.beam.framework.datamodel.ProductNodeListener;
import org.esa.beam.framework.datamodel.VectorDataNode;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class VectorDataCollectionLayer extends CollectionLayer {

    public static final String ID = VectorDataCollectionLayer.class.getName();

    private final ProductNodeListener pnl;
    private final transient WeakReference<ProductNodeGroup<VectorDataNode>> reference;

    public VectorDataCollectionLayer(VectorDataCollectionLayerType layerType,
                                     ProductNodeGroup<VectorDataNode> vectorDataGroup,
                                     PropertySet configuration) {
        super(layerType, configuration, "Geometries");
        Assert.notNull(vectorDataGroup, "vectorDataGroup");

        reference = new WeakReference<ProductNodeGroup<VectorDataNode>>(vectorDataGroup);
        pnl = new PNL();

        setId(ID);
        vectorDataGroup.getProduct().addProductNodeListener(pnl);
    }

    @Override
    public void disposeLayer() {
        ProductNodeGroup<VectorDataNode> productNodeGroup = reference.get();
        if (productNodeGroup != null) {
            Product product = productNodeGroup.getProduct();
            if (product != null) {
                product.removeProductNodeListener(pnl);
            }
        }
        reference.clear();
    }

    private Layer createLayer(final VectorDataNode vectorDataNode) {
        final Layer layer = VectorDataLayerType.createLayer(vectorDataNode);
        layer.setVisible(false);
        return layer;
    }

    private Layer getLayer(final VectorDataNode vectorDataNode) {
        LayerFilter layerFilter = VectorDataLayerFilterFactory.createNodeFilter(vectorDataNode);
        return LayerUtils.getChildLayer(LayerUtils.getRootLayer(this), LayerUtils.SEARCH_DEEP, layerFilter);
    }

    synchronized void updateChildren() {
        final ProductNodeGroup<VectorDataNode> vectorDataGroup = reference.get();
        if (vectorDataGroup == null) {
            return;
        }
        // Collect all current vector layers
        LayerFilter layerFilter = new LayerFilter() {
            @Override
            public boolean accept(Layer layer) {
                PropertySet conf = layer.getConfiguration();
                return conf.isPropertyDefined(VectorDataLayerType.PROPERTY_NAME_VECTOR_DATA) && conf.getValue(VectorDataLayerType.PROPERTY_NAME_VECTOR_DATA) != null;
            }
        };
        List<Layer> vectorLayers = LayerUtils.getChildLayers(LayerUtils.getRootLayer(this), LayerUtils.SEARCH_DEEP, layerFilter);
        final Map<VectorDataNode, Layer> currentLayers = new HashMap<VectorDataNode, Layer>();
        for (final Layer child : vectorLayers) {
            final String name = (String) child.getConfiguration().getValue(VectorDataLayerType.PROPERTY_NAME_VECTOR_DATA);
            final VectorDataNode vectorDataNode = vectorDataGroup.get(name);
            currentLayers.put(vectorDataNode, child);
        }

        // Allign vector layers with available vectors
        final Set<Layer> unusedLayers = new HashSet<Layer>(vectorLayers);
        VectorDataNode[] vectorDataNodes = vectorDataGroup.toArray(new VectorDataNode[vectorDataGroup.getNodeCount()]);
        for (final VectorDataNode vectorDataNode : vectorDataNodes) {
            Layer layer = currentLayers.get(vectorDataNode);
            if (layer != null) {
                unusedLayers.remove(layer);
            } else {
                layer = createLayer(vectorDataNode);
                getChildren().add(layer);
            }
        }

        // Remove unused layers
        for (Layer layer : unusedLayers) {
            layer.dispose();
            Layer layerParent = layer.getParent();
            if (layerParent != null) {
                layerParent.getChildren().remove(layer);
            }
        }
    }

    private class PNL implements ProductNodeListener {

        @Override
        public synchronized void nodeChanged(ProductNodeEvent event) {
            final ProductNode sourceNode = event.getSourceNode();
            if (sourceNode instanceof VectorDataNode) {
                final VectorDataNode vectorDataNode = (VectorDataNode) sourceNode;
                final Layer layer = getLayer(vectorDataNode);
                if (layer != null) {
                    layer.regenerate();
                }
            }
        }

        @Override
        public void nodeDataChanged(ProductNodeEvent event) {
            nodeChanged(event);
        }

        @Override
        public void nodeAdded(ProductNodeEvent event) {
            updateChildren();
        }

        @Override
        public void nodeRemoved(ProductNodeEvent event) {
            updateChildren();
        }
    }
}
