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

package org.esa.beam.visat.actions.pgrab.model;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.esa.beam.dataio.dimap.DimapProductConstants;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.util.io.FileUtils;

public class RepositoryEntry {

    private final File _productFile;
    private Repository _repository;
    private final ArrayList _dataList;
    private Product _product;

    public RepositoryEntry(final String productFilePath) {
        _productFile = new File(productFilePath);
        _dataList = new ArrayList();
    }

    /**
     * Set the <code>Repository</code> to which this <code>RepositoryEntry</code> contains.
     *
     * @param repository the repository.
     */
    public void setOwner(final Repository repository) {
        this._repository = repository;
    }

    /**
     * Gets the owning <code>Repository</code> of this <code>RepositoryEntry</code>
     *
     * @return the repository.
     */
    public Repository getOwner() {
        return _repository;
    }

    public File getProductFile() {
        return _productFile;
    }

    public void openProduct() throws IOException {
        _product = ProductIO.readProduct(_productFile);
    }

    public void closeProduct() {
        if (_product != null) {
            _product.dispose();
            _product = null;
        }
    }

    public Product getProduct() {
        return _product;
    }

    public void setData(final int index, final Object data) {
        if (index >= _dataList.size()) {
            _dataList.add(data);
        } else {
            _dataList.set(index, data);
        }
    }

    public Object getData(final int index) {
        if (index >= 0 && index < _dataList.size()) {
            return _dataList.get(index);
        } else {
            return null;
        }
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof RepositoryEntry)) {
            return false;
        }
        final RepositoryEntry entry = (RepositoryEntry) obj;
        if (!this._productFile.equals(entry._productFile)) {
            return false;
        }
        return true;
    }

    /**
     * Returns the size of the Product in megabytes.
     *
     * @return the size in megabytes.
     */
    public float getProductSize() {
        final File productFile = getProductFile();
        final String extension = FileUtils.getExtension(productFile);
        long dirSize = 0;
        if (productFile.exists() &&
            productFile.getParentFile() != null &&
            DimapProductConstants.DIMAP_HEADER_FILE_EXTENSION.equals(extension)) {
            final File realtedDataDir = new File(productFile.getParentFile(),
                                                 FileUtils.getFilenameWithoutExtension(productFile) + DimapProductConstants.DIMAP_DATA_DIRECTORY_EXTENSION);
            if (realtedDataDir.exists()) {
                final File[] files = realtedDataDir.listFiles();
                for (int i = 0; i < files.length; i++) {
                    dirSize += files[i].length();
                }
            }
        }
        return (productFile.length() + dirSize) / (1024.0f * 1024.0f);
    }

}
