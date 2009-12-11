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
        getProduct().addProductNodeListener(pnl);
    }

    @Override
    public void disposeLayer() {
        if (reference.get() != null) {
            getProduct().removeProductNodeListener(pnl);
            reference.clear();
        }
    }

    private Product getProduct() {
        return reference.get().getProduct();
    }

    private Layer createLayer(final VectorDataNode vectorDataNode) {
        return VectorDataLayerType.createLayer(vectorDataNode);
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
        final Map<VectorDataNode, Layer> children = new HashMap<VectorDataNode, Layer>();
        for (final Layer child : getChildren()) {
            final String name = (String) child.getConfiguration().getValue(PROPERTY_NAME_VECTOR_DATA);
            final VectorDataNode vectorDataNode = vectorDataGroup.get(name);
            children.put(vectorDataNode, child);
        }
        final Set<Layer> orphans = new HashSet<Layer>(children.values());
        for (final VectorDataNode vectorDataNode : vectorDataGroup.toArray(new VectorDataNode[vectorDataGroup.getNodeCount()])) {
            final Layer child = children.get(vectorDataNode);
            if (child == null) {
                final Layer layer = createLayer(vectorDataNode);
                getChildren().add(layer);
            } else {
                orphans.remove(child);
            }
        }
        for (final Layer orphan : orphans) {
            getChildren().remove(orphan);
            orphan.dispose();
        }
    }

    public class PNL implements ProductNodeListener {

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
