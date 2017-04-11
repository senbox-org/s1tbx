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

/**
 * A <code>RecordReader</code> instance is used read records of for a particular dataset contained in an ENVISAT product
 * file.
 * <p> It is composed of a dataset descriptor (DSD) which provides size and offset of the entire dataset and a
 * <code>RecordInfo</code> which comes from a static database (DDDB) and describes in detail the structure of the
 * records to be read.
 * <p> The <code>RecordInfo</code> is also used as a factory to create new record instances. The actual record reading
 * is delegated to this record instances.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 * @see org.esa.snap.dataio.envisat.DSD
 * @see org.esa.snap.dataio.envisat.Record
 * @see org.esa.snap.dataio.envisat.RecordInfo
 */
public class RecordReader {

    /**
     * The DSD which describes the dataset to be read.
     *
     * @supplierCardinality 1
     * @supplierRole -productFile
     */
    private final ProductFile _productFile;

    /**
     * The DSD which describes the dataset to be read.
     *
     * @supplierCardinality 1
     * @supplierRole -dsd
     */
    private final DSD _dsd;

    /**
     * The description of the record structure of which the dataset is composed of.
     *
     * @supplierCardinality 1
     * @supplierRole -recordInfo
     */
    private final RecordInfo _recordInfo;

    /**
     * Constructs a new dataset reader for ENVISAT products.
     *
     * @param productFile the product file which is creating this reader
     * @param dsd         the DSD which describes the dataset to be read
     * @param recordInfo  the description of the record structure of which the dataset is composed of
     */
    RecordReader(ProductFile productFile, DSD dsd, RecordInfo recordInfo) {
        Debug.assertNotNull(productFile);
        Debug.assertNotNull(dsd);
        Debug.assertNotNull(recordInfo);
        _productFile = productFile;
        _dsd = dsd;
        _recordInfo = recordInfo;
    }

    /**
     * Gets the product file which created this reader.
     *
     * @return the product file
     */
    public final ProductFile getProductFile() {
        return _productFile;
    }

    /**
     * Gets the DSD which describes offset, size an number of records contained in the product file.
     */
    public final DSD getDSD() {
        return _dsd;
    }

    /**
     * Gets the description of the record structure of which the dataset is composed of.
     */
    public final RecordInfo getRecordInfo() {
        return _recordInfo;
    }

    /**
     * Gets the number of records contained in the product file. <p> The method is a shortcut for
     * <code>getDSD().getNumRecords()</code>.
     *
     * @see org.esa.snap.dataio.envisat.DSD
     */
    public final int getNumRecords() {
        return getDSD().getNumRecords();
    }


    /**
     * Reads a record from the product file and returns the instance.
     * <p> If the dataset is composed of multiple records this method only reads the first.
     * <p>Note that the method creates a new record instance each time it is called.
     *
     * @throws java.io.IOException if an I/O error occurs
     * @see #readRecord(int, org.esa.snap.dataio.envisat.Record)
     */
    public final Record readRecord() throws IOException {
        return readRecord(0, null);
    }

    /**
     * Reads the record with the given zero-based index from the product file.
     * <p>Note that the method creates a new record instance each time it is called.
     *
     * @param index the record index, must be <code>&gt;=0</code> and <code>&lt;getDSD().getDatasetOffset()</code>
     * @throws java.io.IOException if an I/O error occurs
     * @throws java.lang.IndexOutOfBoundsException
     *                             if the index is out of bounds
     * @see #readRecord(int, org.esa.snap.dataio.envisat.Record)
     */
    public final Record readRecord(int index) throws IOException {
        return readRecord(index, null);
    }

    /**
     * Reads the record with the given zero-based index from from the product file.
     * <p> In order to reduce memory allocation, the method accepts an optional record argument. If this record is not
     * null, it will be used to read in the data. If it is null, a new record will be created.
     *
     * @param index  the record index, must be <code>&gt;=0</code> and <code>&lt;getDSD().getDatasetOffset()</code>
     * @param record record to be recycled, can be <code>null</code>
     * @throws java.io.IOException if an I/O error occurs
     * @throws java.lang.IndexOutOfBoundsException
     *                             if the index is out of bounds
     */
    public Record readRecord(int index, Record record) throws IOException {

        if (record == null) {
            record = createCompatibleRecord();
        }
        Debug.assertTrue(record.getInfo() == _recordInfo);

        if (_dsd.getDatasetType() == 'M') {
            index = _productFile.getMappedMDSRIndex(index);
        }
        final long pos = _dsd.getDatasetOffset() + (index * _dsd.getRecordSize());
        final ImageInputStream istream = _productFile.getDataInputStream();
        synchronized (istream) {
            istream.seek(pos);
            record.readFrom(istream);
        }
        return record;
    }

    /**
     * Factory method which creates a new record wich is compatible to the record description which was passed to the
     * constructor. <p> The method is a shortcut for <code>getRecordInfo().createRecord()</code>.
     *
     * @return a new record instance
     */
    public Record createCompatibleRecord() {
        return getRecordInfo().createRecord();
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
     * @throws java.lang.IndexOutOfBoundsException
     *                             if the index is out of bounds
     */
    public void readFieldSegment(int sourceY, long fieldOffset, int dataFieldSampleSize, int minX, int maxX, Field field) throws IOException {
        if (_dsd.getDatasetType() == 'M' || _dsd.getDatasetType() == 'A') {
            sourceY = _productFile.getMappedMDSRIndex(sourceY);
        }
        final long pos = _dsd.getDatasetOffset() + 
        					sourceY * _dsd.getRecordSize() + 
        					fieldOffset + 
        					minX * dataFieldSampleSize * field.getData().getElemSize();
        final ImageInputStream istream = _productFile.getDataInputStream();
        synchronized (istream) {
            istream.seek(pos);
            field.getData().readFrom(minX * dataFieldSampleSize, (maxX-minX+1) * dataFieldSampleSize, istream);
        }
        
    }

}

