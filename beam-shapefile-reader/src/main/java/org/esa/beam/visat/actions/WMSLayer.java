package org.esa.beam.visat.actions;

import com.bc.ceres.glayer.Layer;
import com.bc.ceres.grender.Rendering;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

import org.geotools.geometry.jts.ReferencedEnvelope;

/**
 * todo - add API doc
 *
 * @author Marco Peters
 * @version $Revision: $ $Date: $
 * @since BEAM 4.6
 */
public class WMSLayer extends Layer {

    private final WMSMapLayer wmsMapLayer;

    public WMSLayer(WMSMapLayer layer) {
        wmsMapLayer = layer;
    }

    @Override
    protected void renderLayer(final Rendering rendering) {
//        Rectangle bounds = rendering.getViewport().getViewBounds();
//        final AffineTransform m2vTransform = rendering.getViewport().getViewToModelTransform();
//        Rectangle2D d = m2vTransform.createTransformedShape(bounds).getBounds2D();
//        ReferencedEnvelope mapArea = new ReferencedEnvelope(d, crs);
//        mapContext.setAreaOfInterest(mapArea);
//
//        applyOpacity(getStyle().getOpacity());
//
//        labelCache.clear();
//
//        renderer.paint(rendering.getGraphics(), bounds, mapArea);
    }
}
