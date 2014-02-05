package org.esa.beam.framework.gpf.descriptor;

/**
 * Metadata used to describe elements of an operator.
 *
 * @author Norman Fomferra
 * @since BEAM 5
 */
public interface ElementDescriptor {
    /**
     * @return The symbolic name used to unambiguously identify this element.
     * E.g. the fully qualified name of a Java class.
     */
    String getName();

    /**
     * @return A short form of the symbolic name.
     * Defaults to the empty string (= not set).
     */
    String getAlias();

    /**
     * @return A human-readable version of the symbolic name to be used in user interfaces.
     * Defaults to the empty string (= not set).
     */
    String getLabel();

    /**
     * @return A short description.
     * Defaults to the empty string (= not set).
     */
    String getDescription();
}
