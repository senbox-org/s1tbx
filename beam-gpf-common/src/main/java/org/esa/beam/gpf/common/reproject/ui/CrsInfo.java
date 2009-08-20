package org.esa.beam.gpf.common.reproject.ui;

import org.geotools.metadata.iso.citation.Citations;
import org.geotools.referencing.ReferencingFactoryFinder;
import org.geotools.referencing.factory.FallbackAuthorityFactory;
import org.geotools.factory.Hints;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.ProjectedCRS;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author Marco Peters
* @version $ Revision $ Date $
* @since BEAM 4.6
*/
class CrsInfo {

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

    public CoordinateReferenceSystem getCrs() throws FactoryException {
        return factory.createCoordinateReferenceSystem(epsgCode);
    }

    @Override
    public String toString() {
        String crsDescription = AUTHORITY + ":" + epsgCode + " - ";
        try {
            crsDescription += factory.getDescriptionText(epsgCode).toString();
        } catch (Exception e) {
            crsDescription += e.getLocalizedMessage();
        }
        return crsDescription;
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

//        final CRSAuthorityFactory authorityFactory = CRS.getAuthorityFactory(true);
        Set<String> codes;
        try {
            codes = crsAuthorityFactory.getAuthorityCodes(ProjectedCRS.class);
        } catch (FactoryException ignore) {
            return Collections.EMPTY_LIST;
        }
        List<CrsInfo> crsList = new ArrayList<CrsInfo>(codes.size());
        for (String code : codes) {
            crsList.add(new CrsInfo(code, crsAuthorityFactory));
        }
        return crsList;
    }
}
