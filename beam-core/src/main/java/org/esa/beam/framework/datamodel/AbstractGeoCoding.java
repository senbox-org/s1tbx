package org.esa.beam.framework.datamodel;

import com.bc.ceres.core.Assert;
import org.esa.beam.framework.dataio.ProductSubsetDef;
import org.esa.beam.jai.ImageManager;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultDerivedCRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.referencing.cs.DefaultCartesianCS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

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

    private CoordinateReferenceSystem imageCRS;
    private CoordinateReferenceSystem mapCRS;
    private CoordinateReferenceSystem geoCRS;
    private volatile MathTransform image2Map;

    protected AbstractGeoCoding() {
        setGeoCRS(DefaultGeographicCRS.WGS84);
        setImageCRS(createImageCRS(geoCRS, new GeoCodingMathTransform(this)));
        setMapCRS(geoCRS);

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
    public CoordinateReferenceSystem getImageCRS() {
        return imageCRS;
    }

    protected final void setImageCRS(CoordinateReferenceSystem imageCRS) {
        Assert.notNull(imageCRS, "imageCRS");
        this.imageCRS = imageCRS;
    }

    @Override
    public CoordinateReferenceSystem getMapCRS() {
        return mapCRS;
    }

    protected final void setMapCRS(CoordinateReferenceSystem mapCRS) {
        Assert.notNull(mapCRS, "mapCRS");
        this.mapCRS = mapCRS;
    }

    @Override
    public CoordinateReferenceSystem getGeoCRS() {
        return geoCRS;
    }

    public final void setGeoCRS(CoordinateReferenceSystem geoCRS) {
        Assert.notNull(geoCRS, "geoCRS");
        this.geoCRS = geoCRS;
    }


    @Override
    public MathTransform getImageToMapTransform() {
        if (image2Map == null) {
            synchronized (this) {
                if (image2Map == null) {
                    try {
                        image2Map = CRS.findMathTransform(imageCRS, mapCRS);
                    } catch (FactoryException e) {
                        throw new IllegalArgumentException(
                                "Not able to find a math transformation from image to map CRS.", e);
                    }
                }
            }
        }
        return image2Map;
    }

    /**
     * @deprecated since BEAM 4.7, use {@link ImageManager#getImageToModelTransform(GeoCoding)} instead
     */
    @Deprecated
    @Override
    public AffineTransform getImageToModelTransform() {
        return ImageManager.getImageToModelTransform(this);
    }

    /**
     * @deprecated since BEAM 4.7, use {@link ImageManager#getModelCrs(GeoCoding)} instead
     */
    @Deprecated
    @Override
    public CoordinateReferenceSystem getModelCRS() {
        return ImageManager.getModelCrs(this);
    }

    /**
     * @deprecated since BEAM 4.7, no replacement.
     *             Implementation does nothing.
     */
    @Deprecated
    protected void setModelCRS(CoordinateReferenceSystem modelCRS) {
    }

    /**
     * @deprecated since BEAM 4.7, use {@link #getMapCRS()} instead.
     */
    @Deprecated
    @Override
    public CoordinateReferenceSystem getBaseCRS() {
        return getMapCRS();
    }

    /**
     * @deprecated since BEAM 4.7, use {@link #setMapCRS(CoordinateReferenceSystem)} instead.
     */
    @Deprecated
    protected final void setBaseCRS(CoordinateReferenceSystem baseCRS) {
        setMapCRS(baseCRS);
    }

    /**
     * @deprecated since BEAM 4.7, use {@link #setImageCRS(CoordinateReferenceSystem)} instead.
     */
    @Deprecated
    protected final void setGridCRS(CoordinateReferenceSystem gridCRS) {
        setImageCRS(gridCRS);
    }

    protected static DefaultDerivedCRS createImageCRS(CoordinateReferenceSystem baseCRS,
                                                      MathTransform baseToDerivedTransform) {
        return new DefaultDerivedCRS("Image CS based on " + baseCRS.getName(),
                                     baseCRS,
                                     baseToDerivedTransform,
                                     DefaultCartesianCS.DISPLAY);
    }
}
