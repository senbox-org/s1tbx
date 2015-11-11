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

package org.esa.snap.core.image;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.util.ImageUtils;

import javax.media.jai.PlanarImage;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.io.IOException;


/**
 * A base class for {@code OpImage}s acting as source image for
 * a {@link RasterDataNode}.
 *
 * @author Norman Fomferra
 * @see RasterDataNode#getSourceImage()
 * @see RasterDataNode#getGeophysicalImage()
 * @see RasterDataNode#setSourceImage(com.bc.ceres.glevel.MultiLevelImage)
 */
public abstract class RasterDataNodeOpImage extends SingleBandedOpImage {
    private final RasterDataNode rasterDataNode;

    private static Dimension getPreferredTileSize(RasterDataNode rdn) {
        Product product = rdn.getProduct();
        if (product != null && product.getPreferredTileSize() != null) {
            return product.getPreferredTileSize();
        }
        return null;
    }

    /**
     * Constructor.
     *
     * @param rasterDataNode The target raster data node.
     * @param level The resolution level.
     * @see ResolutionLevel#create(com.bc.ceres.glevel.MultiLevelModel, int)
     */
    protected RasterDataNodeOpImage(RasterDataNode rasterDataNode, ResolutionLevel level) {
        super(ImageManager.getDataBufferType(rasterDataNode.getDataType()),
              rasterDataNode.getRasterWidth(),
              rasterDataNode.getRasterHeight(),
              getPreferredTileSize(rasterDataNode),
              null, level);
        this.rasterDataNode = rasterDataNode;
    }

    /**
     * @return The target raster data node.
     */
    public RasterDataNode getRasterDataNode() {
        return rasterDataNode;
    }

    /**
     * Utility method that allows to retrieve data from a raster data node whose geophysical image shares the same
     * multi-level model (aka image pyramid model).
     *
     * @param band   A raster data node whose geophysical image shares the same multi-level model.
     * @param region The region in pixel coordinates of the given resolution level (see {@link #getLevel()}).
     * @return The retrieved pixel data in geophysical units.
     * @see #getRawProductData(RasterDataNode, java.awt.Rectangle)
     */
    protected ProductData getGeophysicalProductData(RasterDataNode band, Rectangle region) {
        return getProductData(band.getGeophysicalImage().getImage(getLevel()), band.getGeophysicalDataType(), region);
    }

    /**
     * Utility method that allows to retrieve data from a raster data node whose source image shares the same
     * multi-level model (aka image pyramid model).
     *
     * @param band   A raster data node whose source image shares the same multi-level model.
     * @param region The region in pixel coordinates of the given resolution level (see {@link #getLevel()}).
     * @return The retrieved, raw and unscaled source pixel data.
     * @see #getGeophysicalProductData(RasterDataNode, java.awt.Rectangle)
     */
    protected ProductData getRawProductData(RasterDataNode band, Rectangle region) {
        return getProductData(band.getSourceImage().getImage(getLevel()), band.getDataType(), region);
    }

    private ProductData getProductData(RenderedImage image, int productDataType, Rectangle region) {
        Raster raster = image.getData(region);
        boolean directMode = raster.getDataBuffer().getSize() == region.width * region.height;
        if (directMode) {
            return ProductData.createInstance(productDataType, ImageUtils.getPrimitiveArray(raster.getDataBuffer()));
        } else {
            final ProductData instance = ProductData.createInstance(productDataType, region.width * region.height);
            raster.getDataElements(region.x, region.y, region.width, region.height, instance.getElems());
            return instance;
        }
    }

    @Override
    protected void computeRect(PlanarImage[] sourceImages, WritableRaster tile, Rectangle destRect) {
        ProductData productData;
        boolean directMode = tile.getDataBuffer().getSize() == destRect.width * destRect.height;
        if (directMode) {
            productData = ProductData.createInstance(rasterDataNode.getDataType(),
                                                     ImageUtils.getPrimitiveArray(tile.getDataBuffer()));
        } else {
            productData = ProductData.createInstance(rasterDataNode.getDataType(),
                                                     destRect.width * destRect.height);
        }

        try {
            computeProductData(productData, destRect);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (!directMode) {
            tile.setDataElements(destRect.x, destRect.y, destRect.width, destRect.height, productData.getElems());
        }
    }

    /**
     * Computes the target pixel data for this level image.
     *
     * @param productData The target pixel buffer to write to. The number of elements in this buffer will always be
     *                    {@code region.width * region.height}.
     * @param region      The target region in pixel coordinates valid for this image level.
     * @throws IOException May be thrown if an I/O error occurs during the computation.
     */
    protected abstract void computeProductData(ProductData productData, Rectangle region) throws IOException;

    @Override
    public String toString() {
        String className = getClass().getSimpleName();
        String productName = "";
        if (rasterDataNode.getProduct() != null) {
            productName = ":" + rasterDataNode.getProduct().getName();
        }
        String bandName = "." + rasterDataNode.getName();
        return className + productName + bandName;
    }

    /**
     * Utility method which computes source (offset) coordinates for a given source and target lengths.
     *
     * @param sourceLength The source length in source pixel units.
     * @param targetLength The target length in target pixel units.
     * @return An array of size {@code targetLength} containing source (offset) coordinates.
     */
    protected int[] getSourceCoords(final int sourceLength, final int targetLength) {
        final int[] sourceCoords = new int[targetLength];
        for (int i = 0; i < targetLength; i++) {
            sourceCoords[i] = getSourceCoord(i, 0, sourceLength - 1);
        }
        return sourceCoords;
    }

    protected static void copyLine(final int y, final int destWidth, ProductData src, ProductData dest, int[] sourceCoords) {
        int destOffset = y * destWidth;
        switch (src.getType()) {
            case ProductData.TYPE_INT8:
            case ProductData.TYPE_UINT8:
                byte[] srcArrayB = (byte[]) src.getElems();
                byte[] destArrayB = (byte[]) dest.getElems();
                for (int coord : sourceCoords) {
                    destArrayB[destOffset++] = srcArrayB[coord];
                }
                return;
            case ProductData.TYPE_INT16:
            case ProductData.TYPE_UINT16:
                short[] srcArrayS = (short[]) src.getElems();
                short[] destArrayS = (short[]) dest.getElems();
                for (int coord : sourceCoords) {
                    destArrayS[destOffset++] = srcArrayS[coord];
                }
                return;
            case ProductData.TYPE_INT32:
            case ProductData.TYPE_UINT32:
                int[] srcArrayI = (int[]) src.getElems();
                int[] destArrayI = (int[]) dest.getElems();
                for (int coord : sourceCoords) {
                    destArrayI[destOffset++] = srcArrayI[coord];
                }
                return;
            case ProductData.TYPE_FLOAT32:
                float[] srcArrayF = (float[]) src.getElems();
                float[] destArrayF = (float[]) dest.getElems();
                for (int coord : sourceCoords) {
                    destArrayF[destOffset++] = srcArrayF[coord];
                }
                return;
            case ProductData.TYPE_FLOAT64:
                double[] srcArrayD = (double[]) src.getElems();
                double[] destArrayD = (double[]) dest.getElems();
                for (int coord : sourceCoords) {
                    destArrayD[destOffset++] = srcArrayD[coord];
                }
                return;
            case ProductData.TYPE_ASCII:
            case ProductData.TYPE_UTC:
            default:
                throw new IllegalArgumentException("wrong product data type: " + src.getType());
        }
    }

}
