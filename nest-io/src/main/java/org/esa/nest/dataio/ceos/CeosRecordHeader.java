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
            System.out.println(e.toString() + ':' +e.getCause().toString());
        }
        //System.out.println("\nrec "+recordNum+" type "+recordTypeCode+" length "+recordLength);

        // reset to start pos
        reader.seek(startPos);
    }

    public int getRecordNum() {
        return recordNum;
    }

    public void seekToEnd() throws IOException {
        reader.seek(startPos + recordLength);
    }
}
