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

package org.esa.snap.jai;

import com.bc.ceres.core.Assert;
import com.bc.ceres.jai.NoDataRaster;
import com.bc.jexp.ParseException;
import com.bc.jexp.Term;
import org.esa.snap.framework.datamodel.Product;
import org.esa.snap.framework.datamodel.ProductData;
import org.esa.snap.framework.datamodel.RasterDataNode;
import org.esa.snap.framework.dataop.barithm.BandArithmetic;
import org.esa.snap.framework.dataop.barithm.RasterDataEvalEnv;
import org.esa.snap.framework.dataop.barithm.RasterDataSymbol;
import org.esa.snap.util.ImageUtils;

import javax.media.jai.PlanarImage;
import java.awt.Dimension;
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
 *
 * @author Norman Fomferra
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

    /**
     * Used to create instances of {@link VirtualBandOpImage}s.
     *
     * @author Norman Fomferra
     */
    public static class Builder {
        private String expression;
        private Integer dataType;
        private Number fillValue;
        private Boolean mask;
        private Integer contextSourceIndex;
        private Product source;
        private Product[] sources;
        private Dimension sourceSize;
        private Dimension tileSize;
        private Map imageConfig;
        private ResolutionLevel level;

        public Builder() {
        }

        public Builder expression(String expression) {
            Assert.notNull(expression, "expression");
            Assert.argument(!expression.trim().isEmpty(), "!expression.trim().isEmpty()");
            this.expression = expression;
            return this;
        }

        public Builder dataType(int dataType) {
            this.dataType = dataType;
            return this;
        }

        public Builder fillValue(Number fillValue) {
            this.fillValue = fillValue;
            return this;
        }

        public Builder mask(boolean mask) {
            this.mask = mask;
            return this;
        }

        public Builder source(Product source) {
            Assert.notNull(source, "source");
            this.source = source;
            return this;
        }

        public Builder sources(Product... sources) {
            return sources(0, sources);
        }

        public Builder sources(int contextSourceIndex, Product... sources) {
            Assert.argument(contextSourceIndex >= 0, "contextSourceIndex >= 0");
            Assert.argument(sources.length > 0, "sources.length > 0");
            for (int i = 0; i < sources.length; i++) {
                Assert.argument(sources[i] != null, "sources[" + i + "] != null");
            }
            this.contextSourceIndex = contextSourceIndex;
            this.sources = sources;
            return this;
        }

        public Builder sourceSize(Dimension sourceSize) {
            Assert.notNull(sourceSize, "sourceSize");
            this.sourceSize = sourceSize;
            return this;
        }

        public Builder tileSize(Dimension tileSize) {
            Assert.notNull(tileSize, "tileSize");
            this.tileSize = tileSize;
            return this;
        }

        public Builder imageConfig(Map imageConfig) {
            this.imageConfig = imageConfig;
            return this;
        }

        public Builder level(ResolutionLevel level) {
            Assert.notNull(level, "level");
            this.level = level;
            return this;
        }

        public VirtualBandOpImage create() {
            Assert.state(expression != null, "expression != null");
            Assert.state(!(source == null && sources == null), "!(source == null && sources == null)");
            if (Boolean.TRUE.equals(mask) && dataType != null) {
                Assert.state(dataType.equals(ProductData.TYPE_UINT8), "dataType.equals(ProductData.TYPE_UINT8)");
            }

            Product[] sources;
            int contextSourceIndex;
            if (this.source != null && this.sources == null) {
                if (this.source.getProductManager() != null) {
                    sources = this.source.getProductManager().getProducts();
                    contextSourceIndex = this.source.getProductManager().getProductIndex(this.source);
                } else {
                    sources = new Product[]{this.source};
                    contextSourceIndex = 0;
                }
            } else {
                sources = this.sources;
                contextSourceIndex = this.contextSourceIndex != null ? this.contextSourceIndex : 0;
            }

            Assert.state(sources.length > 0, "sources.length > 0");
            Assert.state(contextSourceIndex >= 0, "contextSourceIndex >= 0");
            Assert.state(contextSourceIndex < sources.length, "contextSourceIndex < sources.length");
            // todo - [multisize_products] check expression/sources for compatibility

            boolean mask = this.mask != null ? this.mask : false;
            int dataType = this.dataType != null ? this.dataType : (mask ? ProductData.TYPE_UINT8 : ProductData.TYPE_FLOAT32);
            Product contextSource = sources[contextSourceIndex];
            int sourceWidth = sourceSize != null ? sourceSize.width : contextSource.getSceneRasterWidth();
            int sourceHeight = sourceSize != null ? sourceSize.height : contextSource.getSceneRasterHeight();
            // todo - [multisize_products] tile size shall fit to width / height
            Dimension tileSize = this.tileSize != null ? this.tileSize : contextSource.getPreferredTileSize();
            ResolutionLevel level = this.level != null ? this.level : ResolutionLevel.MAXRES;
            return new VirtualBandOpImage(expression,
                                          dataType,
                                          fillValue,
                                          mask,
                                          contextSourceIndex,
                                          sources,
                                          sourceWidth,
                                          sourceHeight,
                                          tileSize,
                                          imageConfig,
                                          level);
        }
    }

    private VirtualBandOpImage(String expression,
                               int dataType,
                               Number fillValue,
                               boolean mask,
                               int defaultProductIndex,
                               Product[] products,
                               int sourceWidth,
                               int sourceHeight,
                               Dimension tileSize,
                               Map imageConfig,
                               ResolutionLevel level) {
        super(ImageManager.getDataBufferType(dataType),
              sourceWidth,
              sourceHeight,
              tileSize,
              imageConfig,
              level);
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
        super.dispose();
    }

    @Override
    public Raster computeTile(int tileX, int tileY) {
        final Term term = parseExpression();
        // todo - addDataToReferredRasterDataSymbols() makes wrong assumptions wrt its return value!       (nf, 2015-07-25)
        //        Consider expr "A < 0 ? B : C" --> if C is a no-data tile at (tileX,tileY), then computeTile()
        //        will return a noDataRaster, regardless of A's actual pixel values.
        //
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
        } else if (fillValue != null) {
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

    private Term parseExpression() {
        final Term term;
        try {
            term = BandArithmetic.parseExpression(expression, products, defaultProductIndex);
        } catch (ParseException e) {
            throw new RuntimeException(MessageFormat.format(
                    "Could not parse expression: ''{0}''. {1}", expression, e.getMessage()), e);
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
