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
    
    /**
     * @param layerType
     * @param name
     */
    protected CollectionLayer(LayerType layerType, String name) {
        super(layerType, name);
    }

    @Override
    public boolean isCollectionLayer() {
        return true;
    }
    
    public static class CollectionLayerType extends LayerType {
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
