package org.esa.snap.core.dataio.geocoding;

import org.esa.snap.core.dataio.ProductSubsetDef;
import org.esa.snap.core.dataio.geocoding.util.RasterUtils;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Scene;
import org.esa.snap.core.dataop.maptransf.Datum;
import org.esa.snap.core.transform.GeoCodingMathTransform;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultDerivedCRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.referencing.cs.DefaultCartesianCS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public class ComponentGeoCoding implements GeoCoding {

    private static final GeoPos INVALID_GEO_POS = new GeoPos(Double.NaN, Double.NaN);
    private static final PixelPos INVALID_PIXEL_POS = new PixelPos(Double.NaN, Double.NaN);

    private final ForwardCoding forwardCoding;
    private final InverseCoding inverseCoding;
    private final GeoRaster geoRaster;
    private final GeoChecks geoChecks;

    private final CoordinateReferenceSystem imageCRS;
    private final CoordinateReferenceSystem mapCRS;
    private final CoordinateReferenceSystem geoCRS;
    private volatile MathTransform image2Map;

    /**
     * Constructs a GeoCoding with given GeoRaster, ForwardCoding and InverseCoding. No geoChecks will be performed during initialize phase.
     * Defaults to WGS84 CRS. Forward and/or Inverse coding can be null.
     *
     * @param geoRaster     the GeoRaster
     * @param forwardCoding the ForwardCoding, can be null
     * @param inverseCoding the InverseCoding, can be null
     */
    public ComponentGeoCoding(GeoRaster geoRaster, ForwardCoding forwardCoding, InverseCoding inverseCoding) {
        this(geoRaster, forwardCoding, inverseCoding, GeoChecks.NONE, DefaultGeographicCRS.WGS84);
    }

    /**
     * Constructs a GeoCoding with given GeoRaster, ForwardCoding, InverseCoding and GeoCheck definition to be executed during initialize phase.
     * Defaults to WGS84 CRS. Forward and/or Inverse coding can be null.
     *
     * @param geoRaster     the GeoRaster
     * @param forwardCoding the ForwardCoding, can be null
     * @param inverseCoding the InverseCoding, can be null
     * @param geoChecks     definition of GeoChecks to be executed during initialization
     */
    public ComponentGeoCoding(GeoRaster geoRaster, ForwardCoding forwardCoding, InverseCoding inverseCoding, GeoChecks geoChecks) {
        this(geoRaster, forwardCoding, inverseCoding, geoChecks, DefaultGeographicCRS.WGS84);
    }

    /**
     * Constructs a GeoCoding with given GeoRaster, ForwardCoding, InverseCoding and CRS. No GeoChecks will be performed during initialize phase.
     * Forward and/or Inverse coding can be null.
     *
     * @param geoRaster     the GeoRaster
     * @param forwardCoding the ForwardCoding, can be null
     * @param inverseCoding the InverseCoding, can be null
     * @param geoCRS        the CRS
     */
    public ComponentGeoCoding(GeoRaster geoRaster, ForwardCoding forwardCoding, InverseCoding inverseCoding, CoordinateReferenceSystem geoCRS) {
        this(geoRaster, forwardCoding, inverseCoding, GeoChecks.NONE, geoCRS);
    }

    /**
     * Constructs a GeoCoding with given GeoRaster, ForwardCoding, InverseCoding, GeoChecks to be performed during initialization and CRS.
     * Forward and/or Inverse coding can be null.
     *
     * @param geoRaster the GeoRaster
     * @param forwardCoding the ForwardCoding, can be null
     * @param inverseCoding the InverseCoding, can be null
     * @param geoChecks definition of GeoChecks to be executed during initialization
     * @param geoCRS  the CRS
     */
    public ComponentGeoCoding(GeoRaster geoRaster, ForwardCoding forwardCoding, InverseCoding inverseCoding,
                              GeoChecks geoChecks, CoordinateReferenceSystem geoCRS) {
        this.forwardCoding = forwardCoding;
        this.inverseCoding = inverseCoding;
        this.geoRaster = geoRaster;
        this.geoCRS = geoCRS;
        this.mapCRS = geoCRS;
        this.imageCRS = createImageCRS(getMapCRS(), new GeoCodingMathTransform(this));
        this.geoChecks = geoChecks;
    }

    protected static DefaultDerivedCRS createImageCRS(CoordinateReferenceSystem baseCRS,
                                                      MathTransform baseToDerivedTransform) {
        return new DefaultDerivedCRS("Image CS based on " + baseCRS.getName(),
                baseCRS,
                baseToDerivedTransform,
                DefaultCartesianCS.DISPLAY);
    }

    @Override
    public boolean isCrossingMeridianAt180() {
        throw new NotImplementedException();
    }

    @Override
    public boolean canGetPixelPos() {
        return inverseCoding != null;
    }

    @Override
    public boolean canGetGeoPos() {
        return forwardCoding != null;
    }

    @Override
    public PixelPos getPixelPos(GeoPos geoPos, PixelPos pixelPos) {
        if (inverseCoding == null) {
            return INVALID_PIXEL_POS;
        }
        return inverseCoding.getPixelPos(geoPos, pixelPos);
    }

    @Override
    public GeoPos getGeoPos(PixelPos pixelPos, GeoPos geoPos) {
        if (forwardCoding == null) {
            return INVALID_GEO_POS;
        }

        return forwardCoding.getGeoPos(pixelPos, geoPos);
    }

    /**
     * Transfers the geo-coding of the {@link Scene srcScene} to the {@link Scene destScene} with respect to the given
     * {@link ProductSubsetDef subsetDef}.
     *
     * @param srcScene  the source scene
     * @param destScene the destination scene
     * @param subsetDef the definition of the subset, may be <code>null</code>
     * @return true, if the geo-coding could be transferred.
     */
    public boolean transferGeoCoding(Scene srcScene, Scene destScene, ProductSubsetDef subsetDef) {
        // @todo 1 tb/tb check if we can implement a general solution here. If not, we have to define this class as abstract 2019-08-23
        throw new NotImplementedException();
    }

    @Override
    public void dispose() {
        if (forwardCoding != null) {
            forwardCoding.dispose();
        }
        if (inverseCoding != null) {
            inverseCoding.dispose();
        }
    }

    /**
     * Gets the datum, the reference point or surface against which {@link GeoPos} measurements are made.
     *
     * @return the datum
     * @deprecated use the datum of the associated {@link #getMapCRS() map CRS}.
     */
    @Override
    @Deprecated
    public Datum getDatum() {
        throw new NotImplementedException();
    }

    @Override
    public CoordinateReferenceSystem getImageCRS() {
        return imageCRS;
    }

    @Override
    public CoordinateReferenceSystem getMapCRS() {
        return mapCRS;
    }

    @Override
    public CoordinateReferenceSystem getGeoCRS() {
        return geoCRS;
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

    public void initialize() {
        boolean crossesAntiMeridian = false;
        PixelPos[] poleLocations = new PixelPos[0];

        if (geoChecks != GeoChecks.NONE) {
            crossesAntiMeridian = RasterUtils.containsAntiMeridian(geoRaster.getLongitudes(), geoRaster.getRasterWidth());
            if (crossesAntiMeridian && geoChecks == GeoChecks.POLES) {
                poleLocations = RasterUtils.getPoleLocations(geoRaster);
            }
        }

        if (forwardCoding != null) {
            forwardCoding.initialize(geoRaster, crossesAntiMeridian, poleLocations);
        }
        if (inverseCoding != null) {
            inverseCoding.initialize(geoRaster, crossesAntiMeridian, poleLocations);
        }
    }

    // package access for testing only tb 2019-11-18
    GeoChecks getGeoChecks() {
        return geoChecks;
    }

    // package access for testing only tb 2019-11-18
    ForwardCoding getForwardCoding() {
        return forwardCoding;
    }

    // package access for testing only tb 2019-11-18
    InverseCoding getInverseCoding() {
        return inverseCoding;
    }
}
