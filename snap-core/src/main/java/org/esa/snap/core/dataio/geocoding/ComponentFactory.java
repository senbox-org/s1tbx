package org.esa.snap.core.dataio.geocoding;


import org.esa.snap.core.dataio.geocoding.forward.PixelForward;
import org.esa.snap.core.dataio.geocoding.forward.PixelInterpolatingForward;
import org.esa.snap.core.dataio.geocoding.forward.TiePointBilinearForward;
import org.esa.snap.core.dataio.geocoding.forward.TiePointSplineForward;
import org.esa.snap.core.dataio.geocoding.inverse.PixelGeoIndexInverse;
import org.esa.snap.core.dataio.geocoding.inverse.PixelQuadTreeInverse;
import org.esa.snap.core.dataio.geocoding.inverse.TiePointInverse;

public class ComponentFactory {

    public static final String FWD_PIXEL = "FWD_PIXEL";
    public static final String FWD_PIXEL_INTERPOLATING = "FWD_PIXEL_INTERPOLATING";
    public static final String FWD_TIE_POINT_BILINEAR = "FWD_TIE_POINT_BILINEAR";
    public static final String FWD_TIE_POINT_SPLINE = "FWD_TIE_POINT_SPLINE";

    public static final String INV_PIXEL_QUAD_TREE = "INV_PIXEL_QUAD_TREE";
    public static final String INV_PIXEL_QUAD_TREE_INTERPOLATING = "INV_PIXEL_QUAD_TREE_INTERPOLATING";
    public static final String INV_PIXEL_GEO_INDEX = "INV_PIXEL_GEO_INDEX";
    public static final String INV_PIXEL_GEO_INDEX_INTERPOLATING = "INV_PIXEL_GEO_INDEX_INTERPOLATING";
    public static final String INV_TIE_POINT = "INV_TIE_POINT";

    public static ForwardCoding getForward(String key) {
        switch (key) {
            case FWD_PIXEL:
                return new PixelForward();

            case FWD_PIXEL_INTERPOLATING:
                return new PixelInterpolatingForward();

            case FWD_TIE_POINT_BILINEAR:
                return new TiePointBilinearForward();

            case FWD_TIE_POINT_SPLINE:
                return new TiePointSplineForward();

            default:
                throw new IllegalArgumentException("unknown forward coding: " + key);
        }
    }

    public static InverseCoding getInverse(String key) {
        switch(key) {
            case INV_PIXEL_QUAD_TREE:
                return new PixelQuadTreeInverse(false);

            case INV_PIXEL_QUAD_TREE_INTERPOLATING:
                return new PixelQuadTreeInverse(true);

            case INV_PIXEL_GEO_INDEX:
                return new PixelGeoIndexInverse(false);

            case INV_PIXEL_GEO_INDEX_INTERPOLATING:
                return new PixelGeoIndexInverse(true);

            case INV_TIE_POINT:
                return new TiePointInverse();

            default:
                throw new IllegalArgumentException("unknown inverse coding: " + key);
        }
    }
}
