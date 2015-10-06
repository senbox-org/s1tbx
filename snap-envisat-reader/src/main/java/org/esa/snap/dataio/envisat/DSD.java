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
import org.esa.snap.core.util.Guardian;

/**
 * The DSD structure represents a data set descriptor (DSD) found in the specific product header (SPH) of MERIS level 1b
 * and level 2 products.
 * <p>
 * This class serves as a data holder for DSD information, its only functionality is provided by the <code>adjust</code>
 * method, which corrects a DSD instance created from a defective SPH.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 * @see org.esa.snap.dataio.envisat.DSD#adjust(long, int)
 * @see org.esa.snap.dataio.envisat
 */
public class DSD {

    /**
     * the zero-based index of this DSD within the SPH.
     */
    private final int index;
    /**
     * the name of the dataset.
     */
    private final String datasetName;
    /**
     * the type of the dataset.
     */
    private final char datasetType;
    /**
     * the file name of an external dataset types.
     */
    private final String fileName;
    /**
     * the dataset offset in bytes within the data product file.
     */
    private long datasetOffset;
    /**
     * the dataset size in bytes.
     */
    private long datasetSize;
    /**
     * the number of records contained in the dataset.
     */
    private final int numRecords;
    /**
     * the size in bytes of each record contained in the dataset.
     */
    private int recordSize;

    /**
     * Constructs a new DSD from the given SPH parameters.
     *
     * @param index         the zero-based index of this DSD within the SPH.
     * @param datasetName   the name of the dataset.
     * @param datasetType   the type of the dataset.
     * @param fileName      the file name of an external dataset types (type 'R').
     * @param datasetOffset the dataset offset in bytes within the data product file.
     * @param datasetSize   the dataset size in bytes.
     * @param numRecords    the number of records contained in the dataset.
     * @param recordSize    the size in bytes of each record contained in the dataset.
     */
    public DSD(int index,
               String datasetName,
               char datasetType,
               String fileName,
               long datasetOffset,
               long datasetSize,
               int numRecords,
               int recordSize) {
        Debug.assertTrue(datasetName != null);
        this.index = index;
        this.datasetName = datasetName;
        this.datasetType = datasetType;
        this.fileName = fileName;
        this.datasetOffset = datasetOffset;
        this.datasetSize = datasetSize;
        this.numRecords = numRecords;
        this.recordSize = recordSize;
    }

    /**
     * Returns the zero-based index of this DSD within the SPH.
     */
    public final int getIndex() {
        return index;
    }

    /**
     * Returns the name of the dataset.
     */
    public final String getDatasetName() {
        return datasetName;
    }

    /**
     * Returns the type of the dataset.
     */
    public final char getDatasetType() {
        return datasetType;
    }

    /**
     * Returns file name of an external dataset type.
     */
    public final String getFileName() {
        return fileName;
    }

    /**
     * Returns the dataset offset in bytes within the data product file.
     */
    public final long getDatasetOffset() {
        return datasetOffset;
    }

    /**
     * Returns the dataset size in bytes.
     */
    public final long getDatasetSize() {
        return datasetSize;
    }

    /**
     * Returns the number of records contained in the dataset.
     */
    public final int getNumRecords() {
        return numRecords;
    }

    /**
     * Returns the size in bytes of each record contained in the dataset.
     */
    public final int getRecordSize() {
        return recordSize;
    }

    /**
     * Sets the dataset offset in bytes within the data product file and the size in bytes of each record contained in
     * the dataset. The method then adjusts the dataset size accordingly.
     * <p> The primary use of this method is to correct defective DSD entries found in the SPH.
     *
     * @param datasetOffset the dataset offset in bytes within the data product file.
     * @param recordSize    the size in bytes of each record contained in the dataset.
     *
     * @throws java.lang.IllegalArgumentException
     *          if one of the arguments is <code>&lt;= 0</code>.
     */
    public final void adjust(long datasetOffset, int recordSize) {
        Guardian.assertGreaterThan("datasetOffset", datasetOffset, 0);
        Guardian.assertGreaterThan("recordSize", recordSize, 0);
        this.datasetOffset = datasetOffset;
        this.recordSize = recordSize;
        this.datasetSize = this.numRecords * this.recordSize;
    }

    /**
     * Tests if the datasets which is described by this DSD is empty. This can be true for some datasets in so-called
     * ENVISAT "child" products.
     *
     * @return true, if so
     */
    public final boolean isDatasetEmpty() {
        return datasetSize == 0 || numRecords == 0 || recordSize == 0;
    }

    /**
     * Returns a string representation of this header which can be used for debugging purposes.
     */
    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer(64);
        sb.append("DSD(");
        sb.append(index);
        sb.append(")['");
        sb.append(datasetName);
        sb.append("','");
        sb.append(datasetType);
        sb.append("','");
        sb.append(fileName);
        sb.append("',");
        sb.append(datasetOffset);
        sb.append(',');
        sb.append(datasetSize);
        sb.append(',');
        sb.append(numRecords);
        sb.append(',');
        sb.append(recordSize);
        sb.append(']');
        return sb.toString();
    }
}
