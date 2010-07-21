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

/**
 * Clients can implement <code>RepositoryManagerListener</code> to be informed
 * on changes of {@link RepositoryManager}.
 *
 * @author Marco Peters
 */
public interface RepositoryManagerListener {

    /**
     * Implementation should handle that a new <code>Repository<code> was added.
     *
     * @param repository the <code>Repository<code> that was added.
     */
    void repositoryAdded(Repository repository);

    /**
     * Implementation should handle that a new <code>Repository<code> was removed.
     *
     * @param repository the <code>Repository<code> that was removed.
     */
    void repositoryRemoved(Repository repository);

}
