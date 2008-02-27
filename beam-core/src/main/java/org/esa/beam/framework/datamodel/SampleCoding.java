package org.esa.beam.framework.datamodel;

import org.esa.beam.util.Guardian;
import org.esa.beam.util.Debug;

 /**
  * Provides the information required to decode integer sample values that
  * are combined of single flags (bit indexes).
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


}
