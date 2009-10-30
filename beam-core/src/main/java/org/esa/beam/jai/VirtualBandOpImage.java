package org.esa.beam.jai;

import com.bc.ceres.core.Assert;
import com.bc.jexp.ParseException;
import com.bc.jexp.Parser;
import com.bc.jexp.Term;
import com.bc.jexp.WritableNamespace;
import com.bc.jexp.impl.ParserImpl;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.dataop.barithm.BandArithmetic;
import org.esa.beam.framework.dataop.barithm.RasterDataEvalEnv;
import org.esa.beam.framework.dataop.barithm.RasterDataSymbol;
import org.esa.beam.util.ImageUtils;

import javax.media.jai.PlanarImage;
import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;


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

    public static VirtualBandOpImage createMask(RasterDataNode raster,
                                                ResolutionLevel level) {
        return createMask(raster.getValidMaskExpression(),
                          raster.getProduct(),
                          level);
    }

    public static VirtualBandOpImage createMask(String expression,
                                                Product product,
                                                ResolutionLevel level) {
        return create(expression,
                      ProductData.TYPE_UINT8,
                      null,
                      true,
                      product,
                      level);
    }

    public static VirtualBandOpImage createMask(String expression,
                                                Product[] products,
                                                int defaultProductIndex,
                                                ResolutionLevel level) {
        return create(expression,
                      ProductData.TYPE_UINT8,
                      null,
                      true,
                      products,
                      defaultProductIndex,
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
                      level);
    }

    private static VirtualBandOpImage create(String expression,
                                             int dataType,
                                             Number fillValue,
                                             boolean mask,
                                             Product product,
                                             ResolutionLevel level) {
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
                      level);
    }

    private static VirtualBandOpImage create(String expression,
                                             int dataType,
                                             Number fillValue,
                                             boolean mask,
                                             Product[] products,
                                             int defaultProductIndex,
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
                                      defaultProductIndex,
                                      level);
    }

    private VirtualBandOpImage(String expression,
                               int dataType,
                               Number fillValue,
                               boolean mask,
                               Product[] products,
                               int defaultProductIndex,
                               ResolutionLevel level) {
        super(ImageManager.getDataBufferType(dataType),
              products[defaultProductIndex].getSceneRasterWidth(),
              products[defaultProductIndex].getSceneRasterHeight(),
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
        for (int i = 0; i < products.length; i++) {
            products[i] = null;
        }
    }

    @Override
    protected void computeRect(PlanarImage[] planarImages, WritableRaster writableRaster, Rectangle destRect) {
        final Term term = parseExpression();
        addSourceToRasterDataSymbols(writableRaster.getBounds(), term);

        final ProductData productData = ProductData.createInstance(dataType,
                                                                   ImageUtils.getPrimitiveArray(
                                                                           writableRaster.getDataBuffer()));
        final int rasterSize = writableRaster.getDataBuffer().getSize();
        final RasterDataEvalEnv env = new RasterDataEvalEnv(destRect.x, destRect.y, destRect.width, destRect.height);
        if (mask) {
            for (int i = 0; i < rasterSize; i++) {
                env.setElemIndex(i);
                productData.setElemUIntAt(i, term.evalB(env) ? TRUE : FALSE);
            }
        } else {
            if (fillValue != null) {
                final double fv = fillValue.doubleValue();
                for (int i = 0; i < rasterSize; i++) {
                    env.setElemIndex(i);
                    final double v = term.evalD(env);
                    if (!Double.isNaN(v) && !Double.isInfinite(v)) {
                        productData.setElemDoubleAt(i, v);
                    } else {
                        productData.setElemDoubleAt(i, fv);
                    }
                }
            } else {
                for (int i = 0; i < rasterSize; i++) {
                    env.setElemIndex(i);
                    productData.setElemDoubleAt(i, term.evalD(env));
                }
            }
        }
    }

    private Term parseExpression() {
        WritableNamespace namespace = BandArithmetic.createDefaultNamespace(products, defaultProductIndex);
        final Term term;
        try {
            Parser parser = new ParserImpl(namespace, false);
            term = parser.parse(expression);
        } catch (ParseException e) {
            throw new IllegalStateException("Could not parse expression: " + expression, e);
        }
        return term;
    }

    private void addSourceToRasterDataSymbols(Rectangle destRect, final Term term) {
        RasterDataSymbol[] rasterDataSymbols = BandArithmetic.getRefRasterDataSymbols(term);
        for (RasterDataSymbol rasterDataSymbol : rasterDataSymbols) {
            RasterDataNode sourceRDN = rasterDataSymbol.getRaster();
            RenderedImage sourceImage;
            int productDataType;
            if (rasterDataSymbol.getSource() == RasterDataSymbol.GEOPHYSICAL) {
                sourceImage = ImageManager.getInstance().getGeophysicalImage(sourceRDN, getLevel());
                productDataType = sourceRDN.getGeophysicalDataType();
            } else {
                sourceImage = ImageManager.getInstance().getSourceImage(sourceRDN, getLevel());
                productDataType = sourceRDN.getDataType();
            }
            Raster sourceRaster = sourceImage.getData(destRect);
            DataBuffer dataBuffer = sourceRaster.getDataBuffer();
            if (dataBuffer.getSize() != destRect.width * destRect.height) {
                WritableRaster wr =
                    sourceRaster.createCompatibleWritableRaster(destRect);
                sourceImage.copyData(wr);
                dataBuffer = wr.getDataBuffer();
            }
            Object sourceArray = ImageUtils.getPrimitiveArray(dataBuffer);
            ProductData productData = ProductData.createInstance(productDataType, sourceArray);
            rasterDataSymbol.setData(productData);
        }
    }

    /////////////////////////////////////////////////////////////////////////
    // Deprecated API

    @Deprecated
    public VirtualBandOpImage(Product[] products,
                              String expression,
                              int dataType,
                              ResolutionLevel level) {
        this(expression, dataType, null, false, products, 0, level);
    }

    @Deprecated
    public VirtualBandOpImage(String expression,
                              int dataType,
                              Product[] products,
                              int defaultProductIndex,
                              ResolutionLevel level) {
        this(expression, dataType, null, false, products, defaultProductIndex, level);
    }

    @Deprecated
    public static VirtualBandOpImage createMaskOpImage(Product product,
                                                       String expression,
                                                       ResolutionLevel level) {
        return createMask(expression, product, level);
    }

    @Deprecated
    public static VirtualBandOpImage createMaskOpImage(Product[] products,
                                                       String expression,
                                                       ResolutionLevel level) {
        return createMask(expression, products, 0, level);
    }


}