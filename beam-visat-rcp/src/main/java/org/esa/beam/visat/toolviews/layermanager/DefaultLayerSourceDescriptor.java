package org.esa.beam.visat.toolviews.layermanager;

@SuppressWarnings({"UnusedDeclaration"})
public class DefaultLayerSourceDescriptor implements LayerSourceDescriptor {

    private String id;
    private String name;
    private String description;
    private Class<? extends LayerSourceController> controllerClass;

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
        try {
            return controllerClass.newInstance();
        } catch (Exception e) {
            String message = String.format("Could not create instance of class [%s]", controllerClass.getName());
            throw new IllegalStateException(message, e);
        }
    }
}
