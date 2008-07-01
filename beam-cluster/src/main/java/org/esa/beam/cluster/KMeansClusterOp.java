/*
 * Copyright (C) 2002-2008 by Brockmann Consult
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.cluster;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.media.jai.PlanarImage;
import javax.media.jai.ROI;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.IndexCoding;
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
import org.esa.beam.util.StringUtils;
import org.esa.beam.util.jai.ValidMaskOpImage;
import org.esa.beam.util.math.MathUtils;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;

/**
 * Operator for k-means cluster analysis.
 *
 * @author Ralf Quast
 * @version $Revision: 2223 $ $Date: 2008-06-16 11:59:40 +0200 (Mo, 16 Jun 2008) $
 */
@OperatorMetadata(alias = "KMeansClusterAnalysis",
                  version = "1.0",
                  authors = "Ralf Quast, Marco Zuehlke",
                  copyright = "(c) 2008 by Brockmann Consult",
                  description = "Performs a K-Means cluster analysis.")
public class KMeansClusterOp extends Operator {

    private static final int NO_DATA_VALUE = -1;
    
    @SourceProduct(alias = "source")
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct;

    @Parameter(label = "Number of clusters", defaultValue = "14")
    private int clusterCount;
    @Parameter(label = "Number of iterations", defaultValue = "30")
    private int iterationCount;
    @Parameter(label = "Source band names",
               description = "The names of the bands being used for the cluster analysis.",
               sourceProductId = "source")
    private String[] sourceBandNames;
    @Parameter(label = "Region of interest",
               description = "The name of the band which contains a ROI that should be used.",
               sourceProductId = "source")
    private String roiBandName;
    
    private transient ROI roi;
    private transient Band[] sourceBands;
    private transient Band clusterMapBand;
    private transient KMeansClusterSet clusterSet;

    public KMeansClusterOp() {
    }

    @Override
    public void initialize() throws OperatorException {
        collectSourceBands();
        
        int width = sourceProduct.getSceneRasterWidth();
        int height = sourceProduct.getSceneRasterHeight();
        final String name = sourceProduct.getName() + "_CLUSTERS";
        final String type = sourceProduct.getProductType() + "_CLUSTERS";

        targetProduct = new Product(name, type, width, height);
        ProductUtils.copyTiePointGrids(sourceProduct, targetProduct);
        ProductUtils.copyGeoCoding(sourceProduct, targetProduct);
        targetProduct.setStartTime(sourceProduct.getStartTime());
        targetProduct.setEndTime(sourceProduct.getEndTime());
        
        clusterMapBand = new Band("cluster_map", ProductData.TYPE_INT16, width, height);
        clusterMapBand.setDescription("Cluster map");
        clusterMapBand.setNoDataValue(NO_DATA_VALUE);
        clusterMapBand.setNoDataValueUsed(true);
        targetProduct.addBand(clusterMapBand);

        final IndexCoding indexCoding = new IndexCoding("clusters");
        for (int i = 0; i < clusterCount+1; i++) {
            indexCoding.addIndex("cluster_" + (i + 1), i, "Cluster label");
        }
        targetProduct.getIndexCodingGroup().add(indexCoding);
        clusterMapBand.setSampleCoding(indexCoding);
        
        setTargetProduct(targetProduct);
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
        if (targetBand != clusterMapBand) {
            writeRoi(targetTile);
            return;
        }
        pm.beginTask("Computing clusters...", 2);
        try {
            final KMeansClusterSet theClusterSet = getClusterSet(SubProgressMonitor.create(pm, 1));
            final Rectangle targetRectangle = targetTile.getRectangle();
            final Tile[] sourceTiles = new Tile[sourceBands.length];
            for (int i = 0; i < sourceTiles.length; i++) {
                sourceTiles[i] = getSourceTile(sourceBands[i], targetRectangle, ProgressMonitor.NULL);
            }

            double[] point = new double[sourceTiles.length];
            for (int y = targetRectangle.y; y < targetRectangle.y + targetRectangle.height; y++) {
                for (int x = targetRectangle.x; x < targetRectangle.x + targetRectangle.width; x++) {
                    if (roi.contains(x, y)) {
                        for (int i = 0; i < sourceTiles.length; i++) {
                            point[i] = sourceTiles[i].getSampleDouble(x, y);
                        }
                        targetTile.setSample(x, y, theClusterSet.getMembership(point));
                    } else {
                        targetTile.setSample(x, y, NO_DATA_VALUE);
                    }
                }
            }
            pm.worked(1);
        } finally {
            pm.done();
        }
    }

    private void writeRoi(Tile targetTile) {
        final Rectangle targetRectangle = targetTile.getRectangle();
        for (int y = targetRectangle.y; y < targetRectangle.y + targetRectangle.height; y++) {
            for (int x = targetRectangle.x; x < targetRectangle.x + targetRectangle.width; x++) {
                if (roi.contains(x, y)) {
                    targetTile.setSample(x, y, 1);
                } else {
                    targetTile.setSample(x, y, 0);
                }
            }
        }
    }


    private synchronized KMeansClusterSet getClusterSet(ProgressMonitor pm) {
        if (clusterSet == null) {
            Rectangle[] tileRectangles = getAllTileRactangles();
            pm.beginTask("Extracting data points...", tileRectangles.length * iterationCount * 2 + 2);
            try {
                joinRoiAndNoDataMask(SubProgressMonitor.create(pm, 1));
                final KMeansClusterer clusterer = new KMeansClusterer(clusterCount, sourceBands.length);
                clusterer.initialize(new RandomSceneIter(this, sourceBands, roi, 42));
                pm.worked(1);
                for (int i = 0; i < iterationCount; ++i) {
                    for (Rectangle rectangle : tileRectangles) {
                        checkForCancelation(pm);
                        PixelIter pixelIterr = createPixelIterr(rectangle, SubProgressMonitor.create(pm, 1));
                        clusterer.iterate(pixelIterr);
                        pm.worked(1);
                    }
                }
                clusterSet = clusterer.getClusters();
            } finally {
                pm.done();
            }
        }
        return clusterSet;
    }
    
    private Rectangle[] getAllTileRactangles() {
        Dimension tileSize = targetProduct.getPreferredTileSize();
        final int rasterHeight = targetProduct.getSceneRasterHeight();
        final int rasterWidth = targetProduct.getSceneRasterWidth();
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

    private PixelIter createPixelIterr(Rectangle rectangle, ProgressMonitor pm) {
        final Tile[] sourceTiles = new Tile[sourceBands.length];
        try {
            pm.beginTask("Extracting data points...", sourceBands.length);
            for (int i = 0; i < sourceBands.length; i++) {
                sourceTiles[i] = getSourceTile(sourceBands[i], rectangle, SubProgressMonitor.create(pm, 1));
                pm.worked(1);
            }
        } finally {
            pm.done();
        }
        return new PixelIter(sourceTiles, roi);
    }
    
    private void joinRoiAndNoDataMask(ProgressMonitor pm) {
        if (StringUtils.isNotNullAndNotEmpty(roiBandName)) {
            Band roiBand = sourceProduct.getBand(roiBandName);
            try {
                roi = roiBand.createROI(pm);
            } catch (IOException e) {
                throw new OperatorException(e);
            }
        }
        Set<String> validMaskSet = new HashSet<String>(sourceBands.length);
        for (int i = 0; i < sourceBands.length; i++) {
            Band band = sourceBands[i];
            String validExpression = band.getValidMaskExpression();
            if (!validMaskSet.contains(validExpression)) {
                validMaskSet.add(validExpression);
                PlanarImage noDataImage = new ValidMaskOpImage(band);
                ROI noDataROI = new ROI(noDataImage, 1);
                if(roi == null) {
                    roi = noDataROI;
                } else {
                    roi = roi.intersect(noDataROI);
                }
            }
        }
    }
    
    
    public static class Spi extends OperatorSpi {

        public Spi() {
            super(KMeansClusterOp.class);
        }
    }    
}
