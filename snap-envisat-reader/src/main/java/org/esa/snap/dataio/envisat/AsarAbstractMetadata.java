/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.dataio.envisat;

import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;

import java.io.File;
import java.util.ArrayList;

/**
 * Abstract common metadata from products to be used uniformly by all operators
 */
public final class AsarAbstractMetadata {

    public static final String ABSTRACTED_METADATA_ROOT_NAME = "Abstracted_Metadata";

    /**
     * If AbstractedMetadata is modified by adding new attributes then this version number needs to be incremented
     */
    private static final String METADATA_VERSION = "5.0";

    private final String _productType;
    private final String _version;
    private final File _file;

    AsarAbstractMetadata(String type, String ver, File file) {
        _productType = type;
        _version = ver;
        _file = file;
    }

    /**
     * Abstract common metadata from products to be used uniformly by all operators
     *
     * @param product the product created
     * @param root    the product metadata root
     */
    void addAbstractedMetadataHeader(Product product, MetadataElement root) {
        final MetadataElement absRoot = new MetadataElement(ABSTRACTED_METADATA_ROOT_NAME);
        product.getMetadataRoot().addElementAt(absRoot, 0);

        final MetadataElement mph = root.getElement("MPH");
        final MetadataElement sph = root.getElement("SPH");

        final String productType = product.getProductType();
        boolean waveProduct = false;
        if (productType.equals("ASA_WVI_1P") || productType.equals("ASA_WVS_1P") || productType.equals("ASA_WVW_2P")) {
            waveProduct = true;
        }

        MetadataElement mppAds = root.getElement("MAIN_PROCESSING_PARAMS_ADS");
        if (mppAds != null) {
            final MetadataElement ads = mppAds.getElement("MAIN_PROCESSING_PARAMS_ADS.1");
            if (ads != null)
                mppAds = ads;
        }

        // MPH
        addAbstractedAttribute("PRODUCT", mph.getAttributeString("PRODUCT", ""), absRoot, "Product name");
        addAbstractedAttribute("PRODUCT_TYPE", _productType, absRoot, "Product type");
        addAbstractedAttribute("SPH_DESCRIPTOR", sph.getAttributeString("SPH_DESCRIPTOR", ""), absRoot, "Description");
        addAbstractedAttribute("MISSION", getMission(_productType, _file), absRoot, "Satellite mission");

        String mode = "Stripmap";
        if (productType.startsWith("ASA_WS"))
            mode = "ScanSAR";
        addAbstractedAttribute("ACQUISITION_MODE", mode, absRoot, "Acquisition mode");
        addAbstractedAttribute("BEAMS", " ", absRoot, "Beams used");
        if (waveProduct) {
            addAbstractedAttribute("SWATH", sph.getAttributeString("SWATH_1", ""), absRoot, "Swath name");
        } else {
            addAbstractedAttribute("SWATH", sph.getAttributeString("SWATH", ""), absRoot, "Swath Name");
        }

        addAbstractedAttribute("PROC_TIME", mph.getAttributeUTC("PROC_TIME", new ProductData.UTC(0)), absRoot,
                "Processed time");
        addAbstractedAttribute("Processing_system_identifier", mph.getAttributeString("SOFTWARE_VER", ""), absRoot,
                "Processing system identifier");
        addAbstractedAttribute(mph, "CYCLE", absRoot, "Cycle");
        addAbstractedAttribute(mph, "REL_ORBIT", absRoot, "Track");
        addAbstractedAttribute(mph, "ABS_ORBIT", absRoot, "Orbit");
        addAbstractedAttribute("STATE_VECTOR_TIME", mph.getAttributeUTC("STATE_VECTOR_TIME", new ProductData.UTC(0)), absRoot,
                "Time of orbit state vector");
        addAbstractedAttribute("VECTOR_SOURCE", mph.getAttributeString("VECTOR_SOURCE", ""), absRoot,
                "State vector source");

        // SPH
        addAbstractedAttribute("NUM_SLICES", mph.getAttributeInt("NUM_SLICES", 0), "", absRoot, "Number of slices");
        if (waveProduct) {
            addAbstractedAttribute("first_line_time", sph.getAttributeUTC("first_cell_time", new ProductData.UTC(0)), absRoot,
                    "First cell time");
            addAbstractedAttribute("last_line_time", sph.getAttributeUTC("last_cell_time", new ProductData.UTC(0)), absRoot,
                    "Last cell time");
            addAbstractedAttribute("first_near_lat", 0, "deg", absRoot, "");
            addAbstractedAttribute("first_near_long", 0, "deg", absRoot, "");
            addAbstractedAttribute("first_far_lat", 0, "deg", absRoot, "");
            addAbstractedAttribute("first_far_long", 0, "deg", absRoot, "");
            addAbstractedAttribute("last_near_lat", 0, "deg", absRoot, "");
            addAbstractedAttribute("last_near_long", 0, "deg", absRoot, "");
            addAbstractedAttribute("last_far_lat", 0, "deg", absRoot, "");
            addAbstractedAttribute("last_far_long", 0, "deg", absRoot, "");

            addAbstractedAttribute(sph, "PASS", absRoot, "ASCENDING or DESCENDING");
            addAbstractedAttribute("SAMPLE_TYPE", " ", absRoot, "DETECTED or COMPLEX");

            String mds1_tx_rx_polar = sph.getAttributeString("tx_rx_polar", "");
            mds1_tx_rx_polar = mds1_tx_rx_polar.replace("/", "");
            addAbstractedAttribute("mds1_tx_rx_polar", mds1_tx_rx_polar, absRoot, "Polarization");
            addAbstractedAttribute("mds2_tx_rx_polar", "", absRoot, "Polarization");
            addAbstractedAttribute("mds3_tx_rx_polar", "", absRoot, "Polarization");
            addAbstractedAttribute("mds4_tx_rx_polar", "", absRoot, "Polarization");

        } else {
            addAbstractedAttribute("first_line_time", sph.getAttributeUTC("first_line_time", new ProductData.UTC(0)), absRoot,
                    "First zero doppler azimuth time");
            addAbstractedAttribute("last_line_time", sph.getAttributeUTC("last_line_time", new ProductData.UTC(0)), absRoot,
                    "Last zero doppler azimuth time");

            final double million = 1000000.0;
            addAbstractedAttribute("first_near_lat", sph.getAttributeDouble("first_near_lat", 0) / million, "deg", absRoot, "");
            addAbstractedAttribute("first_near_long", sph.getAttributeDouble("first_near_long", 0) / million, "deg", absRoot, "");
            addAbstractedAttribute("first_far_lat", sph.getAttributeDouble("first_far_lat", 0) / million, "deg", absRoot, "");
            addAbstractedAttribute("first_far_long", sph.getAttributeDouble("first_far_long", 0) / million, "deg", absRoot, "");
            addAbstractedAttribute("last_near_lat", sph.getAttributeDouble("last_near_lat", 0) / million, "deg", absRoot, "");
            addAbstractedAttribute("last_near_long", sph.getAttributeDouble("last_near_long", 0) / million, "deg", absRoot, "");
            addAbstractedAttribute("last_far_lat", sph.getAttributeDouble("last_far_lat", 0) / million, "deg", absRoot, "");
            addAbstractedAttribute("last_far_long", sph.getAttributeDouble("last_far_long", 0) / million, "deg", absRoot, "");

            addAbstractedAttribute("PASS", sph.getAttributeString("PASS", ""), absRoot, "ASCENDING or DESCENDING");
            addAbstractedAttribute("SAMPLE_TYPE", sph.getAttributeString("SAMPLE_TYPE").trim(), absRoot, "DETECTED or COMPLEX");

            String mds1_tx_rx_polar = sph.getAttributeString("mds1_tx_rx_polar", "");
            mds1_tx_rx_polar = mds1_tx_rx_polar.replace("/", "");
            addAbstractedAttribute("mds1_tx_rx_polar", mds1_tx_rx_polar, absRoot, "Polarization");
            String mds2_tx_rx_polar = sph.getAttributeString("mds2_tx_rx_polar", "");
            mds2_tx_rx_polar = mds2_tx_rx_polar.replace("/", "");
            addAbstractedAttribute("mds2_tx_rx_polar", mds2_tx_rx_polar, absRoot, "Polarization");
            addAbstractedAttribute("mds3_tx_rx_polar", "", absRoot, "Polarization");
            addAbstractedAttribute("mds4_tx_rx_polar", "", absRoot, "Polarization");
        }

        addAbstractedAttribute("polsar_data", "0", absRoot, "Polarimetric Matrix");
        addAbstractedAttribute("algorithm", sph.getAttributeString("ALGORITHM", ""), absRoot, "Processing algorithm");
        addAbstractedAttribute("azimuth_looks", sph.getAttributeDouble("azimuth_looks", 0), "", absRoot, "");
        addAbstractedAttribute("range_looks", sph.getAttributeDouble("range_looks", 0), "", absRoot, "");
        addAbstractedAttribute("range_spacing", sph.getAttributeDouble("range_spacing", 0), "m", absRoot, "Range sample spacing");
        addAbstractedAttribute("azimuth_spacing", sph.getAttributeDouble("azimuth_spacing", 0), "m", absRoot, "Azimuth sample spacing");

        if (mppAds != null) {
            addAbstractedAttribute("pulse_repetition_frequency", getPulseRepetitionFreq(mppAds), "Hz", absRoot, "PRF");
            addAbstractedAttribute("radar_frequency",
                    mppAds.getAttributeDouble("radar_freq", 0) / 1000000.0, "MHz", absRoot, "Radar frequency");
        } else {
            addAbstractedAttribute("pulse_repetition_frequency", 0, "Hz", absRoot, "PRF");
            addAbstractedAttribute("radar_frequency", 0, "MHz", absRoot, "Radar frequency");
        }
        addAbstractedAttribute("line_time_interval", sph.getAttributeDouble("line_time_interval", 0), "s", absRoot, "");
        addAbstractedAttribute("total_size", (int) (product.getRawStorageSize() / (1024.0f * 1024.0f)), "MB", absRoot,
                "Total product size");

        //MPP
        if (mppAds != null) {
            addAbstractedAttribute("num_output_lines", product.getSceneRasterHeight(), "lines", absRoot, "Raster height");
            addAbstractedAttribute("num_samples_per_line", product.getSceneRasterWidth(), "samples", absRoot, "Raster width");
            addAbstractedAttribute("subset_offset_x", 0, "samples", absRoot, "X coordinate of UL corner of subset in original image");
            addAbstractedAttribute("subset_offset_y", 0, "samples", absRoot, "Y coordinate of UL corner of subset in original image");
            addAbstractedAttribute(mppAds, "srgr_flag", absRoot, "SRGR applied");
            addAbstractedAttribute("avg_scene_height", mppAds.getAttributeDouble("avg_scene_height_ellpsoid", 0),
                    "m", absRoot, "Average scene height ellipsoid");

            String mapProjection = "";
            String geoRefSystem = "";
            if (productType.contains("APG") || productType.contains("IMG")) {
                final MetadataElement mapGads = root.getElement("MAP_PROJECTION_GADS");
                mapProjection = mapGads.getAttributeString("map_descriptor", "Geocoded");
                geoRefSystem = mapGads.getAttributeString("ellipsoid_name", "WGS84");
            }
            addAbstractedAttribute("map_projection", mapProjection, absRoot, "Map projection applied");

            addAbstractedAttribute("is_terrain_corrected", 0, "flag", absRoot, "orthorectification applied");
            addAbstractedAttribute("DEM", "", absRoot, "Digital Elevation Model used");
            addAbstractedAttribute("geo_ref_system", geoRefSystem, absRoot, "geographic reference system");
            addAbstractedAttribute("lat_pixel_res", 0.0, "deg", absRoot, "pixel resolution in geocoded image");
            addAbstractedAttribute("lon_pixel_res", 0.0, "deg", absRoot, "pixel resolution in geocoded image");

            final MetadataElement gg = root.getElement("GEOLOCATION_GRID_ADS");
            double slantRangeDist = 0;
            if (gg != null) {
                final MetadataElement gg1 = gg.getElement("GEOLOCATION_GRID_ADS.1");
                if (gg1 != null) {
                    final double slantRangeTime = gg1.getAttributeDouble("ASAR_Geo_Grid_ADSR.sd/first_line_tie_points.slant_range_times");
                    final double halfLightSpeed = 299792458.0 / 2.0;
                    slantRangeDist = slantRangeTime * halfLightSpeed / 1000000000.0; // slantRangeTime ns to s
                }
            }
            addAbstractedAttribute("slant_range_to_first_pixel", slantRangeDist, "m", absRoot, "Slant range to 1st data sample");

            addAbstractedAttribute(mppAds, "ant_elev_corr_flag", absRoot, "Antenna elevation applied");
            addAbstractedAttribute(mppAds, "range_spread_comp_flag", absRoot, "range spread compensation applied");
            addAbstractedAttribute("replica_power_corr_flag", ProductData.TYPE_UINT8, "flag",
                    "Replica pulse power correction applied", absRoot);
            addAbstractedAttribute("abs_calibration_flag", ProductData.TYPE_UINT8, "flag", "Product calibrated", absRoot);
            addAbstractedAttribute("calibration_factor",
                    mppAds.getAttributeDouble("ASAR_Main_ADSR.sd/calibration_factors.1.ext_cal_fact", 0), "", absRoot,
                    "Calibration constant");
            if(productType.startsWith("ASA_AP")) {
                addAbstractedAttribute("calibration_factor.2",
                                       mppAds.getAttributeDouble("ASAR_Main_ADSR.sd/calibration_factors.2.ext_cal_fact", 0), "", absRoot,
                                       "Calibration constant");
            }
            addAbstractedAttribute("inc_angle_comp_flag", 0, "flag", absRoot, "incidence angle compensation applied");
            addAbstractedAttribute("ref_inc_angle", 99999.0, "", absRoot, "Reference incidence angle");
            addAbstractedAttribute("ref_slant_range", 99999.0, "", absRoot, "Reference slant range");
            addAbstractedAttribute("ref_slant_range_exp", 99999.0, "", absRoot, "Reference slant range exponent");
            addAbstractedAttribute("rescaling_factor", 99999.0, "", absRoot, "Rescaling factor");

            addAbstractedAttribute("range_sampling_rate",
                    mppAds.getAttributeDouble("range_samp_rate", 0) / 1000000.0, "MHz", absRoot, "Range Sampling Rate");


            addAbstractedAttribute("range_bandwidth",
                    mppAds.getAttributeDouble("ASAR_Main_ADSR.sd/bandwidth.tot_bw_range", 0) / 1000000.0, "MHz", absRoot, "Bandwidth total in range");
            addAbstractedAttribute("azimuth_bandwidth",
                    mppAds.getAttributeDouble("to_bw_az", 0), "Hz", absRoot, "Bandwidth total in azimuth");


            addAbstractedAttribute("multilook_flag", ProductData.TYPE_UINT8, "flag",
                    "Product multilooked", absRoot);
            addAbstractedAttribute("coregistered_stack", ProductData.TYPE_UINT8, "flag", "Coregistration applied", absRoot);
        } else {
            addAbstractedAttribute("num_output_lines", 0, "lines", absRoot, "Raster height");
            addAbstractedAttribute("num_samples_per_line", 0, "samples", absRoot, "Raster width");
            addAbstractedAttribute("subset_offset_x", 0, "samples", absRoot, "X coordinate of UL corner of subset in original image");
            addAbstractedAttribute("subset_offset_y", 0, "samples", absRoot, "Y coordinate of UL corner of subset in original image");
            addAbstractedAttribute("num_samples_per_line", 0, "", absRoot, "");
            if (waveProduct)
                addAbstractedAttribute("srgr_flag", sph.getAttributeInt("SR_GR", 0), "flag", absRoot, "SRGR applied");
            else
                addAbstractedAttribute("srgr_flag", 0, "", absRoot, "SRGR applied");

            addAbstractedAttribute("avg_scene_height", 0, "m", absRoot, "Average scene height ellipsoid");
            addAbstractedAttribute("map_projection", " ", absRoot, "Map projection applied");

            addAbstractedAttribute("is_terrain_corrected", 0, "flag", absRoot, "orthorectification applied");
            addAbstractedAttribute("DEM", "", absRoot, "Digital Elevation Model used");
            addAbstractedAttribute("geo_ref_system", "", absRoot, "geographic reference system");
            addAbstractedAttribute("lat_pixel_res", 0.0, "deg", absRoot, "pixel resolution in geocoded image");
            addAbstractedAttribute("lon_pixel_res", 0.0, "deg", absRoot, "pixel resolution in geocoded image");
            addAbstractedAttribute("slant_range_to_first_pixel", 0, "m", absRoot, "Slant range to 1st data sample");

            if (waveProduct) {
                addAbstractedAttribute("ant_elev_corr_flag", sph.getAttributeInt("antenna_corr", 0), "flag", absRoot,
                        "Antenna elevation applied");
            } else {
                addAbstractedAttribute("ant_elev_corr_flag", 0, "", absRoot, "Antenna elevation applied");
            }
            addAbstractedAttribute("range_spread_comp_flag", 0, "", absRoot, "range spread compensation applied");
            addAbstractedAttribute("replica_power_corr_flag", ProductData.TYPE_UINT8, "flag",
                    "Replica pulse power correction applied", absRoot);
            addAbstractedAttribute("abs_calibration_flag", ProductData.TYPE_UINT8, "flag", "Product calibrated", absRoot);
            addAbstractedAttribute("calibration_factor", 0, "", absRoot, "Calibration constant");
            addAbstractedAttribute("inc_angle_comp_flag", 0, "flag", absRoot, "incidence angle compensation applied");
            addAbstractedAttribute("ref_inc_angle", 99999.0, "", absRoot, "Reference incidence angle");
            addAbstractedAttribute("ref_slant_range", 99999.0, "", absRoot, "Reference slant range");
            addAbstractedAttribute("ref_slant_range_exp", 99999.0, "", absRoot, "Reference slant range exponent");
            addAbstractedAttribute("rescaling_factor", 99999.0, "", absRoot, "Rescaling factor");

            addAbstractedAttribute("range_sampling_rate", 0, "MHz", absRoot, "Range Sampling Rate");
            addAbstractedAttribute("multilook_flag", ProductData.TYPE_UINT8, "flag", "Product multilooked", absRoot);
            addAbstractedAttribute("coregistered_stack", ProductData.TYPE_UINT8, "flag", "Coregistration applied", absRoot);
        }

        final MetadataElement chipADS = root.getElement("CHIRP_PARAMS_ADS");
        if(chipADS != null) {
            addAbstractedAttribute("chirp_power", chipADS.getAttributeDouble("chirp_power", 0), "dB", absRoot, "Chirp power");
        }

        final MetadataElement dsd = root.getElement("DSD");
        if (dsd != null) {
            final MetadataElement dsd17 = dsd.getElement("DSD.17");
            if (dsd17 != null) {
                addAbstractedAttribute("external_calibration_file",
                        dsd17.getAttributeString("FILE_NAME", ""), absRoot, "External calibration file used");
            }
            final MetadataElement dsd18 = dsd.getElement("DSD.18");
            if (dsd18 != null) {
                addAbstractedAttribute("orbit_state_vector_file",
                        dsd18.getAttributeString("FILE_NAME", ""), absRoot, "Orbit file used");
            }
        }

        addOrbitStateVectors(root, absRoot);

        // add SRGR coefficients if found
        final MetadataElement srgrADS = root.getElement("SR_GR_ADS");
        if (srgrADS != null) {
            addSRGRCoefficients(srgrADS, absRoot);
        }

        // add Doppler Centroid coefficients
        final MetadataElement dopplerCentroidCoeffsADS = root.getElement("DOP_CENTROID_COEFFS_ADS");
        if (dopplerCentroidCoeffsADS != null) {
            addDopplerCentroidCoefficients(dopplerCentroidCoeffsADS, absRoot);
        }

        addAbstractedAttribute("abstracted_metadata_version", METADATA_VERSION, absRoot, "AbsMetadata version");
    }

    public static String getMission(final String productType, final File file) {
        if (productType.startsWith("SAR")) {
            final String filename = file.toString().toUpperCase();
            if (filename.endsWith("E2") || filename.endsWith("E2.ZIP"))
                return "ERS2";
            else
                return "ERS1";
        }
        return "ENVISAT";
    }

    /**
     * Adds an attribute from src to dest
     *
     * @param tag   the name of the attribute
     * @param value the string value
     * @param dest  the destination element
     * @param desc  the description
     */
    private static void addAbstractedAttribute(String tag, String value, MetadataElement dest, String desc) {
        if (value == null || value.isEmpty())
            value = " ";
        final MetadataAttribute attribute = new MetadataAttribute(tag, ProductData.TYPE_ASCII, 1);
        attribute.getData().setElems(value);
        attribute.setDescription(desc);
        dest.addAttribute(attribute);
    }

    /**
     * Adds an attribute from src to dest
     *
     * @param tag   the name of the attribute
     * @param value the UTC value
     * @param dest  the destination element
     * @param desc  the description
     */
    private static void addAbstractedAttribute(String tag, ProductData.UTC value, MetadataElement dest, String desc) {
        final MetadataAttribute attribute = new MetadataAttribute(tag, ProductData.TYPE_UTC, 1);
        attribute.getData().setElems(value.getArray());
        attribute.setUnit("utc");
        attribute.setDescription(desc);
        dest.addAttribute(attribute);
    }

    /**
     * Adds an attribute from src to dest
     *
     * @param tag   the name of the attribute
     * @param value the UTC value
     * @param unit  the unit string
     * @param dest  the destination element
     * @param desc  the description
     */
    private static void addAbstractedAttribute(String tag, int value, String unit, MetadataElement dest, String desc) {
        final MetadataAttribute attribute = new MetadataAttribute(tag, ProductData.TYPE_INT32, 1);
        attribute.getData().setElemInt(value);
        attribute.setUnit(unit);
        attribute.setDescription(desc);
        dest.addAttribute(attribute);
    }

    /**
     * Adds an attribute from src to dest
     *
     * @param tag   the name of the attribute
     * @param value the double value
     * @param unit  the unit string
     * @param dest  the destination element
     * @param desc  the description
     */
    private static void addAbstractedAttribute(String tag, double value, String unit, MetadataElement dest, String desc) {
        final MetadataAttribute attribute = new MetadataAttribute(tag, ProductData.TYPE_FLOAT64, 1);
        attribute.getData().setElems(new double[]{value});
        attribute.setUnit(unit);
        attribute.setDescription(desc);
        dest.addAttribute(attribute);
    }

    /**
     * Adds an attribute into dest
     *
     * @param dest     the destination element
     * @param tag      the name of the attribute
     * @param dataType the ProductData type
     * @param unit     The unit
     * @param desc     The description
     * @return MetadataAttribute
     */
    private static MetadataAttribute addAbstractedAttribute(String tag, int dataType,
                                                            String unit, String desc, MetadataElement dest) {
        final MetadataAttribute attribute = new MetadataAttribute(tag, dataType, 1);
        if (dataType == ProductData.TYPE_ASCII)
            attribute.getData().setElems(" ");
        attribute.setUnit(unit);
        attribute.setDescription(desc);
        attribute.setReadOnly(false);
        dest.addAttribute(attribute);
        return attribute;
    }

    /**
     * Adds an attribute from src to dest
     *
     * @param src  the source element
     * @param tag  the name of the attribute
     * @param dest the destination element
     * @param desc the description
     */
    private static void addAbstractedAttribute(MetadataElement src, String tag, MetadataElement dest, String desc) {
        final MetadataAttribute attrib = src.getAttribute(tag);
        if (attrib != null) {
            MetadataAttribute copiedAttrib = attrib.createDeepClone();
            copiedAttrib.setReadOnly(false);
            copiedAttrib.setDescription(desc);
            dest.addAttribute(copiedAttrib);
        }
    }

    private static double getPulseRepetitionFreq(MetadataElement mppAds) {
        double prf = mppAds.getAttributeDouble("ASAR_Main_ADSR.sd/image_parameters.prf_value", 0);
        if (prf == 0)
            prf = mppAds.getAttributeDouble("ASAR_Main_ADSR.sd/image_parameters_IODD_4A.prf_value", 0);
        return prf;
    }

    private static void addOrbitStateVectors(MetadataElement root, MetadataElement dest) {
        final MetadataElement orbitListElem = new MetadataElement("Orbit_State_Vectors");
        dest.addElement(orbitListElem);

        // get all vectors from all MPP
        final MetadataElement rootMPPAds = root.getElement("MAIN_PROCESSING_PARAMS_ADS");
        final ArrayList<MetadataElement> mppList = new ArrayList<MetadataElement>(10);
        if (rootMPPAds != null) {
            final int numElem = rootMPPAds.getNumElements();
            if (numElem == 0) {
                mppList.add(rootMPPAds);
            } else {
                for (int i = 1; i <= numElem; ++i) {
                    final MetadataElement ads = rootMPPAds.getElement("MAIN_PROCESSING_PARAMS_ADS." + i);
                    if (ads != null)
                        mppList.add(ads);
                }
            }
        }

        int i = 1;
        for (MetadataElement mppAds : mppList) {
            addVector(mppAds, orbitListElem, "ASAR_Main_ADSR.sd/orbit_state_vectors.1", "orbit_vector" + i++);
            addVector(mppAds, orbitListElem, "ASAR_Main_ADSR.sd/orbit_state_vectors.2", "orbit_vector" + i++);
            addVector(mppAds, orbitListElem, "ASAR_Main_ADSR.sd/orbit_state_vectors.3", "orbit_vector" + i++);
            addVector(mppAds, orbitListElem, "ASAR_Main_ADSR.sd/orbit_state_vectors.4", "orbit_vector" + i++);
            addVector(mppAds, orbitListElem, "ASAR_Main_ADSR.sd/orbit_state_vectors.5", "orbit_vector" + i++);
        }
    }

    private static void addVector(MetadataElement mppAds, MetadataElement orbitListElem, String orbitPrefix, String tag) {
        ProductData.UTC utcTime;
        double xPos, yPos, zPos, xVel, yVel, zVel;
        try {
            utcTime = mppAds.getAttributeUTC(orbitPrefix + ".state_vect_time_1");
            xPos = mppAds.getAttributeDouble(orbitPrefix + ".x_pos_1") / 100.0; // 10^-2 m to m
            yPos = mppAds.getAttributeDouble(orbitPrefix + ".y_pos_1") / 100.0; // 10^-2 m to m
            zPos = mppAds.getAttributeDouble(orbitPrefix + ".z_pos_1") / 100.0; // 10^-2 m to m
            xVel = mppAds.getAttributeDouble(orbitPrefix + ".x_vel_1") / 100000.0; // 10^-5 m/s to m/s
            yVel = mppAds.getAttributeDouble(orbitPrefix + ".y_vel_1") / 100000.0; // 10^-5 m/s to m/s
            zVel = mppAds.getAttributeDouble(orbitPrefix + ".z_vel_1") / 100000.0; // 10^-5 m/s to m/s
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return;
        }

        final MetadataElement orbitElem = new MetadataElement(tag);
        orbitListElem.addElement(orbitElem);
        addAbstractedAttribute("time", utcTime, orbitElem, "");
        addAbstractedAttribute("x_pos", xPos, "", orbitElem, "");
        addAbstractedAttribute("y_pos", yPos, "", orbitElem, "");
        addAbstractedAttribute("z_pos", zPos, "", orbitElem, "");
        addAbstractedAttribute("x_vel", xVel, "", orbitElem, "");
        addAbstractedAttribute("y_vel", yVel, "", orbitElem, "");
        addAbstractedAttribute("z_vel", zVel, "", orbitElem, "");
    }

    private static void addSRGRCoefficients(MetadataElement srgrAds, MetadataElement dest) {
        final MetadataElement srgrListElem = new MetadataElement("SRGR_Coefficients");
        dest.addElement(srgrListElem);

        int listCnt = 1;
        if (srgrAds.getNumElements() == 0) {
            addOneSetSRGRCoefficients(srgrAds, srgrListElem, listCnt);
        } else {
            for (MetadataElement srgrSrc : srgrAds.getElements()) {

                addOneSetSRGRCoefficients(srgrSrc, srgrListElem, listCnt++);
            }
        }
    }

    private static void addOneSetSRGRCoefficients(MetadataElement srgrSrc, MetadataElement srgrListElem, int listCnt) {
        final ArrayList<Double> coefList = new ArrayList<Double>(5);
        ProductData.UTC utcTime;
        double origin;
        try {
            utcTime = srgrSrc.getAttributeUTC("zero_doppler_time");
            origin = srgrSrc.getAttributeDouble("ground_range_origin");

            final MetadataAttribute srgrCoefAttrib = srgrSrc.getAttribute("srgr_coeff");
            final ProductData data = srgrCoefAttrib.getData();
            final int numElems = data.getNumElems();
            for (int i = 0; i < numElems; ++i) {
                coefList.add(data.getElemDoubleAt(i));
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return;
        }

        final MetadataElement srgrElem = new MetadataElement("srgr_coef_list." + listCnt);
        srgrListElem.addElement(srgrElem);
        addAbstractedAttribute("zero_doppler_time", utcTime, srgrElem, "");
        addAbstractedAttribute("ground_range_origin", origin, "m", srgrElem, "");

        int coefCnt = 1;
        for (Double value : coefList) {
            final MetadataElement coefElem = new MetadataElement("coefficient." + coefCnt);
            ++coefCnt;
            srgrElem.addElement(coefElem);
            addAbstractedAttribute("srgr_coef", value, "", coefElem, "");
        }
    }


    private static void addDopplerCentroidCoefficients(MetadataElement dopplerCentroidCoeffsADS, MetadataElement dest) {
        final MetadataElement dopplerCentroidCoeffsListElem = new MetadataElement("Doppler_Centroid_Coefficients");
        dest.addElement(dopplerCentroidCoeffsListElem);

        int listCnt = 1;
        if (dopplerCentroidCoeffsADS.getNumElements() == 0) {
            addOneSetDopplerCentroidCoefficients(dopplerCentroidCoeffsADS, dopplerCentroidCoeffsListElem, listCnt);
        } else {
            for (MetadataElement dopplerCentroidSrc : dopplerCentroidCoeffsADS.getElements()) {

                addOneSetDopplerCentroidCoefficients(dopplerCentroidSrc, dopplerCentroidCoeffsListElem, listCnt++);
            }
        }
    }

    private static void addOneSetDopplerCentroidCoefficients(
            MetadataElement dopplerCentroidSrc, MetadataElement dopplerCentroidCoeffsListElem, int listCnt) {

        final ArrayList<Double> coefList = new ArrayList<Double>(5);
        ProductData.UTC utcTime;
        double origin;
        try {
            utcTime = dopplerCentroidSrc.getAttributeUTC("zero_doppler_time");
            origin = dopplerCentroidSrc.getAttributeDouble("slant_range_time");

            final MetadataAttribute dopCoefAttrib = dopplerCentroidSrc.getAttribute("dop_coef");
            final ProductData data = dopCoefAttrib.getData();
            final int numElems = data.getNumElems();
            for (int i = 0; i < numElems; ++i) {
                coefList.add(data.getElemDoubleAt(i));
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return;
        }

        final MetadataElement dopElem = new MetadataElement("dop_coef_list." + listCnt);
        dopplerCentroidCoeffsListElem.addElement(dopElem);
        addAbstractedAttribute("zero_doppler_time", utcTime, dopElem, "");
        addAbstractedAttribute("slant_range_time", origin, "ns", dopElem, "");

        int coefCnt = 1;
        for (Double value : coefList) {
            final MetadataElement coefElem = new MetadataElement("coefficient." + coefCnt);
            ++coefCnt;
            dopElem.addElement(coefElem);
            addAbstractedAttribute("dop_coef", value, "", coefElem, "");
        }
    }

}
