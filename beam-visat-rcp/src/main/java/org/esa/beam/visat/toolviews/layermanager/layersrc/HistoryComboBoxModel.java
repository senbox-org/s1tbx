package org.esa.beam.visat.toolviews.layermanager.layersrc;

import org.esa.beam.util.PropertyMap;

import javax.swing.DefaultComboBoxModel;
import java.util.ArrayList;


public class HistoryComboBoxModel extends DefaultComboBoxModel {

    private final PropertyMap preferences;
    private final int historySize;
    private String propertyFormat;


    public HistoryComboBoxModel(PropertyMap preferences, String propertyPrefix, int historySize) {
        super();
        this.preferences = preferences;
        this.historySize = historySize;
        this.propertyFormat = propertyPrefix + ".%d";
        loadHistory();
    }

    @Override
    public void setSelectedItem(Object anObject) {
        synchronized (this) {
            if (getIndexOf(anObject) >= 0) {    // if contained
                removeElement(anObject);        // remove item
            } else {                             // else
                removeElementAt(getSize() - 1);  // remove oldest
            }

            insertElementAt(anObject, 0);
            super.setSelectedItem(anObject);
        }
    }

    @Override
    public void addElement(Object anObject) {
        synchronized (this) {
            if (getSize() >= historySize) {
                removeElementAt(getSize() - 1);
            }
            insertElementAt(anObject, 0);
        }
    }

    public void loadHistory() {
        final String[] historyItems = loadHistory(preferences, propertyFormat, historySize);
        synchronized (this) {
            for (int i = 0; i < getSize(); i++) {
                removeElementAt(0);
            }
            for (String item : historyItems) {
                addElement(item);
            }
        }
    }

    public void saveHistory() {
        synchronized (this) {
            saveHistory(preferences, propertyFormat, historySize);
        }
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
