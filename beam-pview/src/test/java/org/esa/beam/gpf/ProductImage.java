package org.esa.beam.gpf;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;

import javax.media.jai.CollectionImage;
import java.awt.image.RenderedImage;
import java.util.ArrayList;

/**
 * todo - add API doc
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
public class ProductImage extends CollectionImage {
    final Product product;

    /**
     * Default constructor.  The <code>imageCollection</code> parameter is
     * <code>null</code>.  Subclasses that use this constructor must either
     * set the <code>imageCollection</code> parameter themselves, or override
     * the methods defined in the <code>Collection</code> interface.
     * Otherwise, a <code>NullPointerException</code> may be thrown at a later
     * time when methods which use to the <code>imageCollection</code>
     * instance variable are invoked.
     *
     * @param product The product
     */
    public ProductImage(Product product) {
        this.product = product;
        final ArrayList<RenderedImage> images = new ArrayList<RenderedImage>();
        final Band[] bands = product.getBands();
        for (Band band : bands) {
            images.add(band.getSourceImage());
        }
        imageCollection = images;
    }

    public Product getProduct() {
        return product;
    }
}
