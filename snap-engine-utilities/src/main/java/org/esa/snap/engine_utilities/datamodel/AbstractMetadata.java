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
package org.esa.snap.engine_utilities.datamodel;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.util.StringUtils;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Creates a generic interface to metadata
 */
public final class AbstractMetadata {

    /**
     * If AbstractedMetadata is modified by adding new attributes then this version number needs to be incremented
     */
    private static final String METADATA_VERSION = "6.0";

    /**
     * Default no data values
     */
    public static final int NO_METADATA = 99999;
    public static final short NO_METADATA_BYTE = 0;
    public static final String NO_METADATA_STRING = "-";
    public static final ProductData.UTC NO_METADATA_UTC = new ProductData.UTC(0);

    public static final String abstracted_metadata_version = "metadata_version";
    public static final String ABSTRACT_METADATA_ROOT = "Abstracted_Metadata";
    public static final String ORIGINAL_PRODUCT_METADATA = "Original_Product_Metadata";
    public static final String BAND_PREFIX = "Band_";

    /**
     * Abstracted metadata generic to most EO products
     */
    public static final String SLAVE_METADATA_ROOT = "Slave_Metadata";
    public static final String MASTER_BANDS = "Master_bands";
    public static final String SLAVE_BANDS = "Slave_bands";

    public static final String PRODUCT = "PRODUCT";
    public static final String PRODUCT_TYPE = "PRODUCT_TYPE";
    public static final String SPH_DESCRIPTOR = "SPH_DESCRIPTOR";
    public static final String PATH = "PATH";
    public static final String MISSION = "MISSION";
    public static final String ACQUISITION_MODE = "ACQUISITION_MODE";
    public static final String antenna_pointing = "antenna_pointing";
    public static final String BEAMS = "BEAMS";
    public static final String annotation = "annotation";
    public static final String band_names = "band_names";
    public static final String SWATH = "SWATH";
    public static final String swath = "swath";
    public static final String PROC_TIME = "PROC_TIME";
    public static final String ProcessingSystemIdentifier = "Processing_system_identifier";
    public static final String CYCLE = "orbit_cycle";
    public static final String REL_ORBIT = "REL_ORBIT";
    public static final String ABS_ORBIT = "ABS_ORBIT";
    public static final String STATE_VECTOR_TIME = "STATE_VECTOR_TIME";
    public static final String VECTOR_SOURCE = "VECTOR_SOURCE";

    // SPH
    public static final String slice_num = "slice_num";
    public static final String data_take_id = "data_take_id";
    public static final String first_line_time = "first_line_time";
    public static final String last_line_time = "last_line_time";
    public static final String first_near_lat = "first_near_lat";
    public static final String first_near_long = "first_near_long";
    public static final String first_far_lat = "first_far_lat";
    public static final String first_far_long = "first_far_long";
    public static final String last_near_lat = "last_near_lat";
    public static final String last_near_long = "last_near_long";
    public static final String last_far_lat = "last_far_lat";
    public static final String last_far_long = "last_far_long";

    public static final String PASS = "PASS";
    public static final String SAMPLE_TYPE = "SAMPLE_TYPE";
    public static final String sample_type = "sample_type";

    // SAR Specific

    public static final String incidence_near = "incidence_near";
    public static final String incidence_far = "incidence_far";

    public static final String mds1_tx_rx_polar = "mds1_tx_rx_polar";
    public static final String mds2_tx_rx_polar = "mds2_tx_rx_polar";
    public static final String mds3_tx_rx_polar = "mds3_tx_rx_polar";
    public static final String mds4_tx_rx_polar = "mds4_tx_rx_polar";
    public static final String polarization = "polarization";
    public static final String polsarData = "polsar_data";
    public static final String[] polarTags = {AbstractMetadata.mds1_tx_rx_polar, AbstractMetadata.mds2_tx_rx_polar,
            AbstractMetadata.mds3_tx_rx_polar, AbstractMetadata.mds4_tx_rx_polar};
    public static final String algorithm = "algorithm";
    public static final String azimuth_looks = "azimuth_looks";
    public static final String range_looks = "range_looks";
    public static final String range_spacing = "range_spacing";
    public static final String azimuth_spacing = "azimuth_spacing";
    public static final String pulse_repetition_frequency = "pulse_repetition_frequency";
    public static final String radar_frequency = "radar_frequency";
    public static final String line_time_interval = "line_time_interval";

    public static final String TOT_SIZE = "total_size";
    public static final String num_output_lines = "num_output_lines";
    public static final String num_samples_per_line = "num_samples_per_line";

    public static final String subset_offset_x = "subset_offset_x";
    public static final String subset_offset_y = "subset_offset_y";

    // SRGR
    public static final String srgr_flag = "srgr_flag";
    public static final String map_projection = "map_projection";

    // calibration and flags
    public static final String ant_elev_corr_flag = "ant_elev_corr_flag";
    public static final String range_spread_comp_flag = "range_spread_comp_flag";
    public static final String inc_angle_comp_flag = "inc_angle_comp_flag";
    public static final String abs_calibration_flag = "abs_calibration_flag";
    public static final String calibration_factor = "calibration_factor";
    public static final String chirp_power = "chirp_power";
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
    public static final String bistatic_stack = "bistatic_stack";

    public static final String external_calibration_file = "external_calibration_file";
    public static final String orbit_state_vector_file = "orbit_state_vector_file";
    public static final String target_report_file = "target_report_file";
    public static final String wind_field_report_file = "wind_field_report_file";

    // orbit state vectors
    public static final String orbit_state_vectors = "Orbit_State_Vectors";
    public static final String orbit_vector = "orbit_vector";
    public static final String orbit_vector_time = "time";
    public static final String orbit_vector_x_pos = "x_pos";
    public static final String orbit_vector_y_pos = "y_pos";
    public static final String orbit_vector_z_pos = "z_pos";
    public static final String orbit_vector_x_vel = "x_vel";
    public static final String orbit_vector_y_vel = "y_vel";
    public static final String orbit_vector_z_vel = "z_vel";

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

    public static final String compact_mode = "compact_mode";

    /**
     * Abstract common metadata from products to be used uniformly by all operators
     *
     * @param root the product metadata root
     * @return abstracted metadata root
     */
    public static MetadataElement addAbstractedMetadataHeader(final MetadataElement root) {
        MetadataElement absRoot;
        if (root == null) {
            absRoot = new MetadataElement(ABSTRACT_METADATA_ROOT);
        } else {
            absRoot = root.getElement(ABSTRACT_METADATA_ROOT);
            if (absRoot == null) {
                absRoot = new MetadataElement(ABSTRACT_METADATA_ROOT);
                root.addElementAt(absRoot, 0);
            }
        }

        // MPH
        addAbstractedAttribute(absRoot, PRODUCT, ProductData.TYPE_ASCII, "", "Product name");
        addAbstractedAttribute(absRoot, PRODUCT_TYPE, ProductData.TYPE_ASCII, "", "Product type");
        addAbstractedAttribute(absRoot, SPH_DESCRIPTOR, ProductData.TYPE_ASCII, "", "Description");
        addAbstractedAttribute(absRoot, MISSION, ProductData.TYPE_ASCII, "", "Satellite mission");
        addAbstractedAttribute(absRoot, ACQUISITION_MODE, ProductData.TYPE_ASCII, "", "Acquisition mode");
        addAbstractedAttribute(absRoot, antenna_pointing, ProductData.TYPE_ASCII, "", "Right or left facing");
        addAbstractedAttribute(absRoot, BEAMS, ProductData.TYPE_ASCII, "", "Beams used");
        addAbstractedAttribute(absRoot, SWATH, ProductData.TYPE_ASCII, "", "Swath name");
        addAbstractedAttribute(absRoot, PROC_TIME, ProductData.TYPE_UTC, "utc", "Processed time");
        addAbstractedAttribute(absRoot, ProcessingSystemIdentifier, ProductData.TYPE_ASCII, "", "Processing system identifier");
        addAbstractedAttribute(absRoot, CYCLE, ProductData.TYPE_INT32, "", "Cycle");
        addAbstractedAttribute(absRoot, REL_ORBIT, ProductData.TYPE_INT32, "", "Track");
        addAbstractedAttribute(absRoot, ABS_ORBIT, ProductData.TYPE_INT32, "", "Orbit");
        addAbstractedAttribute(absRoot, STATE_VECTOR_TIME, ProductData.TYPE_UTC, "utc", "Time of orbit state vector");
        addAbstractedAttribute(absRoot, VECTOR_SOURCE, ProductData.TYPE_ASCII, "", "State vector source");

        addAbstractedAttribute(absRoot, incidence_near, ProductData.TYPE_FLOAT64, "deg", "");
        addAbstractedAttribute(absRoot, incidence_far, ProductData.TYPE_FLOAT64, "deg", "");

        // SPH
        addAbstractedAttribute(absRoot, slice_num, ProductData.TYPE_INT32, "", "Slice number");
        addAbstractedAttribute(absRoot, data_take_id, ProductData.TYPE_INT32, "", "Data take identifier");
        addAbstractedAttribute(absRoot, first_line_time, ProductData.TYPE_UTC, "utc", "First zero doppler azimuth time");
        addAbstractedAttribute(absRoot, last_line_time, ProductData.TYPE_UTC, "utc", "Last zero doppler azimuth time");
        addAbstractedAttribute(absRoot, first_near_lat, ProductData.TYPE_FLOAT64, "deg", "");
        addAbstractedAttribute(absRoot, first_near_long, ProductData.TYPE_FLOAT64, "deg", "");
        addAbstractedAttribute(absRoot, first_far_lat, ProductData.TYPE_FLOAT64, "deg", "");
        addAbstractedAttribute(absRoot, first_far_long, ProductData.TYPE_FLOAT64, "deg", "");
        addAbstractedAttribute(absRoot, last_near_lat, ProductData.TYPE_FLOAT64, "deg", "");
        addAbstractedAttribute(absRoot, last_near_long, ProductData.TYPE_FLOAT64, "deg", "");
        addAbstractedAttribute(absRoot, last_far_lat, ProductData.TYPE_FLOAT64, "deg", "");
        addAbstractedAttribute(absRoot, last_far_long, ProductData.TYPE_FLOAT64, "deg", "");

        addAbstractedAttribute(absRoot, PASS, ProductData.TYPE_ASCII, "", "ASCENDING or DESCENDING");
        addAbstractedAttribute(absRoot, SAMPLE_TYPE, ProductData.TYPE_ASCII, "", "DETECTED or COMPLEX");
        addAbstractedAttribute(absRoot, mds1_tx_rx_polar, ProductData.TYPE_ASCII, "", "Polarization");
        addAbstractedAttribute(absRoot, mds2_tx_rx_polar, ProductData.TYPE_ASCII, "", "Polarization");
        addAbstractedAttribute(absRoot, mds3_tx_rx_polar, ProductData.TYPE_ASCII, "", "Polarization");
        addAbstractedAttribute(absRoot, mds4_tx_rx_polar, ProductData.TYPE_ASCII, "", "Polarization");
        addAbstractedAttribute(absRoot, polsarData, ProductData.TYPE_UINT8, "flag", "Polarimetric Matrix");
        addAbstractedAttribute(absRoot, algorithm, ProductData.TYPE_ASCII, "", "Processing algorithm");
        addAbstractedAttribute(absRoot, azimuth_looks, ProductData.TYPE_FLOAT64, "", "");
        addAbstractedAttribute(absRoot, range_looks, ProductData.TYPE_FLOAT64, "", "");
        addAbstractedAttribute(absRoot, range_spacing, ProductData.TYPE_FLOAT64, "m", "Range sample spacing");
        addAbstractedAttribute(absRoot, azimuth_spacing, ProductData.TYPE_FLOAT64, "m", "Azimuth sample spacing");
        addAbstractedAttribute(absRoot, pulse_repetition_frequency, ProductData.TYPE_FLOAT64, "Hz", "PRF");
        addAbstractedAttribute(absRoot, radar_frequency, ProductData.TYPE_FLOAT64, "MHz", "Radar frequency");
        addAbstractedAttribute(absRoot, line_time_interval, ProductData.TYPE_FLOAT64, "s", "");

        addAbstractedAttribute(absRoot, TOT_SIZE, ProductData.TYPE_UINT32, "MB", "Total product size");
        addAbstractedAttribute(absRoot, num_output_lines, ProductData.TYPE_UINT32, "lines", "Raster height");
        addAbstractedAttribute(absRoot, num_samples_per_line, ProductData.TYPE_UINT32, "samples", "Raster width");

        addAbstractedAttribute(absRoot, subset_offset_x, ProductData.TYPE_UINT32, "samples", "X coordinate of UL corner of subset in original image");
        addAbstractedAttribute(absRoot, subset_offset_y, ProductData.TYPE_UINT32, "samples", "Y coordinate of UL corner of subset in original image");
        setAttribute(absRoot, subset_offset_x, 0);
        setAttribute(absRoot, subset_offset_y, 0);

        // SRGR
        addAbstractedAttribute(absRoot, srgr_flag, ProductData.TYPE_UINT8, "flag", "SRGR applied");
        MetadataAttribute att = addAbstractedAttribute(absRoot, avg_scene_height, ProductData.TYPE_FLOAT64, "m", "Average scene height ellipsoid");
        att.getData().setElemInt(0);
        addAbstractedAttribute(absRoot, map_projection, ProductData.TYPE_ASCII, "", "Map projection applied");

        // orthorectification
        addAbstractedAttribute(absRoot, is_terrain_corrected, ProductData.TYPE_UINT8, "flag", "orthorectification applied");
        addAbstractedAttribute(absRoot, DEM, ProductData.TYPE_ASCII, "", "Digital Elevation Model used");
        addAbstractedAttribute(absRoot, geo_ref_system, ProductData.TYPE_ASCII, "", "geographic reference system");
        addAbstractedAttribute(absRoot, lat_pixel_res, ProductData.TYPE_FLOAT64, "deg", "pixel resolution in geocoded image");
        addAbstractedAttribute(absRoot, lon_pixel_res, ProductData.TYPE_FLOAT64, "deg", "pixel resolution in geocoded image");
        addAbstractedAttribute(absRoot, slant_range_to_first_pixel, ProductData.TYPE_FLOAT64, "m", "Slant range to 1st data sample");

        // calibration
        addAbstractedAttribute(absRoot, ant_elev_corr_flag, ProductData.TYPE_UINT8, "flag", "Antenna elevation applied");
        addAbstractedAttribute(absRoot, range_spread_comp_flag, ProductData.TYPE_UINT8, "flag", "range spread compensation applied");
        addAbstractedAttribute(absRoot, replica_power_corr_flag, ProductData.TYPE_UINT8, "flag", "Replica pulse power correction applied");
        addAbstractedAttribute(absRoot, abs_calibration_flag, ProductData.TYPE_UINT8, "flag", "Product calibrated");
        addAbstractedAttribute(absRoot, calibration_factor, ProductData.TYPE_FLOAT64, "dB", "Calibration constant");
        addAbstractedAttribute(absRoot, chirp_power, ProductData.TYPE_FLOAT64, "", "Chirp power");
        addAbstractedAttribute(absRoot, inc_angle_comp_flag, ProductData.TYPE_UINT8, "flag", "incidence angle compensation applied");
        addAbstractedAttribute(absRoot, ref_inc_angle, ProductData.TYPE_FLOAT64, "", "Reference incidence angle");
        addAbstractedAttribute(absRoot, ref_slant_range, ProductData.TYPE_FLOAT64, "", "Reference slant range");
        addAbstractedAttribute(absRoot, ref_slant_range_exp, ProductData.TYPE_FLOAT64, "", "Reference slant range exponent");
        addAbstractedAttribute(absRoot, rescaling_factor, ProductData.TYPE_FLOAT64, "", "Rescaling factor");

        addAbstractedAttribute(absRoot, range_sampling_rate, ProductData.TYPE_FLOAT64, "MHz", "Range Sampling Rate");

        // range and azimuth bandwidths for InSAR
        addAbstractedAttribute(absRoot, range_bandwidth, ProductData.TYPE_FLOAT64, "MHz", "Bandwidth total in range");
        addAbstractedAttribute(absRoot, azimuth_bandwidth, ProductData.TYPE_FLOAT64, "Hz", "Bandwidth total in azimuth");

        // Multilook
        addAbstractedAttribute(absRoot, multilook_flag, ProductData.TYPE_UINT8, "flag", "Multilook applied");

        // coregistration
        addAbstractedAttribute(absRoot, coregistered_stack, ProductData.TYPE_UINT8, "flag", "Coregistration applied");

        addAbstractedAttribute(absRoot, external_calibration_file, ProductData.TYPE_ASCII, "", "External calibration file used");
        addAbstractedAttribute(absRoot, orbit_state_vector_file, ProductData.TYPE_ASCII, "", "Orbit file used");

        absRoot.addElement(new MetadataElement(orbit_state_vectors));
        absRoot.addElement(new MetadataElement(srgr_coefficients));
        absRoot.addElement(new MetadataElement(dop_coefficients));

        att = addAbstractedAttribute(absRoot, abstracted_metadata_version, ProductData.TYPE_ASCII, "", "AbsMetadata version");
        att.getData().setElems(METADATA_VERSION);

        return absRoot;
    }

    /**
     * Abstract common metadata from products to be used uniformly by all operators
     * name should be in the form swath_pol_date
     *
     * @param absRoot the abstracted metadata root
     * @param name    the name of the element
     * @return abstracted metadata root
     */
    public static MetadataElement addBandAbstractedMetadata(final MetadataElement absRoot, final String name) {
        MetadataElement bandRoot = absRoot.getElement(name);
        if (bandRoot == null) {
            bandRoot = new MetadataElement(name);
            absRoot.addElement(bandRoot);
        }

        addAbstractedAttribute(bandRoot, swath, ProductData.TYPE_ASCII, "", "Swath name");
        addAbstractedAttribute(bandRoot, polarization, ProductData.TYPE_ASCII, "", "Polarization");
        addAbstractedAttribute(bandRoot, annotation, ProductData.TYPE_ASCII, "", "metadata file");
        addAbstractedAttribute(bandRoot, band_names, ProductData.TYPE_ASCII, "", "corresponding bands");

        addAbstractedAttribute(bandRoot, first_line_time, ProductData.TYPE_UTC, "utc", "First zero doppler azimuth time");
        addAbstractedAttribute(bandRoot, last_line_time, ProductData.TYPE_UTC, "utc", "Last zero doppler azimuth time");
        addAbstractedAttribute(bandRoot, line_time_interval, ProductData.TYPE_FLOAT64, "s", "Time per line");

        addAbstractedAttribute(bandRoot, num_output_lines, ProductData.TYPE_UINT32, "lines", "Raster height");
        addAbstractedAttribute(bandRoot, num_samples_per_line, ProductData.TYPE_UINT32, "samples", "Raster width");
        addAbstractedAttribute(bandRoot, sample_type, ProductData.TYPE_ASCII, "", "DETECTED or COMPLEX");

        addAbstractedAttribute(bandRoot, calibration_factor, ProductData.TYPE_FLOAT64, "", "Calibration constant");

        return bandRoot;
    }

    public static void addBandToBandMap(final MetadataElement bandAbsRoot, final String name) {
        String bandNames = bandAbsRoot.getAttributeString(band_names);
        if(bandNames.equals(NO_METADATA_STRING))
            bandNames = "";
        if (!bandNames.isEmpty())
            bandNames += ' ';
        bandNames += name;
        bandAbsRoot.setAttributeString(band_names, bandNames);
    }

    public static MetadataElement getBandAbsMetadata(final MetadataElement absRoot, final Band band) {
        final MetadataElement[] children = absRoot.getElements();
        for (MetadataElement child : children) {
            if (child.getName().startsWith(BAND_PREFIX)) {
                final String[] bandNameArray = StringUtils.stringToArray(child.getAttributeString(band_names), " ");
                for (String bandName : bandNameArray) {
                    if (bandName.equals(band.getName()))
                        return child;
                }
            }
        }
        return null;
    }

    public static MetadataElement[] getBandAbsMetadataList(final MetadataElement absRoot) {
        final List<MetadataElement> bandMetadataList = new ArrayList<>();
        final MetadataElement[] children = absRoot.getElements();
        for (MetadataElement child : children) {
            if (child.getName().startsWith(BAND_PREFIX)) {
                bandMetadataList.add(child);
            }
        }
        return bandMetadataList.toArray(new MetadataElement[bandMetadataList.size()]);
    }

    /**
     * Returns the orignal product metadata or the root if not found
     *
     * @param product input product
     * @return original metadata
     */
    public static MetadataElement getOriginalProductMetadata(final Product product) {
        final MetadataElement root = product.getMetadataRoot();
        MetadataElement origMetadata = root.getElement(ORIGINAL_PRODUCT_METADATA);
        if (origMetadata == null) {
            return root;
        }
        return origMetadata;
    }

    /**
     * Creates and returns the orignal product metadata
     *
     * @param root input product metadata root
     * @return original metadata
     */
    public static MetadataElement addOriginalProductMetadata(final MetadataElement root) {
        MetadataElement origMetadata = root.getElement(ORIGINAL_PRODUCT_METADATA);
        if (origMetadata == null) {
            origMetadata = new MetadataElement(ORIGINAL_PRODUCT_METADATA);
            root.addElement(origMetadata);
        }
        return origMetadata;
    }

    public static boolean isNoData(final MetadataElement elem, final String tag) {
        final String val = elem.getAttributeString(tag, NO_METADATA_STRING).trim();
        return val.equals(NO_METADATA_STRING) || val.isEmpty();
    }

    /**
     * Adds an attribute into dest
     *
     * @param dest     the destination element
     * @param tag      the name of the attribute
     * @param dataType the ProductData type
     * @param unit     The unit
     * @param desc     The description
     * @return the newly created attribute
     */
    public static MetadataAttribute addAbstractedAttribute(final MetadataElement dest, final String tag, final int dataType,
                                                           final String unit, final String desc) {
        final MetadataAttribute attribute = new MetadataAttribute(tag, dataType, 1);
        if (dataType == ProductData.TYPE_ASCII) {
            attribute.getData().setElems(NO_METADATA_STRING);
        } else if (dataType == ProductData.TYPE_INT8 || dataType == ProductData.TYPE_UINT8) {
            attribute.getData().setElems(new String[]{String.valueOf(NO_METADATA_BYTE)});
        } else if (dataType != ProductData.TYPE_UTC) {
            attribute.getData().setElems(new String[]{String.valueOf(NO_METADATA)});
        }
        attribute.setUnit(unit);
        attribute.setDescription(desc);
        attribute.setReadOnly(false);
        dest.addAttribute(attribute);
        return attribute;
    }

    /**
     * Sets an attribute as a string
     *
     * @param dest  the destination element
     * @param tag   the name of the attribute
     * @param value the string value
     */
    public static void setAttribute(final MetadataElement dest, final String tag, final String value) {
        if (dest == null)
            return;
        MetadataAttribute attrib = dest.getAttribute(tag);
        if (attrib == null) {
            attrib = new MetadataAttribute(tag, ProductData.TYPE_ASCII);
            dest.addAttribute(attrib);
        }
        if (value == null || value.isEmpty())
            attrib.getData().setElems(NO_METADATA_STRING);
        else
            attrib.getData().setElems(value);
    }

    /**
     * Sets an attribute as a UTC
     *
     * @param dest  the destination element
     * @param tag   the name of the attribute
     * @param value the UTC value
     */
    public static void setAttribute(final MetadataElement dest, final String tag, final ProductData.UTC value) {
        if (dest == null)
            return;
        final MetadataAttribute attrib = dest.getAttribute(tag);
        if (attrib != null && value != null) {
            attrib.getData().setElems(value.getArray());
        } else {
            if (attrib == null)
                System.out.println(tag + " not found in metadata");
            if (value == null)
                System.out.println(tag + " metadata value is null");
        }
    }

    /**
     * Sets an attribute as an int
     *
     * @param dest  the destination element
     * @param tag   the name of the attribute
     * @param value the string value
     */
    public static void setAttribute(final MetadataElement dest, final String tag, final int value) {
        if (dest == null)
            return;
        final MetadataAttribute attrib = dest.getAttribute(tag);
        if (attrib == null)
            System.out.println(tag + " not found in metadata");
        else
            attrib.getData().setElemInt(value);
    }

    /**
     * Sets an attribute as a double
     *
     * @param dest  the destination element
     * @param tag   the name of the attribute
     * @param value the string value
     */
    public static void setAttribute(final MetadataElement dest, final String tag, final double value) {
        if (dest == null)
            return;
        final MetadataAttribute attrib = dest.getAttribute(tag);
        if (attrib != null) {
            attrib.getData().setElemDouble(value);
        } else {
            final MetadataAttribute newAttrib = new MetadataAttribute(tag, ProductData.TYPE_FLOAT64);
            dest.addAttribute(newAttrib);
            newAttrib.getData().setElemDouble(value);
        }
    }

    public static void setAttribute(final MetadataElement dest, final String tag, final Double value) {
        if (dest == null || value == null)
            return;
        final MetadataAttribute attrib = dest.getAttribute(tag);
        if (attrib != null) {
            attrib.getData().setElemDouble(value);
        } else {
            final MetadataAttribute newAttrib = new MetadataAttribute(tag, ProductData.TYPE_FLOAT64);
            dest.addAttribute(newAttrib);
            newAttrib.getData().setElemDouble(value);
        }
    }

    public static ProductData.UTC parseUTC(final String timeStr) {
        try {
            if (timeStr == null)
                return NO_METADATA_UTC;
            return ProductData.UTC.parse(timeStr);
        } catch (ParseException e) {
            try {
                final int dotPos = timeStr.lastIndexOf(".");
                if (dotPos > 0) {
                    String fractionString = timeStr.substring(dotPos + 1, timeStr.length());
                    //fix some ERS times
                    fractionString = fractionString.replaceAll("-", "");
                    String newTimeStr = timeStr.substring(0, dotPos) + fractionString;
                    return ProductData.UTC.parse(newTimeStr);
                }
            } catch (ParseException e2) {
                return NO_METADATA_UTC;
            }
        }
        return NO_METADATA_UTC;
    }

    public static ProductData.UTC parseUTC(final String timeStr, final DateFormat format) {
        try {
            final int dotPos = timeStr.lastIndexOf('.');
            if (dotPos > 0) {
                final String newTimeStr = timeStr.substring(0, Math.min(dotPos + 7, timeStr.length()));
                try {
                    return ProductData.UTC.parse(newTimeStr, format);
                } catch (Throwable e) {
                    ProductData.UTC time = ProductData.UTC.parse(newTimeStr, format);
                    return time;
                }
            }
            return ProductData.UTC.parse(timeStr, format);
        } catch (Throwable e) {
            System.out.println("UTC parse error:"+ timeStr +":"+ e.toString());
            return NO_METADATA_UTC;
        }
    }

    public static boolean getAttributeBoolean(final MetadataElement elem, final String tag) throws Exception {
        final int val = elem.getAttributeInt(tag);
        if (val == NO_METADATA)
            throw new Exception("Metadata " + tag + " has not been set");
        return val != 0;
    }

    public static double getAttributeDouble(final MetadataElement elem, final String tag) throws Exception {
        final double val = elem.getAttributeDouble(tag);
        if (val == NO_METADATA)
            throw new Exception("Metadata " + tag + " has not been set");
        return val;
    }

    public static int getAttributeInt(final MetadataElement elem, final String tag) throws Exception {
        final int val = elem.getAttributeInt(tag);
        if (val == NO_METADATA)
            throw new Exception("Metadata " + tag + " has not been set");
        return val;
    }

    /**
     * Check if abstracted metadata exists.
     *
     * @param sourceProduct the product
     * @return true if abstractmetadata exists
     */
    public static boolean hasAbstractedMetadata(final Product sourceProduct) {

        final MetadataElement root = sourceProduct.getMetadataRoot();
        if (root == null) {
            return false;
        }
        MetadataElement abstractedMetadata = root.getElement(AbstractMetadata.ABSTRACT_METADATA_ROOT);
        return (abstractedMetadata != null);
    }

    /**
     * Get abstracted metadata.
     *
     * @param sourceProduct the product
     * @return MetadataElement or null if no root found
     */
    public static MetadataElement getAbstractedMetadata(final Product sourceProduct) {

        final MetadataElement root = sourceProduct.getMetadataRoot();
        if (root == null) {
            return null;
        }
        MetadataElement abstractedMetadata = root.getElement(AbstractMetadata.ABSTRACT_METADATA_ROOT);
        if (abstractedMetadata == null) {
            abstractedMetadata = root.getElement("Abstracted Metadata"); // legacy
            if (abstractedMetadata == null) {
                abstractedMetadata = addAbstractedMetadataHeader(root);
                defaultToProduct(abstractedMetadata, sourceProduct);
            }
        }
        migrateToCurrentVersion(abstractedMetadata);
        patchMissingMetadata(abstractedMetadata);

        return abstractedMetadata;
    }

    private static void defaultToProduct(final MetadataElement abstractedMetadata, final Product product) {
        setAttribute(abstractedMetadata, PRODUCT, product.getName());
        setAttribute(abstractedMetadata, PRODUCT_TYPE, product.getProductType());
        setAttribute(abstractedMetadata, SPH_DESCRIPTOR, product.getDescription());

        setAttribute(abstractedMetadata, num_output_lines, product.getSceneRasterHeight());
        setAttribute(abstractedMetadata, num_samples_per_line, product.getSceneRasterWidth());

        setAttribute(abstractedMetadata, first_line_time, product.getStartTime());
        setAttribute(abstractedMetadata, last_line_time, product.getEndTime());

        if(product.getProductReader() != null && product.getProductReader().getReaderPlugIn() != null) {
            setAttribute(abstractedMetadata, MISSION, product.getProductReader().getReaderPlugIn().getFormatNames()[0]);
        }
    }

    private static void migrateToCurrentVersion(final MetadataElement abstractedMetadata) {
        // check if version has changed
        final String version = abstractedMetadata.getAttributeString(abstracted_metadata_version, "");
        if (version.equals(METADATA_VERSION))
            return;

        //todo
    }

    private static void patchMissingMetadata(final MetadataElement abstractedMetadata) {
        // check if version has changed
        final String version = abstractedMetadata.getAttributeString(abstracted_metadata_version, "");
        if (version.equals(METADATA_VERSION))
            return;

        final MetadataElement tmpElem = new MetadataElement("tmp");
        final MetadataElement completeMetadata = addAbstractedMetadataHeader(tmpElem);

        final MetadataAttribute[] attribs = completeMetadata.getAttributes();
        for (MetadataAttribute at : attribs) {
            if (!abstractedMetadata.containsAttribute(at.getName())) {
                abstractedMetadata.addAttribute(at);
                abstractedMetadata.getProduct().setModified(false);
            }
        }
    }

    public static MetadataElement getSlaveMetadata(final MetadataElement targetRoot) {
        MetadataElement targetSlaveMetadataRoot = targetRoot.getElement(AbstractMetadata.SLAVE_METADATA_ROOT);
        if (targetSlaveMetadataRoot == null) {
            targetSlaveMetadataRoot = new MetadataElement(AbstractMetadata.SLAVE_METADATA_ROOT);
            targetRoot.addElement(targetSlaveMetadataRoot);
        }
        return targetSlaveMetadataRoot;
    }

    /**
     * Band metadata element within AbstractedMetadata
     *
     * @param root     abstracted metadata root
     * @param bandName the name of the band
     * @param create   if null
     * @return MetadataElement of band
     */
    @Deprecated
    public static MetadataElement getBandAbsMetadata(final MetadataElement root, final String bandName,
                                                     final boolean create) {
        final String bandElemName = "Band_" + bandName;
        MetadataElement bandElem = root.getElement(bandElemName);
        if (bandElem == null) {
            // check real band
            if (bandName.startsWith("Intensity")) {
                String realBandName = bandName.replace("Intensity_", "i_");
                bandElem = root.getElement("Band_" + realBandName);
            } else if (bandName.startsWith("Phase")) {
                String realBandName = bandName.replace("Phase_", "i_");
                bandElem = root.getElement("Band_" + realBandName);
            }
            if (bandElem == null && create) {
                bandElem = new MetadataElement(bandElemName);
                root.addElement(bandElem);
            }
        }
        return bandElem;
    }

    /**
     * Get orbit state vectors.
     *
     * @param absRoot Abstracted metadata root.
     * @return orbitStateVectors Array of orbit state vectors.
     */
    public static OrbitStateVector[] getOrbitStateVectors(final MetadataElement absRoot) {

        final MetadataElement elemRoot = absRoot.getElement(orbit_state_vectors);
        if (elemRoot == null) {
            return new OrbitStateVector[]{};
        }
        final int numElems = elemRoot.getNumElements();
        final OrbitStateVector[] orbitStateVectors = new OrbitStateVector[numElems];
        for (int i = 0; i < numElems; i++) {

            final MetadataElement subElemRoot = elemRoot.getElement(orbit_vector + (i + 1));
            final OrbitStateVector vector = new OrbitStateVector(
                    subElemRoot.getAttributeUTC(orbit_vector_time),
                    subElemRoot.getAttributeDouble(orbit_vector_x_pos),
                    subElemRoot.getAttributeDouble(orbit_vector_y_pos),
                    subElemRoot.getAttributeDouble(orbit_vector_z_pos),
                    subElemRoot.getAttributeDouble(orbit_vector_x_vel),
                    subElemRoot.getAttributeDouble(orbit_vector_y_vel),
                    subElemRoot.getAttributeDouble(orbit_vector_z_vel));
            orbitStateVectors[i] = vector;
        }
        return orbitStateVectors;
    }

    /**
     * Set orbit state vectors.
     *
     * @param absRoot           Abstracted metadata root.
     * @param orbitStateVectors The orbit state vectors.
     * @throws Exception if orbit state vector length is not correct
     */
    public static void setOrbitStateVectors(final MetadataElement absRoot, final OrbitStateVector[] orbitStateVectors) throws Exception {

        final MetadataElement elemRoot = absRoot.getElement(orbit_state_vectors);

        //remove old
        final MetadataElement[] oldList = elemRoot.getElements();
        for (MetadataElement old : oldList) {
            elemRoot.removeElement(old);
        }
        //add new
        int i = 1;
        for (OrbitStateVector vector : orbitStateVectors) {
            final MetadataElement subElemRoot = new MetadataElement(orbit_vector + i);
            elemRoot.addElement(subElemRoot);
            ++i;

            subElemRoot.setAttributeUTC(orbit_vector_time, vector.time);
            subElemRoot.setAttributeDouble(orbit_vector_x_pos, vector.x_pos);
            subElemRoot.setAttributeDouble(orbit_vector_y_pos, vector.y_pos);
            subElemRoot.setAttributeDouble(orbit_vector_z_pos, vector.z_pos);
            subElemRoot.setAttributeDouble(orbit_vector_x_vel, vector.x_vel);
            subElemRoot.setAttributeDouble(orbit_vector_y_vel, vector.y_vel);
            subElemRoot.setAttributeDouble(orbit_vector_z_vel, vector.z_vel);
        }
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
     * Set SRGR Coefficients.
     *
     * @param absRoot Abstracted metadata root.
     */
    public static void setSRGRCoefficients(final MetadataElement absRoot, final SRGRCoefficientList[] srgrCoefList) {

        final MetadataElement elemRoot = absRoot.getElement(srgr_coefficients);

        //remove old
        final MetadataElement[] oldList = elemRoot.getElements();
        for (MetadataElement old : oldList) {
            elemRoot.removeElement(old);
        }

        //add new
        int listCnt = 1;
        for (SRGRCoefficientList srgrCoef : srgrCoefList) {
            final MetadataElement srgrListElem = new MetadataElement(srgr_coef_list + '.' + listCnt);
            elemRoot.addElement(srgrListElem);
            ++listCnt;

            srgrListElem.setAttributeUTC(AbstractMetadata.srgr_coef_time, srgrCoef.time);
            AbstractMetadata.setAttribute(srgrListElem, AbstractMetadata.ground_range_origin, srgrCoef.ground_range_origin);

            int cnt = 1;
            for (double val : srgrCoef.coefficients) {
                final MetadataElement coefElem = new MetadataElement(AbstractMetadata.coefficient + '.' + cnt);
                srgrListElem.addElement(coefElem);
                ++cnt;
                AbstractMetadata.addAbstractedAttribute(coefElem, AbstractMetadata.srgr_coef,
                        ProductData.TYPE_FLOAT64, "", "SRGR Coefficient");
                AbstractMetadata.setAttribute(coefElem, AbstractMetadata.srgr_coef, val);

            }
        }
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

    public static void setDopplerCentroidCoefficients(final MetadataElement absRoot, final DopplerCentroidCoefficientList[] dopList) {

        final MetadataElement elemRoot = absRoot.getElement(AbstractMetadata.dop_coefficients);

        //remove old
        final MetadataElement[] oldList = elemRoot.getElements();
        for (MetadataElement old : oldList) {
            elemRoot.removeElement(old);
        }

        //add new
        int listCnt = 1;
        for (DopplerCentroidCoefficientList dop : dopList) {
            final MetadataElement dopplerListElem = new MetadataElement(AbstractMetadata.dop_coef_list + '.' + listCnt);
            elemRoot.addElement(dopplerListElem);
            ++listCnt;

            dopplerListElem.setAttributeUTC(AbstractMetadata.dop_coef_time, dop.time);

            AbstractMetadata.addAbstractedAttribute(dopplerListElem, AbstractMetadata.slant_range_time,
                    ProductData.TYPE_FLOAT64, "ns", "Slant Range Time");
            AbstractMetadata.setAttribute(dopplerListElem, AbstractMetadata.slant_range_time, dop.slant_range_time);

            final double[] coef = dop.coefficients;
            int cnt = 1;
            for (double coefValue : coef) {
                final MetadataElement coefElem = new MetadataElement(AbstractMetadata.coefficient + '.' + cnt);
                dopplerListElem.addElement(coefElem);
                ++cnt;
                AbstractMetadata.addAbstractedAttribute(coefElem, AbstractMetadata.dop_coef,
                        ProductData.TYPE_FLOAT64, "", "Doppler Centroid Coefficient");
                AbstractMetadata.setAttribute(coefElem, AbstractMetadata.dop_coef, coefValue);
            }
        }
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
