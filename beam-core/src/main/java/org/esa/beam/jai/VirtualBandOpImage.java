package org.esa.beam.jai;

import com.bc.ceres.glevel.DownscalableImage;
import org.esa.beam.framework.datamodel.Product;

import javax.media.jai.PlanarImage;
import java.awt.Rectangle;
import java.awt.image.WritableRaster;


/**
 * An {@code OpImage} which retrieves its data from the product reader associated with the
 * given {@code RasterDataNode} at a given pyramid level.
 */
public class VirtualBandOpImage extends SingleBandedOpImage {
    private Product[] products;
    private String expression;
    private int dataType;

    public VirtualBandOpImage(Product[] products, String expression, int dataType) {
        super(ImageManager.getDataBufferType(dataType),
              products[0].getSceneRasterWidth(),
              products[0].getSceneRasterHeight(),
              products[0].getPreferredTileSize(),
              null);
        init(products, expression, dataType);
    }

    private VirtualBandOpImage(Product[] products, String expression, int dataType, DownscalableImageSupport level0, int level) {
        super(level0, level, null);
        init(products, expression, dataType);

    }

    private void init(Product[] products, String expression, int dataType) {
        // todo - check products for compatibilits
        this.products = products;
        this.expression = expression;
        this.dataType = dataType;
    }

    @Override
    public DownscalableImage createDownscalableImage(int level) {
        return new VirtualBandOpImage(products, expression, dataType, getDownscalableImageSupport().getLevel0(), level);
    }

    public Product[] getProducts() {
        return products;
    }

    public String getExpression() {
        return expression;
    }

    public int getDataType() {
        return dataType;
    }

    @Override
    protected void computeRect(PlanarImage[] planarImages, WritableRaster writableRaster, Rectangle rectangle) {
        // todo - impl.
    }
}