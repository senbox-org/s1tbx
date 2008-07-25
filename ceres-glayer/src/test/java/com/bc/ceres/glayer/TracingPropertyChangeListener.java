package com.bc.ceres.glayer;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

/**
 * TODO - Apidoc
*
* @author Norman Fomferra
* @version $revision$ $date$
*/
public class TracingPropertyChangeListener implements PropertyChangeListener {
    public String trace = "";
    public void propertyChange(PropertyChangeEvent event) {
        trace += event.getPropertyName() + ";";
    }
}
