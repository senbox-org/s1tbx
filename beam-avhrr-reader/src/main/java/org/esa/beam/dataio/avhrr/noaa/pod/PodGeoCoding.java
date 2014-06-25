package org.esa.beam.dataio.avhrr.noaa.pod;

import com.bc.ceres.glevel.MultiLevelImage;
import org.esa.beam.framework.datamodel.DefaultPixelFinder;
import org.esa.beam.framework.datamodel.GeoApproximation;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.PixelPosEstimator;
import org.esa.beam.framework.datamodel.TiePointGeoCoding;
import org.esa.beam.framework.datamodel.TiePointGrid;

import java.awt.Rectangle;

/**
 * @author Ralf Quast
 */
final class PodGeoCoding extends TiePointGeoCoding {

    private transient PixelPosEstimator pixelPosEstimator;
    private transient DefaultPixelFinder pixelFinder;

    PodGeoCoding(TiePointGrid latGrid, TiePointGrid lonGrid) {
        super(latGrid, lonGrid);

        final MultiLevelImage lonImage = lonGrid.getGeophysicalImage();
        final MultiLevelImage latImage = latGrid.getGeophysicalImage();

        final GeoApproximation[] approximations = GeoApproximation.createApproximations(lonImage, latImage, null, 0.5);
        final Rectangle bounds = new Rectangle(0, 0, lonGrid.getSceneRasterWidth(), lonGrid.getSceneRasterHeight());
        pixelPosEstimator = new PixelPosEstimator(approximations, bounds);
        pixelFinder = new DefaultPixelFinder(lonImage, latImage, null, 0.00005);
    }

    @Override
    public boolean canGetPixelPos() {
        return pixelPosEstimator.canGetPixelPos();
    }

    @Override
    public PixelPos getPixelPos(GeoPos geoPos, PixelPos pixelPos) {
        if (pixelPosEstimator.canGetPixelPos()) {
            if (pixelPos == null) {
                pixelPos = new PixelPos();
            }
            pixelPosEstimator.getPixelPos(geoPos, pixelPos);
            if (pixelPos.isValid()) {
                pixelFinder.findPixelPos(geoPos, pixelPos);
            }
        } else {
            super.getPixelPos(geoPos, pixelPos);
        }
        return pixelPos;
    }
}
