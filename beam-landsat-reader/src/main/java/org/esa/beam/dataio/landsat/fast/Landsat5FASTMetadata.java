package org.esa.beam.dataio.landsat.fast;

import org.esa.beam.dataio.landsat.GeoPoint;
import org.esa.beam.dataio.landsat.GeometricData;
import org.esa.beam.dataio.landsat.LandsatConstants;
import org.esa.beam.dataio.landsat.LandsatHeader;
import org.esa.beam.dataio.landsat.LandsatLoc;
import org.esa.beam.dataio.landsat.LandsatTMBand;
import org.esa.beam.dataio.landsat.LandsatTMMetadata;
import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;

import java.util.ArrayList;
import java.util.List;

/**
 * The class <code>Landsat5FASTMetadata</code> is used to store the Metadata of a Landsat TM FAST format product
 *
 * @author Christian Berwanger (ai0263@umwelt-campus.de)
 */
public final class Landsat5FASTMetadata extends LandsatTMMetadata {

    private static final String EARTH_SUN_DISTANCE = "Earth Sun Distance";
    private static final String RADIANCE_DESCRIPTION = "The LANDSAT TM radiance values";
    private static final String RADIANCE = "Radiance";
    private static final String METADATA_GEOMETRIC_NODE_DESCRIPTION = "The LANDSAT TM geometric values";
    private static final String METADATA_ADMINISTRATIV_NODE_DESCRIPTION = "The LANDSAT TM header information";
    private static final String GEO_METADATA_DESC = "Geometric";
    private static final String ADMIN_METADATA_DESC = "Header information";

    /**
     * @param header
     * @param landsatBands
     */
    public Landsat5FASTMetadata(LandsatHeader header, LandsatTMBand[] landsatBands) {
        addElement((setAdminstrationMetadata(header)));
        addElement((setGeometricMetadata(header)));
        addElement((setRadianceMetadata(header, landsatBands)));
    }

    /**
     * stores the geometric data of the complete scene in a metadata attribute
     *
     * @param landsatHeader
     *
     * @return geometric metadata element
     */
    private MetadataElement setGeometricMetadata(LandsatHeader landsatHeader) {
        MetadataElement geo = new MetadataElement(GEO_METADATA_DESC);
        geo.setDescription(METADATA_GEOMETRIC_NODE_DESCRIPTION);
        final GeometricData geoData = landsatHeader.getGeoData();
        List<MetadataAttribute> attributContainer = new ArrayList<MetadataAttribute>();

        attributContainer.add(createAttribute(LandsatConstants.DATUM_SHORT, geoData.getEllipsoid(),
                                              LandsatConstants.DATUM_DESCRIPTION));
        attributContainer.add(createAttribute(LandsatConstants.OFFSET_SHORT, geoData.getHorizontalOffset(),
                                              LandsatConstants.OFFSET_DESCRIPTION, LandsatConstants.Unit.PIXEL));
        attributContainer.add(createAttribute(LandsatConstants.PROJECTION_SHORT, geoData.getMapProjection(),
                                              LandsatConstants.PROJECTION_DESCRIPTION));
        attributContainer.add(createAttribute(LandsatConstants.PROJECTION_ID_SHORT,
                                              geoData.getProjectionNumber(),
                                              LandsatConstants.PROJECTION_ID_DESCRIPTION));
        attributContainer.add(createAttribute(LandsatConstants.MAP_ZONE_SHORT, geoData.getMapZoneNumber(),
                                              LandsatConstants.MAP_ZONE_DESCRIPTION));
        attributContainer.add(createAttribute(LandsatConstants.SEMI_MAJ_SHORT, geoData.getSemiMajorAxis(),
                                              LandsatConstants.SEMI_MAJ_DESCRIPTION, LandsatConstants.Unit.METER));
        attributContainer.add(createAttribute(LandsatConstants.SEMI_MIN_SHORT, geoData.getSemiMinorAxis(),
                                              LandsatConstants.SEMI_MIN_DESCRIPTION, LandsatConstants.Unit.METER));
        attributContainer.add(createAttribute(LandsatConstants.SUN_AZIMUTH_SHORT,
                                              geoData.getSunAzimuthAngle(),
                                              LandsatConstants.SUN_AZIMUTH_DESCRIPTION, LandsatConstants.Unit.ANGLE));
        attributContainer.add(createAttribute(LandsatConstants.SUN_ELEV_SHORT,
                                              geoData.getSunElevationAngle(),
                                              LandsatConstants.SUN_ELEV_DESCRIPTION, LandsatConstants.Unit.ANGLE));
        attributContainer.add(createAttribute(LandsatConstants.ORIENTATION, geoData.getLookAngle(), "",
                                              LandsatConstants.Unit.ANGLE));
        for (int i = 0; i < geoData.getProjectionParameter().length; i++) {
            attributContainer.add(createAttribute((i + 1) + LandsatConstants.PARAMETER_SHORT,
                                                  geoData.getProjectionParameter()[i],
                                                  LandsatConstants.PARAMETER_DESCRIPTION));
        }

        for (MetadataAttribute anAttributContainer : attributContainer) {
            geo.addAttribute(anAttributContainer);
        }

        for (int i = 0; i < geoData.getImagePoints().size(); i++) {

            final GeoPoint geoPoint = (GeoPoint) geoData.getImagePoints().get(i);
            MetadataElement geoPointElement = new MetadataElement(geoPoint.toString());
            geoPointElement.setDescription(geoPoint.toString());
            List<MetadataAttribute> geoAttri = new ArrayList<MetadataAttribute>();
            geoAttri.add(createAttribute(LandsatConstants.geoPointsAttributes.LONGITUDE.toString(),
                                         geoPoint.getGeodicLongitude(), "", LandsatConstants.Unit.ANGLE));
            geoAttri.add(createAttribute(LandsatConstants.geoPointsAttributes.LATITUDE.toString(),
                                         geoPoint.getGeodicLatitude(), "", LandsatConstants.Unit.ANGLE));
            geoAttri.add(createAttribute(LandsatConstants.geoPointsAttributes.EASTING.toString(),
                                         geoPoint.getEasting(), "", LandsatConstants.Unit.METER));
            geoAttri.add(createAttribute(LandsatConstants.geoPointsAttributes.NORTHING.toString(),
                                         geoPoint.getNorthing(), "", LandsatConstants.Unit.METER));
            if (geoPoint.getGeoPointID().equals(LandsatConstants.Points.CENTER)) {
                GeoPoint center = (GeoPoint) geoData.getImagePoints().get(i);
                geoAttri.add(createAttribute(LandsatConstants.geoPointsAttributes.CENTER_LINE.toString(),
                                             center.getPixelY(), "", LandsatConstants.Unit.METER));
                geoAttri.add(createAttribute(LandsatConstants.geoPointsAttributes.CENTER_PIXEL.toString(),
                                             center.getPixelX(), "", LandsatConstants.Unit.METER));
            }
            for (MetadataAttribute aGeoAttri : geoAttri) {
                geoPointElement.addAttribute(aGeoAttri);
            }
            geo.addElement(geoPointElement);
        }
        return geo;
    }

    /**
     * stores the adminstration data of the complete scene in a metadata attribute
     *
     * @param landsatHeader
     *
     * @return administration metadata element
     */
    private MetadataElement setAdminstrationMetadata(LandsatHeader landsatHeader) {

        MetadataElement adminEle = new MetadataElement(ADMIN_METADATA_DESC);
        adminEle.setDescription(METADATA_ADMINISTRATIV_NODE_DESCRIPTION);

        List<MetadataAttribute> attributContainer = new ArrayList<MetadataAttribute>();
        LandsatLoc location = landsatHeader.getLoc();

        for (int i = 0; i < location.locationRecord().length; i++) {
            MetadataAttribute locRec = createAttribute(location.locationRecordDescription()[i],
                                                       location.locationRecord()[i]);
            adminEle.addAttribute(locRec);
        }

        attributContainer.add(createAttribute(LandsatConstants.PRODUCT_ID, landsatHeader.getProductID(),
                                              LandsatConstants.PRODUCT_ID_DESCRIPTION));
        attributContainer.add(createAttribute(LandsatConstants.ACQUISITION_DATE, landsatHeader.getAcquisitionDate()));
        attributContainer.add(createAttribute(LandsatConstants.INSTRUMENT, landsatHeader.getInstrumentType()));
        attributContainer.add(createAttribute(LandsatConstants.INSTRUMENT_MODE, landsatHeader.getInstrumentMode(),
                                              LandsatConstants.INSTRUMENT_MODE_DESCRIPTION));
        attributContainer.add(createAttribute(LandsatConstants.PRODUCT_SIZE, landsatHeader.getProductSize()));
        attributContainer.add(createAttribute(LandsatConstants.PRODUCT_TYPE, landsatHeader.getProductType()));
        attributContainer.add(createAttribute(LandsatConstants.RESAMPLING, landsatHeader.getResampling()));
        attributContainer.add(createAttribute(LandsatConstants.TAPE_SPANNING, landsatHeader.getTapeSpanningFlag(),
                                              LandsatConstants.TAPE_SPANNING_DESCRIPTION));
        attributContainer.add(createAttribute(LandsatConstants.START_LINE, landsatHeader.getStartLine(),
                                              LandsatConstants.START_LINE_DESCRIPTION));
        attributContainer.add(createAttribute(LandsatConstants.LINES_PER_VOL,
                                              landsatHeader.getLinesPerVolume(),
                                              LandsatConstants.LINES_PER_VOL_DESCRIPTION));
        attributContainer.add(createAttribute(LandsatConstants.PIXEL_SIZE, landsatHeader.getPixelSize(), "",
                                              LandsatConstants.Unit.METER));
        attributContainer.add(createAttribute(LandsatConstants.PIXEL_PER_LINES,
                                              landsatHeader.getImageWidth(),
                                              LandsatConstants.PIXEL_PER_LINES_DESCRIPTION,
                                              LandsatConstants.Unit.PIXEL));
        attributContainer.add(createAttribute(LandsatConstants.LINES_PER_IMAGE,
                                              landsatHeader.getImageHeight(),
                                              LandsatConstants.LINES_PER_IMAGE_DESCRIPTION));
        attributContainer.add(createAttribute(LandsatConstants.BANDS_PRESENT, landsatHeader.getBandsPresent(),
                                              LandsatConstants.BANDS_PRESENT_DESCRIPTION));
        attributContainer.add(createAttribute(LandsatConstants.BLOCKING_FACTOR,
                                              landsatHeader.getBlockingFactor(),
                                              LandsatConstants.BLOCKING_FACTOR_DESCRIPTION));
        attributContainer.add(createAttribute(LandsatConstants.RECORD_LENGTH, landsatHeader.getRecordLength(),
                                              LandsatConstants.RECORD_LENGTH_DESCRIPTION));
        attributContainer.add(createAttribute(LandsatConstants.VERSION, landsatHeader.getFormatVersion(),
                                              LandsatConstants.VERSION_DESCRIPTION));
        attributContainer.add(createAttribute(EARTH_SUN_DISTANCE, landsatHeader.getEarthSunDistance()));

        for (MetadataAttribute anAttributContainer : attributContainer) {
            adminEle.addAttribute(anAttributContainer);
        }
        return adminEle;
    }

    /**
     * stores the radiance data of each band in a metadata attribute
     *
     * @param landsatHeader
     * @param landsatBand
     *
     * @return radiometric metadata element
     */
    private static MetadataElement setRadianceMetadata(LandsatHeader landsatHeader, LandsatTMBand[] landsatBand) {
        MetadataElement rad = new MetadataElement(RADIANCE);
        rad.setDescription(RADIANCE_DESCRIPTION);
        final double[] radData = new double[10]; //  minRadiance, maxRadiance, Gain, Bias, nomMinRadiance, nomMaxRadiance

        for (int i = 1; i <= landsatHeader.getNumberOfBands(); i++) {

            final LandsatTMBand landsatTMBand = landsatBand[i - 1];
            MetadataElement band = new MetadataElement("Band" + landsatTMBand.getIndex());

            radData[LandsatConstants.MIN_RADIANCE] = landsatTMBand.getMinRadiance();
            radData[LandsatConstants.MAX_RADIANCE] = landsatTMBand.getMaxRadiance();
            radData[LandsatConstants.BIAS] = landsatTMBand.getBias();
            radData[LandsatConstants.GAIN] = landsatTMBand.getGain();
            radData[LandsatConstants.NOM_FORMER_MIN_RADIANCE] = landsatTMBand.getFormerNomMinRadiance();
            radData[LandsatConstants.NOM_FORMER_MAX_RADIANCE] = landsatTMBand.getFormerNomMaxRadiance();
            radData[LandsatConstants.NOM_FORMER_GAIN] = landsatTMBand.getFormerNominalGain();
            radData[LandsatConstants.NOM_NEWER_MIN_RADIANCE] = landsatTMBand.getNewerNomMinRadiance();
            radData[LandsatConstants.NOM_NEWER_MAX_RADIANCE] = landsatTMBand.getNewerNomMaxRadiance();
            radData[LandsatConstants.NOM_NEWER_GAIN] = landsatTMBand.getNewerNomGain();

            for (int j = 0; j < radData.length; j++) {
                MetadataAttribute tempAttri = createAttribute(LandsatConstants.RADIANCE_DESCRIPTION_SHORT[j],
                                                              radData[j]);
                tempAttri.setUnit(LandsatConstants.Unit.RADIANCE.toString());
                tempAttri.setDescription(LandsatConstants.RADIANCE_DESCRIPTION[j]);
                band.addAttribute(tempAttri);
            }
            rad.addElement(band);
        }
        return rad;
    }
}
