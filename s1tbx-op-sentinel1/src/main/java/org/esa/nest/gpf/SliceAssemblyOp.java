/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
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
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.dataop.maptransf.Datum;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.SourceProducts;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.util.ProductUtils;
import org.esa.snap.datamodel.AbstractMetadata;
import org.esa.snap.gpf.OperatorUtils;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Merges Sentinel-1 slice products
 */
@OperatorMetadata(alias = "SliceAssembly",
        category = "SAR Processing/SENTINEL-1",
        authors = "Jun Lu, Luis Veci",
        copyright = "Copyright (C) 2014 by Array Systems Computing Inc.",
        description = "Merges Sentinel-1 slice products")
public final class SliceAssemblyOp extends Operator {

    @SourceProducts
    private Product[] sourceProducts;
    @TargetProduct
    private Product targetProduct;

    private MetadataElement absRoot = null;

    private Product[] sliceProducts;
    private int targetWidth = 0, targetHeight = 0;

    /**
     * Default constructor. The graph processing framework
     * requires that an operator has a default constructor.
     */
    public SliceAssemblyOp() {
    }

    /**
     * Initializes this operator and sets the one and only target product.
     * <p>The target product can be either defined by a field of type {@link org.esa.beam.framework.datamodel.Product} annotated with the
     * {@link org.esa.beam.framework.gpf.annotations.TargetProduct TargetProduct} annotation or
     * by calling {@link #setTargetProduct} method.</p>
     * <p>The framework calls this method after it has created this operator.
     * Any client code that must be performed before computation of tile data
     * should be placed here.</p>
     *
     * @throws org.esa.beam.framework.gpf.OperatorException If an error occurs during operator initialisation.
     * @see #getTargetProduct()
     */
    @Override
    public void initialize() throws OperatorException {

        try {
            sliceProducts = determineSliceProducts();

            absRoot = AbstractMetadata.getAbstractedMetadata(sliceProducts[0]);

            createTargetProduct();

            updateTargetProductMetadata();

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    private Product[] determineSliceProducts() throws Exception {
        if(sourceProducts.length < 2) {
            throw new Exception("Slice assembly requires at least two consecutive slice products");
        }

        final TreeMap<Integer, Product> productSet = new TreeMap<>();
        for(Product srcProduct : sourceProducts) {
            final MetadataElement origMetaRoot = AbstractMetadata.getOriginalProductMetadata(srcProduct);
            final MetadataElement generalProductInformation = getGeneralProductInformation(origMetaRoot);
            if(!isSliceProduct(generalProductInformation)) {
                throw new Exception(srcProduct.getName() +" is not a slice product");
            }

            final int totalSlices = generalProductInformation.getAttributeInt("totalSlices");
            final int sliceNumber = generalProductInformation.getAttributeInt("sliceNumber");

            productSet.put(sliceNumber, srcProduct);
        }

        //check if consecutive
        Integer prev = productSet.firstKey();
        for(Integer i : productSet.keySet()) {
            if(!i.equals(prev)) {
                if(!prev.equals(i-1)) {
                    throw new Exception("Products are not consecutive slices");
                }
                prev = i;
            }
        }

        return productSet.values().toArray(new Product[productSet.size()]);
    }

    private static MetadataElement getGeneralProductInformation(final MetadataElement origMetaRoot) {
        final MetadataElement XFDU = origMetaRoot.getElement("XFDU");
        final MetadataElement metadataSection = XFDU.getElement("metadataSection");

        final MetadataElement metadataObject = findElementByID(metadataSection, "ID", "generalProductInformation");
        final MetadataElement metadataWrap = metadataObject.getElement("metadataWrap");
        final MetadataElement xmlData = metadataWrap.getElement("xmlData");
        MetadataElement generalProductInformation = xmlData.getElement("generalProductInformation");
        if (generalProductInformation == null)
            generalProductInformation = xmlData.getElement("standAloneProductInformation");
        return generalProductInformation;
    }

    private static boolean isSliceProduct(final MetadataElement generalProductInformation) {
        final String sliceProductFlag = generalProductInformation.getAttributeString("sliceProductFlag");
        return sliceProductFlag.equals("true");
    }

    private static MetadataElement findElementByID(final MetadataElement metadataSection, final String tag, final String id) {
        final MetadataElement[] metadataObjectList = metadataSection.getElements();

        for (MetadataElement metadataObject : metadataObjectList) {
            final String attrib = metadataObject.getAttributeString(tag, null);
            if (attrib.equals(id)) {
                return metadataObject;
            }
        }
        return null;
    }

    private void computeTargetWidthAndHeight() {
        for (Product srcProduct : sliceProducts) {
            if (targetWidth < srcProduct.getSceneRasterWidth())
                targetWidth = srcProduct.getSceneRasterWidth();
            targetHeight += srcProduct.getSceneRasterHeight();
        }
    }

    private void computeTargetBandWidthAndHeight(final String bandName, final Dimension dim) throws OperatorException {
        for (Product srcProduct : sliceProducts) {
            final Band srcBand = srcProduct.getBand(bandName);
            if(srcBand == null) {
                throw new OperatorException(bandName +" not found in product "+srcProduct.getName());
            }

            dim.setSize(Math.max(dim.width, srcBand.getRasterWidth()), dim.height + srcBand.getRasterHeight());
        }
    }

    private void createTargetProduct() {
        computeTargetWidthAndHeight();

        final Product firstSliceProduct = sliceProducts[0];
        final Product lastSliceProduct = sliceProducts[sliceProducts.length-1];
        targetProduct = new Product(firstSliceProduct.getName(), firstSliceProduct.getProductType(), targetWidth, targetHeight);

        final Band[] sourceBands = firstSliceProduct.getBands();
        for (Band srcBand : sourceBands) {
            final Dimension dim = new Dimension(0,0);
            computeTargetBandWidthAndHeight(srcBand.getName(), dim);
            final Band newBand = new Band(srcBand.getName(), srcBand.getDataType(), dim.width, dim.height);
            ProductUtils.copyRasterDataNodeProperties(srcBand, newBand);

            targetProduct.addBand(newBand);
        }

        ProductUtils.copyMetadata(firstSliceProduct, targetProduct);
        ProductUtils.copyFlagCodings(firstSliceProduct, targetProduct);
        ProductUtils.copyMasks(firstSliceProduct, targetProduct);
        ProductUtils.copyVectorData(firstSliceProduct, targetProduct);
        ProductUtils.copyIndexCodings(firstSliceProduct, targetProduct);
        targetProduct.setStartTime(firstSliceProduct.getStartTime());
        targetProduct.setEndTime(lastSliceProduct.getEndTime());
        targetProduct.setDescription(firstSliceProduct.getDescription());

        createTiePointGrids();
        addGeocoding();
    }

    private void createTiePointGrids() {
        final Product firstSliceProduct = sliceProducts[0];

        final TiePointGrid[] tpgList = firstSliceProduct.getTiePointGrids();
        for(TiePointGrid tpg : tpgList) {
            final List<Float> newPoints = new ArrayList<>();

            final int gridWidth = tpg.getRasterWidth();
            final int gridHeight = tpg.getRasterHeight();
            final float[] points = tpg.getTiePoints();

            for(float f : points) {
                newPoints.add(f);
            }
            int newGridHeight = gridHeight;

            for(Product srcProduct : sliceProducts) {
                if(srcProduct == firstSliceProduct)
                    continue;

                final TiePointGrid tpg2 = srcProduct.getTiePointGrid(tpg.getName());
                final float[] points2 = tpg2.getTiePoints();

                for(int i=gridWidth; i < points2.length; ++i) {
                    newPoints.add(points2[i]);
                }
                newGridHeight += tpg2.getRasterHeight()-1;
            }

            final int subSamplingX = targetWidth / gridWidth;
            final int subSamplingY = targetHeight / newGridHeight;

            final float[] pointArray = new float[newPoints.size()];
            int i=0;
            for(Float f : newPoints) {
                pointArray[i++] = f;
            }

            final TiePointGrid newGrid = new TiePointGrid(
                    tpg.getName(), gridWidth, newGridHeight, 0, 0, subSamplingX, subSamplingY, pointArray);
            targetProduct.addTiePointGrid(newGrid);
        }
    }

    private void addGeocoding() {
        final TiePointGrid latGrid = targetProduct.getTiePointGrid(OperatorUtils.TPG_LATITUDE);
        final TiePointGrid lonGrid = targetProduct.getTiePointGrid(OperatorUtils.TPG_LONGITUDE);

        final TiePointGeoCoding tpGeoCoding = new TiePointGeoCoding(latGrid, lonGrid, Datum.WGS_84);
        targetProduct.setGeoCoding(tpGeoCoding);
    }

    private void updateTargetProductMetadata() {

        final MetadataElement absTgt = AbstractMetadata.getAbstractedMetadata(targetProduct);

    }

    /**
     * Called by the framework in order to compute a tile for the given target band.
     * <p>The default implementation throws a runtime exception with the message "not implemented".</p>
     *
     * @param targetBand The target band.
     * @param targetTile The current tile associated with the target band to be computed.
     * @param pm         A progress monitor which should be used to determine computation cancelation requests.
     * @throws org.esa.beam.framework.gpf.OperatorException If an error occurs during computation of the target raster.
     */
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        try {
            final Rectangle targetTileRectangle = targetTile.getRectangle();
            final int tx0 = targetTileRectangle.x;
            final int ty0 = targetTileRectangle.y;
            final int tw = targetTileRectangle.width;
            final int th = targetTileRectangle.height;
            final int maxY = ty0 + th;
            final int maxX = tx0 + tw;

            final Map<BandLines, Band> bandLineMap = new HashMap<>(sliceProducts.length);
            int height = 0;
            for(Product srcProduct : sliceProducts) {
                final Band srcBand = srcProduct.getBand(targetBand.getName());
                int start = height;
                height += srcBand.getRasterHeight();
                int end = height;
                bandLineMap.put(new BandLines(start, end), srcBand);
            }

            final Set<BandLines> lines = bandLineMap.keySet();
            for(int y=ty0; y < maxY; ++y) {
                for(BandLines line : lines) {
                    if(y >= line.start && y < line.end) {
                        final Band srcBand = bandLineMap.get(line);
                        final Tile sourceRaster = getSourceTile(srcBand, new Rectangle(0, y-line.start, targetBand.getRasterWidth(), 1));

                        final ProductData trgData = targetTile.getDataBuffer();
                        final ProductData srcData = sourceRaster.getDataBuffer();

                        for (int x = tx0; x < maxX; ++x) {
                            final double val = srcData.getElemDoubleAt(sourceRaster.getDataBufferIndex(x, y-line.start));
                            trgData.setElemDoubleAt(targetTile.getDataBufferIndex(x, y), val);
                        }
                    }
                }
            }

        } catch (Throwable e) {
            throw new OperatorException(e.getMessage());
        }
    }

    private static class BandLines {
        final int start;
        final int end;
        BandLines(final int s, final int e) {
            this.start = s;
            this.end = e;
        }
    }

    /**
     * The SPI is used to register this operator in the graph processing framework
     * via the SPI configuration file
     * {@code META-INF/services/org.esa.beam.framework.gpf.OperatorSpi}.
     * This class may also serve as a factory for new operator instances.
     *
     * @see org.esa.beam.framework.gpf.OperatorSpi#createOperator()
     * @see org.esa.beam.framework.gpf.OperatorSpi#createOperator(java.util.Map, java.util.Map)
     */
    public static class Spi extends OperatorSpi {
        public Spi() {
            super(SliceAssemblyOp.class);
        }
    }
}