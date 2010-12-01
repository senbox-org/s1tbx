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

package org.esa.beam.dataio.avhrr.binio;

import com.bc.ceres.binio.CollectionData;
import com.bc.ceres.binio.CompoundData;
import com.bc.ceres.binio.CompoundMember;
import com.bc.ceres.binio.CompoundType;
import com.bc.ceres.binio.SimpleType;
import com.bc.ceres.binio.Type;
import com.bc.ceres.binio.VarSequenceType;
import com.bc.ceres.binio.internal.VarElementCountSequenceType;
import org.esa.beam.dataio.avhrr.AvhrrConstants;

import java.io.IOException;

import static com.bc.ceres.binio.TypeBuilder.*;

/**
 * binio types for h NOAA AVHRR format.
 */
class NoaaTypes {

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
                    STRING_MEMBER("BEGINNIG_LONGITUDE", 4, META().setUnits(AvhrrConstants.UNIT_DEG)),
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
                    STRING_MEMBER("NUMBER_OF_RECORDS", 6, META().setDescription("Total, including ARS and Data Set Header Records"))
            );

    static CompoundType arsHeaderType = COMPOUND("ARCHIVE_RETRIEVAL_SYSTEM_HEADER",
            COMPOUND_MEMBER(arsHeaderOrderIdType),
            COMPOUND_MEMBER(dataSelectionCriteria),
            COMPOUND_MEMBER(dataSetSummary),
            STRING_MEMBER("fill", 319)
    );

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
                    STRING_MEMBER("fill", 1),
                    META_MEMBER("NOAA_LEVEL_1B_FORMAT_VERSION_NUMBER", USHORT, META().
                            addItem(1, "TIROS-N, NOAA-6 through NOAA-14").
                            addItem(2, "NOAA-15, -16, -17 (pre-April 28, 2005)").
                            addItem(3, "All satellites post-April 28, 2005").
                            addItem(4, "All satellites post-April 28, 2005 (with CLAVR-x)").
                            addItem(5, "All satellites post-November 14, 2006 (with CLAVR-x)")),

                    META_MEMBER("NOAA_LEVEL_1B_FORMAT_VERSION_YEAR", USHORT, META().setUnits(AvhrrConstants.UNIT_YEARS)),
                    META_MEMBER("NOAA_LEVEL_1B_FORMAT_VERSION_DAY_OF_YEAR", USHORT, META().setUnits(AvhrrConstants.UNIT_DAYS)),
                    MEMBER("LOGICAL_RECORD_LENGTH", USHORT),
                    MEMBER("BLOCK_SIZE", USHORT),
                    MEMBER("COUNT_OF_HEADER_RECORDS", USHORT),
                    MEMBER("fill", SEQUENCE(USHORT, 3)),
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
                    META_MEMBER("YEAR_OF_LAST_CPIDS_UPDATE", USHORT, META().setUnits(AvhrrConstants.UNIT_YEARS)),
                    META_MEMBER("DAY_OF_YEAR_OF_LAST_CPIDS_UPDATE", USHORT, META().setUnits(AvhrrConstants.UNIT_DAYS))
            );

    static CompoundType headerRecordType =
            COMPOUND("HeaderRecord",
                MEMBER("FILE_IDENTIFICATION", fileIdentification),
                STRING_MEMBER("fill", 2)
            );
    static CompoundType dataRecordType = COMPOUND("DataRecord", STRING_MEMBER("fill", 1));

    // 0 or 1

    static VarSequenceType getDataRecordSequence() {
        return new VarElementCountSequenceType(dataRecordType) {
            @Override
            protected int resolveElementCount(CollectionData parent) throws IOException {
                CompoundType parenType = (CompoundType) parent.getType();
                int headerIndex = parenType.getMemberIndex("headerRecord");
                CompoundData headerData = parent.getCompound(headerIndex);

                return 1;
            }
        };
    }

    static CompoundType ALL = COMPOUND("NOAA",
            MEMBER("arsRecord", arsHeaderType),
            MEMBER("headerRecord", headerRecordType),
            MEMBER("dataRecords", getDataRecordSequence())

    );

    private static FormatMetadata META() {
        return new FormatMetadata();
    }

    private static CompoundMember COMPOUND_MEMBER(CompoundType compoundType) {
        return MEMBER(compoundType.getName(), compoundType);
    }

    private static CompoundMember STRING_MEMBER(String name, int length) {
        return STRING_MEMBER(name, length, META());
    }

    private static CompoundMember STRING_MEMBER(String name, int length, FormatMetadata metadata) {
        CompoundMember member = MEMBER(name, STRING(length));
        metadata.setType("string");
        member.setMetadata(metadata);
        return member;
    }

    private static Type STRING(int length) {
        return SEQUENCE(SimpleType.BYTE, length);
    }

    private static CompoundMember META_MEMBER(String name, Type type, Object metadata) {
        CompoundMember member = MEMBER(name, type);
        member.setMetadata(metadata);
        return member;
    }

}
