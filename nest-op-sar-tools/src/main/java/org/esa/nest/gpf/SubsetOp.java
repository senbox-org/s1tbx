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
package org.esa.nest.gpf;

import com.bc.ceres.core.ProgressMonitor;
import com.vividsolutions.jts.geom.Geometry;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.dataio.ProductSubsetBuilder;
import org.esa.beam.framework.dataio.ProductSubsetDef;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.VirtualBand;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.util.converters.JtsGeometryConverter;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

@OperatorMetadata(alias = "Subset",
        category = "Utilities",
        description = "Create a spatial subset of the source product.")
public class SubsetOp extends Operator {

    @SourceProduct
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct;

    @Parameter(description = "The list of source bands.", alias = "sourceBands", itemAlias = "band",
            rasterDataNodeType = Band.class, label="Source Bands")
    private
    String[] sourceBandNames;

    @Parameter(label = "X", defaultValue="0")
    private int regionX = 0;
    @Parameter(label = "Y", defaultValue="0")
    private int regionY = 0;
    @Parameter(label = "Width")
    private int width = 1000;
    @Parameter(label = "Height")
    private int height = 1000;
    @Parameter(defaultValue = "1")
    private int subSamplingX = 1;
    @Parameter(defaultValue = "1")
    private int subSamplingY = 1;

    @Parameter(converter = JtsGeometryConverter.class,
               description = "WKT-format, " +
                       "e.g. POLYGON((<lon1> <lat1>, <lon2> <lat2>, ..., <lon1> <lat1>))\n" +
                       "(make sure to quote the option due to spaces in <geometry>)")
    private Geometry geoRegion;

    private ProductReader subsetReader = null;
    private final Map<Band, Band> bandMap = new HashMap<Band, Band>();

    @Override
    public void initialize() throws OperatorException {
        if(width == 0) {
            width = sourceProduct.getSceneRasterWidth() - regionX;
        }
        if(height == 0) {
            height = sourceProduct.getSceneRasterHeight() - regionY;
        }
        if(regionX+width > sourceProduct.getSceneRasterWidth()) {
            throw new OperatorException("Selected region must be within the source product dimensions of "+
                                        sourceProduct.getSceneRasterWidth()+" x "+ sourceProduct.getSceneRasterHeight());
        }
        if(regionY+height > sourceProduct.getSceneRasterHeight()) {
            throw new OperatorException("Selected region must be within the source product dimensions of "+
                                        sourceProduct.getSceneRasterWidth()+" x "+ sourceProduct.getSceneRasterHeight());
        }

        subsetReader = new ProductSubsetBuilder();
        final ProductSubsetDef subsetDef = new ProductSubsetDef();
        subsetDef.addNodeNames(sourceProduct.getTiePointGridNames());

        if (sourceBandNames != null && sourceBandNames.length > 0) {
            subsetDef.addNodeNames(sourceBandNames);
        } else {
            subsetDef.addNodeNames(sourceProduct.getBandNames());
        }
        subsetDef.setRegion(regionX, regionY, width, height);

        if (geoRegion != null) {
            final Rectangle region = computePixelRegion(sourceProduct, geoRegion, 0);
            if (region != null) {
                if (region.isEmpty()) {
                    throw new OperatorException("Subset: No intersection with source product boundary "+sourceProduct.getName());
                    //region.setSize(1,1);
                }
                subsetDef.setRegion(region);
            }
        }

        subsetDef.setSubSampling(subSamplingX, subSamplingY);
        subsetDef.setIgnoreMetadata(false);

        try {
            targetProduct = subsetReader.readProductNodes(sourceProduct, subsetDef);

            // replace virtual bands with real bands
            for(Band b : targetProduct.getBands()) {
                if(b instanceof VirtualBand) {
                    targetProduct.removeBand(b);
                    final Band newBand = targetProduct.addBand(b.getName(), b.getDataType());
                    newBand.setNoDataValue(b.getNoDataValue());
                    newBand.setNoDataValueUsed(b.isNoDataValueUsed());
                    newBand.setDescription(b.getDescription());
                    newBand.setUnit(b.getUnit());
                    bandMap.put(newBand, b);
                }
            }
        } catch (Throwable t) {
            throw new OperatorException(t);
        }
    }

    public static Rectangle computePixelRegion(Product product, Geometry geoRegion, int numBorderPixels) {
        final Geometry productGeometry = org.esa.beam.gpf.operators.standard.SubsetOp.computeProductGeometry(product);
        final Geometry regionIntersection = geoRegion.intersection(productGeometry);
        if (regionIntersection.isEmpty()) {
            return new Rectangle();
        }
        final org.esa.beam.gpf.operators.standard.SubsetOp.PixelRegionFinder pixelRegionFinder =
                new org.esa.beam.gpf.operators.standard.SubsetOp.PixelRegionFinder(product.getGeoCoding());
        regionIntersection.apply(pixelRegionFinder);
        final Rectangle pixelRegion = pixelRegionFinder.getPixelRegion();
        pixelRegion.grow(numBorderPixels, numBorderPixels);
        return pixelRegion.intersection(new Rectangle(product.getSceneRasterWidth(),
                product.getSceneRasterHeight()));
    }

    @Override
    public void computeTile(Band band, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        final ProductData destBuffer = targetTile.getRawSamples();
        final Rectangle rectangle = targetTile.getRectangle();
        try {
            // for virtual bands
            Band tgtBand = bandMap.get(band);
            if(tgtBand == null)
                tgtBand = band;
            subsetReader.readBandRasterData(tgtBand,
                                            rectangle.x,
                                            rectangle.y,
                                            rectangle.width,
                                            rectangle.height,
                                            destBuffer, pm);
            targetTile.setRawSamples(destBuffer);
        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        } finally {
            pm.done();
        }
    }

    public static class Spi extends OperatorSpi {
        public Spi() {
            super(SubsetOp.class);
            setOperatorUI(SubsetUI.class);
        }
    }
}