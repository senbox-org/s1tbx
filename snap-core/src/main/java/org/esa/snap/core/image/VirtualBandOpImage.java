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

package org.esa.snap.core.image;

import com.bc.ceres.core.Assert;
import com.bc.ceres.jai.NoDataRaster;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.dataop.barithm.BandArithmetic;
import org.esa.snap.core.dataop.barithm.RasterDataEvalEnv;
import org.esa.snap.core.dataop.barithm.RasterDataSymbol;
import org.esa.snap.core.dataop.barithm.RasterDataSymbolReplacer;
import org.esa.snap.core.jexp.ParseException;
import org.esa.snap.core.jexp.Term;
import org.esa.snap.core.jexp.impl.TermDecompiler;
import org.esa.snap.core.util.ImageUtils;

import javax.media.jai.PlanarImage;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
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

    private final Term term;
    private final int dataType;
    private final Number fillValue;
    private final boolean mask;

    private final Map<Point, Term> effectiveTerms = new ConcurrentHashMap<Point, Term>();

    private volatile NoDataRaster noDataRaster;

    public static Builder builder(Term term) {
        return new Builder(term);
    }

    public static Builder builder(String expression, Product source) {
        return builder(expression, 0, source);
    }

    public static Builder builder(String expression, int contextSourceIndex, Product... sources) {
        Term term = parseExpression(expression, contextSourceIndex, sources);
        Product contextSource = sources[contextSourceIndex];
        Dimension sourceSize = new Dimension(contextSource.getSceneRasterWidth(), contextSource.getSceneRasterHeight());
        // todo - [multisize_products] tile size shall fit to level
        Dimension tileSize = contextSource.getPreferredTileSize();
        return new Builder(term)
                .sourceSize(sourceSize)
                .tileSize(tileSize);
    }

    public static Term parseExpression(String expression, Product source) {
        int contextSourceIndex;
        Product[] sources;
        if (source.getProductManager() != null) {
            sources = source.getProductManager().getProducts();
            contextSourceIndex = source.getProductManager().getProductIndex(source);
        } else {
            sources = new Product[]{source};
            contextSourceIndex = 0;
        }
        return parseExpression(expression, contextSourceIndex, sources);
    }

    public static Term parseExpression(String expression, int contextSourceIndex, Product... sources) {
        Assert.notNull(expression, "expression");
        Assert.argument(!expression.trim().isEmpty(), "!expression.trim().isEmpty()");
        Assert.argument(contextSourceIndex >= 0, "contextSourceIndex >= 0");
        Assert.argument(sources.length > 0, "sources.length > 0");
        for (int i = 0; i < sources.length; i++) {
            Assert.argument(sources[i] != null, "sources[" + i + "] != null");
        }
        try {
            return BandArithmetic.parseExpression(expression, sources, contextSourceIndex);
        } catch (ParseException e) {
            throw new IllegalArgumentException("expression: " + e.getMessage(), e);
        }
    }

    /**
     * Used to create instances of {@link VirtualBandOpImage}s.
     *
     * @author Norman Fomferra
     */
    public static class Builder {
        private final Term term;
        private Integer dataType;
        private Number fillValue;
        private Boolean mask;
        private Dimension sourceSize;
        private Dimension tileSize;
        private Map imageConfig;
        private ResolutionLevel level;

        protected Builder(Term term) {
            Assert.notNull(term, "term");
            this.term = term;
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

        public Builder sourceSize(Dimension sourceSize) {
            Assert.notNull(sourceSize, "sourceSize");
            this.sourceSize = sourceSize;
            return this;
        }

        public Builder tileSize(Dimension tileSize) {
            this.tileSize = tileSize;
            return this;
        }

        public Builder imageConfig(Map imageConfig) {
            this.imageConfig = imageConfig;
            return this;
        }

        public Builder level(ResolutionLevel level) {
            this.level = level;
            return this;
        }

        public VirtualBandOpImage create() {
            Assert.state(sourceSize != null, "sourceSize != null");
            if (Boolean.TRUE.equals(mask) && dataType != null) {
                Assert.state(dataType.equals(ProductData.TYPE_UINT8), "dataType.equals(ProductData.TYPE_UINT8)");
            }

            boolean mask = this.mask != null ? this.mask : false;
            int dataType = this.dataType != null ? this.dataType : (mask ? ProductData.TYPE_UINT8 : ProductData.TYPE_FLOAT32);
            ResolutionLevel level = this.level != null ? this.level : ResolutionLevel.MAXRES;
            return new VirtualBandOpImage(term,
                                          dataType,
                                          fillValue,
                                          mask,
                                          sourceSize.width,
                                          sourceSize.height,
                                          tileSize,
                                          imageConfig,
                                          level);
        }
    }

    private VirtualBandOpImage(Term term,
                               int dataType,
                               Number fillValue,
                               boolean mask,
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
        this.term = term;
        this.dataType = dataType;
        this.mask = mask;
        this.fillValue = fillValue;
    }

    public int getDataType() {
        return dataType;
    }

    public boolean isMask() {
        return mask;
    }

    public String getExpression() {
        return new TermDecompiler().decompile(term);
    }

    @Override
    public synchronized void dispose() {
        effectiveTerms.clear();
        super.dispose();
    }

    @Override
    public Raster computeTile(int tileX, int tileY) {
        final Term effectiveTerm = new RasterDataSymbolReplacer().apply(this.term);
        // todo - addDataToReferredRasterDataSymbols() makes wrong assumptions wrt its return value!       (nf, 2015-07-25)
        //        Consider expr "A < 0 ? B : C" --> if C is a no-data tile at (tileX,tileY), then computeTile()
        //        will return a noDataRaster, regardless of A's actual pixel values.
        //
        if (addDataToReferredRasterDataSymbols(getTileRect(tileX, tileY), effectiveTerm)) {
            effectiveTerms.put(new Point(tileX, tileY), effectiveTerm);
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
        final Term effectiveTerm = effectiveTerms.remove(getTileIndices(destRect)[0]);
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
                    productData.setElemUIntAt(k + l, effectiveTerm.evalB(env) ? TRUE : FALSE);
                }
            }
        } else if (fillValue != null) {
            final double fv = fillValue.doubleValue();
            for (int i = 0, k = w * y; i < pixelCount; i += colCount, k += w) {
                for (int j = 0, l = x; j < colCount; j++, l++) {
                    env.setElemIndex(i + j);
                    final double v = effectiveTerm.evalD(env);
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
                    productData.setElemDoubleAt(k + l, effectiveTerm.evalD(env));
                }
            }
        }
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
