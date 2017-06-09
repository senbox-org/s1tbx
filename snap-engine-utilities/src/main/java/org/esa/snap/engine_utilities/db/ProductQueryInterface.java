/*
 * Copyright (C) 2017 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.engine_utilities.db;

import com.bc.ceres.core.ProgressMonitor;

/**
 * Common search interface for Products from either a database or repository
 */
public interface ProductQueryInterface {

    boolean isReady();

    /**
     * Quickly query with current user requests
     * @param dbQuery query data
     * @return true if results exists
     * @throws Exception
     */
    boolean partialQuery(final DBQuery dbQuery) throws Exception;

    /**
     * Full query
     * @param dbQuery query data
     * @param pm progress monitor
     * @return true if results exists
     * @throws Exception
     */
    boolean fullQuery(final DBQuery dbQuery, final ProgressMonitor pm) throws Exception;

    ProductEntry[] getProductEntryList();

    String[] getAllMissions();

    String[] getAllProductTypes(final String[] missions);

    String[] getAllAcquisitionModes(final String[] missions);
}
