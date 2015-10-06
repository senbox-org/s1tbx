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
package org.esa.snap.core.datamodel;

import org.esa.snap.core.dataio.ProductSubsetDef;
import org.esa.snap.core.util.Debug;
import org.esa.snap.core.util.Guardian;
import org.esa.snap.core.util.ObjectUtils;

import java.text.MessageFormat;

/**
 * A <code>DataNode</code> is the base class for all nodes within a data product which carry data. The data is
 * represented by an instance of <code>{@link ProductData}</code>.
 */
public abstract class DataNode extends ProductNode {

    public static final String PROPERTY_NAME_DATA = "data";
    public final static String PROPERTY_NAME_READ_ONLY = "readOnly";
    public final static String PROPERTY_NAME_SYNTHETIC = "synthetic";
    public final static String PROPERTY_NAME_UNIT = "unit";

    /**
     * The data type. Always one of <code>ProductData.TYPE_<i>X</i></code>.
     */
    private final int dataType;
    private final long numElems;
    private ProductData data;
    private boolean readOnly;
    private String unit;
    private boolean synthetic;

    /**
     * Constructs a new data node with the given name, data type and number of elements.
     */
    public DataNode(String name, int dataType, long numElems) {
        super(name);
        if (dataType != ProductData.TYPE_INT8
                && dataType != ProductData.TYPE_INT16
                && dataType != ProductData.TYPE_INT32
                && dataType != ProductData.TYPE_UINT8
                && dataType != ProductData.TYPE_UINT16
                && dataType != ProductData.TYPE_UINT32
                && dataType != ProductData.TYPE_FLOAT32
                && dataType != ProductData.TYPE_FLOAT64
                && dataType != ProductData.TYPE_ASCII
                && dataType != ProductData.TYPE_UTC) {
            throw new IllegalArgumentException("dataType is invalid");
        }
        this.dataType = dataType;
        this.numElems = numElems;
        this.data = null;
        this.readOnly = false;
    }

    public DataNode(String name, ProductData data, boolean readOnly) {
        super(name);
        Guardian.assertNotNull("data", data);
        this.dataType = data.getType();
        this.numElems = data.getNumElems();
        this.data = data;
        this.readOnly = readOnly;
    }

    /**
     * Gets the data type of this data node.
     *
     * @return the data type which is always one of the multiple <code>ProductData.TYPE_<i>X</i></code> constants
     */
    public int getDataType() {
        return dataType;
    }

    /**
     * Tests whether the data type of this node is a floating point type.
     *
     * @return true, if so
     */
    public boolean isFloatingPointType() {
        return ProductData.isFloatingPointType(dataType);
    }

    /**
     * Gets the number of data elements in this data node.
     */
    public long getNumDataElems() {
        checkState();
        return numElems;
    }

    /**
     * Sets the data of this data node.
     */
    public void setData(ProductData data) {

        if (isReadOnly()) {
            throw new IllegalArgumentException("data node '" + getName() + "' is read-only");
        }

        if (this.data == data) {
            return;
        }

        if (data != null) {
            checkDataCompatibility(data);
        }

        ProductData oldData = this.data;
        this.data = data;

        fireProductNodeChanged(PROPERTY_NAME_DATA, oldData, data);
        fireProductNodeDataChanged();
        // if data node already had data before, mark that it has been modified so
        // new data is stored on next incremental save
        if (oldData != null) {
            setModified(true);
        }
    }

    /**
     * Gets the data of this data node.
     */
    public ProductData getData() {
        return data;
    }

    /**
     * Sets the data elements of this data node.
     * @see ProductData#setElems(Object)
     */
    public void setDataElems(Object elems) {

        if (isReadOnly()) {
            throw new IllegalArgumentException("attribute is read-only");
        }

        checkState();
        if (data == null) {
            if (numElems > Integer.MAX_VALUE) {
                throw new IllegalStateException("number of elements must be less than "+ (long)Integer.MAX_VALUE + 1);
            }
            data = createCompatibleProductData((int) numElems);
        }
        Object oldData = data.getElems();
        if (!ObjectUtils.equalObjects(oldData, elems)) {
            data.setElems(elems);
            fireProductNodeDataChanged();
            setModified(true);
        }
    }

    /**
     * Gets the data elements of this data node.
     *
     * @see ProductData#getElems()
     */
    public Object getDataElems() {
        return getData() == null ? null : getData().getElems();
    }

    /**
     * Gets the data element size in bytes.
     *
     * @see ProductData#getElemSize(int)
     */
    public int getDataElemSize() {
        return ProductData.getElemSize(getDataType());
    }

    public void setReadOnly(boolean readOnly) {
        final boolean oldValue = this.readOnly;
        if (oldValue != readOnly) {
            this.readOnly = readOnly;
            fireProductNodeChanged(PROPERTY_NAME_READ_ONLY, oldValue, readOnly);
            setModified(true);
        }
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public void setUnit(String unit) {
        final String oldValue = this.unit;
        if (!ObjectUtils.equalObjects(oldValue, unit)) {
            this.unit = unit;
            fireProductNodeChanged(PROPERTY_NAME_UNIT, oldValue, unit);
            setModified(true);
        }
    }

    public String getUnit() {
        return unit;
    }

//    /**
//     * @deprecated since BEAM 4.10 (not used, no replacement)
//     */
//    @Deprecated
    public boolean isSynthetic() {
        return synthetic;
    }

//    /**
//     * @deprecated since BEAM 4.10 (not used, no replacement)
//     */
//    @Deprecated
    public void setSynthetic(boolean synthetic) {
        final boolean oldValue = this.synthetic;
        if (oldValue != synthetic) {
            this.synthetic = synthetic;
            fireProductNodeChanged(PROPERTY_NAME_SYNTHETIC, oldValue, synthetic);
            setModified(true);
        }
    }

    /**
     * Fires a node data changed event. This method is called after the data of this data node changed.
     */
    public void fireProductNodeDataChanged() {
        final Product product = getProduct();
        if (product != null) {
            product.fireNodeDataChanged(this);
        }
    }

    //////////////////////////////////////////////////////////////////////////

    // Implementation helpers

    /**
     * Checks if the data that should be used to access the data is compatible with the data this node can hold.
     *
     * @param data the data to be checked for compatibility
     * @throws IllegalArgumentException if data is invalid.
     */
    protected void checkDataCompatibility(ProductData data) throws IllegalArgumentException {

        Debug.assertNotNull(data);

        if (data.getType() != getDataType()) {
            String msgPattern = "Illegal data for data node ''{0}'', type {1} expected";
            throw new IllegalArgumentException(MessageFormat.format(msgPattern, getName(), ProductData.getTypeString(getDataType())));
        }

        if (data.getNumElems() != getNumDataElems()) {
            String msgPattern = "Illegal number of data elements for data node ''{0}'', {1} elements expected but was {2}";
            throw new IllegalArgumentException(MessageFormat.format(msgPattern, getName(), getNumDataElems(), data.getNumElems()));
        }
    }

    //////////////////////////////////////////////////////////////////////////

    // 'Visitor' pattern support

    /**
     * Accepts the given visitor. This method implements the well known 'Visitor' design pattern of the gang-of-four.
     * The visitor pattern allows to define new operations on the product data model without the need to add more code
     * to it. The new operation is implemented by the visitor.
     *
     * @param visitor the visitor, must not be <code>null</code>
     */
    @Override
    public abstract void acceptVisitor(ProductVisitor visitor);

    /**
     * Gets the estimated size in bytes of this product node.
     *
     * @param subsetDef if not <code>null</code> the subset may limit the size returned
     * @return the size in bytes.
     */
    @Override
    public long getRawStorageSize(ProductSubsetDef subsetDef) {
        long size = 0L;
        if (isPartOfSubset(subsetDef)) {
            size += 256; // add estimated overhead of 256 bytes
            size += ProductData.getElemSize(getDataType()) * getNumDataElems();
        }
        return size;
    }

    /**
     * Releases all of the resources used by this object instance and all of its owned children. Its primary use is to
     * allow the garbage collector to perform a vanilla job.
     * <p>This method should be called only if it is for sure that this object instance will never be used again. The
     * results of referencing an instance of this class after a call to <code>dispose()</code> are undefined.
     * <p>Overrides of this method should always call <code>super.dispose();</code> after disposing this instance.
     */
    @Override
    public void dispose() {
        if (data != null) {
            data.dispose();
            data = null;
        }
        super.dispose();
    }

    /**
     * Creates product data that is compatible to this dataset's data type. The data buffer returned contains exactly
     * <code>numElems</code> elements of a compatible data type.
     *
     * @param numElems the number of elements, must not be less than one
     * @return product data compatible with this data node
     */
    public ProductData createCompatibleProductData(final int numElems) {
        return ProductData.createInstance(getDataType(), numElems);
    }

    private void checkState() {
        if(numElems < 0) {
            throw new IllegalStateException("number of elements must be at last 1");
        }
    }
}
