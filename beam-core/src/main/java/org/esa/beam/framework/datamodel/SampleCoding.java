package org.esa.beam.framework.datamodel;

import org.esa.beam.util.Guardian;

/**
 * Provides the information required to decode integer sample values that
 * are combined of single flags (bit indexes).
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
        if (!attribute.getData().isScalar()) {
            throw new IllegalArgumentException("attribute value is not a scalar");
        }
        super.addAttribute(attribute);
    }

    /**
     * Adds a new coding value to this sample coding.
     *
     * @param name        the coding name
     * @param value       the value
     * @param description the description text
     * @throws IllegalArgumentException if <code>name</code> is null
     * @return A new attribute representing the coded sample.
     */
    public MetadataAttribute addSample(String name, int value, String description) {
        Guardian.assertNotNull("name", name);
        MetadataAttribute attribute = new MetadataAttribute(name, ProductData.TYPE_INT32);
        attribute.setDataElems(new int[]{value});
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
