package org.esa.snap.core.gpf.descriptor;

/**
 * Metadata used to describe data elements of an operator.
 *
 * @author Norman Fomferra
 * @since BEAM 5
 */
public interface DataElementDescriptor extends ElementDescriptor {

    /**
     * @return The element's data type.
     * Defaults to {@link Object}.
     */
    Class<?> getDataType();
}
