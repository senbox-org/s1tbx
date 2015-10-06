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

import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.util.Guardian;

import java.text.ParseException;
import java.util.Date;

/**
 * The <code>Header</code> class represents the contents of ENVISAT main and specific product headers (MPH and SPH). The
 * header parameters are stored as fields within a single <code>Record</code> which this class encloses.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 * @see org.esa.snap.dataio.envisat.Record
 */
public final class Header {

    /**
     * The record containing the header parameters as fields.
     *
     * @link aggregation
     * @associates <{Record}>
     * @supplierCardinality 1
     * @supplierRole -params
     */
    private final Record _params;

    /**
     * Constructs a new header from the given record.
     *
     * @param params the parameters as a list of fields contained in a record
     *
     * @throws java.lang.IllegalArgumentException
     *          if the argument is <code>null</code>
     */
    public Header(Record params) {
        Guardian.assertNotNull("params", params);
        _params = params;
    }

    /**
     * Gets the parameters of this header as a list of fields contained in a record.
     */
    public Record getParams() {
        return _params;
    }

    /**
     * Checks whether the header contains the given key or not.
     *
     * @param key the parameter key to be checked
     *
     * @return true, if so
     */
    public final boolean hasParam(String key) {
        return getParamIndex(key) >= 0;
    }

    /**
     * Checks whether the header is empty or not. An empty header contains no parameters.
     *
     * @return true, if empty
     */
    public final boolean isEmpty() {
        return getNumParams() == 0;
    }

    /**
     * Gets the number of parameters contained in this header. An empty header contains no parameters.
     *
     * @return true, if empty
     */
    public final int getNumParams() {
        return _params.getNumFields();
    }

    /**
     * Gets the header parameter at the specified zero-based index.
     *
     * @return the header parameter
     *
     * @throws java.lang.IndexOutOfBoundsException
     *          if index is <code>&lt;0</code> or <code>&gt;=getNumParams()</code>
     */
    public final Field getParamAt(int index) throws IndexOutOfBoundsException {
        return _params.getFieldAt(index);
    }

    /**
     * Gets the header parameter with the specified parameter key.
     *
     * @param key the parameter key
     *
     * @return the header parameter, or <code>null</code> if a parameter with the given name is not contained in this
     *         header.
     */
    public Field getParam(String key) {
        return _params.getField(key);
    }

    /**
     * Gets the index of the parameter with the given key. The method performs a case-insensitive search.
     *
     * @param key the parameter key
     *
     * @return the parameter index, or <code>-1</code> if a parameter with the given key is not contained in this list.
     */
    public int getParamIndex(String key) {
        return _params.getFieldIndex(key);
    }

    /**
     * Gets the number of elements contained in the value of the parameter with the given key. The method performs a
     * case-insensitive search.
     *
     * @param key the parameter key
     *
     * @return the number of elements contained in the value
     *
     * @throws HeaderEntryNotFoundException if an entry with the given key could not be found in the header
     */
    public int getParamNumElems(String key) throws HeaderEntryNotFoundException {
        return getParamImpl(key).getNumElems();
    }

    /**
     * Gets the <code>int</code> value of the parameter with the given key. The method performs a case-insensitive
     * search.
     *
     * @param key the parameter key
     *
     * @return the parameter value
     *
     * @throws HeaderEntryNotFoundException if an entry with the given key could not be found in the header
     */
    public int getParamInt(String key) throws HeaderEntryNotFoundException {
        return getParamInt(key, 0);
    }

    /**
     * Gets the <code>int</code> value of the parameter with the given key. The method performs a case-insensitive
     * search.
     *
     * @param key the parameter key
     *
     * @return the parameter value
     *
     * @throws HeaderEntryNotFoundException if an entry with the given key could not be found in the header
     */
    public int getParamInt(String key, int elemIndex) throws HeaderEntryNotFoundException {
        return getParamImpl(key).getData().getElemIntAt(elemIndex);
    }

    /**
     * Gets the <code>long</code> value of the parameter with the given key. The method performs a case-insensitive
     * search.
     *
     * @param key the parameter key
     *
     * @return the parameter value
     *
     * @throws HeaderEntryNotFoundException if an entry with the given key could not be found in the header
     */
    public long getParamUInt(String key) throws HeaderEntryNotFoundException {
        return getParamUInt(key, 0);
    }

    /**
     * Gets the <code>long</code> value of the parameter with the given key. The method performs a case-insensitive
     * search.
     *
     * @param key the parameter key
     *
     * @return the parameter value
     *
     * @throws HeaderEntryNotFoundException if an entry with the given key could not be found in the header
     */
    public long getParamUInt(String key, int elemIndex) throws HeaderEntryNotFoundException {
        return getParamImpl(key).getData().getElemUIntAt(elemIndex);
    }

    /**
     * Gets the <code>float</code> value of the parameter with the given key. The method performs a case-insensitive
     * search.
     *
     * @param key the parameter key
     *
     * @return the parameter value
     *
     * @throws HeaderEntryNotFoundException if an entry with the given key could not be found in the header
     */
    public float getParamFloat(String key) throws HeaderEntryNotFoundException {
        return getParamFloat(key, 0);
    }

    /**
     * Gets the <code>float</code> value of the parameter with the given key. The method performs a case-insensitive
     * search.
     *
     * @param key the parameter key
     *
     * @return the parameter value
     *
     * @throws HeaderEntryNotFoundException if an entry with the given key could not be found in the header
     */
    public float getParamFloat(String key, int elemIndex) throws HeaderEntryNotFoundException {
        return getParamImpl(key).getData().getElemFloatAt(elemIndex);
    }

    /**
     * Gets the <code>double</code> value of the parameter with the given key. The method performs a case-insensitive
     * search.
     *
     * @param key the parameter key
     *
     * @return the parameter value
     */
    public double getParamDouble(String key) throws HeaderEntryNotFoundException {
        return getParamDouble(key, 0);
    }

    /**
     * Gets the <code>double</code> value of the parameter with the given key. The method performs a case-insensitive
     * search.
     *
     * @param key the parameter key
     *
     * @return the parameter value
     *
     * @throws HeaderEntryNotFoundException if an entry with the given key could not be found in the header
     */
    public double getParamDouble(String key, int elemIndex) throws HeaderEntryNotFoundException {
        return getParamImpl(key).getData().getElemDoubleAt(elemIndex);
    }

    /**
     * Gets the <code>String</code> value of the parameter with the given key. The method performs a case-insensitive
     * search.
     *
     * @param key the parameter key
     *
     * @return the parameter value
     *
     * @throws HeaderEntryNotFoundException if an entry with the given key could not be found in the header
     */
    public String getParamString(String key) throws HeaderEntryNotFoundException {
        return getParamImpl(key).getAsString();
    }

    /**
     * Gets the <code>java.util.Date</code> value of the parameter with the given key. The method performs a
     * case-insensitive search.
     *
     * @param key the parameter key
     *
     * @return the parameter value
     *
     * @throws HeaderEntryNotFoundException if an entry with the given key could not be found in the header
     */
    public Date getParamDate(String key) throws HeaderParseException,
                                                HeaderEntryNotFoundException {
        return getParamUTC(key).getAsDate();
    }

    /**
     * Gets the {@link ProductData.UTC ProductData.UTC} value of the parameter with
     * the given key. The method performs a case-insensitive search.
     *
     * @param key the parameter key
     *
     * @return the parameter value
     *
     * @throws HeaderEntryNotFoundException if an entry with the given key could not be found in the header
     */
    public ProductData.UTC getParamUTC(String key) throws HeaderParseException,
                                                          HeaderEntryNotFoundException {
        Field field = getParamImpl(key);
        if (field.getDataType() == ProductData.TYPE_ASCII) {
            try {
                return ProductData.UTC.parse(field.getAsString());
            } catch (ParseException e) {
                throw new HeaderParseException(e.getMessage());
            }
        } else if (field.getDataType() == ProductData.TYPE_UTC) {
            return ((ProductData.UTC) field.getData());
        } else {
            throw new HeaderParseException(
                    "Illegal product format: value header entry '" + key + "' is not convertible to UTC date"); /*I18N*/
        }
    }

    /**
     * Gets internal data type of the parameter with the given key. The data type is always one of the multiple
     * <code>TYPE_</code>X constants defined in the <code>ProductData</code>
     * interface.
     *
     * @param key the parameter key
     *
     * @return the data type
     *
     * @throws HeaderEntryNotFoundException if an entry with the given key could not be found in the header
     * @see ProductData
     */
    public final int getParamDataType(String key) throws HeaderEntryNotFoundException {
        return getParamImpl(key).getInfo().getDataType();
    }

    /**
     * Gets the physical unit string of the parameter with the given key. The method performs a case-insensitive
     * search.
     *
     * @param key the parameter key
     *
     * @return the parameter unit, can be <code>null</code> if not set
     *
     * @throws HeaderEntryNotFoundException if an entry with the given key could not be found in the header
     */
    public String getParamUnit(String key) throws HeaderEntryNotFoundException {
        return getParamImpl(key).getInfo().getPhysicalUnit();
    }

    /**
     * Gets the description string  of the parameter with the given key. The method performs a case-insensitive search.
     *
     * @param key the parameter key
     *
     * @return the parameter description, can be <code>null</code> if not set
     *
     * @throws HeaderEntryNotFoundException if an entry with the given key could not be found in the header
     */
    public String getParamDescription(String key) throws HeaderEntryNotFoundException {
        return getParamImpl(key).getInfo().getDescription();
    }

    /**
     * Returns a string representation of this header which can be used for debugging purposes.
     */
    @Override
    public String toString() {
        int n = getNumParams();
        StringBuffer sb = new StringBuffer(32 + 32 * n);
        sb.append("Header[\n");
        sb.append(_params.toString());
        sb.append(']');
        return sb.toString();
    }

    /**
     * Gets the header parameter with the given key. The method performs a case-insensitive search.
     *
     * @param key the parameter key
     *
     * @return the header parameter
     *
     * @throws HeaderEntryNotFoundException if an entry with the given key could not be found in the header
     */
    private Field getParamImpl(String key) throws HeaderEntryNotFoundException {
        Guardian.assertNotNullOrEmpty("key", key);
        Field param = getParam(key);
        if (param == null) {
            throw new HeaderEntryNotFoundException(
                    "Illegal product format: header entry '" + key + "' not found."); /*I18N*/
        }
        return param;
    }
}


