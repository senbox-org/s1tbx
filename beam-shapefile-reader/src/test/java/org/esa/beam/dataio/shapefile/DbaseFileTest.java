package org.esa.beam.dataio.shapefile;

import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

public class DbaseFileTest extends TestCase {

    public void testRead() throws IOException {
        DBaseTable dBaseTable = new DBaseTable(FileFactory.getFile("/shapefile-samples/sedgrabs/sedgrabs.dbf"));

        assertEquals(3, dBaseTable.getVersion());
        assertEquals("2006-07-20", dBaseTable.getLastModified());
        assertEquals(64, dBaseTable.getRecordCount());
        assertEquals(1793, dBaseTable.getHeaderSize());
        assertEquals(3102, dBaseTable.getRecordSize());

        DBaseTable.Field[] fields = dBaseTable.getFields();
        assertEquals(55, fields.length);
        testColumn(fields[0], "ID", 'N', 10, 0, false);
        testColumn(fields[1], "LABNO", 'C', 254, 0, false);
        testColumn(fields[2], "STATIONID", 'C', 254, 0, false);
        testColumn(fields[3], "PROJECTID", 'C', 254, 0, false);
        testColumn(fields[4], "CRUISEID", 'C', 254, 0, false);
        testColumn(fields[5], "PRINCIPAL", 'C', 254, 0, false);
        testColumn(fields[6], "MONTH", 'N', 10, 0, false);
        testColumn(fields[7], "DAY", 'N', 10, 0, false);
        testColumn(fields[8], "YEAR", 'N', 10, 0, false);
        testColumn(fields[9], "LATITUDE", 'N', 19, 11, false);
        testColumn(fields[10], "LONGITUDE", 'N', 19, 11, false);
        testColumn(fields[11], "DEVICE", 'C', 254, 0, false);
        testColumn(fields[12], "AREA", 'C', 254, 0, false);
        testColumn(fields[13], "DEPTH_M", 'N', 19, 11, false);
        testColumn(fields[14], "T_DEPTH", 'N', 10, 0, false);
        testColumn(fields[15], "B_DEPTH", 'N', 10, 0, false);
        testColumn(fields[16], "WEIGHT", 'N', 19, 11, false);
        testColumn(fields[17], "ZGRAVEL", 'N', 19, 11, false);
        testColumn(fields[18], "ZSAND", 'N', 19, 11, false);

        dBaseTable.close();
    }

    private void testColumn(DBaseTable.Field field, String fieldName, char fieldType, int fieldLength, int decimalCount, boolean indexFieldFlag) {
        assertEquals(fieldName, field.getName());
        assertEquals(fieldType, field.getType());
        assertEquals(fieldLength, field.getLength());
        assertEquals(decimalCount, field.getDecimalCount());
        assertEquals(indexFieldFlag, field.isIndexFlag());
    }
}
