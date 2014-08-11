package org.esa.snap.eo;

import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.snap.gpf.TileGeoreferencing;

/**
 * Created with IntelliJ IDEA.
 * User: lveci
 * Date: 03/01/13
 * Time: 10:01 AM
 * To change this template use File | Settings | File Templates.
 */
public class LocalGeometry {

    public double leftPointLat;
    public double leftPointLon;
    public double rightPointLat;
    public double rightPointLon;
    public double upPointLat;
    public double upPointLon;
    public double downPointLat;
    public double downPointLon;
    public double[] sensorPos;
    public double[] centrePoint;

    public LocalGeometry(final int x, final int y, final TileGeoreferencing tileGeoRef,
                         final double[] earthPoint, final double[] sensorPos) {
        final GeoPos geo = new GeoPos();

        tileGeoRef.getGeoPos(x - 1, y, geo);
        this.leftPointLat = geo.lat;
        this.leftPointLon = geo.lon;

        tileGeoRef.getGeoPos(x + 1, y, geo);
        this.rightPointLat = geo.lat;
        this.rightPointLon = geo.lon;

        tileGeoRef.getGeoPos(x, y - 1, geo);
        this.upPointLat = geo.lat;
        this.upPointLon = geo.lon;

        tileGeoRef.getGeoPos(x, y + 1, geo);
        this.downPointLat = geo.lat;
        this.downPointLon = geo.lon;
        this.centrePoint = earthPoint;
        this.sensorPos = sensorPos;
    }

    public LocalGeometry(final double lat, final double lon, final double delLat, final double delLon,
                         final double[] earthPoint, final double[] sensorPos) {

        this.leftPointLat = lat;
        this.leftPointLon = lon - delLon;

        this.rightPointLat = lat;
        this.rightPointLon = lon + delLon;

        this.upPointLat = lat - delLat;
        this.upPointLon = lon;

        this.downPointLat = lat + delLat;
        this.downPointLon = lon;
        this.centrePoint = earthPoint;
        this.sensorPos = sensorPos;
    }

}
