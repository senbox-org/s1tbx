/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.snap.engine_utilities.datamodel.metadata;

import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;

import java.io.IOException;
import java.text.DateFormat;

/**
 * Creates a generic interface to SAR metadata
 */
public final class AbstractMetadataSAR extends AbstractMetadataBase implements AbstractMetadataInterface {

    /**
     * If AbstractedMetadata is modified by adding new attributes then this version number needs to be incremented
     */
    private static final String METADATA_VERSION = "6.0";
    private static final String abstracted_metadata_version = "sar_metadata_version";
    private static final String SAR_METADATA_ROOT = "SAR_Metadata";

    /**
     * SAR Specific Metadata
     */

    public static final String antenna_pointing = "antenna_pointing";
    public static final String incidence_near = "incidence_near";
    public static final String incidence_far = "incidence_far";

    public static final String mds1_tx_rx_polar = "mds1_tx_rx_polar";
    public static final String mds2_tx_rx_polar = "mds2_tx_rx_polar";
    public static final String mds3_tx_rx_polar = "mds3_tx_rx_polar";
    public static final String mds4_tx_rx_polar = "mds4_tx_rx_polar";
    public static final String polarization = "polarization";
    public static final String polsarData = "polsar_data";
    public static final String[] polarTags = {AbstractMetadataSAR.mds1_tx_rx_polar, AbstractMetadataSAR.mds2_tx_rx_polar,
            AbstractMetadataSAR.mds3_tx_rx_polar, AbstractMetadataSAR.mds4_tx_rx_polar};
    public static final String azimuth_looks = "azimuth_looks";
    public static final String range_looks = "range_looks";
    public static final String range_spacing = "range_spacing";
    public static final String azimuth_spacing = "azimuth_spacing";
    public static final String pulse_repetition_frequency = "pulse_repetition_frequency";
    public static final String radar_frequency = "radar_frequency";

    // SRGR
    public static final String srgr_flag = "srgr_flag";

    // calibration and flags
    public static final String ant_elev_corr_flag = "ant_elev_corr_flag";
    public static final String range_spread_comp_flag = "range_spread_comp_flag";
    public static final String inc_angle_comp_flag = "inc_angle_comp_flag";
    public static final String abs_calibration_flag = "abs_calibration_flag";
    public static final String calibration_factor = "calibration_factor";
    public static final String replica_power_corr_flag = "replica_power_corr_flag";
    public static final String range_sampling_rate = "range_sampling_rate";
    public static final String avg_scene_height = "avg_scene_height";
    public static final String multilook_flag = "multilook_flag";
    // cosmo calibration
    public static final String ref_inc_angle = "ref_inc_angle";
    public static final String ref_slant_range = "ref_slant_range";
    public static final String ref_slant_range_exp = "ref_slant_range_exp";
    public static final String rescaling_factor = "rescaling_factor";

    public static final String coregistered_stack = "coregistered_stack";

    public static final String external_calibration_file = "external_calibration_file";
    public static final String orbit_state_vector_file = "orbit_state_vector_file";
    public static final String target_report_file = "target_report_file";
    public static final String wind_field_report_file = "wind_field_report_file";

    // SRGR Coefficients
    public static final String srgr_coefficients = "SRGR_Coefficients";
    public static final String srgr_coef_list = "srgr_coef_list";
    public static final String srgr_coef_time = "zero_doppler_time";
    public static final String ground_range_origin = "ground_range_origin";
    public static final String coefficient = "coefficient";
    public static final String srgr_coef = "srgr_coef";

    // Doppler Centroid Coefficients
    public static final String dop_coefficients = "Doppler_Centroid_Coefficients";
    public static final String dop_coef_list = "dop_coef_list";
    public static final String dop_coef_time = "zero_doppler_time";
    public static final String slant_range_time = "slant_range_time";
    public static final String dop_coef = "dop_coef";

    // orthorectification
    public static final String is_terrain_corrected = "is_terrain_corrected";
    public static final String DEM = "DEM";
    public static final String geo_ref_system = "geo_ref_system";
    public static final String lat_pixel_res = "lat_pixel_res";
    public static final String lon_pixel_res = "lon_pixel_res";
    public static final String slant_range_to_first_pixel = "slant_range_to_first_pixel";

    // bandwidths for insar
    public static final String range_bandwidth = "range_bandwidth";
    public static final String azimuth_bandwidth = "azimuth_bandwidth";

    public final DateFormat dateFormat = ProductData.UTC.createDateFormat("yyyy-MM-dd HH:mm:ss");

    public static final String compact_mode = "compact_mode";

    /**
     * Get abstracted metadata.
     *
     * @param sourceProduct the product
     * @return AbstractMetadata object
     */
    public static AbstractMetadataSAR getSARAbstractedMetadata(final Product sourceProduct) throws IOException {
        AbstractMetadata abstractMetadata = AbstractMetadata.getAbstractedMetadata(sourceProduct);
        if (abstractMetadata == null) {
            throw new IOException("no metadata found in product");
        }
        MetadataElement absRoot = abstractMetadata.getAbsRoot();
        return new AbstractMetadataSAR(absRoot, absRoot.getElement(AbstractMetadataSAR.SAR_METADATA_ROOT));
    }

    private AbstractMetadataSAR(final MetadataElement root, final MetadataElement abstractedMetadata) {
        super(root, abstractedMetadata);
    }

    protected boolean isCurrentVersion() {
        // check if version has changed
        final String version = absRoot.getAttributeString(abstracted_metadata_version, "");
        return (version.equals(METADATA_VERSION));
    }

    protected void migrateToCurrentVersion(final MetadataElement abstractedMetadata) {
        if (isCurrentVersion())
            return;

        //todo
    }

    /**
     * Abstract common metadata from products to be used uniformly by all operators
     *
     * @param root the product metadata root
     * @return abstracted metadata root
     */
    protected MetadataElement addAbstractedMetadataHeader(MetadataElement root) {
        MetadataElement sarRoot;
        if (root == null) {
            sarRoot = new MetadataElement(SAR_METADATA_ROOT);
        } else {
            sarRoot = root.getElement(SAR_METADATA_ROOT);
            if (sarRoot == null) {
                sarRoot = new MetadataElement(SAR_METADATA_ROOT);
                root.addElementAt(sarRoot, 0);
            }
        }

        addAbstractedAttribute(sarRoot, antenna_pointing, ProductData.TYPE_ASCII, "", "Right or left facing");
        addAbstractedAttribute(sarRoot, incidence_near, ProductData.TYPE_FLOAT64, "deg", "");
        addAbstractedAttribute(sarRoot, incidence_far, ProductData.TYPE_FLOAT64, "deg", "");

        addAbstractedAttribute(sarRoot, mds1_tx_rx_polar, ProductData.TYPE_ASCII, "", "Polarization");
        addAbstractedAttribute(sarRoot, mds2_tx_rx_polar, ProductData.TYPE_ASCII, "", "Polarization");
        addAbstractedAttribute(sarRoot, mds3_tx_rx_polar, ProductData.TYPE_ASCII, "", "Polarization");
        addAbstractedAttribute(sarRoot, mds4_tx_rx_polar, ProductData.TYPE_ASCII, "", "Polarization");
        addAbstractedAttribute(sarRoot, azimuth_looks, ProductData.TYPE_FLOAT64, "", "");
        addAbstractedAttribute(sarRoot, range_looks, ProductData.TYPE_FLOAT64, "", "");
        addAbstractedAttribute(sarRoot, range_spacing, ProductData.TYPE_FLOAT64, "m", "Range sample spacing");
        addAbstractedAttribute(sarRoot, azimuth_spacing, ProductData.TYPE_FLOAT64, "m", "Azimuth sample spacing");
        addAbstractedAttribute(sarRoot, pulse_repetition_frequency, ProductData.TYPE_FLOAT64, "Hz", "PRF");
        addAbstractedAttribute(sarRoot, radar_frequency, ProductData.TYPE_FLOAT64, "MHz", "Radar frequency");

        // range and azimuth bandwidths for InSAR
        addAbstractedAttribute(sarRoot, range_bandwidth, ProductData.TYPE_FLOAT64, "MHz", "Bandwidth total in range");
        addAbstractedAttribute(sarRoot, azimuth_bandwidth, ProductData.TYPE_FLOAT64, "Hz", "Bandwidth total in azimuth");

        // SRGR
        addAbstractedAttribute(sarRoot, srgr_flag, ProductData.TYPE_UINT8, "flag", "SRGR applied");
        MetadataAttribute att = addAbstractedAttribute(sarRoot, avg_scene_height, ProductData.TYPE_FLOAT64, "m", "Average scene height ellipsoid");
        att.getData().setElemInt(0);

        // orthorectification
        addAbstractedAttribute(sarRoot, is_terrain_corrected, ProductData.TYPE_UINT8, "flag", "orthorectification applied");
        addAbstractedAttribute(sarRoot, DEM, ProductData.TYPE_ASCII, "", "Digital Elevation Model used");
        addAbstractedAttribute(sarRoot, geo_ref_system, ProductData.TYPE_ASCII, "", "geographic reference system");
        addAbstractedAttribute(sarRoot, lat_pixel_res, ProductData.TYPE_FLOAT64, "deg", "pixel resolution in geocoded image");
        addAbstractedAttribute(sarRoot, lon_pixel_res, ProductData.TYPE_FLOAT64, "deg", "pixel resolution in geocoded image");
        addAbstractedAttribute(sarRoot, slant_range_to_first_pixel, ProductData.TYPE_FLOAT64, "m", "Slant range to 1st data sample");

        // calibration
        addAbstractedAttribute(sarRoot, ant_elev_corr_flag, ProductData.TYPE_UINT8, "flag", "Antenna elevation applied");
        addAbstractedAttribute(sarRoot, range_spread_comp_flag, ProductData.TYPE_UINT8, "flag", "range spread compensation applied");
        addAbstractedAttribute(sarRoot, replica_power_corr_flag, ProductData.TYPE_UINT8, "flag", "Replica pulse power correction applied");
        addAbstractedAttribute(sarRoot, abs_calibration_flag, ProductData.TYPE_UINT8, "flag", "Product calibrated");
        addAbstractedAttribute(sarRoot, calibration_factor, ProductData.TYPE_FLOAT64, "", "Calibration constant");
        addAbstractedAttribute(sarRoot, inc_angle_comp_flag, ProductData.TYPE_UINT8, "flag", "incidence angle compensation applied");
        addAbstractedAttribute(sarRoot, ref_inc_angle, ProductData.TYPE_FLOAT64, "", "Reference incidence angle");
        addAbstractedAttribute(sarRoot, ref_slant_range, ProductData.TYPE_FLOAT64, "", "Reference slant range");
        addAbstractedAttribute(sarRoot, ref_slant_range_exp, ProductData.TYPE_FLOAT64, "", "Reference slant range exponent");
        addAbstractedAttribute(sarRoot, rescaling_factor, ProductData.TYPE_FLOAT64, "", "Rescaling factor");

        addAbstractedAttribute(sarRoot, range_sampling_rate, ProductData.TYPE_FLOAT64, "MHz", "Range Sampling Rate");

        // flags
        addAbstractedAttribute(sarRoot, polsarData, ProductData.TYPE_UINT8, "flag", "Polarimetric Matrix");

        // Multilook
        addAbstractedAttribute(sarRoot, multilook_flag, ProductData.TYPE_UINT8, "flag", "Multilook applied");

        // coregistration
        addAbstractedAttribute(sarRoot, coregistered_stack, ProductData.TYPE_UINT8, "flag", "Coregistration applied");

        addAbstractedAttribute(sarRoot, external_calibration_file, ProductData.TYPE_ASCII, "", "External calibration file used");
        addAbstractedAttribute(sarRoot, orbit_state_vector_file, ProductData.TYPE_ASCII, "", "Orbit file used");

        sarRoot.addElement(new MetadataElement(srgr_coefficients));
        sarRoot.addElement(new MetadataElement(dop_coefficients));

        att = addAbstractedAttribute(sarRoot, abstracted_metadata_version, ProductData.TYPE_ASCII, "", "AbsMetadata version");
        att.getData().setElems(METADATA_VERSION);

        return sarRoot;
    }

    /**
     * Get SRGR Coefficients.
     *
     * @param absRoot Abstracted metadata root.
     * @return Array of SRGR coefficient data sets.
     */
    public static SRGRCoefficientList[] getSRGRCoefficients(final MetadataElement absRoot) {

        final MetadataElement elemRoot = absRoot.getElement(srgr_coefficients);
        final MetadataElement[] srgr_coef_listElem = elemRoot.getElements();
        final SRGRCoefficientList[] srgrCoefficientList = new SRGRCoefficientList[srgr_coef_listElem.length];
        int k = 0;
        for (MetadataElement listElem : srgr_coef_listElem) {
            final SRGRCoefficientList srgrList = new SRGRCoefficientList();
            srgrList.time = listElem.getAttributeUTC(srgr_coef_time);
            srgrList.timeMJD = srgrList.time.getMJD();
            srgrList.ground_range_origin = listElem.getAttributeDouble(ground_range_origin);

            final int numSubElems = listElem.getNumElements();
            srgrList.coefficients = new double[numSubElems];
            for (int i = 0; i < numSubElems; ++i) {
                final MetadataElement coefElem = listElem.getElementAt(i);
                srgrList.coefficients[i] = coefElem.getAttributeDouble(srgr_coef, 0.0);
            }
            srgrCoefficientList[k++] = srgrList;
        }
        return srgrCoefficientList;
    }

    /**
     * Get Doppler Centroid Coefficients.
     *
     * @param absRoot Abstracted metadata root.
     * @return Array of Doppler centroid coefficient data sets.
     */
    public static DopplerCentroidCoefficientList[] getDopplerCentroidCoefficients(final MetadataElement absRoot) {

        final MetadataElement elemRoot = absRoot.getElement(dop_coefficients);
        final MetadataElement[] dop_coef_listElem = elemRoot.getElements();
        final DopplerCentroidCoefficientList[] dopCoefficientList = new DopplerCentroidCoefficientList[dop_coef_listElem.length];
        int k = 0;
        for (MetadataElement listElem : dop_coef_listElem) {
            final DopplerCentroidCoefficientList dopList = new DopplerCentroidCoefficientList();
            dopList.time = listElem.getAttributeUTC(srgr_coef_time);
            dopList.timeMJD = dopList.time.getMJD();
            dopList.slant_range_time = listElem.getAttributeDouble(slant_range_time, 0.0);

            final int numSubElems = listElem.getNumElements();
            dopList.coefficients = new double[numSubElems];
            for (int i = 0; i < numSubElems; ++i) {
                final MetadataElement coefElem = listElem.getElementAt(i);
                dopList.coefficients[i] = coefElem.getAttributeDouble(dop_coef, 0.0);
            }
            dopCoefficientList[k++] = dopList;
        }
        return dopCoefficientList;
    }

    public static class SRGRCoefficientList {
        public ProductData.UTC time = null;
        public double timeMJD = 0;
        public double ground_range_origin = 0.0;
        public double[] coefficients = null;
    }

    public static class DopplerCentroidCoefficientList {
        public ProductData.UTC time = null;
        public double timeMJD = 0;
        public double slant_range_time = 0.0;
        public double[] coefficients = null;
    }
}
