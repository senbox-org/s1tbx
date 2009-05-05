package org.esa.beam.framework.datamodel;

import com.bc.ceres.core.Assert;
import org.esa.beam.framework.dataio.ProductSubsetDef;
import org.geotools.referencing.crs.DefaultDerivedCRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.referencing.cs.DefaultCartesianCS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.awt.geom.AffineTransform;

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

    private CoordinateReferenceSystem baseCRS;
    private CoordinateReferenceSystem gridCRS;
    private CoordinateReferenceSystem modelCRS;

    protected AbstractGeoCoding() {
        setBaseCRS(DefaultGeographicCRS.WGS84);
        setGridCRS(new DefaultDerivedCRS("Grid CS based on " + baseCRS.getName(),
                                         getBaseCRS(),
                                         new GeoCodingMathTransform(this),
                                         DefaultCartesianCS.DISPLAY));
        setModelCRS(getImageCRS());
    }

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
    public AffineTransform getImageToModelTransform() {
        return new AffineTransform();
    }

    @Override
    public CoordinateReferenceSystem getBaseCRS() {
        return baseCRS;
    }

    protected final void setBaseCRS(CoordinateReferenceSystem baseCRS) {
        Assert.notNull(baseCRS, "baseCRS");
        this.baseCRS = baseCRS;
    }

    @Override
    public CoordinateReferenceSystem getImageCRS() {
        return gridCRS;
    }

    protected final void setGridCRS(CoordinateReferenceSystem gridCRS) {
        Assert.notNull(gridCRS, "gridCRS");
        this.gridCRS = gridCRS;
    }

    @Override
    public CoordinateReferenceSystem getModelCRS() {
        return modelCRS;
    }

    protected void setModelCRS(CoordinateReferenceSystem modelCRS) {
        Assert.notNull(modelCRS, "modelCRS");
        this.modelCRS = modelCRS;
    }
}
