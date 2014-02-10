package org.esa.pfa.fe;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.Band;
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
import java.util.Arrays;
import java.util.Map;

/**
 * An effective cloud mask algorithm developed by G.Kirches and M.Paperin in the frame of the Fronts project funded by German BSH.
 *
 * @author Norman Fomferra
 */
@OperatorMetadata(alias = "Meris.Fronts.CloudMask",
                  authors = "G.Kirches, M.Paperin (Algorithm), N.Fomferra, R.Quast (Implementation)")
public class FrontsCloudMaskOperator extends Operator {
    @SourceProduct
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct;
    @Parameter(defaultValue = "TRUE")
    String roiExpr;
    @Parameter(defaultValue = "11")
    int threshold;


    private transient Band cloudDataOriOrFlagBand;
    private transient Band cloudDataAlgoBand;
    private transient Product roiProduct;
    private transient Mask roiMask;

    public void setRoiExpr(String roiExpr) {
        this.roiExpr = roiExpr;
    }

    public void setThreshold(int threshold) {
        this.threshold = threshold;
    }

    @Override
    public void initialize() throws OperatorException {
        targetProduct = new Product("Meris.Fronts.CloudMask",
                                    "Meris.Fronts.CloudMask",
                                    sourceProduct.getSceneRasterWidth(),
                                    sourceProduct.getSceneRasterHeight());
        ProductUtils.copyGeoCoding(sourceProduct, targetProduct);
        targetProduct.setPreferredTileSize(sourceProduct.getPreferredTileSize());
        cloudDataOriOrFlagBand = targetProduct.addBand("cloud_data_ori_or_flag", ProductData.TYPE_FLOAT64);
        cloudDataAlgoBand = targetProduct.addBand("cloud_data_algo", ProductData.TYPE_FLOAT64);
        targetProduct.addMask("cloud_mask", "cloud_data_ori_or_flag  > " + threshold, "", Color.ORANGE, 0.5);

        // todo - use VirtualBandOpImage.createMask(roiExpr, sourceProduct, ResolutionLevel.MAXRES) when making PixelOp
        roiProduct = new Product("Meris.Fronts.ROI",
                                 "Meris.Fronts.ROI",
                                 sourceProduct.getSceneRasterWidth(),
                                 sourceProduct.getSceneRasterHeight());
        ProductUtils.copyFlagBands(sourceProduct, roiProduct, true);
        roiMask = roiProduct.addMask("roi", roiExpr, "", Color.WHITE, 0.0);
    }

    @Override
    public void dispose() {
        super.dispose();
        roiProduct.dispose();
        roiProduct = null;
        roiMask = null;
    }

    @Override
    public void computeTileStack(Map<Band, Tile> targetTiles,
                                 Rectangle targetRectangle,
                                 ProgressMonitor pm) throws OperatorException {
        Tile cloudDataOriOrFlag = targetTiles.get(cloudDataOriOrFlagBand);
        Tile cloudDataAlgo = targetTiles.get(cloudDataAlgoBand);
        Tile roiTile = getSourceTile(roiMask, targetRectangle);

        double[] cloudDataOriOrFlagData = new double[targetRectangle.width * targetRectangle.height];
        double[] cloudDataAlgoData = new double[targetRectangle.width * targetRectangle.height];
        int[] roiTileData = roiTile.getSamplesInt();

        makedMERISCloudMask(targetRectangle.width,
                            targetRectangle.height,
                            getSourceTile(sourceProduct.getBand("reflec_15"), targetRectangle),
                            getSourceTile(sourceProduct.getBand("reflec_11"), targetRectangle),
                            getSourceTile(sourceProduct.getBand("reflec_4"), targetRectangle),
                            getSourceTile(sourceProduct.getBand("reflec_3"), targetRectangle),
                            getSourceTile(sourceProduct.getBand("reflec_1"), targetRectangle),
                            cloudDataOriOrFlagData,
                            cloudDataAlgoData,
                            roiTileData);

        for (int y = 0; y < targetRectangle.height; y++) {
            for (int x = 0; x < targetRectangle.width; x++) {
                int i = y * targetRectangle.width + x;
                cloudDataOriOrFlag.setSample(targetRectangle.x+x, targetRectangle.y+y, cloudDataOriOrFlagData[i]);
                cloudDataAlgo.setSample(targetRectangle.x + x, targetRectangle.y + y, cloudDataAlgoData[i]);
            }
        }
    }

    public static void makedMERISCloudMask(int sourceWidth,
                                           int sourceHeight,
                                           Tile sourceTileRefl15,
                                           Tile sourceTileRefl11,
                                           Tile sourceTileRefl4,
                                           Tile sourceTileRefl3,
                                           Tile sourceTileRefl1,
                                           double[] sourceCloudDataOriOrFlag,
                                           double[] sourceCloudDataAlgo,
                                           int[] flagArray) {


        Arrays.fill(sourceCloudDataOriOrFlag, 0.0);
        Arrays.fill(sourceCloudDataAlgo, 0.0);

        computeCloudFilter(sourceWidth, sourceHeight, 3.6, sourceTileRefl15, sourceCloudDataAlgo, flagArray);
        computeCloudFilter(sourceWidth, sourceHeight, 5.0, sourceTileRefl11, sourceCloudDataAlgo, flagArray);
        computeCloudFilter(sourceWidth, sourceHeight, -0.3333, sourceTileRefl4, sourceCloudDataAlgo, flagArray);
        computeCloudFilter(sourceWidth, sourceHeight, -0.3333, sourceTileRefl3, sourceCloudDataAlgo, flagArray);
        computeCloudFilter(sourceWidth, sourceHeight, -0.3333 * 0.5, sourceTileRefl1, sourceCloudDataAlgo, flagArray);

        for (int j = 0; j < sourceHeight; j++) {
            for (int i = 0; i < sourceWidth; i++) {
                final int index = j * sourceWidth + i;
                for (double k = 0.14; k >= 0.02; k -= 0.005) {
                    if (sourceCloudDataAlgo[index] > k && !Double.isNaN(sourceCloudDataAlgo[index]) && flagArray[index] != 0) {
                        sourceCloudDataOriOrFlag[index]++;
                        //System.out.printf("sourceCloudDataOriOrFlag: %d %d %d %f \n", j, i,index, sourceCloudDataOriOrFlag[index]);
                    }
                }
            }
        }
        //sun_zenith, sun_azimuth, cloud_top_press
    }

    private static void computeCloudFilter(int sourceWidth, int sourceHeight, double factor, Tile sourceTile, double[] sourceCloudDataAlgo, int[] flagArray) {
        double[] cloud_band = sourceTile.getSamplesDouble();

        for (int j = 0; j < sourceHeight; j++) {
            for (int i = 0; i < sourceWidth; i++) {
                final int index = j * sourceWidth + i;
                if (!Double.isNaN(sourceCloudDataAlgo[index]) && flagArray[index] != 0) {
                    sourceCloudDataAlgo[index] = sourceCloudDataAlgo[index] + factor * cloud_band[index];
                }
            }
        }
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(FrontsCloudMaskOperator.class);
        }
    }
}
