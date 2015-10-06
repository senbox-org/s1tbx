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

package org.esa.snap.dataio.envisat;

import org.esa.snap.core.util.Debug;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;


class LineInterleavedRecordReader extends RecordReader {

    private final long recordLength;
    private final long recordOffset;
    private final long headerSize;

    /**
     * Constructs a line interleaved dataset reader for ENVISAT products.
     *
     * @param productFile the product file which is creating this reader
     * @param dsd         the DSD which describes the dataset to be read
     * @param recordInfo  the description of the record structure of which the dataset is composed of
     */
    LineInterleavedRecordReader(ProductFile productFile, DSD dsd, RecordInfo recordInfo) throws IOException {
        super(productFile, dsd, recordInfo);

        long rLength = 0;
        long rOffset = 0;
        boolean currentDsdReached = false;
        String[] mdsNames = productFile.getValidDatasetNames('M');
        headerSize = productFile.getDSD(mdsNames[0]).getDatasetOffset();
        for (int i = 0; i < mdsNames.length; i++) {
            //System.out.println("i = " + i);
            final String mdsName = mdsNames[i];
            //System.out.println("mdsName = " + mdsName);
            final DSD currentDSD = productFile.getDSD(mdsName);
            rLength += currentDSD.getRecordSize();
            if (currentDSD == dsd) {
                currentDsdReached = true;
            }
            if (!currentDsdReached) {
                rOffset += currentDSD.getRecordSize();
            }
        }
        recordLength = rLength;
        recordOffset = rOffset;
//        System.out.println("dsd.getDatasetName() = " + dsd.getDatasetName());
//        System.out.println("recordLength = " + recordLength);
//        System.out.println("recordOffset = " + recordOffset);
//        System.out.println("headerSize = " + headerSize);
    }

    /**
     * Reads the record with the given zero-based index from from the product file.
     * <p> In order to reduce memory allocation, the method accepts an optional record argument. If this record is not
     * null, it will be used to read in the data. If it is null, a new record will be created.
     *
     * @param index  the record index, must be <code>&gt;=0</code> and <code>&lt;getDSD().getDatasetOffset()</code>
     * @param record record to be recycled, can be <code>null</code>
     * @throws java.io.IOException if an I/O error occurs
     * @throws java.lang.IndexOutOfBoundsException if the index is out of bounds
     */
    @Override
    public Record readRecord(int index, Record record) throws IOException {

        if (record == null) {
            record = createCompatibleRecord();
        }
        Debug.assertTrue(record.getInfo() == getRecordInfo());

        final ProductFile productFile = getProductFile();
        if (getDSD().getDatasetType() == 'M') {
            index = productFile.getMappedMDSRIndex(index);
        }
        long pos = headerSize + index * recordLength + recordOffset;
        final ImageInputStream istream = productFile.getDataInputStream();
        synchronized (istream) {
            istream.seek(pos);
            record.readFrom(istream);
        }
        return record;
    }

    /**
     * Reads a segment of a single field from the record with the given zero-based index from from the product file.
     * <p> In order to reduce memory allocation, the method accepts an mandantory record argument.
     * It will be used to read in the data.
     *
     * @param sourceY the record index, must be <code>&gt;=0</code> and <code>&lt;getDSD().getDatasetOffset()</code>
     * @param fieldOffset the offset in byte this field has in its containing record
     * @param dataFieldSampleSize the sample rate of the data field element
     * @param minX the first element of the field to read
     * @param maxX the last element of the field to be read
     * @param field the field into which the data is read
     *
     * @throws java.io.IOException if an I/O error occurs
     * @throws java.lang.IndexOutOfBoundsException if the index is out of bounds
     */
    @Override
    public void readFieldSegment(int sourceY, long fieldOffset, int dataFieldSampleSize, int minX, int maxX, Field field) throws IOException {
        if (getDSD().getDatasetType() == 'M') {
            sourceY = getProductFile().getMappedMDSRIndex(sourceY);
        }
        long pos = headerSize +
                    sourceY * recordLength +
                    recordOffset +
                    fieldOffset +
                    minX * dataFieldSampleSize * field.getData().getElemSize();
//        System.out.println("sourceY = " + sourceY+"; pos = " + pos);
        final ImageInputStream istream = getProductFile().getDataInputStream();
        synchronized (istream) {
            istream.seek(pos);
            field.getData().readFrom(minX * dataFieldSampleSize, (maxX-minX+1) * dataFieldSampleSize, istream);
        }
    }
    
}
