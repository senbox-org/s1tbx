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


/**
 * The <code>DataTypes</code> interface enumerates all valid native data types for fields within an ENVISAT dataset
 * record.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 * @see org.esa.snap.dataio.envisat.Record
 * @see org.esa.snap.dataio.envisat.RecordInfo
 * @see org.esa.snap.dataio.envisat.Field
 * @see org.esa.snap.dataio.envisat.FieldInfo
 */
public interface DataTypes {

    /**
     * The ID for an undefined data type. Its value is <code>0</code>.
     */
    int TYPE_UNDEFINED = 0;

    /**
     * The ID for a signed 8-bit integer data type. Its value is <code>1</code>.
     */
    int TYPE_INT8 = 1;

    /**
     * The ID for an unsigned 8-bit integer data type. Its value is <code>2</code>.
     */
    int TYPE_UINT8 = 2;

    /**
     * The ID for a signed 16-bit integer data type. Its value is <code>3</code>.
     */
    int TYPE_INT16 = 3;

    /**
     * The ID for an unsigned 16-bit integer data type. Its value is <code>4</code>.
     */
    int TYPE_UINT16 = 4;

    /**
     * The ID for a signed 32-bit integer data type. Its value is <code>5</code>.
     */
    int TYPE_INT32 = 5;

    /**
     * The ID for an unsigned 32-bit integer data type. Its value is <code>6</code>.
     */
    int TYPE_UINT32 = 6;

    /**
     * The ID for a signed 32-bit floating point data type. Its value is <code>10</code>.
     */
    int TYPE_FLOAT32 = 10;

    /**
     * The ID for a signed 64-bit floating point data type. Its value is <code>11</code>.
     */
    int TYPE_FLOAT64 = 11;

    /**
     * The ID for a UTC time data type. Its value is <code>20</code>.
     */
    int TYPE_UTC = 20;

    /**
     * The ID for string character data. Its value is <code>21</code>.
     */
    int TYPE_INT8S = 21;
}
