package org.esa.snap.dataio.netcdf.metadata.profiles.cf;

import junit.framework.TestCase;
import ucar.nc2.Attribute;

import java.util.ArrayList;
import java.util.List;

public class CfHdfGeoInfoExtractorTest extends TestCase {

    public void testExtractGeoInfo() throws Exception {
        List<Attribute> attributes = new ArrayList<Attribute>();

        final String attrString = "projeCTIon=GCTP_SNSOID\n\t" +
                "xdim=1200\n\t\t" +
                "yDiM=2400\n\t\t" +
                "upperleFT=(1.23,45.6)\n\t\t" +
                "LOWERRIGHT=(78.9,10.34)\n\t\t";

        attributes.add(new Attribute("dummy", "bla"));
        attributes.add(new Attribute("StructMetadata.0", attrString));
        attributes.add(new Attribute("bla", "blubb"));

        final CfHdfEosGeoInfoExtractor cfHdfEosGeoInfoExtractor = new CfHdfEosGeoInfoExtractor(attributes);
        cfHdfEosGeoInfoExtractor.extractInfo();
        assertNotNull(cfHdfEosGeoInfoExtractor.getProjection());
        assertEquals("GCTP_SNSOID", cfHdfEosGeoInfoExtractor.getProjection());
        assertEquals(1200, cfHdfEosGeoInfoExtractor.getxDim());
        assertEquals(2400, cfHdfEosGeoInfoExtractor.getyDim());
        assertEquals(1.23, cfHdfEosGeoInfoExtractor.getUlLon());
        assertEquals(45.6, cfHdfEosGeoInfoExtractor.getUlLat());
        assertEquals(78.9, cfHdfEosGeoInfoExtractor.getLrLon());
        assertEquals(10.34, cfHdfEosGeoInfoExtractor.getLrLat());
    }
}
