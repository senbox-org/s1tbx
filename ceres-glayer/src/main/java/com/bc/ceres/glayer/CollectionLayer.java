package com.bc.ceres.glayer;

import com.bc.ceres.binding.ValueContainer;


/**
 * A layer which can contain other layers.
 *
 * @author Marco Peters
 * @author Norman Fomferra
 * @version $revision$ $date$
 */
public class CollectionLayer extends Layer {

    private static final Type LAYER_TYPE = (Type) LayerType.getLayerType(Type.class.getName());

    public CollectionLayer() {
        this("Collection layer");
    }

    public CollectionLayer(String name) {
        this(LAYER_TYPE, name);
    }

    protected CollectionLayer(Type type, String name) {
        super(type);
        setName(name);
    }

    @Override
    public boolean isCollectionLayer() {
        return true;
    }

    public static class Type extends LayerType {

        @Override
        public String getName() {
            return "Collection Layer";
        }

        @Override
        public boolean isValidFor(LayerContext ctx) {
            return true;
        }

        @Override
        public ValueContainer getConfigurationTemplate() {
            return new ValueContainer();
        }

        @Override
        public Layer createLayer(LayerContext ctx, ValueContainer configuration) {
            return new CollectionLayer();
        }
    }
}
