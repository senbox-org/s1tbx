package org.esa.beam.binning;

/**
 * The binning context.
 *
 * @author Norman Fomferra
 */
public interface BinningContext {

    /**
     * @return The variable context. Defines numbering of variables involved in the binning.
     */
    VariableContext getVariableContext();

    /**
     * @return The definition of the binning grid.
     */
    PlanetaryGrid getPlanetaryGrid();

    /**
     * @return The bin manager which is used to perform compute bin operations.
     */
    BinManager getBinManager();

    /**
     * @return The compositing type which is used to during the binning.
     */
    CompositingType getCompositingType();

    /**
     * @return The super-sampling of source pixels. May be {@code null}, if not used.
     */
    Integer getSuperSampling();

}
