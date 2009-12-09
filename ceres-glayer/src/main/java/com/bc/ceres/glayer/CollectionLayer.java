package com.bc.ceres.glayer;

import com.bc.ceres.binding.PropertyContainer;


/**
 * A layer which can contain other layers.
 *
 * @author Marco Peters
 * @author Norman Fomferra
 * @version $revision$ $date$
 */
public class CollectionLayer extends Layer {

    public CollectionLayer() {
        this("Collection Layer");
    }

    public CollectionLayer(String name) {
        this(type(), type().createLayerConfig(null), name);
    }

    public CollectionLayer(Type type, PropertyContainer configuration, String name) {
        super(type, configuration);
        setName(name);
    }

    @Override
    public boolean isCollectionLayer() {
        return true;
    }

    private static Type type() {
        return LayerTypeRegistry.getLayerType(Type.class);
    }

    public static class Type extends LayerType {

        @Override
        public boolean isValidFor(LayerContext ctx) {
            return true;
        }

        @Override
        public PropertyContainer createLayerConfig(LayerContext ctx) {
            return new PropertyContainer();
        }

        @Override
        public Layer createLayer(LayerContext ctx, PropertyContainer configuration) {
            return new CollectionLayer(this, configuration, "Collection Layer");
        }
    }

}
