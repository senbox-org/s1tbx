package com.bc.ceres.glayer;

import java.util.Map;



/**
 * A layer which can contain other layers.
 *
 * @author Marco Peters
 * @author Norman Fomferra
 * @version $revision$ $date$
 */
public class CollectionLayer extends Layer {
    private static final LayerType LAYER_TYPE = LayerType.getLayerType(CollectionLayer.Type.class.getName());

    public CollectionLayer() {
        this("Collection layer");
    }

    public CollectionLayer(String name) {
        this(LAYER_TYPE, name);
    }
    
    protected CollectionLayer(LayerType type, String name) {
        super(type, name);
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
        public Map<String, Object> createConfiguration(LayerContext ctx, Layer layer) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Layer createLayer(LayerContext ctx, Map<String, Object> configuration) {
            // TODO Auto-generated method stub
            return null;
        }
    }
}
