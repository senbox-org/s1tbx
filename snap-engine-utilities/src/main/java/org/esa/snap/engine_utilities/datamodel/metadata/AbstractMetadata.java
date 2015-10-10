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
import org.esa.snap.engine_utilities.datamodel.OrbitStateVector;

/**
 * Creates a generic interface to metadata
 */
public final class AbstractMetadata extends AbstractMetadataBase implements AbstractMetadataInterface {

    /**
     * If AbstractedMetadata is modified by adding new attributes then this version number needs to be incremented
     */
    private static final String METADATA_VERSION = "6.0";

    private static final String abstracted_metadata_version = "metadata_version";
    public static final String ABSTRACT_METADATA_ROOT = "Abstracted_Metadata";
    private static final String ORIGINAL_PRODUCT_METADATA = "Original_Product_Metadata";

    /**
     * Abstracted metadata generic to most EO products
     */
    public static final String SLAVE_METADATA_ROOT = "Slave_Metadata";
    public static final String MASTER_BANDS = "Master_bands";
    public static final String SLAVE_BANDS = "Slave_bands";

    public static final String product_name = "PRODUCT";
    public static final String product_type = "PRODUCT_TYPE";
    public static final String descriptor = "SPH_DESCRIPTOR";
    public static final String path = "PATH";
    public static final String mission = "MISSION";
    public static final String acquisition_mode = "ACQUISITION_MODE";
    public static final String beams = "BEAMS";
    public static final String annotation = "annotation";
    public static final String band_names = "band_names";
    public static final String swath = "swath";
    public static final String processing_time = "PROC_TIME";
    public static final String processing_system = "Processing_system_identifier";
    public static final String cycle = "orbit_cycle";
    public static final String rel_orbit = "REL_ORBIT";
    public static final String abs_orbit = "ABS_ORBIT";
    public static final String state_vector_time = "STATE_VECTOR_TIME";

    // SPH
    public static final String num_slices = "num_slices";
    public static final String first_line_time = "first_line_time";
    public static final String last_line_time = "last_line_time";
    public static final String line_time_interval = "line_time_interval";

    public static final String first_near_lat = "first_near_lat";
    public static final String first_near_long = "first_near_long";
    public static final String first_far_lat = "first_far_lat";
    public static final String first_far_long = "first_far_long";
    public static final String last_near_lat = "last_near_lat";
    public static final String last_near_long = "last_near_long";
    public static final String last_far_lat = "last_far_lat";
    public static final String last_far_long = "last_far_long";

    public static final String pass = "pass";
    public static final String sample_type = "sample_type";

    public static final String total_size = "total_size";
    public static final String num_output_lines = "num_output_lines";
    public static final String num_samples_per_line = "num_samples_per_line";

    public static final String subset_offset_x = "subset_offset_x";
    public static final String subset_offset_y = "subset_offset_y";
    public static final String map_projection = "map_projection";

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

    /**
     * Get abstracted metadata.
     *
     * @param sourceProduct the product
     * @return AbstractMetadata object
     */
    public static AbstractMetadata getAbstractedMetadata(final Product sourceProduct) {

        final MetadataElement root = sourceProduct.getMetadataRoot();
        if (root == null) {
            return null;
        }
        return new AbstractMetadata(root, root.getElement(AbstractMetadata.ABSTRACT_METADATA_ROOT));
    }

    private AbstractMetadata(final MetadataElement root, final MetadataElement abstractedMetadata) {
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
    protected MetadataElement addAbstractedMetadataHeader(final MetadataElement root) {
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
        addAbstractedAttribute(absRoot, product_name, ProductData.TYPE_ASCII, "", "Product name");
        addAbstractedAttribute(absRoot, product_type, ProductData.TYPE_ASCII, "", "Product type");
        addAbstractedAttribute(absRoot, descriptor, ProductData.TYPE_ASCII, "", "Description");
        addAbstractedAttribute(absRoot, mission, ProductData.TYPE_ASCII, "", "Satellite mission");
        addAbstractedAttribute(absRoot, acquisition_mode, ProductData.TYPE_ASCII, "", "Acquisition mode");
        addAbstractedAttribute(absRoot, beams, ProductData.TYPE_ASCII, "", "Beams used");
        addAbstractedAttribute(absRoot, swath, ProductData.TYPE_ASCII, "", "Swath name");
        addAbstractedAttribute(absRoot, processing_time, ProductData.TYPE_UTC, "utc", "Processed time");
        addAbstractedAttribute(absRoot, processing_system, ProductData.TYPE_ASCII, "", "Processing system identifier");
        addAbstractedAttribute(absRoot, cycle, ProductData.TYPE_INT32, "", "Cycle");
        addAbstractedAttribute(absRoot, rel_orbit, ProductData.TYPE_INT32, "", "Track");
        addAbstractedAttribute(absRoot, abs_orbit, ProductData.TYPE_INT32, "", "Orbit");
        addAbstractedAttribute(absRoot, state_vector_time, ProductData.TYPE_UTC, "utc", "Time of orbit state vector");

        // SPH
        addAbstractedAttribute(absRoot, num_slices, ProductData.TYPE_INT32, "", "Number of slices");
        addAbstractedAttribute(absRoot, first_line_time, ProductData.TYPE_UTC, "utc", "First zero doppler azimuth time");
        addAbstractedAttribute(absRoot, last_line_time, ProductData.TYPE_UTC, "utc", "Last zero doppler azimuth time");
        addAbstractedAttribute(absRoot, line_time_interval, ProductData.TYPE_FLOAT64, "s", "");
        addAbstractedAttribute(absRoot, first_near_lat, ProductData.TYPE_FLOAT64, "deg", "");
        addAbstractedAttribute(absRoot, first_near_long, ProductData.TYPE_FLOAT64, "deg", "");
        addAbstractedAttribute(absRoot, first_far_lat, ProductData.TYPE_FLOAT64, "deg", "");
        addAbstractedAttribute(absRoot, first_far_long, ProductData.TYPE_FLOAT64, "deg", "");
        addAbstractedAttribute(absRoot, last_near_lat, ProductData.TYPE_FLOAT64, "deg", "");
        addAbstractedAttribute(absRoot, last_near_long, ProductData.TYPE_FLOAT64, "deg", "");
        addAbstractedAttribute(absRoot, last_far_lat, ProductData.TYPE_FLOAT64, "deg", "");
        addAbstractedAttribute(absRoot, last_far_long, ProductData.TYPE_FLOAT64, "deg", "");

        addAbstractedAttribute(absRoot, pass, ProductData.TYPE_ASCII, "", "ASCENDING or DESCENDING");
        addAbstractedAttribute(absRoot, sample_type, ProductData.TYPE_ASCII, "", "DETECTED or COMPLEX");

        addAbstractedAttribute(absRoot, total_size, ProductData.TYPE_UINT32, "MB", "Total product size");
        addAbstractedAttribute(absRoot, num_output_lines, ProductData.TYPE_UINT32, "lines", "Raster height");
        addAbstractedAttribute(absRoot, num_samples_per_line, ProductData.TYPE_UINT32, "samples", "Raster width");

        addAbstractedAttribute(absRoot, subset_offset_x, ProductData.TYPE_UINT32, "samples", "X coordinate of UL corner of subset in original image");
        addAbstractedAttribute(absRoot, subset_offset_y, ProductData.TYPE_UINT32, "samples", "Y coordinate of UL corner of subset in original image");
        setAttribute(subset_offset_x, 0);
        setAttribute(subset_offset_y, 0);
        addAbstractedAttribute(absRoot, map_projection, ProductData.TYPE_ASCII, "", "Map projection applied");

        absRoot.addElement(new MetadataElement(orbit_state_vectors));

        MetadataAttribute att = addAbstractedAttribute(absRoot, abstracted_metadata_version, ProductData.TYPE_ASCII, "", "AbsMetadata version");
        att.getData().setElems(METADATA_VERSION);

        return absRoot;
    }

    /**
     * Returns the original product metadata or the root if not found
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
     * Creates and returns the original product metadata
     *
     * @param product input product
     * @return original metadata
     */
    public static MetadataElement addOriginalProductMetadata(final Product product) {
        final MetadataElement root = product.getMetadataRoot();
        MetadataElement origMetadata = root.getElement(ORIGINAL_PRODUCT_METADATA);
        if (origMetadata == null) {
            origMetadata = new MetadataElement(ORIGINAL_PRODUCT_METADATA);
            root.addElement(origMetadata);
        }
        return origMetadata;
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

    public static MetadataElement getSlaveMetadata(final Product product) {
        final MetadataElement targetRoot = product.getMetadataRoot();
        MetadataElement targetSlaveMetadataRoot = targetRoot.getElement(AbstractMetadata.SLAVE_METADATA_ROOT);
        if (targetSlaveMetadataRoot == null) {
            targetSlaveMetadataRoot = new MetadataElement(AbstractMetadata.SLAVE_METADATA_ROOT);
            targetRoot.addElement(targetSlaveMetadataRoot);
        }
        return targetSlaveMetadataRoot;
    }

    /**
     * Create sub-metadata element.
     *
     * @param root The root metadata element.
     * @param tag  The sub-metadata element name.
     * @return The sub-metadata element.
     */
    public static MetadataElement addElement(final MetadataElement root, final String tag) {

        MetadataElement subElemRoot = root.getElement(tag);
        if (subElemRoot == null) {
            subElemRoot = new MetadataElement(tag);
            root.addElement(subElemRoot);
        }
        return subElemRoot;
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
     * @return orbitStateVectors Array of orbit state vectors.
     */
    public OrbitStateVector[] getOrbitStateVectors() {

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
     * @param orbitStateVectors The orbit state vectors.
     * @throws Exception if orbit state vector length is not correct
     */
    public void setOrbitStateVectors(final OrbitStateVector[] orbitStateVectors) throws Exception {

        final MetadataElement elemRoot = absRoot.getElement(orbit_state_vectors);
        final int numElems = elemRoot.getNumElements();
        if (numElems != orbitStateVectors.length) {
            throw new Exception("Length of orbit state vector array is not correct");
        }

        for (int i = 0; i < numElems; i++) {
            final OrbitStateVector vector = orbitStateVectors[i];
            final MetadataElement subElemRoot = elemRoot.getElement(orbit_vector + (i + 1));
            subElemRoot.setAttributeUTC(orbit_vector_time, vector.time);
            subElemRoot.setAttributeDouble(orbit_vector_x_pos, vector.x_pos);
            subElemRoot.setAttributeDouble(orbit_vector_y_pos, vector.y_pos);
            subElemRoot.setAttributeDouble(orbit_vector_z_pos, vector.z_pos);
            subElemRoot.setAttributeDouble(orbit_vector_x_vel, vector.x_vel);
            subElemRoot.setAttributeDouble(orbit_vector_y_vel, vector.y_vel);
            subElemRoot.setAttributeDouble(orbit_vector_z_vel, vector.z_vel);
        }
    }
}
