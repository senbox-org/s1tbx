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

import org.esa.snap.core.util.Guardian;

import java.util.Vector;

/**
 * The <code>RecordInfo</code> class contains the information about the structure of a particular record.
 * <p> A <code>RecordInfo</code> instance has a record name and contains a list of <code>FieldInfo</code> instances
 * describing each field of a record.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 * @see FieldInfo
 * @see Field
 * @see Record
 */
public final class RecordInfo extends ItemInfo {

    /**
     * The total size in bytes.
     */
    private int _sizeInBytes = 0;

    /**
     * @link aggregation
     * @associates <{FieldInfo}>
     * @supplierCardinality 0..*
     * @supplierRole -fieldInfos
     */
    private final Vector _fieldInfos = new Vector();

    /**
     * Constructs a new and empty record-info. Field-infos must be added using the <code>add</code> method.
     *
     * @param recordName the record's name
     */
    RecordInfo(String recordName) {
        super(recordName, null);
    }

    /**
     * Factory method which creates a new record instance based on the structure description provided by this
     * record-info.
     *
     * @return a new record instance
     */
    public Record createRecord() {
        return Record.create(this);
    }

    /**
     * Returns the total size in bytes of this record-info. <p> Note that the size value may change while new
     * field-infos are added to this record-info.
     */
    public final int getSizeInBytes() {
        return _sizeInBytes;
    }

    /**
     * Returns the number of field-infos contained in this record-info.
     */
    public final int getNumFieldInfos() {
        return _fieldInfos.size();
    }

    /**
     * Gets the field-info with the given index.
     *
     * @param index the zero-based field-info index, must be <code>&gt;= 0</code> and <code>&lt;
     *              getNumFieldInfos()</code>
     *
     * @return the field-info with the given index
     *
     * @throws java.lang.IndexOutOfBoundsException
     *          if index is <code>&lt;0</code> or <code>&gt;=getNumFieldInfos()</code>
     */
    public final FieldInfo getFieldInfoAt(int index) {
        return (FieldInfo) _fieldInfos.elementAt(index);
    }

    /**
     * Returns the field for the given field name. The method performs a case-insensitive search.
     *
     * @param fieldName the field name, must not be null
     *
     * @return the field info or null if no field with the given name was found
     *
     * @throws java.lang.IllegalArgumentException
     *          if the given name is null
     */
    public final FieldInfo getFieldInfo(String fieldName) {
        int index = getFieldInfoIndex(fieldName);
        return index >= 0 ? getFieldInfoAt(index) : null;
    }

    /**
     * Returns the index of the field info for the given field name or <code>-1</code> if this record info does not a
     * contain a field info with the given name. The method performs a case-insensitive search.
     *
     * @param fieldName the field name, must not be null
     *
     * @return the field info index, or <code>-1</code> if no field with the given name was found
     *
     * @throws java.lang.IllegalArgumentException
     *          if the given name is null
     */
    public final int getFieldInfoIndex(String fieldName) {
        Guardian.assertNotNull("fieldName", fieldName);
        int n = getNumFieldInfos();
        for (int i = 0; i < n; i++) {
            if (getFieldInfoAt(i).isNameEqualTo(fieldName)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Adds a new field-info given by the supplied arguments to this record-info.
     *
     * @param fieldName   the field's name
     * @param dataType    the data type
     * @param numElemns   the number of elements
     * @param unit        the optional unit text
     * @param description the optional description   text
     *
     * @throws java.lang.IllegalArgumentException
     *          if field name is null or empty or if a field with the name already exists
     * @see <{FieldInfo}>
     */
    final void add(String fieldName,
                   int dataType,
                   int numElemns,
                   String unit,
                   String description) {
        add(new FieldInfo(fieldName, dataType, numElemns, unit, description));
    }

    /**
     * Adds a new field-info to this record-info. The given field-info is cloned before it is added to the internal
     * field-info list.
     *
     * @param fieldInfo the field-info to be copied and added
     *
     * @throws java.lang.IllegalArgumentException
     *          if fieldInfo is null or a field with the name already exists
     * @see <{FieldInfo}>
     */
    final void add(FieldInfo fieldInfo) {
        Guardian.assertNotNull("fieldInfo", fieldInfo);
        checkName(fieldInfo.getName());
        _fieldInfos.addElement(fieldInfo);
        updateSizeInBytes();
    }

    /**
     * Adds a copy of a recordinfo with a new prefix
     *
     * @param subRecord the record info to be added
     * @param prefix the prefix string to add to the name of each field
     *
     * @throws java.lang.IllegalArgumentException
     *          if recordInfo is null or one of the fields already exists
     * @see <{FieldInfo}>
     */
    final void add(RecordInfo subRecord, String prefix) {

        for (int numFields = 0; numFields < subRecord.getNumFieldInfos(); numFields++) {
            FieldInfo f = subRecord.getFieldInfoAt(numFields);
            _fieldInfos.addElement(new FieldInfo(prefix + '.' + f.getName(), f.getDataType(), f.getNumDataElems(),
                              f.getPhysicalUnit(), f.getDescription()) );
        }
    }

    /**
     * Adds all fields of the given record info to this record info.
     *
     * @param recordInfo the record info whose fields to be added
     *
     * @throws java.lang.IllegalArgumentException
     *          if recordInfo is null or one of the fields already exists
     */
    final void add(RecordInfo recordInfo) {
        Guardian.assertNotNull("recordInfo", recordInfo);
        for (int n = 0; n < recordInfo.getNumFieldInfos(); n++) {
            add(recordInfo.getFieldInfoAt(n));
        }
    }

    /**
     * Updates the record size in bytes. This methos is called after a new field-info has been added to this
     * record-info,
     */
    void updateSizeInBytes() {
        _sizeInBytes = 0;
        int n = getNumFieldInfos();
        for (int i = 0; i < n; i++) {
            _sizeInBytes += getFieldInfoAt(i).getSizeInBytes();
        }
    }

    /**
     * Returns the offset in bytes of a field given by its index.
     * @param fieldIndex The field index.
     * @return The offset in bytes.
     * @since BEAM 5
     */
    public long getFieldOffset(int fieldIndex) {
        long offset = 0;
        for (int i = 0; i < fieldIndex; i++) {
            FieldInfo fieldInfo = getFieldInfoAt(i);
            offset += fieldInfo.getSizeInBytes();
        }
        return offset;
    }

    /**
     * Returns a string representation of this record-info which can be used for debugging purposes.
     */
    @Override
    public String toString() {
        int n = getNumFieldInfos();
        StringBuffer sb = new StringBuffer(32 + 32 * n);
        sb.append("RecordInfo['");
        sb.append(getName());
        sb.append("',");
        sb.append(getSizeInBytes());
        sb.append(",\n");
        for (int i = 0; i < n; i++) {
            sb.append("  ");
            sb.append(getFieldInfoAt(i).toString());
            if (i < n - 1) {
                sb.append(',');
            }
            sb.append('\n');
        }
        sb.append(']');
        return sb.toString();
    }

    /**
     * Adds a name prefix to all fields in this record. new_name = prefix.old_name
     *
     * @param prefix the prefix string to be added.
     */
    @Override
    public void setNamePrefix(String prefix) {
        FieldInfo info = null;
        for (int n = 0; n < _fieldInfos.size(); n++) {
            info = (FieldInfo) _fieldInfos.elementAt(n);
            info.setNamePrefix(prefix);
        }
    }

    /**
     * @throws java.lang.IllegalArgumentException
     *          if a field info with the given name is already contained in this record info object.
     */
    private void checkName(String name) {
        int n = getNumFieldInfos();
        for (int i = 0; i < n; i++) {
            if (getFieldInfoAt(i).isNameEqualTo(name)) {
                throw new IllegalArgumentException("a field with the given name '" + name + "' already exists here");
            }
        }
    }

}



