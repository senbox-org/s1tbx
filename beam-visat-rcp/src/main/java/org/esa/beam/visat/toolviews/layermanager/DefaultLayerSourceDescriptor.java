package org.esa.beam.visat.toolviews.layermanager;

import com.bc.ceres.glayer.LayerType;
import org.esa.beam.visat.toolviews.layermanager.layersrc.SimpleLayerSource;

@SuppressWarnings({"UnusedDeclaration"})
public class DefaultLayerSourceDescriptor implements LayerSourceDescriptor {

    private String id;
    private String name;
    private String description;
    private Class<? extends LayerSource> layerSourceClass;
    private Class<? extends LayerType> layerTypeClass;
    private LayerType layerType;


    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public LayerSource createLayerSource() {
        if (layerSourceClass == null) {
            return new SimpleLayerSource(getLayerType());
        }
        try {
            return layerSourceClass.newInstance();
        } catch (Exception e) {
            String message = String.format("Could not create instance of class [%s]", layerSourceClass.getName());
            throw new IllegalStateException(message, e);
        }
    }

    @Override
    public synchronized LayerType getLayerType() {
        if (layerTypeClass == null) {
            return null;
        }
        if (layerType == null) {
            try {
                layerType = layerTypeClass.newInstance();
            } catch (Exception e) {
                String message = String.format("Could not create instance of class [%s]", layerTypeClass.getName());
                throw new IllegalStateException(message, e);
            }
        }
        return layerType;
    }


}
