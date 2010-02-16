package com.bc.ceres.glayer;

import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertySet;


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

    public CollectionLayer(Type type, PropertySet configuration, String name) {
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
        
        private static final String TYPE_NAME = "CollectionLayerType";
        private static final String[] ALIASES = {"com.bc.ceres.glayer.CollectionLayer$Type"};

        @Override
        public String getName() {
            return TYPE_NAME;
        }
        
        @Override
        public String[] getAliases() {
            return ALIASES;
        }
        
        @Override
        public boolean isValidFor(LayerContext ctx) {
            return true;
        }

        @Override
        public PropertySet createLayerConfig(LayerContext ctx) {
            return new PropertyContainer();
        }

        @Override
        public Layer createLayer(LayerContext ctx, PropertySet configuration) {
            return new CollectionLayer(this, configuration, "Collection Layer");
        }
    }

}
