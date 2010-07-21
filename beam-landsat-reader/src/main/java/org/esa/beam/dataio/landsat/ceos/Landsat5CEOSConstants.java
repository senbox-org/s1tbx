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

package org.esa.beam.dataio.landsat.ceos;

public class Landsat5CEOSConstants {

    static final String [] CEOS_HEADER_NAMES = {"VDF_CAL",
            "VDF_SUP",
            "VDF_DAT",
            "VDF_NUL",
            "LEA_01",
            "LEA_02",
            "LEA_03",
            "LEA_04",
            "LEA_05",
            "LEA_06",
            "LEA_07"};
    static final int SIZE_OF_FILE_RECORD = 4320;
    static final String CEOS_LEADFILE_IDENTIFIER = "Lea";
    public static final int CEOS_PATH_ROW_SIZE = 3;
    static final int CEOS_ROW_OFFSET = 32;
    static final int CEOS_PATH_OFFSET = 29;

    static final int [] DEFAULT_BANDS = {1, 2, 3, 4, 5, 6, 7};

    public static final class DataType {

        private final int _dataType;

        private DataType(final int dataType) {
            _dataType = dataType;
        }

        public int toInt() {
            return _dataType;
        }

        public final static DataType INT16 = new DataType(16);
        public final static DataType STRING3 = new DataType(3);
        public final static DataType DATETIME14 = new DataType(14);
        public final static DataType FLOAT16 = new DataType(16);
        public final static DataType DOUBLE16 = new DataType(16);

    }
}
