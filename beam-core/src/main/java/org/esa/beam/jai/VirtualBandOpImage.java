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
import javax.media.jai.RasterAccessor;
import java.awt.Rectangle;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;


/**
 * An {@code OpImage} which retrieves its data from the product reader associated with the
 * given {@code RasterDataNode} at a given pyramid level.
 */
public class VirtualBandOpImage extends SingleBandedOpImage {
    private final Product[] products;
    private final String expression;

    public VirtualBandOpImage(Product[] products, String expression, int dataType, int level) {
        super(ImageManager.getDataBufferType(dataType),
              products[0].getSceneRasterWidth(),
              products[0].getSceneRasterHeight(),
              products[0].getPreferredTileSize(),
              null,
              level);
        // todo - check products for compatibilits
        this.products = products;
        this.expression = expression;
    }

    @Override
    protected void computeRect(PlanarImage[] planarImages, WritableRaster writableRaster, Rectangle destRect) {
        WritableNamespace namespace = BandArithmetic.createDefaultNamespace(products, 0);
        final Term term;
        try {
            Parser parser = new ParserImpl(namespace, false);
            term = parser.parse(expression);
        } catch (ParseException e) {
            throw new IllegalStateException("Could not parse expression: " + expression, e);
        }
        RasterDataSymbol[] rasterDataSymbols = BandArithmetic.getRefRasterDataSymbols(term);
        for (RasterDataSymbol rasterDataSymbol : rasterDataSymbols) {
            RasterDataNode sourceRDN = rasterDataSymbol.getRaster();
            PlanarImage sourceImage = ImageManager.getInstance().getGeophysicalBandImage(sourceRDN, getLevel());
            Raster sourceRaster = sourceImage.getData(destRect);
            Object sourceArray = ImageUtils.getPrimitiveArray(sourceRaster.getDataBuffer());
            ProductData productData = ProductData.createInstance(sourceRDN.getGeophysicalDataType(), sourceArray);
            rasterDataSymbol.setData(productData);
        }
        RasterAccessor targetAccessor = new RasterAccessor(writableRaster,
                                                           destRect,
                                                           getFormatTags()[0],
                                                           getColorModel());
        RasterDataEvalEnv env = new RasterDataEvalEnv(destRect.x, destRect.y, destRect.width, destRect.height);
        int pixelIndex;
        int lineIndex = targetAccessor.getBandOffset(0);
        for (int y = destRect.y; y < destRect.y + destRect.height; y++) {
            env.setPixelY(y);
            pixelIndex = lineIndex;
            for (int x = destRect.x; x < destRect.x + destRect.width; x++) {
                env.setElemIndex(pixelIndex);
                env.setPixelX(x);
                writableRaster.setSample(x, y, 0, term.evalD(env));
                pixelIndex++;
            }
            lineIndex += targetAccessor.getScanlineStride();
        }
    }
}