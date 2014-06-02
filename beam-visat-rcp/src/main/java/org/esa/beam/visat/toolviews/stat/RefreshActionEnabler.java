package org.esa.beam.visat.toolviews.stat;

import com.bc.ceres.binding.PropertyContainer;

import javax.swing.AbstractButton;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashSet;

/**
* Created with IntelliJ IDEA.
* User: tonio
* Date: 23.04.12
* Time: 11:07
* To change this template use File | Settings | File Templates.
*/
public class RefreshActionEnabler implements PropertyChangeListener {

    public final static String PROPERTY_NAME_AUTO_MIN_MAX = "autoMinMax";
    public final static String PROPERTY_NAME_MIN = "min";
    public final static String PROPERTY_NAME_MAX = "max";
    public static final String PROPERTY_NAME_NUM_BINS = "numBins";
    public final static String PROPERTY_NAME_USE_ROI_MASK = "useRoiMask";
    public final static String PROPERTY_NAME_ROI_MASK = "roiMask";
    public final static String PROPERTY_NAME_X_BAND = "xBand";
    public final static String PROPERTY_NAME_Y_BAND = "yBand";

    HashSet<String> names = new HashSet<String>();
    AbstractButton refreshButton;

    public RefreshActionEnabler(AbstractButton rb, String ... componentNames) {
        for(String name:componentNames){
            names.add(name);
        }
        refreshButton = rb;
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (names.contains(evt.getPropertyName())) {
            if(evt.getPropertyName().equals(PROPERTY_NAME_USE_ROI_MASK) && evt.getNewValue().equals(true) &&
                ((PropertyContainer)evt.getSource()).getProperty(PROPERTY_NAME_ROI_MASK).getValue()==null){
                return;
            }
            else if(evt.getPropertyName().equals(PROPERTY_NAME_NUM_BINS) && evt.getOldValue().equals(evt.getNewValue())){
                return;
            }
            else if(evt.getPropertyName().equals(PROPERTY_NAME_AUTO_MIN_MAX) && evt.getNewValue().equals(false)){
                return;
            }
            else if(evt.getPropertyName().equals(PROPERTY_NAME_MIN) && (evt.getOldValue().equals(evt.getNewValue()) ||
                ((PropertyContainer)evt.getSource()).getProperty(PROPERTY_NAME_AUTO_MIN_MAX).getValue().equals(true))){
                return;
            }
            else if(evt.getPropertyName().equals(PROPERTY_NAME_MAX) && (evt.getOldValue().equals(evt.getNewValue()) ||
                    ((PropertyContainer)evt.getSource()).getProperty(PROPERTY_NAME_AUTO_MIN_MAX).getValue().equals(true))){
                return;
            }
            else if(evt.getPropertyName().equals(PROPERTY_NAME_X_BAND) &&
                    ((PropertyContainer)evt.getSource()).getProperty(PROPERTY_NAME_Y_BAND).getValue()==null){
                return;
            }
            else if(evt.getPropertyName().equals(PROPERTY_NAME_Y_BAND) &&
                    ((PropertyContainer)evt.getSource()).getProperty(PROPERTY_NAME_X_BAND).getValue()==null){
                return;
            }
            refreshButton.setEnabled(true);
        }
    }

}
