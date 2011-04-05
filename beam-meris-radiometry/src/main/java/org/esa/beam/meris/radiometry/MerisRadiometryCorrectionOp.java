/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.meris.radiometry;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.meris.radiometry.equalization.ReprocessingVersion;
import org.esa.beam.util.io.FileUtils;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static org.esa.beam.dataio.envisat.EnvisatConstants.*;


@OperatorMetadata(alias = "Meris.CorrectRadiometry",
                  description = "Performs radiometric corrections on MERIS L1b data products.",
                  authors = "Marc Bouvet (ESTEC); Marco Peters, Ralf Quast, Thomas Storm, Marco Zuehlke (Brockmann Consult)",
                  copyright = "(c) 2010 by Brockmann Consult",
                  version = "1.0")
public class MerisRadiometryCorrectionOp extends Operator {

    @Parameter(defaultValue = "true",
               label = "Perform calibration",
               description = "Whether to perform the calibration.")
    private boolean doCalibration;

    @Parameter(label = "Source radiometric correction file (optional)",
               description = "The radiometric correction auxiliary file for the source product.")
    private File sourceRacFile;

    @Parameter(label = "Target radiometric correction file (optional)",
               description = "The radiometric correction auxiliary file for the target product.")
    private File targetRacFile;

    @Parameter(defaultValue = "true",
               label = "Perform SMILE correction",
               description = "Whether to perform SMILE correction.")
    private boolean doSmile;

    @Parameter(defaultValue = "true",
               label = "Perform equalization",
               description = "Perform removal of detector-to-detector systematic radiometric differences in MERIS L1b data products.")
    private boolean doEqualization;

    @Parameter(label = "Reprocessing version", valueSet = {"AUTO_DETECT", "REPROCESSING_2", "REPROCESSING_3"},
               defaultValue = "AUTO_DETECT",
               description = "The version of the reprocessing the product comes from. Is only used if " +
                             "equalisation is enabled.")
    private ReprocessingVersion reproVersion;

    @Parameter(defaultValue = "false",
               label = "Perform radiance-to-reflectance conversion",
               description = "Whether to perform radiance-to-reflectance conversion.")
    private boolean doRadToRefl;

    @Parameter(label = "Write Envisat N1 File", description = "Writes the result to an Envisat N1 file.")
    private File n1File;

    @SourceProduct(alias = "source", label = "Name", description = "The source product.",
                   bands = {
                           MERIS_L1B_FLAGS_DS_NAME, MERIS_DETECTOR_INDEX_DS_NAME,
                           MERIS_L1B_RADIANCE_1_BAND_NAME,
                           MERIS_L1B_RADIANCE_2_BAND_NAME,
                           MERIS_L1B_RADIANCE_3_BAND_NAME,
                           MERIS_L1B_RADIANCE_4_BAND_NAME,
                           MERIS_L1B_RADIANCE_5_BAND_NAME,
                           MERIS_L1B_RADIANCE_6_BAND_NAME,
                           MERIS_L1B_RADIANCE_7_BAND_NAME,
                           MERIS_L1B_RADIANCE_8_BAND_NAME,
                           MERIS_L1B_RADIANCE_9_BAND_NAME,
                           MERIS_L1B_RADIANCE_10_BAND_NAME,
                           MERIS_L1B_RADIANCE_11_BAND_NAME,
                           MERIS_L1B_RADIANCE_12_BAND_NAME,
                           MERIS_L1B_RADIANCE_13_BAND_NAME,
                           MERIS_L1B_RADIANCE_14_BAND_NAME,
                           MERIS_L1B_RADIANCE_15_BAND_NAME
                   })
    private Product sourceProduct;

    @Override
    public void initialize() throws OperatorException {
        Map<String, Object> radioParams = new HashMap<String, Object>();
        radioParams.put("doCalibration", doCalibration);
        radioParams.put("sourceRacFile", sourceRacFile);
        radioParams.put("targetRacFile", targetRacFile);
        radioParams.put("doSmile", doSmile);
        radioParams.put("doEqualization", doEqualization);
        radioParams.put("reproVersion", reproVersion);
        radioParams.put("doRadToRefl", doRadToRefl);
        Product targetProduct = GPF.createProduct("Internal.CorrectRadiometry", radioParams, sourceProduct);
        if (n1File != null) {
            if (doRadToRefl) {
                throw new OperatorException(
                        "Radiance to reflectance conversion can not be performed if Envisat file shall be written.");
            }
            final HashMap<String, Object> n1Parameters = new HashMap<String, Object>();
            File targetN1File = FileUtils.exchangeExtension(n1File, ".N1");
            n1Parameters.put("patchedFile", targetN1File);
            final HashMap<String, Product> sourceProductMap = new HashMap<String, Product>();
            sourceProductMap.put("n1Product", sourceProduct);
            sourceProductMap.put("sourceProduct", targetProduct);
            targetProduct = GPF.createProduct("N1Patcher", n1Parameters, sourceProductMap);
        }
        setTargetProduct(targetProduct);
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(MerisRadiometryCorrectionOp.class);
        }
    }
}
