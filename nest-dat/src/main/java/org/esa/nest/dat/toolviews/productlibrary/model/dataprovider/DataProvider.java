/*
 * Copyright (C) 2012 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.nest.dat.toolviews.productlibrary.model.dataprovider;

import javax.swing.table.TableColumn;
import java.util.Comparator;

/**

 */
public interface DataProvider {

    /**
     * Returns the {@link java.util.Comparator} for the data provided by this <code>DataProvider</code>.
     *
     * @return the comparator.
     */
    Comparator getComparator();

    /**
     * Returns a {@link javax.swing.table.TableColumn} which defines the UI representation of the provided data within a
     * {@link javax.swing.JTable Table}.
     *
     * @return the {@link javax.swing.table.TableColumn}.
     */
    TableColumn getTableColumn();

}