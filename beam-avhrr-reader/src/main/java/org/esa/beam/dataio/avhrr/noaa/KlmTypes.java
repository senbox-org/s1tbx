/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.beam.dataio.avhrr.noaa;

import com.bc.ceres.binio.CompoundMember;
import com.bc.ceres.binio.CompoundType;
import com.bc.ceres.binio.Type;
import com.bc.ceres.binio.internal.CompoundMemberImpl;
import org.esa.beam.dataio.avhrr.AvhrrConstants;

import java.util.ArrayList;
import java.util.List;

import static com.bc.ceres.binio.TypeBuilder.COMPOUND;
import static com.bc.ceres.binio.TypeBuilder.INT;
import static com.bc.ceres.binio.TypeBuilder.MEMBER;
import static com.bc.ceres.binio.TypeBuilder.SEQUENCE;
import static com.bc.ceres.binio.TypeBuilder.SHORT;
import static com.bc.ceres.binio.TypeBuilder.UBYTE;
import static com.bc.ceres.binio.TypeBuilder.UINT;
import static com.bc.ceres.binio.TypeBuilder.USHORT;
import static com.bc.ceres.binio.TypeBuilder.VAR_SEQUENCE;
import static org.esa.beam.dataio.avhrr.noaa.TypeUtils.FILL_MEMBER;
import static org.esa.beam.dataio.avhrr.noaa.TypeUtils.META;
import static org.esa.beam.dataio.avhrr.noaa.TypeUtils.META_MEMBER;
import static org.esa.beam.dataio.avhrr.noaa.TypeUtils.STRING_MEMBER;

/**
 * bin-io types for the NOAA AVHRR format.
 */
class KlmTypes {

    ////////////////////////////////////////////////////////////////////////////////////////
    static CompoundType arsHeaderOrderIdType =
            COMPOUND("ORDER_ID",
                     STRING_MEMBER("COST_NUMBER", 6),
                     STRING_MEMBER("CLASS_NUMBER", 8),
                     STRING_MEMBER("ORDER_CREATION_YEAR", 4),
                     STRING_MEMBER("ORDER_CREATION_DAY_OF_YEAR", 3),
                     STRING_MEMBER("PROCESSING_SITE", 1, META().
                                           addItem("A", "CLASS").
                                           addItem("S", "NCDC/Suitland").
                                           addItem("N", "NCDC/Asheville")
                     ),
                     STRING_MEMBER("PROCESSING_SOFTWARE_ID", 8)
            );

    static CompoundType dataSelectionCriteria =
            COMPOUND("DATA_SELECTION_CRITERIA",
                     STRING_MEMBER("DATA_SET_NAME", 42),
                     STRING_MEMBER("fill", 2),
                     STRING_MEMBER("SELECT_FLAG", 1, META().
                             addItem("T", "Total Data Set Copy").
                             addItem("S", "Selective Data Set Copy (Subset)")),
                     STRING_MEMBER("BEGINNIG_LATITUDE", 3, META().setUnits(AvhrrConstants.UNIT_DEG)),
                     STRING_MEMBER("ENDING_LATITUDE", 3, META().setUnits(AvhrrConstants.UNIT_DEG)),
                     STRING_MEMBER("BEGINNIG_LONGITUDE", 4,
                                   META().setUnits(AvhrrConstants.UNIT_DEG)),
                     STRING_MEMBER("ENDING_LONGITUDE", 4, META().setUnits(AvhrrConstants.UNIT_DEG)),
                     STRING_MEMBER("START_HOUR", 2),
                     STRING_MEMBER("START_MINUTE", 2),
                     STRING_MEMBER("NUMBER_OF_MINUTES", 3),
                     STRING_MEMBER("APPEND_DATA_FLAG", 1),
                     STRING_MEMBER("CHANNEL_SELECT_FLAGS", 20),
                     STRING_MEMBER("SENSOR_DATA_WORD_SIZE", 2)
            );

    static CompoundType dataSetSummary =
            COMPOUND("DATA_SET_SUMMARY",
                     STRING_MEMBER("fill", 27),
                     STRING_MEMBER("ASCEND_DESCEND_FLAG", 1, META().
                             addItem("A", "Ascending only").
                             addItem("D", "Descending only").
                             addItem("B", "Both ascending and descending")),
                     STRING_MEMBER("FIRST_LATITUDE", 3, META().
                             setUnits(AvhrrConstants.UNIT_DEG).
                             setDescription("First latitude value in the first data record")),
                     STRING_MEMBER("LAST_LATITUDE", 3, META().
                             setUnits(AvhrrConstants.UNIT_DEG).
                             setDescription("Last latitude value in the last data record")),
                     STRING_MEMBER("FIRST_LONGITUDE", 4, META().
                             setUnits(AvhrrConstants.UNIT_DEG).
                             setDescription("First longitude value in the first data record")),
                     STRING_MEMBER("LAST_LONGITUDE", 4, META().
                             setUnits(AvhrrConstants.UNIT_DEG).
                             setDescription("Last longitude value in the last data record")),
                     STRING_MEMBER("DATA_FORMAT", 20),
                     STRING_MEMBER("SIZE_OF_RECORDS", 6, META().setUnits(AvhrrConstants.UNIT_BYTES)),
                     STRING_MEMBER("NUMBER_OF_RECORDS", 6,
                                   META().setDescription(
                                           "Total, including ARS and Data Set Header Records")
                     )
            );

    static CompoundType arsHeaderType = COMPOUND("ARCHIVE_RETRIEVAL_SYSTEM_HEADER",
                                                 COMPOUND_MEMBER(arsHeaderOrderIdType),
                                                 COMPOUND_MEMBER(dataSelectionCriteria),
                                                 COMPOUND_MEMBER(dataSetSummary),
                                                 STRING_MEMBER("fill", 319)
    );
    ////////////////////////////////////////////////////////////////////////////////////////
    static CompoundType date =
            COMPOUND("DATE",
                     MEMBER("daysSince1950", UINT),
                     MEMBER("year", USHORT),
                     MEMBER("dayOfYear", USHORT),
                     MEMBER("UTCmillis", UINT)
            );

    static CompoundType fileIdentification =
            COMPOUND("FILE_IDENTIFICATION",
                     STRING_MEMBER("DATA_SET_CREATION_SITE_ID", 3),
                     FILL_MEMBER(1),
                     META_MEMBER("NOAA_LEVEL_1B_FORMAT_VERSION_NUMBER", USHORT, META().
                             addItem(1, "TIROS-N, NOAA-6 through NOAA-14").
                             addItem(2, "NOAA-15, -16, -17 (pre-April 28, 2005)").
                             addItem(3, "All satellites post-April 28, 2005").
                             addItem(4, "All satellites post-April 28, 2005 (with CLAVR-x)").
                             addItem(5, "All satellites post-November 14, 2006 (with CLAVR-x)")),

                     META_MEMBER("NOAA_LEVEL_1B_FORMAT_VERSION_YEAR", USHORT,
                                 META().setUnits(AvhrrConstants.UNIT_YEARS)),
                     META_MEMBER("NOAA_LEVEL_1B_FORMAT_VERSION_DAY_OF_YEAR", USHORT,
                                 META().setUnits(AvhrrConstants.UNIT_DAYS)),
                     MEMBER("LOGICAL_RECORD_LENGTH", USHORT),
                     MEMBER("BLOCK_SIZE", USHORT),
                     MEMBER("COUNT_OF_HEADER_RECORDS", USHORT),
                     FILL_MEMBER(6),
                     STRING_MEMBER("DATA_SET_NAME", 42),
                     STRING_MEMBER("PROCESSING_BLOCK_IDENTIFICATION", 8),
                     META_MEMBER("NOAA_SPACECRAFT_IDENTIFICATION_CODE", USHORT, META().
                             addItem(2, "NOAA-16 (NOAA-L)").
                             addItem(4, "NOAA-15 (NOAA-K)").
                             addItem(6, "NOAA-17 (NOAA-M)").
                             addItem(7, "NOAA-18 (NOAA-N)").
                             addItem(8, "(NOAA-P)").
                             addItem(11, "MetOp-1").
                             addItem(12, "MetOp-A")),
                     MEMBER("INSTRUMENT_ID", USHORT),
                     META_MEMBER("DATA_TYPE_CODE", USHORT, META().
                             addItem(1, "LAC").
                             addItem(2, "GAC").
                             addItem(3, "HRPT").
                             addItem(13, "FRAC")),
                     MEMBER("TIP_SOURCE_CODE", USHORT),
                     MEMBER("START_OF_DATA_SET", date),
                     MEMBER("END_OF_DATA_SET", date),
                     META_MEMBER("YEAR_OF_LAST_CPIDS_UPDATE", USHORT,
                                 META().setUnits(AvhrrConstants.UNIT_YEARS)),
                     META_MEMBER("DAY_OF_YEAR_OF_LAST_CPIDS_UPDATE", USHORT,
                                 META().setUnits(AvhrrConstants.UNIT_DAYS)),
                     FILL_MEMBER(8)
            );
    static CompoundType datasetQualityIndicators =
            COMPOUND("DATA_SET_QUALITY_INDICATORS",
                     MEMBER("INSTRUMENT_STATUS", UINT),
                     FILL_MEMBER(2),
                     MEMBER("RECORD_NUMBER_OF_STATUS_CHANGE", USHORT),
                     MEMBER("SECOND_INSTRUMENT_STATUS", UINT),
                     MEMBER("COUNT_OF_DATA_RECORDS", USHORT),
                     MEMBER("COUNT_OF_CALIBRATED_EARTH_LOCATED_SCAN_LINES", USHORT),
                     MEMBER("COUNT_OF_MISSING_SCAN_LINES", USHORT),
                     MEMBER("COUNT_OF_DATA_GAPS", USHORT),
                     MEMBER("COUNT_OF_DATA_FRAMES_WITHOUT_FRAME_SYNC_WORD_ERRORS", USHORT),
                     MEMBER("COUNT_OF_PACS_DETECTED_TIP_PARITY_ERRORS", USHORT),
                     MEMBER("COUNT_OF_ALL_AUXILIARY_SYNC_ERRORS", USHORT),
                     MEMBER("TIME_SEQUENCE_ERROR", USHORT),
                     MEMBER("TIME_SEQUENCE_ERROR_CODE", USHORT),
                     MEMBER("SOCC_CLOCK_UPDATE_INDICATOR", USHORT),
                     MEMBER("EARTH_LOCATION_ERROR_INDICATOR", USHORT),
                     MEMBER("EARTH_LOCATION_ERROR_CODE", USHORT),
                     MEMBER("PACS_STATUS_BIT_FIELD", USHORT),
                     MEMBER("PACS_DATA_SOURCE", USHORT),
                     FILL_MEMBER(4),
                     STRING_MEMBER("INGESTER", 8),
                     STRING_MEMBER("DECOMUTATION", 8),
                     FILL_MEMBER(10)
            );

    static CompoundType calibration =
            COMPOUND("CALIBRATION",
                     FILL_MEMBER(70)
            );

    static CompoundType radianceConversion =
            COMPOUND("RADIANCE_CONVERSION",
                     META_MEMBER("CHANNEL_1_SOLAR_IRRADIANCE", INT, META().setScalingFactor(1E-1)),
                     META_MEMBER("CHANNEL_1_EQUIVALENT_WIDTH", INT, META().setScalingFactor(1E-3)),
                     META_MEMBER("CHANNEL_2_SOLAR_IRRADIANCE", INT, META().setScalingFactor(1E-1)),
                     META_MEMBER("CHANNEL_2_EQUIVALENT_WIDTH", INT, META().setScalingFactor(1E-3)),
                     META_MEMBER("CHANNEL_3A_SOLAR_IRRADIANCE", INT, META().setScalingFactor(1E-1)),
                     META_MEMBER("CHANNEL_3A_EQUIVALENT_WIDTH", INT, META().setScalingFactor(1E-3)),
                     META_MEMBER("CHANNEL_3B_CENTRAL_WAVENUMBER", INT,
                                 META().setScalingFactor(1E-2).setUnits(AvhrrConstants.UNIT_PER_CM)),
                     META_MEMBER("CHANNEL_3B_CONSTANT_1", INT, META().setScalingFactor(1E-5)),
                     META_MEMBER("CHANNEL_3B_CONSTANT_2", INT, META().setScalingFactor(1E-6)),
                     META_MEMBER("CHANNEL_4_CENTRAL_WAVENUMBER", INT,
                                 META().setScalingFactor(1E-3).setUnits(AvhrrConstants.UNIT_PER_CM)),
                     META_MEMBER("CHANNEL_4_CONSTANT_1", INT, META().setScalingFactor(1E-5)),
                     META_MEMBER("CHANNEL_4_CONSTANT_2", INT, META().setScalingFactor(1E-6)),
                     META_MEMBER("CHANNEL_5_CENTRAL_WAVENUMBER", INT,
                                 META().setScalingFactor(1E-3).setUnits(AvhrrConstants.UNIT_PER_CM)),
                     META_MEMBER("CHANNEL_5_CONSTANT_1", INT, META().setScalingFactor(1E-5)),
                     META_MEMBER("CHANNEL_5_CONSTANT_2", INT, META().setScalingFactor(1E-6)),
                     FILL_MEMBER(12)
            );

    static CompoundType navigation =
            COMPOUND("NAVIGATION",
                     STRING_MEMBER("REFERENCE_ELIPSOID_MODEL_ID", 8),
                     META_MEMBER("NADIR_EARTH_LOCATION_TOLERANCE", USHORT,
                                 META().setScalingFactor(1E-1).setUnits(AvhrrConstants.UNIT_KM)),
                     MEMBER("EARTH_LOCATION_BIT_FIELD", USHORT),
                     FILL_MEMBER(2),
                     META_MEMBER("CONSTANT_ROLL_ATTITUDE_ERROR", SHORT,
                                 META().setScalingFactor(1E-3).setUnits(AvhrrConstants.UNIT_DEG)),
                     META_MEMBER("CONSTANT_PITCH_ATTITUDE_ERROR", SHORT,
                                 META().setScalingFactor(1E-3).setUnits(AvhrrConstants.UNIT_DEG)),
                     META_MEMBER("CONSTANT_YAW_ATTITUDE_ERROR", SHORT,
                                 META().setScalingFactor(1E-3).setUnits(AvhrrConstants.UNIT_DEG)),
                     META_MEMBER("EPOCH_YEAR_FOR_ORBIT_VECTOR", USHORT,
                                 META().setUnits(AvhrrConstants.UNIT_YEARS)),
                     META_MEMBER("DAY_OF_EPOCH_YEAR_FOR_ORBIT_VECTOR", USHORT,
                                 META().setUnits(AvhrrConstants.UNIT_DAYS)),
                     META_MEMBER("EPOCH_UTC_TIME_OF_DAY_FOR_ORBIT_VECTOR", UINT,
                                 META().setUnits(AvhrrConstants.UNIT_MS)),

                     META_MEMBER("SEMI_MAJOR_AXIS", INT,
                                 META().setScalingFactor(1E-5).setUnits(AvhrrConstants.UNIT_KM)),
                     META_MEMBER("ECCENTRICITY", INT, META().setScalingFactor(1E-8)),
                     META_MEMBER("INCLINATION", INT,
                                 META().setScalingFactor(1E-5).setUnits(AvhrrConstants.UNIT_DEG)),
                     META_MEMBER("ARGUMENT_OF_PERIGEE", INT,
                                 META().setScalingFactor(1E-5).setUnits(AvhrrConstants.UNIT_DEG)),
                     META_MEMBER("RIGHT_ASCENSION_OF_THE_ASCENDING_NODE", INT,
                                 META().setScalingFactor(1E-5).setUnits(AvhrrConstants.UNIT_DEG)),
                     META_MEMBER("MEAN_ANOMALY", INT,
                                 META().setScalingFactor(1E-5).setUnits(AvhrrConstants.UNIT_DEG)),

                     META_MEMBER("POSITION_VECTOR_X_COMPONENT", INT,
                                 META().setScalingFactor(1E-5).setUnits(AvhrrConstants.UNIT_KM)),
                     META_MEMBER("POSITION_VECTOR_Y_COMPONENT", INT,
                                 META().setScalingFactor(1E-5).setUnits(AvhrrConstants.UNIT_KM)),
                     META_MEMBER("POSITION_VECTOR_Z_COMPONENT", INT,
                                 META().setScalingFactor(1E-5).setUnits(AvhrrConstants.UNIT_KM)),
                     META_MEMBER("VELOCITY_VECTOR_X_DOT_COMPONENT", INT,
                                 META().setScalingFactor(1E-8).setUnits(AvhrrConstants.UNIT_KM)),
                     META_MEMBER("VELOCITY_VECTOR_Y_DOT_COMPONENT", INT,
                                 META().setScalingFactor(1E-8).setUnits(AvhrrConstants.UNIT_KM)),
                     META_MEMBER("VELOCITY_VECTOR_Z_DOT_COMPONENT", INT,
                                 META().setScalingFactor(1E-8).setUnits(AvhrrConstants.UNIT_KM)),

                     META_MEMBER("EARTH_SUN_DISTANCE_RATIO", UINT, META().setScalingFactor(1E-6)),
                     FILL_MEMBER(16)
            );


    private static CompoundMember COMPOUND_MEMBER(CompoundType compoundType) {
        return MEMBER(compoundType.getName(), compoundType);
    }

    private static CompoundType getHeaderRecordType(int blockSize) {
        final int headFill = blockSize - fileIdentification.getSize() - datasetQualityIndicators.getSize() -
                             calibration.getSize() - radianceConversion.getSize() - navigation.getSize() - 528 - 36 - 2;
        return COMPOUND("HeaderRecord",
                        MEMBER("FILE_IDENTIFICATION", fileIdentification),
                        MEMBER("DATA_SET_QUALITY_INDICATORS", datasetQualityIndicators),
                        MEMBER("CALIBRATION", calibration),
                        MEMBER("RADIANCE_CONVERSION", radianceConversion),
                        MEMBER("NAVIGATION", navigation),
                        FILL_MEMBER(528), //ANALOG TELEMETRY CONVERSION
                        FILL_MEMBER(36), //METOP MANEUVERS IDENTIFICATION
                        MEMBER("CLAVR_STATUS", USHORT),
                        FILL_MEMBER(headFill)
        );
    }

    private static CompoundType getDataRecordType(ProductFormat productFormat) {
        int endFillBytes = productFormat.getBlockSize() - 14 - 10 - 16 - 8 -
                           (INT.getSize() * AvhrrConstants.CALIB_COEFF_LENGTH) -
                           12 - 16 - (SHORT.getSize() * 153) - 6 - 408 - 8 - 208 -
                           (productFormat.getElementType().getSize() * productFormat.getElementCount()) -
                           8 - 16 - 32 - 4 - 4 -
                           productFormat.getProductDimension().getCloudBytes();

        return COMPOUND("DataRecord",
                        // SCAN LINE INFORMATION
                        MEMBER("SCANLINE_NUMBER", USHORT),
                        MEMBER("SCANLINE_YEAR", USHORT),
                        MEMBER("SCANLINE_DAY_OF_YEAR", USHORT),
                        MEMBER("SATELLITE _CLOCK_DRIFT_DELTA", SHORT),
                        MEMBER("SCANLINE_UTC_TIME_OF_DAY", UINT),
                        MEMBER("SCANLINE_BIT_FIELD", USHORT),
                        FILL_MEMBER(10),
                        // QUALITY INDICATORS
                        MEMBER("QUALITY_INDICATOR_BIT_FIELD", UINT),
                        MEMBER("SCANLINE_QUALITY_FLAGS", UINT),
                        MEMBER("CALIBRATION_QUALITY_FLAGS_3B", USHORT),
                        MEMBER("CALIBRATION_QUALITY_FLAGS_4", USHORT),
                        MEMBER("CALIBRATION_QUALITY_FLAGS_5", USHORT),
                        MEMBER("COUNT_OF_BIT_ERRORS_IN_FRAME_SYNC", USHORT),
                        FILL_MEMBER(8),
                        // CALIBRATION COEFFICIENTS
                        MEMBER("CALIBRATION_COEFFICIENTS", SEQUENCE(INT, AvhrrConstants.CALIB_COEFF_LENGTH)),
                        FILL_MEMBER(12),
                        // NAVIGATION
                        MEMBER("NAVIGATION_STATUS_BIT_FIELD", UINT),
                        MEMBER("TIP_EULER_ANGLES_TIME", UINT),
                        MEMBER("TIP_EULER_ANGLES", SEQUENCE(SHORT, 3)),
                        MEMBER("SPACECRAFT_ALTITUDE", USHORT),
                        MEMBER("ANGULAR_RELATIONSHIPS", SEQUENCE(SHORT, 153)),
                        FILL_MEMBER(6),
                        MEMBER("EARTH_LOCATION", SEQUENCE(INT, 102)),
                        FILL_MEMBER(8),
                        // HRPT MINOR FRAME TELEMETRY
                        FILL_MEMBER(208),
                        // AVHRR SENSOR DATA
                        MEMBER("AVHRR_SENSOR_DATA",
                               SEQUENCE(productFormat.getElementType(), productFormat.getElementCount())),
                        FILL_MEMBER(8),
                        // DIGITAL B TELEMETRY
                        FILL_MEMBER(16),
                        // ANALOG HOUSEKEEPING DATA (TIP)
                        FILL_MEMBER(32),
                        // CLOUDS FROM AVHRR (CLAVR)
                        MEMBER("CLAVR_STATUS_BIT_FIELD", UINT),
                        FILL_MEMBER(4),
                        MEMBER("CCM", SEQUENCE(UBYTE, productFormat.getProductDimension().getCloudBytes())),
                        FILL_MEMBER(endFillBytes)
        );
    }

    static CompoundType getFileType(boolean hasArsHeader, ProductFormat productFormat, int dataRecordcount) {
        final List<CompoundMember> members = new ArrayList<>(3);
        if (hasArsHeader) {
            members.add(MEMBER("ArsRecord", arsHeaderType));
        }
        members.add(MEMBER("HeaderRecord", getHeaderRecordType(productFormat.getBlockSize())));

        final Type dataRecordType = getDataRecordType(productFormat);
        members.add(MEMBER("DataRecord", SEQUENCE(dataRecordType, dataRecordcount)));
        return COMPOUND("NOAA", members.toArray(new CompoundMember[members.size()]));
    }

}
