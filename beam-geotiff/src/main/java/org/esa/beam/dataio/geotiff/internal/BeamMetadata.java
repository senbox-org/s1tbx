package org.esa.beam.dataio.geotiff.internal;

import com.bc.ceres.core.Assert;
import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.ImageInfo;
import org.esa.beam.framework.datamodel.IndexCoding;
import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.Product;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

/**
 * todo - add API doc
 *
 * @author Sabine Embacher
 * @author Marco Peters
 * @version $Revision: $ $Date: $
 * @since BEAM 4.5
 */
public class BeamMetadata {

    public static final int PRIVATE_TIFF_TAG_NUMBER = 65000;

    public static final String ROOT_NODENAME = "beam_metadata";
    public static final String ROOT_ATTRIB = "version";

    public static final String ROOT_VERSION_0_1 = "0.1";

    public static final String NODE_NAME = "name";
    public static final String NODE_SAMPLE = "sample";
    public static final String NODE_VALUE = "value";
    public static final String NODE_COLOUR = "colour";
    public static final String NODE_DESCRIPTION = "description";


    public static final String NODE_PRODUCT = "product";
    public static final String NODE_PRODUCTTYPE = "product_type";

    public static final String NODE_BAND = "band";
    public static final String NODE_UNIT = "unit";
    public static final String NODE_SCALING_FACTOR = "scaling_factor";
    public static final String NODE_SCALING_OFFSET = "scaling_offset";
    public static final String NODE_LOG_10_SCALED = "log_10_scaled";
    public static final String NODE_NO_DATA_VALUE = "no_data_value";
    public static final String NODE_NO_DATA_VALUE_USED = "no_data_value_used";

    public static final String NODE_INDEX_CODING = "index_coding";

    public static Metadata createMetadata(final Document dom) {
        Assert.notNull(dom);
        if (!isBeamMetadata(dom)) {
            throw new IllegalArgumentException("DOM not valid");
        }
        return new DomMetadata_0_1(dom);
    }

    public static Metadata createMetadata(final Product product) {
        Assert.notNull(product);
        return new ProductMetadata(product);
    }

    public static boolean isBeamMetadata(final Document dom) {
        if (dom == null) {
            return false;
        }
        if (!dom.getRootElement().getName().equals(ROOT_NODENAME)) {
            return false;
        }
        return dom.getRootElement().getAttribute(ROOT_ATTRIB) != null;
    }

    public abstract static class Metadata {

        public abstract Document getDocument();

        public abstract String getProductProperty(final String name);

        public abstract String getBandProperty(final int bandindex, final String name);

        public String getAsString() {
            final Document dom = getDocument();
            final StringWriter writer = new StringWriter();
            try {
                new XMLOutputter(Format.getCompactFormat()).output(dom, writer);
            } catch (IOException ignore) {
            }
            return writer.toString();
        }
    }

    static class DomMetadata_0_1 extends Metadata {

        private final Document dom;

        DomMetadata_0_1(final Document dom) {
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

    static class ProductMetadata extends Metadata {

        private final Product product;

        ProductMetadata(final Product product) {
            this.product = product;
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

            for (int i = 0; i < product.getNumBands(); i++) {
                final Element bandNode = new Element(NODE_BAND);
                bandNode.addContent(new Element(NODE_NAME).setText(getBandProperty(i, NODE_NAME)));
                bandNode.addContent(new Element(NODE_DESCRIPTION).setText(getBandProperty(i, NODE_DESCRIPTION)));
                bandNode.addContent(new Element(NODE_UNIT).setText(getBandProperty(i, NODE_UNIT)));
                bandNode.addContent(new Element(NODE_SCALING_FACTOR).setText(getBandProperty(i, NODE_SCALING_FACTOR)));
                bandNode.addContent(new Element(NODE_SCALING_OFFSET).setText(getBandProperty(i, NODE_SCALING_OFFSET)));
                bandNode.addContent(new Element(NODE_LOG_10_SCALED).setText(getBandProperty(i, NODE_LOG_10_SCALED)));
                bandNode.addContent(new Element(NODE_NO_DATA_VALUE).setText(getBandProperty(i, NODE_NO_DATA_VALUE)));
                bandNode.addContent(
                            new Element(NODE_NO_DATA_VALUE_USED).setText(getBandProperty(i, NODE_NO_DATA_VALUE_USED)));
                final Band band = product.getBandAt(i);
                if(band.isIndexBand()) {
                    bandNode.addContent(createIndexCodingNode(band));
                }
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
            if (bandindex >= product.getNumBands()) {
                return null;
            }
            final Band band = product.getBandAt(bandindex);
            if (NODE_NAME.equals(name)) {
                return band.getName();
            } else if (NODE_DESCRIPTION.equals(name)) {
                return band.getDescription();
            } else if (NODE_UNIT.equals(name)) {
                return band.getUnit();
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

        private static Element createIndexCodingNode(Band band) {
            final IndexCoding indexCoding = band.getIndexCoding();
            final ImageInfo imageInfo = band.getImageInfo(ProgressMonitor.NULL);
            final Element indexElem = new Element(NODE_INDEX_CODING);
            indexElem.addContent(new Element(NODE_NAME).setText(indexCoding.getName()));
            final MetadataAttribute[] attributes = indexCoding.getAttributes();
            for (int i = 0; i < attributes.length; i++) {
                MetadataAttribute attribute = attributes[i];
                final Element sampleElem = new Element(NODE_SAMPLE);
                final int sampleValue = attribute.getData().getElemInt();
                final int sampleColor = imageInfo.getColorPaletteDef().getColors()[i].getRGB();

                sampleElem.addContent(new Element(NODE_NAME).setText(attribute.getName()));
                sampleElem.addContent(new Element(NODE_VALUE).setText(String.valueOf(sampleValue)));
                sampleElem.addContent(new Element(NODE_COLOUR).setText(String.valueOf(sampleColor)));
                sampleElem.addContent(new Element(NODE_DESCRIPTION).setText(attribute.getDescription()));
                indexElem.addContent(sampleElem);
            }
            return indexElem;
        }

    }
}
