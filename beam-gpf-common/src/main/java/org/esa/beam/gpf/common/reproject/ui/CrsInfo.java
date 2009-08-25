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
import java.util.HashSet;
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

        Set<String> codes = new HashSet<String>();
        List<CrsInfo> crsList = new ArrayList<CrsInfo>(1024);
        retrieveCodes(codes, GeodeticCRS.class, crsAuthorityFactory);
        retrieveCodes(codes, ProjectedCRS.class, crsAuthorityFactory);
        for (String code : codes) {
            crsList.add(new CrsInfo(AUTHORITY+":"+code, crsAuthorityFactory));
        }
        codes.clear();
        AutoCRSFactory autoCRSFactory = new AutoCRSFactory();
        retrieveCodes(codes, ProjectedCRS.class, autoCRSFactory);
        for (String code : codes) {
            crsList.add(new AutoCrsInfo("AUTO:"+code, autoCRSFactory));
        }
        Collections.sort(crsList);
        return crsList;
    }
    
    private static void retrieveCodes(Set<String> codes, Class<? extends CoordinateReferenceSystem> crsType, CRSAuthorityFactory factory) {
        Set<String> localCodes;
        try {
            localCodes = factory.getAuthorityCodes(crsType);
        } catch (FactoryException ignore) {
            return;
        }
        codes.addAll(localCodes);
    }
}
