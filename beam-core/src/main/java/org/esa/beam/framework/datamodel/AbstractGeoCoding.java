package org.esa.beam.framework.datamodel;

import com.bc.ceres.core.Assert;
import org.esa.beam.framework.dataio.ProductSubsetDef;
import org.geotools.referencing.crs.DefaultDerivedCRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.referencing.cs.DefaultCartesianCS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

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

    private CoordinateReferenceSystem crs;

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

    @Override
    public CoordinateReferenceSystem getCRS() {
        if (crs == null) {
            // the name is used as hashcode, if the name is constant over all instances
            // the CRS.findMathTransform(...) will return always the first acquired Transform,
            // because this method caches.
            crs = new DefaultDerivedCRS(String.format("%s@%d", getClass().getSimpleName(), this.hashCode()),
                                        DefaultGeographicCRS.WGS84,
                                        new GeoCodingMathTransform(this, GeoCodingMathTransform.Mode.G2P),
                                        DefaultCartesianCS.DISPLAY);

        }
        return crs;
    }

    @Override
    public void setCRS(CoordinateReferenceSystem crs) {
        Assert.notNull(crs, "crs");
        this.crs = crs;
    }
}
