package org.esa.beam.framework.ui.crs;

import org.esa.beam.framework.datamodel.GeoPos;
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
    private final String crsCode;
    private final CRSAuthorityFactory factory;

    CrsInfo(String crsCode, CRSAuthorityFactory factory) {
        this.crsCode = crsCode;
        this.factory = factory;
    }

    public CoordinateReferenceSystem getCrs(GeoPos referencePos) throws FactoryException {
        return factory.createCoordinateReferenceSystem(crsCode);
    }

    @Override
    public int compareTo(CrsInfo o) {
        return crsCode.compareTo(o.crsCode);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CrsInfo)) {
            return false;
        }

        CrsInfo crsInfo = (CrsInfo) o;

        return !(crsCode != null ? !crsCode.equals(crsInfo.crsCode) : crsInfo.crsCode != null);

    }

    @Override
    public int hashCode() {
        return crsCode != null ? crsCode.hashCode() : 0;
    }

    @Override
    public String toString() {
        String crsDescription = crsCode + " - ";
        try {
            crsDescription += factory.getDescriptionText(crsCode).toString();
        } catch (Exception e) {
            crsDescription += e.getLocalizedMessage();
        }
        return crsDescription;
    }

    public String getDescription() {
        try {
            return getCrs(null).toString();
        } catch (FactoryException e) {
            return e.getMessage();
        }
    }

    private static class AutoCrsInfo extends CrsInfo {

        AutoCrsInfo(String epsgCode, CRSAuthorityFactory factory) {
            super(epsgCode, factory);
        }

        @Override
        public CoordinateReferenceSystem getCrs(GeoPos referencePos) throws FactoryException {
            if (referencePos == null) {
                referencePos = new GeoPos(0, 0);
            }

            String code = String.format("%s,%s,%s", super.crsCode, referencePos.lon, referencePos.lat);
            return super.factory.createCoordinateReferenceSystem(code);
        }

        @Override
        public String toString() {
            String crsDescription = super.crsCode + " - ";
            try {
                String code = super.crsCode + ",0,0";
                crsDescription += super.factory.getDescriptionText(code).toString();
            } catch (Exception e) {
                crsDescription += e.getLocalizedMessage();
            }
            return crsDescription;
        }

        @Override
        public String getDescription() {
            return toString();
        }


    }

    static List<CrsInfo> generateCRSList() {
        // todo - (mp/mz) this method takes time (2 sec.) try to speed up

        Hints hints = new Hints(Hints.FORCE_LONGITUDE_FIRST_AXIS_ORDER, true);
        Set<CRSAuthorityFactory> factories = ReferencingFactoryFinder.getCRSAuthorityFactories(hints);
        final List<CRSAuthorityFactory> filtered = new ArrayList<CRSAuthorityFactory>();
        for (final CRSAuthorityFactory factory : factories) {
            if (Citations.identifierMatches(factory.getAuthority(), AUTHORITY)) {
                filtered.add(factory);
            }
        }
        CRSAuthorityFactory crsAuthorityFactory = FallbackAuthorityFactory.create(CRSAuthorityFactory.class, filtered);

        Set<String> codes = new HashSet<String>();
        List<CrsInfo> crsList = new ArrayList<CrsInfo>(1024);
        retrieveCodes(codes, GeodeticCRS.class, crsAuthorityFactory);
        retrieveCodes(codes, ProjectedCRS.class, crsAuthorityFactory);
        for (String code : codes) {
            final String authCode = String.format("%s:%s", AUTHORITY, code);
            crsList.add(new CrsInfo(authCode, crsAuthorityFactory));
        }
        codes.clear();
        AutoCRSFactory autoCRSFactory = new AutoCRSFactory();
        retrieveCodes(codes, ProjectedCRS.class, autoCRSFactory);
        for (String code : codes) {
            final String authCode = String.format("AUTO:%s", code);
            crsList.add(new AutoCrsInfo(authCode, autoCRSFactory));
        }
        Collections.sort(crsList);
        return crsList;
    }

    private static void retrieveCodes(Set<String> codes, Class<? extends CoordinateReferenceSystem> crsType,
                                      CRSAuthorityFactory factory) {
        Set<String> localCodes;
        try {
            localCodes = factory.getAuthorityCodes(crsType);
        } catch (FactoryException ignore) {
            return;
        }
        codes.addAll(localCodes);
    }
}
