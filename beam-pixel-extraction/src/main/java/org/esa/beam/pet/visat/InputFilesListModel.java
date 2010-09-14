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

package org.esa.beam.pet.visat;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.ValidationException;

import javax.swing.AbstractListModel;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Thomas Storm
 */
class InputFilesListModel extends AbstractListModel {

    private List<File> list = new ArrayList<File>();
    private Property property;

    InputFilesListModel(Property property) {
        this.property = property;
    }

    @Override
    public File getElementAt(int index) {
        return list.get(index);
    }

    @Override
    public int getSize() {
        return list.size();
    }

    void addElement(File... elements) throws ValidationException {
        for (File element : elements) {
            if (!list.contains(element)) {
                list.add(element);
            }
        }
        updateProperty();
        fireIntervalAdded(this, 0, list.size());
    }

    void removeElement(File... elements) {
        for (File element : elements) {
            list.remove(element);
        }
        try {
            updateProperty();
        } catch (ValidationException ignored) {
        }
        fireIntervalRemoved(this, 0, list.size());
    }

    void clear() {
        list.clear();
        try {
            updateProperty();
        } catch (ValidationException ignored) {
        }
    }

    void removeElementsAt(int[] selectedIndices) {
        List<File> toRemove = new ArrayList<File>();
        for (int selectedIndex : selectedIndices) {
            toRemove.add(list.get(selectedIndex));
        }
        list.removeAll(toRemove);

        try {
            updateProperty();
        } catch (ValidationException ignored) {
        }
        fireIntervalRemoved(this, 0, list.size());
    }

    private void updateProperty() throws ValidationException {
        File[] files = new File[list.size()];
        for (int i = 0; i < list.size(); i++) {
            files[i] = list.get(i);
        }
        property.setValue(files);
    }
}
