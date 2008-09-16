package org.esa.beam.dataio.geotiff;

import com.bc.ceres.core.Assert;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductNode;
import org.jdom.Document;
import org.jdom.Element;

import java.util.List;
import java.util.ArrayList;

/**
 * todo - add API doc
 *
 * @author Sabine Embacher
 * @author Marco Peters
 * @version $Revision: $ $Date: $
 * @since BEAM 4.5
 */
class BeamMetadata {

    public static final int PRIVATE_TIFF_TAG_NUMBER = 65000;

    public static final String ROOT_NODENAME = "beam_metadata";
    public static final String ROOT_ATTRIB = "version";

    public static final String ROOT_VERSION_0_1 = "0.1";

    public static final String NODE_NAME = "name";
    public static final String NODE_PRODUCT = "product";

    public static final String NODE_PRODUCTTYPE = "product_type";
    public static final String NODE_BAND = "band";
    public static final String NODE_DATATYPE = "data_type";
    public static final String NODE_SCALING_FACTOR = "scaling_factor";
    public static final String NODE_SCALING_OFFSET = "scaling_offset";
    public static final String NODE_LOG_10_SCALED = "log_10_scaled";
    public static final String NODE_NO_DATA_VALUE = "no_data_value";
    public static final String NODE_NO_DATA_VALUE_USED = "no_data_value_used";

    public static interface Validator {

        public boolean validate(ProductNode node);
    }

    static Metadata createMetadata(final Document dom) {
        Assert.notNull(dom);
        if (!isBeamMetadata(dom)) {
            throw new IllegalArgumentException("DOM not valid");
        }
        return new DomMetadata_0_1(dom);
    }

    static Metadata createMetadata(final Product product, Validator validator) {
        Assert.notNull(product);
        Assert.notNull(validator);
        return new ProductMetadata(product, validator);
    }

    static boolean isBeamMetadata(final Document dom) {
        if (dom == null) {
            return false;
        }
        if (!dom.getRootElement().getName().equals(BeamMetadata.ROOT_NODENAME)) {
            return false;
        }
        if (dom.getRootElement().getAttribute(BeamMetadata.ROOT_ATTRIB) == null) {
            return false;
        }
        return true;
    }

    static interface Metadata {

        public Document getDocument();

        public String getProductProperty(final String name);

        public String getBandProperty(final int bandindex, final String name);
    }

    static class DomMetadata_0_1 implements Metadata {

        private final Document dom;

        public DomMetadata_0_1(final Document dom) {
            this.dom = dom;
        }

        public Document getDocument() {
            return (Document) dom.clone();
        }

        public String getProductProperty(final String name) {
            final Element product = getProductElem();
            return product.getChildText(name);
        }

        public String getBandProperty(final int bandindex, final String name) {
            final List<Element> bands = getProductElem().getChildren(NODE_BAND);
            if (bands == null || bandindex >= bands.size()) {
                return null;
            }
            final Element bandElem = bands.get(bandindex);
            return bandElem.getChildText(name);
        }

        private Element getProductElem() {
            return dom.getRootElement().getChild(NODE_PRODUCT);
        }
    }

    static class ProductMetadata implements Metadata {

        private final Product product;
        private final ArrayList<Band> bandList;

        public ProductMetadata(final Product product, Validator validator) {
            this.product = product;
            bandList = initBandsToWrite(validator);
        }

        public Document getDocument() {
            final Document dom = new Document();
            final Element root = new Element(ROOT_NODENAME);
            root.setAttribute(ROOT_ATTRIB, ROOT_VERSION_0_1);
            dom.setRootElement(root);

            final Element productNode = new Element(NODE_PRODUCT);
            productNode.addContent(new Element(NODE_NAME).setText(getProductProperty(NODE_NAME)));
            productNode.addContent(new Element(NODE_PRODUCTTYPE).setText(getProductProperty(NODE_PRODUCTTYPE)));
            root.addContent(productNode);

            for (int i = 0; i < bandList.size(); i++) {
                final Element bandNode = new Element(NODE_BAND);
                bandNode.addContent(new Element(NODE_NAME).setText(getBandProperty(i, NODE_NAME)));
                bandNode.addContent(new Element(NODE_DATATYPE).setText(getBandProperty(i, NODE_DATATYPE)));
                bandNode.addContent(new Element(NODE_SCALING_FACTOR).setText(getBandProperty(i, NODE_SCALING_FACTOR)));
                bandNode.addContent(new Element(NODE_SCALING_OFFSET).setText(getBandProperty(i, NODE_SCALING_OFFSET)));
                bandNode.addContent(new Element(NODE_LOG_10_SCALED).setText(getBandProperty(i, NODE_LOG_10_SCALED)));
                bandNode.addContent(new Element(NODE_NO_DATA_VALUE).setText(getBandProperty(i, NODE_NO_DATA_VALUE)));
                bandNode.addContent(
                            new Element(NODE_NO_DATA_VALUE_USED).setText(getBandProperty(i, NODE_NO_DATA_VALUE_USED)));
                productNode.addContent(bandNode);
            }

            return dom;
        }

        public String getProductProperty(final String name) {
            if (NODE_NAME.equals(name)) {
                return product.getName();
            } else if (NODE_PRODUCTTYPE.equals(name)) {
                return product.getProductType();
            }
            return null;
        }

        public String getBandProperty(int bandindex, String name) {
            if (bandindex >= bandList.size()) {
                return null;
            }
            final Band band = bandList.get(bandindex);
            if (NODE_NAME.equals(name)) {
                return band.getName();
            } else if (NODE_DATATYPE.equals(name)) {
                return String.valueOf(band.getDataType());
            } else if (NODE_SCALING_FACTOR.equals(name)) {
                return String.valueOf(band.getScalingFactor());
            } else if (NODE_SCALING_OFFSET.equals(name)) {
                return String.valueOf(band.getScalingOffset());
            } else if (NODE_LOG_10_SCALED.equals(name)) {
                return String.valueOf(band.isLog10Scaled());
            } else if (NODE_NO_DATA_VALUE.equals(name)) {
                return String.valueOf(band.getNoDataValue());
            } else if (NODE_NO_DATA_VALUE_USED.equals(name)) {
                return String.valueOf(band.isNoDataValueUsed());
            }
            return null;
        }

        private ArrayList<Band> initBandsToWrite(Validator validator) {
            final Band[] bands = product.getBands();
            final ArrayList<Band> bandList = new ArrayList<Band>();
            for (Band band : bands) {
                if (validator.validate(band)) {
                    bandList.add(band);
                }
            }
            return bandList;
        }
    }
}
