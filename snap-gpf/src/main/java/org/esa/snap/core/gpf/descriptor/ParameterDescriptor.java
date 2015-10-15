package org.esa.snap.core.gpf.descriptor;

import com.bc.ceres.binding.Converter;
import com.bc.ceres.binding.Validator;
import com.bc.ceres.binding.dom.DomConverter;
import org.esa.snap.core.datamodel.RasterDataNode;

/**
 * Target parameter element metadata.
 *
 * @author Norman Fomferra
 * @since BEAM 5
 */
public interface ParameterDescriptor extends DataElementDescriptor {

    /**
     * @return An alias name for the elements of a parameter array.
     * Forces element-wise array conversion from and to DOM representation.
     * Defaults to the empty string (= not set).
     */
    String getItemAlias();

    /**
     * Gets the parameter's default value.
     * The default value set is given as a textual representations of the actual value.
     * The framework creates the actual value set by converting the text value to
     * an object using the associated {@link com.bc.ceres.binding.Converter}.
     *
     * @return The default value.
     * Defaults to the empty string (= not set).
     * @see #getConverterClass()
     */
    String getDefaultValue();

    /**
     * @return The parameter physical unit.
     * Defaults to the empty string (= not set).
     */
    String getUnit();

    /**
     * Gets the set of values which can be assigned to a parameter field.
     * The value set is given as textual representations of the actual values.
     * The framework creates the actual value set by converting each text value to
     * an object value using the associated {@link com.bc.ceres.binding.Converter}.
     *
     * @return The value set. Defaults to empty array (= not set).
     * @see #getConverterClass()
     */
    String[] getValueSet();

    /**
     * Gets the valid interval for numeric parameters, e.g. {@code "[10,20)"}: in the range 10 (inclusive) to 20 (exclusive).
     *
     * @return The valid interval. Defaults to empty string (= not set).
     */
    String getInterval();

    /**
     * Gets a conditional expression which must return {@code true} in order to indicate
     * that the parameter value is valid, e.g. {@code "value > 2.5"}.
     *
     * @return A conditional expression. Defaults to empty string (= not set).
     */
    String getCondition();

    /**
     * Gets a regular expression pattern to which a textual parameter value must match in order to indicate
     * a valid value, e.g. {@code "a*"}.
     *
     * @return A regular expression pattern. Defaults to empty string (= not set).
     * @see java.util.regex.Pattern
     */
    String getPattern();

    /**
     * Gets a format string to which a textual parameter value must match in order to indicate
     * a valid value, e.g. {@code "yyyy-MM-dd HH:mm:ss.Z"}.
     *
     * @return A format string. Defaults to empty string (= not set).
     * @see java.text.Format
     */
    String getFormat();

    /**
     * Parameter value must not be {@code null}?
     *
     * @return {@code true}, if so. Defaults to {@code false}.
     */
    boolean isNotNull();

    /**
     * Parameter value must not be an empty string?
     *
     * @return {@code true}, if so. Defaults to {@code false}.
     */
    boolean isNotEmpty();

    /**
     * Is the parameter marked as deprecated?
     *
     * @return {@code true}, if so. Defaults to {@code false}.
     */
    boolean isDeprecated();

    /**
     * A validator to be used to validate a parameter value.
     *
     * @return The validator class.
     */
    Class<? extends Validator> getValidatorClass();

    /**
     * A converter to be used to convert a text to the parameter value and vice versa.
     *
     * @return The converter class.
     */
    Class<? extends Converter> getConverterClass();

    /**
     * A converter to be used to convert an (XML) DOM to the parameter value and vice versa.
     *
     * @return The DOM converter class.
     */
    Class<? extends DomConverter> getDomConverterClass();

    /**
     * Specifies which {@code RasterDataNode} subclass of the source products is used
     * to fill the {@link #getValueSet()} for this parameter.
     *
     * @return The raster data node type.
     */
    Class<? extends RasterDataNode> getRasterDataNodeClass();

    /**
     * @return {@code true} if the parameter type is a composite data structure.
     * @see #getDataType()
     * @see #getStructureMemberDescriptors()
     */
    boolean isStructure();

    /**
     * @return The descriptors for the structure members of this parameter type. The returned array will be empty, if
     * this parameter doesn't have a structure data type.
     * @see #getDataType()
     * @see #isStructure()
     */
    ParameterDescriptor[] getStructureMemberDescriptors();
}
