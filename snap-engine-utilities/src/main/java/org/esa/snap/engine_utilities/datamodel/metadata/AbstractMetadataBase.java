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
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.util.SystemUtils;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.logging.Logger;

/**
 * Base AbstractMetadata
 */
public abstract class AbstractMetadataBase {

    protected MetadataElement absRoot;
    private static Logger log = SystemUtils.LOG;

    /**
     * Default no data values
     */
    public static final int NO_METADATA = 99999;
    public static final short NO_METADATA_BYTE = 0;
    public static final String NO_METADATA_STRING = " ";
    public static final ProductData.UTC NO_METADATA_UTC = new ProductData.UTC(0);

    public final DateFormat dateFormat = ProductData.UTC.createDateFormat("yyyy-MM-dd HH:mm:ss");

    AbstractMetadataBase(final MetadataElement root, final MetadataElement abstractedMetadata) {
        absRoot = abstractedMetadata;
        if (absRoot == null) {
            absRoot = addAbstractedMetadataHeader(root);
        } else {
            migrateToCurrentVersion(absRoot);
            patchMissingMetadata(absRoot);
        }
    }

    protected final MetadataElement getAbsRoot() {
        return absRoot;
    }

    protected abstract boolean isCurrentVersion();

    protected abstract void migrateToCurrentVersion(final MetadataElement abstractedMetadata);

    protected abstract MetadataElement addAbstractedMetadataHeader(final MetadataElement root);

    private void patchMissingMetadata(final MetadataElement abstractedMetadata) {
        if (isCurrentVersion())
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
     * @param tag   the name of the attribute
     * @param value the string value
     */
    public void setAttribute(final String tag, final String value) {
        setAttribute(absRoot, tag, value);
    }

    /**
     * Sets an attribute as a string
     *
     * @param dest  the destination element
     * @param tag   the name of the attribute
     * @param value the string value
     */
    private static void setAttribute(final MetadataElement dest, final String tag, final String value) {
        if (dest == null)
            return;
        final MetadataAttribute attrib = dest.getAttribute(tag);
        if (attrib != null && value != null) {
            if (value.isEmpty())
                attrib.getData().setElems(NO_METADATA_STRING);
            else
                attrib.getData().setElems(value);
        } else {
            if (attrib == null)
                log.severe(tag + " not found in metadata");
            if (value == null)
                log.severe(tag + " metadata value is null");
        }
    }

    /**
     * Sets an attribute as a UTC
     *
     * @param tag   the name of the attribute
     * @param value the UTC value
     */
    public void setAttribute(final String tag, final ProductData.UTC value) {
        setAttribute(absRoot, tag, value);
    }

    /**
     * Sets an attribute as a UTC
     *
     * @param dest  the destination element
     * @param tag   the name of the attribute
     * @param value the UTC value
     */
    private static void setAttribute(final MetadataElement dest, final String tag, final ProductData.UTC value) {
        if (dest == null)
            return;
        final MetadataAttribute attrib = dest.getAttribute(tag);
        if (attrib != null && value != null) {
            attrib.getData().setElems(value.getArray());
        } else {
            if (attrib == null)
                log.severe(tag + " not found in metadata");
            if (value == null)
                log.severe(tag + " metadata value is null");
        }
    }

    /**
     * Sets an attribute as an int
     *
     * @param tag   the name of the attribute
     * @param value the int value
     */
    public void setAttribute(final String tag, final int value) {
        setAttribute(absRoot, tag, value);
    }

    /**
     * Sets an attribute as an int
     *
     * @param dest  the destination element
     * @param tag   the name of the attribute
     * @param value the int value
     */
    private static void setAttribute(final MetadataElement dest, final String tag, final int value) {
        if (dest == null)
            return;
        final MetadataAttribute attrib = dest.getAttribute(tag);
        if (attrib == null)
            log.severe(tag + " not found in metadata");
        else
            attrib.getData().setElemInt(value);
    }

    /**
     * Sets an attribute as a Double
     *
     * @param tag   the name of the attribute
     * @param value the Double value
     */
    public void setAttribute(final String tag, final Double value) {
        setAttribute(absRoot, tag, value);
    }

    /**
     * Sets an attribute as a Double
     *
     * @param dest  the destination element
     * @param tag   the name of the attribute
     * @param value the Double value
     */
    public static void setAttribute(final MetadataElement dest, final String tag, final Double value) {
        if (dest == null)
            return;
        final MetadataAttribute attrib = dest.getAttribute(tag);
        if (attrib == null)
            log.severe(tag + " not found in metadata");
        else if (value != null)
            attrib.getData().setElemDouble(value);
    }

    public static ProductData.UTC parseUTC(final String timeStr) {
        try {
            if (timeStr == null)
                return NO_METADATA_UTC;
            return ProductData.UTC.parse(timeStr);
        } catch (ParseException e) {
            return NO_METADATA_UTC;
        }
    }

    private static ProductData.UTC parseUTC(final String timeStr, final DateFormat format) {
        try {
            final int dotPos = timeStr.lastIndexOf('.');
            if (dotPos > 0) {
                final String newTimeStr = timeStr.substring(0, Math.min(dotPos + 6, timeStr.length()));
                return ProductData.UTC.parse(newTimeStr, format);
            }
            return ProductData.UTC.parse(timeStr, format);
        } catch (ParseException e) {
            log.severe("UTC parse error:" + e.toString());
            return NO_METADATA_UTC;
        }
    }

    public boolean getAttributeBoolean(final String tag) throws IllegalArgumentException {
        final int val = absRoot.getAttributeInt(tag);
        if (val == NO_METADATA)
            throw new IllegalArgumentException("Metadata " + tag + " has not been set");
        return val != 0;
    }

    public double getAttributeDouble(final String tag) throws IllegalArgumentException {
        final double val = absRoot.getAttributeDouble(tag);
        if (val == NO_METADATA)
            throw new IllegalArgumentException("Metadata " + tag + " has not been set");
        return val;
    }

    public int getAttributeInt(final String tag) throws IllegalArgumentException {
        final int val = absRoot.getAttributeInt(tag);
        if (val == NO_METADATA)
            throw new IllegalArgumentException("Metadata " + tag + " has not been set");
        return val;
    }

    public String getAttributeString(final String tag) throws IllegalArgumentException {
        return absRoot.getAttributeString(tag);
    }

    public ProductData.UTC getAttributeUTC(final String tag) {
        return absRoot.getAttributeUTC(tag, NO_METADATA_UTC);
    }

    public MetadataElement getElement(final String tag) throws IllegalArgumentException {
        MetadataElement elem = absRoot.getElement(tag);
        if (elem == null) {
            throw new IllegalArgumentException("Element " + tag + " has not been set");
        }
        return elem;
    }
}
