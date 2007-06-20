package org.esa.beam.framework.datamodel;

import org.esa.beam.framework.dataio.ProductSubsetDef;

/**
 * <code>AbstractGeoCoding</code> is the base class of all geo-coding implementation.
 * <p/>
 * <p> <b> Note:</b> New geo-coding implementations shall implement this abstract class, instead of
 * implementing the interface {@link GeoCoding}.
 * </p>
 *
 * @author Marco Peters
 */
public abstract class AbstractGeoCoding implements GeoCoding {

    /**
     * Transfers the geo-coding of the {@link Scene srcScene} to the {@link Scene destScene} with respect to the given
     * {@link ProductSubsetDef subsetDef}.
     *
     * @param srcScene  the source scene
     * @param destScene the destination scene
     * @param subsetDef the definition of the subset, may be <code>null</code>
     *
     * @return true, if the geo-coding could be transferred.
     */
    public abstract boolean transferGeoCoding(Scene srcScene, Scene destScene, ProductSubsetDef subsetDef);
}
