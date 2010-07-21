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

package org.esa.beam.dataio.landsat;


import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.ProductData;

import java.util.List;
import java.util.Vector;

/**
 * The class <code>LandsatTMMetadata</code> is used to store the Metadata
 * of Landsat TM products
 *
 * @author Christian Berwanger (ai0263@umwelt-campus.de)
 */

public class LandsatTMMetadata {

    private List<MetadataElement> landsatMetadataElements;

    /**
     * stores an integer value in a metadata attibute
     *
     * @param name
     * @param intData
     *
     * @return metadata attribut
     */
    private static MetadataAttribute createAttribute(final String name, final int intData) {
        final ProductData data = ProductData.createInstance(new int[]{intData});
        return new MetadataAttribute(name, data, true);
    }

    /**
     * stores a string value in a metadata attibute
     *
     * @param name
     * @param stringData
     *
     * @return metadata attribut
     */
    protected static MetadataAttribute createAttribute(final String name, final String stringData) {
        final ProductData data = ProductData.createInstance(stringData);
        return new MetadataAttribute(name, data, true);
    }

    /**
     * stores an double value in a metadata attibute
     *
     * @param name
     * @param doubleData
     *
     * @return metadata attribute
     */
    protected static MetadataAttribute createAttribute(final String name, final double doubleData) {
        final ProductData data = ProductData.createInstance(new double []{doubleData});
        return new MetadataAttribute(name, data, true);
    }

    /**
     * stores a UTC Date in a metadata attribute
     *
     * @param name
     * @param date
     *
     * @return metadata attribute
     */
    protected static MetadataAttribute createAttribute(final String name, final ProductData.UTC date) {
        return new MetadataAttribute(name, date, true);
    }

    /**
     * @param name
     * @param longdata
     *
     * @return metadata attribute
     */
    private static MetadataAttribute createAttribute(final String name, final long longdata) {
        final ProductData data = ProductData.createInstance(new long []{longdata});
        return new MetadataAttribute(name, data, true);
    }

    /**
     * @param name
     * @param floatdata
     *
     * @return metadata attribute
     */

    private static MetadataAttribute createAttribute(final String name, final float floatdata) {
        final ProductData data = ProductData.createInstance(new float []{floatdata});
        return new MetadataAttribute(name, data, true);
    }


    /**
     * stores any data type with its long description
     *
     * @param shortDescription
     * @param data
     * @param description
     *
     * @return metadata attribute
     */
    protected static final MetadataAttribute createAttribute(final String shortDescription, final Object data,
                                                             final String description) {
        return createAttribute(shortDescription, data, description, null);
    }

    /**
     * stores any data with its long description and the data's unit
     *
     * @param shortDescription
     * @param data
     * @param description
     * @param unit
     *
     * @return metadata attribute
     */
    protected static MetadataAttribute createAttribute(final String shortDescription, final Object data,
                                                       final String description, final LandsatConstants.Unit unit) {

        MetadataAttribute attri;

        if (data instanceof String) {
            attri = createAttribute(shortDescription, (String) data);
        } else if (data instanceof Integer) {
            attri = createAttribute(shortDescription, ((Integer) data).intValue());
        } else if (data instanceof Double) {
            attri = createAttribute(shortDescription, ((Double) data).doubleValue());
        } else if (data instanceof Long) {
            attri = createAttribute(shortDescription, ((Long) data).longValue());
        } else if (data instanceof Float) {
            attri = createAttribute(shortDescription, ((Float) data).floatValue());
        } else if (data instanceof ProductData.UTC) {
            attri = createAttribute(shortDescription, (ProductData.UTC) data);
        } else {
            return null;
        }

        attri.setDescription(description);
        if (unit != null) {
            attri.setUnit(unit.toString());
        }
        return attri;
    }

    /**
     * @return container includes metadataelements
     */
    public final List<MetadataElement> getLandsatMetadataElements() {
        return landsatMetadataElements;
    }

    /**
     * adds a new metadata element to the landsat metadata collection
     *
     * @param element
     */
    protected final void addElement(MetadataElement element) {
        if (landsatMetadataElements == null) {
            landsatMetadataElements = new Vector<MetadataElement>();
        }
        landsatMetadataElements.add(element);
    }
}
