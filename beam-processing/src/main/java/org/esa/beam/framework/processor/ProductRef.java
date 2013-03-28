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
package org.esa.beam.framework.processor;

import org.esa.beam.util.Guardian;

import java.io.File;
import java.io.Serializable;
import java.util.Vector;

/**
 * The <code>ProductRef</code> class serves as a leightweight information conatiner for products. It contains three
 * fields: <ul> <li>an URL describing the product location, </li> <li>a file format string and</li> <li>a product type
 * identifier</li> </ul>
 *
 * @deprecated since BEAM 4.11. Use the {@link org.esa.beam.framework.gpf Graph Processing Framework} instead.
 */
@Deprecated
public class ProductRef implements Serializable {

    private File _file;
    private String _typeId;
    private String _fileFormat;
    private Vector<ProductRef> _components;

    /**
     * Constructs a new <code>ProductRef</code> instance with the given product URL. File format and product type ID are
     * initially set to <code>null</code>.
     *
     * @param file the product file
     */
    public ProductRef(File file) throws IllegalArgumentException {
        this(file, null, null);
    }

    /**
     * Constructs a new <code>ProductRef</code> instance with the given product file, file format and producttype ID.
     *
     * @param file       the product file
     * @param fileFormat the file format
     * @param typeId     the product type ID
     */
    public ProductRef(File file, String fileFormat, String typeId) {
        Guardian.assertNotNull("file", file);
        _file = file;
        _fileFormat = fileFormat;
        _typeId = typeId;
    }

    public File getFile() {
        return _file;
    }

    public void setFile(File file) {
        Guardian.assertNotNull("file", file);
        _file = file;
    }

    /**
     * Retrieves the file path of the product or <code>null</code> if no URL is set
     */
    public String getFilePath() {
        if (_file != null) {
            return _file.getPath();
        }
        return null;
    }

    /**
     * Returns the product type ID.
     *
     * @return the product type ID, can be <code>null</code>
     */
    public String getTypeId() {
        return _typeId;
    }

    /**
     * Sets the type identifier for this class.
     */
    public void setTypeId(String typeId) {
        _typeId = typeId;
    }

    /**
     * Returns the file format string.
     *
     * @return the file format string, can be <code>null</code>
     */
    public String getFileFormat() {
        return _fileFormat;
    }

    /**
     * Sets the file format string.
     *
     * @param fileFormat the file format string, can be <code>null</code>
     */
    public void setFileFormat(String fileFormat) {
        _fileFormat = fileFormat;
    }

    /**
     * Adds a component to the <code>ProductRef</code>. Creates the <code>Vector</code> if necessary.
     *
     * @param component the component to be added
     *
     * @throws IllegalArgumentException
     */
    public void addComponent(ProductRef component) {
        Guardian.assertNotNull("component", component);

        if (_components == null) {
            _components = new Vector<ProductRef>();
        }
        _components.addElement(component);
    }

    /**
     * Removes the component passed in from the component list.
     *
     * @param component the component to be removed
     */
    public void removeComponent(ProductRef component) {
        if (_components == null) {
            return;
        }
        _components.removeElement(component);

        if (_components.isEmpty()) {
            _components = null;
        }
    }

    /**
     * Returns the number of components available in the product
     */
    public int getNumComponents() {
        return (_components == null) ? 0 : _components.size();
    }

    /**
     * Returns the component at given index or null if no components are set.
     *
     * @param index the component index
     *
     * @throws ArrayIndexOutOfBoundsException if invalid index is supplied
     */
    public ProductRef getComponentAt(int index) {
        return (_components == null) ? null : _components.elementAt(index);
    }
}
