package org.esa.beam.framework.dataop.maptransf;

import org.esa.beam.framework.datamodel.GeoPos;

/**
 * <p><i>Note that this class is not yet public API and may change in future releases.</i></p>
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 * 
 * @deprecated since BEAM 4.7, no replacement.
 */
@Deprecated
public class DatumTransform {

    /**
     * Molodensky transform. (<a href="http://home.hiwaay.net/~taylorc/bookshelf/math-science/geodesy/datum/transform/molodensky/">source</a>)
     *
     * @param from     The geodetic position to be translated.
     * @param from_h   The height obove the "from" ellipsoid
     * @param from_a   The semi-major axis of the "from" ellipsoid.
     * @param from_f   Flattening of the "from" ellipsoid.
     * @param from_esq Eccentricity-squared of the "from" ellipsoid.
     * @param da       Change in semi-major axis length (meters); "to" minus "from"
     * @param df       Change in flattening; "to" minus "from"
     * @param dx       Change in x between "from" and "to" datum
     * @param dy       Change in y between "from" and "to" datum
     * @param dz       Change in z between "from" and "to" datum
     */
    public GeoPos transform(GeoPos from,
                            double from_h,
                            double from_a,
                            double from_f,
                            double from_esq,
                            double da,
                            double df,
                            double dx,
                            double dy,
                            double dz) {
        double slat = Math.sin(from.lat);
        double clat = Math.cos(from.lat);
        double slon = Math.sin(from.lon);
        double clon = Math.cos(from.lon);
        double ssqlat = slat * slat;
        double adb = 1.0 / (1.0 - from_f);  // "a divided by b"
        double rn = from_a / Math.sqrt(1.0 - from_esq * ssqlat);
        double rm = from_a * (1. - from_esq) / Math.pow((1.0 - from_esq * ssqlat), 1.5);

        double dlat = (((((-dx * slat * clon - dy * slat * slon) + dz * clat)
                  + (da * ((rn * from_esq * slat * clat) / from_a)))
                 + (df * (rm * adb + rn / adb) * slat * clat)))
               / (rm + from_h);

        double dlon = (-dx * slon + dy * clon) / ((rn + from_h) * clat);

        //double dh = (dx * clat * clon) + (dy * clat * slon) + (dz * slat)
        //     - (da * (from_a / rn)) + ((df * rn * ssqlat) / adb);
        // to_h = from_h + dh;
        return new GeoPos((float) (from.lat + dlat), (float) (from.lon + dlon));
    }
}
