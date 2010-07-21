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

package org.esa.beam.framework.ui.crs;

import javax.swing.AbstractListModel;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Marco Peters
* @version $ Revision $ Date $
* @since BEAM 4.6
*/
class CrsInfoListModel extends AbstractListModel {

    private final List<CrsInfo> crsList;

    CrsInfoListModel(List<CrsInfo> crsList) {
        this.crsList = new ArrayList<CrsInfo>(crsList);
    }

    @Override
    public CrsInfo getElementAt(int index) {
        return crsList.get(index);
    }

    @Override
    public int getSize() {
        return crsList.size();
    }
}
