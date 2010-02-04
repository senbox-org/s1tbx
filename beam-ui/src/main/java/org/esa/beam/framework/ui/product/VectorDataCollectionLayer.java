package org.esa.beam.framework.ui.product;

import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.core.Assert;
import com.bc.ceres.glayer.CollectionLayer;
import com.bc.ceres.glayer.Layer;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.datamodel.ProductNodeEvent;
import org.esa.beam.framework.datamodel.ProductNodeGroup;
import org.esa.beam.framework.datamodel.ProductNodeListener;
import org.esa.beam.framework.datamodel.VectorDataNode;
import static org.esa.beam.framework.ui.product.VectorDataLayerType.PROPERTY_NAME_VECTOR_DATA;

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

    private Layer getLayer(VectorDataNode vectorDataNode) {
        for (final Layer child : getChildren()) {
            final Object value = child.getConfiguration().getValue(PROPERTY_NAME_VECTOR_DATA);
            if (vectorDataNode == value) {
                return child;
            }
        }
        return null;
    }

    synchronized void updateChildren() {
        final ProductNodeGroup<VectorDataNode> vectorDataGroup = reference.get();
        if (vectorDataGroup == null) {
            return;
        }
        final Map<VectorDataNode, Layer> children = new HashMap<VectorDataNode, Layer>();
        List<Layer> childLayers = getChildren();
        for (final Layer child : childLayers) {
            final String name = (String) child.getConfiguration().getValue(PROPERTY_NAME_VECTOR_DATA);
            final VectorDataNode vectorDataNode = vectorDataGroup.get(name);
            children.put(vectorDataNode, child);
        }
        final Set<Layer> orphans = new HashSet<Layer>(children.values());
        VectorDataNode[] vectorDataNodes = vectorDataGroup.toArray(new VectorDataNode[vectorDataGroup.getNodeCount()]);
        for (final VectorDataNode vectorDataNode : vectorDataNodes) {
            final Layer child = children.get(vectorDataNode);
            if (child == null) {
                final Layer layer = createLayer(vectorDataNode);
                childLayers.add(layer);
            } else {
                orphans.remove(child);
            }
        }
        for (final Layer orphan : orphans) {
            childLayers.remove(orphan);
            orphan.dispose();
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
