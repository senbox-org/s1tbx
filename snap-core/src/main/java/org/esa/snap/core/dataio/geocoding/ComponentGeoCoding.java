package org.esa.snap.core.dataio.geocoding;

import org.esa.snap.core.dataio.ProductSubsetDef;
import org.esa.snap.core.dataio.geocoding.util.RasterUtils;
import org.esa.snap.core.datamodel.AbstractGeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Scene;
import org.esa.snap.core.dataop.maptransf.Datum;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public class ComponentGeoCoding extends AbstractGeoCoding {

    private static final GeoPos INVALID_GEO_POS = new GeoPos(Double.NaN, Double.NaN);
    private static final PixelPos INVALID_PIXEL_POS = new PixelPos(Double.NaN, Double.NaN);

    private final ForwardCoding forwardCoding;
    private final InverseCoding inverseCoding;
    private final GeoRaster geoRaster;
    private final GeoChecks geoChecks;

    private boolean isInitialized;
    private boolean isCrossingAntiMeridian;

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
     * @param geoRaster     the GeoRaster
     * @param forwardCoding the ForwardCoding, can be null
     * @param inverseCoding the InverseCoding, can be null
     * @param geoChecks     definition of GeoChecks to be executed during initialization
     * @param geoCRS        the CRS
     */
    public ComponentGeoCoding(GeoRaster geoRaster, ForwardCoding forwardCoding, InverseCoding inverseCoding,
                              GeoChecks geoChecks, CoordinateReferenceSystem geoCRS) {
        super(geoCRS);
        this.forwardCoding = forwardCoding;
        this.inverseCoding = inverseCoding;
        this.geoRaster = geoRaster;
        this.geoChecks = geoChecks;

        isInitialized = false;
        isCrossingAntiMeridian = false;
    }

    @Override
    public boolean isCrossingMeridianAt180() {
        if (!isInitialized) {
            throw new IllegalStateException("Geocoding is not initialized.");
        }

        return isCrossingAntiMeridian;
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
        // geo-subsetting
        // subsampling
        //
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

    public void initialize() {
        PixelPos[] poleLocations = new PixelPos[0];

        if (geoChecks != GeoChecks.NONE) {
            isCrossingAntiMeridian = RasterUtils.containsAntiMeridian(geoRaster.getLongitudes(), geoRaster.getRasterWidth());
            if (isCrossingAntiMeridian && geoChecks == GeoChecks.POLES) {
                poleLocations = RasterUtils.getPoleLocations(geoRaster);
            }
        }

        if (forwardCoding != null) {
            forwardCoding.initialize(geoRaster, isCrossingAntiMeridian, poleLocations);
        }
        if (inverseCoding != null) {
            inverseCoding.initialize(geoRaster, isCrossingAntiMeridian, poleLocations);
        }

        isInitialized = true;
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
