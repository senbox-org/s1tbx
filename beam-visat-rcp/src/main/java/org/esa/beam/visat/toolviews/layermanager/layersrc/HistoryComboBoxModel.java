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

package org.esa.beam.visat.toolviews.layermanager.layersrc;

import org.esa.beam.framework.ui.UserInputHistory;

import javax.swing.ComboBoxModel;
import javax.swing.event.EventListenerList;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

/**
 * <i>Note: This API is not public yet and may significantly change in the future. Use it at your own risk.</i>
 */
public class HistoryComboBoxModel implements ComboBoxModel {

    private final UserInputHistory history;
    private EventListenerList listenerList;
    private String selectedItem;


    public HistoryComboBoxModel(UserInputHistory history) {
        this.history = history;
        listenerList = new EventListenerList();
    }

    public UserInputHistory getHistory() {
        return history;
    }

    @Override
    public void setSelectedItem(Object anObject) {
        if (anObject instanceof String) {
            selectedItem = (String) anObject;
            history.push(selectedItem);
            fireContentChanged();
        }
    }

    @Override
    public Object getSelectedItem() {
        return selectedItem;
    }

    @Override
    public int getSize() {
        return history.getNumEntries();

    }

    @Override
    public Object getElementAt(int index) {
        return history.getEntries()[index];
    }

    @Override
    public void addListDataListener(ListDataListener listener) {
        listenerList.add(ListDataListener.class, listener);
    }

    @Override
    public void removeListDataListener(ListDataListener listener) {
        listenerList.remove(ListDataListener.class, listener);
    }

    private void fireContentChanged() {
        final ListDataListener[] listDataListeners = listenerList.getListeners(ListDataListener.class);
        for (ListDataListener listener : listDataListeners) {
            listener.contentsChanged(new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, 0, 0));
        }
    }
}
