/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.beam.aatsr.sst;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.pointop.PixelOperator;
import org.esa.beam.framework.gpf.pointop.ProductConfigurer;
import org.esa.beam.framework.gpf.pointop.Sample;
import org.esa.beam.framework.gpf.pointop.SampleConfigurer;
import org.esa.beam.framework.gpf.pointop.WritableSample;
import org.esa.beam.framework.processor.ProcessorException;
import org.esa.beam.jai.ResolutionLevel;
import org.esa.beam.jai.VirtualBandOpImage;
import org.esa.beam.processor.sst.SstCoefficientLoader;
import org.esa.beam.processor.sst.SstCoefficientSet;
import org.esa.beam.processor.sst.SstCoefficients;
import org.esa.beam.processor.sst.SstConstants;
import org.esa.beam.util.ResourceInstaller;
import org.esa.beam.util.SystemUtils;

import javax.media.jai.OpImage;
import java.awt.image.Raster;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * An operator for computing sea surface temperature from (A)ATSR products.
 *
 * @author Tom Block
 * @author Ralf Quast
 */
@OperatorMetadata(alias = "Aatsr.SST", authors = "Tom Block, Ralf Quast", copyright = "Brockmann Consult GmbH",
                  version = "2.0",
                  description = "Computes sea surface temperature (SST) from (A)ATSR products.")
public class AatsrSstOp extends PixelOperator {

    private static final float COEFF_0_SCALE = 1.0f;

    private static final String SST_AUXDATA_DIR_PROPERTY = "sst.auxdata.dir";
    private static final String NADIR_SST_BAND_NAME = "nadir_sst";
    private static final String DUAL_SST_BAND_NAME = "dual_sst";

    private enum Files {
        AVERAGE_POLAR_DUAL_VIEW("Average polar dual view", "AV_POL_DUAL.coef"),
        AVERAGE_TEMPERATE_DUAL_VIEW("Average temperate dual view", "AV_TEM_DUAL.coef"),
        AVERAGE_TROPICAL_DUAL_VIEW("Average tropical dual view", "AV_TRO_DUAL.coef"),
        GRIDDED_POLAR_DUAL_VIEW("Gridded polar dual view", "GR_POL_DUAL.coef"),
        GRIDDED_TEMPERATE_DUAL_VIEW("Gridded temperate dual view", "GR_TEM_DUAL.coef"),
        GRIDDED_TROPICAL_DUAL_VIEW("Gridded tropical dual view", "GR_TRO_DUAL.coef"),
        AVERAGE_POLAR_SINGLE_VIEW("Average polar single view", "AV_POL_SING.coef"),
        AVERAGE_TEMPERATE_SINGLE_VIEW("Average temperate single view", "AV_TEM_SING.coef"),
        AVERAGE_TROPICAL_SINGLE_VIEW("Average tropical single view", "AV_TRO_SING.coef"),
        GRIDDED_POLAR_SINGLE_VIEW("Gridded polar single view", "GR_POL_SING.coef"),
        GRIDDED_TEMPERATE_SINGLE_VIEW("Gridded temperate single view", "GR_TEM_SING.coef"),
        GRIDDED_TROPICAL_SINGLE_VIEW("Gridded tropical single view", "GR_TRO_SING.coef"),
        GRIDDED_DUAL_VIEW_IPF("Gridded dual view (IPF)", "GR_IPF_DUAL.coef");

        private final String label;
        private final String filename;

        private Files(String label, String filename) {
            this.label = label;
            this.filename = filename;
        }

        @Override
        public String toString() {
            return label;
        }

        private URL getURL(File dir) throws MalformedURLException {
            return new File(dir, filename).toURI().toURL();
        }
    }

    @SourceProduct(alias = "source",
                   description = "The path of the (A)ATSR source product",
                   label = "(A)ATSR source product",
                   bands = {
                           SstConstants.NADIR_370_BAND,
                           SstConstants.NADIR_1100_BAND,
                           SstConstants.NADIR_1200_BAND
                   })
    private Product sourceProduct;

    @Parameter(defaultValue = "true",
               label = SstConstants.PROCESS_DUAL_VIEW_SST_LABELTEXT,
               description = SstConstants.PROCESS_DUAL_VIEW_SST_DESCRIPTION)
    private boolean dual;

    @Parameter(defaultValue = "AVERAGE_POLAR_DUAL_VIEW", label = "Dual-view coefficient file",
               description = SstConstants.DUAL_VIEW_COEFF_FILE_DESCRIPTION,
               valueSet = {
                       "AVERAGE_POLAR_DUAL_VIEW", "AVERAGE_TEMPERATE_DUAL_VIEW", "AVERAGE_TROPICAL_DUAL_VIEW",
                       "GRIDDED_POLAR_DUAL_VIEW", "GRIDDED_TEMPERATE_DUAL_VIEW", "GRIDDED_TROPICAL_DUAL_VIEW",
                       "GRIDDED_DUAL_VIEW_IPF"
               })
    private Files dualCoefficientsFile;

    @Parameter(defaultValue = SstConstants.DEFAULT_DUAL_VIEW_BITMASK, label = "Dual-view mask",
               description = "Mask used for the dual-view SST")  // todo - use ExpressionEditor
    private String dualMaskExpression;

    @Parameter(defaultValue = "true", label = SstConstants.PROCESS_NADIR_VIEW_SST_LABELTEXT,
               description = SstConstants.PROCESS_NADIR_VIEW_SST_DESCRIPTION)
    private boolean nadir;

    @Parameter(defaultValue = "AVERAGE_POLAR_SINGLE_VIEW", label = "Nadir-view coefficient file",
               description = SstConstants.NADIR_VIEW_COEFF_FILE_DESCRIPTION,
               valueSet = {
                       "AVERAGE_POLAR_SINGLE_VIEW", "AVERAGE_TEMPERATE_SINGLE_VIEW", "AVERAGE_TROPICAL_SINGLE_VIEW",
                       "GRIDDED_POLAR_SINGLE_VIEW", "GRIDDED_TEMPERATE_SINGLE_VIEW", "GRIDDED_TROPICAL_SINGLE_VIEW"
               })
    private Files nadirCoefficientsFile;

    @Parameter(defaultValue = SstConstants.DEFAULT_NADIR_VIEW_BITMASK, label = "Nadir-view mask",
               description = "Mask used for the nadir-view SST")  // todo - use ExpressionEditor
    private String nadirMaskExpression;

    @Parameter(defaultValue = "-999.0f", label = "Invalid SST value",
               description = "Value used to fill invalid SST pixels")
    private float invalidSstValue;

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

        if (nadirMaskOpImage != null) {
            nadirMaskOpImage.dispose();
        }
        if (dualMaskOpImage != null) {
            dualMaskOpImage.dispose();
        }
    }

    @Override
    protected void computePixel(int x, int y, Sample[] sourceSamples, WritableSample[] targetSamples) {
        if (nadir) {
            if (isMasked(nadirMaskOpImage, x, y)) {
                final float ir37 = sourceSamples[0].getFloat();
                final float ir11 = sourceSamples[1].getFloat();
                final float ir12 = sourceSamples[2].getFloat();
                final float sea = sourceSamples[3].getFloat();

                final int i = nadirCoefficientIndexes[x];
                final float nadirSst = computeNadirSst(i, ir37, ir11, ir12, sea);

                targetSamples[0].set(nadirSst);
            } else {
                targetSamples[0].set(invalidSstValue);
            }
        }
        if (dual) {
            if (isMasked(dualMaskOpImage, x, y)) {
                final float ir37N = sourceSamples[0].getFloat();
                final float ir11N = sourceSamples[1].getFloat();
                final float ir12N = sourceSamples[2].getFloat();
                final float ir37F = sourceSamples[4].getFloat();
                final float ir11F = sourceSamples[5].getFloat();
                final float ir12F = sourceSamples[6].getFloat();

                final float seaN = sourceSamples[3].getFloat();
                final float seaF = sourceSamples[7].getFloat();

                final int i = dualCoefficientIndexes[x];
                final float dualSst = computeDualSst(i, ir37N, ir11N, ir12N, ir37F, ir11F, ir12F, seaN, seaF);

                targetSamples[1].set(dualSst);
            } else {
                targetSamples[1].set(invalidSstValue);
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

        if (dual) {
            sampleConfigurer.defineSample(4, SstConstants.FORWARD_370_BAND);
            sampleConfigurer.defineSample(5, SstConstants.FORWARD_1100_BAND);
            sampleConfigurer.defineSample(6, SstConstants.FORWARD_1200_BAND);
            sampleConfigurer.defineSample(7, SstConstants.SUN_ELEV_FORWARD);
        }
    }

    @Override
    protected void configureTargetSamples(SampleConfigurer sampleConfigurer) throws OperatorException {
        if (nadir) {
            sampleConfigurer.defineSample(0, NADIR_SST_BAND_NAME);
        }
        if (dual) {
            sampleConfigurer.defineSample(1, DUAL_SST_BAND_NAME);
        }
    }

    @Override
    protected void configureTargetProduct(ProductConfigurer productConfigurer) {
        super.configureTargetProduct(productConfigurer);

        if (nadir) {
            final Band nadirSstBand = productConfigurer.addBand(NADIR_SST_BAND_NAME, ProductData.TYPE_FLOAT32);
            nadirSstBand.setUnit(SstConstants.OUT_BAND_UNIT);
            nadirSstBand.setDescription(SstConstants.OUT_BAND_NADIR_DESCRIPTION);
            nadirSstBand.setGeophysicalNoDataValue(invalidSstValue);
            nadirSstBand.setNoDataValueUsed(true);
        }
        if (dual) {
            final Band dualSstBand = productConfigurer.addBand(DUAL_SST_BAND_NAME, ProductData.TYPE_FLOAT32);
            dualSstBand.setUnit(SstConstants.OUT_BAND_UNIT);
            dualSstBand.setDescription(SstConstants.OUT_BAND_DUAL_DESCRIPTION);
            dualSstBand.setGeophysicalNoDataValue(invalidSstValue);
            dualSstBand.setNoDataValueUsed(true);
        }
    }

    @Override
    protected void prepareInputs() throws OperatorException {
        super.prepareInputs();

        final File auxdataDir = installAuxiliaryData();
        if (nadir) {
            initNadirCoefficients(auxdataDir);
            if (nadirMaskExpression != null && !nadirMaskExpression.isEmpty()) {
                nadirMaskOpImage = VirtualBandOpImage.createMask(nadirMaskExpression, sourceProduct,
                                                                 ResolutionLevel.MAXRES);
            }
        }
        if (dual) {
            initDualCoefficients(auxdataDir);
            if (dualMaskExpression != null && !dualMaskExpression.isEmpty()) {
                dualMaskOpImage = VirtualBandOpImage.createMask(dualMaskExpression, sourceProduct,
                                                                ResolutionLevel.MAXRES);
            }
        }
    }

    private void initNadirCoefficients(File auxdataDir) throws OperatorException {
        final SstCoefficientLoader loader = new SstCoefficientLoader();
        final SstCoefficientSet coefficientSet;
        try {
            coefficientSet = loader.load(nadirCoefficientsFile.getURL(auxdataDir));
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

    private void initDualCoefficients(File auxdataDir) throws OperatorException {
        final SstCoefficientLoader loader = new SstCoefficientLoader();
        final SstCoefficientSet coefficientSet;
        try {
            coefficientSet = loader.load(dualCoefficientsFile.getURL(auxdataDir));
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

    private File installAuxiliaryData() {
        final File defaultTargetDir = new File(SystemUtils.getApplicationDataDir(), "beam-aatsr-sst/auxdata/aatsr/sst");
        final String targetPath = System.getProperty(SST_AUXDATA_DIR_PROPERTY, defaultTargetDir.getAbsolutePath());
        final File targetDir = new File(targetPath);

        final URL url = ResourceInstaller.getSourceUrl(getClass());
        final ResourceInstaller installer = new ResourceInstaller(url, "auxdata/aatsr/sst", targetDir);
        try {
            installer.install(".*", ProgressMonitor.NULL);
        } catch (IOException e) {
            throw new OperatorException(e);
        }

        return targetDir;
    }

    private static boolean isMasked(final OpImage maskOpImage, final int x, final int y) {
        if (maskOpImage == null) {
            return true;
        }
        final int tileX = maskOpImage.XToTileX(x);
        final int tileY = maskOpImage.YToTileY(y);
        final Raster tile = maskOpImage.getTile(tileX, tileY);

        return tile.getSample(x, y, 0) != 0;
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(AatsrSstOp.class);
        }
    }
}
