package org.esa.beam.visat.toolviews.layermanager;

import com.bc.ceres.glayer.LayerType;
import org.esa.beam.visat.toolviews.layermanager.layersrc.SimpleLayerSourceController;

@SuppressWarnings({"UnusedDeclaration"})
public class DefaultLayerSourceDescriptor implements LayerSourceDescriptor {

    private String id;
    private String name;
    private String description;
    private Class<? extends LayerSourceController> controllerClass;
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
    public LayerSourceController createController() {
        if (controllerClass == null) {
            return new SimpleLayerSourceController(getLayerType());
        }
        try {
            return controllerClass.newInstance();
        } catch (Exception e) {
            String message = String.format("Could not create instance of class [%s]", controllerClass.getName());
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
