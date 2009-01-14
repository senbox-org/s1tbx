/* 
 * Copyright (C) 2002-2008 by Brockmann Consult
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.smos.visat;

import org.esa.beam.smos.visat.SnapshotProvider;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import java.util.Arrays;

class SnapshotSelectorComboModel {
    private final ComboBoxModel comboBoxModel;

    public SnapshotSelectorComboModel(SnapshotProvider provider) {
        if (provider.getSnapshotIdsXY().length != 0) {
            comboBoxModel = new DefaultComboBoxModel(
                    new SnapshotSelectorModel[]{
                            new SnapshotSelectorModel(Arrays.asList(provider.getSnapshotIds())),
                            new SnapshotSelectorModel(Arrays.asList(provider.getSnapshotIdsX())),
                            new SnapshotSelectorModel(Arrays.asList(provider.getSnapshotIdsY())),
                            new SnapshotSelectorModel(Arrays.asList(provider.getSnapshotIdsXY()))});
        } else {
            comboBoxModel = new DefaultComboBoxModel(
                    new SnapshotSelectorModel[]{
                            new SnapshotSelectorModel(Arrays.asList(provider.getSnapshotIds())),
                            new SnapshotSelectorModel(Arrays.asList(provider.getSnapshotIdsX())),
                            new SnapshotSelectorModel(Arrays.asList(provider.getSnapshotIdsY()))});
        }
    }

    SnapshotSelectorModel getSelectedSnapshotSelectorModel() {
        return (SnapshotSelectorModel) comboBoxModel.getSelectedItem();
    }

    public ComboBoxModel getComboBoxModel() {
        return comboBoxModel;
    }
}
