package org.esa.beam.visat.toolviews.layermanager.layersrc;

import com.bc.ceres.core.Assert;
import org.esa.beam.util.PropertyMap;

import javax.swing.DefaultComboBoxModel;
import java.util.ArrayList;


public class HistoryComboBoxModel extends DefaultComboBoxModel {

    private static final Validator DEFAULT_VALIDATOR = new Validator() {
        @Override
        public boolean isValid(String entry) {
            return !entry.isEmpty();
        }
    };

    private final PropertyMap preferences;
    private final int historySize;
    private final String propertyFormat;
    private Validator validator;


    public HistoryComboBoxModel(PropertyMap preferences, String propertyPrefix, int historySize) {
        this(preferences, propertyPrefix, historySize, DEFAULT_VALIDATOR);
    }

    public HistoryComboBoxModel(PropertyMap preferences, String propertyPrefix, int historySize, Validator validator) {
        this.preferences = preferences;
        this.historySize = historySize;
        this.propertyFormat = propertyPrefix + ".%d";
        this.validator = validator;
        loadHistory();
    }

    public void setValidator(Validator validator) {
        Assert.argument(validator != null, "validator != null");
        this.validator = validator;
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

    public final void loadHistory() {
        final String[] historyItems = loadHistory(preferences, propertyFormat, historySize);
        for (int i = 0; i < getSize(); i++) {
            removeElementAt(0);
        }
        for (String item : historyItems) {
            addElement(item);
        }
    }

    public synchronized void saveHistory() {
        saveHistory(preferences, propertyFormat);
    }

    private String[] loadHistory(PropertyMap preferences, String propertyFormat, int historySize) {
        ArrayList<String> historyList = new ArrayList<String>(historySize);
        for (int i = 0; i < historySize; i++) {
            String filePath = preferences.getPropertyString(String.format(propertyFormat, i));
            if (validator.isValid(filePath)) {
                historyList.add(0, filePath);
            }
        }
        return historyList.toArray(new String[historyList.size()]);
    }

    private void saveHistory(PropertyMap preferences, String propertyFormat) {
        for (int i = 0; i < historySize; i++) {
            String property = "";
            if (i < getSize()) {
                property = (String) getElementAt(i);
            }
            preferences.setPropertyString(String.format(propertyFormat, i), property);
        }
    }

    public interface Validator {

        boolean isValid(String entry);
    }
}
