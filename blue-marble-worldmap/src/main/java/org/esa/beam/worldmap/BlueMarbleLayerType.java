package org.esa.beam.worldmap;

import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.ValueContainer;
import com.bc.ceres.binding.ValueModel;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerContext;
import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.ceres.glevel.MultiLevelSource;
import org.esa.beam.glevel.TiledFileMultiLevelSource;
import org.geotools.referencing.AbstractIdentifiedObject;
import org.geotools.referencing.crs.DefaultGeographicCRS;

import java.awt.geom.AffineTransform;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * @author Marco Peters
 * @version $Revision: $ $Date: $
 * @since BEAM 4.6
 */
public class BlueMarbleLayerType extends ImageLayer.Type {

    private static final String WORLD_IMAGE_DIR_PROPERTY_NAME = "org.esa.beam.worldImageDir";
    private MultiLevelSource multiLevelSource;

    @Override
    public String getName() {
        return "NASA Blue Marble";
    }

    @Override
    public boolean isValidFor(LayerContext ctx) {
        if (ctx.getCoordinateReferenceSystem() instanceof AbstractIdentifiedObject) {
            AbstractIdentifiedObject crs = (AbstractIdentifiedObject) ctx.getCoordinateReferenceSystem();
            return DefaultGeographicCRS.WGS84.equals(crs, false);
        }
        return false;
    }

    @Override
    protected Layer createLayerImpl(LayerContext ctx, ValueContainer configuration) {
        if (multiLevelSource == null) {
            synchronized (this) {
                if (multiLevelSource == null) {
                    multiLevelSource = createMultiLevelSource();
                }
            }
        }
        for (final ValueModel model : super.getConfigurationTemplate().getModels()) {
            if (configuration.getModel(model.getDescriptor().getName()) == null) {
                configuration.addModel(model);
            }
        }
        try {
            configuration.setValue(ImageLayer.PROPERTY_NAME_MULTI_LEVEL_SOURCE, multiLevelSource);
            final AffineTransform imageToModelTransform = multiLevelSource.getModel().getImageToModelTransform(0);
            configuration.setValue(ImageLayer.PROPERTY_NAME_IMAGE_TO_MODEL_TRANSFORM, imageToModelTransform);
        } catch (ValidationException e) {
            throw new IllegalStateException(e);
        }

        return new BlueMarbleWorldMapLayer(configuration);
    }

    private static MultiLevelSource createMultiLevelSource() {
        String dirPath = System.getProperty(WORLD_IMAGE_DIR_PROPERTY_NAME);
        if (dirPath == null || dirPath.isEmpty()) {
            dirPath = getDirPathFromModule();
        }
        if (dirPath == null) {
            throw new IllegalStateException("World image directory not found.");
        }
        final MultiLevelSource multiLevelSource;
        try {
            multiLevelSource = TiledFileMultiLevelSource.create(new File(dirPath), false);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return multiLevelSource;
    }

    private static String getDirPathFromModule() {
        final URL resource = BlueMarbleWorldMapLayer.class.getResource("image.properties");
        try {
            return new File(resource.toURI()).getParent();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return null;
    }

}
