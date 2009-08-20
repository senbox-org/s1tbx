package org.esa.beam.gpf.common.reproject.ui;

import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.geotools.factory.Hints;
import org.geotools.metadata.iso.citation.Citations;
import org.geotools.referencing.ReferencingFactoryFinder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.referencing.crs.DefaultProjectedCRS;
import org.geotools.referencing.cs.DefaultCartesianCS;
import org.geotools.referencing.factory.FallbackAuthorityFactory;
import org.geotools.referencing.factory.wms.AutoCRSFactory;
import org.geotools.referencing.operation.DefaultMathTransformFactory;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchIdentifierException;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.ProjectedCRS;
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.operation.MathTransform;

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

    private static class StaticCrsInfo extends CrsInfo {

        private final String desc;
        private final CoordinateReferenceSystem crs;

        StaticCrsInfo(String desc, CoordinateReferenceSystem crs) {
            super(desc, null);
            this.desc = desc;
            this.crs = crs;
        }

        @Override
        public CoordinateReferenceSystem getCrs(Product product) throws FactoryException {
            return crs;
        }

        @Override
        public String toString() {
            return desc;
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

//        final CRSAuthorityFactory authorityFactory = CRS.getAuthorityFactory(true);
        Set<String> codes;
        try {
            codes = crsAuthorityFactory.getAuthorityCodes(ProjectedCRS.class);
        } catch (FactoryException ignore) {
            return Collections.EMPTY_LIST;
        }
        List<CrsInfo> crsList = new ArrayList<CrsInfo>(codes.size());
        for (String code : codes) {
            crsList.add(new CrsInfo(AUTHORITY+":"+code, crsAuthorityFactory));
        }
        AutoCRSFactory autoCRSFactory = new AutoCRSFactory();
        Set<String> autoCodes;
        try {
            autoCodes = autoCRSFactory.getAuthorityCodes(ProjectedCRS.class);
        } catch (FactoryException ignore) {
            return crsList;
        }
        for (String code : autoCodes) {
            crsList.add(new CrsInfo.AutoCrsInfo("AUTO:"+code, autoCRSFactory));
        }
        crsList.add(new StaticCrsInfo("BEAM: Geographic Lat/Lon WGS 84", createLatLonCRS()));
        Collections.sort(crsList);
        return crsList;
    }
    
    private static CoordinateReferenceSystem createLatLonCRS() {
        try {
            final DefaultMathTransformFactory factory = new DefaultMathTransformFactory();
            final ParameterValueGroup valueGroup = factory.getDefaultParameters("Plate_Carree");
            final Ellipsoid ellipsoid = DefaultGeographicCRS.WGS84.getDatum().getEllipsoid();
            valueGroup.parameter("semi_minor").setValue(ellipsoid.getSemiMajorAxis());
            valueGroup.parameter("semi_major").setValue(ellipsoid.getSemiMajorAxis());
            final MathTransform transform = factory.createParameterizedTransform(valueGroup);

            return new DefaultProjectedCRS("Geographic Lat/Lon", DefaultGeographicCRS.WGS84,
                                                               transform, DefaultCartesianCS.PROJECTED);
        } catch (NoSuchIdentifierException e) {
            e.printStackTrace();
        } catch (FactoryException e) {
            e.printStackTrace();
        }
        return null;
    }

}
