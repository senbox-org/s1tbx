package org.esa.beam.jai;

import java.awt.Rectangle;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;

import javax.media.jai.PlanarImage;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.dataop.barithm.BandArithmetic;
import org.esa.beam.framework.dataop.barithm.RasterDataSymbol;
import org.esa.beam.util.ImageUtils;

import com.bc.jexp.ParseException;
import com.bc.jexp.Parser;
import com.bc.jexp.Term;
import com.bc.jexp.WritableNamespace;
import com.bc.jexp.impl.ParserImpl;


/**
 * An {@code OpImage} which retrieves its data from the product reader associated with the
 * given {@code RasterDataNode} at a given pyramid level.
 */
public class VirtualBandOpImage extends SingleBandedOpImage {
    private final Product[] products;
    private final String expression;
    private final int dataType;

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
        this.dataType = dataType;
    }

    @Override
    protected void computeRect(PlanarImage[] planarImages, WritableRaster writableRaster, Rectangle rectangle) {
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
            // todo - sourceImage = ImageManager#getScaledBandImage(rdn, level);
            PlanarImage planarImage = ImageManager.getInstance().getBandImage(rasterDataSymbol.getRaster(), getLevel());
            final Raster raster = planarImage.getData(rectangle);
            final Object array = ImageUtils.getPrimitiveArray(raster.getDataBuffer());
            final ProductData productData = ProductData.createInstance(dataType, array);
            rasterDataSymbol.setData(productData);
        }



    }
}