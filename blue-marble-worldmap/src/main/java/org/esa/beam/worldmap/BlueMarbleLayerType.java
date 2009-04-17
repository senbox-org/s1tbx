package org.esa.beam.worldmap;

import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerContext;
import com.bc.ceres.glayer.LayerType;
import com.bc.ceres.binding.ValueContainer;
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
    public Layer createLayer(LayerContext ctx, ValueContainer configuration) {
        Layer worldMapLayer = BlueMarbleWorldMapLayer.createWorldMapLayer();
        worldMapLayer.setVisible(true);
        return worldMapLayer;
    }

    @Override
    public ValueContainer getConfigurationCopy(LayerContext ctx, Layer layer) {
        return new ValueContainer();
    }
}
