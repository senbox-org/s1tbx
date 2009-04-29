package org.esa.beam.worldmap;

import com.bc.ceres.binding.ValueContainer;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerContext;
import com.bc.ceres.glayer.LayerType;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * @author Marco Peters
 * @version $Revision: $ $Date: $
 * @since BEAM 4.6
 */
public class BlueMarbleLayerType extends LayerType {

    @Override
    public String getName() {
        return "Nasa Blue Marble";
    }

    @Override
    public boolean isValidFor(LayerContext ctx) {
        if (ctx.getCoordinateReferenceSystem() instanceof CoordinateReferenceSystem) {
            CoordinateReferenceSystem crs = (CoordinateReferenceSystem) ctx.getCoordinateReferenceSystem();
            return DefaultGeographicCRS.WGS84.equals(crs);
        }
        return false;
    }

    @Override
    protected Layer createLayerImpl(LayerContext ctx, ValueContainer configuration) {
        return new BlueMarbleWorldMapLayer(configuration);
    }

    @Override
    public ValueContainer getConfigurationTemplate() {
        return new ValueContainer();

    }
}
