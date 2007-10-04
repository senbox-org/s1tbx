package org.esa.beam.collocation;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.FlagCoding;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.dataop.resamp.Resampling;
import org.esa.beam.framework.gpf.AbstractOperator;
import org.esa.beam.framework.gpf.AbstractOperatorSpi;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.util.ProductUtils;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

/**
 * Colocation operator.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public class CollocationOp extends AbstractOperator {

    @SourceProduct(alias = "master")
    Product masterProduct;
    @SourceProduct(alias = "slave")
    Product slaveProduct;
    @TargetProduct
    Product targetProduct;

    @Parameter
    Resampling resampling;

    private transient List<Band> slaveBandList;

    @Override
    protected Product initialize() throws OperatorException {
        // todo - name and type
        targetProduct = new Product("CollocationProduct", "COLLOCATION",
                                    masterProduct.getSceneRasterWidth(),
                                    masterProduct.getSceneRasterHeight());

        targetProduct.setStartTime(masterProduct.getStartTime());
        targetProduct.setEndTime(masterProduct.getEndTime());

        ProductUtils.copyFlagCodings(masterProduct, targetProduct);
        ProductUtils.copyElementsAndAttributes(masterProduct.getMetadataRoot(), targetProduct.getMetadataRoot());
        ProductUtils.copyTiePointGrids(masterProduct, targetProduct);
        ProductUtils.copyGeoCoding(masterProduct, targetProduct);

        for (final Band band : masterProduct.getBands()) {
            final Band targetBand = ProductUtils.copyBand(band.getName(), masterProduct, targetProduct);
            final FlagCoding flagCoding = band.getFlagCoding();
            if (flagCoding != null) {
                targetBand.setFlagCoding(targetProduct.getFlagCoding(flagCoding.getName()));
            }
        }
        ProductUtils.copyBitmaskDefs(masterProduct, targetProduct);

        slaveBandList = new ArrayList<Band>();

        for (final Band band : slaveProduct.getBands()) {
            final Band slaveBand = targetProduct.addBand(band.getName(), band.getDataType());

            slaveBand.setDescription(band.getDescription());
            slaveBand.setUnit(band.getUnit());
            slaveBand.setScalingFactor(band.getScalingFactor());
            slaveBand.setScalingOffset(band.getScalingOffset());
            slaveBand.setLog10Scaled(band.isLog10Scaled());
            // slaveBand.setSpectralBandIndex(band.getSpectralBandIndex());
            slaveBand.setSpectralWavelength(band.getSpectralWavelength());
            slaveBand.setSpectralBandwidth(band.getSpectralBandwidth());
            slaveBand.setSolarFlux(band.getSolarFlux());
            slaveBand.setNoDataValueUsed(band.isNoDataValueUsed());
            slaveBand.setNoDataValue(band.getNoDataValue());
//            slaveBand.setValidPixelExpression(band.getValidPixelExpression());

            slaveBandList.add(slaveBand);
        }

        // todo - slave metadata
        // todo - slave flag codings
        // todo - slave tie point grids
        // todo - slave bitmask definitions

        resampling = Resampling.CUBIC_CONVOLUTION;

        return targetProduct;
    }

    @Override
    public void computeTile(Band targetBand, Tile targetTile) throws OperatorException {

        if (slaveBandList.contains(targetBand)) {
            collocateSlaveBand(targetBand, targetTile);
        } else {
            final ProgressMonitor pm = createProgressMonitor();
            try {
                pm.beginTask("copying master band", targetTile.getHeight());

                final Band masterBand = masterProduct.getBand(targetBand.getName());
                final Rectangle targetRectangle = targetTile.getRectangle();
                final Tile sourceTile = getSourceTile(masterBand, targetRectangle);

                for (int y = targetRectangle.y; y < targetRectangle.y + targetRectangle.height; ++y) {
                    for (int x = targetRectangle.x; x < targetRectangle.x + targetRectangle.width; ++x) {
                        targetTile.setSample(x, y, sourceTile.getSampleDouble(x, y));
                    }
                    pm.worked(1);
                }
            } finally {
                pm.done();
            }
        }
    }

    @Override
    public void dispose() {
        slaveBandList = null;
    }

    private void collocateSlaveBand(Band targetBand, Tile targetTile) throws OperatorException {
        final ProgressMonitor pm = createProgressMonitor();
        try {
            pm.beginTask("collocating slave band", targetTile.getHeight());

            final Band sourceBand = slaveProduct.getBand(targetBand.getName());
            final Resampling.Index resamplingIndex = resampling.createIndex();

            final GeoCoding sourceGeoCoding = slaveProduct.getGeoCoding();
            final GeoCoding targetGeoCoding = targetProduct.getGeoCoding();

            final int sourceRasterHeight = slaveProduct.getSceneRasterHeight();
            final int sourceRasterWidth = slaveProduct.getSceneRasterWidth();

            final Rectangle targetRectangle = targetTile.getRectangle();

            for (int y = targetRectangle.y; y < targetRectangle.y + targetRectangle.height; ++y) {
                final Rectangle line = new Rectangle(targetRectangle.x, y, targetRectangle.width, 1);
                final PixelPos[] sourcePixelPositions =
                        ProductUtils.computeSourcePixelCoordinates(sourceGeoCoding, sourceRasterWidth,
                                                                   sourceRasterHeight, targetGeoCoding, line);
                final Rectangle sourceRectangle =
                        createSourceRectangle(sourcePixelPositions, sourceRasterWidth, sourceRasterHeight);

                if (sourceRectangle != null) {
                    final Tile sourceTile = getSourceTile(sourceBand, sourceRectangle);
                    final ResamplingRaster resamplingRaster = new ResamplingRaster(sourceTile);

                    for (int x = targetRectangle.x; x < targetRectangle.x + targetRectangle.width; ++x) {
                        final PixelPos pixelPos = sourcePixelPositions[x - targetRectangle.x];

                        if (pixelPos != null) {
                            final int sourceX = (int) Math.floor(pixelPos.getX());
                            final int sourceY = (int) Math.floor(pixelPos.getY());

                            targetTile.setSample(x, y, sourceTile.getSampleDouble(sourceX, sourceY));

                            resampling.computeIndex(pixelPos.x, pixelPos.y, sourceRasterWidth, sourceRasterHeight,
                                                    resamplingIndex);
                            try {
                                targetTile.setSample(x, y, resampling.resample(resamplingRaster, resamplingIndex));
                                // todo - no data value
                            } catch (Exception e) {
                                throw new OperatorException(e.getMessage());
                            }
                        } else {
                            // todo -- no source pixel pos
                        }
                    }
                } else {
                    // todo -- no source pixel pos for the whole target line
                }
                pm.worked(1);
            }
        } finally {
            pm.done();
        }
    }

    private static Rectangle createSourceRectangle(PixelPos[] sourcePixelPos,
                                                   int sourceRasterWidth,
                                                   int sourceRasterHeight) {
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;

        for (final PixelPos pixelsPos : sourcePixelPos) {
            if (pixelsPos != null) {
                final int x = (int) Math.floor(pixelsPos.getX());
                final int y = (int) Math.floor(pixelsPos.getY());

                if (x < minX) {
                    minX = x;
                }
                if (x > maxX) {
                    maxX = x;
                }
                if (y < minY) {
                    minY = y;
                }
                if (y > maxY) {
                    maxY = y;
                }
            }
        }
        if (minX > maxX || minY > maxY) {
            return null;
        }

        minX = Math.max(minX - 2, 0);
        maxX = Math.min(maxX + 2, sourceRasterWidth - 1);
        minY = Math.max(minY - 2, 0);
        maxY = Math.min(maxY + 2, sourceRasterHeight - 1);

        return new Rectangle(minX, minY, maxX - minX + 1, maxY - minY + 1);
    }

    private static class ResamplingRaster implements Resampling.Raster {

        private final Tile tile;

        public ResamplingRaster(Tile tile) {
            this.tile = tile;
        }

        public final int getWidth() {
            return tile.getWidth();
        }

        public final int getHeight() {
            return tile.getHeight();
        }

        public final float getSample(int x, int y) throws Exception {
            if (tile.getRasterDataNode().isPixelValid(x, y)) {
                return tile.getSampleFloat(x, y);
            }

            return Float.NaN;
        }
    }

    /**
     * Collocation operator SPI.
     */
    public static class Spi extends AbstractOperatorSpi {

        public Spi() {
            super(CollocationOp.class, "Collocation");
        }
    }

}
