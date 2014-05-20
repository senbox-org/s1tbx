package org.esa.beam.dataio.avhrr.noaa.pod;

import com.bc.ceres.binio.CompoundType;
import org.esa.beam.dataio.avhrr.noaa.FormatMetadata;
import org.esa.beam.dataio.avhrr.noaa.TypeUtils;

import static com.bc.ceres.binio.SimpleType.BYTE;
import static com.bc.ceres.binio.SimpleType.INT;
import static com.bc.ceres.binio.SimpleType.SHORT;
import static com.bc.ceres.binio.SimpleType.UBYTE;
import static com.bc.ceres.binio.SimpleType.UINT;
import static com.bc.ceres.binio.SimpleType.USHORT;
import static com.bc.ceres.binio.TypeBuilder.COMPOUND;
import static com.bc.ceres.binio.TypeBuilder.MEMBER;
import static com.bc.ceres.binio.TypeBuilder.SEQUENCE;
import static com.bc.ceres.binio.TypeBuilder.VAR_SEQUENCE;
import static org.esa.beam.dataio.avhrr.noaa.TypeUtils.FILL_MEMBER;
import static org.esa.beam.dataio.avhrr.noaa.TypeUtils.META;
import static org.esa.beam.dataio.avhrr.noaa.TypeUtils.META_MEMBER;

/**
 * Data types for the NOAA HRPT data format.
 *
 * @author Ralf Quast
 */
final class PodTypes {

    private static final CompoundType TIME_CODE_TYPE =
            COMPOUND("TIME_CODE",
                     MEMBER("YEAR_DAY", USHORT),
                     MEMBER("MILLISECONDS", UINT));

    private static final CompoundType CALIBRATION_COEFFICIENTS_TYPE =
            COMPOUND("",
                     META_MEMBER("SLOPE", INT,
                                 META().setScalingFactor(9.313225746154785E-10)),
                     META_MEMBER("INTERCEPT", INT,
                                 META().setScalingFactor(2.384185791015625E-7))
            );

    private static final CompoundType EARTH_LOCATION_TYPE =
            COMPOUND("",
                     META_MEMBER("LAT", SHORT,
                                 META().setScalingFactor(0.0078125).setUnits("degrees north")),
                     META_MEMBER("LON", SHORT,
                                 META().setScalingFactor(0.0078125).setUnits("degrees east"))
            );

    private static final CompoundType KEPLERIAN_ORBITAL_ELEMENTS_TYPE =
            COMPOUND("KEPLERIAN_ORBITAL_ELEMENTS",
                     META_MEMBER("SEMI_MAJOR_AXIS", UINT,
                                 META().setScalingFactor(0.0010).setUnits("km")),
                     META_MEMBER("ECCENTRICITY", UINT,
                                 META().setScalingFactor(1.0E-8)),
                     META_MEMBER("INCLINATION", INT,
                                 META().setScalingFactor(1.0E-5).setUnits("degree")),
                     META_MEMBER("ARGUMENT_OF_PERIGEE", INT,
                                 META().setScalingFactor(1.0E-5).setUnits("degree")),
                     META_MEMBER("RIGHT_ASCENSION_OF_THE_ASCENDING_NODE", INT,
                                 META().setScalingFactor(1.0E-5).setUnits("degree")),
                     META_MEMBER("MEAN_ANOMALY", INT,
                                 META().setScalingFactor(1.0E-5).setUnits("degree"))
            );

    private static final CompoundType CARTESIAN_INERTIAL_ELEMENTS_TYPE =
            COMPOUND("CARTESIAN_INERTIAL_ELEMENTS",
                     META_MEMBER("X", INT,
                                 META().setScalingFactor(1.0E-4).setUnits("km")),
                     META_MEMBER("Y", INT,
                                 META().setScalingFactor(1.0E-4).setUnits("km")),
                     META_MEMBER("Z", INT,
                                 META().setScalingFactor(1.0E-4).setUnits("km")),
                     META_MEMBER("U", INT,
                                 META().setScalingFactor(1.0E-6).setUnits("km s-1")),
                     META_MEMBER("V", INT,
                                 META().setScalingFactor(1.0E-6).setUnits("km s-1")),
                     META_MEMBER("W", INT,
                                 META().setScalingFactor(1.0E-6).setUnits("km s-1"))
            );

    private static final CompoundType DUMMY_RECORD_TYPE =
            COMPOUND("DUMMY_RECORD",
                     FILL_MEMBER(7400)
            );

    static final CompoundType TBM_HEADER_RECORD_TYPE =
            COMPOUND("TBM_HEADER_RECORD",
                     FILL_MEMBER(30),
                     TypeUtils.STRING_MEMBER("DATASET_NAME", 44),
                     TypeUtils.STRING_MEMBER("TOTAL_OR_SELECTIVE_COPY", 1),
                     TypeUtils.STRING_MEMBER("BEGINNING_LATITUDE", 3),
                     TypeUtils.STRING_MEMBER("ENDING_LATITUDE", 3),
                     TypeUtils.STRING_MEMBER("BEGINNING_LONGITUDE", 4),
                     TypeUtils.STRING_MEMBER("ENDING_LONGITUDE", 4),
                     TypeUtils.STRING_MEMBER("START_HOUR", 2),
                     TypeUtils.STRING_MEMBER("START_MINUTE", 2),
                     TypeUtils.STRING_MEMBER("NUMBER_OF_MINUTES", 3),
                     TypeUtils.STRING_MEMBER("APPENDED_DATA_SELECTION", 1),
                     MEMBER("CHANNELS_SELECTED", SEQUENCE(UBYTE, 20)),
                     TypeUtils.STRING_MEMBER("SENSOR_DATA_WORD_SIZE", 2),
                     FILL_MEMBER(3)
            );

    static final CompoundType DATASET_HEADER_RECORD_TYPE =
            COMPOUND("DATASET_HEADER_RECORD",
                     // The members below are 'inlined', but nominally belong to the Dataset Header Record
                     // MEMBER("SPACECRAFT_ID", UBYTE),
                     // MEMBER("DATA_TYPE", UBYTE),
                     // MEMBER("START_TIME", TIME_CODE_TYPE),
                     // MEMBER("NUMBER_OF_SCANS", USHORT),
                     // MEMBER("END_TIME", TIME_CODE_TYPE),
                     TypeUtils.STRING_MEMBER("PROCESSING_BLOCK_ID", 7),
                     MEMBER("RAMP_AUTO_CALIBRATION", UBYTE),
                     MEMBER("NUMBER_OF_DATA_GAPS", USHORT),
                     MEMBER("DACS_QUALITY", SEQUENCE(USHORT, 3)),
                     TypeUtils.STRING_MEMBER("CALIBRATION_PARAMETER_ID", 2),
                     MEMBER("DACS_STATUS", UBYTE),
                     MEMBER("MOUNTING_AND_FIXED_ATTITUDE_INDICATOR", UBYTE),
                     MEMBER("NADIR_EARTH_LOCATION_TOLERANCE", UBYTE),
                     FILL_MEMBER(1),
                     MEMBER("YEAR_FOR_START_OF_DATA", USHORT),
                     TypeUtils.STRING_MEMBER("DATASET_NAME", 44),
                     MEMBER("YEAR_OF_EPOCH_FOR_ORBIT_VECTOR", USHORT),
                     MEMBER("JULIAN_DAY_OF_EPOCH", USHORT),
                     MEMBER("MILLISECOND_UTC_EPOCH_TIME_OF_DAY", UINT),
                     MEMBER("KEPLERIAN_ORBITAL_ELEMENTS", KEPLERIAN_ORBITAL_ELEMENTS_TYPE),
                     MEMBER("CARTESIAN_INERTIAL_ELEMENTS", CARTESIAN_INERTIAL_ELEMENTS_TYPE),
                     MEMBER("YAW_FIXED_ERROR_CORRECTION", SHORT),
                     MEMBER("ROLL_FIXED_ERROR_CORRECTION", SHORT),
                     MEMBER("PITCH_FIXED_ERROR_CORRECTION", SHORT),
                     FILL_MEMBER(
                             7400 - 1 - 1 - 6 - 2 - 6 - 7 - 1 - 2 - 6 - 2 - 1 - 1 - 1 - 1 - 2 - 44 - 2 - 2 - 4 - KEPLERIAN_ORBITAL_ELEMENTS_TYPE.getSize() - CARTESIAN_INERTIAL_ELEMENTS_TYPE.getSize() - 2 - 2 - 2)
            );

    static final CompoundType DATA_RECORD_TYPE =
            COMPOUND("DATA_RECORD",
                     MEMBER("SCAN_LINE_NUMBER", USHORT),
                     MEMBER("TIME_CODE", TIME_CODE_TYPE),
                     MEMBER("QUALITY_INDICATORS", UINT),
                     MEMBER("CALIBRATION_COEFFICIENTS", SEQUENCE(CALIBRATION_COEFFICIENTS_TYPE, 5)),
                     MEMBER("NUMBER_OF_MEANINGFUL_ZENITH_ANGLES_AND_EARTH_LOCATION_POINTS", UBYTE),
                     META_MEMBER("SOLAR_ZENITH_ANGLES", SEQUENCE(BYTE, 51),
                                 META().setScalingFactor(0.5).setUnits("degree")),
                     MEMBER("EARTH_LOCATION", SEQUENCE(EARTH_LOCATION_TYPE, 51)),
                     MEMBER("TELEMETRY", SEQUENCE(UBYTE, 140)),
                     MEMBER("VIDEO_DATA", SEQUENCE(UINT, 3414)),
                     MEMBER("DECIMAL_PORTION_OF_SOLAR_ZENITH_ANGLES", SEQUENCE(UBYTE, 20)),
                     MEMBER("CLOCK_DRIFT_DELTA", USHORT),
                     FILL_MEMBER(674)
            );

    static final CompoundType HRPT_TYPE =
            COMPOUND("HRPT",
                     MEMBER("TBM_HEADER_RECORD", PodTypes.TBM_HEADER_RECORD_TYPE),
                     MEMBER("SPACECRAFT_ID", UBYTE),
                     MEMBER("DATA_TYPE", UBYTE),
                     MEMBER("START_TIME", TIME_CODE_TYPE),
                     MEMBER("NUMBER_OF_SCANS", USHORT),
                     MEMBER("END_TIME", TIME_CODE_TYPE),
                     MEMBER("DATASET_HEADER_RECORD", PodTypes.DATASET_HEADER_RECORD_TYPE),
                     MEMBER("DUMMY_RECORD", PodTypes.DUMMY_RECORD_TYPE),
                     MEMBER("DATA_RECORDS", VAR_SEQUENCE(PodTypes.DATA_RECORD_TYPE, "NUMBER_OF_SCANS"))
            );

    static FormatMetadata getSolarZenithAnglesMetadata() {
        return (FormatMetadata) DATA_RECORD_TYPE.getMember(5).getMetadata();
    }

    static FormatMetadata getLatMetadata() {
        return (FormatMetadata) EARTH_LOCATION_TYPE.getMember(0).getMetadata();
    }

    static FormatMetadata getLonMetadata() {
        return (FormatMetadata) EARTH_LOCATION_TYPE.getMember(1).getMetadata();
    }

    static FormatMetadata getSlopeMetadata() {
        return (FormatMetadata) CALIBRATION_COEFFICIENTS_TYPE.getMember(0).getMetadata();
    }

    static FormatMetadata getInterceptMetadata() {
        return (FormatMetadata) CALIBRATION_COEFFICIENTS_TYPE.getMember(1).getMetadata();
    }
}
