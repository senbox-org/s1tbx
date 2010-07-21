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
package org.esa.beam.dataio.avhrr;

import java.util.Calendar;

import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.ProductData;

public class HeaderUtil {

    public static MetadataAttribute createAttribute(String name, int bitField, int field, String offValue, String onValue) {
        final int bitValue = (bitField & (1 << field));
        final String stringValue;
        if (bitValue == 0) {
            stringValue = offValue;
        } else {
            stringValue = onValue;
        }
        return createAttribute(name, stringValue);
    }

    public static MetadataAttribute createAttribute(String name, int intData) {
        ProductData data = ProductData.createInstance(new int[]{intData});
        return new MetadataAttribute(name, data, true);
    }

    public static MetadataAttribute createAttribute(String name, int intData, String unit) {
        return createAttribute(name, intData, unit, null);
    }

    public static MetadataAttribute createAttribute(String name, int intData, String unit, String description) {
        MetadataAttribute attribute = createAttribute(name, intData);
        extendAttribute(attribute, unit, description);
        return attribute;
    }

    public static MetadataAttribute createAttribute(String name, float floatData) {
        ProductData data = ProductData.createInstance(new float[]{floatData});
        return new MetadataAttribute(name, data, true);
    }

    public static MetadataAttribute createAttribute(String name, float floatData, String unit) {
        return createAttribute(name, floatData, unit, null);
    }

    public static MetadataAttribute createAttribute(String name, float floatData, String unit, String description) {
        MetadataAttribute attribute = createAttribute(name, floatData);
        extendAttribute(attribute, unit, description);
        return attribute;
    }

    public static MetadataAttribute createAttribute(String name, String stringData) {
        ProductData data = ProductData.createInstance(stringData);
        return new MetadataAttribute(name, data, true);
    }

    public static MetadataAttribute createAttribute(String name, String stringData, String unit) {
        return createAttribute(name, stringData, unit, null);
    }

    public static MetadataAttribute createAttribute(String name, String stringData, String unit, String description) {
        MetadataAttribute attribute = createAttribute(name, stringData);
        extendAttribute(attribute, unit, description);
        return attribute;
    }

    public static ProductData.UTC createUTCDate(int year, int dayOfYear, int millisInDay) {
        Calendar calendar = ProductData.UTC.createCalendar();

        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.DAY_OF_YEAR, dayOfYear);
        calendar.add(Calendar.MILLISECOND, millisInDay);

        return ProductData.UTC.create(calendar.getTime(), 0);
    }

    private static void extendAttribute(MetadataAttribute attribute, String unit, String description) {
        if (unit != null) {
            attribute.setUnit(unit);
        }
        if (description != null) {
            attribute.setDescription(description);
        }
    }
}
