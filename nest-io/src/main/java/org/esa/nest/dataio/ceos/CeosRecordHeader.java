/*
 * Copyright (C) 2013 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.nest.dataio.ceos;

import org.esa.nest.dataio.binary.BinaryFileReader;

import java.io.IOException;

/**
 * Check the header of each record
 */
public class CeosRecordHeader {

    private final BinaryFileReader reader;
    private final long startPos;
    private int recordNum;
    private int firstRecordSubtype;
    private int recordTypeCode;
    private int secondRecordSubtype;
    private int thirdRecordSubtype;
    private int recordLength;

    public CeosRecordHeader(final BinaryFileReader reader) throws IOException {
        this.reader = reader;
        startPos = reader.getCurrentPos();

        try {
            recordNum = reader.readB4();
            firstRecordSubtype = reader.readB1();
            recordTypeCode = reader.readB1();
            secondRecordSubtype = reader.readB1();
            thirdRecordSubtype = reader.readB1();
            recordLength = reader.readB4();
        } catch(Exception e) {
            //System.out.println(e.toString() + ':' +e.getCause().toString());
        }
        //System.out.println("\nrec "+recordNum+" type "+recordTypeCode+" length "+recordLength);

        // reset to start pos
        reader.seek(startPos);
    }

    public int getRecordNum() {
        return recordNum;
    }

    public int getRecordLength() {
        return recordLength;
    }

    public void seekToEnd() throws IOException {
        reader.seek(startPos + recordLength);
    }
}
