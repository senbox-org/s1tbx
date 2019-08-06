
package org.csa.rstb.soilmoisture.gpf;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.util.DefaultPropertyMap;
import org.esa.snap.core.util.PropertyMap;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.eo.Constants;
import org.esa.snap.engine_utilities.gpf.OperatorUtils;
import org.esa.snap.engine_utilities.util.Settings;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Map;

import static java.lang.StrictMath.exp;
import static java.lang.StrictMath.log;

/**
 * By inverting the Mironov dielectric model, soil moisture is retrieved from the real dielectric constant.
 */
public class MironovDielectricModel extends BaseDielectricModel implements DielectricModel {

    // Input: RDC, clay, effect soil temperature, symmetrized or not, coefficients and thresholds.

    private static final double ZERO_C_IN_K = 273.15d; // 0 deg Celsius = 273.15 K
    private final Delta deltaFunc = new Delta();
    private Band clayBand = null; // source
    private double INVALID_CLAY_VALUE = -1.0d;
    // They are in a config file
    private boolean useSymmetrized = true;
    private double theSM1 = 0.02; // only used if useSymmetrized is true
    private double effectiveSoilTemperatureCelsius = 18.0d;
    // from product metadata
    private double theF; // Hz
    // The Mironov model coefficients
    // They are in a config file
    private double thePERMIT0 = 0.0d;
    private double theEPWI0 = 0.0d;
    private double theND0 = 0.0d;
    private double theND1 = 0.0d;
    private double theND2 = 0.0d;
    private double theKD0 = 0.0d;
    private double theKD1 = 0.0d;
    private double theXMVT0 = 0.0d;
    private double theXMVT1 = 0.0d;
    private double theTF0 = 0.0d;
    private double theE0PB0 = 0.0d;
    private double theE0PB1 = 0.0d;
    private double theE0PB2 = 0.0d;
    private double theBVB0 = 0.0d;
    private double theBVB1 = 0.0d;
    private double theBVB2 = 0.0d;
    private double theBVB3 = 0.0d;
    private double theBVB4 = 0.0d;
    private double theBSGB0 = 0.0d;
    private double theBSGB1 = 0.0d;
    private double theBSGB2 = 0.0d;
    private double theBSGB3 = 0.0d;
    private double theBSGB4 = 0.0d;
    private double theDHBR0 = 0.0d;
    private double theDHBR1 = 0.0d;
    private double theDHBR2 = 0.0d;
    private double theDSRB0 = 0.0d;
    private double theDSRB1 = 0.0d;
    private double theDSRB2 = 0.0d;
    private double theTAUB0 = 0.0d;
    private double theSBT0 = 0.0d;
    private double theSBT1 = 0.0d;
    private double theE0PU = 0.0d;
    private double theBVU0 = 0.0d;
    private double theBVU1 = 0.0d;
    private double theBSGU0 = 0.0d;
    private double theBSGU1 = 0.0d;
    private double theDHUR0 = 0.0d;
    private double theDHUR1 = 0.0d;
    private double theDSUR0 = 0.0d;
    private double theDSUR1 = 0.0d;
    private double theTAUU0 = 0.0d;
    private double theSUT0 = 0.0d;
    private double theSUT1 = 0.0d;
    private SVFMinimizer minimizer = null;

    /**
     * Default constructor. The graph processing framework
     * requires that an operator has a default constructor.
     */
    public MironovDielectricModel(final Operator op, final Product srcProduct, final Product tgtProduct,
                                  final double invalidSMValue,
                                  final double minSM, final double maxSM,
                                  final Band smBand, final Band qualityIndexBand,
                                  String rdcBandName) {

        super(op, srcProduct, tgtProduct, invalidSMValue, minSM, maxSM, smBand, qualityIndexBand, rdcBandName);

        //System.out.println("minSM = " + minSM + " maxSM = " + maxSM);
    }

    public void setParameters(final double effectiveSoilTemperatureCelsius) {

        this.effectiveSoilTemperatureCelsius = effectiveSoilTemperatureCelsius;

        if (!isValid(effectiveSoilTemperatureCelsius)) {

            throw new OperatorException("MironovDielectricModel::setParameters(): invalid effective soil temperature ");
        }
        //System.out.println("effective soil temperature (Celsius) = " + effectiveSoilTemperatureCelsius);
    }

    public void initialize() throws OperatorException {

        //System.out.println("effectiveSoilTemperatureCelsius = " + effectiveSoilTemperatureCelsius);

        minimizer = new SVFMinimizer(INVALID_SM_VALUE, minSM, maxSM, deltaFunc);

        clayBand = getSourceBand("Clay");
        INVALID_CLAY_VALUE = clayBand.getNoDataValue();
        /*
        int datatype = clayBand.getDataType();
        if (datatype != ProductData.TYPE_FLOAT64) {
            System.out.println("clayBand data type = " + datatype);
        }
        */

        try {
            final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(sourceProduct);
            theF = absRoot.getAttributeDouble(AbstractMetadata.radar_frequency) * Constants.oneMillion; // MHz to Hz

            //System.out.println("theF = " + theF);

            getConfigParameters();

        } catch (IOException e) {

            throw new OperatorException("MironovDielectricModel::initialize(): Failed to get config parameters ");
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

            final Tile smTile = targetTiles.get(smBand);
            final ProductData smData = smTile.getDataBuffer(); // target

            final Tile qualityIndexTile = targetTiles.get(qualityIndexBand);
            final ProductData qualityIndexData = qualityIndexTile.getDataBuffer(); // target

            final ProductData rdcData = rdcTile.getDataBuffer(); // source
            final ProductData clayData = clayTile.getDataBuffer(); // source

            final int maxy = ty0 + th;
            final int maxx = tx0 + tw;

            // Process pixel by pixel in the tile
            for (int y = ty0; y < maxy; y++) { // loop through rows

                for (int x = tx0; x < maxx; x++) { // loop through columns

                    // Don't assume all source bands are doubles.
                    int index = rdcTile.getDataBufferIndex(x, y);
                    int srcIndex = rdcTile.getDataBufferIndex(x, y);
                    int clayIndex = clayTile.getDataBufferIndex(x, y);

                    final double rdc = rdcData.getElemDoubleAt(srcIndex);

                    // Clay is in percentage
                    final double clay = clayData.getElemDoubleAt(clayIndex);

                    final double tsoilC = effectiveSoilTemperatureCelsius; // effective soil temperature in Celsius

                    double sm = INVALID_SM_VALUE;
                    long qualityIndex;

                    if (!isValid(rdc) || rdc == INVALID_RDC_VALUE) {

                        qualityIndex = QUALITY_INDEX_NO_RDC;

                    } else if (!isValid(clay) || clay == INVALID_CLAY_VALUE || clay < 0.0d) {

                        qualityIndex = QUALITY_INDEX_NO_SAND_OR_CLAY;

                    } else {

                        sm = invert(rdc, clay, tsoilC);
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

    // clay is in percentage
    // tsoilC is in Celsius
    private double invert(final double rdc, final double clay, final double tsoilC) {

        // Want to find the sm value in (minSM, maxSM) that minimizes
        // f(sm) = abs(rdc - computeRDC(sm, tsoilK, clay))
        // The fixed values are clay, effective soil temperature and rdc in that order. The order is IMPORTANT.
        return minimizer.minimize(new double[]{clay, tsoilC, rdc});
    }

    // soilMoisture is in m^3/m^3
    // clay is in percentage
    // tC is in Celsius
    private double computeRDC(final double soilMoisture, final double clay, final double tC) {

        final double c = clay / 100.0; // convert percentage to fraction

        final double c2 = c * c;
        final double c3 = c2 * c;
        final double c4 = c2 * c2;

        final double tK = tC + ZERO_C_IN_K; // convert from Celsius to Kelvin
        final double delta_tC_theTF0 = tC - theTF0;

        final double nd = theND0 + theND1 * c + theND2 * c2;

        final double kd = theKD0 + theKD1 * c;

        // maximum bound water fraction
        final double xmvt = theXMVT0 + theXMVT1 * c;

        final double e0pb = theE0PB0 + theE0PB1 * c + theE0PB2 * c2;

        final double Bvb = theBVB0 + theBVB1 * c + theBVB2 * c2 + theBVB3 * c3 + theBVB4 * c4;

        final double Bsgb = theBSGB0 + theBSGB1 * c + theBSGB2 * c2 + theBSGB3 * c3 +
                theBSGB4 * c4;

        final double Fpb = log((e0pb - 1) / (e0pb + 2));
        final double ep0b = (1 + 2 * exp(Fpb - Bvb * delta_tC_theTF0)) /
                (1 - exp(Fpb - Bvb * delta_tC_theTF0));

        // taub computation
        final double dHbR = theDHBR0 + theDHBR1 * c + theDHBR2 * c2;

        final double dSbR = theDSRB0 + theDSRB1 * c + theDSRB2 * c2;

        final double taub = theTAUB0 * exp(dHbR / tK - dSbR) / tK;

        // sigmab computation
        final double sigmabt = theSBT0 + theSBT1 * c;
        final double sigmab = sigmabt + Bsgb * delta_tC_theTF0;

        final double Bvu = theBVU0 + theBVU1 * c;

        final double Bsgu = theBSGU0 + theBSGU1 * c;
        final double Fpu = log((theE0PU - 1) / (theE0PU + 2)) - Bvu * delta_tC_theTF0;
        final double ep0u = (1 + 2 * exp(Fpu)) / (1 - exp(Fpu));

        // tauu computation
        final double dHuR = theDHUR0 + theDHUR1 * c;
        final double dSuR = theDSUR0 + theDSUR1 * c;
        final double tauu = theTAUU0 * exp(dHuR / tK - dSuR) / tK;

        // sigmau computation
        final double sigmaut = theSUT0 + theSUT1 * c;
        final double sigmau = sigmaut + Bsgu * delta_tC_theTF0;

        //-----------------------------------------------
        // computation of epsilon water (bound & unbound)
        final double twopiFtaub = 2 * Math.PI * theF * taub;
        final double twopiP0F = 2 * Math.PI * thePERMIT0 * theF;

        final double cxb = (ep0b - theEPWI0) / (1. + twopiFtaub * twopiFtaub);
        final double epwbx = theEPWI0 + cxb;
        final double epwby = cxb * twopiFtaub + sigmab / twopiP0F;

        final double twopiFtauu = 2 * Math.PI * theF * tauu;
        final double cxu = (ep0u - theEPWI0) / (1. + twopiFtauu * twopiFtauu);
        final double epwux = theEPWI0 + cxu;
        final double epwuy = cxu * twopiFtauu + sigmau / twopiP0F;

        // -----------------------------------------------------------
        // computation of refractive index of water (bound & unbound):
        final double epwbnorm = Math.sqrt(epwbx * epwbx + epwby * epwby);

        final double nb = Math.sqrt(epwbnorm + epwbx) / Constants.sqrt2;
        final double kb = Math.sqrt(epwbnorm - epwbx) / Constants.sqrt2;

        final double epwunorm = Math.sqrt(epwux * epwux + epwuy * epwuy);
        final double nu = Math.sqrt(epwunorm + epwux) / Constants.sqrt2;
        final double ku = Math.sqrt(epwunorm - epwux) / Constants.sqrt2;

        double rdc;

        final double sm = useSymmetrized ? Math.abs(soilMoisture) : soilMoisture;

        if (sm <= theSM1 && useSymmetrized) {

            // parabolic around SM=0 with 0 derivative @ SM=0
            // and equal derivative and values @ SM=SM1
            final double a = kd + kb * theSM1;
            final double b = nd + theSM1 * (nb - 1);

            final double ex1 = b * b - a * a;
            final double ey1 = 2.0d * a * b;

            final double dex1 = 2.0d * b * (nb - 1) - 2.0d * kb * a;
            final double dey1 = 2.0d * a * (nb - 1) + 2.0d * kb * b;

            final double ax = 0.5d * dex1 / theSM1;
            //final double ay = 0.5d*dey1/theSM1;

            final double cx = ex1 - 0.5d * dex1 * theSM1;
            //final double cy = ey1-0.5d*dey1*theSM1;

            rdc = ax * sm * sm + cx;
            // imaginary part = -(ay*sm*sm+cy);

        } else {

            // Normal case possibly using abs(sm) if symetrization is active
            final double xmvt2 = Math.min(sm, xmvt);

            final double nm = nd + (nb - 1.) * xmvt2 + ((sm >= xmvt) ?
                    (nu - 1.) * (sm - xmvt) : 0.0d);

            final double km = kd + kb * xmvt2 + ((sm >= xmvt) ? ku * (sm - xmvt) : 0.0d);

            // -----------------------------------------------------------
            // computation of soil dielectric constant:

            rdc = nm * nm - km * km;
            // imaginary part = -nm*km*2.;
        }

        return rdc;
    }

    private void getConfigParameters() throws IOException {

        final File configFolder = new File(Settings.instance().getAuxDataFolder(), "sm_luts");
        final File configFile = new File(configFolder, "soil_moisture.config");

        final PropertyMap configPref = new DefaultPropertyMap();
        configPref.load(configFile.toPath());

        useSymmetrized = configPref.getPropertyBool("Mironov.Use_Symmetrized_Model");
        theSM1 = configPref.getPropertyDouble("Mironov.SM1_Thld");

        thePERMIT0 = configPref.getPropertyDouble("Mironov.PERMIT0");
        theEPWI0 = configPref.getPropertyDouble("Mironov.EPWI0");
        theND0 = configPref.getPropertyDouble("Mironov.ND0");
        theND1 = configPref.getPropertyDouble("Mironov.ND1");
        theND2 = configPref.getPropertyDouble("Mironov.ND2");
        theKD0 = configPref.getPropertyDouble("Mironov.KD0");
        theKD1 = configPref.getPropertyDouble("Mironov.KD1");
        theXMVT0 = configPref.getPropertyDouble("Mironov.XMVT0");
        theXMVT1 = configPref.getPropertyDouble("Mironov.XMVT1");
        theTF0 = configPref.getPropertyDouble("Mironov.TF0");
        theE0PB0 = configPref.getPropertyDouble("Mironov.E0PB0");
        theE0PB1 = configPref.getPropertyDouble("Mironov.E0PB1");
        theE0PB2 = configPref.getPropertyDouble("Mironov.E0PB2");
        theBVB0 = configPref.getPropertyDouble("Mironov.BVB0");
        theBVB1 = configPref.getPropertyDouble("Mironov.BVB1");
        theBVB2 = configPref.getPropertyDouble("Mironov.BVB2");
        theBVB3 = configPref.getPropertyDouble("Mironov.BVB3");
        theBVB4 = configPref.getPropertyDouble("Mironov.BVB4");
        theBSGB0 = configPref.getPropertyDouble("Mironov.BSGB0");
        theBSGB1 = configPref.getPropertyDouble("Mironov.BSGB1");
        theBSGB2 = configPref.getPropertyDouble("Mironov.BSGB2");
        theBSGB3 = configPref.getPropertyDouble("Mironov.BSGB3");
        theBSGB4 = configPref.getPropertyDouble("Mironov.BSGB4");
        theDHBR0 = configPref.getPropertyDouble("Mironov.DHBR0");
        theDHBR1 = configPref.getPropertyDouble("Mironov.DHBR1");
        theDHBR2 = configPref.getPropertyDouble("Mironov.DHBR2");
        theDSRB0 = configPref.getPropertyDouble("Mironov.DSRB0");
        theDSRB1 = configPref.getPropertyDouble("Mironov.DSRB1");
        theDSRB2 = configPref.getPropertyDouble("Mironov.DSRB2");
        theTAUB0 = configPref.getPropertyDouble("Mironov.TAUB0");
        theSBT0 = configPref.getPropertyDouble("Mironov.SBT0");
        theSBT1 = configPref.getPropertyDouble("Mironov.SBT1");
        theE0PU = configPref.getPropertyDouble("Mironov.E0PU");
        theBVU0 = configPref.getPropertyDouble("Mironov.BVU0");
        theBVU1 = configPref.getPropertyDouble("Mironov.BVU1");
        theBSGU0 = configPref.getPropertyDouble("Mironov.BSGU0");
        theBSGU1 = configPref.getPropertyDouble("Mironov.BSGU1");
        theDHUR0 = configPref.getPropertyDouble("Mironov.DHUR0");
        theDHUR1 = configPref.getPropertyDouble("Mironov.DHUR1");
        theDSUR0 = configPref.getPropertyDouble("Mironov.DSUR0");
        theDSUR1 = configPref.getPropertyDouble("Mironov.DSUR1");
        theTAUU0 = configPref.getPropertyDouble("Mironov.TAUU0");
        theSUT0 = configPref.getPropertyDouble("Mironov.SUT0");
        theSUT1 = configPref.getPropertyDouble("Mironov.SUT1");

        /*
        System.out.println("useSymmetrized = " + useSymmetrized);
        System.out.println("theSM1 = " + theSM1);
        System.out.println("thePERMIT0 = " + thePERMIT0);
        System.out.println("theEPWI0 = " + theEPWI0);
        System.out.println("theND0 = " + theND0);
        System.out.println("theND1 = " + theND1);
        System.out.println("theND2 = " + theND2);
        System.out.println("theKD0 = " + theKD0);
        System.out.println("theKD1 = " + theKD1);
        System.out.println("theXMVT0 = " + theXMVT0);
        System.out.println("theXMVT1 = " + theXMVT1);
        System.out.println("theTF0 = " + theTF0);
        System.out.println("theE0PB0 = " + theE0PB0);
        System.out.println("theE0PB1 = " + theE0PB1);
        System.out.println("theE0PB2 = " + theE0PB2);
        System.out.println("theBVB0 = " + theBVB0);
        System.out.println("theBVB1 = " + theBVB1);
        System.out.println("theBVB2 = " + theBVB2);
        System.out.println("theBVB3 = " + theBVB3);
        System.out.println("theBVB4 = " + theBVB4);
        System.out.println("theBSGB0 = " + theBSGB0);
        System.out.println("theBSGB1 = " + theBSGB1);
        System.out.println("theBSGB2 = " + theBSGB2);
        System.out.println("theBSGB3 = " + theBSGB3);
        System.out.println("theBSGB4 = " + theBSGB4);
        System.out.println("theDHBR0 = " + theDHBR0);
        System.out.println("theDHBR1 = " + theDHBR1);
        System.out.println("theDHBR2 = " + theDHBR2);
        System.out.println("theDSRB0 = " + theDSRB0);
        System.out.println("theDSRB1 = " + theDSRB1);
        System.out.println("theDSRB2 = " + theDSRB2);
        System.out.println("theTAUB0 = " + theTAUB0);
        System.out.println("theSBT0 = " + theSBT0);
        System.out.println("theSBT1 = " + theSBT1);
        System.out.println("theE0PU = " + theE0PU);
        System.out.println("theBVU0 = " + theBVU0);
        System.out.println("theBVU1 = " + theBVU1);
        System.out.println("theBSGU0 = " + theBSGU0);
        System.out.println("theBSGU1 = " + theBSGU1);
        System.out.println("theDHUR0 = " + theDHUR0);
        System.out.println("theDHUR1 = " + theDHUR1);
        System.out.println("theDSUR0 = " + theDSUR0);
        System.out.println("theDSUR1 = " + theDSUR1);
        System.out.println("theTAUU0 = " + theTAUU0);
        System.out.println("theSUT0 = " + theSUT0);
        System.out.println("theSUT1 = " + theSUT1);
        */
    }

    class Delta implements SingleVarFunc {

        // The fixed values are clay, effective soil temperature and rdc in that order. The order is IMPORTANT.
        public double compute(double sm, double[] fixed) {

            final double clay = fixed[0]; // percentage
            final double tsoilC = fixed[1]; // Celsius
            final double rdc = fixed[2];

            final double modelledRDC = computeRDC(sm, clay, tsoilC);

            return Math.abs(rdc - modelledRDC);
        }
    }
}

