package org.esa.beam.jai;

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
import java.awt.image.Raster;
import java.awt.image.WritableRaster;


/**
 * An {@code OpImage} which retrieves its data from the product reader associated with the
 * given {@code RasterDataNode} at a given pyramid level.
 */
public class VirtualBandOpImage extends SingleBandedOpImage {
    private static final int TRUE = 255;
    private static final int FALSE = 0;

    private final Product[] products;
    private final String expression;
    private final boolean mask;
    private final int dataType;

    public static VirtualBandOpImage createMaskOpImage(RasterDataNode rasterDataNode, ResolutionLevel level) {
        return createMaskOpImage(rasterDataNode.getProduct(),
                                 rasterDataNode.getValidMaskExpression(),
                                 level);
    }

    public static VirtualBandOpImage createMaskOpImage(Product product,
                                                       String expression,
                                                       ResolutionLevel level) {
        return new VirtualBandOpImage(new Product[]{product},
                                      expression,
                                      ProductData.TYPE_UINT8,
                                      true,
                                      level);
    }

    public VirtualBandOpImage(Product[] products, String expression, int dataType, ResolutionLevel level) {
        this(products, expression, dataType, false, level);
    }

    public VirtualBandOpImage(Product[] products, String expression, int dataType, boolean mask, ResolutionLevel level) {
        super(ImageManager.getDataBufferType(dataType),
              products[0].getSceneRasterWidth(),
              products[0].getSceneRasterHeight(),
              products[0].getPreferredTileSize(),
              null,
              level);
        // todo - check products for compatibility
        this.products = products;
        this.expression = expression;
        this.dataType = dataType;
        this.mask = mask;
    }

    @Override
    protected void computeRect(PlanarImage[] planarImages, WritableRaster writableRaster, Rectangle destRect) {
        final Term term = createTerm();
        addSourceToRasterDataSymbols(destRect, term);

        final ProductData productData = ProductData.createInstance(dataType, ImageUtils.getPrimitiveArray(writableRaster.getDataBuffer()));
        final int rasterSize = writableRaster.getDataBuffer().getSize();
        final RasterDataEvalEnv env = new RasterDataEvalEnv(destRect.x, destRect.y, destRect.width, destRect.height);
        if (mask) {
            for (int i = 0; i < rasterSize; i++) {
                env.setElemIndex(i);
                productData.setElemUIntAt(i, term.evalB(env) ? TRUE : FALSE);
            }
        } else {
            for (int i = 0; i < rasterSize; i++) {
                env.setElemIndex(i);
                productData.setElemDoubleAt(i, term.evalD(env));
            }
        }
    }

    private Term createTerm() {
        WritableNamespace namespace = BandArithmetic.createDefaultNamespace(products, 0);
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
            PlanarImage sourceImage = ImageManager.getInstance().getGeophysicalImage(sourceRDN, getLevel());
            Raster sourceRaster = sourceImage.getData(destRect);
            Object sourceArray = ImageUtils.getPrimitiveArray(sourceRaster.getDataBuffer());
            ProductData productData = ProductData.createInstance(sourceRDN.getGeophysicalDataType(), sourceArray);
            rasterDataSymbol.setData(productData);
        }
    }

}