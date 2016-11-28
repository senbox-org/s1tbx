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

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.ProductData;

import java.text.DateFormat;

/**
 * Creates a generic interface to metadata specific to a band
 */
public final class AbstractMetadataBand {


    public static final String BAND_PREFIX = "Band_";


    // SAR Specific

    public static final String incidence_near = "incidence_near";
    public static final String incidence_far = "incidence_far";

    public static final String mds1_tx_rx_polar = "mds1_tx_rx_polar";
    public static final String mds2_tx_rx_polar = "mds2_tx_rx_polar";
    public static final String mds3_tx_rx_polar = "mds3_tx_rx_polar";
    public static final String mds4_tx_rx_polar = "mds4_tx_rx_polar";
    public static final String polarization = "polarization";
    public static final String polsarData = "polsar_data";
    public static final String[] polarTags = {AbstractMetadataBand.mds1_tx_rx_polar, AbstractMetadataBand.mds2_tx_rx_polar,
            AbstractMetadataBand.mds3_tx_rx_polar, AbstractMetadataBand.mds4_tx_rx_polar};
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

    public final DateFormat dateFormat = ProductData.UTC.createDateFormat("yyyy-MM-dd HH:mm:ss");

    public static final String compact_mode = "compact_mode";

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

  /*      addAbstractedAttribute(bandRoot, swath, ProductData.TYPE_ASCII, "", "Swath name");
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
*/
        return bandRoot;
    }

    public static void addBandToBandMap(final MetadataElement bandAbsRoot, final String name) {
    /*    String bandNames = bandAbsRoot.getAttributeString(band_names);
        if (!bandNames.isEmpty())
            bandNames += ' ';
        bandNames += name;
        bandAbsRoot.setAttributeString(band_names, bandNames);*/
    }

    public static MetadataElement getBandAbsMetadata(final MetadataElement absRoot, final Band band) {
        final MetadataElement[] children = absRoot.getElements();
   /*     for (MetadataElement child : children) {
            if (child.getName().startsWith(BAND_PREFIX)) {
                final String[] bandNameArray = StringUtils.stringToArray(child.getAttributeString(band_names), " ");
                for (String bandName : bandNameArray) {
                    if (bandName.equals(band.getName()))
                        return child;
                }
            }
        }*/
        return null;
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
}
