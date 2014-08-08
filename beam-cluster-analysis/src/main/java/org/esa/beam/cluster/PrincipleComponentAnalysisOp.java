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
package org.esa.beam.cluster;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.Validator;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.FlagCoding;
import org.esa.beam.framework.datamodel.Mask;
import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
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
import org.esa.beam.framework.ui.GeneralExpressionConverter;
import org.esa.beam.jai.ImageManager;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.util.StringUtils;
import org.esa.beam.util.math.MathUtils;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;

/**
 * Implements a Principle Component Analysis.
 *
 * @author Norman Fomferra
 * @since BEAM 5
 */
@OperatorMetadata(alias = "PCA",
                  category = "Classification",
                  version = "1.0",
                  authors = "Norman Fomferra",
                  copyright = "(c) 2013 by Brockmann Consult",
                  description = "Performs a Principle Component Analysis.")
public class PrincipleComponentAnalysisOp extends Operator {

    @SourceProduct(alias = "source", label = "Source product", description="The source product.")
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct;

    @Parameter(label = "Source band names",
               description = "The names of the bands being used for the cluster analysis.",
               rasterDataNodeType = Band.class)
    private String[] sourceBandNames;

    @Parameter(label = "Maximum component count",
               description = "The maximum number of principle components to compute.",
               defaultValue = "-1")
    private int componentCount;

    @Parameter(label = "ROI mask name",
               description = "The name of the ROI mask that should be used.",
               defaultValue = "",
               rasterDataNodeType = Mask.class/*,
               validator = RoiMaskNameValidator.class*/)
    private String roiMaskName;

    @Parameter(label = "ROI mask expression",
               description = "The expression of the ROI mask that should be used. If not given, a mask given by 'roiMaskName' must already exist.",
               defaultValue = "",
               validator = RoiMaskExprValidator.class,
               converter = GeneralExpressionConverter.class)
    private String roiMaskExpr;

    @Parameter(label = "Remove non-ROI pixels",
               description = "Removes all non-ROI pixels in the target product.",
               defaultValue = "false")
    private boolean removeNonRoiPixels;

    private transient Roi roi;
    private transient Band[] sourceBands;
    private transient PrincipleComponentAnalysis pca;
    private transient Band[] componentBands;
    private transient Band responseBand;
    private transient Band errorBand;
    private transient Band flagsBand;

    public PrincipleComponentAnalysisOp() {
    }

    @Override
    public void initialize() throws OperatorException {
        collectSourceBands();

        if (componentCount <= 0 || componentCount > sourceBands.length) {
            componentCount = sourceBands.length;
        }

        int width = sourceProduct.getSceneRasterWidth();
        int height = sourceProduct.getSceneRasterHeight();
        final String name = sourceProduct.getName() + "_PCA";
        final String type = sourceProduct.getProductType() + "_PCA";

        targetProduct = new Product(name, type, width, height);
        ProductUtils.copyTiePointGrids(sourceProduct, targetProduct);
        ProductUtils.copyGeoCoding(sourceProduct, targetProduct);
        targetProduct.setStartTime(sourceProduct.getStartTime());
        targetProduct.setEndTime(sourceProduct.getEndTime());

        componentBands = new Band[componentCount];
        for (int i = 0; i < componentCount; i++) {
            final Band componentBand = targetProduct.addBand("component_" + (i + 1), ProductData.TYPE_FLOAT32);
            // Reuse spectral properties for components --> allow for analysis in spectrum view
            // Although, this is geo-physical nonsense.
            ProductUtils.copySpectralBandProperties(sourceBands[i], componentBand);
            componentBands[i] = componentBand;
        }
        responseBand = targetProduct.addBand("response", ProductData.TYPE_FLOAT32);
        errorBand = targetProduct.addBand("error", ProductData.TYPE_FLOAT32);
        flagsBand = targetProduct.addBand("flags", ProductData.TYPE_UINT8);
        final FlagCoding flags = new FlagCoding("flags");
        flags.addFlag("PCA_ROI_PIXEL", 0x01, "Pixel has been used to perform the PCA.");
        flagsBand.setSampleCoding(flags);
        targetProduct.getFlagCodingGroup().add(flags);
        targetProduct.addMask("pca_roi_pixel", "flags.PCA_ROI_PIXEL", "Pixel has been used to perform the PCA.", Color.RED, 0.5);
        targetProduct.addMask("pca_non_roi_pixel", "!flags.PCA_ROI_PIXEL", "Pixel has not been used to perform the PCA.", Color.BLACK, 0.5);

        if (removeNonRoiPixels) {
            for (Band componentBand : componentBands) {
                componentBand.setValidPixelExpression("flags.PCA_ROI_PIXEL");
            }
            responseBand.setValidPixelExpression("flags.PCA_ROI_PIXEL");
            errorBand.setValidPixelExpression("flags.PCA_ROI_PIXEL");
        }

        if (StringUtils.isNullOrEmpty(roiMaskName)) {
            roiMaskName = "pca_roi_pixel";
        }

        if (StringUtils.isNotNullAndNotEmpty(roiMaskExpr)) {
            sourceProduct.addMask(roiMaskName, roiMaskExpr, "PCA ROI mask", Color.RED, 0.5);
        }

        if (sourceProduct.getMaskGroup().get(roiMaskName) == null) {
            throw new OperatorException("Missing mask '" + roiMaskName + "' in source product.");
        }

        // note - in BEAM 5, we shall have a progress monitor here!!! (NF)
        initPca(ProgressMonitor.NULL);

        targetProduct.getMetadataRoot().addElement(createPcaMetadata());

        setTargetProduct(targetProduct);
    }

    private MetadataElement createPcaMetadata() {
        final MetadataElement meanVector = new MetadataElement("MEAN_VECTOR");
        final double[] meanVector1 = pca.getMeanVector();
        for (int i = 0; i < componentCount; i++) {
            meanVector.addAttribute(new MetadataAttribute(sourceBands[i].getName(), ProductData.createInstance(new double[]{meanVector1[i]}), true));
        }
        final MetadataElement basisVectors = new MetadataElement("BASIS_VECTORS");
        for (int i = 0; i < componentCount; i++) {
            final double[] basisVector = pca.getBasisVector(i);
            basisVectors.addAttribute(new MetadataAttribute("component_" + (i + 1), ProductData.createInstance(basisVector), true));
        }
        MetadataElement pcaAnalysisMD = new MetadataElement("PCA_RESULT");
        pcaAnalysisMD.addElement(meanVector);
        pcaAnalysisMD.addElement(basisVectors);
        return pcaAnalysisMD;
    }

    private Band[] collectSourceBands() {
        if (sourceBandNames != null && sourceBandNames.length > 0) {
            sourceBands = new Band[sourceBandNames.length];
            for (int i = 0; i < sourceBandNames.length; i++) {
                final Band sourceBand = sourceProduct.getBand(sourceBandNames[i]);
                if (sourceBand == null) {
                    throw new OperatorException("source band not found: " + sourceBandNames[i]);
                }
                sourceBands[i] = sourceBand;
            }
        } else {
            sourceBands = sourceProduct.getBands();
        }
        return sourceBands;
    }

    @Override
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {

        pm.beginTask("Computing component...", targetTile.getHeight());
        try {
            int componentIndex = getComponentIndex(targetBand);
            final Rectangle targetRectangle = targetTile.getRectangle();
            final Tile[] sourceTiles = new Tile[sourceBands.length];
            for (int i = 0; i < sourceTiles.length; i++) {
                sourceTiles[i] = getSourceTile(sourceBands[i], targetRectangle);
            }
            double[] point = new double[sourceTiles.length];
            for (int y = targetRectangle.y; y < targetRectangle.y + targetRectangle.height; y++) {
                this.checkForCancellation();
                for (int x = targetRectangle.x; x < targetRectangle.x + targetRectangle.width; x++) {
                    for (int i = 0; i < sourceTiles.length; i++) {
                        point[i] = sourceTiles[i].getSampleDouble(x, y);
                    }

                    if (componentIndex >= 0) {
                        final double[] eigenPoint = pca.sampleToEigenSpace(point);
                        targetTile.setSample(x, y, eigenPoint[componentIndex]);
                    } else if (targetBand == responseBand) {
                        final double response = pca.response(point);
                        targetTile.setSample(x, y, response);
                    } else if (targetBand == errorBand) {
                        final double error = pca.errorMembership(point);
                        targetTile.setSample(x, y, error);
                    } else if (targetBand == flagsBand) {
                        final boolean roiPixel = roi.contains(x, y);
                        targetTile.setSample(x, y, roiPixel ? 0x01 : 0x00);
                    }
                }
                pm.worked(1);
            }
        } finally {
            pm.done();
        }
    }

    @Override
    public void dispose() {
        pca = null;
        targetProduct = null;
        componentBands = null;
        responseBand = null;
        flagsBand = null;
    }

    private int getComponentIndex(Band targetBand) {
        for (int i = 0; i < componentBands.length; i++) {
            if (componentBands[i] == targetBand) {
                return i;
            }
        }
        return -1;
    }

    private synchronized void initPca(ProgressMonitor pm) {
        Rectangle[] tileRectangles = getAllTileRectangles();
        pm.beginTask("Extracting data points...", tileRectangles.length * 2);
        try {
            Mask roiMask = null;
            if (StringUtils.isNotNullAndNotEmpty(roiMaskName)) {
                roiMask = sourceProduct.getMaskGroup().get(roiMaskName);
            }  else if (StringUtils.isNotNullAndNotEmpty(roiMaskExpr)) {
                roiMask = Mask.BandMathsType.create("_pca_roi_expr", "", targetProduct.getSceneRasterWidth(), targetProduct.getSceneRasterHeight(),
                                                                    roiMaskExpr, Color.RED, 0.5);
                // todo roiMask.
            }

            roi = new Roi(sourceProduct, sourceBands, roiMaskName);

            final int pointSize = sourceBands.length;
            final double[] point = new double[pointSize];
            double[] pointData = new double[10000 * pointSize];
            int pointCount = 0;
            for (Rectangle rectangle : tileRectangles) {
                PixelIter iter = createPixelIter(rectangle, SubProgressMonitor.create(pm, 1));
                while (iter.next(point) != null) {
                    if (pointData.length < (pointCount + 1) * pointSize) {
                        final double[] tmp = pointData;
                        pointData = new double[2 * pointData.length];
                        System.arraycopy(tmp, 0, pointData, 0, tmp.length);
                    }
                    System.arraycopy(point, 0, pointData, pointCount * pointSize, pointSize);
                    pointCount++;
                }
                pm.worked(1);
            }

            if (pointData.length > pointCount * pointSize) {
                final double[] tmp = pointData;
                pointData = new double[pointCount * pointSize];
                System.arraycopy(tmp, 0, pointData, 0, pointData.length);
            }

            pca = new PrincipleComponentAnalysis(pointSize);
            pca.computeBasis(pointData, componentCount);
            pm.worked(tileRectangles.length);
        } finally {
            pm.done();
        }
    }

    private Rectangle[] getAllTileRectangles() {
        Dimension tileSize = ImageManager.getPreferredTileSize(sourceProduct);
        final int rasterHeight = sourceProduct.getSceneRasterHeight();
        final int rasterWidth = sourceProduct.getSceneRasterWidth();
        final Rectangle boundary = new Rectangle(rasterWidth, rasterHeight);
        final int tileCountX = MathUtils.ceilInt(boundary.width / (double) tileSize.width);
        final int tileCountY = MathUtils.ceilInt(boundary.height / (double) tileSize.height);

        Rectangle[] rectangles = new Rectangle[tileCountX * tileCountY];
        int index = 0;
        for (int tileY = 0; tileY < tileCountY; tileY++) {
            for (int tileX = 0; tileX < tileCountX; tileX++) {
                final Rectangle tileRectangle = new Rectangle(tileX * tileSize.width,
                                                              tileY * tileSize.height,
                                                              tileSize.width,
                                                              tileSize.height);
                final Rectangle intersection = boundary.intersection(tileRectangle);
                rectangles[index] = intersection;
                index++;
            }
        }
        return rectangles;
    }

    private PixelIter createPixelIter(Rectangle rectangle, ProgressMonitor pm) {
        final Tile[] sourceTiles = new Tile[sourceBands.length];
        try {
            pm.beginTask("Extracting data points...", sourceBands.length);
            for (int i = 0; i < sourceBands.length; i++) {
                sourceTiles[i] = getSourceTile(sourceBands[i], rectangle);
                pm.worked(1);
            }
        } finally {
            pm.done();
        }
        return new PixelIter(sourceTiles, roi);
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(PrincipleComponentAnalysisOp.class);
        }
    }

    public static class RoiMaskNameValidator implements Validator {
        @Override
        public void validateValue(Property roiMaskExprProp, Object value) throws ValidationException {
            if (value != null) {
                validateRoiMaskExpr(roiMaskExprProp);
            }
        }
    }

    public static class RoiMaskExprValidator implements Validator {
        @Override
        public void validateValue(Property roiMaskExprProp, Object value) throws ValidationException {
            if (value != null && !value.toString().trim().isEmpty()) {
                validateRoiMaskExpr(roiMaskExprProp);
            }
        }
    }

    private static void validateRoiMaskExpr(Property roiMaskExprProp) throws ValidationException {
        final Property roiMaskNameProp = roiMaskExprProp.getContainer().getProperty("roiMaskName");
        if (roiMaskNameProp != null && !roiMaskNameProp.getValueAsText().equals("")) {
            throw new ValidationException("You cannot specify a ROI-mask expression,\nbecause a ROI-mask is given by its name.\n" +
                                                  "Deselect the mask name, if you want to provide your own mask expression.");
        }
    }

}
