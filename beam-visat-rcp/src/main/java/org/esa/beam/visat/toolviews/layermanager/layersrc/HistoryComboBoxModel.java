package org.esa.beam.visat.toolviews.layermanager.layersrc;

import org.esa.beam.util.PropertyMap;

import javax.swing.DefaultComboBoxModel;
import java.util.ArrayList;


public class HistoryComboBoxModel extends DefaultComboBoxModel {

    private final PropertyMap preferences;
    private final int historySize;
    private final String propertyFormat;


    public HistoryComboBoxModel(PropertyMap preferences, String propertyPrefix, int historySize) {
        this.preferences = preferences;
        this.historySize = historySize;
        this.propertyFormat = propertyPrefix + ".%d";
        loadHistory();
    }

    @Override
    public synchronized void setSelectedItem(Object anObject) {
        if (getIndexOf(anObject) >= 0) {            // if contained
            removeElement(anObject);                // remove item
        } else if (getSize() >= historySize) {      // else
            removeElementAt(getSize() - 1);         // remove oldest
        }

        insertElementAt(anObject, 0);
        super.setSelectedItem(anObject);
    }

    @Override
    public synchronized void addElement(Object anObject) {
        if (getSize() >= historySize) {
            removeElementAt(getSize() - 1);
        }
        insertElementAt(anObject, 0);
    }

    public final synchronized void loadHistory() {
        final String[] historyItems = loadHistory(preferences, propertyFormat, historySize);
        for (int i = 0; i < getSize(); i++) {
            removeElementAt(0);
        }
        for (String item : historyItems) {
            addElement(item);
        }
    }

    public synchronized void saveHistory() {
        saveHistory(preferences, propertyFormat, historySize);
    }

    private static String[] loadHistory(PropertyMap preferences, String propertyFormat, int historySize) {
        ArrayList<String> historyList = new ArrayList<String>(historySize);
        for (int i = 0; i < historySize; i++) {
            String filePath = preferences.getPropertyString(String.format(propertyFormat, i));
            if (!filePath.isEmpty()) {
                historyList.add(0, filePath);
            }
        }
        return historyList.toArray(new String[historyList.size()]);
    }

    private void saveHistory(PropertyMap preferences, String propertyFormat, int historySize) {
        for (int i = 0; i < historySize; i++) {
            preferences.setPropertyString(String.format(propertyFormat, i), (String) getElementAt(i));
        }
    }

}
