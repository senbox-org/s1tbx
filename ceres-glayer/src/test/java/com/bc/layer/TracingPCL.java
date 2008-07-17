package com.bc.layer;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * Created by IntelliJ IDEA.
 * User: norman
 * Date: 09.04.2008
 * Time: 14:26:10
 * To change this template use File | Settings | File Templates.
 */
public class TracingPCL implements PropertyChangeListener {

    String trace = "";

    public void propertyChange(PropertyChangeEvent evt) {
        trace += evt.getPropertyName() + ";";
    }
}
