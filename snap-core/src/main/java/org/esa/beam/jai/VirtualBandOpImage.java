/*
 * Copyright (C) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.jai;

import com.bc.ceres.core.Assert;
import com.bc.ceres.jai.NoDataRaster;
import com.bc.jexp.ParseException;
import com.bc.jexp.Term;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.dataop.barithm.BandArithmetic;
import org.esa.beam.framework.dataop.barithm.RasterDataEvalEnv;
import org.esa.beam.framework.dataop.barithm.RasterDataSymbol;
import org.esa.beam.util.ImageUtils;

import javax.media.jai.PlanarImage;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.text.MessageFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * An {@code OpImage} which retrieves its data from the product reader associated with the
 * given {@code RasterDataNode} at a given pyramid level.
 */
public class VirtualBandOpImage extends SingleBandedOpImage {

    private static final int TRUE = 255;
    private static final int FALSE = 0;

    private final String expression;
    private final int dataType;
    private final Number fillValue;
    private final boolean mask;
    private final Product[] products;
    private final int defaultProductIndex;
    private final Map<Point, Term> termMap = new ConcurrentHashMap<Point, Term>();

    private volatile NoDataRaster noDataRaster;

    public static VirtualBandOpImage createMask(String expression,
                                                Product product,
                                                ResolutionLevel level) {
        return createMask(expression,
                          product,
                          product.getSceneRasterWidth(),
                          product.getSceneRasterHeight(),
                          level);
    }

    public static VirtualBandOpImage createMask(String expression,
                                                Product product,
                                                int width, int height,
                                                ResolutionLevel level) {
        return create(expression,
                      ProductData.TYPE_UINT8,
                      null,
                      true,
                      product,
                      width, height,
                      level);
    }

    public static VirtualBandOpImage createMask(String expression,
                                                Product[] products,
                                                int defaultProductIndex,
                                                int width, int height,
                                                ResolutionLevel level) {
        return create(expression,
                      ProductData.TYPE_UINT8,
                      null,
                      true,
                      products,
                      defaultProductIndex,
                      width, height,
                      level);
    }


    public static VirtualBandOpImage create(String expression,
                                            int dataType,
                                            Number fillValue,
                                            Product product,
                                            ResolutionLevel level) {
        return create(expression,
                      dataType,
                      fillValue,
                      false,
                      product,
                      product.getSceneRasterWidth(),
                      product.getSceneRasterHeight(),
                      level);
    }

    public static VirtualBandOpImage create(String expression,
                                            int dataType,
                                            Number fillValue,
                                            Product product,
                                            int width, int height,
                                            ResolutionLevel level) {
        return create(expression,
                      dataType,
                      fillValue,
                      false,
                      product,
                      width, height,
                      level);
    }

    public static VirtualBandOpImage create(String expression,
                                            int dataType,
                                            Number fillValue,
                                            Product[] products,
                                            int defaultProductIndex,
                                            ResolutionLevel level) {
        return create(expression,
                      dataType,
                      fillValue,
                      false,
                      products,
                      defaultProductIndex,
                      products[defaultProductIndex].getSceneRasterWidth(),
                      products[defaultProductIndex].getSceneRasterHeight(),
                      level);
    }

    public static VirtualBandOpImage create(String expression,
                                            int dataType,
                                            Number fillValue,
                                            Product[] products,
                                            int defaultProductIndex,
                                            int width, int height,
                                            ResolutionLevel level) {
        return create(expression,
                      dataType,
                      fillValue,
                      false,
                      products,
                      defaultProductIndex, width, height,
                      level);
    }

    private static VirtualBandOpImage create(String expression,
                                             int dataType,
                                             Number fillValue,
                                             boolean mask,
                                             Product product,
                                             int width, int height,
                                             ResolutionLevel level) {
        Assert.notNull(product, "product");
        Assert.notNull(level, "level");

        final Product[] products;
        final int defaultProductIndex;
        if (product.getProductManager() != null) {
            products = product.getProductManager().getProducts();
            defaultProductIndex = product.getProductManager().getProductIndex(product);
        } else {
            products = new Product[]{product};
            defaultProductIndex = 0;
        }
        Assert.state(defaultProductIndex >= 0 && defaultProductIndex < products.length);
        Assert.state(products[defaultProductIndex] == product);

        return create(expression,
                      dataType,
                      fillValue,
                      mask,
                      products,
                      defaultProductIndex,
                      width, height,
                      level);
    }

    private static VirtualBandOpImage create(String expression,
                                             int dataType,
                                             Number fillValue,
                                             boolean mask,
                                             Product[] products,
                                             int defaultProductIndex,
                                             int width, int height,
                                             ResolutionLevel level) {
        Assert.notNull(expression, "expression");
        Assert.notNull(products, "products");
        Assert.argument(products.length > 0, "products");
        Assert.argument(defaultProductIndex >= 0, "defaultProductIndex");
        Assert.argument(defaultProductIndex < products.length, "defaultProductIndex");
        Assert.notNull(level, "level");

        return new VirtualBandOpImage(expression,
                                      dataType,
                                      fillValue,
                                      mask,
                                      products,
                                      width, height,
                                      defaultProductIndex,
                                      level);
    }

    private VirtualBandOpImage(String expression,
                               int dataType,
                               Number fillValue,
                               boolean mask,
                               Product[] products,
                               int width, int height,
                               int defaultProductIndex,
                               ResolutionLevel level) {
        super(ImageManager.getDataBufferType(dataType),
              width,
              height,
              // todo - [multisize_products] tile size shall fit to width / height
              products[defaultProductIndex].getPreferredTileSize(),
              null,
              level);

        // todo - check products for compatibility
        this.expression = expression;
        this.dataType = dataType;
        this.mask = mask;
        this.products = products;
        this.defaultProductIndex = defaultProductIndex;
        this.fillValue = fillValue;
    }

    public String getExpression() {
        return expression;
    }

    public int getDataType() {
        return dataType;
    }

    public boolean isMask() {
        return mask;
    }

    public Product[] getProducts() {
        return products.clone();
    }

    public int getDefaultProductIndex() {
        return defaultProductIndex;
    }

    @Override
    public synchronized void dispose() {
        termMap.clear();
    }

    @Override
    public Raster computeTile(int tileX, int tileY) {
        final Term term = parseExpression();
        if (addDataToReferredRasterDataSymbols(getTileRect(tileX, tileY), term)) {
            termMap.put(new Point(tileX, tileY), term);
            return super.computeTile(tileX, tileY);
        } else {
            if (noDataRaster == null) {
                synchronized (this) {
                    if (noDataRaster == null) {
                        noDataRaster = createNoDataRaster(fillValue == null ? 0.0 : fillValue.doubleValue());
                    }
                }
            }
            return noDataRaster.createTranslatedChild(tileXToX(tileX), tileYToY(tileY));
        }
    }

    @Override
    protected void computeRect(PlanarImage[] planarImages, WritableRaster writableRaster, Rectangle destRect) {
        final Term term = termMap.remove(getTileIndices(destRect)[0]);
        final ProductData productData = ProductData.createInstance(dataType,
                                                                   ImageUtils.getPrimitiveArray(
                                                                           writableRaster.getDataBuffer()));
        final int x = destRect.x - writableRaster.getMinX();
        final int y = destRect.y - writableRaster.getMinY();
        final int w = writableRaster.getWidth();

        final int colCount = destRect.width;
        final int rowCount = destRect.height;
        final int pixelCount = colCount * rowCount;
        final RasterDataEvalEnv env = new RasterDataEvalEnv(destRect.x, destRect.y,
                                                            colCount, rowCount,
                                                            getLevelImageSupport());

        if (mask) {
            for (int i = 0, k = w * y; i < pixelCount; i += colCount, k += w) {
                for (int j = 0, l = x; j < colCount; j++, l++) {
                    env.setElemIndex(i + j);
                    productData.setElemUIntAt(k + l, term.evalB(env) ? TRUE : FALSE);
                }
            }
        } else {
            if (fillValue != null) {
                final double fv = fillValue.doubleValue();
                for (int i = 0, k = w * y; i < pixelCount; i += colCount, k += w) {
                    for (int j = 0, l = x; j < colCount; j++, l++) {
                        env.setElemIndex(i + j);
                        final double v = term.evalD(env);
                        if (Double.isNaN(v) || Double.isInfinite(v)) {
                            productData.setElemDoubleAt(k + l, fv);
                        } else {
                            productData.setElemDoubleAt(k + l, v);
                        }
                    }
                }
            } else {
                for (int i = 0, k = w * y; i < pixelCount; i += colCount, k += w) {
                    for (int j = 0, l = x; j < colCount; j++, l++) {
                        env.setElemIndex(i + j);
                        productData.setElemDoubleAt(k + l, term.evalD(env));
                    }
                }
            }
        }
    }

    private Term parseExpression() {
        final Term term;
        try {
            term = BandArithmetic.parseExpression(expression, products, defaultProductIndex);
        } catch (ParseException e) {
            throw new RuntimeException(MessageFormat.format(
                    "Could not parse expression: ''{0}''.", expression), e);
        }
        final ImageManager imageManager = ImageManager.getInstance();
        for (final RasterDataSymbol symbol : BandArithmetic.getRefRasterDataSymbols(term)) {
            if (imageManager.getSourceImage(symbol.getRaster(), getLevel()) == this) {
                throw new RuntimeException(MessageFormat.format(
                        "Invalid reference ''{0}''.", symbol.getName()));
            }
        }
        return term;
    }

    private boolean addDataToReferredRasterDataSymbols(Rectangle destRect, Term term) {
        for (final RasterDataSymbol symbol : BandArithmetic.getRefRasterDataSymbols(term)) {
            final RenderedImage sourceImage;
            final int dataType;
            final RasterDataNode rasterDataNode = symbol.getRaster();
            if (symbol.getSource() == RasterDataSymbol.GEOPHYSICAL) {
                sourceImage = ImageManager.getInstance().getGeophysicalImage(rasterDataNode, getLevel());
                dataType = rasterDataNode.getGeophysicalDataType();
            } else {
                sourceImage = ImageManager.getInstance().getSourceImage(rasterDataNode, getLevel());
                dataType = rasterDataNode.getDataType();
            }
            final Raster sourceRaster = sourceImage.getData(destRect);
            if (sourceRaster instanceof NoDataRaster) {
                return false;
            }
            DataBuffer dataBuffer = sourceRaster.getDataBuffer();
            if (dataBuffer.getSize() != destRect.width * destRect.height) {
                final WritableRaster writableRaster = sourceRaster.createCompatibleWritableRaster(destRect);
                sourceImage.copyData(writableRaster);
                dataBuffer = writableRaster.getDataBuffer();
            }
            symbol.setData(ProductData.createInstance(dataType, ImageUtils.getPrimitiveArray(dataBuffer)));
        }
        return true;
    }

}