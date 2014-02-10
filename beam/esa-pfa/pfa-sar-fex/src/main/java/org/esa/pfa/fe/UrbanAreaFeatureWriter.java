/*
 * Copyright (C) 2013 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.pfa.fe;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.Stx;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.pfa.fe.op.Feature;
import org.esa.pfa.fe.op.FeatureType;
import org.esa.pfa.fe.op.Patch;
import org.esa.pfa.fe.op.out.PatchOutput;

import java.awt.*;
import java.awt.image.RenderedImage;
import java.io.IOException;

/**
 * Output features into patches
 */
@OperatorMetadata(alias = "UrbanAreaFeatureWriter",
        authors = "Jun Lu, Luis Veci",
        copyright = "Copyright (C) 2013 by Array Systems Computing Inc.",
        description = "Writes features into patches.",
        category = "Classification\\Feature Extraction")
public class UrbanAreaFeatureWriter extends FeatureWriter {

    public static final String featureBandName = "_speckle_divergence";

    private static final double Tdsl = 0.4; // threshold for detection adopted from Esch's paper.

    private FeatureType[] featureTypes;

    public UrbanAreaFeatureWriter() {
        setRequiresAllBands(true);
    }

    @Override
    protected FeatureType[] getFeatureTypes() {
        if (featureTypes == null) {
            featureTypes = new FeatureType[]{
                    /*00*/ new FeatureType("patch", "Patch product", Product.class),
                    /*01*/ new FeatureType("sigma0_ql", "Sigma0 quicklook", RenderedImage.class),
                    /*02*/ new FeatureType("speckle_divergence_ql", "Speckle_divergence quicklook", RenderedImage.class),
                    /*03*/ new FeatureType("speckle_divergence", "Speckle divergence statistics", STX_ATTRIBUTE_TYPES),
                    /*04*/ new FeatureType("speckle_divergence.percentOverPnt4", "Sample percent over threshold of 0.4", Double.class),
                    /*05*/ new FeatureType("speckle_divergence.largestConnectedBlob", "Largest connected cluster size as a percent of patch", Double.class),
            };
        }
        return featureTypes;
    }

    @Override
    protected boolean processPatch(Patch patch, PatchOutput patchOutput) throws IOException {
        if (skipFeaturesOutput && skipQuicklookOutput && skipProductOutput) {
            return false;
        }

        final int patchX = patch.getPatchX();
        final int patchY = patch.getPatchY();
        final Product patchProduct = patch.getPatchProduct();

        final int numPixelsRequired = patchWidth * patchHeight;
        final int numPixelsTotal = patchProduct.getSceneRasterWidth() * patchProduct.getSceneRasterHeight();

        final double patchPixelRatio = numPixelsTotal / (double) numPixelsRequired;
        if (patchPixelRatio < 0.6) {
            getLogger().warning(String.format("Rejected patch x%dy%d, patchPixelRatio=%f%%", patchX, patchY,
                    patchPixelRatio * 100));
            return false;
        }

        final Product featureProduct = patch.getPatchProduct();
        final Band targetBand = getFeatureBand(featureProduct);

        final int tw = targetBand.getRasterWidth();
        final int th = targetBand.getRasterHeight();
        final double patchSize = tw*th;

        final Stx stx = targetBand.getStx();
        final double pctValid = (stx.getSampleCount()/patchSize);
        if(pctValid < minValidPixels)
            return false;

        final Tile srcTile = getSourceTile(targetBand, new Rectangle(0, 0, tw, th));
        final double[] dataArray = new double[tw*th];

        final RegionGrower blob = new RegionGrower(srcTile);
        blob.run(Tdsl, dataArray);
        final double maxClusterSize = blob.getMaxClusterSize();
        final int numSamplesOverThreshold = blob.getNumSamples();

        final double pctOverPnt4 = numSamplesOverThreshold/patchSize;

        if(pctOverPnt4 < minValidPixels)
            return false;

        final Feature[] features = {
                new Feature(featureTypes[0], featureProduct),
                new Feature(featureTypes[1], createColoredBandImage(featureProduct.getBandAt(0), 0, 1)),
                new Feature(featureTypes[2], createColoredBandImage(featureProduct.getBandAt(1), 0, 1)),
                createStxFeature(featureTypes[3], targetBand),
                new Feature(featureTypes[4], pctOverPnt4),
                new Feature(featureTypes[5], maxClusterSize/patchSize),
        };

        patchOutput.writePatch(patch, features);

        return true;
    }

    private static Band getFeatureBand(final Product product) throws OperatorException {
        for(Band b : product.getBands()) {
            if(!b.getName().contains(featureBandName)) {
                return b;
            }
        }
        throw new OperatorException(featureBandName +" not found");
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(UrbanAreaFeatureWriter.class);
        }
    }
}