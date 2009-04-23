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
        this(LAYER_TYPE.getName());
    }

    public CollectionLayer(String name) {
        this(LAYER_TYPE, LAYER_TYPE.getConfigurationTemplate(), name);
    }

    public CollectionLayer(Type type, ValueContainer configuration, String name) {
        super(type, configuration);
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
        protected Layer createLayerImpl(LayerContext ctx, ValueContainer configuration) {
            return new CollectionLayer(this, configuration, "Collection layer");
        }
    }
}
