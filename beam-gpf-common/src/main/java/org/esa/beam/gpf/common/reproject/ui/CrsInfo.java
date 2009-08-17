package org.esa.beam.gpf.common.reproject.ui;

import org.opengis.referencing.crs.ProjectedCRS;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.FactoryException;
import org.geotools.referencing.CRS;

import java.util.List;
import java.util.Set;
import java.util.Collections;
import java.util.ArrayList;

/**
 * @author Marco Peters
* @version $ Revision $ Date $
* @since BEAM 4.6
*/
class CrsInfo {

    private final String epsgCode;
    private final ProjectedCRS crs;

    CrsInfo(String epsgCode, ProjectedCRS crs) {
        this.epsgCode = epsgCode;
        this.crs = crs;
    }

    public String getEpsgCode() {
        return epsgCode;
    }

    public ProjectedCRS getCrs() {
        return crs;
    }

    @Override
    public String toString() {
        return epsgCode + " : " + crs.getName().getCode();
    }

    static List<CrsInfo> generateSupportedCRSList() {
        // todo - (mp/mz) this takes much time (5 sec.) try to speed up
        final CRSAuthorityFactory authorityFactory = CRS.getAuthorityFactory(true);
        Set<String> codes;
        try {
            codes = authorityFactory.getAuthorityCodes(ProjectedCRS.class);
        } catch (FactoryException ignore) {
            return Collections.EMPTY_LIST;
        }
        List<CrsInfo> crsList = new ArrayList<CrsInfo>(codes.size());
        for (String code : codes) {
            try {
                CoordinateReferenceSystem crs = authorityFactory.createCoordinateReferenceSystem(code);
                crsList.add(new CrsInfo(code, (ProjectedCRS) crs));
            } catch (FactoryException ignore) {
                // bad CRS --> ignore
            }
        }
        return crsList;
    }
}
