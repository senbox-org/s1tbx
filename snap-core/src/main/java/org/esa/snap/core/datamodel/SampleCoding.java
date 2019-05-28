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

package org.esa.snap.core.datamodel;

import org.esa.snap.core.util.Guardian;

/**
 * Provides the information required to decode integer sample values that
 * are combined of single flags (bit indexes).
 *
 * @since BEAM 4.2
 */
public class SampleCoding extends MetadataElement {
    public SampleCoding(String name) {
        super(name);
    }

    /**
     * Overrides the base class <code>addElement</code> in order to <b>not</b> add an element to this flag coding
     * because flag codings do not support inner elements.
     *
     * @param element the element to be added, always ignored
     */
    @Override
    public void addElement(MetadataElement element) {
    }

    /**
     * Adds an attribute to this node. If an attribute with the same name already exists, the method does nothing.
     *
     * @param attribute the attribute to be added
     * @throws IllegalArgumentException if the attribute added is not an integer or does not have a scalar value
     */
    @Override
    public void addAttribute(MetadataAttribute attribute) {
        if (!attribute.getData().isInt()) {
            throw new IllegalArgumentException("attribute value is not a integer");
        }
        if (attribute.getData().getNumElems() == 0) {
            throw new IllegalArgumentException("attribute value is missing");
        }
        if (containsAttribute(attribute.getName())) {
            throw new IllegalArgumentException("SampleCoding contains already an attribute with the name '" + attribute.getName() + "'");
        }
        super.addAttribute(attribute);
    }

    /**
     * Adds a new coding value to this sample coding.
     *
     * @param name        the coding name
     * @param value       the value
     * @param description the description text
     * @return A new attribute representing the coded sample.
     * @throws IllegalArgumentException if <code>name</code> is null
     */
    public MetadataAttribute addSample(String name, int value, String description) {
        return addSamples(name, new int[]{value}, description);
    }

    /**
     * Adds a new coding value to this sample coding.
     *
     * @param name        the coding name
     * @param values      the values
     * @param description the description text
     * @return A new attribute representing the coded sample.
     * @throws IllegalArgumentException if <code>name</code> is null
     */
    public MetadataAttribute addSamples(String name, int[] values, String description) {
        Guardian.assertNotNull("name", name);
        final ProductData productData = ProductData.createInstance(ProductData.TYPE_UINT32, values.length);
        MetadataAttribute attribute = new MetadataAttribute(name, productData, false);
        attribute.setDataElems(values);
        if (description != null) {
            attribute.setDescription(description);
        }
        addAttribute(attribute);
        return attribute;
    }

    /**
     * Gets the number of coded sample values.
     *
     * @return the number of coded sample values
     */
    public int getSampleCount() {
        return getNumAttributes();
    }

    /**
     * Gets the sample name at the specified attribute index.
     *
     * @param index the attribute index.
     * @return the sample name.
     */
    public String getSampleName(int index) {
        return getAttributeAt(index).getName();
    }

    /**
     * Gets the sample value at the specified attribute index.
     *
     * @param index the attribute index.
     * @return the sample value.
     */
    public int getSampleValue(int index) {
        return getAttributeAt(index).getData().getElemInt();
    }

}
