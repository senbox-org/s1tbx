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
 * The <code>Record</code> class represents a record in a dataset of of an ENVISAT product.
 * <p> A record performs its <code>readFrom</code> operation by delegating it sequentially to all of its fields.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 */
public class Record {

    /**
     * The info for this record.
     *
     * @supplierCardinality 1
     * @supplierRole -info
     */
    private final RecordInfo _info;

    /**
     * This record's fields.
     *
     * @link aggregation
     * @supplierRole -fields
     */
    private final Field[] _fields;

    /**
     * Constructs a new record for the given record info. The record info tells the constructor which and how many
     * fields to be internally created.
     *
     * @param info the record info, must not be null
     */
    private Record(RecordInfo info) {
        Debug.assertTrue(info != null);
        _info = info;
        _fields = new Field[info.getNumFieldInfos()];
        for (int i = 0; i < _fields.length; i++) {
            _fields[i] = info.getFieldInfoAt(i).createField();
        }
    }

    /**
     * Factory method which creates a new record for the given record info. The record info tells which and how many
     * fields shall be internally created for the new record.
     *
     * @param info the record info, must not be null
     *
     * @return a new record instance
     */
    public static Record create(RecordInfo info) {
        return new Record(info);
    }

    /**
     * Returns the record info for this record.
     */
    public final RecordInfo getInfo() {
        return _info;
    }

    /**
     * Gets the name of this record.
     */
    public final String getName() {
        return getInfo().getName();
    }

    /**
     * Gets the total record size in bytes.
     */
    public final int getSizeInBytes() {
        return getInfo().getSizeInBytes();
    }

    /**
     * Gets the total number of fields contained in this record.
     */
    public final int getNumFields() {
        return _fields.length;
    }

    /**
     * Gets the field with the given index.
     *
     * @param index the field index, must be
     *
     * @throws java.lang.ArrayIndexOutOfBoundsException
     *          if the index is out of bounds
     */
    public final Field getFieldAt(int index) throws ArrayIndexOutOfBoundsException {
        return _fields[index];
    }

    /**
     * Returns the field for the given field name. The method performs a case-insensitive search.
     */
    public final Field getField(String fieldName) {
        int index = getFieldIndex(fieldName);
        return index >= 0 ? getFieldAt(index) : null;
    }

    /**
     * Returns the index of the field for the given field name or <code>-1</code> if this record does not a contain a
     * field with the given name. The method performs a case-insensitive search.
     *
     * @param fieldName the field name
     *
     * @return the field index, or <code>-1</code>
     */
    public final int getFieldIndex(String fieldName) {
        return getInfo().getFieldInfoIndex(fieldName);
    }

    /**
     * Reads the record data from the given input stream. The method delegates the <code>readFrom</code> call
     * sequentially to all fields of this record.
     *
     * @param dataInputStream a seekable data input stream
     *
     * @throws java.io.IOException if an I/O error occurs
     */
    public void readFrom(ImageInputStream dataInputStream) throws IOException {
        for (Field _field : _fields) {
            _field.readFrom(dataInputStream);
        }
    }

    /**
     * Returns a string representation of this record which can be used for debugging purposes.
     */
    @Override
    public String toString() {
        int n = getNumFields();
        StringBuffer sb = new StringBuffer(32 + 32 * n);
        sb.append("Record('");
        sb.append(getName());
        sb.append("')[\n");
        for (int i = 0; i < n; i++) {
            sb.append("  ");
            sb.append(getFieldAt(i).toString());
            if (i < n - 1) {
                sb.append(',');
            }
            sb.append('\n');
        }
        sb.append(']');
        return sb.toString();
    }
}


