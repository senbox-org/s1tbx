package org.esa.beam.processor.sst;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.pointop.PixelOperator;
import org.esa.beam.framework.gpf.pointop.ProductConfigurer;
import org.esa.beam.framework.gpf.pointop.Sample;
import org.esa.beam.framework.gpf.pointop.SampleConfigurer;
import org.esa.beam.framework.gpf.pointop.WritableSample;
import org.esa.beam.framework.processor.ProcessorException;
import org.esa.beam.gpf.operators.standard.BandMathsOp;
import org.esa.beam.jai.ResolutionLevel;
import org.esa.beam.jai.VirtualBandOpImage;

import javax.media.jai.OpImage;
import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;

public class SstOp extends PixelOperator {

    private static final float COEFF_0_SCALE = 1.0f;

    private static final String NADIR_SST_BAND_NAME = "nadir_sst";
    private static final String DUAL_SST_BAND_NAME = "dual_sst";

    @Parameter
    private File nadirCoefficientsFile;

    @Parameter
    private String nadirMaskExpression;

    @Parameter(defaultValue = "false")
    private boolean dual;

    @Parameter
    private File dualCoefficientsFile;

    @Parameter
    private String dualMaskExpression;

    @Parameter(defaultValue = "999.0f")
    private float noDataValue;

    private transient float[] a0;
    private transient float[] a1;
    private transient float[] a2;
    private transient float[] b0;
    private transient float[] b1;
    private transient float[] b2;
    private transient float[] b3;
    private transient float[] c0;
    private transient float[] c1;
    private transient float[] c2;
    private transient float[] c3;
    private transient float[] c4;
    private transient float[] d0;
    private transient float[] d1;
    private transient float[] d2;
    private transient float[] d3;
    private transient float[] d4;
    private transient float[] d5;
    private transient float[] d6;

    private transient int[] nadirCoefficientIndexes;
    private transient int[] dualCoefficientIndexes;

    private transient OpImage nadirMaskOpImage;
    private transient OpImage dualMaskOpImage;

    @Override
    public void dispose() {
        super.dispose();

        nadirMaskOpImage.dispose();
        if (dualMaskOpImage != null) {
            dualMaskOpImage.dispose();
        }
    }

    @Override
    protected void prepareInputs() throws OperatorException {
        super.prepareInputs();

        initNadirCoefficients();
        nadirMaskOpImage = VirtualBandOpImage.createMask(nadirMaskExpression, getSourceProduct(),
                                                         ResolutionLevel.MAXRES);
        if (dual) {
            initDualCoefficients();
            dualMaskOpImage = VirtualBandOpImage.createMask(dualMaskExpression, getSourceProduct(),
                                                            ResolutionLevel.MAXRES);
        }
    }

    @Override
    protected void computePixel(int x, int y, Sample[] sourceSamples, WritableSample[] targetSamples) {
        if (nadirMaskOpImage.getData(new Rectangle(x, y, 1, 1)).getSample(0, 0, 0) != 0) {
            final float ir37 = sourceSamples[0].getFloat();
            final float ir11 = sourceSamples[1].getFloat();
            final float ir12 = sourceSamples[2].getFloat();
            final float sea = sourceSamples[3].getFloat();

            final int i = nadirCoefficientIndexes[x];

            targetSamples[0].set(computeNadirSst(i, ir37, ir11, ir12, sea));
        } else {
            targetSamples[0].set(noDataValue);
        }

        if (dual) {
            if (dualMaskOpImage.getData(new Rectangle(x, y, 1, 1)).getSample(0, 0, 0) != 0) {
                final float ir37N = sourceSamples[0].getFloat();
                final float ir11N = sourceSamples[1].getFloat();
                final float ir12N = sourceSamples[2].getFloat();
                final float ir37F = sourceSamples[4].getFloat();
                final float ir11F = sourceSamples[5].getFloat();
                final float ir12F = sourceSamples[6].getFloat();

                final float seaN = sourceSamples[3].getFloat();
                final float seaF = sourceSamples[7].getFloat();

                final int i = dualCoefficientIndexes[x];

                targetSamples[1].set(computeDualSst(i, ir37N, ir11N, ir12N, ir37F, ir11F, ir12F, seaN, seaF));
            } else {
                targetSamples[1].set(noDataValue);
            }
        }
    }

    private float computeNadirSst(int i, float ir37, float ir11, float ir12, float sea) {
        // is night?
        if (sea < 0.0f && ir37 > 0.0f) {
            return b0[i] + b1[i] * ir11 + b2[i] * ir12 + b3[i] * ir37;
        } else {
            return a0[i] + a1[i] * ir11 + a2[i] * ir12;
        }
    }

    private float computeDualSst(int i, float ir37N, float ir11N, float ir12N, float ir37F, float ir11F,
                                 float ir12F, float seaN, float seaF) {
        // is night?
        if (seaN < 0.0f && seaF < 0.0f && ir37N > 0.0f && ir37F > 0.0f) {
            return d0[i] + d1[i] * ir11N + d2[i] * ir12N + d3[i] * ir37N + d4[i] * ir11F + d5[i] * ir12F + d6[i] * ir37F;
        } else {
            return c0[i] + c1[i] * ir11N + c2[i] * ir12N + c3[i] * ir11F + c4[i] * ir12F;
        }
    }

    @Override
    protected void configureSourceSamples(SampleConfigurer sampleConfigurer) throws OperatorException {
        sampleConfigurer.defineSample(0, SstConstants.NADIR_370_BAND);
        sampleConfigurer.defineSample(1, SstConstants.NADIR_1100_BAND);
        sampleConfigurer.defineSample(2, SstConstants.NADIR_1200_BAND);
        sampleConfigurer.defineSample(3, SstConstants.SUN_ELEV_NADIR);

        /*
        final BandMathsOp nadirMaskOp = BandMathsOp.createBooleanExpressionBand(nadirMaskExpression, getSourceProduct());
        Product nadirMaskProduct = nadirMaskOp.getTargetProduct();
        sampleConfigurer.defineSample(10, nadirMaskProduct.getBandAt(0).getName(), nadirMaskProduct);
        */

        if (dual) {
            sampleConfigurer.defineSample(4, SstConstants.FORWARD_370_BAND);
            sampleConfigurer.defineSample(5, SstConstants.FORWARD_1100_BAND);
            sampleConfigurer.defineSample(6, SstConstants.FORWARD_1200_BAND);
            sampleConfigurer.defineSample(7, SstConstants.SUN_ELEV_FORWARD);
        }
    }

    @Override
    protected void configureTargetSamples(SampleConfigurer sampleConfigurer) throws OperatorException {
        sampleConfigurer.defineSample(0, NADIR_SST_BAND_NAME);

        if (dual) {
            sampleConfigurer.defineSample(1, DUAL_SST_BAND_NAME);
        }
    }

    @Override
    protected void configureTargetProduct(ProductConfigurer productConfigurer) {
        super.configureTargetProduct(productConfigurer);

        final Band nadirSstBand = productConfigurer.addBand(NADIR_SST_BAND_NAME, ProductData.TYPE_FLOAT32);
        nadirSstBand.setUnit(SstConstants.OUT_BAND_UNIT);
        nadirSstBand.setDescription(SstConstants.OUT_BAND_NADIR_DESCRIPTION);
        nadirSstBand.setGeophysicalNoDataValue(noDataValue);
        nadirSstBand.setNoDataValueUsed(true);

        if (dual) {
            final Band dualSstBand = productConfigurer.addBand(DUAL_SST_BAND_NAME, ProductData.TYPE_FLOAT32);
            dualSstBand.setUnit(SstConstants.OUT_BAND_UNIT);
            dualSstBand.setDescription(SstConstants.OUT_BAND_DUAL_DESCRIPTION);
            dualSstBand.setGeophysicalNoDataValue(noDataValue);
            dualSstBand.setNoDataValueUsed(true);
        }
    }

    private void initNadirCoefficients() throws OperatorException {
        final SstCoefficientLoader loader = new SstCoefficientLoader();
        final SstCoefficientSet coefficientSet;
        try {
            coefficientSet = loader.load(nadirCoefficientsFile.toURI().toURL());
        } catch (IOException e) {
            throw new OperatorException(e);
        } catch (ProcessorException e) {
            throw new OperatorException(e);
        }

        final int numCoeffs = coefficientSet.getNumCoefficients();

        // get the highest map pixel
        int maxIndex = 0;
        for (int i = 0; i < numCoeffs; i++) {
            final int endIndex = coefficientSet.getCoefficientsAt(i).getEnd();
            if (endIndex > maxIndex) {
                maxIndex = endIndex;
            }
        }
        // index is zero based
        nadirCoefficientIndexes = new int[maxIndex + 1];

        a0 = new float[numCoeffs];
        a1 = new float[numCoeffs];
        a2 = new float[numCoeffs];
        b0 = new float[numCoeffs];
        b1 = new float[numCoeffs];
        b2 = new float[numCoeffs];
        b3 = new float[numCoeffs];

        for (int i = 0; i < numCoeffs; i++) {
            final SstCoefficients coefficients = coefficientSet.getCoefficientsAt(i);

            for (int k = coefficients.getStart(); k <= coefficients.getEnd(); k++) {
                nadirCoefficientIndexes[k] = i;
            }

            final float[] aCoefficients = coefficients.get_A_Coeffs();
            if (aCoefficients == null) {
                throw new OperatorException("Invalid coefficient file: no nadir view \"a\" coefficients set");
            }

            a0[i] = aCoefficients[0] * COEFF_0_SCALE;
            a1[i] = aCoefficients[1];
            a2[i] = aCoefficients[2];

            final float[] bCoefficients = coefficients.get_B_Coeffs();
            if (bCoefficients == null) {
                throw new OperatorException("Invalid coefficient file: no nadir view \"b\" coefficients set");
            }

            b0[i] = bCoefficients[0] * COEFF_0_SCALE;
            b1[i] = bCoefficients[1];
            b2[i] = bCoefficients[2];
            b3[i] = bCoefficients[3];
        }
    }

    private void initDualCoefficients() throws OperatorException {
        final SstCoefficientLoader loader = new SstCoefficientLoader();
        final SstCoefficientSet coefficientSet;
        try {
            coefficientSet = loader.load(dualCoefficientsFile.toURI().toURL());
        } catch (IOException e) {
            throw new OperatorException(e);
        } catch (ProcessorException e) {
            throw new OperatorException(e);
        }

        final int n = coefficientSet.getNumCoefficients();

        // get the highest map pixel
        int maxIndex = 0;
        for (int i = 0; i < n; i++) {
            final int endIndex = coefficientSet.getCoefficientsAt(i).getEnd();
            if (endIndex > maxIndex) {
                maxIndex = endIndex;
            }
        }
        // index is zero based
        dualCoefficientIndexes = new int[maxIndex + 1];

        c0 = new float[n];
        c1 = new float[n];
        c2 = new float[n];
        c3 = new float[n];
        c4 = new float[n];
        d0 = new float[n];
        d1 = new float[n];
        d2 = new float[n];
        d3 = new float[n];
        d4 = new float[n];
        d5 = new float[n];
        d6 = new float[n];

        for (int i = 0; i < n; i++) {
            final SstCoefficients coefficients = coefficientSet.getCoefficientsAt(i);

            for (int k = coefficients.getStart(); k <= coefficients.getEnd(); k++) {
                dualCoefficientIndexes[k] = i;
            }

            final float[] cCoefficients = coefficients.get_C_Coeffs();
            if (cCoefficients == null) {
                throw new OperatorException("Invalid coefficient file: no dual view \"c\" coefficients set");
            }

            c0[i] = cCoefficients[0] * COEFF_0_SCALE;
            c1[i] = cCoefficients[1];
            c2[i] = cCoefficients[2];
            c3[i] = cCoefficients[3];
            c4[i] = cCoefficients[4];

            final float[] dCoefficients = coefficients.get_D_Coeffs();
            if (dCoefficients == null) {
                throw new OperatorException("Invalid coefficient file: no dual view \"d\" coefficients set");
            }

            d0[i] = dCoefficients[0] * COEFF_0_SCALE;
            d1[i] = dCoefficients[1];
            d2[i] = dCoefficients[2];
            d3[i] = dCoefficients[3];
            d4[i] = dCoefficients[4];
            d5[i] = dCoefficients[5];
            d6[i] = dCoefficients[6];
        }
    }
}
