package org.esa.beam.gpf.common.reproject.ui;

import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.geotools.factory.Hints;
import org.geotools.metadata.iso.citation.Citations;
import org.geotools.referencing.ReferencingFactoryFinder;
import org.geotools.referencing.factory.FallbackAuthorityFactory;
import org.geotools.referencing.factory.wms.AutoCRSFactory;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.GeodeticCRS;
import org.opengis.referencing.crs.ProjectedCRS;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author Marco Peters
 * @author Marco Zühlke
* @version $ Revision $ Date $
* @since BEAM 4.7
*/
class CrsInfo implements Comparable<CrsInfo> {

    private static final String AUTHORITY = "EPSG";
    private final String epsgCode;
    private final CRSAuthorityFactory factory;

    CrsInfo(String epsgCode, CRSAuthorityFactory factory) {
        this.epsgCode = epsgCode;
        this.factory = factory;
    }
    
    public String getEpsgCode() {
        return epsgCode;
    }

    public CoordinateReferenceSystem getCrs(Product product) throws FactoryException {
        return factory.createCoordinateReferenceSystem(epsgCode);
    }
    
    @Override
    public int compareTo(CrsInfo o) {
        return epsgCode.compareTo(o.epsgCode);
    }

    @Override
    public String toString() {
        String crsDescription = epsgCode + " - ";
        try {
            crsDescription += factory.getDescriptionText(epsgCode).toString();
        } catch (Exception e) {
            crsDescription += e.getLocalizedMessage();
        }
        return crsDescription;
    }
    
    private static class AutoCrsInfo extends CrsInfo {

        AutoCrsInfo(String epsgCode, CRSAuthorityFactory factory) {
            super(epsgCode, factory);
        }

        public CoordinateReferenceSystem getCrs(Product product) throws FactoryException {
            PixelPos pixelPos = new PixelPos(product.getSceneRasterWidth()/2, product.getSceneRasterHeight()/2);
            GeoPos geoPos = product.getGeoCoding().getGeoPos(pixelPos, null);
            String code = super.epsgCode+","+geoPos.lon+","+geoPos.lat;
            return super.factory.createCoordinateReferenceSystem(code);
        }

        @Override
        public String toString() {
            String crsDescription = super.epsgCode + " - ";
            try {
                String code = super.epsgCode + ",0,0";
                crsDescription += super.factory.getDescriptionText(code).toString();
            } catch (Exception e) {
                crsDescription += e.getLocalizedMessage();
            }
            return crsDescription;
        }
    }

    static List<CrsInfo> generateCRSList() {
        // todo - (mp/mz) this method takes time (2 sec.) try to speed up

        Hints hints = new Hints(Hints.FORCE_LONGITUDE_FIRST_AXIS_ORDER, true);
        Set<CRSAuthorityFactory> factories = ReferencingFactoryFinder.getCRSAuthorityFactories(hints);
        final List<CRSAuthorityFactory> filtered = new ArrayList<CRSAuthorityFactory>();
        for (final CRSAuthorityFactory factory : factories) {
            if (Citations.identifierMatches(factory.getAuthority(), AUTHORITY )) {
                filtered.add(factory);
            }
        }
        CRSAuthorityFactory crsAuthorityFactory = FallbackAuthorityFactory.create(CRSAuthorityFactory.class, filtered);

        List<CrsInfo> crsList = new ArrayList<CrsInfo>(1024);
        createCrsInfos(crsList, GeodeticCRS.class, AUTHORITY, crsAuthorityFactory);
        createCrsInfos(crsList, ProjectedCRS.class, AUTHORITY, crsAuthorityFactory);
        createCrsInfos(crsList, ProjectedCRS.class, "AUTO", new AutoCRSFactory());
        Collections.sort(crsList);
        return crsList;
    }
    
    private static void createCrsInfos(List<CrsInfo> crsList, Class<? extends CoordinateReferenceSystem> crsType, String authority, CRSAuthorityFactory factory) {
        Set<String> codes;
        try {
            codes = factory.getAuthorityCodes(crsType);
        } catch (FactoryException ignore) {
            return;
        }
        for (String code : codes) {
            crsList.add(new CrsInfo(authority+":"+code, factory));
        }
    }
}
