
package org.csa.rstb.soilmoisture.gpf;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.util.DefaultPropertyMap;
import org.esa.snap.core.util.PropertyMap;
import org.esa.snap.engine_utilities.gpf.OperatorUtils;
import org.esa.snap.engine_utilities.util.Settings;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * By inverting the Hallikainen dielectric model, soil moisture is retrieved from the real dielectric constant.
 */
public class HallikainenDielectricModel extends BaseDielectricModel implements DielectricModel {

    private final Delta deltaFunc = new Delta();
    private Band clayBand = null; // source
    private Band sandBand = null; // source
    private double INVALID_CLAY_VALUE = -1.0d;
    private double INVALID_SAND_VALUE = -1.0d;
    // The Hallikainen model coefficients
    // They are in a config file
    private double a0 = 0.0d;
    private double a1 = 0.0d;
    private double a2 = 0.0d;
    private double b0 = 0.0d;
    private double b1 = 0.0d;
    private double b2 = 0.0d;
    private double c0 = 0.0d;
    private double c1 = 0.0d;
    private double c2 = 0.0d;
    private SVFMinimizer minimizer = null;

    /**
     * Default constructor. The graph processing framework
     * requires that an operator has a default constructor.
     */
    public HallikainenDielectricModel(final Operator op, final Product srcProduct, final Product tgtProduct,
                                      final double invalidSMValue,
                                      final double minSM, final double maxSM,
                                      final Band smBand, final Band qualityIndexBand,
                                      String rdcBandName) {

        super(op, srcProduct, tgtProduct, invalidSMValue, minSM, maxSM, smBand, qualityIndexBand, rdcBandName);
    }

    public void initialize() throws OperatorException {

        minimizer = new SVFMinimizer(INVALID_SM_VALUE, minSM, maxSM, deltaFunc);

        clayBand = getSourceBand("Clay");
        INVALID_CLAY_VALUE = clayBand.getNoDataValue();
        /*
        int datatype = clayBand.getDataType();
        if (datatype != ProductData.TYPE_FLOAT64) {
            System.out.println("clayBand data type = " + datatype);
        }
        */

        sandBand = getSourceBand("Sand");
        INVALID_SAND_VALUE = sandBand.getNoDataValue();
        /*
        datatype = sandBand.getDataType();
        if (datatype != ProductData.TYPE_FLOAT64) {
            System.out.println("sandBand data type = " + datatype);
        }
        */

        try {

            getConfigParameters();

        } catch (IOException e) {

            throw new OperatorException("HallikainenDielectricModel::initialize(): Failed to get config parameters ");
        }
    }

    public void computeTileStack(final Map<Band, Tile> targetTiles, final Rectangle targetRectangle, final ProgressMonitor pm)
            throws OperatorException {

        final int tx0 = targetRectangle.x;
        final int ty0 = targetRectangle.y;
        final int tw = targetRectangle.width;
        final int th = targetRectangle.height;

        // Source tile has the same dimension as the target tile.
        final Rectangle sourceTileRectangle = new Rectangle(tx0, ty0, tw, th);

        try {

            final Tile rdcTile = smDielectricModelInverOp.getSourceTile(rdcBand, sourceTileRectangle);

            if (rdcTile == null) {

                throw new OperatorException("Failed to get source rdc tile");
            }

            final Tile clayTile = smDielectricModelInverOp.getSourceTile(clayBand, sourceTileRectangle);

            if (clayTile == null) {

                throw new OperatorException("Failed to get source clay tile");
            }

            final Tile sandTile = smDielectricModelInverOp.getSourceTile(sandBand, sourceTileRectangle);

            if (sandTile == null) {

                throw new OperatorException("Failed to get source sand tile");
            }

            final Tile smTile = targetTiles.get(smBand);
            final ProductData smData = smTile.getDataBuffer(); // target

            final Tile qualityIndexTile = targetTiles.get(qualityIndexBand);
            final ProductData qualityIndexData = qualityIndexTile.getDataBuffer(); // target

            final ProductData rdcData = rdcTile.getDataBuffer(); // source
            final ProductData clayData = clayTile.getDataBuffer(); // source
            final ProductData sandData = sandTile.getDataBuffer(); // source

            final int maxy = ty0 + th;
            final int maxx = tx0 + tw;

            // Process pixel by pixel in the tile
            for (int y = ty0; y < maxy; y++) { // loop through rows

                for (int x = tx0; x < maxx; x++) { // loop through columns

                    // Don't assume all source bands are doubles.
                    int index = smTile.getDataBufferIndex(x, y);
                    int srcIndex = rdcTile.getDataBufferIndex(x, y);
                    int clayIndex = clayTile.getDataBufferIndex(x, y);

                    final double rdc = rdcData.getElemDoubleAt(srcIndex);

                    // Clay and sand are in percentages
                    final double clay = clayData.getElemDoubleAt(clayIndex);
                    final double sand = sandData.getElemDoubleAt(clayIndex);

                    // Matlab code initializes to zero; but zero is a valid sm value, so it is better
                    // to initialize to INVALID_SM_VALUE.
                    double sm = INVALID_SM_VALUE;
                    long qualityIndex;

                    if (!isValid(rdc) || rdc == INVALID_RDC_VALUE) {

                        qualityIndex = QUALITY_INDEX_NO_RDC;

                    } else if (!isValid(clay) || !isValid(sand) || clay == INVALID_CLAY_VALUE || sand == INVALID_SAND_VALUE
                            || clay < 0.0d || sand < 0.0d) {

                        qualityIndex = QUALITY_INDEX_NO_SAND_OR_CLAY;

                    } else {

                        sm = invert(rdc, clay, sand);
                        qualityIndex = getQualityIndex(sm);
                    }

                    smData.setElemDoubleAt(index, sm);
                    qualityIndexData.setElemUIntAt(index, qualityIndex);
                }
            }

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(smDielectricModelInverOp.getId(), e);
        } finally {
            pm.done();
        }
    }

    private double invert(final double rdc, final double clay, final double sand) {

        // Want to find the sm value in (minSM, maxSM) that minimizes
        // f(sm) = abs(rdc - computeRDC(sm, clay, sand))
        // The fixed values are clay, sand and rdc in that order. The order is IMPORTANT.
        return minimizer.minimize(new double[]{clay, sand, rdc});
    }

    private double computeRDC(final double sm, final double clay, final double sand) {

        return (a0 + a1 * sand + a2 * clay) +
                ((b0 + b1 * sand + b2 * clay) * sm) +
                ((c0 + c1 * sand + c2 * clay) * (sm * sm));
    }

    private void getConfigParameters() throws IOException {

        final File configFolder = new File(Settings.instance().getAuxDataFolder(), "sm_luts");
        final File configFile = new File(configFolder, "soil_moisture.config");

        final PropertyMap configPref = new DefaultPropertyMap();
        configPref.load(configFile.toPath());

        a0 = configPref.getPropertyDouble("Hallikainen.a0");
        a1 = configPref.getPropertyDouble("Hallikainen.a1");
        a2 = configPref.getPropertyDouble("Hallikainen.a2");
        b0 = configPref.getPropertyDouble("Hallikainen.b0");
        b1 = configPref.getPropertyDouble("Hallikainen.b1");
        b2 = configPref.getPropertyDouble("Hallikainen.b2");
        c0 = configPref.getPropertyDouble("Hallikainen.c0");
        c1 = configPref.getPropertyDouble("Hallikainen.c1");
        c2 = configPref.getPropertyDouble("Hallikainen.c2");

        //System.out.println("a0 = " + a0 + " a1 = " + a1 + " a2 = " + a2);
        //System.out.println("b0 = " + b0 + " b1 = " + b1 + " b2 = " + b2);
        //System.out.println("c0 = " + c0 + " c1 = " + c1 + " c2 = " + c2);
    }

    class Delta implements SingleVarFunc {

        // The fixed values are clay, sand and rdc in that order. The order is IMPORTANT.
        public double compute(double sm, double[] fixed) {

            final double clay = fixed[0];
            final double sand = fixed[1];
            final double rdc = fixed[2];

            final double modelledRDC = computeRDC(sm, clay, sand);

            return Math.abs(rdc - modelledRDC);
        }
    }
}

