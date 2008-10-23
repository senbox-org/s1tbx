package org.esa.beam.dataio.envisat;

import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.ProductData;

import java.io.File;
import java.text.ParseException;

/**
  Abstract common metadata from products to be used uniformly by all operators
 */
public final class AsarAbstractMetadata {

    String _productType;
    String _version;
    File _file;

    AsarAbstractMetadata(String type, String ver, File file) {
        _productType = type;
        _version = ver;
        _file = file;
    }

    /**
     * Abstract common metadata from products to be used uniformly by all operators
     * @param root the product metadata root
     */
    void addAbstractedMetadataHeader(MetadataElement root) {
        MetadataElement absRoot = root.getElement("Abstracted Metadata");
        if(absRoot == null) return;

        MetadataElement mph = root.getElement("MPH");
        MetadataElement sph = root.getElement("SPH");

        MetadataElement mppAds = root.getElement("MAIN_PROCESSING_PARAMS_ADS");
        if(mppAds != null) {
            MetadataElement ads = mppAds.getElement("MAIN_PROCESSING_PARAMS_ADS.1");
            if(ads != null)
                mppAds = ads;
        }

        // MPH
        addAbstractedAttribute(mph, "PRODUCT", absRoot);
        addAbstractedAttributeString("PRODUCT_TYPE", _productType, absRoot);
        addAbstractedAttribute(sph, "SPH_DESCRIPTOR", absRoot);
        addAbstractedAttributeString("MISSION", getMission(), absRoot);
        addAbstractedAttribute("PROC_TIME", parseUTC(mph, "PROC_TIME"), absRoot);
        addAbstractedAttributeString("Processing system identifier", mph.getAttributeString("SOFTWARE_VER", ""), absRoot);
        addAbstractedAttribute(mph, "CYCLE", absRoot);
        addAbstractedAttribute(mph, "REL_ORBIT", absRoot);
        addAbstractedAttribute(mph, "ABS_ORBIT", absRoot);
        addAbstractedAttribute("STATE_VECTOR_TIME", parseUTC(mph, "STATE_VECTOR_TIME"), absRoot);
        addAbstractedAttribute(mph, "VECTOR_SOURCE", absRoot);
        addAbstractedAttribute(mph, "TOT_SIZE", absRoot);

        // SPH
        addAbstractedAttribute("first_line_time", parseUTC(sph, "first_line_time"), absRoot);
        addAbstractedAttribute("last_line_time", parseUTC(sph, "last_line_time"), absRoot);
        addAbstractedAttribute(sph, "first_near_lat", absRoot);
        addAbstractedAttribute(sph, "first_near_long", absRoot);
        addAbstractedAttribute(sph, "first_mid_lat", absRoot);
        addAbstractedAttribute(sph, "first_mid_long", absRoot);
        addAbstractedAttribute(sph, "first_far_lat", absRoot);
        addAbstractedAttribute(sph, "first_far_long", absRoot);
        addAbstractedAttribute(sph, "last_near_lat", absRoot);
        addAbstractedAttribute(sph, "last_near_long", absRoot);
        addAbstractedAttribute(sph, "last_mid_lat", absRoot);
        addAbstractedAttribute(sph, "last_mid_long", absRoot);
        addAbstractedAttribute(sph, "last_far_lat", absRoot);
        addAbstractedAttribute(sph, "last_far_long", absRoot);
        addAbstractedAttribute(sph, "SWATH", absRoot);
        addAbstractedAttribute(sph, "PASS", absRoot);
        addAbstractedAttribute(sph, "SAMPLE_TYPE", absRoot);

        String mds1_tx_rx_polar = getAttributeString(sph, "mds1_tx_rx_polar");
        mds1_tx_rx_polar = mds1_tx_rx_polar.replace("/","");
        addAbstractedAttributeString("mds1_tx_rx_polar", mds1_tx_rx_polar, absRoot);
        String mds2_tx_rx_polar = getAttributeString(sph, "mds2_tx_rx_polar");
        mds2_tx_rx_polar = mds2_tx_rx_polar.replace("/","");
        addAbstractedAttributeString("mds2_tx_rx_polar", mds2_tx_rx_polar, absRoot);

        addAbstractedAttribute(sph, "ALGORITHM", absRoot);
        addAbstractedAttribute(sph, "azimuth_looks", absRoot);
        addAbstractedAttribute(sph, "range_looks", absRoot);
        addAbstractedAttribute(sph, "range_spacing", absRoot);
        addAbstractedAttribute(sph, "azimuth_spacing", absRoot);

        if(mppAds != null) {
            addAbstractedAttributeDouble("pulse_repetition_frequency", getPulseRepetitionFreq(mppAds), absRoot);
        }
        addAbstractedAttribute(sph, "line_time_interval", absRoot);
        addAbstractedAttribute(sph, "data_type", absRoot);

        //MPP
        if(mppAds != null) {
            addAbstractedAttribute(mppAds, "num_output_lines", absRoot);
            addAbstractedAttribute(mppAds, "num_samples_per_line", absRoot);
            addAbstractedAttribute(mppAds, "srgr_flag", absRoot);
            addAbstractedAttribute(mppAds, "ant_elev_corr_flag", absRoot);
            addAbstractedAttribute(mppAds, "range_spread_comp_flag", absRoot);
        }
    }
    
    private String getMission() {
        if(_productType.startsWith("SAR")) {
            if(_file.toString().endsWith("E2"))
                return "ERS-2";
            else
                return "ERS-1";
        }
        return "ENVISAT";
    }

    /**
     * Adds an attribute from src to dest
     * @param tag the name of the attribute
     * @param value the string value
     * @param dest the destination element
     */
    private static void addAbstractedAttributeString(String tag, String value, MetadataElement dest) {
        MetadataAttribute attribute = new MetadataAttribute(tag, ProductData.TYPE_ASCII, 1);
        attribute.getData().setElems(value);
        dest.addAttributeFast(attribute);
    }

    /**
     * Adds an attribute from src to dest
     * @param tag the name of the attribute
     * @param value the UTC value
     * @param dest the destination element
     */
    private static void addAbstractedAttribute(String tag, ProductData.UTC value, MetadataElement dest) {
        MetadataAttribute attribute = new MetadataAttribute(tag, ProductData.TYPE_UTC, 1);
        attribute.getData().setElems(value.getArray());
        dest.addAttributeFast(attribute);
    }

    /**
     * Adds an attribute from src to dest
     * @param tag the name of the attribute
     * @param values the double value
     * @param dest the destination element
     */
    private static void addAbstractedAttributeDouble(String tag, double[] values, MetadataElement dest) {
        MetadataAttribute attribute = new MetadataAttribute(tag, ProductData.TYPE_FLOAT64, 1);
        attribute.getData().setElems(values);
        dest.addAttributeFast(attribute);
    }

    /**
     * Adds an attribute from src to dest
     * @param src the source element
     * @param tag the name of the attribute
     * @param dest the destination element
     */
    private static void addAbstractedAttribute(MetadataElement src, String tag, MetadataElement dest) {
        MetadataAttribute attrib = src.getAttribute(tag);
        if(attrib != null) {
            MetadataAttribute copiedAttrib = attrib.createDeepClone();
            copiedAttrib.setReadOnly(false);
            dest.addAttributeFast(copiedAttrib);
        }
    }

    private double[] getPulseRepetitionFreq(MetadataElement mppAds) {
        double[] values = {0.0};
        MetadataAttribute attribute = mppAds.getAttribute("ASAR_Main_ADSR.sd/image_parameters"+_version+".prf_value");
        if(attribute != null)
            values[0] = attribute.getData().getElemDouble();
        return values;
    }

    /**
     * Gets an attribute from src
     * @param src the source element
     * @param tag the name of the attribute
     * @return a string of the value
     */
    private static String getAttributeString(MetadataElement src, String tag) {
        String str = "";
        MetadataAttribute attrib = src.getAttribute(tag);
        if(attrib != null) {
            str = attrib.getData().getElemString();
        }
        return str;
    }

    private static ProductData.UTC parseUTC(MetadataElement src, String tag) {
        try {
            return ProductData.UTC.parse(getAttributeString(src, tag));
        } catch(ParseException e) {
            return new ProductData.UTC(0);
        }
    }
}
