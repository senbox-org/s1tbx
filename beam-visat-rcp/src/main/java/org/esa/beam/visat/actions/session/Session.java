package org.esa.beam.visat.actions.session;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.product.ProductNodeView;
import org.esa.beam.framework.dataio.ProductIO;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.awt.Rectangle;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.bc.ceres.core.ProgressMonitor;

/**
 * todo - add API doc
*
 * @author Ralf Quast
 * @author Norman Fomferra
* @version $Revision$ $Date$
* @since BEAM 4.6
*/
@XStreamAlias("session")
public class Session {

    public static final String CURRENT_MODEL_VERSION = "1.0.0";

    private String modelVersion;

    @XStreamAlias("products")
    private ProductRef[] productRefs;
    @XStreamAlias("views")
    private ViewRef[] viewRefs;

    public Session() {
        modelVersion = CURRENT_MODEL_VERSION;
        productRefs = new ProductRef[0];
        viewRefs = new ViewRef[0];
    }

    public Session(Product[] products, ProductNodeView[] views) {
        modelVersion = CURRENT_MODEL_VERSION;

        productRefs = new ProductRef[products.length];
        for (int i = 0; i < products.length; i++) {
            Product product = products[i];
            productRefs[i] = new ProductRef(product.getRefNo(), product.getFileLocation());
        }

        viewRefs = new ViewRef[views.length];
        for (int i = 0; i < views.length; i++) {
            ProductNodeView view = views[i];
            viewRefs[i] = new ViewRef(i,
                                      view.getClass().getName(),
                                      view.getVisibleProductNode().getProduct().getRefNo(),
                                      view.getVisibleProductNode().getName(),
                                      view.getBounds());
        }
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public int getProductCount() {
        return productRefs.length;
    }

    public ProductRef getProductRef(int index) {
        return productRefs[index];
    }

    public int getViewCount() {
        return viewRefs.length;
    }

    public ViewRef getViewRef(int index) {
        return viewRefs[index];
    }

    public Product[] restoreProducts(ProgressMonitor pm, List<IOException> problems) {
        ArrayList<Product> products = new ArrayList<Product>();
        try {
            pm.beginTask("Restoring products", productRefs.length);
            for (ProductRef productRef : productRefs) {
                try {
                    final Product product = ProductIO.readProduct(productRef.fileLocation, null);
                    products.add(product);
                    product.setRefNo(productRef.refNo);
                } catch (IOException e) {
                    problems.add(e);
                }
                pm.worked(1);
            }
        } finally {
            pm.done();
        }
        return products.toArray(new Product[products.size()]);
    }

    public ProductNodeView[] restoreViews(Product[] restoredProducts, ProgressMonitor aNull) {
        return new ProductNodeView[0];
    }

    @XStreamAlias("product")
    public static class ProductRef {
        int refNo;
        File fileLocation;

        public ProductRef(int refNo, File fileLocation) {
            this.refNo = refNo;
            this.fileLocation = fileLocation;
        }
    }

    @XStreamAlias("views")
    public static class ViewRef {
        int id;
        String type;
        int productRefNo;
        String nodeName;
        Rectangle bounds;

        public ViewRef(int id, String type, int productRefNo, String nodeName, Rectangle bounds) {
            this.id = id;
            this.type = type;
            this.productRefNo = productRefNo;
            this.nodeName = nodeName;
            this.bounds = bounds;
        }
    }
}
