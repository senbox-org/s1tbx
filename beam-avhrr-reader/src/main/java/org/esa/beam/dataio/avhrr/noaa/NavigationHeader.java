/*
 * $Id: NavigationHeader.java,v 1.1 2006/09/12 11:42:42 marcop Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.dataio.avhrr.noaa;

import java.io.IOException;
import java.io.InputStream;

import org.esa.beam.dataio.avhrr.AvhrrConstants;
import org.esa.beam.dataio.avhrr.HeaderUtil;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.ProductData;

/**
 * Created by IntelliJ IDEA.
 * User: marcoz
 * Date: 13.06.2005
 * Time: 09:31:46
 * To change this template use File | Settings | File Templates.
 */
class NavigationHeader {
    private static final String META_DATA_NAME = "NAVIGATION";
    private static final int REFERENCE_ELIPSOID_LENGTH = 8;

    private String referenceElipsoid;
    private float nadirEarthLocationTolerance;
    private int earthLocationBitField;
    private float rollAttitudeError;
    private float pitchAttitudeError;
    private float yawAttitudeError;
    private int vectorYear;
    private int vectorDay;
    private int vectorTimeOfDay;
    private float semiMajorAxis;
    private float eccentricity;
    private float inclination;
    private float argumentOfPerigee;
    private float rightAscension;
    private float meanAnomaly;
    private float positionVectorXComponent;
    private float positionVectorYComponent;
    private float positionVectorZComponent;
    private float velocityVectorXComponent;
    private float velocityVectorYComponent;
    private float velocityVectorZComponent;
    private float earthSunDistanceRatio;

    public NavigationHeader(InputStream header) throws IOException {
        parse(header);
    }

    public float getEarthSunDistanceRatio() {
        return earthSunDistanceRatio;
    }

    public MetadataElement getMetadata() {
        ProductData.UTC vectorTime = HeaderUtil.createUTCDate(vectorYear, vectorDay, vectorTimeOfDay);

        MetadataElement element = new MetadataElement(META_DATA_NAME);

        element.addAttribute(HeaderUtil.createAttribute("REFERENCE_ELIPSOID_MODEL_ID", referenceElipsoid));
        element.addAttribute(HeaderUtil.createAttribute("NADIR_EARTH_LOCATION_TOLERANCE", nadirEarthLocationTolerance, AvhrrConstants.UNIT_KM));
        element.addAttribute(HeaderUtil.createAttribute("REASONABLENESS_TEST", earthLocationBitField, 1, "inactive", "active"));
        element.addAttribute(HeaderUtil.createAttribute("ATTITUDE_ERROR_CORRECTION", earthLocationBitField, 0, "not corrected", "corrected"));
        element.addAttribute(HeaderUtil.createAttribute("CONSTANT_ROLL_ATTITUDE_ERROR", rollAttitudeError, AvhrrConstants.UNIT_DEG));
        element.addAttribute(HeaderUtil.createAttribute("CONSTANT_PITCH_ATTITUDE_ERROR", pitchAttitudeError, AvhrrConstants.UNIT_DEG));
        element.addAttribute(HeaderUtil.createAttribute("CONSTANT_YAW_ATTITUDE_ERROR", yawAttitudeError, AvhrrConstants.UNIT_DEG));

        element.addAttribute(HeaderUtil.createAttribute("EPOCH_YEAR_FOR_ORBIT_VECTOR", vectorYear, AvhrrConstants.UNIT_YEARS));
        element.addAttribute(HeaderUtil.createAttribute("DAY_OF_EPOCH_YEAR_FOR_ORBIT_VECTOR", vectorDay, AvhrrConstants.UNIT_DAYS));
        element.addAttribute(HeaderUtil.createAttribute("EPOCH_UTC_TIME_OF_DAY_FOR_ORBIT_VECTOR", vectorTimeOfDay, AvhrrConstants.UNIT_MS));
        element.addAttribute(HeaderUtil.createAttribute("TIME_FOR_ORBIT_VECTOR", vectorTime.getElemString(), AvhrrConstants.UNIT_DATE));
        element.addAttribute(HeaderUtil.createAttribute("SEMI_MAJOR_AXIS", semiMajorAxis, AvhrrConstants.UNIT_KM));
        element.addAttribute(HeaderUtil.createAttribute("ECCENTRICITY", eccentricity));
        element.addAttribute(HeaderUtil.createAttribute("INCLINATION", inclination, AvhrrConstants.UNIT_DEG));
        element.addAttribute(HeaderUtil.createAttribute("ARGUMENT_OF_PERIGEE", argumentOfPerigee, AvhrrConstants.UNIT_DEG));
        element.addAttribute(HeaderUtil.createAttribute("RIGHT_ASCENSION_OF_THE_ASCENDING_NODE", rightAscension, AvhrrConstants.UNIT_DEG));
        element.addAttribute(HeaderUtil.createAttribute("MEAN_ANOMALY", meanAnomaly, AvhrrConstants.UNIT_DEG));
        element.addAttribute(HeaderUtil.createAttribute("POSITION_VECTOR_X_COMPONENT", positionVectorXComponent, AvhrrConstants.UNIT_KM));
        element.addAttribute(HeaderUtil.createAttribute("POSITION_VECTOR_Y_COMPONENT", positionVectorYComponent, AvhrrConstants.UNIT_KM));
        element.addAttribute(HeaderUtil.createAttribute("POSITION_VECTOR_Z_COMPONENT", positionVectorZComponent, AvhrrConstants.UNIT_KM));
        element.addAttribute(HeaderUtil.createAttribute("VELOCITY_VECTOR_X_DOT_COMPONENT", velocityVectorXComponent, AvhrrConstants.UNIT_KM_PER_S));
        element.addAttribute(HeaderUtil.createAttribute("VELOCITY_VECTOR_Y_DOT_COMPONENT", velocityVectorYComponent, AvhrrConstants.UNIT_KM_PER_S));
        element.addAttribute(HeaderUtil.createAttribute("VELOCITY_VECTOR_Z_DOT_COMPONENT", velocityVectorZComponent, AvhrrConstants.UNIT_KM_PER_S));
        element.addAttribute(HeaderUtil.createAttribute("EARTH_SUN_DISTANCE_RATIO", earthSunDistanceRatio));

        return element;

    }

    private void parse(InputStream header) throws IOException {
        ExtendedDataInputStream inStream = new ExtendedDataInputStream(header);

        referenceElipsoid = inStream.readString(REFERENCE_ELIPSOID_LENGTH);
        nadirEarthLocationTolerance = inStream.readUnsignedShort() * 1E-1f;
        earthLocationBitField = inStream.readUnsignedShort();
        inStream.skip(2);
        rollAttitudeError = inStream.readShort() * 1E-3f;
        pitchAttitudeError = inStream.readShort() * 1E-3f;
        yawAttitudeError = inStream.readShort() * 1E-3f;
        vectorYear = inStream.readUnsignedShort();
        vectorDay = inStream.readUnsignedShort();
        vectorTimeOfDay = inStream.readInt();
        semiMajorAxis = inStream.readInt() * 1E-5f;
        eccentricity = inStream.readInt() * 1E-8f;
        inclination = inStream.readInt() * 1E-5f;
        argumentOfPerigee = inStream.readInt() * 1E-5f;
        rightAscension = inStream.readInt() * 1E-5f;
        meanAnomaly = inStream.readInt() * 1E-5f;
        positionVectorXComponent = inStream.readInt() * 1E-5f;
        positionVectorYComponent = inStream.readInt() * 1E-5f;
        positionVectorZComponent = inStream.readInt() * 1E-5f;
        velocityVectorXComponent = inStream.readInt() * 1E-8f;
        velocityVectorYComponent = inStream.readInt() * 1E-8f;
        velocityVectorZComponent = inStream.readInt() * 1E-8f;
        earthSunDistanceRatio = inStream.readInt() * 1E-6f;
    }
}
