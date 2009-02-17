package org.esa.beam.visat.actions;

import junit.framework.TestCase;
import org.esa.beam.framework.dataio.ProductSubsetDef;
import org.esa.beam.framework.datamodel.AbstractGeoCoding;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Scene;
import org.esa.beam.framework.dataop.maptransf.Datum;
import org.geotools.referencing.CRS;
import org.geotools.referencing.factory.ReferencingObjectFactory;
import org.geotools.referencing.crs.DefaultDerivedCRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.referencing.cs.DefaultCartesianCS;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.GeneralDirectPosition;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.datum.DatumFactory;
import org.opengis.referencing.cs.EllipsoidalCS;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.crs.SingleCRS;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.geometry.DirectPosition;

import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.Point;
import java.util.HashMap;

/**
 * todo - add API doc
 *
 * @author Marco Peters
 * @version $Revision: $ $Date: $
 * @since BEAM 4.6
 */
public class CRSTest extends TestCase {

    public void testIt() throws FactoryException, TransformException {

        AffineTransform at = new AffineTransform();
        at.translate(-40, +10);
        assertEquals(new Point(-40, 10),at.transform(new Point(0, 0),null));

        GeoCoding geoCoding = new AffineGeoCoding(at);
        assertEquals(new GeoPos(10, -40),geoCoding.getGeoPos(new PixelPos(0,0), new GeoPos()));
        assertEquals(new PixelPos(0,0),geoCoding.getPixelPos(new GeoPos(10, -40), new PixelPos()));

        GeographicCRS geoCRS = DefaultGeographicCRS.WGS84;
        SingleCRS gridCRS = new DefaultDerivedCRS("xyz",
                                                  geoCRS,
                                                  new GeoCodingMathTransform(geoCoding, GeoCodingMathTransform.Mode.G2P),
                                                  DefaultCartesianCS.GRID);

        assertEquals(geoCRS.getDatum(), gridCRS.getDatum());

        MathTransform transform = CRS.findMathTransform(gridCRS, geoCRS);
        assertNotNull(transform);

        DirectPosition position;
        position = transform.transform(new GeneralDirectPosition(0, 0), null);
        assertNotNull(position);
        assertEquals(new GeneralDirectPosition(-40, 10), position);

        //assertEquals();
        position = transform.transform(new DirectPosition2D(gridCRS, 1, 1), null);
        assertEquals(new GeneralDirectPosition(-39, 11), position);

        transform = CRS.findMathTransform(gridCRS, gridCRS);
        assertNotNull(transform);
    }

    private static class AffineGeoCoding extends AbstractGeoCoding {

        final AffineTransform at;

        private AffineGeoCoding(AffineTransform at) {
            this.at = at;
        }

        public boolean transferGeoCoding(Scene srcScene, Scene destScene, ProductSubsetDef subsetDef) {
            return false;
        }

        public boolean isCrossingMeridianAt180() {
            return false;
        }

        public boolean canGetPixelPos() {
            return true;
        }

        public boolean canGetGeoPos() {
            return true;
        }

        public PixelPos getPixelPos(GeoPos geoPos, PixelPos pixelPos) {
            final Point2D point2D;
            try {
                point2D = at.inverseTransform(new Point2D.Double(geoPos.lon, geoPos.lat), null);
            } catch (NoninvertibleTransformException e) {
                throw new IllegalStateException(e);
            }
            pixelPos.x = (float) point2D.getX();
            pixelPos.y = (float) point2D.getY();
            return pixelPos;
        }

        public GeoPos getGeoPos(PixelPos pixelPos, GeoPos geoPos) {
            final Point2D point2D = at.transform(new Point2D.Double(pixelPos.x, pixelPos.y), null);
            geoPos.lon = (float) point2D.getX();
            geoPos.lat = (float) point2D.getY();
            return geoPos;
        }

        public Datum getDatum() {
            return Datum.WGS_84;
        }

        public void dispose() {
        }
    }
}
