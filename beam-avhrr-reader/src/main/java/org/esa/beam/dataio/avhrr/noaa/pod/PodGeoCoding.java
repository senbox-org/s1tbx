package org.esa.beam.dataio.avhrr.noaa.pod;

import org.esa.beam.framework.dataio.ProductSubsetDef;
import org.esa.beam.framework.datamodel.GeoApproximation;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.PixelPosEstimator;
import org.esa.beam.framework.datamodel.Scene;
import org.esa.beam.framework.datamodel.TiePointGeoCoding;
import org.esa.beam.framework.datamodel.TiePointGrid;

import javax.media.jai.PlanarImage;
import java.awt.Rectangle;

/**
 * This geo-coding improves the inverse approximations used in the {@code TiePointGeoCoding} in order
 * to facilitate accurate re-projections and graticule drawing.
 * <p/>
 * Limitation: this geo-coding is not transferred when making subsets and is not saved when a product
 * is written to disk.
 *
 * @author Ralf Quast
 */
final class PodGeoCoding extends TiePointGeoCoding {

    private transient PixelPosEstimator pixelPosEstimator;
    private transient PodPixelFinder pixelFinder;

    PodGeoCoding(TiePointGrid latGrid, TiePointGrid lonGrid) {
        super(latGrid, lonGrid);

        final PlanarImage lonImage = lonGrid.getGeophysicalImage();
        final PlanarImage latImage = latGrid.getGeophysicalImage();

        final GeoApproximation[] approximations = createApproximations(lonImage, latImage);
        final Rectangle bounds = new Rectangle(0, 0, lonGrid.getSceneRasterWidth(), lonGrid.getSceneRasterHeight());
        pixelPosEstimator = new PixelPosEstimator(approximations, bounds);
        pixelFinder = new PodPixelFinder(lonImage, latImage, null, 0.01);
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

    private static GeoApproximation[] createApproximations(PlanarImage lonImage, PlanarImage latImage) {
        return GeoApproximation.createApproximations(lonImage, latImage, null, 0.5);
    }

    @Override
    public boolean transferGeoCoding(Scene sourceScene, Scene targetScene, ProductSubsetDef subsetDef) {
        return false;
    }

    @Override
    public void dispose() {
        super.dispose();

        pixelFinder = null;
        pixelPosEstimator = null;
    }
}
