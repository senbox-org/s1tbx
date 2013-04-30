package org.esa.beam.tutorials;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.FlagCoding;
import org.esa.beam.framework.datamodel.Mask;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.util.ProductUtils;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.Map;


/*
Example taken from the tutorial given at Sentinel-3 OLCI/SLSTR and MERIS/(A)ATSR workshop 2012.
See http://www.brockmann-consult.de/cms/web/beam/tutorials
 */
@OperatorMetadata(
        alias = "FLH",
        description = "FLH Operator developed during the MERIS/AATSR Workshop, Esrin, Oct 2012")
public class FluorescenceLineHeightOp extends Operator {

    @SourceProduct
    private Product sourceProduct;

    @TargetProduct
    private Product flhProduct;

    @Parameter(interval = "(0,2]", defaultValue = "1", description = "Ratio scaling for FLH.")
    private float k;

    private Band flhBand;
    private Band band1;
    private Band band2;
    private Band band3;
    private float wavelengthsRatio;
    private Band flhQfBand;
    private Mask valid;

    @Override
    public void initialize() throws OperatorException {
        band1 = sourceProduct.getBand("radiance_7");
        band2 = sourceProduct.getBand("radiance_8");
        band3 = sourceProduct.getBand("radiance_9");
        if (band1 == null || band2 == null || band3 == null) {
            throw new OperatorException("Need a MERIS L1b.");
        }

        float wl1 = band1.getSpectralWavelength();
        float wl2 = band2.getSpectralWavelength();
        float wl3 = band3.getSpectralWavelength();
        wavelengthsRatio = (wl2 - wl1) / (wl3 - wl1);

        flhProduct = new Product("FLH", "FLH_TYPE",
                                 sourceProduct.getSceneRasterWidth(),
                                 sourceProduct.getSceneRasterHeight());

        ProductUtils.copyTiePointGrids(sourceProduct, flhProduct);
        ProductUtils.copyGeoCoding(sourceProduct, flhProduct);
        ProductUtils.copyFlagBands(sourceProduct, flhProduct, true);

        valid = sourceProduct.addMask("valid",
                                      "!l1_flags.INVALID && !l1_flags.LAND_OCEAN",
                                      "True, if a source pixel is valid", Color.GREEN, 0.5);

        flhBand = flhProduct.addBand("FLH", ProductData.TYPE_FLOAT32);
        flhQfBand = flhProduct.addBand("FLH_QF", ProductData.TYPE_INT8);

        flhBand.setValidPixelExpression("!FLH_QF.INV");

        FlagCoding flagCoding = new FlagCoding("FLH_Quality_Flags");
        flagCoding.addFlag("MIN", 0x01, "FLH below min");
        flagCoding.addFlag("MAX", 0x02, "FLH above max");
        flagCoding.addFlag("INV", 0x04, "FLH is invalid (NaN)");
        flhProduct.getFlagCodingGroup().add(flagCoding);

        flhProduct.addMask("FLH_LOW",
                           "FLH_QF.MIN",
                           "Low FLH", Color.BLUE.brighter(), 0.5);
        flhProduct.addMask("FLH_HI",
                           "FLH_QF.MAX",
                           "High FLH", Color.YELLOW.darker(), 0.5);


        flhQfBand.setSampleCoding(flagCoding);
    }

    @Override
    public void computeTileStack(Map<Band, Tile> targetTiles, Rectangle targetRectangle, ProgressMonitor pm) throws OperatorException {
        Tile tileFLH = targetTiles.get(flhBand);
        Tile tileFLHQF = targetTiles.get(flhQfBand);

        pm.beginTask("Computing FLH", targetRectangle.height);

        Tile tileL1 = getSourceTile(band1, targetRectangle);
        Tile tileL2 = getSourceTile(band2, targetRectangle);
        Tile tileL3 = getSourceTile(band3, targetRectangle);
        Tile tileMask = getSourceTile(valid, targetRectangle);

        try {
            for (int y = targetRectangle.y; y < targetRectangle.y + targetRectangle.height; y++) {
                if (pm.isCanceled()) {
                    return;
                }
                for (int x = targetRectangle.x; x < targetRectangle.x + targetRectangle.width; x++) {
                    float FLH;
                    byte flags = 0;
                    if (tileMask.getSampleBoolean(x, y)) {
                        float L1 = tileL1.getSampleFloat(x, y);
                        float L2 = tileL2.getSampleFloat(x, y);
                        float L3 = tileL3.getSampleFloat(x, y);
                        FLH = L2 - k * (L1 + (L3 - L1) * wavelengthsRatio);
                    } else {
                        FLH = Float.NaN;
                    }
                    tileFLH.setSample(x, y, FLH);
                    if (FLH < 0) {
                        flags |= 0x01;
                    }
                    if (FLH > 1) {
                        flags |= 0x02;
                    }
                    if (Float.isNaN(FLH)) {
                        flags |= 0x04;
                    }
                    tileFLHQF.setSample(x, y, flags);
                }
                pm.worked(1); // Report to the progress monitor that we have completed 1 work unit
            }
        } finally {
            pm.done(); // Report to the progress monitor that we are done
        }
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(FluorescenceLineHeightOp.class);
        }
    }
}
