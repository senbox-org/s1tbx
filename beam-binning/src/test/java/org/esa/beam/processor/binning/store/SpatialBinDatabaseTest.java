package org.esa.beam.processor.binning.store;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import junit.framework.TestCase;

import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.processor.ProcessorException;
import org.esa.beam.framework.dataop.maptransf.Datum;
import org.esa.beam.processor.binning.L3Context;
import org.esa.beam.processor.binning.database.SpatialBinDatabase;


//  TODO Hey Marco, was macht diese Klasse denn so?

/*
 * Created by IntelliJ IDEA.
 * User: marcoz
 * Date: 18.07.2005
 * Time: 15:21:44
 * To change this template use File | Settings | File Templates.
 */
public class SpatialBinDatabaseTest extends TestCase {
    
    public void testCreate() throws ProcessorException, IOException {
        L3Context context = new L3Context();
        context.setMainParameter(new File("temp"), "", 100);
        context.setBorder(-90, 90, -180, 180);

        Product product = new Product("TestProduct", "type", 1000, 2000);
        GeoCoding geoCoding = new GeoCodingMock();
        product.setGeoCoding(geoCoding);
        new SpatialBinDatabase(context, product, Logger.getAnonymousLogger() );
//        spatialDb.processSpatialBinning();
        assertTrue(true);
    }

    static class GeoCodingMock implements GeoCoding {

        public boolean isCrossingMeridianAt180() {
            return false;
        }

        public Datum getDatum() {
            return Datum.WGS_84;
        }

        public boolean canGetPixelPos() {
            return true;
        }

        public boolean canGetGeoPos() {
            return true;
        }

        public PixelPos getPixelPos(GeoPos geoPos, PixelPos pixelPos) {
            return pixelPos;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public GeoPos getGeoPos(PixelPos pixelPos, GeoPos geoPos) {
            return geoPos;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public void dispose() {
        }
    }
}
