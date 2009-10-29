package org.esa.beam.visat.toolviews.layermanager;

import com.bc.ceres.glayer.LayerType;
import com.bc.ceres.glayer.LayerTypeRegistry;

import org.esa.beam.visat.toolviews.layermanager.layersrc.SimpleLayerSource;

/**
 * The {@code DefaultLayerSourceDescriptor} provides metadata and
 * a factory method for a {@link LayerSource}.
 * <p/>
 * <p>
 * Instances of this class are created by reading the extension configuration of
 * the extension point {@code "layerSources"} in the {@code module.xml}.
 * </p>
 * Example 1:<br/>
 * <p/>
 * <pre>
 *    &lt;extension point="beam-visat-rcp:layerSources"&gt;
 *      &lt;layerSource&gt;
 *          &lt;id&gt;shapefile-layer-source&lt;/id&gt;
 *          &lt;name&gt;ESRI Shapefile&lt;/name&gt;
 *          &lt;description&gt;Displays shapes from an ESRI Shapefile&lt;/description&gt;
 *          &lt;class&gt;org.esa.beam.visat.toolviews.layermanager.layersrc.shapefile.ShapefileLayerSource&lt;/class&gt;
 *      &lt;/layerSource&gt;
 *    &lt;/extension&gt;
 * </pre>
 * Example 1:<br/>
 * <p/>
 * <pre>
 *    &lt;extension point="beam-visat-rcp:layerSources"&gt;
 *      &lt;layerSource&gt;
 *          &lt;id&gt;bluemarble-layer-source&lt;/id&gt;
 *          &lt;name&gt;NASA Blue Marble;/name&gt;
 *          &lt;description&gt;Adds NASA Blue Marble image layer to the background.&lt;/description&gt;
 *          &lt;layerType&gt;org.esa.beam.worldmap.BlueMarbleLayerType&lt;/class&gt;
 *      &lt;/layerSource&gt;
 *    &lt;/extension&gt;
 * </pre>
 * <p/>
 * <i>Note: This API is not public yet and may significantly change in the future. Use it at your own risk.</i>
 */
@SuppressWarnings({"UnusedDeclaration"})
public class DefaultLayerSourceDescriptor implements LayerSourceDescriptor {

    private String id;
    private String name;
    private String description;
    private Class<? extends LayerSource> layerSourceClass;
    private String layerTypeClassName;
    private LayerType layerType;

    /**
     * Constructor used by Ceres runtime.
     */
    public DefaultLayerSourceDescriptor() {
    }

    // Constructor only used in tests.
    DefaultLayerSourceDescriptor(String id, String name, String description,
                                 Class<? extends LayerSource> layerSourceClass) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.layerSourceClass = layerSourceClass;
    }

    // Constructor only used in tests.
    DefaultLayerSourceDescriptor(String id, String name, String description,
                                 String layerTypeClassName) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.layerTypeClassName = layerTypeClassName;
    }

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
        if (layerTypeClassName == null) {
            return null;
        }
        if (layerType == null) {
            try {
                return LayerTypeRegistry.getLayerType(layerTypeClassName);
            } catch (Exception e) {
                String message = String.format("Could not create instance of class [%s]", layerTypeClassName);
                throw new IllegalStateException(message, e);
            }
        }
        return layerType;
    }


}
