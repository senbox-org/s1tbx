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
import org.esa.beam.framework.datamodel.VectorData;
import static org.esa.beam.framework.ui.product.VectorDataLayerType.PROPERTY_NAME_VECTOR_DATA;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class VectorDataCollectionLayer extends CollectionLayer {

    public static final String ID = VectorDataCollectionLayer.class.getName();

    private final ProductNodeListener pnl;
    private final transient WeakReference<ProductNodeGroup<VectorData>> reference;

    public VectorDataCollectionLayer(VectorDataCollectionLayerType layerType,
                                     ProductNodeGroup<VectorData> vectorDataGroup,
                                     PropertySet configuration) {
        super(layerType, configuration, "Vector Data Layers");
        Assert.notNull(vectorDataGroup, "vectorDataGroup");

        reference = new WeakReference<ProductNodeGroup<VectorData>>(vectorDataGroup);
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

    private Layer createLayer(final VectorData vectorData) {
        return VectorDataLayerType.createLayer(vectorData);
    }

    private Layer getLayer(VectorData vectorData) {
        for (final Layer child : getChildren()) {
            final Object value = child.getConfiguration().getValue(PROPERTY_NAME_VECTOR_DATA);
            if (vectorData == value) {
                return child;
            }
        }
        return null;
    }

    synchronized void updateChildren() {
        final ProductNodeGroup<VectorData> vectorDataGroup = reference.get();
        final Map<VectorData, Layer> children = new HashMap<VectorData, Layer>();
        for (final Layer child : getChildren()) {
            final String name = (String) child.getConfiguration().getValue(PROPERTY_NAME_VECTOR_DATA);
            final VectorData vectorData = vectorDataGroup.get(name);
            children.put(vectorData, child);
        }
        final Set<Layer> orphans = new HashSet<Layer>(children.values());
        for (final VectorData vectorData : vectorDataGroup.toArray(new VectorData[vectorDataGroup.getNodeCount()])) {
            final Layer child = children.get(vectorData);
            if (child == null) {
                final Layer layer = createLayer(vectorData);
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
            if (sourceNode instanceof VectorData) {
                final VectorData vectorData = (VectorData) sourceNode;
                final Layer layer = getLayer(vectorData);
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
