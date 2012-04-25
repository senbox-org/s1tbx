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
package org.esa.beam.visat.actions.pgrab.model.dataprovider;

import java.io.IOException;
import java.util.Comparator;

import javax.swing.table.TableColumn;

import org.esa.beam.visat.actions.pgrab.model.Repository;
import org.esa.beam.visat.actions.pgrab.model.RepositoryEntry;

/**
 * This interface shall be implemented to provide new data to a {@link RepositoryEntry}.
 * The data is shown in a table within the <code>ProductGrabber</code>.
 * <p/>
 * <p> To add a <code>DataProvider</code> to the <code>ProductGrabber</code> use the following example code:
 * <p/>
 * <code>
 * ProductGrabberVPI.getInstance().getRepositoryManager().addDataProvider(new SampleDataProvider());
 * </code>
 * </p>
 * </p>
 */
public interface DataProvider {

    /**
     * Implementation should check if the data this <code>DataProvider</code> provides must be created, or if it is
     * already stored.
     *
     * @param entry      the entry for which the data shall be provided.
     * @param repository the repsoitory containing the entry.
     *
     * @return true, if the data must be created, otherwise false.
     */
    boolean mustCreateData(RepositoryEntry entry, Repository repository);

    /**
     * Implementation should create the data this <code>DataProvider</code> provides.
     * Also the created should be stored for performance reasons.
     * Created data can be stored into a {@link org.esa.beam.util.PropertyMap PropertyMap} retrieved by calling
     * {@link org.esa.beam.visat.actions.pgrab.model.Repository#getPropertyMap() Repository.getPropertyMap()}
     * or in a directory retrieved from
     * {@link org.esa.beam.visat.actions.pgrab.model.Repository#getStorageDir() Repository.getStorageDir()}.
     *
     * @param entry      the entry for which the data shall be provided.
     * @param repository the repository containing the entry. // todo - (from nf)  for what? entry knows it repository!
     *
     * @throws IOException if an error occurs during creating the data.
     */
    void createData(RepositoryEntry entry, Repository repository) throws IOException;

    /**
     * Returns the data which is provided by this implementation.
     *
     * @param entry      the entry for which the data shall be provided.
     * @param repository the repository containing the entry. // todo - (from nf)  for what? entry knows it repository!
     *
     * @return the provided data.
     *
     * @throws IOException if an error occurs during providing the data.
     */
    Object getData(RepositoryEntry entry, Repository repository) throws IOException;

    /**
     * Returns the {@link Comparator} for the data provided by this <code>DataProvider</code>.
     *
     * @return the comparator.
     */
    Comparator getComparator();

    /**
     * Implementation should delete all stored data.
     *
     * @param entry      the entry for which the data was provided.
     * @param repository the repository contained the entry.    // todo - (from nf)  for what? entry knows it repository!
     */
    void cleanUp(RepositoryEntry entry, Repository repository);

    /**
     * Returns a {@link TableColumn} which defines the UI representation of the provided data within a
     * {@link javax.swing.JTable Table}.
     *
     * @return the {@link TableColumn}.
     */
    TableColumn getTableColumn();

}
